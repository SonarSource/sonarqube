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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.qualityprofile.ActiveRuleInheritance;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.user.UserSession;

import static org.sonar.api.server.ws.WebService.Param.ASCENDING;
import static org.sonar.api.server.ws.WebService.Param.SORT;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_02;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.db.organization.OrganizationDto.Subscription.PAID;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER_QUALITY_PROFILES;
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
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;

@ServerSide
public class RuleWsSupport {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public RuleWsSupport(DbClient dbClient, UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  public void checkQProfileAdminPermissionOnDefaultOrganization() {
    userSession
      .checkLoggedIn()
      .checkPermission(ADMINISTER_QUALITY_PROFILES, defaultOrganizationProvider.get().getUuid());
  }

  public OrganizationDto getOrganizationByKey(DbSession dbSession, @Nullable String organizationKey) {
    String organizationOrDefaultKey = Optional.ofNullable(organizationKey)
      .orElseGet(defaultOrganizationProvider.get()::getKey);
    return checkFoundWithOptional(
      dbClient.organizationDao().selectByKey(dbSession, organizationOrDefaultKey),
      "No organization with key '%s'", organizationOrDefaultKey);
  }

  Map<String, UserDto> getUsersByUuid(DbSession dbSession, List<RuleDto> rules) {
    Set<String> userUuids = rules.stream().map(RuleDto::getNoteUserUuid).filter(Objects::nonNull).collect(toSet());
    return dbClient.userDao().selectByUuids(dbSession, userUuids).stream().collect(uniqueIndex(UserDto::getUuid));
  }

  boolean areActiveRulesVisible(OrganizationDto organization) {
    if (!organization.getSubscription().equals(PAID)) {
      return true;
    }
    return userSession.hasMembership(organization);
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
      .setExampleValue("squid:S001");

    action
      .createParam(PARAM_REPOSITORIES)
      .setDescription("Comma-separated list of repositories")
      .setExampleValue("checkstyle,findbugs");

    action
      .createParam(PARAM_SEVERITIES)
      .setDescription("Comma-separated list of default severities. Not the same than severity of rules in Quality profiles.")
      .setPossibleValues(Severity.ALL)
      .setExampleValue("CRITICAL,BLOCKER");

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

    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setRequired(false)
      .setInternal(true)
      .setExampleValue("my-org")
      .setSince("6.4");
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
