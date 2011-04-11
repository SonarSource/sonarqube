/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.bytecode.asm;

import org.junit.Test;
import org.sonar.java.ast.SquidTestUtils;
import org.sonar.java.bytecode.ClassworldsClassLoader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AsmClassVisitorTest {

  private static AsmClassProvider asmClassProvider = new AsmClassProviderImpl(ClassworldsClassLoader.create(SquidTestUtils.getFile("/bytecode/bin/")));

  @Test
  public void testVisit() {
    AsmClass asmClass = asmClassProvider.getClass("java/lang/String");
    assertEquals("java/lang/String", asmClass.getInternalName());
    assertFalse(asmClass.isAbstract());
    assertFalse(asmClass.isInterface());
  }

  @Test
  public void testVisitMethod() {
    AsmClass asmClass = asmClassProvider.getClass("java/lang/String");
    assertNotNull(asmClass.getMethod("charAt(I)C"));
    assertTrue(asmClass.getMethod("charAt(I)C").isPublic());
    assertFalse(asmClass.getMethod("charAt(I)C").isDeprecated());
    assertFalse(asmClass.getMethod("charAt(I)C").isStatic());
    assertTrue(asmClass.getMethod("valueOf(C)Ljava/lang/String;").isStatic());

    asmClass = asmClassProvider.getClass("tags/File");
    AsmMethod getLines = asmClass.getMethod("read(Ljava/util/Collection;Ljava/lang/String;)Ljava/lang/String;");
    assertTrue(getLines.getDistinctUsedAsmClasses().contains(new AsmClass("java/util/Collection")));
    assertTrue(getLines.getDistinctUsedAsmClasses().contains(new AsmClass("tags/File")));
    assertTrue(getLines.getDistinctUsedAsmClasses().contains(new AsmClass("java/lang/String")));
    assertTrue(getLines.getDistinctUsedAsmClasses().contains(new AsmClass("java/lang/RuntimeException")));
  }

  @Test
  public void testVisitMehtodAccessFlags() {
    AsmClass fileClass = asmClassProvider.getClass("tags/File");
    assertTrue(fileClass.getMethod("read()V").isPublic());
    AsmClass stringClass = asmClassProvider.getClass("java/lang/String");
    assertFalse(stringClass.getMethod("toString()Ljava/lang/String;").isDeprecated());
  }

  @Test
  public void testInheritedMethodProperty() {
    AsmClass asmClass = asmClassProvider.getClass("properties/InheritedMethodsProperty");
    assertTrue(asmClass.getMethod("equals(Ljava/lang/Object;)Z").isInherited());
    assertFalse(asmClass.getMethod("notInheritedMethod()V").isInherited());
    assertTrue(asmClass.getMethod("run()V").isInherited());
  }

  @Test
  public void testMethodBodyLoadedProperty() {
    AsmClass asmClass = asmClassProvider.getClass("properties/MethodBodyLoadedProperty");
    assertTrue(asmClass.getMethod("doJob()V").isBodyLoaded());
    assertFalse(asmClass.getMethod("run()V").isBodyLoaded());
  }

  @Test
  public void testResourceTouchedProperty() {
    AsmClass asmClass = asmClassProvider.getClass("properties/ResourceTouchedProperty");
    assertFalse(asmClass.getField("unusedField").isUsed());
    assertTrue(asmClass.getField("usedField").isUsed());
    assertTrue(asmClass.getMethod("doPrivateJob()V").isUsed());
    assertFalse(asmClass.getMethod("run()V").isUsed());
    assertFalse(asmClass.isUsed());
    assertTrue(asmClassProvider.getClass("java/lang/Runnable").isUsed());
  }

  @Test
  public void testVisitFieldAccessFlags() {
    AsmClass asmClass = asmClassProvider.getClass("java/lang/String");
    assertNotNull(asmClass.getField("CASE_INSENSITIVE_ORDER"));
    assertTrue(asmClass.getField("CASE_INSENSITIVE_ORDER").isStatic());
    assertTrue(asmClass.getField("CASE_INSENSITIVE_ORDER").isPublic());
    assertTrue(asmClass.getField("CASE_INSENSITIVE_ORDER").isFinal());
    assertFalse(asmClass.getField("CASE_INSENSITIVE_ORDER").isDeprecated());
  }

  @Test
  public void testVisitObjectField() {
    AsmClass asmClass = asmClassProvider.getClass("tags/SourceFile");
    AsmEdge pathAsmEdge = asmClass.getField("path").getOutgoingEdges().iterator().next();
    assertEquals("java/lang/String", pathAsmEdge.getTargetAsmClass().getInternalName());
  }

}
