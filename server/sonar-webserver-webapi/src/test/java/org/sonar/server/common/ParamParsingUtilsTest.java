/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.common;

import org.sonarsource.compliancereports.reports.ReportKey;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sonar.server.common.ParamParsingUtils.parseComplianceStandardsFilter;
import static org.sonar.server.common.ParamParsingUtils.parseImpact;

class ParamParsingUtilsTest {

  @Test
  void parseImpact_whenCorrectParam_ShouldReturnExpectedResult() {
    Pair<SoftwareQuality, Severity> result = parseImpact("MAINTAINABILITY=BLOCKER");
    assertEquals(SoftwareQuality.MAINTAINABILITY, result.getKey());
    assertEquals(Severity.BLOCKER, result.getValue());
  }

  @Test
  void parseImpact_whenInvalidParam_ShouldThrowException() {
    assertThatThrownBy(() -> parseImpact("MAINTAINABILITY"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid impact format: MAINTAINABILITY");
  }

  @Test
  void parseImpact_whenInvalidValues_ShouldThrowException() {
    assertThatThrownBy(() -> parseImpact("MAINTAINABILITY=MAJOR"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No enum constant org.sonar.api.issue.impact.Severity.MAJOR");
    assertThatThrownBy(() -> parseImpact("BUG=LOW"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No enum constant org.sonar.api.issue.impact.SoftwareQuality.BUG");
  }

  @Test
  void parseComplianceStandardsFilter_ShouldReturnExpectedResult() {
    Map<ReportKey, Set<String>> result = parseComplianceStandardsFilter("standard1:1=category1,category2&standard2:1=category3");
    assertThat(result).containsOnly(
      entry(new ReportKey("standard1", "1"), Set.of("category1", "category2")),
      entry(new ReportKey("standard2", "1"), Set.of("category3")));
  }

  @Test
  void parseComplianceStandardsFilter_WhenParamIsNullShouldReturnEmpty() {
    assertThat(parseComplianceStandardsFilter(null)).isEmpty();
  }

  @Test
  void parseComplianceStandardsFilter_WhenParamIsInvalidShouldThrowException() {
    assertThatThrownBy(() -> parseComplianceStandardsFilter("invalid"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid format: invalid");
  }
}

