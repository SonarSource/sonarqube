/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.api.rule.Severity;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Map;

public class RuleActivation {

  private final ActiveRuleKey key;
  private final Map<String, String> parameters = Maps.newHashMap();
  private String severity = null;
  private boolean cascade = false;

  public RuleActivation(ActiveRuleKey key) {
    this.key = key;
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
  RuleActivation isCascade(boolean b) {
    this.cascade = b;
    return this;
  }

  /**
   * For internal use
   */
  boolean isReset() {
    return severity == null && parameters.isEmpty();
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
    if (value == null) {
      parameters.remove(key);
    } else {
      parameters.put(key, sanitizedValue);
    }
    return this;
  }

  public RuleActivation setParameters(Map<String, String> m) {
    parameters.clear();
    parameters.putAll(m);
    return this;
  }

  public ActiveRuleKey getKey() {
    return key;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }
}
