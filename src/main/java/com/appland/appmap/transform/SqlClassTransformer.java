package com.appland.appmap.transform;

import com.appland.appmap.process.EventProcessorType;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;

public class SqlClassTransformer extends SelectiveClassFileTransformer {
  private static final ClassProcessorInfo sqlClasses = new ClassProcessorInfo()
      .addInterface("java.sql.Connection",
        new BehaviorInfo("nativeSQL"),
        new BehaviorInfo("prepareCall"),
        new BehaviorInfo("prepareStatement"))
      .addInterface("java.sql.Statement",
        new BehaviorInfo("addBatch"),
        new BehaviorInfo("execute"),
        new BehaviorInfo("executeQuery"),
        new BehaviorInfo("executeUpdate"));

  @Override
  public Boolean canTransformClass(CtClass classType) {
    return sqlClasses.inheritsKnownInterface(classType);
  }

  @Override
  public Boolean canTransformBehavior(CtBehavior behavior) {
    return sqlClasses.isKnownInterfaceBehavior(behavior);
  }

  @Override
  public EventProcessorType getProcessorType(CtBehavior behavior) {
    return EventProcessorType.SqlJdbc;
  }
}
