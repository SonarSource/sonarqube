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

import com.google.common.base.Strings;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.Severity;

/**
 * The request for activation.
 */
@Immutable
public class RuleActivation {

  private final String ruleUuid;
  private final boolean reset;
  private final String severity;
  private final Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impactSeverities;
  private final Boolean prioritizedRule;
  private final Map<String, String> parameters = new HashMap<>();

  private RuleActivation(String ruleUuid, boolean reset, @Nullable String severity, @Nullable Boolean prioritizedRule, @Nullable Map<String, String> parameters,
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impactSeverities) {
    this.ruleUuid = ruleUuid;
    this.reset = reset;
    this.severity = severity;
    this.prioritizedRule = prioritizedRule;
    this.impactSeverities = impactSeverities.isEmpty() ? Map.of() : new EnumMap<>(impactSeverities);
    if (severity != null && !Severity.ALL.contains(severity)) {
      throw new IllegalArgumentException("Unknown severity: " + severity);
    }
    if (parameters != null) {
      for (Map.Entry<String, String> entry : parameters.entrySet()) {
        this.parameters.put(entry.getKey(), Strings.emptyToNull(entry.getValue()));
      }
    }
  }

  public static RuleActivation createReset(String ruleUuid) {
    return new RuleActivation(ruleUuid, true, null, null, null, Map.of());
  }

  public static RuleActivation create(String ruleUuid, @Nullable String severity, @Nullable Boolean prioritizedRule,
    @Nullable Map<String, String> parameters) {
    return new RuleActivation(ruleUuid, false, severity, prioritizedRule, parameters, Map.of());
  }

  public static RuleActivation create(String ruleUuid, @Nullable String severity, @Nullable Map<SoftwareQuality,
    org.sonar.api.issue.impact.Severity> impactSeverities, @Nullable Boolean prioritizedRule, @Nullable Map<String, String> parameters) {
    if (impactSeverities == null) {
      impactSeverities = Map.of();
    }
    return new RuleActivation(ruleUuid, false, severity, prioritizedRule, parameters, impactSeverities);
  }

  public static RuleActivation create(String ruleUuid, @Nullable String severity, @Nullable Map<String, String> parameters) {
    return create(ruleUuid, severity, null, parameters);
  }

  public static RuleActivation createOverrideImpacts(String ruleUuid, Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impactSeverities,
    @Nullable Map<String, String> parameters) {
    return new RuleActivation(ruleUuid, false, null, null, parameters, impactSeverities);
  }

  public static RuleActivation create(String ruleUuid) {
    return create(ruleUuid, null, null, null);
  }

  /**
   * Optional severity. Use the parent severity or default rule severity if null.
   */
  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  public Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> getImpactSeverities() {
    return impactSeverities;
  }

  public String getRuleUuid() {
    return ruleUuid;
  }

  @CheckForNull
  public String getParameter(String key) {
    return parameters.get(key);
  }

  public boolean hasParameter(String key) {
    return parameters.containsKey(key);
  }

  public boolean isReset() {
    return reset;
  }

  @CheckForNull
  public Boolean isPrioritizedRule() {
    return prioritizedRule;
  }
}
