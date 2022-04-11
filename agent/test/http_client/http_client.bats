#!/usr/bin/env bats

load '../../build/bats/bats-support/load'
load '../../build/bats/bats-assert/load'
load '../helper'

WS_URL=${WS_URL:-http://localhost:8080}
appmap_jar=build/libs/$(ls build/libs | grep 'appmap-[[:digit:]]')
mkdir -p appmap

@test "expected number of http client events captured" {
  skip

  # Exception in thread "main" java.lang.NoClassDefFoundError: java/sql/Time
  #       at com.appland.shade.com.alibaba.fastjson.util.TypeUtils.addBaseClassMappings(TypeUtils.java:1456)
  #       at com.appland.shade.com.alibaba.fastjson.util.TypeUtils.<clinit>(TypeUtils.java:119)
  #       at com.appland.shade.com.alibaba.fastjson.serializer.SerializeConfig.getObjectWriter(SerializeConfig.java:544)
  #       at com.appland.shade.com.alibaba.fastjson.serializer.SerializeConfig.getObjectWriter(SerializeConfig.java:436)
  #       at com.appland.shade.com.alibaba.fastjson.serializer.SerializeConfig.getObjectWriter(SerializeConfig.java:527)
  #       at com.appland.shade.com.alibaba.fastjson.serializer.SerializeConfig.getObjectWriter(SerializeConfig.java:436)
  #       at com.appland.shade.com.alibaba.fastjson.serializer.JSONSerializer.getObjectWriter(JSONSerializer.java:410)
  #       at com.appland.shade.com.alibaba.fastjson.serializer.JSONSerializer.write(JSONSerializer.java:282)
  #       at com.appland.shade.com.alibaba.fastjson.JSONWriter.writeObject(JSONWriter.java:61)
  #       at com.appland.shade.com.alibaba.fastjson.JSONWriter.writeValue(JSONWriter.java:48)
  #       at com.appland.appmap.record.AppMapSerializer.writeClassMap(AppMapSerializer.java:211)

  java -Xbootclasspath/a:$appmap_jar -javaagent:$appmap_jar -Dappmap.debug -Dappmap.config.file=test/http_client/appmap.yml \
   -Dappmap.output.directory=test/http_client/appmap -Dappmap.record=http_client.HttpClientTest.main -cp test/http_client/classes http_client.HttpClientTest ${WS_URL}
  output=$(<appmap/*.appmap.json)
  assert_json_eq '[.events[] | select(.http_client_request)] | length' 3
  assert_json_eq '[.events[] | select(.http_client_response)] | length' 3
}
