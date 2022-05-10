package com.appland.appmap.integration;

import org.junit.Test;

import static org.mockito.Mockito.mock;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

// These tests are run with recording enabled. Getting to the end of the test
// shows that recording is working correctly.
public class MockerTest {
  @Test
  public void testMockedStatement() {
    final Statement s = mock(Statement.class);
    try {
      s.execute("select 1;");
    }
    catch (SQLException e) {
      e.printStackTrace();
    }

    assert(true);
  }

  @Test
  public void testMockedConnection() {
    final Connection c = mock(Connection.class);
    try {
      c.nativeSQL("select 1");
    }
    catch (SQLException e) {
      e.printStackTrace();
    }

    assert(true);
  }
}
