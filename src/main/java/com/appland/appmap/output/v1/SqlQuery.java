package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * Represents a snapshot of a SQL query issued at runtime. Embedded within an {@link Event}.
 * @see Event
 * @see <a href="https://github.com/applandinc/appmap#sql-query-attributes">GitHub: AppMap - SQL query attributes</a>
 */
public class SqlQuery {
  public String sql;

  @JSONField(name = "database_type")
  public String databaseType;

  @JSONField(name = "explain_sql")
  public String explainSql;

  @JSONField(name = "server_version")
  public String serverVersion;

  /**
   * Sets the "sql" field.
   * @param sql A SQL query string
   * @return {@code this}
   */
  public SqlQuery setSql(String sql) {
    this.sql = sql;
    return this;
  }

  /**
   * Sets the "database_type" field.
   * @param databaseType A database type
   * @return {@code this}
   */
  public SqlQuery setDatabaseType(String databaseType) {
    this.databaseType = databaseType;
    return this;
  }
}
