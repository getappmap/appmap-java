package com.appland.appmap.transform;

import com.appland.appmap.process.EventProcessorType;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.NotFoundException;

public class SqlClassTransformer extends SelectiveClassFileTransformer {
  // private static final ClassProcessorInfo sqlClasses = new ClassProcessorInfo()
  //     .add("java.sql.http.HttpServlet",
  //       new BehaviorInfo("service")
  //         .addParam("javax.servlet.http.HttpServletRequest")
  //         .addParam("javax.servlet.http.HttpServletResponse")
  //         .processedBy(EventProcessorType.Http_Tomcat));

  @Override
  public Boolean canTransformClass(CtClass classType) {
    try {
      final CtClass[] interfaces = classType.getInterfaces();
      for (CtClass superType : interfaces) {
        final String superTypeName = superType.getName();
        if (superTypeName.equals("java.sql.PreparedStatement")) {
          return true;
        }

        if (superTypeName.equals("java.sql.Statement")) {
          return true;
        }

        if (superTypeName.equals("java.sql.CallableStatement")) {
          return true;
        }
      }
    } catch (NotFoundException e) {
      // fall through
    }
    
    return false;
  }

  @Override
  public Boolean canTransformBehavior(CtBehavior behavior) {
    final String behaviorName = behavior.getName();
    return behaviorName.equals("addBatch")
        || behaviorName.equals("execute")
        || behaviorName.equals("executeQuery")
        || behaviorName.equals("executeUpdate");
  }

  @Override
  public EventProcessorType getProcessorType(CtBehavior behavior) {
    return EventProcessorType.Sql_Jdbc;
  }
}
