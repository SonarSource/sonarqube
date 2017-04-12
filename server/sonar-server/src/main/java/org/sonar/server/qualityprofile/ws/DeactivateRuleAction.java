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

import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.user.UserSession;

import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_DEACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_PROFILE_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_RULE_KEY;

@ServerSide
public class DeactivateRuleAction implements QProfileWsAction {

  private final DbClient dbClient;
  private final RuleActivator ruleActivator;
  private final UserSession userSession;
  private final QProfileWsSupport wsSupport;

  public DeactivateRuleAction(DbClient dbClient, RuleActivator ruleActivator, UserSession userSession, QProfileWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.ruleActivator = ruleActivator;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
  }

  public void define(WebService.NewController controller) {
    WebService.NewAction deactivate = controller
      .createAction(ACTION_DEACTIVATE_RULE)
      .setDescription("Deactivate a rule on a Quality profile")
      .setHandler(this)
      .setPost(true)
      .setSince("4.4");

    deactivate.createParam(PARAM_PROFILE_KEY)
      .setDescription("Key of Quality profile, can be obtained through <code>api/qualityprofiles/search</code>")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    deactivate.createParam(PARAM_RULE_KEY)
      .setDescription("Key of the rule")
      .setRequired(true)
      .setExampleValue("squid:AvoidCycles");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    RuleKey ruleKey = RuleKey.parse(request.mandatoryParam(PARAM_RULE_KEY));
    String qualityProfileKey = request.mandatoryParam(PARAM_PROFILE_KEY);
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      wsSupport.checkPermission(dbSession, qualityProfileKey);
      ActiveRuleKey activeRuleKey = ActiveRuleKey.of(qualityProfileKey, ruleKey);
      ruleActivator.deactivateAndUpdateIndex(dbSession, activeRuleKey);
    }
    response.noContent();
  }
}
