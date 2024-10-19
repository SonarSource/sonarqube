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
package org.sonar.server.issue;

import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.Severity;
import org.sonarqube.ws.Common;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImpactFormatterTest {

  @Test
  void mapImpactSeverity_shouldReturnExpectedValue() {
    assertEquals(Common.ImpactSeverity.ImpactSeverity_BLOCKER, ImpactFormatter.mapImpactSeverity(Severity.BLOCKER));
    assertEquals(Common.ImpactSeverity.HIGH, ImpactFormatter.mapImpactSeverity(Severity.HIGH));
    assertEquals(Common.ImpactSeverity.MEDIUM, ImpactFormatter.mapImpactSeverity(Severity.MEDIUM));
    assertEquals(Common.ImpactSeverity.LOW, ImpactFormatter.mapImpactSeverity(Severity.LOW));
    assertEquals(Common.ImpactSeverity.ImpactSeverity_INFO, ImpactFormatter.mapImpactSeverity(Severity.INFO));
  }
}
