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

package org.sonar.server.rule;

import com.google.common.collect.ImmutableList;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.server.rule.index.RuleDoc;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Will be removed in the future. Please use {@link org.sonar.server.rule.RuleService}
 */
public class DefaultRuleFinder implements RuleFinder {

  private final RuleIndex index;

  public DefaultRuleFinder(IndexClient indexes) {
    this.index = indexes.get(RuleIndex.class);
  }

  @Override
  @CheckForNull
  public org.sonar.api.rules.Rule findById(int ruleId) {
    Rule rule = index.getById(ruleId);
    if (rule != null && rule.status() != RuleStatus.REMOVED) {
      return toRule(rule);
    }
    return null;
  }

  public Collection<org.sonar.api.rules.Rule> findByIds(Collection<Integer> ruleIds) {
    List<org.sonar.api.rules.Rule> rules = newArrayList();
    if (ruleIds.isEmpty()) {
      return rules;
    }
    for (Rule rule : index.getByIds(ruleIds)) {
      rules.add(toRule(rule));
    }
    return rules;
  }

  public Collection<org.sonar.api.rules.Rule> findByKeys(Collection<RuleKey> ruleKeys) {
    List<org.sonar.api.rules.Rule> rules = newArrayList();
    if (ruleKeys.isEmpty()) {
      return rules;
    }
    for (Rule rule : index.getByKeys(ruleKeys)) {
      rules.add(toRule(rule));
    }
    return rules;
  }

  @Override
  @CheckForNull
  public org.sonar.api.rules.Rule findByKey(RuleKey key) {
    Rule rule = index.getNullableByKey(key);
    if (rule != null && rule.status() != RuleStatus.REMOVED) {
      return toRule(rule);
    } else {
      return null;
    }
  }

  @Override
  @CheckForNull
  public org.sonar.api.rules.Rule findByKey(String repositoryKey, String key) {
    return findByKey(RuleKey.of(repositoryKey, key));
  }

  @Override
  public final org.sonar.api.rules.Rule find(org.sonar.api.rules.RuleQuery query) {
    Result<Rule> result = index.search(toQuery(query), new QueryContext());
    if (!result.getHits().isEmpty()) {
      return toRule(result.getHits().get(0));
    } else {
      return null;
    }
  }

  @Override
  public final Collection<org.sonar.api.rules.Rule> findAll(org.sonar.api.rules.RuleQuery query) {
    List<org.sonar.api.rules.Rule> rules = newArrayList();
    for (Rule rule : index.search(toQuery(query), new QueryContext()).getHits()) {
      rules.add(toRule(rule));
    }
    return rules;
  }

  private org.sonar.api.rules.Rule toRule(Rule rule) {
    org.sonar.api.rules.Rule apiRule = new org.sonar.api.rules.Rule();
    apiRule
      .setName(rule.name())
      .setLanguage(rule.language())
      .setKey(rule.key().rule())
      .setConfigKey(rule.internalKey())
      .setIsTemplate(rule.isTemplate())
      .setCreatedAt(rule.createdAt())
      .setUpdatedAt(rule.updatedAt())
      .setDescription(rule.htmlDescription())
      .setRepositoryKey(rule.key().repository())
      .setSeverity(rule.severity() != null ? RulePriority.valueOf(rule.severity()) : null)
      .setStatus(rule.status().name())
      .setDefaultCharacteristicKey(rule.defaultDebtCharacteristicKey())
      .setDefaultSubCharacteristicKey(rule.defaultDebtSubCharacteristicKey())
      .setCharacteristicKey(rule.debtCharacteristicKey())
      .setSubCharacteristicKey(rule.debtSubCharacteristicKey())
      .setTags(rule.tags().toArray(new String[rule.tags().size()]))
      .setId(((RuleDoc) rule).id());

    List<org.sonar.api.rules.RuleParam> apiParams = newArrayList();
    for (RuleParam param : rule.params()) {
      apiParams.add(new org.sonar.api.rules.RuleParam(apiRule, param.key(), param.description(), param.type().type())
        .setDefaultValue(param.defaultValue()));
    }
    apiRule.setParams(apiParams);

    return apiRule;
  }

  private RuleQuery toQuery(org.sonar.api.rules.RuleQuery apiQuery) {
    RuleQuery query = new RuleQuery();
    if (apiQuery.getConfigKey() != null) {
      query.setInternalKey(apiQuery.getConfigKey());
    }
    if (apiQuery.getKey() != null) {
      query.setRuleKey(apiQuery.getKey());
    }
    if (apiQuery.getRepositoryKey() != null) {
      query.setRepositories(ImmutableList.of(apiQuery.getRepositoryKey()));
    }
    return query;
  }
}
