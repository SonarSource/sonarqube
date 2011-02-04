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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AsmClassTest {

  @Test
  public void testGetFieldOrCreateIt() {
    AsmClass asmClass = new AsmClass("java/lang/String");
    assertNull(asmClass.getField("internalString"));
    assertNotNull(asmClass.getFieldOrCreateIt("internalString"));
    assertNotNull(asmClass.getField("internalString"));
  }

  @Test
  public void testGetMethoddOrCreateIt() {
    AsmClass asmClass = new AsmClass("java/lang/String");
    assertNull(asmClass.getMethod("toString()Ljava/lang/String;"));
    assertNotNull(asmClass.getMethodOrCreateIt("toString()Ljava/lang/String;"));
    assertNotNull(asmClass.getMethod("toString()Ljava/lang/String;"));
  }

  @Test
  public void testEqualsAndHashcode() {
    assertEquals(new AsmClass("java/lang/String"), new AsmClass("java/lang/String"));
    assertEquals(new AsmClass("java/lang/String").hashCode(), new AsmClass("java/lang/String").hashCode());
    assertFalse(new AsmClass("java/lang/String").equals(new AsmClass("java/lang/Number")));
    assertFalse(new AsmClass("java/lang/String").hashCode() == new AsmClass("java/lang/Number").hashCode());
  }
}
