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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@EnabledIfEnvironmentVariable(named = "ORACLE_URL", matches = ".*")
@Execution(ExecutionMode.SAME_THREAD)
public class PureJDBCTests {

  private Connection connection;

  @BeforeEach
  public void setUp() throws SQLException {
    String oracleUrl = System.getenv("ORACLE_URL");
    String oracleUsername = System.getenv("ORACLE_USERNAME");
    if (oracleUsername == null) {
      oracleUsername = "system";
    }
    String oraclePassword = System.getenv("ORACLE_PASSWORD");
    if (oraclePassword == null) {
      oraclePassword = "oracle";
    }

    connection = DriverManager.getConnection(oracleUrl, oracleUsername, oraclePassword);

    try (Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE customer (id NUMBER(19,0) NOT NULL, first_name VARCHAR2(255 CHAR), last_name VARCHAR2(255 CHAR), PRIMARY KEY (id))");
    }
  }

  @AfterEach
  public void tearDown() throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE customer");
    }
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }
  }

  @Test
  public void testJDBC() throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM DUAL")) {
      statement.execute();
      statement.execute();
    }
    try (CallableStatement statement = connection.prepareCall("begin null; end; --foobar")) {
      statement.execute();
    }
  }

  @Test
  public void testPreparedStatement() throws SQLException {
    try (
        PreparedStatement preparedStatement = connection
            .prepareStatement("SELECT * FROM DUAL WHERE DUMMY = ?")) {
      preparedStatement.setString(1, "X");
      preparedStatement.execute();
    }
  }

  @Test
  public void testCallableStatement() throws SQLException {
    try (
        CallableStatement callableStatement = connection.prepareCall("begin :1 := 1; end;")) {
      callableStatement.registerOutParameter(1, java.sql.Types.INTEGER);
      callableStatement.execute();
    }
  }

  @Test
  public void testStatementBatch() throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.addBatch(
          "INSERT INTO customer (id, first_name, last_name) VALUES (1000, 'John', 'Doe')");
      statement.addBatch(
          "INSERT INTO customer (id, first_name, last_name) VALUES (1001, 'Jane', 'Doe')");
      int[] updateCounts = statement.executeBatch();
      assertArrayEquals(new int[] {1, 1}, updateCounts);

      statement.clearBatch();
      statement.addBatch(
          "INSERT INTO customer (id, first_name, last_name) VALUES (1002, 'Foo', 'Bar')");
      updateCounts = statement.executeBatch();
      assertArrayEquals(new int[] {1}, updateCounts);
    }
  }

  @Test
  public void testPreparedStatementBatch() throws SQLException {
    try (PreparedStatement statement = connection
        .prepareStatement("INSERT INTO customer (id, first_name, last_name) VALUES (?, ?, ?)")) {
      statement.setLong(1, 2000);
      statement.setString(2, "John");
      statement.setString(3, "Smith");
      statement.addBatch();

      statement.setLong(1, 2001);
      statement.setString(2, "Jane");
      statement.setString(3, "Smith");
      statement.addBatch();

      int[] updateCounts = statement.executeBatch();
      assertArrayEquals(new int[] {1, 1}, updateCounts);
    }
  }

  @Test
  public void testPreparedStatementLargeBatch() throws SQLException {
    try (PreparedStatement statement = connection
        .prepareStatement("INSERT INTO customer (id, first_name, last_name) VALUES (?, ?, ?)")) {
      statement.setLong(1, 3000);
      statement.setString(2, "Big");
      statement.setString(3, "Batch");
      statement.addBatch();

      long[] updateCounts = statement.executeLargeBatch();
      assertArrayEquals(new long[] {1}, updateCounts);
    }
  }

  @Test
  public void testUpdates() throws SQLException {
    try (PreparedStatement statement = connection
        .prepareStatement("INSERT INTO customer (id, first_name, last_name) VALUES (?, ?, ?)")) {
      statement.setLong(1, 4000);
      statement.setString(2, "Test");
      statement.setString(3, "User1");
      int updateCount = statement.executeUpdate();
      assertEquals(1, updateCount);
    }

    try (PreparedStatement statement = connection
        .prepareStatement("UPDATE customer SET last_name = ? WHERE first_name = ?")) {
      statement.setString(1, "User2");
      statement.setString(2, "Test");
      long largeUpdateCount = statement.executeLargeUpdate();
      assertEquals(1L, largeUpdateCount);
    }
  }

  @Test
  public void testExecute() throws SQLException {
    // With update
    try (PreparedStatement statement = connection.prepareStatement(
        "INSERT INTO customer (id, first_name, last_name) VALUES (5000, 'Exec', 'Test')")) {
      boolean result = statement.execute();
      assertFalse(result); // false if it is an update count or there are no results
      assertEquals(1, statement.getUpdateCount());
    }

    // With query
    try (PreparedStatement statement = connection
        .prepareStatement("SELECT * FROM customer where id = 5000")) {
      boolean result = statement.execute();
      assertTrue(result); // true if the result is a ResultSet
      try (ResultSet rs = statement.getResultSet()) {
        assertTrue(rs.next());
        assertEquals("Exec", rs.getString("first_name"));
      }
    }
  }

  @Test
  public void testMultipleExecutions() throws SQLException {
    try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM DUAL")) {
      for (int i = 0; i < 3; i++) {
        ps.execute();
      }
    }
  }

  @Test
  public void testexecuteQuery() throws SQLException {
    try (Statement statement = connection.createStatement()) {
        statement.execute("INSERT INTO customer (id, first_name, last_name) VALUES (6000, 'first', 'last')");
    }

    try (Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("select * from customer")) {
      assertTrue(rs.next());
      assertEquals("first", rs.getString("first_name"));
    }
  }
}
