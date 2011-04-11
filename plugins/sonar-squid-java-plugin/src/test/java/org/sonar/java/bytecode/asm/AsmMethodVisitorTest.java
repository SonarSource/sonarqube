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

import static org.junit.Assert.*;

public class AsmMethodVisitorTest {

  private AsmClassProvider asmClassProvider = new AsmClassProviderImpl(ClassworldsClassLoader.create(SquidTestUtils.getFile("/bytecode/bin/")));

  @Test
  public void testVisitFieldInsn() {
    AsmClass tagNameClass = asmClassProvider.getClass("tags/TagName");
    AsmField nameField = tagNameClass.getField("name");
    AsmMethod constructorWithString = tagNameClass.getMethod("<init>(Ljava/lang/String;)V");
    assertEquals(1, constructorWithString.getCallsToField().size());
    assertTrue(constructorWithString.getCallsToField().contains(nameField));
  }

  @Test
  public void testVisitMethodInsn() {
    AsmClass sourceFileClass = asmClassProvider.getClass("tags/SourceFile");
    AsmMethod readMethod = sourceFileClass.getMethod("read()V");
    AsmMethod readSourceFileMethod = sourceFileClass.getMethod("readSourceFile()V");
    assertEquals(2, readSourceFileMethod.getCallsToMethod().size());
    assertTrue(readSourceFileMethod.getCallsToMethod().contains(readMethod));
    assertEquals(1, readSourceFileMethod.getCallsToField().size());
  }

  @Test
  public void testVisitTryCatchBlock() {
    AsmClass sourceFileClass = asmClassProvider.getClass("tags/SourceFile");
    AsmClass tagExceptionClass = asmClassProvider.getClass("tags/TagException");
    AsmMethod readSourceFileMethod = sourceFileClass.getMethod("readSourceFile()V");
    assertTrue(readSourceFileMethod.getDistinctUsedAsmClasses().contains(tagExceptionClass));
  }

  @Test
  public void testVisitTypeInsn() {
    AsmClass sourceFileClass = asmClassProvider.getClass("tags/SourceFile");
    AsmMethod constructor = sourceFileClass.getMethod("<init>()V");
    assertNotNull(constructor.getDistinctUsedAsmClasses().contains(asmClassProvider.getClass("java/util/ArrayList")));
  }

  @Test
  public void testEmptyMethodProperty() {
    AsmClass asmClass = asmClassProvider.getClass("properties/EmptyMethodProperty");
    assertFalse(asmClass.getMethod("notEmptyMethod()V").isEmpty());
    assertTrue(asmClass.getMethod("emptyMethod()V").isEmpty());
    assertTrue(asmClass.getMethod("emptyAbstractMethod()V").isEmpty());
  }
}
