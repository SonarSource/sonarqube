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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.sonar.java.bytecode.asm.AsmSignature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AsmSignatureTest {

  @Test
  public void testExtractInternalNamesWithoutGenerics() {
    String[] internalNames = AsmSignature.extractInternalNames("Ljava/lang/String;", null);
    assertEquals("java/lang/String", internalNames[0]);
    assertEquals(1, internalNames.length);
  }

  @Test
  public void testExtractInternalNamesWithGenerics() {
    List<String> internalNames = Arrays.asList(AsmSignature.extractInternalNames("(Ljava/util/List;)Ljava/lang/Number;",
        "(Ljava/util/List<-Ljava/lang/Integer;>;)Ljava/lang/Number;"));
    assertTrue(internalNames.contains("java/lang/Number"));
    assertTrue(internalNames.contains("java/lang/Integer"));
    assertTrue(internalNames.contains("java/util/List"));
    assertEquals(3, internalNames.size());
  }
}
