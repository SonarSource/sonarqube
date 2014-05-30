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
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;

/**
 * @Since 4.4
 */
public class RuleResetAction implements ServerComponent {

  public static final String PROFILE_KEY = "profile_key";
  public static final String RULE_KEY = "rule_key";

  public static final String RESET_ACTION = "reset";

  private final RuleActivator service;

  public RuleResetAction(RuleActivator service) {
    this.service = service;
  }

  void define(WebService.NewController controller) {
    defineResetAction(controller);
  }

  private void defineResetAction(WebService.NewController controller) {
    WebService.NewAction resetAction = controller
      .createAction(RESET_ACTION)
      .setDescription("Reset an Activate rule based on its parent profile")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          reset(request, response);
        }
      })
      .setPost(true)
      .setSince("4.4");

    resetAction.createParam(PROFILE_KEY)
      .setDescription("Key of Quality profile")
      .setRequired(true)
      .setExampleValue("Sonar way:java");

    resetAction.createParam(RULE_KEY)
      .setDescription("Key of the rule to activate")
      .setRequired(true)
      .setExampleValue("squid:AvoidCycles");  }

  private void reset(Request request, Response response) {
    ActiveRuleKey key = readKey(request);
    RuleActivation activation = new RuleActivation(key);
    service.reset(activation);
  }

  private ActiveRuleKey readKey(Request request) {
    return ActiveRuleKey.of(
      QualityProfileKey.parse(request.mandatoryParam(PROFILE_KEY)),
      RuleKey.parse(request.mandatoryParam(RULE_KEY)));
  }
}
