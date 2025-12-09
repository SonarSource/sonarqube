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
package org.sonar.ce.task.projectanalysis.issue;

import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImpactMapperTest {

  @Test
  void mapImpactSeverity_shouldReturnExpectedValue() {
    assertThat(ImpactMapper.mapImpactSeverity(ScannerReport.ImpactSeverity.ImpactSeverity_BLOCKER)).isEqualTo(Severity.BLOCKER);
    assertThat(ImpactMapper.mapImpactSeverity(ScannerReport.ImpactSeverity.ImpactSeverity_HIGH)).isEqualTo(Severity.HIGH);
    assertThat(ImpactMapper.mapImpactSeverity(ScannerReport.ImpactSeverity.ImpactSeverity_MEDIUM)).isEqualTo(Severity.MEDIUM);
    assertThat(ImpactMapper.mapImpactSeverity(ScannerReport.ImpactSeverity.ImpactSeverity_LOW)).isEqualTo(Severity.LOW);
    assertThat(ImpactMapper.mapImpactSeverity(ScannerReport.ImpactSeverity.ImpactSeverity_INFO)).isEqualTo(Severity.INFO);
  }

  @Test
  void mapImpactSeverity_whenUnknownValue_shouldThrowException() {
    assertThatThrownBy(() -> ImpactMapper.mapImpactSeverity(ScannerReport.ImpactSeverity.ImpactSeverity_UNKNOWN_IMPACT_SEVERITY)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ImpactMapper.mapImpactSeverity(ScannerReport.ImpactSeverity.UNRECOGNIZED)).isInstanceOf(IllegalArgumentException.class);
  }

}
