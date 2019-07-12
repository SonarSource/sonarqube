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
package org.sonar.api.server.rule.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;

import static org.sonar.api.utils.Preconditions.checkArgument;

public class DefaultNewRepository implements RulesDefinition.NewRepository {
  private final RulesDefinition.Context context;
  private final String key;
  private final boolean isExternal;
  private final String language;
  private String name;
  private final Map<String, RulesDefinition.NewRule> newRules = new HashMap<>();

  public DefaultNewRepository(RulesDefinition.Context context, String key, String language, boolean isExternal) {
    this.context = context;
    this.key = key;
    this.name = key;
    this.language = language;
    this.isExternal = isExternal;
  }

  @Override
  public boolean isExternal() {
    return isExternal;
  }

  @Override
  public String key() {
    return key;
  }

  public String language() {
    return language;
  }

  public Map<String, RulesDefinition.NewRule> newRules() {
    return newRules;
  }

  public String name() {
    return name;
  }

  @Override
  public DefaultNewRepository setName(@Nullable String s) {
    if (StringUtils.isNotEmpty(s)) {
      this.name = s;
    }
    return this;
  }

  @Override
  public RulesDefinition.NewRule createRule(String ruleKey) {
    checkArgument(!newRules.containsKey(ruleKey), "The rule '%s' of repository '%s' is declared several times", ruleKey, key);
    RulesDefinition.NewRule newRule = new DefaultNewRule(context.currentPluginKey(), key, ruleKey);
    newRules.put(ruleKey, newRule);
    return newRule;
  }

  @CheckForNull
  @Override
  public RulesDefinition.NewRule rule(String ruleKey) {
    return newRules.get(ruleKey);
  }

  @Override
  public Collection<RulesDefinition.NewRule> rules() {
    return newRules.values();
  }

  @Override
  public void done() {
    // note that some validations can be done here, for example for
    // verifying that at least one rule is declared

    context.registerRepository(this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("NewRepository{");
    sb.append("key='").append(key).append('\'');
    sb.append(", language='").append(language).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
