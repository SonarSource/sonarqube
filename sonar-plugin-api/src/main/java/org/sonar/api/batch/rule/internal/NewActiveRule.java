/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

/**
 * @since 4.2
 */
public class NewActiveRule {
  final RuleKey ruleKey;
  String name;
  String severity = Severity.defaultSeverity();
  Map<String, String> params = new HashMap<>();
  long createdAt;
  String internalKey;
  String language;
  String templateRuleKey;
  private final ActiveRulesBuilder builder;

  NewActiveRule(ActiveRulesBuilder builder, RuleKey ruleKey) {
    this.builder = builder;
    this.ruleKey = ruleKey;
  }

  public NewActiveRule setName(String name) {
    this.name = name;
    return this;
  }

  public NewActiveRule setSeverity(@Nullable String severity) {
    this.severity = StringUtils.defaultIfBlank(severity, Severity.defaultSeverity());
    return this;
  }

  public NewActiveRule setInternalKey(@Nullable String internalKey) {
    this.internalKey = internalKey;
    return this;
  }

  public NewActiveRule setTemplateRuleKey(@Nullable String templateRuleKey) {
    this.templateRuleKey = templateRuleKey;
    return this;
  }

  public NewActiveRule setLanguage(@Nullable String language) {
    this.language = language;
    return this;
  }

  public NewActiveRule setParam(String key, @Nullable String value) {
    // possible improvement : check that the param key exists in rule definition
    if (value == null) {
      params.remove(key);
    } else {
      params.put(key, value);
    }
    return this;
  }

  public Map<String, String> params() {
    return params;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  public ActiveRulesBuilder activate() {
    builder.activate(this);
    return builder;
  }
}
