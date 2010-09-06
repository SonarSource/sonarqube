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
package org.sonar.api.measures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;

public class RuleMeasureTest {

  @Test
  public void equals() {
    assertEquals(
        RuleMeasure.createForPriority(CoreMetrics.CLASSES, RulePriority.CRITICAL, 4.5),
        RuleMeasure.createForPriority(CoreMetrics.CLASSES, RulePriority.CRITICAL, 3.4));

    assertEquals(
        RuleMeasure.createForCategory(CoreMetrics.CLASSES, 3, 4.5),
        RuleMeasure.createForCategory(CoreMetrics.CLASSES, 3, 3.4));

    assertEquals(
        RuleMeasure.createForRule(CoreMetrics.CLASSES, new Rule("pmd", "abc1"), 4.5),
        RuleMeasure.createForRule(CoreMetrics.CLASSES, new Rule("pmd", "abc1"), 3.4));

  }

  @Test
  public void notEquals() {
    assertNotEquals(
        RuleMeasure.createForPriority(CoreMetrics.CLASSES, RulePriority.CRITICAL, 4.5),
        RuleMeasure.createForPriority(CoreMetrics.CLASSES, RulePriority.BLOCKER, 3.4));

    assertNotEquals(
        RuleMeasure.createForCategory(CoreMetrics.CLASSES, 3, 4.5),
        RuleMeasure.createForCategory(CoreMetrics.CLASSES, 331, 3.4));

    assertNotEquals(
        RuleMeasure.createForRule(CoreMetrics.CLASSES, new Rule("pmd", "abc1"), 4.5),
        RuleMeasure.createForRule(CoreMetrics.CLASSES, new Rule("pmd", "def2"), 3.4));

    assertNotEquals(
        RuleMeasure.createForPriority(CoreMetrics.CLASSES, RulePriority.CRITICAL, 4.5),
        RuleMeasure.createForRule(CoreMetrics.CLASSES, new Rule("pmd", "abc1"), 3.4));

  }

  private void assertNotEquals(RuleMeasure rm1, RuleMeasure rm2) {
    assertFalse(rm1.equals(rm2));
  }
}
