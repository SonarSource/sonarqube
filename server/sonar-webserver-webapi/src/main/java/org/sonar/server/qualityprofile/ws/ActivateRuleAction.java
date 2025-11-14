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
package org.sonar.server.qualityprofile.ws;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.qualityprofile.QProfileRules;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.user.UserSession;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_IMPACTS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PARAMS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_PRIORITIZED_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_RESET;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.PARAM_SEVERITY;

public class ActivateRuleAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final QProfileRules ruleActivator;
  private final UserSession userSession;
  private final QProfileWsSupport wsSupport;

  public ActivateRuleAction(DbClient dbClient, QProfileRules ruleActivator, UserSession userSession, QProfileWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.ruleActivator = ruleActivator;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
  }

  public void define(WebService.NewController controller) {
    WebService.NewAction activate = controller
      .createAction(ACTION_ACTIVATE_RULE)
      .setDescription("Activate a rule on a Quality Profile.<br> " +
        "Requires one of the following permissions:" +
        "<ul>" +
        "  <li>'Administer Quality Profiles'</li>" +
        "  <li>Edit right on the specified quality profile</li>" +
        "</ul>")
      .setChangelog(
        new Change("10.8", format("The parameter '%s' is not deprecated anymore.", PARAM_SEVERITY)),
        new Change("10.8", format("Add new parameter '%s'", PARAM_IMPACTS)),
        new Change("10.6", format("Add parameter '%s'.", PARAM_PRIORITIZED_RULE)),
        new Change("10.2", format("Parameter '%s' is now deprecated.", PARAM_SEVERITY)))
      .setHandler(this)
      .setPost(true)
      .setSince("4.4");

    activate.createParam(PARAM_KEY)
      .setDescription("Quality Profile key. Can be obtained through <code>api/qualityprofiles/search</code>")
      .setRequired(true)
      .setExampleValue(UUID_EXAMPLE_01);

    activate.createParam(PARAM_RULE)
      .setDescription("Rule key")
      .setRequired(true)
      .setExampleValue("java:AvoidCycles");

    activate.createParam(PARAM_SEVERITY)
      .setDescription(format("Severity. Cannot be used as the same time as '%s'.Ignored if parameter %s is true.", PARAM_IMPACTS, PARAM_RESET))
      .setPossibleValues(Severity.ALL);

    activate.createParam(PARAM_IMPACTS)
      .setDescription(format("Override of impact severities for the rule. Cannot be used as the same time as '%s'. Ignored if parameter %s is true.", PARAM_SEVERITY, PARAM_RESET))
      .setExampleValue("impacts=MAINTAINABILITY=HIGH;SECURITY=MEDIUM");

    activate.createParam(PARAM_PARAMS)
      .setDescription(format("Parameters as semi-colon list of <code>key=value</code>. Ignored if parameter %s is true.", PARAM_RESET))
      .setExampleValue("params=key1=v1;key2=v2");

    activate.createParam(PARAM_RESET)
      .setDescription("Reset severity and parameters of activated rule. Set the values defined on parent profile or from rule default " +
        "values.")
      .setBooleanPossibleValues();

    activate.createParam(PARAM_PRIORITIZED_RULE)
      .setDescription("Mark activated rule as prioritized, so all corresponding Issues will have to be fixed.")
      .setBooleanPossibleValues()
      .setSince("10.6");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      String profileKey = request.mandatoryParam(PARAM_KEY);
      QProfileDto profile = wsSupport.getProfile(dbSession, QProfileReference.fromKey(profileKey));
      wsSupport.checkCanEdit(dbSession, profile);
      RuleActivation activation = readActivation(dbSession, request);
      ruleActivator.activateAndCommit(dbSession, profile, singletonList(activation));
    }

    response.noContent();
  }

  private RuleActivation readActivation(DbSession dbSession, Request request) {
    RuleKey ruleKey = RuleKey.parse(request.mandatoryParam(PARAM_RULE));
    RuleDto ruleDto = wsSupport.getRule(dbSession, ruleKey);
    boolean reset = TRUE.equals(request.paramAsBoolean(PARAM_RESET));
    if (reset) {
      return RuleActivation.createReset(ruleDto.getUuid());
    }
    String severity = request.param(PARAM_SEVERITY);
    String impactsAsString = request.param(PARAM_IMPACTS);

    if (impactsAsString != null && severity != null) {
      throw BadRequestException.create(format("'%s' and '%s' parameters can't be provided both at the same time", PARAM_SEVERITY, PARAM_IMPACTS));
    }

    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts = new EnumMap<>(SoftwareQuality.class);
    if (impactsAsString != null) {
      impacts = getImpacts(impactsAsString, ruleDto);
    }

    Boolean prioritizedRule = request.paramAsBoolean(PARAM_PRIORITIZED_RULE);
    Map<String, String> params = null;
    String paramsAsString = request.param(PARAM_PARAMS);
    if (paramsAsString != null) {
      params = KeyValueFormat.parse(paramsAsString);
    }
    return RuleActivation.create(ruleDto.getUuid(), severity, impacts, prioritizedRule, params);
  }

  @NotNull
  private static Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> getImpacts(String impactsAsString, RuleDto ruleDto) {
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> result;
    try {
      result = KeyValueFormat.parse(impactsAsString)
        .entrySet()
        .stream().collect(Collectors.toMap(e -> SoftwareQuality.valueOf(e.getKey()), e -> org.sonar.api.issue.impact.Severity.valueOf(e.getValue())));
    } catch (Exception e) {
      throw BadRequestException.create(format("Unexpected value for parameter '%s': %s", PARAM_IMPACTS, impactsAsString));
    }
    if (!ruleDto.getDefaultImpactsMap().keySet().containsAll(result.keySet())) {
      throw BadRequestException.create(
        format("Only impacts defined on the rule can be overridden. (%s)", ruleDto.getDefaultImpactsMap().keySet().stream().map(Enum::name).collect(Collectors.joining(","))));
    }
    return result;

  }

}
