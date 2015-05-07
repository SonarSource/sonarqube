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

import org.sonar.api.ServerSide;
import org.sonar.api.i18n.I18n;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.qualityprofile.BulkChangeResult;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.rule.RuleService;
import org.sonar.server.rule.ws.SearchAction;
import org.sonar.server.user.UserSession;

@ServerSide
public class BulkRuleActivationActions {

  public static final String PROFILE_KEY = "profile_key";
  public static final String SEVERITY = "activation_severity";

  public static final String BULK_ACTIVATE_ACTION = "activate_rules";
  public static final String BULK_DEACTIVATE_ACTION = "deactivate_rules";

  private final QProfileService profileService;
  private final RuleService ruleService;
  private final I18n i18n;

  public BulkRuleActivationActions(QProfileService profileService, RuleService ruleService, I18n i18n) {
    this.profileService = profileService;
    this.ruleService = ruleService;
    this.i18n = i18n;
  }

  void define(WebService.NewController controller) {
    defineActivateAction(controller);
    defineDeactivateAction(controller);
  }

  private void defineActivateAction(WebService.NewController controller) {
    WebService.NewAction activate = controller
      .createAction(BULK_ACTIVATE_ACTION)
      .setDescription("Bulk-activate rules on one or several Quality profiles")
      .setPost(true)
      .setSince("4.4")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          bulkActivate(request, response);
        }
      });

    SearchAction.defineRuleSearchParameters(activate);
    defineProfileKeyParameter(activate);

    activate.createParam(SEVERITY)
      .setDescription("Optional severity of rules activated in bulk")
      .setPossibleValues(Severity.ALL);
  }

  private void defineDeactivateAction(WebService.NewController controller) {
    WebService.NewAction deactivate = controller
      .createAction(BULK_DEACTIVATE_ACTION)
      .setDescription("Bulk deactivate rules on Quality profiles")
      .setPost(true)
      .setSince("4.4")
      .setHandler(new RequestHandler() {
        @Override
        public void handle(Request request, Response response) throws Exception {
          bulkDeactivate(request, response);
        }
      });

    SearchAction.defineRuleSearchParameters(deactivate);
    defineProfileKeyParameter(deactivate);
  }

  private void defineProfileKeyParameter(WebService.NewAction action) {
    action.createParam(PROFILE_KEY)
      .setDescription("Quality Profile Key. To retrieve a profile key for a given language please see the api/qprofiles documentation")
      .setRequired(true)
      .setExampleValue("java:MyProfile");
  }

  private void bulkActivate(Request request, Response response) {
    BulkChangeResult result = profileService.bulkActivate(
      SearchAction.createRuleQuery(ruleService.newRuleQuery(), request),
      request.mandatoryParam(PROFILE_KEY),
      request.param(SEVERITY));
    writeResponse(result, response);
  }

  private void bulkDeactivate(Request request, Response response) {
    BulkChangeResult result = profileService.bulkDeactivate(
      SearchAction.createRuleQuery(ruleService.newRuleQuery(), request),
      request.mandatoryParam(PROFILE_KEY));
    writeResponse(result, response);
  }

  private void writeResponse(BulkChangeResult result, Response response) {
    JsonWriter json = response.newJsonWriter().beginObject();
    json.prop("succeeded", result.countSucceeded());
    json.prop("failed", result.countFailed());
    result.getErrors().writeJsonAsWarnings(json, i18n, UserSession.get().locale());
    json.endObject().close();
  }
}
