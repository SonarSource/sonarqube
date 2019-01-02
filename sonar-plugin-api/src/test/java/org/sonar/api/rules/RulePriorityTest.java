/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.rules;

import org.junit.Test;
import org.sonar.check.Priority;

import static org.assertj.core.api.Assertions.assertThat;

public class RulePriorityTest {

  @Test
  public void testValueOfString() {
    assertThat(RulePriority.INFO).isEqualTo(RulePriority.valueOfString("info"));
    assertThat(RulePriority.MAJOR).isEqualTo(RulePriority.valueOfString("MAJOR"));
    assertThat(RulePriority.MAJOR).isEqualTo(RulePriority.valueOfString("ERROR"));
    assertThat(RulePriority.INFO).isEqualTo(RulePriority.valueOfString("WARNING"));
    assertThat(RulePriority.MAJOR).isEqualTo(RulePriority.valueOfString("ErRor"));
    assertThat(RulePriority.INFO).isEqualTo(RulePriority.valueOfString("WaRnInG"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnknownValueOfString() {
    RulePriority.valueOfString("make me crash");
  }

  @Test
  public void test_toCheckPriority() {
    assertThat(RulePriority.fromCheckPriority(Priority.BLOCKER)).isEqualTo(RulePriority.BLOCKER);
    assertThat(RulePriority.fromCheckPriority(Priority.CRITICAL)).isEqualTo(RulePriority.CRITICAL);
    assertThat(RulePriority.fromCheckPriority(Priority.MAJOR)).isEqualTo(RulePriority.MAJOR);
    assertThat(RulePriority.fromCheckPriority(Priority.MINOR)).isEqualTo(RulePriority.MINOR);
    assertThat(RulePriority.fromCheckPriority(Priority.INFO)).isEqualTo(RulePriority.INFO);
  }
}
