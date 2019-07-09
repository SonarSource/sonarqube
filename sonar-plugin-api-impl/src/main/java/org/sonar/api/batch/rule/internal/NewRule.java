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
package org.sonar.api.batch.rule.internal;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;

public class NewRule {

  private static final String DEFAULT_SEVERITY = Severity.defaultSeverity();

  final RuleKey key;
  Integer id;
  String name;
  String description;
  String severity = DEFAULT_SEVERITY;
  String type;
  String internalKey;
  RuleStatus status = RuleStatus.defaultStatus();
  Map<String, NewRuleParam> params = new HashMap<>();

  public NewRule(RuleKey key) {
    this.key = key;
  }

  public NewRule setId(@Nullable Integer id) {
    this.id = id;
    return this;
  }

  public NewRule setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public NewRule setName(@Nullable String s) {
    this.name = s;
    return this;
  }

  public NewRule setSeverity(@Nullable String severity) {
    this.severity = StringUtils.defaultIfBlank(severity, DEFAULT_SEVERITY);
    return this;
  }
  
  public NewRule setType(@Nullable String type) {
    this.type = type;
    return this;
  }

  public NewRule setStatus(@Nullable RuleStatus s) {
    this.status = (RuleStatus) ObjectUtils.defaultIfNull(s, RuleStatus.defaultStatus());
    return this;
  }

  public NewRule setInternalKey(@Nullable String s) {
    this.internalKey = s;
    return this;
  }

  public NewRuleParam addParam(String paramKey) {
    if (params.containsKey(paramKey)) {
      throw new IllegalStateException(String.format("Parameter '%s' already exists on rule '%s'", paramKey, key));
    }
    NewRuleParam param = new NewRuleParam(paramKey);
    params.put(paramKey, param);
    return param;
  }
}
