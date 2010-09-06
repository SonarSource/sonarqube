/*
 * Sonar, entreprise quality control tool.
 * Copyright (C) 2007-2008 Hortis-GRC SA
 * mailto:be_agile HAT hortis DOT ch
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
package org.sonar.samples;

import static junit.framework.Assert.assertEquals;
import org.junit.Test;

public class InnerClassTest {

  @Test
  public void shouldTestMethodToTest() {
    InnerClass innerClass = new InnerClass();
    assertEquals(3, innerClass.methodReturnThree());
  }

  @Test
  public void shouldTestInnerClassInside() {
    InnerClass innerClass = new InnerClass();
    InnerClass.InnerClassInside innerClassInside = innerClass.new InnerClassInside();
    assertEquals(4, innerClassInside.methodReturnFour());
  }

  @Test
  public void shouldTestPrivateClass() {
    PrivateClass privateClass = new PrivateClass();
    assertEquals(5, privateClass.methodReturnfive());
  }
}
