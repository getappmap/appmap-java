package com.appland.appmap.process.hooks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Renders a MongoDB collection operation as a query statement with a
 * normalized argument shape, so that it can be recorded as a {@code sql_query}
 * event (database_type "mongodb") and compared across recordings the way SQL
 * is.
 *
 * <p>
 * The same rules are implemented by the Node agent
 * ({@code src/hooks/mongoQuery.ts} in appmap-node). Keep them in sync.
 *
 * <p>
 * Statement form: {@code db.<collection>.<method>(<arg>, <arg>, ...)}
 * <ul>
 * <li>The collection is written {@code db.name} when the name is a plain
 * identifier path, and {@code db.getCollection("name")} otherwise.</li>
 * <li>Arguments are rendered in the order of the driver method's parameters.
 * Trailing null arguments are omitted.</li>
 * <li>Documents (filters, updates, replacements, inserted documents, index
 * specs) keep their keys, in order, and every leaf value becomes {@code ?}.
 * Keys are written as JSON strings.</li>
 * <li>Arrays inside documents, and top-level lists of documents (insertMany,
 * bulkWrite, createIndexes), keep only the distinct element shapes, in order
 * of first appearance. {@code {"$in": [1, 2, 3]}} becomes
 * {@code {"$in": [?]}}, and a thousand inserted documents of the same shape
 * become one.</li>
 * <li>Aggregation pipelines (aggregate, watch, and update pipelines) keep
 * every stage in order, because stage order and repetition are part of the
 * query.</li>
 * <li>Name-like arguments (a distinct field, an index name, a new collection
 * name) are kept verbatim as JSON strings; they identify the query the same
 * way a table or column name does.</li>
 * <li>Everything that is not a Map, an Iterable, an array, or convertible to
 * a document by the {@link DocumentConverter} (BSON values such as ObjectId
 * and Decimal128, primitives, Dates, null) is a leaf and becomes
 * {@code ?}.</li>
 * <li>Options objects ({@code UpdateOptions}, {@code IndexOptions}, ...) are
 * opaque in Java and become {@code ?}. The Node agent renders the keys of its
 * options document instead.</li>
 * <li>Nesting deeper than {@link #MAX_DEPTH}, and cyclic references, become
 * {@code ?}. Only the first {@link #MAX_ARRAY_ELEMENTS} elements of an array
 * are examined.</li>
 * </ul>
 */
public final class MongoQueryShape {
  public static final String DATABASE_TYPE = "mongodb";
  public static final int MAX_DEPTH = 32;
  public static final int MAX_ARRAY_ELEMENTS = 1000;

  static final String PLACEHOLDER = "?";

  /**
   * Turns driver-specific objects into documents the shape renderer can walk.
   * Implementations reach into the driver by reflection; this class has no
   * dependency on it.
   */
  public interface DocumentConverter {
    /**
     * @param value a non-null value that is not a Map, Iterable or array
     * @param documentPosition true when the value sits where the driver expects
     *          a document (an inserted document, a replacement, a pipeline
     *          stage), so that an arbitrary object may be encoded with the
     *          collection's codec
     * @return a Map, Iterable or array to walk instead of {@code value}, a
     *         String to render verbatim, or null when the value is a leaf
     */
    Object convert(Object value, boolean documentPosition);
  }

  static final DocumentConverter NO_CONVERSION = new DocumentConverter() {
    @Override
    public Object convert(Object value, boolean documentPosition) {
      return null;
    }
  };

  enum ArgKind {
    DOCUMENT, PIPELINE, NAME, OPTIONS
  }

  static ArgKind argKind(String name) {
    switch (name) {
    case "pipeline":
      return ArgKind.PIPELINE;
    case "key":
    case "newName":
    case "indexName":
    case "indexes":
      return ArgKind.NAME;
    case "options":
      return ArgKind.OPTIONS;
    default:
      return ArgKind.DOCUMENT;
    }
  }

  private MongoQueryShape() {
  }

  /**
   * Formats a collection method call as a normalized statement. Never throws: if
   * the arguments cannot be inspected the statement is rendered with a single
   * {@code ?} in place of the argument list.
   */
  public static String formatStatement(String collection, String method, String[] argNames, Object[] args,
      DocumentConverter converter) {
    String prefix = formatCollection(collection) + "." + method;
    try {
      return prefix + "(" + formatArgs(argNames, args, converter) + ")";
    } catch (Throwable e) {
      return prefix + "(" + PLACEHOLDER + ")";
    }
  }

  private static final Pattern IDENTIFIER_PATH = Pattern.compile("[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)*");

  public static String formatCollection(String name) {
    if (name == null) {
      return "db.getCollection(" + PLACEHOLDER + ")";
    }
    if (IDENTIFIER_PATH.matcher(name).matches()) {
      return "db." + name;
    }
    return "db.getCollection(" + quote(name) + ")";
  }

  private static String formatArgs(String[] argNames, Object[] args, DocumentConverter converter) {
    int last = Math.min(argNames.length, args == null ? 0 : args.length);
    while (last > 0 && args[last - 1] == null) {
      last--;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < last; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(formatArg(argNames[i], args[i], converter));
    }
    return sb.toString();
  }

  private static String formatArg(String name, Object value, DocumentConverter converter) {
    switch (argKind(name)) {
    case NAME:
      if (value instanceof CharSequence) {
        return quote(value.toString());
      }
      if (value instanceof Iterable && allStrings((Iterable<?>) value)) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object v : (Iterable<?>) value) {
          if (!first) {
            sb.append(", ");
          }
          first = false;
          sb.append(quote(v.toString()));
        }
        return sb.append("]").toString();
      }
      return shape(value, false, false, converter);
    case PIPELINE:
      return shape(value, true, true, converter);
    case OPTIONS:
      return shape(value, false, false, converter);
    case DOCUMENT:
    default:
      // An update can be a pipeline (a list of stages) instead of a document.
      boolean ordered = "update".equals(name) && isSequence(value);
      return shape(value, ordered, true, converter);
    }
  }

  private static boolean allStrings(Iterable<?> values) {
    for (Object v : values) {
      if (!(v instanceof CharSequence)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Renders the shape of a value: keys kept, leaves replaced with {@code ?}.
   *
   * @param ordered when true, a top-level array keeps all of its elements in
   *          order (a pipeline); otherwise distinct element shapes are kept
   * @param documentPosition whether the value (or the elements of a top-level
   *          array) sit where the driver expects a document
   */
  public static String shape(Object value, boolean ordered, boolean documentPosition, DocumentConverter converter) {
    StringBuilder sb = new StringBuilder();
    shapeOf(value, ordered, documentPosition, 0, new IdentityHashMap<Object, Boolean>(), converter, sb);
    return sb.toString();
  }

  private static void shapeOf(Object value, boolean ordered, boolean documentPosition, int depth,
      IdentityHashMap<Object, Boolean> ancestors, DocumentConverter converter, StringBuilder sb) {
    if (depth > MAX_DEPTH || value == null) {
      sb.append(PLACEHOLDER);
      return;
    }

    if (!(value instanceof Map) && !isSequence(value)) {
      Object converted = converter.convert(value, documentPosition);
      if (converted == null) {
        sb.append(PLACEHOLDER);
        return;
      }
      if (converted instanceof CharSequence) {
        sb.append(quote(converted.toString()));
        return;
      }
      value = converted;
    }

    if (ancestors.containsKey(value)) {
      sb.append(PLACEHOLDER);
      return;
    }
    ancestors.put(value, Boolean.TRUE);
    try {
      if (value instanceof Map) {
        shapeOfMap((Map<?, ?>) value, depth, ancestors, converter, sb);
      } else if (isSequence(value)) {
        shapeOfSequence(value, ordered, documentPosition, depth, ancestors, converter, sb);
      } else {
        sb.append(PLACEHOLDER);
      }
    } finally {
      ancestors.remove(value);
    }
  }

  private static void shapeOfMap(Map<?, ?> map, int depth, IdentityHashMap<Object, Boolean> ancestors,
      DocumentConverter converter, StringBuilder sb) {
    sb.append('{');
    boolean first = true;
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!first) {
        sb.append(", ");
      }
      first = false;
      sb.append(quote(String.valueOf(entry.getKey()))).append(": ");
      shapeOf(entry.getValue(), false, false, depth + 1, ancestors, converter, sb);
    }
    sb.append('}');
  }

  private static void shapeOfSequence(Object value, boolean ordered, boolean documentPosition, int depth,
      IdentityHashMap<Object, Boolean> ancestors, DocumentConverter converter, StringBuilder sb) {
    List<String> parts = new ArrayList<String>();
    Set<String> seen = ordered ? null : new HashSet<String>();
    Iterator<?> it = iterator(value);
    for (int i = 0; i < MAX_ARRAY_ELEMENTS && it.hasNext(); i++) {
      StringBuilder part = new StringBuilder();
      shapeOf(it.next(), false, documentPosition, depth + 1, ancestors, converter, part);
      String rendered = part.toString();
      if (ordered || seen.add(rendered)) {
        parts.add(rendered);
      }
    }
    sb.append('[');
    for (int i = 0; i < parts.size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(parts.get(i));
    }
    sb.append(']');
  }

  static boolean isSequence(Object value) {
    return value instanceof Iterable || (value != null && value.getClass().isArray()
        && !value.getClass().getComponentType().isPrimitive());
  }

  private static Iterator<?> iterator(Object value) {
    if (value instanceof Iterable) {
      return ((Iterable<?>) value).iterator();
    }
    final Object[] array = (Object[]) value;
    return new Iterator<Object>() {
      private int i = 0;

      @Override
      public boolean hasNext() {
        return i < array.length;
      }

      @Override
      public Object next() {
        return array[i++];
      }
    };
  }

  /** Quotes a string the way JSON.stringify does. */
  static String quote(String s) {
    StringBuilder sb = new StringBuilder(s.length() + 2);
    sb.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
      case '"':
        sb.append("\\\"");
        break;
      case '\\':
        sb.append("\\\\");
        break;
      case '\n':
        sb.append("\\n");
        break;
      case '\r':
        sb.append("\\r");
        break;
      case '\t':
        sb.append("\\t");
        break;
      case '\b':
        sb.append("\\b");
        break;
      case '\f':
        sb.append("\\f");
        break;
      default:
        if (c < 0x20) {
          sb.append(String.format("\\u%04x", (int) c));
        } else {
          sb.append(c);
        }
      }
    }
    return sb.append('"').toString();
  }
}
