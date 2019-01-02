/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.Severity;

/**
 * The request for activation.
 */
@Immutable
public class RuleActivation {

  private final int ruleId;
  private final boolean reset;
  private final String severity;
  private final Map<String, String> parameters = new HashMap<>();

  private RuleActivation(int ruleId, boolean reset, @Nullable String severity, @Nullable Map<String, String> parameters) {
    this.ruleId = ruleId;
    this.reset = reset;
    this.severity = severity;
    if (severity != null && !Severity.ALL.contains(severity)) {
      throw new IllegalArgumentException("Unknown severity: " + severity);
    }
    if (parameters != null) {
      for (Map.Entry<String, String> entry : parameters.entrySet()) {
        this.parameters.put(entry.getKey(), Strings.emptyToNull(entry.getValue()));
      }
    }
  }

  public static RuleActivation createReset(int ruleId) {
    return new RuleActivation(ruleId, true, null, null);
  }

  public static RuleActivation create(int ruleId, @Nullable String severity, @Nullable Map<String, String> parameters) {
    return new RuleActivation(ruleId, false, severity, parameters);
  }

  public static RuleActivation create(int ruleId) {
    return create(ruleId, null, null);
  }

  /**
   * Optional severity. Use the parent severity or default rule severity if null.
   */
  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  public int getRuleId() {
    return ruleId;
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
}
