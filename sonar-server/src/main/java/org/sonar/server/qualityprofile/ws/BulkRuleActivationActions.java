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

import com.google.common.collect.Multimap;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.rule.RuleService;
import org.sonar.server.rule.ws.SearchAction;

public class BulkRuleActivationActions implements ServerComponent {

  public static final String PROFILE_KEY = "profile_key";
  public static final String SEVERITY = "severity";

  public static final String BULK_ACTIVATE_ACTION = "activate_rules";
  public static final String BULK_DEACTIVATE_ACTION = "deactivate_rules";

  private final RuleActivator activation;
  private final RuleService ruleService;

  public BulkRuleActivationActions(QProfileService service, RuleActivator activation, RuleService ruleService) {
    this.activation = activation;
    this.ruleService = ruleService;
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
    defineProfileKeyParameters(activate);

    activate.createParam(SEVERITY)
      .setDescription("Set severity of rules activated in bulk")
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
    defineProfileKeyParameters(deactivate);
  }

  private void defineProfileKeyParameters(WebService.NewAction action) {
    action.createParam(PROFILE_KEY)
      .setDescription("Quality Profile Key. To retrieve a profileKey for a given language please see the /api/qprofile documentation")
      .setRequired(true)
      .setExampleValue("java:My Profile");
  }

  private void bulkActivate(Request request, Response response) throws Exception {
    Multimap<String, String> results = activation.bulkActivate(
      SearchAction.createRuleQuery(ruleService.newRuleQuery(), request),
      readKey(request),
      request.param(SEVERITY));
    writeResponse(results, response);
  }

  private void bulkDeactivate(Request request, Response response) throws Exception {
    Multimap<String, String> results = activation.bulkDeactivate(
      SearchAction.createRuleQuery(ruleService.newRuleQuery(), request),
      readKey(request));
    writeResponse(results, response);
  }

  private void writeResponse(Multimap<String, String> results, Response response){
    JsonWriter json = response.newJsonWriter().beginObject();
    for(String action:results.keySet()){
      json.name(action).beginArray();
      for(String key:results.get(action)){
        json.beginObject()
          .prop("key",key)
          .endObject();
      }
      json.endArray();
    }
    json.endObject().close();
  }

  private QualityProfileKey readKey(Request request) {
    return QualityProfileKey.parse(request.mandatoryParam(PROFILE_KEY));
  }
}
