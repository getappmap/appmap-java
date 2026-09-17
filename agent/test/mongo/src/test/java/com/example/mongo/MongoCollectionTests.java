package com.example.mongo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

/**
 * Exercises the driver's MongoCollection API against an in-process server.
 * The bats test checks the AppMaps these tests produce. mongo-java-server does
 * not support sessions, so the ClientSession overloads are covered by the
 * agent's unit tests instead.
 */
@Execution(ExecutionMode.SAME_THREAD)
public class MongoCollectionTests {
  private static MongoServer server;
  private static MongoClient client;
  private MongoDatabase db;
  private MongoCollection<Document> people;

  @BeforeAll
  public static void startServer() {
    server = new MongoServer(new MemoryBackend());
    InetSocketAddress address = server.bind();
    client = MongoClients.create("mongodb://" + address.getHostString() + ":" + address.getPort());
  }

  @AfterAll
  public static void stopServer() {
    client.close();
    server.shutdown();
  }

  @BeforeEach
  public void setUp() {
    db = client.getDatabase("appmap-java");
    people = db.getCollection("people");
    people.drop();
  }

  @AfterEach
  public void tearDown() {
    people.drop();
  }

  @Test
  public void crud() {
    people.insertOne(new Document("name", "alice").append("age", 30));
    people.insertMany(Arrays.asList(new Document("name", "bob").append("age", 40),
        new Document("name", "carol").append("age", 50).append("tags", Arrays.asList("x", "y"))));

    people.updateOne(Filters.eq("name", "alice"), Updates.combine(Updates.set("age", 31), Updates.inc("visits", 1)));
    people.updateMany(Filters.gt("age", 35), Updates.set("senior", true));
    people.replaceOne(Filters.eq("name", "bob"), new Document("name", "bob").append("age", 41));

    Document alice = people.find(Filters.eq("name", "alice")).first();
    assertEquals(31, alice.getInteger("age"));

    // bob was replaced after the update, so only carol is still marked senior
    List<Document> seniors = people.find(Filters.and(Filters.in("name", "bob", "carol"), Filters.exists("senior")))
        .sort(Sorts.descending("age")).into(new ArrayList<Document>());
    assertEquals(1, seniors.size());

    assertEquals(3, people.countDocuments());
    assertEquals(1, people.countDocuments(Filters.lt("age", 35)));
    assertEquals(3, people.estimatedDocumentCount());

    List<String> names = people.distinct("name", String.class).into(new ArrayList<String>());
    assertEquals(3, names.size());

    Document total = people.aggregate(Arrays.asList(Aggregates.match(Filters.gte("age", 31)),
        Aggregates.group(null, com.mongodb.client.model.Accumulators.sum("total", "$age")))).first();
    assertEquals(122, total.getInteger("total")); // 31 + 41 + 50

    people.deleteOne(Filters.eq("name", "carol"));
    people.deleteMany(Filters.in("name", Arrays.asList("alice", "bob")));
    assertEquals(0, people.countDocuments());
  }

  @Test
  public void bulkWriteAndPipelineUpdate() {
    List<WriteModel<Document>> ops = new ArrayList<WriteModel<Document>>();
    ops.add(new InsertOneModel<Document>(new Document("name", "dave").append("age", 20)));
    ops.add(new InsertOneModel<Document>(new Document("name", "erin").append("age", 25)));
    ops.add(new UpdateOneModel<Document>(Filters.eq("name", "dave"), Updates.set("age", 21)));
    people.bulkWrite(ops);

    List<Bson> pipeline = Arrays.<Bson>asList(new Document("$set", new Document("adult", true)),
        new Document("$unset", "age"));
    try {
      people.updateMany(Filters.exists("name"), pipeline);
    } catch (MongoCommandException e) {
      // mongo-java-server 1.43 does not implement pipeline updates. The
      // statement is recorded either way.
    }

    Document found = people.findOneAndUpdate(Filters.eq("name", "erin"), Updates.set("age", 26));
    assertEquals("erin", found.getString("name"));
    people.findOneAndDelete(Filters.eq("name", "dave"));
  }

  @Test
  public void indexes() {
    String name = people.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true));
    List<Document> indexes = people.listIndexes().into(new ArrayList<Document>());
    assertTrue(indexes.size() >= 2);
    people.insertOne(new Document("name", "frank"));
    // The second insert violates the unique index. The failure must be
    // recorded on the query event and still be thrown to the application.
    assertThrows(MongoWriteException.class, () -> people.insertOne(new Document("name", "frank")));
    people.dropIndex(name);
    people.dropIndexes();
  }

  @Test
  public void pojos() {
    CodecRegistry pojoRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
        CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
    MongoCollection<Person> persons = db.getCollection("people", Person.class).withCodecRegistry(pojoRegistry);
    persons.insertOne(new Person("grace", 35));
    persons.insertMany(Arrays.asList(new Person("heidi", 36), new Person("ivan", 37)));
    Person grace = persons.find(Filters.eq("name", "grace")).first();
    assertEquals(35, grace.getAge());
    assertEquals(3, persons.countDocuments());
  }

  @Test
  public void oddCollectionName() {
    MongoCollection<Document> odd = db.getCollection("with-dash");
    odd.insertOne(new Document("a", 1));
    assertEquals(1, odd.countDocuments());
    odd.drop();
  }
}
