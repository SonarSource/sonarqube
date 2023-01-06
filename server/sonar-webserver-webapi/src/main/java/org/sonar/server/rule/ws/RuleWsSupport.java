/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.security.SecurityStandards;
import org.sonar.server.security.SecurityStandards.SQCategory;
import org.sonar.server.user.UserSession;

import static org.sonar.api.server.ws.WebService.Param.ASCENDING;
import static org.sonar.api.server.ws.WebService.Param.SORT;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVATION;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_ACTIVE_SEVERITIES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_AVAILABLE_SINCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_COMPARE_TO_PROFILE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_CWE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_INCLUDE_EXTERNAL;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_INHERITANCE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_IS_TEMPLATE;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_LANGUAGES;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_OWASP_TOP_10;
import static org.sonar.server.rule.ws.RulesWsParameters.PARAM_OWASP_TOP_10_2021;
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
public class RuleWsSupport {

  private final DbClient dbClient;
  private final UserSession userSession;

  public RuleWsSupport(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  public void checkQProfileAdminPermission() {
    userSession
      .checkLoggedIn()
      .checkPermission(ADMINISTER_QUALITY_PROFILES);
  }

  Map<String, UserDto> getUsersByUuid(DbSession dbSession, List<RuleDto> rules) {
    Set<String> userUuids = rules.stream().map(RuleDto::getNoteUserUuid).filter(Objects::nonNull).collect(toSet());
    return dbClient.userDao().selectByUuids(dbSession, userUuids).stream().collect(uniqueIndex(UserDto::getUuid));
  }

  public static void defineGenericRuleSearchParameters(WebService.NewAction action) {
    action
      .createParam(TEXT_QUERY)
      .setMinimumLength(2)
      .setDescription("UTF-8 search query")
      .setExampleValue("xpath");

    action
      .createParam(PARAM_RULE_KEY)
      .setDescription("Key of rule to search for")
      .setExampleValue("java:S1144");

    action
      .createParam(PARAM_REPOSITORIES)
      .setDescription("Comma-separated list of repositories")
      .setExampleValue("java,html");

    action
      .createParam(PARAM_SEVERITIES)
      .setDescription("Comma-separated list of default severities. Not the same than severity of rules in Quality profiles.")
      .setPossibleValues(Severity.ALL)
      .setExampleValue("CRITICAL,BLOCKER");

    action
      .createParam(PARAM_CWE)
      .setDescription("Comma-separated list of CWE identifiers. Use '" + SecurityStandards.UNKNOWN_STANDARD + "' to select rules not associated to any CWE.")
      .setExampleValue("12,125," + SecurityStandards.UNKNOWN_STANDARD);

    action.createParam(PARAM_OWASP_TOP_10)
      .setDescription("Comma-separated list of OWASP Top 10 2017 lowercase categories.")
      .setSince("7.3")
      .setPossibleValues("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10");

    action.createParam(PARAM_OWASP_TOP_10_2021)
      .setDescription("Comma-separated list of OWASP Top 10 2021 lowercase categories.")
      .setSince("9.4")
      .setPossibleValues("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10");

    action.createParam(PARAM_SANS_TOP_25)
      .setDescription("Comma-separated list of SANS Top 25 categories.")
      .setSince("7.3")
      .setPossibleValues(SecurityStandards.CWES_BY_SANS_TOP_25.keySet());

    action
      .createParam(PARAM_SONARSOURCE_SECURITY)
      .setDescription("Comma-separated list of SonarSource security categories. Use '" + SQCategory.OTHERS.getKey() + "' to select rules not associated" +
        " with any category")
      .setSince("7.8")
      .setPossibleValues(Arrays.stream(SQCategory.values()).map(SQCategory::getKey).toList())
      .setExampleValue("sql-injection,command-injection,others");

    action
      .createParam(PARAM_LANGUAGES)
      .setDescription("Comma-separated list of languages")
      .setExampleValue("java,js");

    action
      .createParam(PARAM_STATUSES)
      .setDescription("Comma-separated list of status codes")
      .setPossibleValues(RuleStatus.values())
      .setExampleValue(RuleStatus.READY);

    action
      .createParam(PARAM_AVAILABLE_SINCE)
      .setDescription("Filters rules added since date. Format is yyyy-MM-dd")
      .setExampleValue("2014-06-22");

    action
      .createParam(PARAM_TAGS)
      .setDescription("Comma-separated list of tags. Returned rules match any of the tags (OR operator)")
      .setExampleValue("security,java8");

    action
      .createParam(PARAM_TYPES)
      .setSince("5.5")
      .setDescription("Comma-separated list of types. Returned rules match any of the tags (OR operator)")
      .setPossibleValues(RuleType.values())
      .setExampleValue(RuleType.BUG);

    action
      .createParam(PARAM_ACTIVATION)
      .setDescription("Filter rules that are activated or deactivated on the selected Quality profile. Ignored if " +
        "the parameter '" + PARAM_QPROFILE + "' is not set.")
      .setBooleanPossibleValues();

    action
      .createParam(PARAM_QPROFILE)
      .setDescription("Quality profile key to filter on. Used only if the parameter '" +
        PARAM_ACTIVATION + "' is set.")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_COMPARE_TO_PROFILE)
      .setDescription("Quality profile key to filter rules that are activated. Meant to compare easily to profile set in '%s'", PARAM_QPROFILE)
      .setInternal(true)
      .setSince("6.5")
      .setExampleValue(UUID_EXAMPLE_02);

    action
      .createParam(PARAM_INHERITANCE)
      .setDescription("Comma-separated list of values of inheritance for a rule within a quality profile. Used only if the parameter '" +
        PARAM_ACTIVATION + "' is set.")
      .setPossibleValues(ActiveRuleInheritance.NONE.name(),
        ActiveRuleInheritance.INHERITED.name(),
        ActiveRuleInheritance.OVERRIDES.name())
      .setExampleValue(ActiveRuleInheritance.INHERITED.name() + "," +
        ActiveRuleInheritance.OVERRIDES.name());

    action
      .createParam(PARAM_ACTIVE_SEVERITIES)
      .setDescription("Comma-separated list of activation severities, i.e the severity of rules in Quality profiles.")
      .setPossibleValues(Severity.ALL)
      .setExampleValue("CRITICAL,BLOCKER");

    action
      .createParam(PARAM_IS_TEMPLATE)
      .setDescription("Filter template rules")
      .setBooleanPossibleValues();

    action
      .createParam(PARAM_TEMPLATE_KEY)
      .setDescription("Key of the template rule to filter on. Used to search for the custom rules based on this template.")
      .setExampleValue("java:S001");

    action
      .createParam(SORT)
      .setDescription("Sort field")
      .setPossibleValues(RuleIndexDefinition.SORT_FIELDS)
      .setExampleValue(RuleIndexDefinition.SORT_FIELDS.iterator().next());

    action
      .createParam(ASCENDING)
      .setDescription("Ascending sort")
      .setBooleanPossibleValues()
      .setDefaultValue(true);
  }

  static void defineIsExternalParam(WebService.NewAction action) {
    action
      .createParam(PARAM_INCLUDE_EXTERNAL)
      .setDescription("Include external engine rules in the results")
      .setDefaultValue(false)
      .setBooleanPossibleValues()
      .setSince("7.2");
  }

}
