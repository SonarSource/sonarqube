/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.rules;

import static junit.framework.Assert.assertEquals;
import org.junit.Test;

public class RulePriorityTest {

  @Test
  public void testValueOfString() {
    assertEquals(RulePriority.INFO, RulePriority.valueOfString("info"));
    assertEquals(RulePriority.MAJOR, RulePriority.valueOfString("MAJOR"));
    assertEquals(RulePriority.MAJOR, RulePriority.valueOfString("ERROR"));
    assertEquals(RulePriority.INFO, RulePriority.valueOfString("WARNING"));
    assertEquals(RulePriority.MAJOR, RulePriority.valueOfString("ErRor"));
    assertEquals(RulePriority.INFO, RulePriority.valueOfString("WaRnInG"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnknownValueOfString() {
    RulePriority.valueOfString("make me crash");
  }

}
