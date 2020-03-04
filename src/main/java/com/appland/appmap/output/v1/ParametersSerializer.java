package com.appland.appmap.output.v1;

import java.io.IOException;
import java.lang.reflect.Type;
import java.lang.reflect.Field;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class ParametersSerializer implements ObjectSerializer {
  static {
    SerializeConfig.getGlobalInstance().put(Parameters.class, new ParametersSerializer());
  }

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
      System.err.println("AppMap: failed to serialize parameters");
      System.err.println(e.getMessage());
      out.writeNull();
    }
  }
}