/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QProfileChangeDto;

public class ActiveRuleChange {

  public enum Type {
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

  public ActiveRuleChange setSeverity(@Nullable String s) {
    this.severity = s;
    return this;
  }

  public ActiveRuleChange setInheritance(@Nullable ActiveRule.Inheritance i) {
    this.inheritance = i;
    return this;
  }

  @CheckForNull
  public ActiveRule.Inheritance getInheritance() {
    return inheritance;
  }

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

  public QProfileChangeDto toDto(@Nullable String login) {
    QProfileChangeDto dto = new QProfileChangeDto();
    dto.setChangeType(type.name());
    dto.setProfileKey(getKey().qProfile());
    dto.setLogin(login);
    Map<String, String> data = new HashMap<>();
    data.put("key", getKey().toString());
    data.put("ruleKey", getKey().ruleKey().toString());

    parameters.entrySet().stream()
      .filter(param -> !param.getKey().isEmpty())
      .forEach(param -> data.put("param_" + param.getKey(), param.getValue()));

    if (StringUtils.isNotEmpty(severity)) {
      data.put("severity", severity);
    }
    if (inheritance != null) {
      data.put("inheritance", inheritance.name());
    }
    dto.setData(data);
    return dto;
  }

  public static ActiveRuleChange createFor(Type type, ActiveRuleKey key) {
    return new ActiveRuleChange(type, key);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("type", type)
      .add("key", key)
      .add("severity", severity)
      .add("inheritance", inheritance)
      .add("parameters", parameters)
      .toString();
  }
}
