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
package org.sonar.api.batch.rule.internal;

import com.google.common.collect.ImmutableMap;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.rule.RuleKey;

import javax.annotation.concurrent.Immutable;

import java.util.Map;

@Immutable
public class DefaultActiveRule implements ActiveRule {
  private final RuleKey ruleKey;
  private final String name;
  private final String severity, internalKey, language, templateRuleKey;
  private final Map<String, String> params;

  DefaultActiveRule(NewActiveRule newActiveRule) {
    this.severity = newActiveRule.severity;
    this.name = newActiveRule.name;
    this.internalKey = newActiveRule.internalKey;
    this.templateRuleKey = newActiveRule.templateRuleKey;
    this.ruleKey = newActiveRule.ruleKey;
    this.params = ImmutableMap.copyOf(newActiveRule.params);
    this.language = newActiveRule.language;
  }

  @Override
  public RuleKey ruleKey() {
    return ruleKey;
  }

  public String name() {
    return name;
  }

  @Override
  public String severity() {
    return severity;
  }

  @Override
  public String language() {
    return language;
  }

  @Override
  public String param(String key) {
    return params.get(key);
  }

  @Override
  public Map<String, String> params() {
    // already immutable
    return params;
  }

  @Override
  public String internalKey() {
    return internalKey;
  }

  @Override
  public String templateRuleKey() {
    return templateRuleKey;
  }
}
