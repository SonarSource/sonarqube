/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.issue.index;

import org.junit.Test;

import static java.util.OptionalInt.empty;
import static org.assertj.core.api.Assertions.assertThat;

public class SecurityStandardCategoryStatisticsTest {

  @Test
  public void hasMoreRules_default_false() {
    SecurityStandardCategoryStatistics standardCategoryStatistics = new SecurityStandardCategoryStatistics(
      "cat", 0, empty(), 0,
      0, 5, null
    );
    assertThat(standardCategoryStatistics.hasMoreRules()).isFalse();
  }

  @Test
  public void hasMoreRules_is_updatable() {
    SecurityStandardCategoryStatistics standardCategoryStatistics = new SecurityStandardCategoryStatistics(
      "cat", 0, empty(), 0,
      0, 5, null
    );
    standardCategoryStatistics.setHasMoreRules(true);
    assertThat(standardCategoryStatistics.hasMoreRules()).isTrue();
  }

}