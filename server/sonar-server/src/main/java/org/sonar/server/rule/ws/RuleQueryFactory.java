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
package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableList;
import java.util.Date;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.rule.index.RuleQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_COMPARE_TO_PROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_INCLUDE_EXTERNAL;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_INHERITANCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ORGANIZATION;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_QPROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_REPOSITORIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_RULE_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_STATUSES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TAGS;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TEMPLATE_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TYPES;
import static org.sonar.server.util.EnumUtils.toEnums;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;

@ServerSide
public class RuleQueryFactory {

  private final DbClient dbClient;
  private final RuleWsSupport wsSupport;

  public RuleQueryFactory(DbClient dbClient, RuleWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
  }

  /**
   * Similar to {@link #createRuleQuery(DbSession, Request)} but sets additional fields which are only used
   * for the rule search WS. 
   */
  public RuleQuery createRuleSearchQuery(DbSession dbSession, Request request) {
    RuleQuery query = createRuleQuery(dbSession, request);
    query.setIncludeExternal(request.mandatoryParamAsBoolean(PARAM_INCLUDE_EXTERNAL));
    return query;
  }

  /**
   * Create a {@link RuleQuery} from a {@link Request}.
   * When a profile key is set, the language of the profile is automatically set in the query
   */
  public RuleQuery createRuleQuery(DbSession dbSession, Request request) {
    RuleQuery query = new RuleQuery();
    query.setQueryText(request.param(WebService.Param.TEXT_QUERY));
    query.setSeverities(request.paramAsStrings(PARAM_SEVERITIES));
    query.setRepositories(request.paramAsStrings(PARAM_REPOSITORIES));
    Date availableSince = request.paramAsDate(PARAM_AVAILABLE_SINCE);
    query.setAvailableSince(availableSince != null ? availableSince.getTime() : null);
    query.setStatuses(toEnums(request.paramAsStrings(PARAM_STATUSES), RuleStatus.class));

    // Order is important : 1. Load profile, 2. Load organization either from parameter or from profile, 3. Load compare to profile
    setProfile(dbSession, query, request);
    setOrganization(dbSession, query, request);
    setCompareToProfile(dbSession, query, request);
    QProfileDto profile = query.getQProfile();
    query.setLanguages(profile == null ? request.paramAsStrings(PARAM_LANGUAGES) : ImmutableList.of(profile.getLanguage()));
    if (wsSupport.areActiveRulesVisible(query.getOrganization())) {
      query.setActivation(request.paramAsBoolean(PARAM_ACTIVATION));
    }
    query.setTags(request.paramAsStrings(PARAM_TAGS));
    query.setInheritance(request.paramAsStrings(PARAM_INHERITANCE));
    query.setActiveSeverities(request.paramAsStrings(PARAM_ACTIVE_SEVERITIES));
    query.setIsTemplate(request.paramAsBoolean(PARAM_IS_TEMPLATE));
    query.setTemplateKey(request.param(PARAM_TEMPLATE_KEY));
    query.setTypes(toEnums(request.paramAsStrings(PARAM_TYPES), RuleType.class));
    query.setKey(request.param(PARAM_RULE_KEY));

    String sortParam = request.param(WebService.Param.SORT);
    if (sortParam != null) {
      query.setSortField(sortParam);
      query.setAscendingSort(request.mandatoryParamAsBoolean(WebService.Param.ASCENDING));
    }
    return query;
  }

  private void setProfile(DbSession dbSession, RuleQuery query, Request request) {
    String profileUuid = request.param(PARAM_QPROFILE);
    if (profileUuid == null) {
      return;
    }
    QProfileDto profileOptional = dbClient.qualityProfileDao().selectByUuid(dbSession, profileUuid);
    QProfileDto profile = checkFound(profileOptional, "The specified qualityProfile '%s' does not exist", profileUuid);
    query.setQProfile(profile);
  }

  private void setOrganization(DbSession dbSession, RuleQuery query, Request request) {
    String organizationKey = request.param(PARAM_ORGANIZATION);
    QProfileDto profile = query.getQProfile();
    if (profile == null) {
      query.setOrganization(wsSupport.getOrganizationByKey(dbSession, organizationKey));
      return;
    }
    OrganizationDto organization = checkFoundWithOptional(dbClient.organizationDao().selectByUuid(dbSession, profile.getOrganizationUuid()), "No organization with UUID %s",
      profile.getOrganizationUuid());
    if (organizationKey != null) {
      OrganizationDto inputOrganization = checkFoundWithOptional(dbClient.organizationDao().selectByKey(dbSession, organizationKey), "No organization with key '%s'",
        organizationKey);
      checkArgument(organization.getUuid().equals(inputOrganization.getUuid()),
        format("The specified quality profile '%s' is not part of the specified organization '%s'", profile.getKee(), organizationKey));
    }
    query.setOrganization(organization);
  }

  private void setCompareToProfile(DbSession dbSession, RuleQuery query, Request request) {
    String compareToProfileUuid = request.param(PARAM_COMPARE_TO_PROFILE);
    if (compareToProfileUuid == null) {
      return;
    }
    QProfileDto profileOptional = dbClient.qualityProfileDao().selectByUuid(dbSession, compareToProfileUuid);
    QProfileDto profile = checkFound(profileOptional, "The specified qualityProfile '%s' does not exist", compareToProfileUuid);

    checkArgument(query.getOrganization().getUuid().equals(profile.getOrganizationUuid()),
      format("The specified quality profile '%s' is not part of the specified organization '%s'", profile.getKee(), query.getOrganization().getKey()));

    query.setCompareToQProfile(profile);
  }
}
