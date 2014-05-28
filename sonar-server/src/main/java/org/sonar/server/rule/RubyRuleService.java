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
import com.google.common.collect.Lists;
import org.picocontainer.Startable;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.server.paging.PagedResult;
import org.sonar.server.paging.PagingResult;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.rule.index.RuleResult;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.RubyUtils;

import javax.annotation.CheckForNull;
import java.util.List;
import java.util.Map;

/**
 * Used through ruby code <pre>Internal.rules</pre>
 *
 * @deprecated in 4.4 because Ruby on Rails is deprecated too !
 */
@Deprecated
public class RubyRuleService implements ServerComponent, Startable {

  private final RuleService service;
  private final RuleUpdater updater;

  public RubyRuleService(RuleService service, RuleUpdater updater) {
    this.service = service;
    this.updater = updater;
  }

  /**
   * Used in issues_controller.rb
   */
  @CheckForNull
  public org.sonar.server.rule.Rule findByKey(String ruleKey) {
    return service.getByKey(RuleKey.parse(ruleKey));
  }

  /**
   * Used in SQALE
   */
  public PagedResult<org.sonar.server.rule.Rule> find(Map<String, Object> params) {
    RuleQuery query = service.newRuleQuery();
    query.setQueryText(Strings.emptyToNull((String) params.get("searchQuery")));
    query.setKey(Strings.emptyToNull((String) params.get("key")));
    query.setLanguages(RubyUtils.toStrings(params.get("languages")));
    query.setRepositories(RubyUtils.toStrings(params.get("repositories")));
    query.setSeverities(RubyUtils.toStrings(params.get("severities")));
    query.setStatuses(RubyUtils.toEnums(params.get("statuses"), RuleStatus.class));
    query.setTags(RubyUtils.toStrings(params.get("tags")));
    query.setDebtCharacteristics(RubyUtils.toStrings(params.get("debtCharacteristics")));
    query.setHasDebtCharacteristic(RubyUtils.toBoolean(params.get("hasDebtCharacteristic")));
    query.setSortField(RuleNormalizer.RuleField.NAME);

    QueryOptions options = new QueryOptions();
    options.setLimit(QueryOptions.MAX_LIMIT);

    List<Rule> rules = Lists.newArrayList();
    boolean more = true;
    int offset = 0;
    while (more) {
      options.setOffset(offset);
      RuleResult result = service.search(query, options);
      rules.addAll(result.getRules());
      if (result.getHits().size() < QueryOptions.MAX_LIMIT) {
        more = false;
      } else {
        offset += QueryOptions.MAX_LIMIT;
      }
    }
    return new PagedResult<org.sonar.server.rule.Rule>(rules, PagingResult.create(rules.size(), 1, rules.size()));
  }

  // sqale
  public void updateRule(Map<String, Object> params) {
    RuleUpdate update = new RuleUpdate(RuleKey.parse((String) params.get("ruleKey")));
    update.setDebtSubCharacteristic(Strings.emptyToNull((String) params.get("debtCharacteristicKey")));
    String fn = (String) params.get("debtRemediationFunction");
    if (fn == null) {
      update.setDebtRemediationFunction(null);
    } else {
      update.setDebtRemediationFunction(new DefaultDebtRemediationFunction(
          DebtRemediationFunction.Type.valueOf(fn),
          Strings.emptyToNull((String) params.get("debtRemediationCoefficient")),
          Strings.emptyToNull((String) params.get("debtRemediationOffset")))
      );
    }
    updater.update(update, UserSession.get());
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
