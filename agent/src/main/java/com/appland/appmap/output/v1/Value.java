package com.appland.appmap.output.v1;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import com.appland.appmap.config.Properties;
import com.appland.appmap.util.Logger;
import org.apache.commons.lang3.StringUtils;

/**
 * A serializable snapshot of a runtime Object.
 *
 * @see Event
 * @see Parameters
 * @see <a href="https://github.com/applandinc/appmap#parameter-object-format">Github: AppMap - Parameter object format</a>
 */
public class Value {
  public String kind;

  public String name;

  @JSONField(serializeUsing = ToStringSerializer.class)
  public Object value;

  @JSONField(name = "class")
  public String classType;

  @JSONField(name = "object_id")
  public Integer objectId;

  /**
   * Store an Object in this value. Does not set the name of this Value. If given null, the value
   * will be null and the object ID will be set to zero.
   * @param val The object to be stored.
   */
  public void set(Object val) {
    if (val != null) {
      this.classType = val.getClass().getName();
      this.objectId = System.identityHashCode(val);
      this.value = val;
    } else {
      this.value = null;
      this.objectId = 0;
    }
  }

  public Value() { }

  /**
   * Copy constructor. Copies class type, kind and name.
   */
  public Value(Value master) {
    this.classType = master.classType;
    this.kind = master.kind;
    this.name = master.name;
  }

  /**
   * Construct from an existing object. Copies class type, object ID, and value.
   */
  public Value(Object val) {
    this.set(val);
  }

  /**
   * Constructs a Value from an existing object and set its name. Copies class type, object ID, and
   * value.
   */
  public Value(Object val, String name) {
    this.name = name;
    this.kind = "req";
    this.set(val);
  }

  /**
   * Set the "class_type" field.
   * @param classType The class type of this Value
   * @return {@code this}
   */
  public Value setClassType(String classType) {
    this.classType = classType;
    return this;
  }

  /**
   * Sets the "name" field.
   * @return {@code this}
   */
  public Value setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets the "kind" field.
   * @return {@code this}
   */
  public Value setKind(String kind) {
    this.kind = kind;
    return this;
  }

  /**
   * Gets the object stored within this Value.
   */
  @SuppressWarnings("unchecked")
  public <T> T get() {
    return (T) this.value.getClass();
  }

  /**
   * Removes external Object references by casting the current value to a String. This prevents
   * future null pointer exceptions or undefined behavior caused by state changes. This should be
   * called once a Value is finalized.
   * @return {@code this}
   */
  public Value freeze() {
    if (this.value != null) {
      if (Properties.DisableValue) {
        this.value = "< disabled >";
      } else {
        try {
          this.value = this.value.toString();

          if (Properties.MaxValueSize > 0 && this.value != null) {
            this.value = StringUtils.abbreviate((String) this.value, "...", Properties.MaxValueSize);
          }
        } catch (Throwable e) {
          Logger.println("failed to resolve value of " + this.classType);
          Logger.println(e.getMessage());
          // it's possible our value object has been partially cleaned up and
          // calls toString on a null object or the operation is otherwise
          // unsupported
          this.value = "< invalid >";
        }
      }
    }
    return this;
  }
}
