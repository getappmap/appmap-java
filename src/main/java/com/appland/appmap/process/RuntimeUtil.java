package com.appland.appmap.process;

import com.appland.appmap.output.v1.CodeObject;
import com.appland.appmap.output.v1.Event;
import com.appland.appmap.output.v1.Value;
import com.appland.appmap.record.EventAction;
import com.appland.appmap.record.EventTemplateRegistry;
import com.appland.appmap.record.UnknownEventException;

import java.util.HashMap;
import javassist.CtBehavior;

public class RuntimeUtil {
  public static Object boxValue(byte value) {
    return new Byte(value);
  }

  public static Object boxValue(char value) {
    return new Character(value);
  }

  public static Object boxValue(short value) {
    return new Short(value);
  }

  public static Object boxValue(long value) {
    return new Long(value);
  }

  public static Object boxValue(float value) {
    return new Float(value);
  }

  public static Object boxValue(double value) {
    return new Double(value);
  }

  public static Object boxValue(int value) {
    return new Integer(value);
  }

  public static Object boxValue(boolean value) {
    return new Boolean(value);
  }

  public static Object boxValue(Object value) {
    return value;
  }
}
