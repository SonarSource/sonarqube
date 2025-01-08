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
package org.sonar.core.rule;

import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.Severity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImpactSeverityMapperTest {

  @Test
  void mapImpactSeverity_shouldReturnExpectedValues() {

    assertThat(ImpactSeverityMapper.mapRuleSeverity(Severity.INFO)).isEqualTo(org.sonar.api.rule.Severity.INFO);
  }

  @Test
  void mapImpactSeverity_shouldThrowExceptionWhenValueIsUnknownOrNull() {

    assertThatThrownBy(() -> ImpactSeverityMapper.mapImpactSeverity("unknown")).isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Severity not supported: unknown");

    assertThatThrownBy(() -> ImpactSeverityMapper.mapImpactSeverity(null)).isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Severity not supported: null");
  }

  @Test
  void mapRuleSeverity_shouldReturnExpectedValues() {
    assertThat(ImpactSeverityMapper.mapImpactSeverity(org.sonar.api.rule.Severity.INFO)).isEqualTo(Severity.INFO);
  }

  @Test
  void mapRuleSeverity_shouldThrowExceptionWhenValueIsUnknownOrNull() {
    assertThatThrownBy(() -> ImpactSeverityMapper.mapRuleSeverity(null)).isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Impact Severity not supported: null");
  }
}
