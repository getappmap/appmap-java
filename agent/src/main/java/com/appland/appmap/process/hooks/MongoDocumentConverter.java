package com.appland.appmap.process.hooks;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;

/**
 * Reaches into the MongoDB Java driver by reflection, so that
 * {@link MongoQueryShape} can walk {@code Bson} filters and updates, write
 * models, index models and POJO documents as plain Maps and Lists.
 *
 * <p>
 * Every lookup is defensive: the agent may run against any driver version
 * (3.x through 5.x), or against a wrapper that only implements part of the
 * API. Anything that cannot be converted is reported as null and rendered as
 * {@code ?}.
 */
final class MongoDocumentConverter implements MongoQueryShape.DocumentConverter {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);

  static final String BSON = "org.bson.conversions.Bson";
  static final String BSON_VALUE = "org.bson.BsonValue";
  static final String BSON_DOCUMENT = "org.bson.BsonDocument";
  static final String BSON_WRITER = "org.bson.BsonWriter";
  static final String BSON_DOCUMENT_WRITER = "org.bson.BsonDocumentWriter";
  static final String CODEC_REGISTRY = "org.bson.codecs.configuration.CodecRegistry";
  static final String ENCODER = "org.bson.codecs.Encoder";
  static final String ENCODER_CONTEXT = "org.bson.codecs.EncoderContext";
  static final String CLIENT_SESSION = "com.mongodb.session.ClientSession";
  static final String WRITE_MODEL = "com.mongodb.client.model.WriteModel";
  static final String INDEX_MODEL = "com.mongodb.client.model.IndexModel";
  static final String MONGO_NAMESPACE = "com.mongodb.MongoNamespace";

  // Whether a class is (a subtype of) a named driver type. Keyed by the class,
  // then by the type name. Classes are held weakly so unloaded applications
  // are not pinned.
  private static final Map<Class<?>, ConcurrentHashMap<String, Boolean>> typeCache = Collections
      .synchronizedMap(new WeakHashMap<Class<?>, ConcurrentHashMap<String, Boolean>>());

  static boolean isA(Object value, String typeName) {
    if (value == null) {
      return false;
    }
    Class<?> cls = value.getClass();
    ConcurrentHashMap<String, Boolean> forClass = typeCache.get(cls);
    if (forClass == null) {
      forClass = new ConcurrentHashMap<String, Boolean>();
      typeCache.put(cls, forClass);
    }
    Boolean known = forClass.get(typeName);
    if (known == null) {
      known = hasSupertype(cls, typeName);
      forClass.put(typeName, known);
    }
    return known;
  }

  private static boolean hasSupertype(Class<?> cls, String typeName) {
    for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
      if (c.getName().equals(typeName)) {
        return true;
      }
      for (Class<?> i : c.getInterfaces()) {
        if (i.getName().equals(typeName) || hasSupertype(i, typeName)) {
          return true;
        }
      }
    }
    return false;
  }

  static boolean isClientSession(Object value) {
    return isA(value, CLIENT_SESSION);
  }

  // Reflection handles for one class loader's copy of the driver.
  private static final class Handles {
    final Class<?> bsonDocument;
    final Method toBsonDocument;
    /** The no-argument overload, available from bson 4.2 on; null before that. */
    final Method toBsonDocumentDefault;
    final Method registryGet;
    final Method encode;
    final Object encoderContext;
    final java.lang.reflect.Constructor<?> documentWriter;

    Handles(ClassLoader loader) throws Exception {
      Class<?> bson = Class.forName(BSON, false, loader);
      Class<?> registry = Class.forName(CODEC_REGISTRY, false, loader);
      bsonDocument = Class.forName(BSON_DOCUMENT, false, loader);
      toBsonDocument = bson.getMethod("toBsonDocument", Class.class, registry);
      Method noArgs = null;
      try {
        noArgs = bson.getMethod("toBsonDocument");
      } catch (NoSuchMethodException e) {
        // bson < 4.2
      }
      toBsonDocumentDefault = noArgs;
      registryGet = registry.getMethod("get", Class.class);
      Class<?> writer = Class.forName(BSON_WRITER, false, loader);
      Class<?> context = Class.forName(ENCODER_CONTEXT, false, loader);
      encode = Class.forName(ENCODER, false, loader).getMethod("encode", writer, Object.class, context);
      Object builder = context.getMethod("builder").invoke(null);
      encoderContext = builder.getClass().getMethod("build").invoke(builder);
      documentWriter = Class.forName(BSON_DOCUMENT_WRITER, false, loader).getConstructor(bsonDocument);
    }
  }

  private static final Map<ClassLoader, Handles> handlesByLoader = Collections
      .synchronizedMap(new WeakHashMap<ClassLoader, Handles>());
  private static final Handles NO_HANDLES = null;

  private static Handles handles(ClassLoader loader) {
    if (loader == null) {
      loader = ClassLoader.getSystemClassLoader();
    }
    synchronized (handlesByLoader) {
      if (handlesByLoader.containsKey(loader)) {
        return handlesByLoader.get(loader);
      }
      Handles h;
      try {
        h = new Handles(loader);
      } catch (Throwable e) {
        logger.debug(e, "bson classes not available from {}", loader);
        h = NO_HANDLES;
      }
      handlesByLoader.put(loader, h);
      return h;
    }
  }

  // Accessors on the collection object itself, cached per collection class.
  private static final Map<Class<?>, Method[]> collectionAccessors = Collections
      .synchronizedMap(new WeakHashMap<Class<?>, Method[]>());

  private static Method[] accessors(Object collection) {
    Class<?> cls = collection.getClass();
    Method[] found = collectionAccessors.get(cls);
    if (found == null) {
      found = new Method[2];
      try {
        found[0] = cls.getMethod("getNamespace");
        found[0].setAccessible(true);
      } catch (Throwable e) {
        logger.debug(e, "{} has no getNamespace()", cls.getName());
      }
      try {
        found[1] = cls.getMethod("getCodecRegistry");
        found[1].setAccessible(true);
      } catch (Throwable e) {
        logger.debug(e, "{} has no getCodecRegistry()", cls.getName());
      }
      collectionAccessors.put(cls, found);
    }
    return found;
  }

  /** The collection name, or null when it cannot be determined. */
  static String collectionName(Object collection) {
    if (collection == null) {
      return null;
    }
    Method getNamespace = accessors(collection)[0];
    if (getNamespace == null) {
      return null;
    }
    try {
      Object namespace = getNamespace.invoke(collection);
      if (namespace == null) {
        return null;
      }
      Object name = namespace.getClass().getMethod("getCollectionName").invoke(namespace);
      return name == null ? null : name.toString();
    } catch (Throwable e) {
      logger.debug(e, "failed to get collection name");
      return null;
    }
  }

  static MongoQueryShape.DocumentConverter forCollection(Object collection) {
    Object registry = null;
    ClassLoader loader = collection == null ? null : collection.getClass().getClassLoader();
    if (collection != null) {
      Method getCodecRegistry = accessors(collection)[1];
      if (getCodecRegistry != null) {
        try {
          registry = getCodecRegistry.invoke(collection);
        } catch (Throwable e) {
          logger.debug(e, "failed to get codec registry");
        }
      }
    }
    return new MongoDocumentConverter(loader, registry);
  }

  private final Handles handles;
  private final Object registry;

  MongoDocumentConverter(ClassLoader loader, Object registry) {
    this.handles = handles(loader);
    this.registry = registry;
  }

  @Override
  public Object convert(Object value, boolean documentPosition) {
    if (value == null) {
      return null;
    }
    try {
      if (isA(value, BSON)) {
        return toBsonDocument(value);
      }
      if (isA(value, BSON_VALUE)) {
        // A scalar BsonValue (BsonDocument and BsonArray are a Map and a List,
        // and never get here).
        return null;
      }
      if (isA(value, WRITE_MODEL)) {
        return writeModel(value);
      }
      if (isA(value, INDEX_MODEL)) {
        Map<String, Object> shape = new LinkedHashMap<String, Object>();
        shape.put("key", getter(value, "getKeys"));
        return shape;
      }
      if (isA(value, MONGO_NAMESPACE)) {
        Object fullName = getter(value, "getFullName");
        return fullName == null ? null : fullName.toString();
      }
      if (documentPosition && !isJdkType(value)) {
        return encode(value);
      }
    } catch (Throwable e) {
      logger.debug(e, "failed to convert {} to a document", value.getClass().getName());
    }
    return null;
  }

  private static boolean isJdkType(Object value) {
    String name = value.getClass().getName();
    return name.startsWith("java.") || name.startsWith("javax.");
  }

  private Object toBsonDocument(Object bson) throws Exception {
    if (handles == null) {
      return null;
    }
    if (registry != null) {
      return handles.toBsonDocument.invoke(bson, handles.bsonDocument, registry);
    }
    if (handles.toBsonDocumentDefault != null) {
      return handles.toBsonDocumentDefault.invoke(bson);
    }
    return null;
  }

  /**
   * Encodes an arbitrary document object (a POJO, a Kotlin data class, ...) with
   * the collection's codec registry, the way the driver will when it sends it.
   * Returns null when the registry has no codec for it.
   */
  private Object encode(Object document) throws Exception {
    if (handles == null || registry == null) {
      return null;
    }
    Object codec;
    try {
      codec = handles.registryGet.invoke(registry, document.getClass());
    } catch (Throwable e) {
      // CodecConfigurationException: no codec for this class
      return null;
    }
    if (codec == null) {
      return null;
    }
    Object target = handles.bsonDocument.getConstructor().newInstance();
    Object writer = handles.documentWriter.newInstance(target);
    handles.encode.invoke(codec, writer, document, handles.encoderContext);
    return target;
  }

  // Renders a WriteModel the way the Node driver's bulkWrite operations look:
  // { "insertOne": { "document": ... } }, { "updateOne": { "filter": ...,
  // "update": ... } }, and so on.
  private Object writeModel(Object model) {
    String kind = model.getClass().getSimpleName();
    Map<String, Object> body = new LinkedHashMap<String, Object>();
    String key;
    switch (kind) {
    case "InsertOneModel":
      key = "insertOne";
      body.put("document", document(getter(model, "getDocument")));
      break;
    case "UpdateOneModel":
    case "UpdateManyModel":
      key = kind.equals("UpdateOneModel") ? "updateOne" : "updateMany";
      body.put("filter", getter(model, "getFilter"));
      Object update = getter(model, "getUpdate");
      if (update == null) {
        update = getter(model, "getUpdatePipeline");
      }
      body.put("update", update);
      break;
    case "ReplaceOneModel":
      key = "replaceOne";
      body.put("filter", getter(model, "getFilter"));
      body.put("replacement", document(getter(model, "getReplacement")));
      break;
    case "DeleteOneModel":
    case "DeleteManyModel":
      key = kind.equals("DeleteOneModel") ? "deleteOne" : "deleteMany";
      body.put("filter", getter(model, "getFilter"));
      break;
    default:
      return null;
    }
    Map<String, Object> shape = new LinkedHashMap<String, Object>();
    shape.put(key, body);
    return shape;
  }

  // A document nested in a write model sits in a document position, but the
  // shape renderer only knows that for top-level arguments. Convert it here.
  private Object document(Object value) {
    if (value == null || value instanceof Map || MongoQueryShape.isSequence(value)) {
      return value;
    }
    Object converted = convert(value, true);
    return converted == null ? value : converted;
  }

  private static Object getter(Object target, String name) {
    if (target == null) {
      return null;
    }
    try {
      Method m = target.getClass().getMethod(name);
      m.setAccessible(true);
      return m.invoke(target);
    } catch (Throwable e) {
      logger.debug(e, "{}.{}() failed", target.getClass().getName(), name);
      return null;
    }
  }
}
