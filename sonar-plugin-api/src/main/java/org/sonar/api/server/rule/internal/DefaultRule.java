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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RuleTagsToTypeConverter;
import org.sonar.api.server.rule.RulesDefinition;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;

@Immutable
public class DefaultRule extends RulesDefinition.Rule {
  private final String pluginKey;
  private final RulesDefinition.Repository repository;
  private final String repoKey;
  private final String key;
  private final String name;
  private final RuleType type;
  private final String htmlDescription;
  private final String markdownDescription;
  private final String internalKey;
  private final String severity;
  private final boolean template;
  private final DebtRemediationFunction debtRemediationFunction;
  private final String gapDescription;
  private final Set<String> tags;
  private final Set<String> securityStandards;
  private final Map<String, RulesDefinition.Param> params;
  private final RuleStatus status;
  private final boolean activatedByDefault;
  private final RuleScope scope;
  private final Set<RuleKey> deprecatedRuleKeys;

  DefaultRule(DefaultRepository repository, DefaultNewRule newRule) {
    this.pluginKey = newRule.pluginKey();
    this.repository = repository;
    this.repoKey = newRule.repoKey();
    this.key = newRule.key();
    this.name = newRule.name();
    this.htmlDescription = newRule.htmlDescription();
    this.markdownDescription = newRule.markdownDescription();
    this.internalKey = newRule.internalKey();
    this.severity = newRule.severity();
    this.template = newRule.template();
    this.status = newRule.status();
    this.debtRemediationFunction = newRule.debtRemediationFunction();
    this.gapDescription = newRule.gapDescription();
    this.scope = newRule.scope() == null ? RuleScope.MAIN : newRule.scope();
    this.type = newRule.type() == null ? RuleTagsToTypeConverter.convert(newRule.tags()) : newRule.type();
    Set<String> tagsBuilder = new TreeSet<>(newRule.tags());
    tagsBuilder.removeAll(RuleTagsToTypeConverter.RESERVED_TAGS);
    this.tags = Collections.unmodifiableSet(tagsBuilder);
    this.securityStandards = Collections.unmodifiableSet(new TreeSet<>(newRule.securityStandards()));
    Map<String, RulesDefinition.Param> paramsBuilder = new HashMap<>();
    for (RulesDefinition.NewParam newParam : newRule.paramsByKey().values()) {
      paramsBuilder.put(newParam.key(), new DefaultParam((DefaultNewParam) newParam));
    }
    this.params = Collections.unmodifiableMap(paramsBuilder);
    this.activatedByDefault = newRule.activatedByDefault();
    this.deprecatedRuleKeys = Collections.unmodifiableSet(new TreeSet<>(newRule.deprecatedRuleKeys()));
  }

  @Override
  public RulesDefinition.Repository repository() {
    return repository;
  }

  @Override
  @CheckForNull
  public String pluginKey() {
    return pluginKey;
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public RuleScope scope() {
    return scope;
  }

  @Override
  public RuleType type() {
    return type;
  }

  @Override
  public String severity() {
    return severity;
  }

  @Override
  @CheckForNull
  public String htmlDescription() {
    return htmlDescription;
  }

  @Override
  @CheckForNull
  public String markdownDescription() {
    return markdownDescription;
  }

  @Override
  public boolean template() {
    return template;
  }

  @Override
  public boolean activatedByDefault() {
    return activatedByDefault;
  }

  @Override
  public RuleStatus status() {
    return status;
  }

  @CheckForNull
  @Deprecated
  @Override
  public String debtSubCharacteristic() {
    return null;
  }

  @CheckForNull
  @Override
  public DebtRemediationFunction debtRemediationFunction() {
    return debtRemediationFunction;
  }

  @Deprecated
  @CheckForNull
  @Override
  public String effortToFixDescription() {
    return gapDescription();
  }

  @CheckForNull
  @Override
  public String gapDescription() {
    return gapDescription;
  }

  @CheckForNull
  @Override
  public RulesDefinition.Param param(String key) {
    return params.get(key);
  }

  @Override
  public List<RulesDefinition.Param> params() {
    return unmodifiableList(new ArrayList<>(params.values()));
  }

  @Override
  public Set<String> tags() {
    return tags;
  }

  @Override
  public Set<String> securityStandards() {
    return securityStandards;
  }

  @Override
  public Set<RuleKey> deprecatedRuleKeys() {
    return deprecatedRuleKeys;
  }

  @CheckForNull
  @Override
  public String internalKey() {
    return internalKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultRule other = (DefaultRule) o;
    return key.equals(other.key) && repoKey.equals(other.repoKey);
  }

  @Override
  public int hashCode() {
    int result = repoKey.hashCode();
    result = 31 * result + key.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return format("[repository=%s, key=%s]", repoKey, key);
  }
}

