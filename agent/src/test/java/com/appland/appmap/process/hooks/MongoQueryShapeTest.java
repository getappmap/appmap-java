package com.appland.appmap.process.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.appland.appmap.process.hooks.MongoQueryShape.DocumentConverter;

/**
 * The expectations here match the Node agent's tests for
 * {@code src/hooks/mongoQuery.ts}: both agents must render the same
 * statement for the same operation.
 */
public class MongoQueryShapeTest {
  private static final DocumentConverter NONE = MongoQueryShape.NO_CONVERSION;

  private static Map<String, Object> doc(Object... kv) {
    Map<String, Object> m = new LinkedHashMap<String, Object>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put((String) kv[i], kv[i + 1]);
    }
    return m;
  }

  private static String statement(String method, String[] names, Object... args) {
    return MongoQueryShape.formatStatement("users", method, names, args, NONE);
  }

  private static String shape(Object value) {
    return MongoQueryShape.shape(value, false, false, NONE);
  }

  @Test
  public void collectionNames() {
    assertEquals("db.users", MongoQueryShape.formatCollection("users"));
    assertEquals("db.users.archive", MongoQueryShape.formatCollection("users.archive"));
    assertEquals("db._tmp$1", MongoQueryShape.formatCollection("_tmp$1"));
    assertEquals("db.getCollection(\"my-coll\")", MongoQueryShape.formatCollection("my-coll"));
    assertEquals("db.getCollection(\"with space\")", MongoQueryShape.formatCollection("with space"));
    assertEquals("db.getCollection(\"\")", MongoQueryShape.formatCollection(""));
    assertEquals("db.getCollection(\"q\\\"uote\")", MongoQueryShape.formatCollection("q\"uote"));
    assertEquals("db.getCollection(\"a..b\")", MongoQueryShape.formatCollection("a..b"));
    assertEquals("db.getCollection(?)", MongoQueryShape.formatCollection(null));
  }

  @Test
  public void keysKeptLeavesReplaced() {
    assertEquals("{\"a\": ?, \"b\": ?, \"c\": ?, \"e\": ?}", shape(doc("a", 1, "b", "x", "c", null, "e", true)));
    assertEquals("{\"z\": {\"y\": {\"x\": ?}}, \"a\": ?}", shape(doc("z", doc("y", doc("x", 1)), "a", 2)));
  }

  @Test
  public void operatorsAreKeys() {
    Object filter = doc("age", doc("$gt", 18, "$lt", 65), "$or", Arrays.asList(doc("a", 1), doc("b", 2)));
    assertEquals("{\"age\": {\"$gt\": ?, \"$lt\": ?}, \"$or\": [{\"a\": ?}, {\"b\": ?}]}", shape(filter));
  }

  @Test
  public void arraysCollapseToDistinctShapes() {
    assertEquals("{\"$in\": [?]}", shape(doc("$in", Arrays.asList(1, 2, 3))));
    assertEquals("[{\"a\": ?}, {\"b\": ?}]", shape(Arrays.asList(doc("a", 1), doc("a", 2), doc("b", 3), doc("a", 4))));
    assertEquals("[?]", shape(new Object[] { "x", "y" }));
    assertEquals("[]", shape(Collections.emptyList()));
    assertEquals("{}", shape(Collections.emptyMap()));
  }

  @Test
  public void orderedArraysKeepEveryElement() {
    List<Object> pipeline = Arrays.<Object>asList(doc("$unwind", "$a"), doc("$unwind", "$b"));
    assertEquals("[{\"$unwind\": ?}, {\"$unwind\": ?}]", MongoQueryShape.shape(pipeline, true, true, NONE));
    // only the top level is ordered; nested arrays are still collapsed
    Object stage = doc("$match", doc("a", doc("$in", Arrays.asList(1, 2))));
    assertEquals("[{\"$match\": {\"a\": {\"$in\": [?]}}}]",
        MongoQueryShape.shape(Collections.singletonList(stage), true, true, NONE));
  }

  @Test
  public void leaves() {
    assertEquals("{\"at\": ?, \"bytes\": ?, \"n\": ?, \"o\": ?}",
        shape(doc("at", new Date(), "bytes", new byte[] { 1 }, "n", 1.5, "o", new Object())));
    assertEquals("?", shape(null));
    assertEquals("?", shape("string"));
  }

  @Test
  public void keysAreEscaped() {
    assertEquals("{\"he said \\\"hi\\\"\": ?, \"a.b\": ?, \"\": ?, \"\\n\": ?, \"\\u0001\": ?}",
        shape(doc("he said \"hi\"", 1, "a.b", 2, "", 3, "\n", 4, "\u0001", 5)));
    // non-string keys are stringified
    Map<Object, Object> m = new LinkedHashMap<Object, Object>();
    m.put(2, "x");
    assertEquals("{\"2\": ?}", shape(m));
  }

  @Test
  public void cyclesDoNotRecurse() {
    Map<String, Object> d = doc("a", 1);
    d.put("self", d);
    List<Object> arr = new ArrayList<Object>();
    arr.add(1);
    arr.add(arr);
    d.put("arr", arr);
    assertEquals("{\"a\": ?, \"self\": ?, \"arr\": [?]}", shape(d));

    // the same object in two places is not a cycle
    Map<String, Object> inner = doc("x", 1);
    assertEquals("{\"a\": {\"x\": ?}, \"b\": {\"x\": ?}}", shape(doc("a", inner, "b", inner)));
  }

  @Test
  public void depthLimit() {
    Object d = 1;
    for (int i = 0; i < MongoQueryShape.MAX_DEPTH + 5; i++) {
      d = doc("n", d);
    }
    String rendered = shape(d);
    StringBuilder open = new StringBuilder();
    StringBuilder close = new StringBuilder();
    for (int i = 0; i <= MongoQueryShape.MAX_DEPTH; i++) {
      open.append("{\"n\": ");
      close.append("}");
    }
    assertTrue(rendered.startsWith(open.toString()), rendered);
    assertTrue(rendered.endsWith("\"n\": ?" + close), rendered);
  }

  @Test
  public void arrayElementLimit() {
    List<Object> arr = new ArrayList<Object>();
    for (int i = 0; i < MongoQueryShape.MAX_ARRAY_ELEMENTS; i++) {
      arr.add(doc("a", i));
    }
    arr.add(doc("b", 2));
    assertEquals("[{\"a\": ?}]", shape(arr));
  }

  @Test
  public void statementArguments() {
    String[] names = { "filter", "update", "options" };
    assertEquals("db.users.updateOne({\"_id\": ?}, {\"$set\": {\"name\": ?}, \"$inc\": {\"n\": ?}}, ?)",
        statement("updateOne", names, doc("_id", new Object()), doc("$set", doc("name", "x"), "$inc", doc("n", 1)),
            new Object()));
    // trailing nulls are omitted; inner nulls are placeholders
    assertEquals("db.users.find()", statement("find", new String[] { "filter" }));
    assertEquals("db.users.find()", statement("find", new String[] { "filter" }, (Object) null));
    assertEquals("db.users.find({})", statement("find", new String[] { "filter", "options" }, doc(), null));
    assertEquals("db.users.countDocuments(?, ?)",
        statement("countDocuments", new String[] { "filter", "options" }, null, new Object()));
    // arguments beyond the declared parameters are ignored
    assertEquals("db.users.drop({})", statement("drop", new String[] { "options" }, doc(), "extra", 1));
    assertEquals("db.users.listIndexes()", statement("listIndexes", new String[0]));
  }

  @Test
  public void pipelinesKeepOrder() {
    List<Object> pipeline = Arrays.<Object>asList(doc("$match", doc("status", "A")), doc("$unwind", "$items"),
        doc("$unwind", "$items.parts"), doc("$group", doc("_id", "$cust", "total", doc("$sum", "$amount"))));
    assertEquals(
        "db.users.aggregate([{\"$match\": {\"status\": ?}}, {\"$unwind\": ?}, {\"$unwind\": ?}, {\"$group\": {\"_id\": ?, \"total\": {\"$sum\": ?}}}])",
        statement("aggregate", new String[] { "pipeline" }, pipeline));
    // an update given as a pipeline keeps its order too
    assertEquals("db.users.updateMany({}, [{\"$set\": {\"a\": ?}}, {\"$set\": {\"b\": ?}}])",
        statement("updateMany", new String[] { "filter", "update", "options" }, doc(),
            Arrays.asList(doc("$set", doc("a", 1)), doc("$set", doc("b", 2)))));
  }

  @Test
  public void documentListsCollapse() {
    assertEquals("db.users.insertMany([{\"a\": ?}])",
        statement("insertMany", new String[] { "docs", "options" }, Arrays.asList(doc("a", 1), doc("a", 2))));
    Object ops = Arrays.asList(doc("insertOne", doc("document", doc("a", 1))),
        doc("insertOne", doc("document", doc("a", 2))),
        doc("updateOne", doc("filter", doc("a", 1), "update", doc("$set", doc("b", 1)))));
    assertEquals(
        "db.users.bulkWrite([{\"insertOne\": {\"document\": {\"a\": ?}}}, {\"updateOne\": {\"filter\": {\"a\": ?}, \"update\": {\"$set\": {\"b\": ?}}}}])",
        statement("bulkWrite", new String[] { "operations", "options" }, ops));
  }

  @Test
  public void namesAreKeptVerbatim() {
    assertEquals("db.users.distinct(\"email\", {\"a\": ?})",
        statement("distinct", new String[] { "key", "filter" }, "email", doc("a", 1)));
    assertEquals("db.users.renameCollection(\"people\")",
        statement("renameCollection", new String[] { "newName", "options" }, "people"));
    assertEquals("db.users.dropIndex(\"a_1\")", statement("dropIndex", new String[] { "indexName", "options" }, "a_1"));
    assertEquals("db.users.indexExists([\"a_1\", \"b_1\"])",
        statement("indexExists", new String[] { "indexes", "options" }, Arrays.asList("a_1", "b_1")));
    // a name argument that is not a string is rendered as a shape
    assertEquals("db.users.dropIndex({\"a\": ?})",
        statement("dropIndex", new String[] { "indexName", "options" }, doc("a", 1)));
    assertEquals("db.users.distinct([?])", statement("distinct", new String[] { "key", "filter" }, Arrays.asList("a", 1)));
  }

  @Test
  public void converterOutputIsWalked() {
    DocumentConverter converter = new DocumentConverter() {
      @Override
      public Object convert(Object value, boolean documentPosition) {
        if (value instanceof StringBuilder) {
          return value.toString();
        }
        if (documentPosition && value instanceof Integer) {
          return doc("boxed", value);
        }
        if (value instanceof Long) {
          return doc("filter", doc("id", "x"));
        }
        return null;
      }
    };
    assertEquals("db.users.renameCollection(\"db.people\")", MongoQueryShape.formatStatement("users",
        "renameCollection", new String[] { "newName" }, new Object[] { new StringBuilder("db.people") }, converter));
    // a converted value in document position, at the top level and in a list
    assertEquals("db.users.insertOne({\"boxed\": ?})",
        MongoQueryShape.formatStatement("users", "insertOne", new String[] { "doc" }, new Object[] { 1 }, converter));
    assertEquals("db.users.insertMany([{\"boxed\": ?}])", MongoQueryShape.formatStatement("users", "insertMany",
        new String[] { "docs" }, new Object[] { Arrays.asList(1, 2) }, converter));
    // nested values are not in document position
    assertEquals("{\"n\": ?, \"m\": {\"filter\": {\"id\": ?}}}",
        MongoQueryShape.shape(doc("n", 1, "m", 2L), false, false, converter));
    // options are opaque even when the converter would know them
    assertEquals("db.users.drop(?)",
        MongoQueryShape.formatStatement("users", "drop", new String[] { "options" }, new Object[] { 1 }, converter));
  }

  @Test
  public void neverThrows() {
    Map<String, Object> hostile = new LinkedHashMap<String, Object>() {
      @Override
      public java.util.Set<Map.Entry<String, Object>> entrySet() {
        throw new IllegalStateException("boom");
      }
    };
    assertEquals("db.users.find(?)", statement("find", new String[] { "filter" }, hostile));
    DocumentConverter failing = new DocumentConverter() {
      @Override
      public Object convert(Object value, boolean documentPosition) {
        throw new RuntimeException("boom");
      }
    };
    assertEquals("db.users.insertOne(?)", MongoQueryShape.formatStatement("users", "insertOne",
        new String[] { "doc" }, new Object[] { new Object() }, failing));
  }
}
