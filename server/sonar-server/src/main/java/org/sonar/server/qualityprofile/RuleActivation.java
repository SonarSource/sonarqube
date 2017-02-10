/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import com.google.common.collect.Maps;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Map;

public class RuleActivation {

  private final RuleKey ruleKey;
  private final Map<String, String> parameters;
  private String severity = null;
  private boolean cascade = false;
  private boolean reset = false;

  public RuleActivation(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    this.parameters = Maps.newHashMap();
  }

  public RuleActivation(RuleActivation other) {
    this.ruleKey = other.ruleKey;
    this.parameters = Maps.newHashMap(other.parameters);
    this.severity = other.severity;
    this.reset = other.reset;
    this.cascade = other.cascade;
  }

  /**
   * For internal use
   */
  boolean isCascade() {
    return this.cascade;
  }

  /**
   * For internal use
   */
  RuleActivation setCascade(boolean b) {
    this.cascade = b;
    return this;
  }

  public RuleActivation setSeverity(@Nullable String s) {
    if (s != null && !Severity.ALL.contains(s)) {
      throw new IllegalArgumentException("Unknown severity: " + s);
    }
    this.severity = s;
    return this;
  }

  /**
   * Optional severity. Use the parent severity or default rule severity if null.
   */
  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  public RuleActivation setParameter(String key, @Nullable String value) {
    String sanitizedValue = Strings.emptyToNull(value);
    parameters.put(key, sanitizedValue);
    return this;
  }

  public RuleActivation setParameters(Map<String, String> m) {
    parameters.clear();
    for (Map.Entry<String, String> entry : m.entrySet()) {
      setParameter(entry.getKey(), entry.getValue());
    }
    return this;
  }

  public RuleKey getRuleKey() {
    return ruleKey;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }

  public boolean isReset() {
    return reset;
  }

  public RuleActivation setReset(boolean b) {
    this.reset = b;
    return this;
  }
}
