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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.ServerComponent;
import org.sonar.server.paging.PagedResult;
import org.sonar.server.util.RubyUtils;

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

  /**
   * No more used
   */
  public void saveOrUpdate(int ruleId) {
    ruleRegistry.saveOrUpdate(ruleId);
  }

  public PagedResult<Rule> find(Map<String, Object> params){
    return rules.find(toRuleQuery(params));
  }

  @VisibleForTesting
  static RuleQuery toRuleQuery(Map<String, Object> props) {
    RuleQuery.Builder builder = RuleQuery.builder()
      .searchQuery(Strings.emptyToNull((String) props.get("searchQuery")))
      .key(Strings.emptyToNull((String) props.get("key")))
      .languages(RubyUtils.toStrings(props.get("languages")))
      .repositories(RubyUtils.toStrings(props.get("repositories")))
      .severities(RubyUtils.toStrings(props.get("severities")))
      .statuses(RubyUtils.toStrings(props.get("statuses")))
      .tags(RubyUtils.toStrings(props.get("tags")))
      .debtCharacteristics(RubyUtils.toStrings(props.get("debtCharacteristics")))
      .hasDebtCharacteristic(RubyUtils.toBoolean(props.get("hasDebtCharacteristic")))
      .pageSize(RubyUtils.toInteger(props.get("pageSize")))
      .pageIndex(RubyUtils.toInteger(props.get("pageIndex")));
    return builder.build();
  }

  public void updateRuleNote(int ruleId, String note) {
    rules.updateRuleNote(ruleId, note);
  }

  public Integer createRule(int ruleId, @Nullable String name, @Nullable String severity, @Nullable String description, Map<String, String> paramsByKey) {
    return rules.createRule(ruleId, name, severity, description, paramsByKey);
  }

  public void updateRule(int ruleId, @Nullable String name, @Nullable String severity, @Nullable String description, Map<String, String> paramsByKey) {
    rules.updateRule(ruleId, name, severity, description, paramsByKey);
  }

  public void deleteRule(int ruleId) {
    rules.deleteRule(ruleId);
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
