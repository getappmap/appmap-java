package com.appland.appmap.process.hooks;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.Collections;
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
@SuppressWarnings("unused")
public class SqlQuery {
  private static final Recorder recorder = Recorder.getInstance();
  private static final Map<Statement, String> statementSql = Collections.synchronizedMap(new WeakHashMap<>());

  public static void recordSql(Event event, String databaseType, String sql) {
    event.setSqlQuery(databaseType, sql);
    event.setParameters(null);
    recorder.add(event);
  }

  public static void recordSql(Event event, Connection c, String sql) {
    recordSql(event, getDbName(c), sql);
  }

  public static void recordSql(Event event, Statement s, String sql) {
    recordSql(event, getDbName(s), sql);
  }

  private static void recordSql(Event event, Statement s, Object[] args) {
    String sql = statementSql.get(s);
    if (sql == null && args.length > 0 && args[0] instanceof String) {
      sql = (String) args[0];
    }
    if (sql == null) sql = "[unknown sql]";
    recordSql(event, s, sql);
  }

  private static boolean isMock(Object o) {
    final Class<?> c = o.getClass();
    final Package p = c.getPackage();
    if (p == null) {
      // If there's no package info, it's not a Mockito object.
      return false;
    }

    return p.getName().startsWith("org.mockito");
  }

  private static String getDbName(Connection c) {
    String dbname = "";
    if (c == null) {
      return dbname;
    }

    try {
      DatabaseMetaData metadata;
      if (isMock(c) || isMock(metadata = c.getMetaData())) {
        return "[mocked]";
      }

      dbname = metadata.getDatabaseProductName();
    } catch (Throwable e) {
      Logger.println("WARNING, failed to get database name");
      e.printStackTrace(System.err);
    }
    return dbname;
  }

  private static String getDbName(Statement s) {
    String dbname = "";
    if (s == null) {
      return dbname;
    }

    try {
      if (isMock(s)) {
        return "[mocked]";
      }

      dbname = getDbName(s.getConnection());
    } catch (Throwable e) {
      Logger.println("WARNING, failed to get statement's connection");
      e.printStackTrace(System.err);
    }
    return dbname;
  }

  // ================================================================================================
  // nativeSQL
  // ================================================================================================

  @HookClass("java.sql.Connection")
  public static void nativeSQL(Event event, Connection c, String sql) {
    recordSql(event, c, sql);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void nativeSQL(Event event, Connection c, Object returnValue, String sql) {
    recorder.add(event);
  }

  @ArgumentArray
  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void nativeSQL(Event event, Connection c, Throwable exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
  }

  // ================================================================================================
  // addBatch
  // ================================================================================================

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void addBatch(Event event, Statement s, String sql) {
    recordSql(event, s, sql);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void addBatch(Event event, Statement s, Object returnValue, String sql) {
    recorder.add(event);
  }

  @ArgumentArray
  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void addBatch(Event event, Statement s, Throwable exception, Object[] args) {
    event.setException(exception);
    recorder.add(event);
  }

  // ================================================================================================
  // execute
  // ================================================================================================

  @ArgumentArray
  @HookClass("java.sql.Statement")
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
  // executeQuery
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
  // executeUpdate
  // ================================================================================================

  @ArgumentArray
  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, Statement s, Object[] args) {
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
  // prepareCall
  // ================================================================================================

  @ArgumentArray
  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareCall(Event event, Connection c, Object returnValue, Object[] args) {
    if (returnValue instanceof Statement) {
      String sql = "[unknown sql]";
      if (args.length > 0 && args[0] instanceof String) sql = (String) args[0];
      statementSql.put((Statement) returnValue, sql);
    }
  }

  // ================================================================================================
  // prepareStatement
  // ================================================================================================

  @ArgumentArray
  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareStatement(Event event, Connection c, Object returnValue, Object[] args) {
    if (returnValue instanceof Statement) {
      String sql = "[unknown sql]";
      if (args.length > 0 && args[0] instanceof String) sql = (String) args[0];
      statementSql.put((Statement) returnValue, sql);
    }
  }
}