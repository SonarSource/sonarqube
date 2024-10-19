/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.ArrayList;
import org.junit.Test;

import static java.util.OptionalInt.empty;
import static org.assertj.core.api.Assertions.assertThat;

public class SecurityStandardCategoryStatisticsTest {

  @Test
  public void hasMoreRules_default_false() {
    SecurityStandardCategoryStatistics standardCategoryStatistics = new SecurityStandardCategoryStatistics(
      "cat", 0, empty(), 0,
      0, 5, null, null
    );
    assertThat(standardCategoryStatistics.hasMoreRules()).isFalse();
  }

  @Test
  public void hasMoreRules_is_updatable() {
    SecurityStandardCategoryStatistics standardCategoryStatistics = new SecurityStandardCategoryStatistics(
      "cat", 0, empty(), 0,
      0, 5, null, null
    );
    standardCategoryStatistics.setHasMoreRules(true);
    assertThat(standardCategoryStatistics.hasMoreRules()).isTrue();
  }

  @Test
  public void test_getters() {
    SecurityStandardCategoryStatistics standardCategoryStatistics = new SecurityStandardCategoryStatistics(
      "cat", 1, empty(), 0,
      0, 5, new ArrayList<>(), "version"
    ).setLevel("1");

    standardCategoryStatistics.setActiveRules(3);
    standardCategoryStatistics.setTotalRules(3);

    assertThat(standardCategoryStatistics.getCategory()).isEqualTo("cat");
    assertThat(standardCategoryStatistics.getVulnerabilities()).isEqualTo(1);
    assertThat(standardCategoryStatistics.getVulnerabilityRating()).isEmpty();
    assertThat(standardCategoryStatistics.getToReviewSecurityHotspots()).isZero();
    assertThat(standardCategoryStatistics.getReviewedSecurityHotspots()).isZero();
    assertThat(standardCategoryStatistics.getSecurityReviewRating()).isEqualTo(5);
    assertThat(standardCategoryStatistics.getChildren()).isEmpty();
    assertThat(standardCategoryStatistics.getActiveRules()).isEqualTo(3);
    assertThat(standardCategoryStatistics.getTotalRules()).isEqualTo(3);
    assertThat(standardCategoryStatistics.getVersion()).isPresent();
    assertThat(standardCategoryStatistics.getVersion().get()).contains("version");
    assertThat(standardCategoryStatistics.getLevel().get()).contains("1");
    assertThat(standardCategoryStatistics.hasMoreRules()).isFalse();
  }

}
