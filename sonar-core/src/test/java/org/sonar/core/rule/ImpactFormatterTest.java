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
package org.sonar.core.rule;

import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonarqube.ws.Common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImpactFormatterTest {

  @Test
  void mapImpactSeverity_whenMappingStandardSeverity_shouldReturnExpectedValue() {
    assertThat(ImpactFormatter.mapImpactSeverity(Severity.BLOCKER)).isEqualTo(Common.ImpactSeverity.ImpactSeverity_BLOCKER);
    assertThat(ImpactFormatter.mapImpactSeverity(Severity.HIGH)).isEqualTo(Common.ImpactSeverity.HIGH);
    assertThat(ImpactFormatter.mapImpactSeverity(Severity.MEDIUM)).isEqualTo(Common.ImpactSeverity.MEDIUM);
    assertThat(ImpactFormatter.mapImpactSeverity(Severity.LOW)).isEqualTo(Common.ImpactSeverity.LOW);
    assertThat(ImpactFormatter.mapImpactSeverity(Severity.INFO)).isEqualTo(Common.ImpactSeverity.ImpactSeverity_INFO);
  }

  @Test
  void mapImpactSeverity_whenMappingProtobufSeverity_shouldReturnExpectedValue() {
    assertThat(ImpactFormatter.mapImpactSeverity(Common.ImpactSeverity.ImpactSeverity_BLOCKER)).isEqualTo(Severity.BLOCKER);
    assertThat(ImpactFormatter.mapImpactSeverity(Common.ImpactSeverity.HIGH)).isEqualTo(Severity.HIGH);
    assertThat(ImpactFormatter.mapImpactSeverity(Common.ImpactSeverity.MEDIUM)).isEqualTo(Severity.MEDIUM);
    assertThat(ImpactFormatter.mapImpactSeverity(Common.ImpactSeverity.LOW)).isEqualTo(Severity.LOW);
    assertThat(ImpactFormatter.mapImpactSeverity(Common.ImpactSeverity.ImpactSeverity_INFO)).isEqualTo(Severity.INFO);
    assertThatThrownBy(() -> ImpactFormatter.mapImpactSeverity(Common.ImpactSeverity.UNKNOWN_IMPACT_SEVERITY))
      .isInstanceOf(UnsupportedOperationException.class);
  }
}
