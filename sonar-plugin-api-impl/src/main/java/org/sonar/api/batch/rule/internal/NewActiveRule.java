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
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;

/**
 * @since 4.2
 */
@Immutable
public class NewActiveRule {
  final RuleKey ruleKey;
  final String name;
  final String severity;
  final Map<String, String> params;
  final long createdAt;
  final long updatedAt;
  final String internalKey;
  final String language;
  final String templateRuleKey;
  final String qProfileKey;

  NewActiveRule(Builder builder) {
    this.ruleKey = builder.ruleKey;
    this.name = builder.name;
    this.severity = builder.severity;
    this.params = builder.params;
    this.createdAt = builder.createdAt;
    this.updatedAt = builder.updatedAt;
    this.internalKey = builder.internalKey;
    this.language = builder.language;
    this.templateRuleKey = builder.templateRuleKey;
    this.qProfileKey = builder.qProfileKey;
  }

  public RuleKey ruleKey() {
    return this.ruleKey;
  }

  public static class Builder {
    private RuleKey ruleKey;
    private String name;
    private String severity = Severity.defaultSeverity();
    private Map<String, String> params = new HashMap<>();
    private long createdAt;
    private long updatedAt;
    private String internalKey;
    private String language;
    private String templateRuleKey;
    private String qProfileKey;

    public Builder setRuleKey(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setSeverity(@Nullable String severity) {
      this.severity = StringUtils.defaultIfBlank(severity, Severity.defaultSeverity());
      return this;
    }

    public Builder setParam(String key, @Nullable String value) {
      // possible improvement : check that the param key exists in rule definition
      if (value == null) {
        params.remove(key);
      } else {
        params.put(key, value);
      }
      return this;
    }

    public Builder setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder setUpdatedAt(long updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public Builder setInternalKey(@Nullable String internalKey) {
      this.internalKey = internalKey;
      return this;
    }

    public Builder setLanguage(@Nullable String language) {
      this.language = language;
      return this;
    }

    public Builder setTemplateRuleKey(@Nullable String templateRuleKey) {
      this.templateRuleKey = templateRuleKey;
      return this;
    }

    public Builder setQProfileKey(String qProfileKey) {
      this.qProfileKey = qProfileKey;
      return this;
    }

    public NewActiveRule build() {
      return new NewActiveRule(this);
    }
  }
}
