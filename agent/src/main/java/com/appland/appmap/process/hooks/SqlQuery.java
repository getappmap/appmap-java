package com.appland.appmap.process.hooks;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.transform.annotations.Unique;
import com.appland.appmap.util.Logger;

/**
 * Hooks to capture {@code sql_query} data from classes included in
 * configuration.
 */
@Unique("sql_query")
public class SqlQuery {
  private static final Recorder recorder = Recorder.getInstance();

  public static void recordSql(Event event, String databaseType, String sql) {
    event.setSqlQuery(databaseType, sql);
    event.setParameters(null);
    recorder.add(event);
  }

  private static final Map<Object, String> databases = Collections.synchronizedMap(new WeakHashMap<Object, String>());

  private static String getDbName(Connection c) {
    if (c == null) {
      return null;
    }
    if (databases.containsKey(c)) {
      return databases.get(c);
    }

    String dbname = null;
    try {
      dbname = c.getMetaData().getDatabaseProductName();
    } catch (Throwable e) {
      Logger.println("WARNING, failed to get database name");
      e.printStackTrace(System.err);
      // fall through and put null to ensure we don't try again
    }
    databases.put(c, dbname);
    return dbname;
  }

  private static String getDbName(Statement s) {
    if (s == null) {
      return null;
    }
    if (databases.containsKey(s)) {
      return databases.get(s);
    }

    String dbname = null;
    try {
      dbname = getDbName(s.getConnection());
    } catch (Throwable e) {
      Logger.println("WARNING, failed to get statement's connection");
      e.printStackTrace(System.err);
      // fall through and put null to ensure we don't try again
    }
    databases.put(s, dbname);
    return dbname;
  }

  public static void recordSql(Event event, Statement s, String sql) {
    recordSql(event, getDbName(s), sql);
  }

  public static void recordSql(Event event, Statement s, Object args[]) {
    recordSql(event, getDbName(s), getSql(s, args));
  }

  private static Map<Object, String> statements = Collections.synchronizedMap(new WeakHashMap<Object, String>());

  /**
   * Get the SQL string based on the arguments or the prepared statement.
   *
   * If the first argument is a string, it is returned.
   * If the statement is a prepared statement, the SQL string is returned.
   * Otherwise, the last resort is to return "-- [unknown sql]".
   *
   * @param s    The statement
   * @param args The arguments
   * @return The SQL string
   */
  private static String getSql(Statement s, Object args[]) {
    if (args.length > 0 && args[0] instanceof String) {
      return (String) args[0];
    }
    String sql = statements.get(s);
    if (sql == null) {
      // last resort, shouldn't happen
      return "-- [unknown sql]";
    }
    return sql;
  }

  // ================================================================================================
  // Preparing calls and statements
  // ================================================================================================

  @ArgumentArray
  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareCall(Event event, Connection c, Object returnValue, Object[] args) {
    databases.put(returnValue, getDbName(c));
    if (args.length > 0 && args[0] instanceof String) {
      statements.put(returnValue, (String) args[0]);
    }
  }

  @ArgumentArray
  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareStatement(Event event, Connection c, Object returnValue, Object[] args) {
    databases.put(returnValue, getDbName(c));
    if (args.length > 0 && args[0] instanceof String) {
      statements.put(returnValue, (String) args[0]);
    }
  }

  // ================================================================================================
  // Batch manipulation
  // ================================================================================================

  private static final Map<Object, List<String>> batchStatements = new WeakHashMap<>();

  /**
   * Pop the batch statements for the given statement.
   * The batch statements are joined with ";\n".
   *
   * Note that this will remove the batch statements from the map.
   *
   * @param s The statement
   * @return The batch statements
   */
  private static String popBatchStatements(Statement s) {
    synchronized (batchStatements) {
      List<String> statements = batchStatements.remove(s);
      if (statements == null) {
        return "";
      }
      return String.join(";\n", statements);
    }
  }

  @ArgumentArray
  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void addBatch(Event event, Statement s, Object returnValue, Object[] args) {
    String sql = getSql(s, args);
    synchronized (batchStatements) {
      batchStatements.computeIfAbsent(s, k -> new ArrayList<>()).add(sql);
    }
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void clearBatch(Event event, Statement s, Object returnValue) {
    synchronized (batchStatements) {
      batchStatements.remove(s);
    }
  }

  // ================================================================================================
  // Statement.executeBatch
  // ================================================================================================

  @HookClass(value = "java.sql.Statement")
  public static void executeBatch(Event event, Statement s) {
    recordSql(event, s, popBatchStatements(s));
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void executeBatch(Event event, Statement s, Object returnValue) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void executeBatch(Event event, Statement s, Throwable exception) {
    event.setException(exception);
    recorder.add(event);
  }

  // ================================================================================================
  // Statement.executeLargeBatch
  // ================================================================================================

  @HookClass(value = "java.sql.Statement")
  public static void executeLargeBatch(Event event, Statement s) {
    recordSql(event, s, popBatchStatements(s));
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void executeLargeBatch(Event event, Statement s, Object returnValue) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void executeLargeBatch(Event event, Statement s, Throwable exception) {
    event.setException(exception);
    recorder.add(event);
  }

  // ================================================================================================
  // Statement.execute
  // ================================================================================================

  @HookClass("java.sql.Statement")
  @ArgumentArray
  public static void execute(Event event, Statement s, Object[] args) {
    recordSql(event, s, args);
  }

  @ArgumentArray
  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void execute(Event event, Statement s, Object returnValue, Object[] args) {
    recorder.add(event);
  }

  @ArgumentArray
  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void execute(Event event, Statement s, Throwable exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
  }

  // ================================================================================================
  // Statement.executeQuery
  // ================================================================================================

  @ArgumentArray
  @HookClass("java.sql.Statement")
  public static void executeQuery(Event event, Statement s, Object[] args) {
    recordSql(event, s, args);
  }

  @ArgumentArray
  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void executeQuery(Event event, Statement s, Object returnValue, Object[] args) {
    recorder.add(event);
  }

  @ArgumentArray
  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void executeQuery(Event event, Statement s, Throwable exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
  }

  // ================================================================================================
  // Statement.executeUpdate
  // ================================================================================================

  @ArgumentArray
  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, Statement s, Object args[]) {
    recordSql(event, s, args);
  }

  @ArgumentArray
  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void executeUpdate(Event event, Statement s, Object returnValue, Object[] args) {
    recorder.add(event);
  }

  @ArgumentArray
  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void executeUpdate(Event event, Statement s, Throwable exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
  }

  // ================================================================================================
  // Statement.executeLargeUpdate
  // ================================================================================================

  @ArgumentArray
  @HookClass("java.sql.Statement")
  public static void executeLargeUpdate(Event event, Statement s, Object args[]) {
    recordSql(event, s, args);
  }

  @ArgumentArray
  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void executeLargeUpdate(Event event, Statement s, Object returnValue, Object[] args) {
    recorder.add(event);
  }

  @ArgumentArray
  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void executeLargeUpdate(Event event, Statement s, Throwable exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
  }
}
