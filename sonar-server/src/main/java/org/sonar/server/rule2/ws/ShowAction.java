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
package org.sonar.server.rule2.ws;

import com.google.common.io.Resources;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.ActiveRuleService;
import org.sonar.server.rule2.Rule;
import org.sonar.server.rule2.RuleService;
import org.sonar.server.search.BaseDoc;

import java.util.List;
import java.util.Map;

/**
 * @since 4.4
 */
public class ShowAction implements RequestHandler {

  private static final String PARAM_KEY = "key";
  private static final String PARAM_ACTIVATION = "activation";

  private final RuleService service;
  private final RuleMapping mapping;
  private final ActiveRuleService activeRuleService;

  public ShowAction(RuleService service, ActiveRuleService activeRuleService, RuleMapping mapping) {
    this.service = service;
    this.mapping = mapping;
    this.activeRuleService = activeRuleService;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("show")
      .setDescription("Get detailed information about a rule")
      .setSince("4.2")
      .setResponseExample(Resources.getResource(getClass(), "example-show.json"))
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setDescription("Rule key")
      .setRequired(true)
      .setExampleValue("javascript:EmptyBlock");

    action
      .createParam(PARAM_ACTIVATION)
      .setDescription("Show rule's activations for all profiles (ActiveRules)")
      .setRequired(false)
      .setDefaultValue("true")
      .setBooleanPossibleValues()
      .setExampleValue("true");
  }

  @Override
  public void handle(Request request, Response response) {
    RuleKey key = RuleKey.parse(request.mandatoryParam(PARAM_KEY));
    Rule rule = service.getByKey(key);
    if (rule == null) {
      throw new NotFoundException("Rule not found: " + key);
    }
    JsonWriter json = response.newJsonWriter().beginObject().name("rule");
    mapping.write((BaseDoc) rule, json);

    /** add activeRules (or not) */
    if (request.paramAsBoolean(PARAM_ACTIVATION)) {
      writeActiveRules(key, activeRuleService.findByRuleKey(key), json);
    }

    json.endObject().close();
  }

  private void writeActiveRules(RuleKey key, List<ActiveRule> activeRules, JsonWriter json) {
    json.name("actives").beginArray();
    for (ActiveRule activeRule : activeRules) {
      json
        .beginObject()
        .prop("qProfile", activeRule.key().qProfile().toString())
        .prop("inherit", activeRule.inheritance().toString())
        .prop("severity", activeRule.severity());
      if (activeRule.parentKey() != null) {
        json.prop("parent", activeRule.parentKey().toString());
      }
      json.name("params").beginArray();
      for (Map.Entry<String, String> param : activeRule.params().entrySet()) {
        json
          .beginObject()
          .prop("key", param.getKey())
          .prop("value", param.getValue())
          .endObject();
      }
      json.endArray()
        .endObject();
    }
    json.endArray();

  }
}
