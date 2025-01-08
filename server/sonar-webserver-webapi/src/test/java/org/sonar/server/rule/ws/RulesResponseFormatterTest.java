/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.rule.ws;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Rules;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.sonar.server.rule.ws.RulesResponseFormatter.mapImpacts;

class RulesResponseFormatterTest {

  @ParameterizedTest
  @MethodSource("impacts")
  void mapImpacts_shouldReturnExpectedValue(Map<SoftwareQuality, Severity> impacts, Rules.Impacts expected) {
    assertThat(mapImpacts(impacts)).isEqualTo(expected);
  }

  private static Stream<Arguments> impacts() {
    return Stream.of(
      Arguments.of(Map.of(SoftwareQuality.SECURITY, Severity.BLOCKER),
        Rules.Impacts.newBuilder().addImpacts(Common.Impact.newBuilder()
          .setSoftwareQuality(Common.SoftwareQuality.SECURITY)
          .setSeverity(Common.ImpactSeverity.ImpactSeverity_BLOCKER)).build()),
      Arguments.of(Map.of(SoftwareQuality.RELIABILITY, Severity.INFO),
        Rules.Impacts.newBuilder().addImpacts(Common.Impact.newBuilder()
          .setSoftwareQuality(Common.SoftwareQuality.RELIABILITY)
          .setSeverity(Common.ImpactSeverity.ImpactSeverity_INFO)).build()),
      Arguments.of(Map.of(SoftwareQuality.MAINTAINABILITY, Severity.MEDIUM),
        Rules.Impacts.newBuilder().addImpacts(Common.Impact.newBuilder()
          .setSoftwareQuality(Common.SoftwareQuality.MAINTAINABILITY)
          .setSeverity(Common.ImpactSeverity.MEDIUM)).build()));
  }
}
