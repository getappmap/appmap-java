package com.appland.appmap.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.appland.appmap.util.StringUtil.identifierToSentence;
import static org.junit.Assert.assertEquals;

public class StringUtilTest {
  @Test
  public void testIdentifierToSentence() throws Exception {
    Map<String, String> idToSentence = new HashMap<String, String>(){{
        put("com.myorg.ParameterBuilderTest", "Parameter builder");
        put("LoginTest", "Login");
        put("HTTP", "HTTP");
        put("AuthProviderHTTP", "Auth provider HTTP");
      }};

    for (Map.Entry<String, String> entry : idToSentence.entrySet()) {
      assertEquals(identifierToSentence(entry.getKey()), entry.getValue());
    }
  }
}
