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
package org.sonar.server.qualityprofile;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.internal.ImpactMapper;
import org.sonar.core.rule.ImpactSeverityMapper;

/**
 * Class to map impact severity and rule severity during the override of severity of quality profile.
 * We want to keep the severities synchronized, if the rule type or the impacts severities are customized
 */
public class QProfileImpactSeverityMapper {

  private QProfileImpactSeverityMapper() {
  }

  public static Map<SoftwareQuality, Severity> mapImpactSeverities(@Nullable String severity, Map<SoftwareQuality, Severity> ruleImpacts, RuleType ruleType) {
    Map<SoftwareQuality, Severity> result = ruleImpacts.isEmpty() ? Map.of() : new EnumMap<>(ruleImpacts);
    if (severity == null || ruleImpacts.isEmpty()) {
      return result;
    }
    SoftwareQuality softwareQuality = ImpactMapper.convertToSoftwareQuality(ruleType);
    if (ruleImpacts.containsKey(softwareQuality)) {
      result.put(softwareQuality, ImpactSeverityMapper.mapImpactSeverity(severity));
    } else if (ruleImpacts.size() == 1) {
      result.replaceAll((sq, sev) -> ImpactSeverityMapper.mapImpactSeverity(severity));
    }
    return result;
  }

  @CheckForNull
  public static String mapSeverity(Map<SoftwareQuality, Severity> impacts, RuleType ruleType, @Nullable String ruleSeverity) {
    SoftwareQuality softwareQuality = ImpactMapper.convertToSoftwareQuality(ruleType);
    if (impacts.containsKey(softwareQuality)) {
      return ImpactSeverityMapper.mapRuleSeverity(impacts.get(softwareQuality));
    } else if (impacts.size() == 1) {
      return ImpactSeverityMapper.mapRuleSeverity(impacts.entrySet().iterator().next().getValue());
    }
    return ruleSeverity;
  }
}
