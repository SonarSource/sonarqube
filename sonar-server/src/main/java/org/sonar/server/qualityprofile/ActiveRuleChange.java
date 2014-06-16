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

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.core.activity.ActivityLog;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class ActiveRuleChange implements ActivityLog {

  static enum Type {
    ACTIVATED, DEACTIVATED, UPDATED
  }

  private final Type type;
  private final ActiveRuleKey key;
  private String severity = null;
  private ActiveRule.Inheritance inheritance = null;
  private Map<String, String> parameters = Maps.newHashMap();

  private ActiveRuleChange(Type type, ActiveRuleKey key) {
    this.type = type;
    this.key = key;
  }

  public ActiveRuleKey getKey() {
    return key;
  }

  public Type getType() {
    return type;
  }

  @CheckForNull
  public String getSeverity() {
    return severity;
  }

  public ActiveRuleChange setSeverity(@Nullable String severity) {
    this.severity = severity;
    return this;
  }

  public ActiveRuleChange setInheritance(@Nullable ActiveRule.Inheritance inheritance) {
    this.inheritance = inheritance;
    return this;
  }

  @CheckForNull
  public ActiveRule.Inheritance getInheritance() {
    return this.inheritance;
  }

  @CheckForNull
  public Map<String, String> getParameters() {
    return parameters;
  }

  public ActiveRuleChange setParameter(String key, @Nullable String value) {
    parameters.put(key, value);
    return this;
  }

  public ActiveRuleChange setParameters(Map<String, String> m) {
    parameters.clear();
    parameters.putAll(m);
    return this;
  }

  @Override
  public Map<String, String> getDetails() {
    HashMap<String, String> details = new HashMap<String, String>();
    if (getType() != null) {
      details.put("type", getType().name());
    }
    if (getKey() != null) {
      details.put("key", getKey().toString());
      details.put("ruleKey", getKey().ruleKey().toString());
      details.put("profileKey", getKey().qProfile().toString());
    }
    if (!parameters.isEmpty()) {
      for (Map.Entry<String, String> param : parameters.entrySet()) {
        if (!param.getKey().isEmpty()) {
          details.put("param_" + param.getKey(), param.getValue());
        }
      }
    }
    if (StringUtils.isNotEmpty(severity)) {
      details.put("severity", severity);
    }
    if (inheritance != null) {
      details.put("inheritance", inheritance.name());
    }
    return details;
  }

  public static ActiveRuleChange createFor(Type type, ActiveRuleKey key) {
    return new ActiveRuleChange(type, key);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("type", type)
      .add("key", key)
      .add("severity", severity)
      .add("inheritance", inheritance)
      .add("parameters", parameters)
      .toString();
  }
}
