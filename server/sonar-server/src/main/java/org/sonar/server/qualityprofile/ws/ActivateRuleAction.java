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
package org.sonar.server.qualityprofile.ws;

import java.util.List;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.qualityprofile.ActiveRuleChange;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndexer;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_PARAMS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_PROFILE_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_RESET;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_RULE_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_SEVERITY;

@ServerSide
public class ActivateRuleAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final RuleActivator ruleActivator;
  private final UserSession userSession;
  private final QProfileWsSupport wsSupport;
  private final ActiveRuleIndexer activeRuleIndexer;

  public ActivateRuleAction(DbClient dbClient, RuleActivator ruleActivator, UserSession userSession, QProfileWsSupport wsSupport, ActiveRuleIndexer activeRuleIndexer) {
    this.dbClient = dbClient;
    this.ruleActivator = ruleActivator;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
    this.activeRuleIndexer = activeRuleIndexer;
  }

  public void define(WebService.NewController controller) {
    WebService.NewAction activate = controller
      .createAction(ACTION_ACTIVATE_RULE)
      .setDescription("Activate a rule on a Quality profile")
      .setHandler(this)
      .setPost(true)
      .setSince("4.4");

    activate.createParam(PARAM_PROFILE_KEY)
      .setDescription("Key of Quality profile, can be obtained through <code>api/qualityprofiles/search</code>")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    activate.createParam(PARAM_RULE_KEY)
      .setDescription("Key of the rule")
      .setRequired(true)
      .setExampleValue("squid:AvoidCycles");

    activate.createParam(PARAM_SEVERITY)
      .setDescription(format("Severity. Ignored if parameter %s is true.", PARAM_RESET))
      .setPossibleValues(Severity.ALL);

    activate.createParam(PARAM_PARAMS)
      .setDescription(format("Parameters as semi-colon list of <key>=<value>. Ignored if parameter %s is true.", PARAM_RESET))
    .setExampleValue("params=key1=v1;key2=v2");

    activate.createParam(PARAM_RESET)
      .setDescription("Reset severity and parameters of activated rule. Set the values defined on parent profile " +
        "or from rule default values.")
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    RuleKey ruleKey = readRuleKey(request);
    RuleActivation activation = new RuleActivation(ruleKey);
    activation.setSeverity(request.param(PARAM_SEVERITY));
    String params = request.param(PARAM_PARAMS);
    if (params != null) {
      activation.setParameters(KeyValueFormat.parse(params));
    }
    activation.setReset(Boolean.TRUE.equals(request.paramAsBoolean(PARAM_RESET)));
    String profileKey = request.mandatoryParam(PARAM_PROFILE_KEY);
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      wsSupport.checkPermission(dbSession, profileKey);
      List<ActiveRuleChange> changes = ruleActivator.activate(dbSession, activation, profileKey);
      dbSession.commit();
      activeRuleIndexer.index(changes);
    }
    response.noContent();
  }

  private static RuleKey readRuleKey(Request request) {
    return RuleKey.parse(request.mandatoryParam(PARAM_RULE_KEY));
  }
}
