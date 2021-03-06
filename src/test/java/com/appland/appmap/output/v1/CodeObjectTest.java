package com.appland.appmap.output.v1;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import org.junit.Test;

import static org.junit.Assert.*;

public class CodeObjectTest {

    @Test
    public void getSourceFilePath_for_RegularClass() throws NotFoundException {
        CtClass testCtClass = ClassPool.getDefault().get("com.appland.appmap.ExampleClass");
        assertEquals("com/appland/appmap/ExampleClass.java", CodeObject.getSourceFilePath(testCtClass));
    }

    @Test
    public void getSourceFilePath_for_InnerClass_ResultInBaseClass() throws NotFoundException {
        CtClass testCtClass = ClassPool.getDefault().get("com.appland.appmap.output.v1.testclasses.ExampleInnerClass$StaticFinalInnerClass");
        assertEquals("com/appland/appmap/output/v1/testclasses/ExampleInnerClass.java", CodeObject.getSourceFilePath(testCtClass));
    }

    @Test
    public void getSourceFilePath_for_AnonymousClass_ResultInBaseClass() throws NotFoundException {
        CtClass testCtClass = ClassPool.getDefault().get("com.appland.appmap.output.v1.testclasses.Anonymous$1");
        assertEquals("com/appland/appmap/output/v1/testclasses/Anonymous.java", CodeObject.getSourceFilePath(testCtClass));
    }


}