package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;

public class SqlQuery {
  public String sql;

  @JSONField(name = "database_type")
  public String databaseType;

  @JSONField(name = "explain_sql")
  public String explainSql;

  @JSONField(name = "server_version")
  public String serverVersion;

  public SqlQuery setSql(String sql) {
    this.sql = sql;
    return this;
  }

  public SqlQuery setDatabaseType(String databaseType) {
    this.databaseType = databaseType;
    return this;
  }
}