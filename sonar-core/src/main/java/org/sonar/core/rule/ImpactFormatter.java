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

import org.sonar.api.issue.impact.Severity;
import org.sonarqube.ws.Common;

import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.Severity.INFO;
import static org.sonar.api.issue.impact.Severity.LOW;
import static org.sonar.api.issue.impact.Severity.MEDIUM;

public class ImpactFormatter {
  private ImpactFormatter() {
  }

  public static Common.ImpactSeverity mapImpactSeverity(Severity severity) {
    return switch (severity) {
      case BLOCKER -> Common.ImpactSeverity.ImpactSeverity_BLOCKER;
      case HIGH -> Common.ImpactSeverity.HIGH;
      case MEDIUM -> Common.ImpactSeverity.MEDIUM;
      case LOW -> Common.ImpactSeverity.LOW;
      case INFO -> Common.ImpactSeverity.ImpactSeverity_INFO;
    };
  }

  public static Severity mapImpactSeverity(Common.ImpactSeverity severity) {
    return switch (severity) {
      case ImpactSeverity_BLOCKER -> BLOCKER;
      case HIGH -> HIGH;
      case MEDIUM -> MEDIUM;
      case LOW -> LOW;
      case ImpactSeverity_INFO -> INFO;
      case UNKNOWN_IMPACT_SEVERITY -> throw new UnsupportedOperationException("Impact severity not supported");
    };
  }

}
