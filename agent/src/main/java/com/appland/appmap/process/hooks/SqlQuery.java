package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.transform.annotations.Unique;
import com.appland.appmap.util.Logger;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Hooks to capture {@code sql_query} data from classes included in configuration.
 */
@Unique("sql_query")
public class SqlQuery {
  private static final Recorder recorder = Recorder.getInstance();
  
  //================================================================================================
  // Calls
  //================================================================================================

  public static void recordSql(Event event, String databaseType, String sql) {
    event.setSqlQuery(databaseType, sql);
    event.setParameters(null);
    recorder.add(event);
  }

  private static boolean isMock(Object o) {
    return o.getClass().getPackage().getName().startsWith("org.mockito");
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
    }
    catch (SQLException e) {
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
      if(isMock(s)) {
        return "[mocked]";
      }

      dbname = getDbName(s.getConnection());
    }
    catch (SQLException e) {
      Logger.println("WARNING, failed to get statement's connection");
      e.printStackTrace(System.err);
    }
    return dbname;
  }
    
  public static void recordSql(Event event, Connection c, String sql) {
    recordSql(event, getDbName(c), sql);
  }

  public static void recordSql(Event event, Statement s, String sql) {
    recordSql(event, getDbName(s), sql);
  }
  
  @HookClass("java.sql.Connection")
  public static void nativeSQL(Event event, Connection c, String sql) {
    recordSql(event, c, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareCall(Event event, Connection c, String sql) {
    recordSql(event, c, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareCall(Event event, Connection c, String sql, int resultSetType, int resultSetConcurrency) {
    recordSql(event, c, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareCall(Event event, Connection c, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
    recordSql(event, c, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, Connection c, String sql) {
    recordSql(event, c, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, Connection c, String sql, int autoGeneratedKeys) {
    recordSql(event, c, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, Connection c, String sql, int[] columnIndexes) {
    recordSql(event, c, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, Connection c, String sql, int resultSetType, int resultSetConcurrency) {
    recordSql(event, c, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, Connection c, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
    recordSql(event, c, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, Connection c, String sql, String[] columnNames) {
    recordSql(event, c, sql);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void addBatch(Event event, Statement s, String sql) {
    recordSql(event, s, sql);
  }

  @HookClass("java.sql.Statement")
  public static void execute(Event event, Statement s, String sql) {
    recordSql(event, s, sql);
  }

  @HookClass("java.sql.Statement")
  public static void execute(Event event, Statement s, String sql, int autoGeneratedKeys) {
    recordSql(event, s, sql);
  }

  @HookClass("java.sql.Statement")
  public static void execute(Event event, Statement s, String sql, int[] columnIndexes) {
    recordSql(event, s, sql);
  }

  @HookClass("java.sql.Statement")
  public static void execute(Event event, Statement s, String sql, String[] columnNames) {
    recordSql(event, s, sql);
  }

  @HookClass("java.sql.Statement")
  public static void executeQuery(Event event, Statement s, String sql) {
    recordSql(event, s, sql);
  }

  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, Statement s, String sql) {
    recordSql(event, s, sql);
  }

  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, Statement s, String sql, int autoGeneratedKeys) {
    recordSql(event, s, sql);
  }

  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, Statement s, String sql, int[] columnIndexes) {
    recordSql(event, s, sql);
  }

  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, Statement s, String sql, String[] columnNames) {
    recordSql(event, s, sql);
  }

  //================================================================================================
  // Returns
  //================================================================================================

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void nativeSQL(Event event, Connection c, Object returnValue, String sql) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareCall(Event event, Connection c, Object returnValue, String sql) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareCall(Event event, Connection c, Object returnValue, String sql, int resultSetType, int resultSetConcurrency) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareCall(Event event, Connection c, Object returnValue, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareStatement(Event event, Connection c, Object returnValue, String sql) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareStatement(Event event, Connection c, Object returnValue, String sql, int autoGeneratedKeys) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareStatement(Event event, Connection c, Object returnValue, String sql, int[] columnIndexes) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareStatement(Event event, Connection c, Object returnValue, String sql, int resultSetType, int resultSetConcurrency) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareStatement(Event event, Connection c, Object returnValue, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_RETURN)
  public static void prepareStatement(Event event, Connection c, Object returnValue, String sql, String[] columnNames) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void addBatch(Event event, Statement s, Object returnValue, String sql) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void execute(Event event, Statement s, Object returnValue, String sql) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void execute(Event event, Statement s, Object returnValue, String sql, int autoGeneratedKeys) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void execute(Event event, Statement s, Object returnValue, String sql, int[] columnIndexes) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void execute(Event event, Statement s, Object returnValue, String sql, String[] columnNames) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void executeQuery(Event event, Statement s, Object returnValue, String sql) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void executeUpdate(Event event, Statement s, Object returnValue, String sql) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void executeUpdate(Event event, Statement s, Object returnValue, String sql, int autoGeneratedKeys) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void executeUpdate(Event event, Statement s, Object returnValue, String sql, int[] columnIndexes) {
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_RETURN)
  public static void executeUpdate(Event event, Statement s, Object returnValue, String sql, String[] columnNames) {
    recorder.add(event);
  }

  //================================================================================================
  // Exceptions
  //================================================================================================

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void nativeSQL(Event event, Connection c, Exception exception, String sql) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void prepareCall(Event event, Connection c, Exception exception, String sql) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void prepareCall(Event event, Connection c, Exception exception, String sql, int resultSetType, int resultSetConcurrency) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void prepareCall(Event event, Connection c, Exception exception, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void prepareStatement(Event event, Connection c, Exception exception, String sql) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void prepareStatement(Event event, Connection c, Exception exception, String sql, int autoGeneratedKeys) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void prepareStatement(Event event, Connection c, Exception exception, String sql, int[] columnIndexes) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void prepareStatement(Event event, Connection c, Exception exception, String sql, int resultSetType, int resultSetConcurrency) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void prepareStatement(Event event, Connection c, Exception exception, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Connection", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void prepareStatement(Event event, Connection c, Exception exception, String sql, String[] columnNames) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void addBatch(Event event, Statement s, Exception exception, String sql) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void execute(Event event, Statement s, Exception exception, String sql) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void execute(Event event, Statement s, Exception exception, String sql, int autoGeneratedKeys) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void execute(Event event, Statement s, Exception exception, String sql, int[] columnIndexes) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void execute(Event event, Statement s, Exception exception, String sql, String[] columnNames) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void executeQuery(Event event, Statement s, Exception exception, String sql) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void executeUpdate(Event event, Statement s, Exception exception, String sql) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void executeUpdate(Event event, Statement s, Exception exception, String sql, int autoGeneratedKeys) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void executeUpdate(Event event, Statement s, Exception exception, String sql, int[] columnIndexes) {
    event.setException(exception);
    recorder.add(event);
  }

  @HookClass(value = "java.sql.Statement", methodEvent = MethodEvent.METHOD_EXCEPTION)
  public static void executeUpdate(Event event, Statement s, Exception exception, String sql, String[] columnNames) {
    event.setException(exception);
    recorder.add(event);
  }
}