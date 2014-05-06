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

import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.qualityprofile.RuleActivationService;

public class ActivateRuleAction implements RequestHandler {

  private final RuleActivationService service;

  public ActivateRuleAction(RuleActivationService service) {
    this.service = service;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("activate_rule")
      .setHandler(this)
      .setPost(true)
      .setSince("4.4");

    action.createParam("profile_lang")
      .setRequired(true);

    action.createParam("profile_name")
      .setRequired(true);

    action.createParam("rule_repo")
      .setRequired(true);

    action.createParam("rule_key")
      .setRequired(true);

    action.createParam("severity")
      .setPossibleValues(Severity.ALL);

    action.createParam("params");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {

  }
}
