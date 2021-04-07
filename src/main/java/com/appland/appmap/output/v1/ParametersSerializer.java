package com.appland.appmap.output.v1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.*;

import com.appland.appmap.util.Logger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Serializes the {@link Parameters} type to JSON.
 * @see Parameters
 */
public class ParametersSerializer implements ObjectSerializer {
  static {
    SerializeConfig.getGlobalInstance().put(Parameters.class, new ParametersSerializer());
  }

  /**
   * Serializes the object.
  * @param serializer The JSON serializer
  * @param object The object that needs to be converted to Json.
  * @param fieldName Parent object field name
  * @param fieldType Parent object field type
  * @param features Parent object field serializer features
  * @throws IOException On failure
   */
  public void write(JSONSerializer serializer,
                    Object object,
                    Object fieldName,
                    Type fieldType,
                    int features) throws IOException {
    SerializeWriter out = serializer.getWriter();
    Parameters params = (Parameters) object;
    if (params == null) {
      if (serializer.isEnabled(SerializerFeature.WriteNullListAsEmpty)) {
        out.write("[]");
      } else {
        out.writeNull();
      }
      return;
    }

    try {
      Field valuesField = Parameters.class.getDeclaredField("values");
      out.write(JSON.toJSONString(valuesField.get(params)));
    } catch (Exception e) {
      Logger.println("failed to serialize parameters");
      Logger.println(e);
      out.writeNull();
    }
  }
}
