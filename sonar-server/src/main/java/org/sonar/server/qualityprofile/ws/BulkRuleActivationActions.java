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
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.qualityprofile.ActiveRuleService;
import org.sonar.server.rule.RuleService;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.rule.ws.SearchAction;
import org.sonar.server.search.ws.SearchOptions;

public class BulkRuleActivationActions implements ServerComponent {

  private final ActiveRuleService service;
  private final RuleService ruleService;

  public BulkRuleActivationActions(ActiveRuleService service, RuleService ruleService) {
    this.service = service;
    this.ruleService = ruleService;
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

    SearchAction.defineRuleSearchParameters(activate);
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
    action.createParam("profile_key")
      .setDescription("Quality Profile Key. To retrieve a profileKey for a given language please see the /api/qprofile documentation")
      .setRequired(true)
      .setExampleValue("java:My Profile");
  }

  private void bulkActivate(Request request, Response response) throws Exception {
    Multimap<String, String> results = service.activateByRuleQuery(createRuleQuery(request), readKey(request));
    writeResponse(results, response);
  }

  private void bulkDeactivate(Request request, Response response) throws Exception {
    Multimap<String, String> results = service.deActivateByRuleQuery(createRuleQuery(request), readKey(request));
    writeResponse(results, response);
  }

  private void writeResponse(Multimap<String, String> results, Response response){
    JsonWriter json = response.newJsonWriter().beginObject();
    for(String action:results.keySet()){
      json.name(action).beginObject();
      for(String key:results.get(action)){
        json.prop("key",key);
      }
      json.endObject();
    }
    json.endObject();
  }

  private RuleQuery createRuleQuery(Request request) {
    RuleQuery query = ruleService.newRuleQuery();
    query.setQueryText(request.param(SearchOptions.PARAM_TEXT_QUERY));
    query.setSeverities(request.paramAsStrings(SearchAction.PARAM_SEVERITIES));
    query.setRepositories(request.paramAsStrings(SearchAction.PARAM_REPOSITORIES));
    query.setStatuses(request.paramAsEnums(SearchAction.PARAM_STATUSES, RuleStatus.class));
    query.setLanguages(request.paramAsStrings(SearchAction.PARAM_LANGUAGES));
    query.setDebtCharacteristics(request.paramAsStrings(SearchAction.PARAM_DEBT_CHARACTERISTICS));
    query.setHasDebtCharacteristic(request.paramAsBoolean(SearchAction.PARAM_HAS_DEBT_CHARACTERISTIC));
    query.setActivation(request.paramAsBoolean(SearchAction.PARAM_ACTIVATION));
    query.setQProfileKey(request.param(SearchAction.PARAM_QPROFILE));
    query.setTags(request.paramAsStrings(SearchAction.PARAM_TAGS));
    query.setAllOfTags(request.paramAsStrings(SearchAction.PARAM_ALL_OF_TAGS));
    return query;
  }

  private QualityProfileKey readKey(Request request) {
    return QualityProfileKey.parse(request.mandatoryParam("profile_key"));
  }
}
