package com.appland.appmap.process.hooks;

import com.appland.appmap.output.v1.Event;
import com.appland.appmap.process.conditions.ConfigCondition;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.transform.annotations.MethodEvent;
import com.appland.appmap.transform.annotations.Unique;
import com.appland.appmap.transform.annotations.ArgumentArray;
import com.appland.appmap.transform.annotations.CallbackOn;
import com.appland.appmap.transform.annotations.ExcludeReceiver;
import com.appland.appmap.transform.annotations.Hook;
import com.appland.appmap.transform.annotations.HookClass;
import com.appland.appmap.transform.annotations.HookCondition;

/**
 * Hooks to capture {@code sql_query} data from classes included in configuration.
 */
@Unique("sql_query")
@ExcludeReceiver
public class SqlQuery {
  private static final Recorder recorder = Recorder.getInstance();

  //================================================================================================
  // Calls
  //================================================================================================

  public static void recordSql(Event event, String sql) {
    event.setParameters(null);
    event.setSqlQuery(sql);
    recorder.add(event);
  }

  @HookClass("java.sql.Connection")
  public static void nativeSQL(Event event, String sql) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareCall(Event event, String sql) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareCall(Event event, String sql, int resultSetType, int resultSetConcurrency) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Connection")
  
  public static void prepareCall(Event event, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, String sql) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, String sql, int autoGeneratedKeys) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, String sql, int[] columnIndexes) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, String sql, int resultSetType, int resultSetConcurrency) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, String sql, String[] columnNames) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Statement")
  public static void addBatch(Event event, String sql) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Statement")
  public static void execute(Event event, String sql) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Statement")
  public static void execute(Event event, String sql, int autoGeneratedKeys) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Statement")
  public static void execute(Event event, String sql, int[] columnIndexes) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Statement")
  public static void execute(Event event, String sql, String[] columnNames) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Statement")
  public static void executeQuery(Event event, String sql) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, String sql) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, String sql, int autoGeneratedKeys) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, String sql, int[] columnIndexes) {
    recordSql(event, sql);
  }

  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, String sql, String[] columnNames) {
    recordSql(event, sql);
  }

  //================================================================================================
  // Returns
  //================================================================================================

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Connection")
  public static void nativeSQL(Event event, Object returnValue, String sql) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Connection")
  public static void prepareCall(Event event, Object returnValue, String sql) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Connection")
  public static void prepareCall(Event event, Object returnValue, String sql, int resultSetType, int resultSetConcurrency) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Connection")
  public static void prepareCall(Event event, Object returnValue, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, Object returnValue, String sql) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, Object returnValue, String sql, int autoGeneratedKeys) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, Object returnValue, String sql, int[] columnIndexes) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, Object returnValue, String sql, int resultSetType, int resultSetConcurrency) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, Object returnValue, String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Connection")
  public static void prepareStatement(Event event, Object returnValue, String sql, String[] columnNames) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Statement")
  public static void addBatch(Event event, Object returnValue, String sql) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Statement")
  public static void execute(Event event, Object returnValue, String sql) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Statement")
  public static void execute(Event event, Object returnValue, String sql, int autoGeneratedKeys) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Statement")
  public static void execute(Event event, Object returnValue, String sql, int[] columnIndexes) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Statement")
  public static void execute(Event event, Object returnValue, String sql, String[] columnNames) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Statement")
  public static void executeQuery(Event event, Object returnValue, String sql) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, Object returnValue, String sql) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, Object returnValue, String sql, int autoGeneratedKeys) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, Object returnValue, String sql, int[] columnIndexes) {
    recorder.add(event);
  }

  @CallbackOn(MethodEvent.METHOD_RETURN)
  @HookClass("java.sql.Statement")
  public static void executeUpdate(Event event, Object returnValue, String sql, String[] columnNames) {
    recorder.add(event);
  }
}