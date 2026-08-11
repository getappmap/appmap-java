package com.appland.appmap.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson.JSON;
import com.appland.appmap.config.AppMapConfig;
import com.appland.appmap.record.Recorder;
import com.appland.appmap.record.Recording;
import com.appland.appmap.test.fixture.labels.LabelFixture;

/**
 * End-to-end check that the {@code @Labels} bypass and config-named bypass take effect when the
 * agent instruments real classes. Runs under {@code integrationTest} with the agent attached as
 * {@code -javaagent:}.
 */
public class LabelFixtureRecorderTest {
  private static final String FX = "com.appland.appmap.test.fixture.labels.LabelFixture";

  private final Recorder recorder = Recorder.getInstance();

  @BeforeEach
  public void initialize() throws Exception {
    AppMapConfig.initialize(FileSystems.getDefault());
  }

  @Test
  public void labeledGettersAndSettersAreRecorded() throws IOException {
    final LabelFixture fixture = new LabelFixture();

    Recording recording = recorder.record(() -> {
      fixture.getPlain();
      fixture.getSecret();
      fixture.setSecret("updated");
      fixture.getNamedInConfig();
      fixture.describe();
    });
    assertNotNull(recording);

    StringWriter sw = new StringWriter();
    recording.readFully(true, sw);
    Map<?, ?> appmap = JSON.parseObject(sw.toString(), Map.class);

    Map<String, Map<String, Object>> recorded = collectFunctions(
        (List<?>)appmap.get("classMap"), "");

    assertTrue(recorded.containsKey(FX + "#getSecret"),
        "@Labels-annotated getter should be recorded; saw " + recorded.keySet());
    assertTrue(recorded.containsKey(FX + "#setSecret"),
        "@Labels-annotated setter should be recorded; saw " + recorded.keySet());
    assertTrue(recorded.containsKey(FX + "#getNamedInConfig"),
        "Getter named in appmap.yml should be recorded; saw " + recorded.keySet());
    assertTrue(recorded.containsKey(FX + "#describe"),
        "Non-trivial method should be recorded; saw " + recorded.keySet());

    assertFalse(recorded.containsKey(FX + "#getPlain"),
        "Plain unlabeled getter should NOT be recorded; saw " + recorded.keySet());

    assertEquals(java.util.Collections.singletonList("secret"),
        recorded.get(FX + "#getSecret").get("labels"));
    assertEquals(java.util.Collections.singletonList("mutator"),
        recorded.get(FX + "#setSecret").get("labels"));

    Set<String> calledMethods = new HashSet<>();
    for (Object e : (List<?>)appmap.get("events")) {
      Map<?, ?> event = (Map<?, ?>)e;
      if ("call".equals(event.get("event"))) {
        Object cls = event.get("defined_class");
        Object method = event.get("method_id");
        if (cls != null && method != null) {
          calledMethods.add(cls + "#" + method);
        }
      }
    }
    assertTrue(calledMethods.contains(FX + "#getSecret"),
        "Expected call event for getSecret; saw " + calledMethods);
    assertTrue(calledMethods.contains(FX + "#setSecret"),
        "Expected call event for setSecret; saw " + calledMethods);
    assertTrue(calledMethods.contains(FX + "#getNamedInConfig"),
        "Expected call event for getNamedInConfig; saw " + calledMethods);
    assertFalse(calledMethods.contains(FX + "#getPlain"),
        "Plain unlabeled getter should not appear in events; saw " + calledMethods);
  }

  /** Walk the classMap tree and collect all "function" leaves keyed by fully qualified name. */
  @SuppressWarnings("unchecked")
  private Map<String, Map<String, Object>> collectFunctions(List<?> nodes, String parent) {
    Map<String, Map<String, Object>> out = new HashMap<>();
    if (nodes == null) {
      return out;
    }
    for (Object n : nodes) {
      Map<String, Object> node = (Map<String, Object>)n;
      String name = (String)node.get("name");
      String type = (String)node.get("type");
      String qualified;
      if ("function".equals(type)) {
        Boolean isStatic = (Boolean)node.get("static");
        qualified = parent + (Boolean.TRUE.equals(isStatic) ? "." : "#") + name;
        out.put(qualified, node);
      } else {
        qualified = parent.isEmpty() ? name : parent + "." + name;
      }
      out.putAll(collectFunctions((List<?>)node.get("children"), qualified));
    }
    return out;
  }
}
