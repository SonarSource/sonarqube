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
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.server.qualityprofile.QProfileService;

import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_DEACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_PROFILE_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_RULE_KEY;

@ServerSide
public class RuleDeactivationAction implements QProfileWsAction {

  private final QProfileService service;

  public RuleDeactivationAction(QProfileService service) {
    this.service = service;
  }

  public void define(WebService.NewController controller) {
    WebService.NewAction deactivate = controller
      .createAction(ACTION_DEACTIVATE_RULE)
      .setDescription("Deactivate a rule on a Quality profile")
      .setHandler(this)
      .setPost(true)
      .setSince("4.4");

    deactivate.createParam(PARAM_PROFILE_KEY)
      .setDescription("Key of Quality profile, can be obtained through <code>api/profiles/list</code>")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    deactivate.createParam(PARAM_RULE_KEY)
      .setDescription("Key of the rule")
      .setRequired(true)
      .setExampleValue("squid:AvoidCycles");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    RuleKey ruleKey = readRuleKey(request);
    service.deactivate(ActiveRuleKey.of(request.mandatoryParam(PARAM_PROFILE_KEY), ruleKey));
  }

  private static RuleKey readRuleKey(Request request) {
    return RuleKey.parse(request.mandatoryParam(PARAM_RULE_KEY));
  }
}
