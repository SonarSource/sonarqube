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
package org.sonar.plugins.pmd;

import org.junit.Test;
import org.sonar.api.rules.RulePriority;

import static org.fest.assertions.Assertions.assertThat;

public class PmdLevelUtilsTest {
  @Test
  public void should_get_priority_from_level() {
    assertThat(PmdLevelUtils.fromLevel("1")).isSameAs(RulePriority.BLOCKER);
    assertThat(PmdLevelUtils.fromLevel("2")).isSameAs(RulePriority.CRITICAL);
    assertThat(PmdLevelUtils.fromLevel("3")).isSameAs(RulePriority.MAJOR);
    assertThat(PmdLevelUtils.fromLevel("4")).isSameAs(RulePriority.MINOR);
    assertThat(PmdLevelUtils.fromLevel("5")).isSameAs(RulePriority.INFO);
    assertThat(PmdLevelUtils.fromLevel("?")).isNull();
    assertThat(PmdLevelUtils.fromLevel(null)).isNull();
  }

  @Test
  public void should_get_level_from_priority() {
    assertThat(PmdLevelUtils.toLevel(RulePriority.BLOCKER)).isEqualTo("1");
    assertThat(PmdLevelUtils.toLevel(RulePriority.CRITICAL)).isEqualTo("2");
    assertThat(PmdLevelUtils.toLevel(RulePriority.MAJOR)).isEqualTo("3");
    assertThat(PmdLevelUtils.toLevel(RulePriority.MINOR)).isEqualTo("4");
    assertThat(PmdLevelUtils.toLevel(RulePriority.INFO)).isEqualTo("5");
  }
}
