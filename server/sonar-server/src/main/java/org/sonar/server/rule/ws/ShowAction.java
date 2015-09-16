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
package org.sonar.server.rule.ws;

import com.google.common.io.Resources;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonarqube.ws.Rules.ShowResponse;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

/**
 * @since 4.4
 */
public class ShowAction implements RulesWsAction {

  public static final String PARAM_KEY = "key";
  public static final String PARAM_ACTIVES = "actives";

  private final RuleService service;
  private final RuleMapping mapping;
  private final ActiveRuleCompleter activeRuleCompleter;

  public ShowAction(RuleService service, ActiveRuleCompleter activeRuleCompleter, RuleMapping mapping) {
    this.service = service;
    this.mapping = mapping;
    this.activeRuleCompleter = activeRuleCompleter;
  }

  @Override
  public void define(WebService.NewController controller) {
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
      .createParam(PARAM_ACTIVES)
      .setDescription("Show rule's activations for all profiles (\"active rules\")")
      .setBooleanPossibleValues()
      .setDefaultValue(false);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    RuleKey key = RuleKey.parse(request.mandatoryParam(PARAM_KEY));
    Rule rule = service.getByKey(key);
    if (rule == null) {
      throw new NotFoundException("Rule not found: " + key);
    }

    ShowResponse showResponse = buildResponse(request, rule);
    writeProtobuf(showResponse, request, response);
  }

  private ShowResponse buildResponse(Request request, Rule rule) {
    ShowResponse.Builder responseBuilder = ShowResponse.newBuilder();
    responseBuilder.setRule(mapping.buildRuleResponse(rule, null /* TODO replace by SearchOptions immutable constant */));

    if (request.mandatoryParamAsBoolean(PARAM_ACTIVES)) {
      activeRuleCompleter.completeShow(rule, responseBuilder);
    }

    return responseBuilder.build();
  }
}
