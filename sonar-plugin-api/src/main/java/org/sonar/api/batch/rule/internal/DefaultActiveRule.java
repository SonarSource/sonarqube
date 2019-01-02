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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.rule.RuleKey;

@Immutable
public class DefaultActiveRule implements ActiveRule {
  private final RuleKey ruleKey;
  private final String severity;
  private final String internalKey;
  private final String language;
  private final String templateRuleKey;
  private final Map<String, String> params;
  private final long createdAt;
  private final long updatedAt;
  private final String qProfileKey;

  DefaultActiveRule(NewActiveRule newActiveRule) {
    this.severity = newActiveRule.severity;
    this.internalKey = newActiveRule.internalKey;
    this.templateRuleKey = newActiveRule.templateRuleKey;
    this.ruleKey = newActiveRule.ruleKey;
    this.params = Collections.unmodifiableMap(new HashMap<>(newActiveRule.params));
    this.language = newActiveRule.language;
    this.createdAt = newActiveRule.createdAt;
    this.updatedAt = newActiveRule.updatedAt;
    this.qProfileKey = newActiveRule.qProfileKey;
  }

  @Override
  public RuleKey ruleKey() {
    return ruleKey;
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

  public long createdAt() {
    return createdAt;
  }

  public long updatedAt() {
    return updatedAt;
  }

  @Override
  public String qpKey() {
    return qProfileKey;
  }
}
