#!/usr/bin/env bats
#
# Test the MongoCollection hook against an in-process MongoDB server
# (mongo-java-server), so no external service is needed.

load '../helper'
load '../jdbc/helper'

setup_file() {
  cd "$BATS_TEST_DIRNAME" || exit 1
  _configure_logging

  gradlew -q clean
}

setup() {
  rm -rf tmp/appmap
}

# Loads the AppMap of one test method into $output for assert_json_eq. Note
# that `run` overwrites $output, so call this again after a `run`.
appmap_for() {
  local map_file="tmp/appmap/junit/com_example_mongo_MongoCollectionTests_$1.appmap.json"
  [ -f "$map_file" ]
  output="$(<"$map_file")"
}

# Prints the sql of every sql_query event, one per line.
queries() {
  jq -r '.events[] | select(.sql_query) | .sql_query.sql' <<< "$output"
}

@test "collection operations are recorded as queries" {
  run gradlew -q test --tests 'MongoCollectionTests.crud' --rerun-tasks
  assert_success

  appmap_for crud
  assert_json_eq '.metadata.test_status' succeeded
  # every query event says which database it is for
  assert_json_eq '[.events[] | select(.sql_query) | .sql_query.database_type] | unique | .[0]' mongodb

  run assert_all_calls_returned tmp/appmap/junit/*.appmap.json
  assert_success

  appmap_for crud
  run queries
  assert_line 'db.people.insertOne({"name": ?, "age": ?})'
  assert_line 'db.people.insertMany([{"name": ?, "age": ?}, {"name": ?, "age": ?, "tags": [?]}])'
  assert_line 'db.people.updateOne({"name": ?}, {"$set": {"age": ?}, "$inc": {"visits": ?}})'
  assert_line 'db.people.updateMany({"age": {"$gt": ?}}, {"$set": {"senior": ?}})'
  assert_line 'db.people.replaceOne({"name": ?}, {"name": ?, "age": ?})'
  assert_line 'db.people.find({"name": ?})'
  assert_line 'db.people.find({"$and": [{"name": {"$in": [?]}}, {"senior": {"$exists": ?}}]})'
  assert_line 'db.people.countDocuments()'
  assert_line 'db.people.countDocuments({"age": {"$lt": ?}})'
  assert_line 'db.people.estimatedDocumentCount()'
  assert_line 'db.people.distinct("name")'
  assert_line 'db.people.aggregate([{"$match": {"age": {"$gte": ?}}}, {"$group": {"_id": ?, "total": {"$sum": ?}}}])'
  assert_line 'db.people.deleteOne({"name": ?})'
  assert_line 'db.people.deleteMany({"name": {"$in": [?]}})'
}

@test "the function call event carries the Node parameter names" {
  run gradlew -q test --tests 'MongoCollectionTests.crud' --rerun-tasks
  assert_success

  appmap_for crud
  # the query event is a child of the collection method call
  assert_json_eq '[.events[] | select(.method_id == "updateOne")][0].parameters | map(.name) | join(",")' 'filter,update'
  local call_id
  call_id="$(jq -r '[.events[] | select(.method_id == "updateOne")][0].id' <<< "$output")"
  assert_json_eq "[.events[] | select(.event == \"call\" and .id == $((call_id + 1)))][0].sql_query.database_type" mongodb
  assert_json_eq '[.events[] | select(.method_id == "distinct")][0].parameters | map(.name) | join(",")' 'key,resultClass'
}

@test "bulk writes and update pipelines" {
  run gradlew -q test --tests 'MongoCollectionTests.bulkWriteAndPipelineUpdate' --rerun-tasks
  assert_success

  appmap_for bulkWriteAndPipelineUpdate
  run queries
  assert_line 'db.people.bulkWrite([{"insertOne": {"document": {"name": ?, "age": ?}}}, {"updateOne": {"filter": {"name": ?}, "update": {"$set": {"age": ?}}}}])'
  assert_line 'db.people.updateMany({"name": {"$exists": ?}}, [{"$set": {"adult": ?}}, {"$unset": ?}])'
  assert_line 'db.people.findOneAndUpdate({"name": ?}, {"$set": {"age": ?}})'
  assert_line 'db.people.findOneAndDelete({"name": ?})'
}

@test "index operations and a failed insert" {
  run gradlew -q test --tests 'MongoCollectionTests.indexes' --rerun-tasks
  assert_success

  appmap_for indexes
  # the duplicate insert is recorded as an exception on the query and on the call
  assert_json_eq '[.events[] | select(.exceptions) | .exceptions[0].class] | unique | .[0]' com.mongodb.MongoWriteException
  assert_json_eq '[.events[] | select(.exceptions)] | length' 2

  run queries
  assert_line 'db.people.createIndex({"name": ?}, ?)'
  assert_line 'db.people.listIndexes()'
  assert_line 'db.people.dropIndex("name_1")'
  assert_line 'db.people.dropIndexes()'
}

@test "POJO documents" {
  run gradlew -q test --tests 'MongoCollectionTests.pojos' --rerun-tasks
  assert_success

  appmap_for pojos
  run queries
  # a POJO is encoded with the collection's codec, so its fields are the shape
  assert_line 'db.people.insertOne({"age": ?, "name": ?})'
  assert_line 'db.people.insertMany([{"age": ?, "name": ?}])'
  assert_line 'db.people.find({"name": ?})'
  assert_line 'db.people.countDocuments()'
}

@test "collection names that are not identifiers" {
  run gradlew -q test --tests 'MongoCollectionTests.oddCollectionName' --rerun-tasks
  assert_success

  appmap_for oddCollectionName
  run queries
  assert_line 'db.getCollection("with-dash").insertOne({"a": ?})'
  assert_line 'db.getCollection("with-dash").countDocuments()'
}
