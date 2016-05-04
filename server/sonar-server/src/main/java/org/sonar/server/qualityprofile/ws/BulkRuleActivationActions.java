/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.api.i18n.I18n;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.qualityprofile.BulkChangeResult;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.rule.ws.RuleQueryFactory;
import org.sonar.server.user.UserSession;

import static org.sonar.server.rule.ws.SearchAction.defineRuleSearchParameters;

@ServerSide
public class BulkRuleActivationActions {

  public static final String PROFILE_KEY = "profile_key";
  public static final String SEVERITY = "activation_severity";

  public static final String BULK_ACTIVATE_ACTION = "activate_rules";
  public static final String BULK_DEACTIVATE_ACTION = "deactivate_rules";

  private final QProfileService profileService;
  private final RuleQueryFactory ruleQueryFactory;
  private final I18n i18n;
  private final UserSession userSession;

  public BulkRuleActivationActions(QProfileService profileService, RuleQueryFactory ruleQueryFactory, I18n i18n, UserSession userSession) {
    this.profileService = profileService;
    this.ruleQueryFactory = ruleQueryFactory;
    this.i18n = i18n;
    this.userSession = userSession;
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

    defineRuleSearchParameters(activate);
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

    defineRuleSearchParameters(deactivate);
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
      ruleQueryFactory.createRuleQuery(request),
      request.mandatoryParam(PROFILE_KEY),
      request.param(SEVERITY));
    writeResponse(result, response);
  }

  private void bulkDeactivate(Request request, Response response) {
    BulkChangeResult result = profileService.bulkDeactivate(
      ruleQueryFactory.createRuleQuery(request),
      request.mandatoryParam(PROFILE_KEY));
    writeResponse(result, response);
  }

  private void writeResponse(BulkChangeResult result, Response response) {
    JsonWriter json = response.newJsonWriter().beginObject();
    json.prop("succeeded", result.countSucceeded());
    json.prop("failed", result.countFailed());
    result.getErrors().writeJsonAsWarnings(json, i18n, userSession.locale());
    json.endObject().close();
  }
}
