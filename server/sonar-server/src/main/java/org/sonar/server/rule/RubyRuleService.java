/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.rule;

import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.picocontainer.Startable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.es.SearchIdResult;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.paging.PagedResult;
import org.sonar.server.paging.PagingResult;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.user.UserSession;
import org.sonar.server.util.RubyUtils;

/**
 * Used through ruby code <pre>Internal.rules</pre>
 *
 * @deprecated in 4.4 because Ruby on Rails is deprecated too !
 */
@Deprecated
@ServerSide
public class RubyRuleService implements Startable {

  private final DbClient dbClient;
  private final RuleService service;
  private final RuleUpdater updater;
  private final UserSession userSession;

  public RubyRuleService(DbClient dbClient, RuleService service, RuleUpdater updater, UserSession userSession) {
    this.dbClient = dbClient;
    this.service = service;
    this.updater = updater;
    this.userSession = userSession;
  }

  /**
   * Used in issues_controller.rb and in manual_rules_controller.rb and in SQALE
   */
  @CheckForNull
  public RuleDto findByKey(String ruleKey) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return dbClient.ruleDao().selectByKey(dbSession, RuleKey.parse(ruleKey)).orNull();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  /**
   * Used in SQALE
   */
  public PagedResult<RuleDto> find(Map<String, Object> params) {
    RuleQuery query = new RuleQuery();
    query.setQueryText(Strings.emptyToNull((String) params.get("searchQuery")));
    query.setKey(Strings.emptyToNull((String) params.get("key")));
    query.setLanguages(RubyUtils.toStrings(params.get("languages")));
    query.setRepositories(RubyUtils.toStrings(params.get("repositories")));
    query.setSeverities(RubyUtils.toStrings(params.get("severities")));
    query.setStatuses(RubyUtils.toEnums(params.get("statuses"), RuleStatus.class));
    query.setTags(RubyUtils.toStrings(params.get("tags")));
    query.setSortField(RuleIndexDefinition.FIELD_RULE_NAME);
    String profile = Strings.emptyToNull((String) params.get("profile"));
    if (profile != null) {
      query.setQProfileKey(profile);
      query.setActivation(true);
    }

    SearchOptions options = new SearchOptions();
    Integer pageSize = RubyUtils.toInteger(params.get("pageSize"));
    int size = pageSize != null ? pageSize : 50;
    Integer page = RubyUtils.toInteger(params.get("p"));
    int pageIndex = page != null ? page : 1;
    options.setPage(pageIndex, size);
    SearchIdResult<RuleKey> result = service.search(query, options);
    List<RuleDto> ruleDtos = loadDtos(result.getIds());
    return new PagedResult<>(ruleDtos, PagingResult.create(options.getLimit(), pageIndex, result.getTotal()));
  }

  // sqale
  public void updateRule(Map<String, Object> params) {
    RuleUpdate update = RuleUpdate.createForPluginRule(RuleKey.parse((String) params.get("ruleKey")));
    String fn = (String) params.get("debtRemediationFunction");
    if (fn == null) {
      update.setDebtRemediationFunction(null);
    } else {
      update.setDebtRemediationFunction(new DefaultDebtRemediationFunction(
        DebtRemediationFunction.Type.valueOf(fn),
        Strings.emptyToNull((String) params.get("debtRemediationCoefficient")),
        Strings.emptyToNull((String) params.get("debtRemediationOffset"))));
    }
    updater.update(update, userSession);
  }

  private List<RuleDto> loadDtos(List<RuleKey> ruleKeys) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return dbClient.ruleDao().selectByKeys(dbSession, ruleKeys);
    } finally {
      dbClient.closeSession(dbSession);
    }
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
