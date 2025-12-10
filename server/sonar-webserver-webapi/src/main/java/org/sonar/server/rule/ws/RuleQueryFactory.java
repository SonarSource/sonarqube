/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import io.sonarcloud.compliancereports.reports.MetadataRules;
import io.sonarcloud.compliancereports.reports.MetadataRules.ComplianceCategoryRules;
import io.sonarcloud.compliancereports.reports.ReportKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.server.rule.index.RuleQuery;

import static org.sonar.server.common.ParamParsingUtils.parseComplianceStandardsFilter;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.rule.ws.EnumUtils.toEnums;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVE_IMPACT_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_COMPARE_TO_PROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_COMPLIANCE_STANDARDS;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_CWE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_IMPACT_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_IMPACT_SOFTWARE_QUALITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_INCLUDE_EXTERNAL;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_INHERITANCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_OWASP_MOBILE_TOP_10_2024;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_OWASP_TOP_10;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_OWASP_TOP_10_2021;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_PRIORITIZED_RULE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_QPROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_REPOSITORIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_RULE_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_SANS_TOP_25;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_SONARSOURCE_SECURITY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_STATUSES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TAGS;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TEMPLATE_KEY;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_TYPES;

@ServerSide
public class RuleQueryFactory {
  private final DbClient dbClient;
  private final MetadataRules metadataRules;

  public RuleQueryFactory(DbClient dbClient, MetadataRules metadataRules) {
    this.dbClient = dbClient;
    this.metadataRules = metadataRules;
  }

  /**
   * Similar to {@link #createRuleQuery(DbSession, Request)} but sets additional fields which are only used
   * for the rule search WS.
   */
  public RuleQuery createRuleSearchQuery(DbSession dbSession, Request request) {
    RuleQuery query = createRuleQuery(dbSession, request);
    query.setIncludeExternal(request.mandatoryParamAsBoolean(PARAM_INCLUDE_EXTERNAL));
    query.setPrioritizedRule(request.paramAsBoolean(PARAM_PRIORITIZED_RULE));
    setComplianceFilter(query, parseComplianceStandardsFilter(request.param(PARAM_COMPLIANCE_STANDARDS)));
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

    // Order is important : 1. Load profile, 2. Load compare to profile
    setProfile(dbSession, query, request);
    setCompareToProfile(dbSession, query, request);
    QProfileDto profile = query.getQProfile();
    query.setLanguages(profile == null ? request.paramAsStrings(PARAM_LANGUAGES) : List.of(profile.getLanguage()));
    query.setActivation(request.paramAsBoolean(PARAM_ACTIVATION));
    query.setTags(request.paramAsStrings(PARAM_TAGS));
    query.setInheritance(request.paramAsStrings(PARAM_INHERITANCE));
    query.setActiveSeverities(request.paramAsStrings(PARAM_ACTIVE_SEVERITIES));
    query.setIsTemplate(request.paramAsBoolean(PARAM_IS_TEMPLATE));
    query.setTemplateKey(request.param(PARAM_TEMPLATE_KEY));
    query.setTypes(toEnums(request.paramAsStrings(PARAM_TYPES), RuleType.class));
    query.setKey(request.param(PARAM_RULE_KEY));
    query.setCwe(request.paramAsStrings(PARAM_CWE));
    query.setOwaspTop10(request.paramAsStrings(PARAM_OWASP_TOP_10));
    query.setOwaspTop10For2021(request.paramAsStrings(PARAM_OWASP_TOP_10_2021));
    query.setOwaspMobileTop10For2024(request.paramAsStrings(PARAM_OWASP_MOBILE_TOP_10_2024));
    query.setSansTop25(request.paramAsStrings(PARAM_SANS_TOP_25));
    query.setSonarsourceSecurity(request.paramAsStrings(PARAM_SONARSOURCE_SECURITY));
    query.setCleanCodeAttributesCategories(request.paramAsStrings(PARAM_CLEAN_CODE_ATTRIBUTE_CATEGORIES));
    query.setImpactSeverities(request.paramAsStrings(PARAM_IMPACT_SEVERITIES));
    query.setImpactSoftwareQualities(request.paramAsStrings(PARAM_IMPACT_SOFTWARE_QUALITIES));
    query.setActiveImpactSeverities(request.paramAsStrings(PARAM_ACTIVE_IMPACT_SEVERITIES));

    String sortParam = request.param(WebService.Param.SORT);
    if (sortParam != null) {
      query.setSortField(sortParam);
      query.setAscendingSort(request.mandatoryParamAsBoolean(WebService.Param.ASCENDING));
    }

    return query;
  }

  private void setComplianceFilter(RuleQuery query, Map<ReportKey, Set<String>> categoriesByStandard) {
    if (categoriesByStandard.isEmpty()) {
      return;
    }

    query.setComplianceCategoryRules(getComplianceStandardRules(categoriesByStandard));
  }

  private List<ComplianceCategoryRules> getComplianceStandardRules(Map<ReportKey, Set<String>> categoriesByStandard) {
    return metadataRules.getRulesByStandard(categoriesByStandard).values().stream()
      .map(e -> e.isEmpty() ? new ComplianceCategoryRules(Set.of(), Set.of("non-existing-uuid")) : e)
      .toList();
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

  private void setCompareToProfile(DbSession dbSession, RuleQuery query, Request request) {
    String compareToProfileUuid = request.param(PARAM_COMPARE_TO_PROFILE);
    if (compareToProfileUuid == null) {
      return;
    }
    QProfileDto profileOptional = dbClient.qualityProfileDao().selectByUuid(dbSession, compareToProfileUuid);
    QProfileDto profile = checkFound(profileOptional, "The specified qualityProfile '%s' does not exist", compareToProfileUuid);

    query.setCompareToQProfile(profile);
  }
}
