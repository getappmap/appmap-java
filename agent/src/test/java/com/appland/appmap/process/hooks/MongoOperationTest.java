package com.appland.appmap.process.hooks;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.appland.appmap.process.hooks.Mongo.Operation;

public class MongoOperationTest {
  /** Looks enough like a driver collection for the hook: it has a namespace. */
  public static class FakeCollection {
    public FakeNamespace getNamespace() {
      return new FakeNamespace();
    }
  }

  public static class FakeNamespace {
    public String getCollectionName() {
      return "people";
    }
  }

  private static final Object SESSION = new com.mongodb.session.ClientSession() {
  };

  @Test
  public void plainArguments() {
    Object filter = Collections.singletonMap("a", 1);
    Object update = Collections.singletonMap("$set", 1);
    Operation op = Operation.of(new FakeCollection(), "updateOne", new Object[] { filter, update });
    assertEquals("people", op.collection);
    assertArrayEquals(new String[] { "filter", "update" }, op.parameterNames);
    assertArrayEquals(new String[] { "filter", "update", "options" }, op.argNames);
    assertArrayEquals(new Object[] { filter, update }, op.operationArgs);
  }

  @Test
  public void sessionAndResultClassAreNamedButNotPartOfTheStatement() {
    Object filter = Collections.singletonMap("a", 1);
    Operation op = Operation.of(new FakeCollection(), "find", new Object[] { SESSION, filter, String.class });
    assertArrayEquals(new String[] { "session", "filter", "resultClass" }, op.parameterNames);
    assertArrayEquals(new Object[] { filter }, op.operationArgs);

    op = Operation.of(new FakeCollection(), "countDocuments", new Object[] { SESSION });
    assertArrayEquals(new String[] { "session" }, op.parameterNames);
    assertEquals(0, op.operationArgs.length);

    op = Operation.of(new FakeCollection(), "distinct", new Object[] { "email", String.class });
    assertArrayEquals(new String[] { "key", "resultClass" }, op.parameterNames);
    assertArrayEquals(new Object[] { "email" }, op.operationArgs);
  }

  @Test
  public void unknownMethodsAndCollections() {
    Operation op = Operation.of(new Object(), "somethingNew", new Object[] { 1, 2 });
    assertNull(op.collection);
    assertArrayEquals(new String[] { "arg0", "arg1" }, op.parameterNames);
    assertEquals(0, op.argNames.length);

    op = Operation.of(null, "find", null);
    assertNull(op.collection);
    assertEquals(0, op.parameterNames.length);
  }

  @Test
  public void statementFromOperation() {
    Object filter = Collections.singletonMap("a", 1);
    Operation op = Operation.of(new FakeCollection(), "find", new Object[] { SESSION, filter, String.class });
    assertEquals("db.people.find({\"a\": ?})", MongoQueryShape.formatStatement(op.collection, "find", op.argNames,
        op.operationArgs, MongoDocumentConverter.forCollection(new FakeCollection())));
  }
}
