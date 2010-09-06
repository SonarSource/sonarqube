package org.sonar.java.bytecode.visitor;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.java.bytecode.ClassworldsClassLoader;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmClassProvider;
import org.sonar.java.bytecode.asm.AsmClassProviderImpl;
import org.sonar.java.bytecode.asm.AsmMethod;

import static org.junit.Assert.*;
import static org.sonar.java.ast.SquidTestUtils.getFile;

public class AccessorVisitorTest {

  private static AsmClassProvider asmClassProvider;
  private static AsmClass javaBean;
  private static AccessorVisitor accessorVisitor = new AccessorVisitor();

  @BeforeClass
  public static void init() {
    asmClassProvider = new AsmClassProviderImpl(ClassworldsClassLoader.create(getFile("/bytecode/bin/")));
    javaBean = asmClassProvider.getClass("properties/JavaBean");
    accessorVisitor.visitClass(javaBean);
    for (AsmMethod method : javaBean.getMethods()) {
      accessorVisitor.visitMethod(method);
    }
  }

  @Test
  public void testAccessorMethods() {
    assertTrue(javaBean.getMethod("getName()Ljava/lang/String;").isAccessor());
    assertTrue(javaBean.getMethod("setName(Ljava/lang/String;)V").isAccessor());
    assertTrue(javaBean.getMethod("setFrench(Z)V").isAccessor());
    assertTrue(javaBean.getMethod("isFrench()Z").isAccessor());
    assertFalse(javaBean.getMethod("anotherMethod()V").isAccessor());
  }
}
