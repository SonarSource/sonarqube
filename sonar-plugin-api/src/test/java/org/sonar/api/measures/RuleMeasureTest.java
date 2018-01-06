/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.measures;

import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RuleMeasureTest {

  @Test
  public void shouldEquals() {
    assertEquals(
        RuleMeasure.createForPriority(CoreMetrics.CLASSES, RulePriority.CRITICAL, 4.5),
        RuleMeasure.createForPriority(CoreMetrics.CLASSES, RulePriority.CRITICAL, 3.4));

    assertEquals(
        RuleMeasure.createForRule(CoreMetrics.CLASSES, new Rule("pmd", "abc1"), 4.5),
        RuleMeasure.createForRule(CoreMetrics.CLASSES, new Rule("pmd", "abc1"), 3.4));

  }

  @Test
  public void shouldNotEquals() {
    assertNotEquals(
        RuleMeasure.createForRule(CoreMetrics.BLOCKER_VIOLATIONS, new Rule("pmd", "abc1"), 4.5),
        RuleMeasure.createForRule(CoreMetrics.BLOCKER_VIOLATIONS, new Rule("pmd", "def2"), 3.4));

    assertNotEquals(
        RuleMeasure.createForRule(CoreMetrics.INFO_VIOLATIONS, new Rule("pmd", "abc1"), 4.5),
        RuleMeasure.createForRule(CoreMetrics.BLOCKER_VIOLATIONS, new Rule("pmd", "abc1"), 3.4));
  }

  private void assertNotEquals(RuleMeasure rm1, RuleMeasure rm2) {
    assertFalse(rm1.equals(rm2));
  }
}
