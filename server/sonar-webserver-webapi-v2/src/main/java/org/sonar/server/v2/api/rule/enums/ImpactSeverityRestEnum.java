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
package org.sonar.server.v2.api.rule.enums;

import java.util.Arrays;
import org.sonar.api.issue.impact.Severity;

public enum ImpactSeverityRestEnum {
  INFO(Severity.INFO),
  LOW(Severity.LOW),
  MEDIUM(Severity.MEDIUM),
  HIGH(Severity.HIGH),
  BLOCKER(Severity.BLOCKER);

  private final Severity severity;

  ImpactSeverityRestEnum(Severity severity) {

    this.severity = severity;
  }

  public Severity getSeverity() {
    return severity;
  }

  public static ImpactSeverityRestEnum from(Severity severity) {
    return Arrays.stream(ImpactSeverityRestEnum.values())
      .filter(severityRestResponse -> severityRestResponse.severity.equals(severity))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unsupported impact severity: " + severity));
  }
}
