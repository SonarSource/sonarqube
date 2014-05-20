/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.qualityprofile.ws;

import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.qualityprofile.ActiveRuleService;
import org.sonar.server.qualityprofile.RuleActivation;

public class RuleActivationActions implements ServerComponent {

  private final ActiveRuleService service;

  public RuleActivationActions(ActiveRuleService service) {
    this.service = service;
  }

  void define(WebService.NewController controller) {
    defineActivateAction(controller);
    defineDeactivateAction(controller);
  }

  private void defineActivateAction(WebService.NewController controller) {
    WebService.NewAction activate = controller
      .createAction("activate_rule")
      .setDescription("Activate a rule on a Quality profile")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          activate(request, response);
        }
      })
      .setPost(true)
      .setSince("4.4");

    defineActiveRuleKeyParameters(activate);

    activate.createParam("severity")
      .setDescription("Severity")
      .setPossibleValues(Severity.ALL);

    activate.createParam("params")
      .setDescription("Parameters");
  }

  private void defineDeactivateAction(WebService.NewController controller) {
    WebService.NewAction deactivate = controller
      .createAction("deactivate_rule")
      .setDescription("Deactivate a rule on a Quality profile")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          deactivate(request, response);
        }
      })
      .setPost(true)
      .setSince("4.4");
    defineActiveRuleKeyParameters(deactivate);
  }

  private void defineActiveRuleKeyParameters(WebService.NewAction action) {
    action.createParam("profile_key")
      .setDescription("Key of Quality profile")
      .setRequired(true)
      .setExampleValue("Sonar way:java");

    action.createParam("rule_key")
      .setDescription("Key of the rule to activate")
      .setRequired(true)
      .setExampleValue("squid:AvoidCycles");
  }

  private void activate(Request request, Response response) throws Exception {
    ActiveRuleKey key = readKey(request);
    RuleActivation activation = new RuleActivation(key);
    activation.setSeverity(request.param("severity"));
    String params = request.param("params");
    if (params != null) {
      activation.setParameters(KeyValueFormat.parse(params));
    }
    service.activate(activation);
  }

  private void deactivate(Request request, Response response) throws Exception {
    service.deactivate(readKey(request));
  }

  private ActiveRuleKey readKey(Request request) {
    return ActiveRuleKey.of(
      QualityProfileKey.parse(request.mandatoryParam("profile_key")),
      RuleKey.parse(request.mandatoryParam("rule_key")));
  }
}
