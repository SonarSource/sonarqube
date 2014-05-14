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
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.qualityprofile.ActiveRuleService;
import org.sonar.server.qualityprofile.BulkRuleActivation;
import org.sonar.server.rule2.ws.SearchAction;

public class BulkRuleActivationActions implements ServerComponent {

  private final ActiveRuleService service;

  public BulkRuleActivationActions(ActiveRuleService service) {
    this.service = service;
  }

  void define(WebService.NewController controller) {
    defineActivateAction(controller);
    defineDeactivateAction(controller);
  }

  private void defineActivateAction(WebService.NewController controller) {
    WebService.NewAction activate = controller
      .createAction("activate_rules")
      .setDescription("Bulk-activate rules on one or several Quality profiles")
      .setPost(true)
      .setSince("4.4")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          bulkActivate(request, response);
        }
      });

    SearchAction.defineSearchParameters(activate);
    defineProfileKeyParameters(activate);

    activate.createParam("activation_severity")
      .setDescription("Severity")
      .setPossibleValues(Severity.ALL);
  }

  private void defineDeactivateAction(WebService.NewController controller) {
    WebService.NewAction deactivate = controller
      .createAction("deactivate_rules")
      .setDescription("Bulk deactivate rules on Quality profiles")
      .setPost(true)
      .setSince("4.4")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          bulkDeactivate(request, response);
        }
      });

    defineProfileKeyParameters(deactivate);
  }

  private void defineProfileKeyParameters(WebService.NewAction action) {
    action.createParam("target_profile_lang")
      .setDescription("Profile language")
      .setRequired(true)
      .setExampleValue("java");

    action.createParam("target_profile_name")
      .setDescription("Profile name")
      .setRequired(true)
      .setExampleValue("My profile");
  }

  private void bulkActivate(Request request, Response response) throws Exception {
    BulkRuleActivation activation = new BulkRuleActivation();
    // TODO
    service.bulkActivate(activation);
  }

  private void bulkDeactivate(Request request, Response response) throws Exception {
    // TODO
  }
}
