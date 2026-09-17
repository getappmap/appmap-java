package com.appland.appmap.process.hooks;

import java.util.HashMap;
import java.util.Map;

import org.tinylog.TaggedLogger;

import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.transform.annotations.Unique;

/**
 * Hooks for the MongoDB Java driver's synchronous {@code MongoCollection} API
 * (mongodb-driver-sync 3.x, 4.x and 5.x).
 *
 * <p>
 * This is a port of the Node agent's mongo hook. Each collection operation is
 * recorded as two events:
 * <ol>
 * <li>a function call on the driver's collection class, with the parameters
 * named the way the Node agent names them ({@code filter}, {@code update},
 * {@code doc}, ...) rather than the driver's own parameter names, and</li>
 * <li>nested under it, a {@code sql_query} event with database_type
 * {@code mongodb} whose "sql" is the normalized statement built by
 * {@link MongoQueryShape}, so that Mongo operations land in the same digests
 * and diffs as SQL queries.</li>
 * </ol>
 *
 * <p>
 * Overloads: a leading {@code ClientSession} argument is recorded as a
 * parameter named {@code session} and a trailing result {@code Class} as
 * {@code resultClass}; neither takes part in the statement. The remaining
 * arguments map onto the Node names positionally.
 *
 * <p>
 * {@code find}, {@code aggregate}, {@code watch}, {@code listIndexes} and
 * {@code distinct} return lazy iterables; their statement is recorded when the
 * iterable is created, so options applied to the iterable afterwards
 * ({@code sort}, {@code limit}, {@code projection}) are not part of it.
 *
 * <p>
 * The hook has no compile-time dependency on the driver. It reaches the
 * collection's namespace and codec registry, and converts {@code Bson} values,
 * write models and POJOs to documents, by reflection
 * ({@link MongoDocumentConverter}). Any failure there degrades the shape to
 * {@code ?}; it never fails the application's call.
 *
 * <p>
 * The {@code mongo_operation} unique key keeps a wrapper collection that
 * delegates to the driver's implementation from recording the same operation
 * twice. Not covered: the legacy {@code DBCollection} API, the reactive
 * streams driver, and the Atlas Search index methods.
 */
@Unique("mongo_operation")
public class Mongo {
  private static final TaggedLogger logger = AppMapConfig.getLogger(null);
  private static final Recorder recorder = Recorder.getInstance();

  static final String MONGO_COLLECTION = "com.mongodb.client.MongoCollection";

  /** Parameter names for each hooked method, in the order the Node agent uses. */
  static final Map<String, String[]> ARG_NAMES = new HashMap<String, String[]>();
  static {
    ARG_NAMES.put("insertOne", new String[] { "doc", "options" });
    ARG_NAMES.put("insertMany", new String[] { "docs", "options" });
    ARG_NAMES.put("bulkWrite", new String[] { "operations", "options" });
    ARG_NAMES.put("updateOne", new String[] { "filter", "update", "options" });
    ARG_NAMES.put("replaceOne", new String[] { "filter", "replacement", "options" });
    ARG_NAMES.put("updateMany", new String[] { "filter", "update", "options" });
    ARG_NAMES.put("deleteOne", new String[] { "filter", "options" });
    ARG_NAMES.put("deleteMany", new String[] { "filter", "options" });
    ARG_NAMES.put("renameCollection", new String[] { "newName", "options" });
    ARG_NAMES.put("drop", new String[] { "options" });
    ARG_NAMES.put("find", new String[] { "filter" });
    ARG_NAMES.put("createIndex", new String[] { "indexSpec", "options" });
    ARG_NAMES.put("createIndexes", new String[] { "indexSpecs", "options" });
    ARG_NAMES.put("dropIndex", new String[] { "indexName", "options" });
    ARG_NAMES.put("dropIndexes", new String[] { "options" });
    ARG_NAMES.put("listIndexes", new String[] {  });
    ARG_NAMES.put("estimatedDocumentCount", new String[] { "options" });
    ARG_NAMES.put("countDocuments", new String[] { "filter", "options" });
    ARG_NAMES.put("distinct", new String[] { "key", "filter" });
    ARG_NAMES.put("findOneAndDelete", new String[] { "filter", "options" });
    ARG_NAMES.put("findOneAndReplace", new String[] { "filter", "replacement", "options" });
    ARG_NAMES.put("findOneAndUpdate", new String[] { "filter", "update", "options" });
    ARG_NAMES.put("aggregate", new String[] { "pipeline" });
    ARG_NAMES.put("watch", new String[] { "pipeline" });
  }

  // What was recorded for the operation in progress on this thread. The
  // unique key above means at most one hooked operation is in progress per
  // thread, so a single slot is enough. It prevents a return event from being
  // emitted for a call that was never recorded (a recording that started
  // while the operation was running), which would unbalance the call stack.
  private enum Recorded {
    NOTHING, CALL, CALL_AND_QUERY
  }

  private static final ThreadLocal<Recorded> recorded = new ThreadLocal<Recorded>() {
    @Override
    protected Recorded initialValue() {
      return Recorded.NOTHING;
    }
  };

  static void onCall(Event event, Object self, Object[] args, String method) {
    recorded.set(Recorded.NOTHING);
    if (!recorder.hasActiveSession()) {
      return;
    }

    Operation op;
    try {
      op = Operation.of(self, method, args);
    } catch (Throwable e) {
      logger.debug(e, "failed to inspect mongo operation {}", method);
      op = null;
    }

    event.setParameters(null);
    if (op != null) {
      for (int i = 0; i < op.parameterNames.length; i++) {
        event.addParameter(args[i], op.parameterNames[i]);
      }
    } else if (args != null) {
      for (int i = 0; i < args.length; i++) {
        event.addParameter(args[i], "arg" + i);
      }
    }
    event.setReceiver(self);
    recorder.add(event);
    recorded.set(Recorded.CALL);

    if (op == null) {
      return;
    }
    String statement;
    try {
      statement = MongoQueryShape.formatStatement(op.collection, method, op.argNames, op.operationArgs,
          MongoDocumentConverter.forCollection(self));
    } catch (Throwable e) {
      logger.debug(e, "failed to format mongo statement for {}", method);
      statement = MongoQueryShape.formatCollection(op.collection) + "." + method + "(" + MongoQueryShape.PLACEHOLDER
          + ")";
    }
    Event query = Event.functionCallEvent();
    query.setSqlQuery(MongoQueryShape.DATABASE_TYPE, statement);
    recorder.add(query);
    recorded.set(Recorded.CALL_AND_QUERY);
  }

  static void onReturn(Event event, Object returnValue) {
    Recorded state = recorded.get();
    recorded.set(Recorded.NOTHING);
    if (state == Recorded.NOTHING) {
      return;
    }
    if (state == Recorded.CALL_AND_QUERY) {
      recorder.add(Event.functionReturnEvent());
    }
    event.setReturnValue(returnValue);
    recorder.add(event);
  }

  static void onException(Event event, Throwable exception) {
    Recorded state = recorded.get();
    recorded.set(Recorded.NOTHING);
    if (state == Recorded.NOTHING) {
      return;
    }
    if (state == Recorded.CALL_AND_QUERY) {
      Event queryReturn = Event.functionReturnEvent();
      queryReturn.setException(exception);
      recorder.add(queryReturn);
    }
    event.setException(exception);
    recorder.add(event);
  }

  /** A hooked call, split into the parts the statement and the parameters need. */
  static final class Operation {
    final String collection;
    /** Node-style names for every argument, session and result class included. */
    final String[] parameterNames;
    /** Node-style names for the arguments that take part in the statement. */
    final String[] argNames;
    /** The arguments that take part in the statement. */
    final Object[] operationArgs;

    private Operation(String collection, String[] parameterNames, String[] argNames, Object[] operationArgs) {
      this.collection = collection;
      this.parameterNames = parameterNames;
      this.argNames = argNames;
      this.operationArgs = operationArgs;
    }

    static Operation of(Object self, String method, Object[] args) {
      if (args == null) {
        args = new Object[0];
      }
      String[] names = ARG_NAMES.get(method);
      if (names == null) {
        names = new String[0];
      }

      int first = 0;
      int last = args.length;
      boolean session = args.length > 0 && MongoDocumentConverter.isClientSession(args[0]);
      if (session) {
        first = 1;
      }
      boolean resultClass = last > first && args[last - 1] instanceof Class;
      if (resultClass) {
        last--;
      }

      Object[] operationArgs = new Object[last - first];
      System.arraycopy(args, first, operationArgs, 0, operationArgs.length);

      String[] parameterNames = new String[args.length];
      int n = 0;
      if (session) {
        parameterNames[n++] = "session";
      }
      for (int i = 0; i < operationArgs.length; i++) {
        parameterNames[n++] = i < names.length ? names[i] : "arg" + i;
      }
      if (resultClass) {
        parameterNames[n++] = "resultClass";
      }

      return new Operation(MongoDocumentConverter.collectionName(self), parameterNames, names, operationArgs);
    }
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.insertOne
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void insertOne(Event event, Object self, Object[] args) {
    onCall(event, self, args, "insertOne");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void insertOne(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void insertOne(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.insertMany
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void insertMany(Event event, Object self, Object[] args) {
    onCall(event, self, args, "insertMany");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void insertMany(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void insertMany(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.bulkWrite
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void bulkWrite(Event event, Object self, Object[] args) {
    onCall(event, self, args, "bulkWrite");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void bulkWrite(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void bulkWrite(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.updateOne
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void updateOne(Event event, Object self, Object[] args) {
    onCall(event, self, args, "updateOne");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void updateOne(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void updateOne(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.replaceOne
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void replaceOne(Event event, Object self, Object[] args) {
    onCall(event, self, args, "replaceOne");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void replaceOne(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void replaceOne(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.updateMany
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void updateMany(Event event, Object self, Object[] args) {
    onCall(event, self, args, "updateMany");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void updateMany(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void updateMany(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.deleteOne
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void deleteOne(Event event, Object self, Object[] args) {
    onCall(event, self, args, "deleteOne");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void deleteOne(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void deleteOne(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.deleteMany
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void deleteMany(Event event, Object self, Object[] args) {
    onCall(event, self, args, "deleteMany");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void deleteMany(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void deleteMany(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.renameCollection
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void renameCollection(Event event, Object self, Object[] args) {
    onCall(event, self, args, "renameCollection");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void renameCollection(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void renameCollection(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.drop
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void drop(Event event, Object self, Object[] args) {
    onCall(event, self, args, "drop");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void drop(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void drop(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.find
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void find(Event event, Object self, Object[] args) {
    onCall(event, self, args, "find");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void find(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void find(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.createIndex
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void createIndex(Event event, Object self, Object[] args) {
    onCall(event, self, args, "createIndex");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void createIndex(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void createIndex(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.createIndexes
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void createIndexes(Event event, Object self, Object[] args) {
    onCall(event, self, args, "createIndexes");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void createIndexes(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void createIndexes(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.dropIndex
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void dropIndex(Event event, Object self, Object[] args) {
    onCall(event, self, args, "dropIndex");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void dropIndex(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void dropIndex(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.dropIndexes
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void dropIndexes(Event event, Object self, Object[] args) {
    onCall(event, self, args, "dropIndexes");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void dropIndexes(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void dropIndexes(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.listIndexes
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void listIndexes(Event event, Object self, Object[] args) {
    onCall(event, self, args, "listIndexes");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void listIndexes(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void listIndexes(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.estimatedDocumentCount
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void estimatedDocumentCount(Event event, Object self, Object[] args) {
    onCall(event, self, args, "estimatedDocumentCount");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void estimatedDocumentCount(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void estimatedDocumentCount(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.countDocuments
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void countDocuments(Event event, Object self, Object[] args) {
    onCall(event, self, args, "countDocuments");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void countDocuments(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void countDocuments(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.distinct
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void distinct(Event event, Object self, Object[] args) {
    onCall(event, self, args, "distinct");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void distinct(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void distinct(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.findOneAndDelete
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void findOneAndDelete(Event event, Object self, Object[] args) {
    onCall(event, self, args, "findOneAndDelete");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void findOneAndDelete(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void findOneAndDelete(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.findOneAndReplace
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void findOneAndReplace(Event event, Object self, Object[] args) {
    onCall(event, self, args, "findOneAndReplace");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void findOneAndReplace(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void findOneAndReplace(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.findOneAndUpdate
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void findOneAndUpdate(Event event, Object self, Object[] args) {
    onCall(event, self, args, "findOneAndUpdate");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void findOneAndUpdate(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void findOneAndUpdate(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.aggregate
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void aggregate(Event event, Object self, Object[] args) {
    onCall(event, self, args, "aggregate");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void aggregate(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void aggregate(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }

  // ---------------------------------------------------------------------------
  // MongoCollection.watch
  // ---------------------------------------------------------------------------

  @ArgumentArray
  @HookClass(MONGO_COLLECTION)
  public static void watch(Event event, Object self, Object[] args) {
    onCall(event, self, args, "watch");
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_RETURN)
  public static void watch(Event event, Object self, Object returnValue, Object[] args) {
    onReturn(event, returnValue);
  }

  @ArgumentArray
  @HookClass(value = MONGO_COLLECTION, methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void watch(Event event, Object self, Throwable exception, Object[] args) {
    onException(event, exception);
  }
}
