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

import static org.fest.assertions.Assertions.assertThat;

public class AsmFieldTest {

  private AsmClass stringClass = new AsmClass("java/lang/String");
  private AsmClass numberClass = new AsmClass("java/lang/Number");

  @Test
  public void testEquals() {
    assertThat(new AsmField(stringClass, "firstField")).isEqualTo(new AsmField(stringClass, "firstField"));
    assertThat(new AsmField(stringClass, "firstField")).isNotEqualTo(new AsmField(stringClass, "secondField"));
    assertThat(new AsmField(stringClass, "firstField")).isNotEqualTo(new AsmField(numberClass, "firstField"));
  }

  @Test
  public void testHashCode() {
    assertThat(new AsmField(stringClass, "firstField").hashCode()).isEqualTo(new AsmField(stringClass, "firstField").hashCode());
    assertThat(new AsmField(stringClass, "firstField").hashCode()).isNotEqualTo(new AsmField(stringClass, "secondField").hashCode());
    assertThat(new AsmField(stringClass, "firstField").hashCode()).isNotEqualTo(new AsmField(numberClass, "firstField").hashCode());
  }
}
