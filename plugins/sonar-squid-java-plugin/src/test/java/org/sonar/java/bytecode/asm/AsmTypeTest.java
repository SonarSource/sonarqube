/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
import org.objectweb.asm.Type;
import org.sonar.java.bytecode.asm.AsmType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AsmTypeTest {

  @Test
  public void testIsArray() {
    assertTrue(AsmType.isArray(Type.getType("[Ljava/lang/String;")));
    assertTrue(AsmType.isArray(Type.getType("[I")));
    assertFalse(AsmType.isArray(Type.getType("I")));
  }

  @Test
  public void testIsObject() {
    assertTrue(AsmType.isObject(Type.getType("Ljava/lang/Number;")));
    assertFalse(AsmType.isObject(Type.getType("B")));
  }

  @Test
  public void testIsArrayOfObject() {
    assertTrue(AsmType.isArrayOfObject(Type.getType("[Ljava/lang/Number;")));
    assertTrue(AsmType.isArrayOfObject(Type.getType("[[Ljava/lang/Number;")));
  }

  @Test
  public void testIsVoid() {
    assertTrue(AsmType.isVoid(Type.getType("V")));
    assertFalse(AsmType.isVoid(Type.getType("B")));
  }

  @Test
  public void testGetInternalName() {
    assertEquals("java/lang/String", AsmType.getObjectInternalName(Type.getType("[[[Ljava/lang/String;")));
    assertEquals("java/lang/String", AsmType.getObjectInternalName(Type.getType("Ljava/lang/String;")));
  }

  @Test
  public void testContainsObject() {
    assertTrue(AsmType.containsObject(Type.getType("[[[Ljava/lang/String;")));
    assertTrue(AsmType.containsObject(Type.getType("Ljava/lang/String;")));
    assertFalse(AsmType.containsObject(Type.getType("B")));
    assertFalse(AsmType.containsObject(Type.getType("[B")));
  }

  @Test(expected = IllegalStateException.class)
  public void testGetInternalNameOnPrimitiveDescriptor() {
    AsmType.getObjectInternalName(Type.getType("[[[I"));
  }

}
