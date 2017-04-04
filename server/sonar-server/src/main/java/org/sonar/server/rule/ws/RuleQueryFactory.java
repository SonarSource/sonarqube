/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableList;
import java.util.Date;
import javax.annotation.CheckForNull;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.server.rule.index.RuleQuery;

import static org.sonar.server.util.EnumUtils.toEnums;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_INHERITANCE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_QPROFILE;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_REPOSITORIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_RULE_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_STATUSES;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TAGS;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TEMPLATE_KEY;
import static org.sonarqube.ws.client.rule.RulesWsParameters.PARAM_TYPES;

@ServerSide
public class RuleQueryFactory {

  private final DbClient dbClient;
  private final RuleWsSupport wsSupport;

  public RuleQueryFactory(DbClient dbClient, RuleWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
  }

  /**
   * Create a {@link RuleQuery} from a {@link Request}.
   * When a profile key is set, the language of the profile is automatically set in the query
   */
  public RuleQuery createRuleQuery(DbSession dbSession, Request request) {
    RuleQuery ruleQuery = createRuleQuery(dbSession, new RuleQuery(), request);

    String qProfileKey = ruleQuery.getQProfileKey();
    if (qProfileKey != null) {
      QualityProfileDto qProfile = getProfileByKey(dbSession, qProfileKey);
      if (qProfile != null) {
        ruleQuery.setLanguages(ImmutableList.of(qProfile.getLanguage()));
      }
    }
    return ruleQuery;
  }

  private RuleQuery createRuleQuery(DbSession dbSession, RuleQuery query, Request request) {
    query.setQueryText(request.param(WebService.Param.TEXT_QUERY));
    query.setSeverities(request.paramAsStrings(PARAM_SEVERITIES));
    query.setRepositories(request.paramAsStrings(PARAM_REPOSITORIES));
    Date availableSince = request.paramAsDate(PARAM_AVAILABLE_SINCE);
    query.setAvailableSince(availableSince != null ? availableSince.getTime() : null);
    query.setStatuses(toEnums(request.paramAsStrings(PARAM_STATUSES), RuleStatus.class));
    query.setLanguages(request.paramAsStrings(PARAM_LANGUAGES));
    query.setActivation(request.paramAsBoolean(PARAM_ACTIVATION));
    query.setQProfileKey(request.param(PARAM_QPROFILE));
    query.setTags(request.paramAsStrings(PARAM_TAGS));
    query.setInheritance(request.paramAsStrings(PARAM_INHERITANCE));
    query.setActiveSeverities(request.paramAsStrings(PARAM_ACTIVE_SEVERITIES));
    query.setIsTemplate(request.paramAsBoolean(PARAM_IS_TEMPLATE));
    query.setTemplateKey(request.param(PARAM_TEMPLATE_KEY));
    query.setTypes(toEnums(request.paramAsStrings(PARAM_TYPES), RuleType.class));
    query.setKey(request.param(PARAM_RULE_KEY));
    query.setOrganizationUuid(wsSupport.getOrganizationByKey(dbSession, request.param(PARAM_ORGANIZATION)).getUuid());

    String sortParam = request.param(WebService.Param.SORT);
    if (sortParam != null) {
      query.setSortField(sortParam);
      query.setAscendingSort(request.mandatoryParamAsBoolean(WebService.Param.ASCENDING));
    }
    return query;
  }

  @CheckForNull
  private QualityProfileDto getProfileByKey(DbSession dbSession, String key) {
    return dbClient.qualityProfileDao().selectByKey(dbSession, key);
  }

}
