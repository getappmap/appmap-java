package com.example.accessingdatajpa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class PureJDBCTests {

  private Connection connection;
  private boolean isOracle;

  @BeforeEach
  public void setUp() throws SQLException {
    String oracleUrl = System.getenv("ORACLE_URL");

    // Determine which database to use
    if (oracleUrl != null && !oracleUrl.isEmpty()) {
      // Use Oracle
      isOracle = true;
      String oracleUsername = System.getenv("ORACLE_USERNAME");
      if (oracleUsername == null) {
        oracleUsername = "system";
      }
      String oraclePassword = System.getenv("ORACLE_PASSWORD");
      if (oraclePassword == null) {
        oraclePassword = "oracle";
      }
      connection = DriverManager.getConnection(oracleUrl, oracleUsername, oraclePassword);
    } else {
      // Use H2
      isOracle = false;
      connection = DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "");
    }

    // Create table with database-specific DDL
    try (Statement statement = connection.createStatement()) {
      String createTableSql;
      if (isOracle) {
        createTableSql = "CREATE TABLE customer (id NUMBER(19,0) NOT NULL, first_name VARCHAR2(255 CHAR), last_name VARCHAR2(255 CHAR), PRIMARY KEY (id))";
      } else {
        createTableSql = "CREATE TABLE customer (id BIGINT NOT NULL, first_name VARCHAR(255), last_name VARCHAR(255), PRIMARY KEY (id))";
      }
      statement.execute("DROP TABLE IF EXISTS customer");
      statement.execute(createTableSql);

      // Create a test procedure for CallableStatement tests
      if (isOracle) {
        statement.execute("CREATE OR REPLACE PROCEDURE test_proc(p1 IN VARCHAR2, p2 IN VARCHAR2) AS BEGIN NULL; END;");
      } else {
        statement.execute("CREATE ALIAS IF NOT EXISTS test_proc FOR \"java.lang.System.setProperty\"");
      }
    }
  }

  @AfterEach
  public void tearDown() throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE customer");
    }
    connection.close();
  }

  @Test
  void testStatementExecute() throws Exception {
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("INSERT INTO customer (id, first_name, last_name) VALUES (1, 'A', 'B')");
      stmt.execute("UPDATE customer SET first_name = 'C' WHERE id = 1", Statement.NO_GENERATED_KEYS);
      stmt.execute("DELETE FROM customer WHERE id = 1", new int[] { 1 });
      stmt.execute("INSERT INTO customer (id, first_name, last_name) VALUES (2, 'X', 'Y')", new String[] { "id" });
    }
  }

  // Note this test should generate no SQL in the AppMap - this was a bug in the
  // agent
  @Test
  void testNativeSQL() throws Exception {
    // Test nativeSQL method which converts SQL to the database's native grammar
    String sql = "SELECT * FROM customer WHERE id = ?";
    String nativeSql = connection.nativeSQL(sql);

    assertTrue(nativeSql != null);
    assertTrue(nativeSql.contains("customer"));
  }

  @Test
  void testBatch() throws Exception {
    try (Statement stmt = connection.createStatement()) {
      stmt.addBatch("INSERT INTO customer (id, first_name, last_name) VALUES (3, 'E', 'F')");
      stmt.addBatch("INSERT INTO customer (id, first_name, last_name) VALUES (4, 'G', 'H')");
      stmt.clearBatch();

      stmt.addBatch("INSERT INTO customer (id, first_name, last_name) VALUES (5, 'I', 'J')");
      stmt.addBatch("INSERT INTO customer (id, first_name, last_name) VALUES (6, 'K', 'L')");
      stmt.executeBatch();

      stmt.addBatch("INSERT INTO customer (id, first_name, last_name) VALUES (7, 'M', 'N')");
      stmt.addBatch("INSERT INTO customer (id, first_name, last_name) VALUES (8, 'O', 'P')");
      stmt.executeLargeBatch();

      // This should generate empty SQL in the AppMap
      stmt.executeLargeBatch();

      // Let's try invalid sequel to cause an exception
      try {
        stmt.addBatch("SELECT * FROM customer WHERE batch error = ?");
        stmt.executeBatch();
      } catch (SQLException e) {
        // expected
      }
    }
  }

  @Test
  void testPreparedStatementBatch() throws Exception {
    String sql = "INSERT INTO customer (id, first_name, last_name) VALUES (?, ?, ?)";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
      pstmt.setLong(1, 9);
      pstmt.setString(2, "Q");
      pstmt.setString(3, "R");
      pstmt.addBatch();
      pstmt.clearBatch();

      pstmt.setLong(1, 10);
      pstmt.setString(2, "S");
      pstmt.setString(3, "T");
      pstmt.addBatch();
      pstmt.setLong(1, 11);
      pstmt.setString(2, "U");
      pstmt.setString(3, "V");
      pstmt.addBatch();
      pstmt.executeBatch();

      pstmt.setLong(1, 12);
      pstmt.setString(2, "W");
      pstmt.setString(3, "X");
      pstmt.addBatch();
      pstmt.setLong(1, 13);
      pstmt.setString(2, "Y");
      pstmt.setString(3, "Z");
      pstmt.addBatch();
      pstmt.executeLargeBatch();
    }
  }

  @Test
  void testCallableStatement() throws Exception {
    // Each call uses slightly different SQL to ensure unique identification in
    // AppMap
    String sql1 = "{call test_proc(?, ?)} -- call 1";
    String sql2 = "{call test_proc(?, ?)} -- call 2";
    String sql3 = "{call test_proc(?, ?)} -- call 3";

    // Test various prepareCall overloads and execute multiple times
    try (CallableStatement cstmt = connection.prepareCall(sql1)) {
      cstmt.setString(1, "key1.1");
      cstmt.setString(2, "val1.1");
      cstmt.execute();
      cstmt.setString(1, "key1.2");
      cstmt.setString(2, "val1.2");
      cstmt.execute();
    }

    try (CallableStatement cstmt = connection.prepareCall(sql2, ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY)) {
      cstmt.setString(1, "key2.1");
      cstmt.setString(2, "val2.1");
      cstmt.execute();
      cstmt.setString(1, "key2.2");
      cstmt.setString(2, "val2.2");
      cstmt.execute();
    }

    try (CallableStatement cstmt = connection.prepareCall(sql3, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
        ResultSet.HOLD_CURSORS_OVER_COMMIT)) {
      cstmt.setString(1, "key3.1");
      cstmt.setString(2, "val3.1");
      cstmt.execute();
      cstmt.setString(1, "key3.2");
      cstmt.setString(2, "val3.2");
      cstmt.execute();
    }
  }

  @Test
  void testExecuteUpdate() throws Exception {
    try (Statement stmt = connection.createStatement()) {
      stmt.executeUpdate("INSERT INTO customer (id, first_name, last_name) VALUES (20, 'A', 'B')");
      stmt.executeUpdate("UPDATE customer SET first_name = 'C' WHERE id = 20", Statement.NO_GENERATED_KEYS);
      stmt.executeUpdate("DELETE FROM customer WHERE id = 20", new int[] { 1 });
      stmt.executeUpdate("INSERT INTO customer (id, first_name, last_name) VALUES (21, 'D', 'E')",
          new String[] { "id" });

      // Test executeLargeUpdate overloads
      stmt.executeLargeUpdate("UPDATE customer SET first_name = 'F' WHERE id = 21");
      stmt.executeLargeUpdate("UPDATE customer SET first_name = 'G' WHERE id = 21", Statement.NO_GENERATED_KEYS);
      stmt.executeLargeUpdate("UPDATE customer SET first_name = 'H' WHERE id = 21", new int[] { 1 });
      stmt.executeLargeUpdate("UPDATE customer SET first_name = 'I' WHERE id = 21", new String[] { "id" });
    }
  }

  @Test
  void testExecuteQuery() throws Exception {
    try (Statement stmt = connection.createStatement()) {
      try (ResultSet rs = stmt.executeQuery("SELECT * FROM customer")) {
        while (rs.next()) {
        }
      }
    }
  }

  @Test
  void testPrepareStatement() throws Exception {
    // Unique SQL for each overload
    String sql1 = "SELECT * FROM customer WHERE id = ? -- op 1";
    String sql2 = "SELECT first_name FROM customer WHERE id = ? -- op 2";
    String sql3 = "UPDATE customer SET last_name = ? WHERE id = ? -- op 3";
    String sql4 = "UPDATE customer SET first_name = ? WHERE id = ? -- op 4";
    String sql5 = "UPDATE customer SET last_name = ? WHERE id = ? -- op 5";
    String sql6 = "SELECT count(*) FROM customer WHERE id = ? -- op 6";

    try (PreparedStatement pstmt = connection.prepareStatement(sql1)) {
      pstmt.setLong(1, 1);
      pstmt.execute();
      pstmt.setLong(1, 2);
      pstmt.execute();
    }
    try (PreparedStatement pstmt = connection.prepareStatement(sql2, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.setLong(1, 1);
      try (ResultSet rs = pstmt.executeQuery()) {
      }
      pstmt.setLong(1, 2);
      try (ResultSet rs = pstmt.executeQuery()) {
      }
    }
    try (PreparedStatement pstmt = connection.prepareStatement(sql3, new int[] { 1 })) {
      pstmt.setString(1, "LastName3.1");
      pstmt.setLong(2, 1);
      pstmt.executeUpdate();
      pstmt.setString(1, "LastName3.2");
      pstmt.setLong(2, 1);
      pstmt.executeUpdate();
    }
    try (PreparedStatement pstmt = connection.prepareStatement(sql4, ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY)) {
      pstmt.setString(1, "Name1");
      pstmt.setLong(2, 21);
      pstmt.executeLargeUpdate();
      pstmt.setString(1, "Name2");
      pstmt.setLong(2, 21);
      pstmt.executeLargeUpdate();
    }
    try (PreparedStatement pstmt = connection.prepareStatement(sql5, ResultSet.TYPE_FORWARD_ONLY,
        ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT)) {
      pstmt.setString(1, "LName1");
      pstmt.setLong(2, 21);
      pstmt.executeUpdate();
      pstmt.setString(1, "LName2");
      pstmt.setLong(2, 21);
      pstmt.executeUpdate();
    }
    try (PreparedStatement pstmt = connection.prepareStatement(sql6, new String[] { "id" })) {
      pstmt.setLong(1, 1);
      pstmt.execute();
      pstmt.setLong(1, 2);
      pstmt.execute();
    }
  }

  @Test
  void testExceptions() throws Exception {
    try (Statement stmt = connection.createStatement()) {
      try {
        stmt.execute("SELECT * FROM non_existent_table");
      } catch (SQLException e) {
        // Expected
      }
    }

    try {
      // note this will fail to prepare on h2 but only fail on execution on oracle
      try (PreparedStatement stmt = connection.prepareStatement("INVALID SQL")) {
        stmt.execute();
      }
    } catch (SQLException e) {
      // Expected
    }
  }
}
