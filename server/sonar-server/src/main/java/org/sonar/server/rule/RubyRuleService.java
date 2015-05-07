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
import org.picocontainer.Startable;
import org.sonar.api.ServerSide;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.server.paging.PagedResult;
import org.sonar.server.paging.PagingResult;
import org.sonar.server.rule.index.RuleDoc;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.RubyUtils;

import javax.annotation.CheckForNull;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Used through ruby code <pre>Internal.rules</pre>
 *
 * @deprecated in 4.4 because Ruby on Rails is deprecated too !
 */
@Deprecated
@ServerSide
public class RubyRuleService implements Startable {

  private final RuleService service;
  private final RuleUpdater updater;

  public RubyRuleService(RuleService service, RuleUpdater updater) {
    this.service = service;
    this.updater = updater;
  }

  /**
   * Used in issues_controller.rb and in manual_rules_controller.rb and in SQALE
   */
  @CheckForNull
  public Rule findByKey(String ruleKey) {
    return service.getByKey(RuleKey.parse(ruleKey));
  }

  /**
   * Used in SQALE
   * If 'pageSize' params is set no -1, all rules are returned (using scrolling)
   */
  public PagedResult<Rule> find(Map<String, Object> params) {
    RuleQuery query = new RuleQuery();
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
    String profile = Strings.emptyToNull((String) params.get("profile"));
    if (profile != null) {
      query.setQProfileKey(profile);
      query.setActivation(true);
    }

    QueryContext options = new QueryContext();
    Integer pageSize = RubyUtils.toInteger(params.get("pageSize"));
    int size = pageSize != null ? pageSize : 50;
    if (size > -1) {
      Integer page = RubyUtils.toInteger(params.get("p"));
      int pageIndex = page != null ? page : 1;
      options.setPage(pageIndex, size);
      Result<Rule> result = service.search(query, options);
      return new PagedResult<>(result.getHits(), PagingResult.create(options.getLimit(), pageIndex, result.getTotal()));
    } else {
      List<Rule> rules = newArrayList(service.search(query, new QueryContext().setScroll(true)).scroll());
      return new PagedResult<>(rules, PagingResult.create(Integer.MAX_VALUE, 1, rules.size()));
    }
  }

  /**
   * Used in manual_rules_controller.rb
   */
  public List<Rule> searchManualRules() {
    return service.search(new RuleQuery().setRepositories(newArrayList(RuleDoc.MANUAL_REPOSITORY)).setSortField(RuleNormalizer.RuleField.NAME), new QueryContext()).getHits();
  }

  // sqale
  public void updateRule(Map<String, Object> params) {
    RuleUpdate update = RuleUpdate.createForPluginRule(RuleKey.parse((String) params.get("ruleKey")));
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

  /**
   * Used in manual_rules_controller.rb
   */
  public void createManualRule(Map<String, Object> params) {
    NewRule newRule = NewRule.createForManualRule((String) params.get("manualKey"))
      .setName((String) params.get("name"))
      .setMarkdownDescription((String) params.get("markdownDescription"));
    service.create(newRule);
  }

  /**
   * Used in manual_rules_controller.rb
   */
  public void updateManualRule(Map<String, Object> params) {
    RuleUpdate update = RuleUpdate.createForManualRule(RuleKey.parse((String) params.get("ruleKey")))
      .setName((String) params.get("name"))
      .setMarkdownDescription((String) params.get("markdownDescription"));
    service.update(update);
  }

  /**
   * Used in manual_rules_controller.rb
   */
  public void deleteManualRule(String ruleKey) {
    service.delete(RuleKey.parse(ruleKey));
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
