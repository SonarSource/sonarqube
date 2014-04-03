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

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.paging.PagedResult;
import org.sonar.server.util.RubyUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Map;

/**
 * Used through ruby code <pre>Internal.rules</pre>
 */
public class RubyRuleService implements ServerComponent, Startable {

  private final Rules rules;
  private final RuleRegistry ruleRegistry;

  private static final String OPTIONS_STATUS = "status";
  private static final String OPTIONS_LANGUAGE = "language";

  public RubyRuleService(Rules rules, RuleRegistry ruleRegistry) {
    this.rules = rules;
    this.ruleRegistry = ruleRegistry;
  }

  public Integer[] findIds(Map<String, String> options) {
    Map<String, String> params = Maps.newHashMap();
    translateNonBlankKey(options, params, OPTIONS_STATUS, OPTIONS_STATUS);
    translateNonBlankKey(options, params, "repositories", "repositoryKey");
    translateNonBlankKey(options, params, OPTIONS_LANGUAGE, OPTIONS_LANGUAGE);
    translateNonBlankKey(options, params, "searchtext", "nameOrKey");
    return ruleRegistry.findIds(params).toArray(new Integer[0]);
  }

  private static void translateNonBlankKey(Map<String, String> options, Map<String, String> params, String optionKey, String paramKey) {
    if (options.get(optionKey) != null && StringUtils.isNotBlank(options.get(optionKey).toString())) {
      params.put(paramKey, options.get(optionKey).toString());
    }
  }

  @CheckForNull
  public Rule findByKey(String ruleKey) {
    return rules.findByKey(RuleKey.parse(ruleKey));
  }

  public PagedResult<Rule> find(Map<String, Object> params) {
    return rules.find(RuleQuery.builder()
      .searchQuery(Strings.emptyToNull((String) params.get("searchQuery")))
      .key(Strings.emptyToNull((String) params.get("key")))
      .languages(RubyUtils.toStrings(params.get("languages")))
      .repositories(RubyUtils.toStrings(params.get("repositories")))
      .severities(RubyUtils.toStrings(params.get("severities")))
      .statuses(RubyUtils.toStrings(params.get("statuses")))
      .tags(RubyUtils.toStrings(params.get("tags")))
      .debtCharacteristics(RubyUtils.toStrings(params.get("debtCharacteristics")))
      .hasDebtCharacteristic(RubyUtils.toBoolean(params.get("hasDebtCharacteristic")))
      .pageSize(RubyUtils.toInteger(params.get("pageSize")))
      .pageIndex(RubyUtils.toInteger(params.get("pageIndex"))).build());
  }

  public void updateRule(Map<String, Object> params) {
    rules.updateRule(new RuleOperations.RuleChange()
      .setRuleKey(RuleKey.parse((String) params.get("ruleKey")))
      .setDebtCharacteristicKey(Strings.emptyToNull((String) params.get("debtCharacteristicKey")))
      .setDebtRemediationFunction((String) params.get("debtRemediationFunction"))
      .setDebtRemediationCoefficient(Strings.emptyToNull((String) params.get("debtRemediationCoefficient")))
      .setDebtRemediationOffset(Strings.emptyToNull((String) params.get("debtRemediationOffset"))));
  }

  public void updateRuleNote(int ruleId, String note) {
    rules.updateRuleNote(ruleId, note);
  }

  public Integer createCustomRule(int ruleId, @Nullable String name, @Nullable String severity, @Nullable String description, Map<String, String> paramsByKey) {
    return rules.createCustomRule(ruleId, name, severity, description, paramsByKey);
  }

  public void updateCustomRule(int ruleId, @Nullable String name, @Nullable String severity, @Nullable String description, Map<String, String> paramsByKey) {
    rules.updateCustomRule(ruleId, name, severity, description, paramsByKey);
  }

  public void deleteCustomRule(int ruleId) {
    rules.deleteCustomRule(ruleId);
  }

  public void updateRuleTags(int ruleId, Object tags) {
    rules.updateRuleTags(ruleId, tags);
  }

  @Override
  public void start() {
    // used to force pico to instantiate the singleton at startup
  }

  @Override
  public void stop() {
    // implement startable
  }
}
