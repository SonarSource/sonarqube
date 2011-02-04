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
import org.sonar.java.bytecode.ClassworldsClassLoader;
import org.sonar.java.bytecode.asm.AsmClassProvider.DETAIL_LEVEL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.sonar.java.ast.SquidTestUtils.getFile;

public class AsmClassProviderImplTest {

  private AsmClassProviderImpl asmClassProviderImpl = new AsmClassProviderImpl();

  @Test
  public void testReadClass() {
    AsmClass asmClass = asmClassProviderImpl.getClass("java/lang/Object");
    assertNotNull(asmClass);
    assertEquals("java/lang/Object", asmClass.getInternalName());
    assertNotNull(asmClassProviderImpl.getClass("bytecode/bin/tags/File"));
  }

  @Test
  public void testCacheMechanism() {
    AsmClass asmClass = asmClassProviderImpl.getClass("java/lang/Object");
    AsmClass asmClass2 = asmClassProviderImpl.getClass("java/lang/Object");
    assertEquals(asmClass, asmClass2);
  }

  @Test
  public void testGetUnknownClass() {
    AsmClass unknownClass = asmClassProviderImpl.getClass("java/lang/UnknownClass");
    assertNotNull(unknownClass);
    assertNull(unknownClass.getSuperClass());
  }

  @Test
  public void testloadSuperClass() {
    AsmClass doubleClass = asmClassProviderImpl.getClass("java/lang/Double");
    assertEquals("java/lang/Number", doubleClass.getSuperClass().getInternalName());
    assertEquals("java/lang/Object", doubleClass.getSuperClass().getSuperClass().getInternalName());
    assertNull(doubleClass.getSuperClass().getSuperClass().getSuperClass());
  }

  @Test
  public void testloadInterfaces() {
    AsmClass characterClass = asmClassProviderImpl.getClass("java/lang/Character");
    assertEquals(2, characterClass.getInterfaces().size());
  }

  @Test
  public void getSeveralTimesTheSameClassButWithHigherDetailLevel() {
    AsmClass integerClass = asmClassProviderImpl.getClass("java/lang/Integer", DETAIL_LEVEL.NOTHING);
    assertEquals("java/lang/Integer", integerClass.getInternalName());
    assertEquals(DETAIL_LEVEL.NOTHING, integerClass.getDetailLevel());
    assertEquals(0, integerClass.getMethods().size());
    assertNull(integerClass.getSuperClass());

    AsmClass integerClassWithHigherDetailLevel = asmClassProviderImpl.getClass("java/lang/Integer", DETAIL_LEVEL.STRUCTURE_AND_CALLS);
    assertEquals(DETAIL_LEVEL.STRUCTURE_AND_CALLS, integerClassWithHigherDetailLevel.getDetailLevel());
    assertSame(integerClass, integerClassWithHigherDetailLevel);
    assertEquals("java/lang/Number", integerClass.getSuperClass().getInternalName());
  }

  @Test
  public void testPersonalClassLoader() {
    asmClassProviderImpl = new AsmClassProviderImpl(ClassworldsClassLoader.create(getFile("/bytecode/bin/")));
    assertEquals(DETAIL_LEVEL.STRUCTURE_AND_CALLS, asmClassProviderImpl.getClass("tags/Line", DETAIL_LEVEL.STRUCTURE_AND_CALLS).getDetailLevel());
  }
}
