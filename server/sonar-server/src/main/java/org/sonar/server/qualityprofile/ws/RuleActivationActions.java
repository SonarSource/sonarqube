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
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.util.Uuids;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.qualityprofile.RuleActivation;

import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_ACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ACTION_DEACTIVATE_RULE;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_PARAMS;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_PROFILE_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_RESET;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_RULE_KEY;
import static org.sonarqube.ws.client.qualityprofile.QualityProfileWsParameters.ActivateActionParameters.PARAM_SEVERITY;

@ServerSide
public class RuleActivationActions {

  private final QProfileService service;

  public RuleActivationActions(QProfileService service) {
    this.service = service;
  }

  void define(WebService.NewController controller) {
    defineActivateAction(controller);
    defineDeactivateAction(controller);
  }

  private void defineActivateAction(WebService.NewController controller) {
    WebService.NewAction activate = controller
      .createAction(ACTION_ACTIVATE_RULE)
      .setDescription("Activate a rule on a Quality profile")
      .setHandler(this::activate)
      .setPost(true)
      .setSince("4.4");

    defineActiveRuleKeyParameters(activate);

    activate.createParam(PARAM_SEVERITY)
      .setDescription(String.format("Severity. Ignored if parameter %s is true.", PARAM_RESET))
      .setPossibleValues(Severity.ALL);

    activate.createParam(PARAM_PARAMS)
      .setDescription(String.format("Parameters as semi-colon list of <key>=<value>, for example " +
        "'<code>params=key1=v1;key2=v2</code>'. Ignored if parameter %s is true.", PARAM_RESET));

    activate.createParam(PARAM_RESET)
      .setDescription("Reset severity and parameters of activated rule. Set the values defined on parent profile " +
        "or from rule default values.")
      .setBooleanPossibleValues();
  }

  private void defineDeactivateAction(WebService.NewController controller) {
    WebService.NewAction deactivate = controller
      .createAction(ACTION_DEACTIVATE_RULE)
      .setDescription("Deactivate a rule on a Quality profile")
      .setHandler(this::deactivate)
      .setPost(true)
      .setSince("4.4");
    defineActiveRuleKeyParameters(deactivate);
  }

  private static void defineActiveRuleKeyParameters(WebService.NewAction action) {
    action.createParam(PARAM_PROFILE_KEY)
      .setDescription("Key of Quality profile, can be obtained through <code>api/profiles/list</code>")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action.createParam(PARAM_RULE_KEY)
      .setDescription("Key of the rule")
      .setRequired(true)
      .setExampleValue("squid:AvoidCycles");
  }

  private void activate(Request request, Response response) {
    RuleKey ruleKey = readRuleKey(request);
    RuleActivation activation = new RuleActivation(ruleKey);
    activation.setSeverity(request.param(PARAM_SEVERITY));
    String params = request.param(PARAM_PARAMS);
    if (params != null) {
      activation.setParameters(KeyValueFormat.parse(params));
    }
    activation.setReset(Boolean.TRUE.equals(request.paramAsBoolean(PARAM_RESET)));
    service.activate(request.mandatoryParam(PARAM_PROFILE_KEY), activation);
  }

  private void deactivate(Request request, Response response) {
    RuleKey ruleKey = readRuleKey(request);
    service.deactivate(ActiveRuleKey.of(request.mandatoryParam(PARAM_PROFILE_KEY), ruleKey));
  }

  private static RuleKey readRuleKey(Request request) {
    return RuleKey.parse(request.mandatoryParam(PARAM_RULE_KEY));
  }

}
