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

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import java.util.Optional;
import org.sonar.api.issue.impact.Severity;

public class ImpactSeverityMapper {
  private static final BiMap<Severity, String> SEVERITY_MAPPING = new ImmutableBiMap.Builder<Severity, String>()
    .put(Severity.INFO, org.sonar.api.rule.Severity.INFO)
    .put(Severity.LOW, org.sonar.api.rule.Severity.MINOR)
    .put(Severity.MEDIUM, org.sonar.api.rule.Severity.MAJOR)
    .put(Severity.HIGH, org.sonar.api.rule.Severity.CRITICAL)
    .put(Severity.BLOCKER, org.sonar.api.rule.Severity.BLOCKER)
    .build();

  private ImpactSeverityMapper() {

  }

  public static Severity mapImpactSeverity(String severity) {
    return Optional.ofNullable(SEVERITY_MAPPING.inverse().get(severity))
      .orElseThrow(() -> new IllegalArgumentException("Severity not supported: " + severity));
  }

  public static String mapRuleSeverity(Severity severity) {
    return Optional.ofNullable(SEVERITY_MAPPING.get(severity))
      .orElseThrow(() -> new IllegalArgumentException("Impact Severity not supported: " + severity));
  }

}
