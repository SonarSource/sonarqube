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

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.NewRule;
import org.sonar.server.rule.ReactivationException;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonarqube.ws.Rules;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

/**
 * @since 4.4
 */
public class CreateAction implements RulesWsAction {

  public static final String PARAM_CUSTOM_KEY = "custom_key";
  public static final String PARAM_MANUAL_KEY = "manual_key";
  public static final String PARAM_NAME = "name";
  public static final String PARAM_DESCRIPTION = "markdown_description";
  public static final String PARAM_SEVERITY = "severity";
  public static final String PARAM_STATUS = "status";
  public static final String PARAM_TEMPLATE_KEY = "template_key";
  public static final String PARAMS = "params";

  public static final String PARAM_PREVENT_REACTIVATION = "prevent_reactivation";

  private final RuleService service;
  private final RuleMapping mapping;

  public CreateAction(RuleService service, RuleMapping mapping) {
    this.service = service;
    this.mapping = mapping;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("create")
      .setDescription("Create a custom rule or a manual rule")
      .setSince("4.4")
      .setPost(true)
      .setHandler(this);

    action
      .createParam(PARAM_CUSTOM_KEY)
      .setDescription("Key of the custom rule")
      .setExampleValue("Todo_should_not_be_used");

    action
      .createParam(PARAM_MANUAL_KEY)
      .setDescription("Key of the manual rule")
      .setExampleValue("Error_handling");

    action
      .createParam(PARAM_TEMPLATE_KEY)
      .setDescription("Key of the template rule in order to create a custom rule (mandatory for custom rule)")
      .setExampleValue("java:XPath");

    action
      .createParam(PARAM_NAME)
      .setDescription("Rule name")
      .setRequired(true)
      .setExampleValue("My custom rule");

    action
      .createParam(PARAM_DESCRIPTION)
      .setDescription("Rule description")
      .setRequired(true)
      .setExampleValue("Description of my custom rule");

    action
      .createParam(PARAM_SEVERITY)
      .setDescription("Rule severity (Only for custom rule)")
      .setPossibleValues(Severity.ALL);

    action
      .createParam(PARAM_STATUS)
      .setDescription("Rule status (Only for custom rule)")
      .setDefaultValue(RuleStatus.READY)
      .setPossibleValues(RuleStatus.values());

    action.createParam(PARAMS)
      .setDescription("Parameters as semi-colon list of <key>=<value>, for example 'params=key1=v1;key2=v2' (Only for custom rule)");

    action
      .createParam(PARAM_PREVENT_REACTIVATION)
      .setDescription("If set to true and if the rule has been deactivated (status 'REMOVED'), a status 409 will be returned")
      .setDefaultValue(false)
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String customKey = request.param(PARAM_CUSTOM_KEY);
    String manualKey = request.param(PARAM_MANUAL_KEY);
    if (isNullOrEmpty(customKey) && isNullOrEmpty(manualKey)) {
      throw new BadRequestException(String.format("Either '%s' or '%s' parameters should be set", PARAM_CUSTOM_KEY, PARAM_MANUAL_KEY));
    }

    try {
      if (!isNullOrEmpty(customKey)) {
        NewRule newRule = NewRule.createForCustomRule(customKey, RuleKey.parse(request.mandatoryParam(PARAM_TEMPLATE_KEY)))
          .setName(request.mandatoryParam(PARAM_NAME))
          .setMarkdownDescription(request.mandatoryParam(PARAM_DESCRIPTION))
          .setSeverity(request.mandatoryParam(PARAM_SEVERITY))
          .setStatus(RuleStatus.valueOf(request.mandatoryParam(PARAM_STATUS)))
          .setPreventReactivation(request.mandatoryParamAsBoolean(PARAM_PREVENT_REACTIVATION));
        String params = request.param(PARAMS);
        if (!isNullOrEmpty(params)) {
          newRule.setParameters(KeyValueFormat.parse(params));
        }
        writeResponse(request, response, service.create(newRule));
      }

      if (!isNullOrEmpty(manualKey)) {
        NewRule newRule = NewRule.createForManualRule(manualKey)
          .setName(request.mandatoryParam(PARAM_NAME))
          .setMarkdownDescription(request.mandatoryParam(PARAM_DESCRIPTION))
          .setSeverity(request.param(PARAM_SEVERITY))
          .setPreventReactivation(request.mandatoryParamAsBoolean(PARAM_PREVENT_REACTIVATION));
        writeResponse(request, response, service.create(newRule));
      }
    } catch (ReactivationException e) {
      write409(request, response, e.ruleKey());
    }
  }

  private void writeResponse(Request request, Response response, RuleKey ruleKey) throws Exception {
    Rule rule = service.getNonNullByKey(ruleKey);
    Rules.CreateResponse createResponse = Rules.CreateResponse.newBuilder()
      .setRule(mapping.buildRuleResponse(rule, null /* TODO replace by SearchOptions immutable constant */))
      .build();

    writeProtobuf(createResponse, request, response);
  }

  private void write409(Request request, Response response, RuleKey ruleKey) throws Exception {
    Rule rule = service.getNonNullByKey(ruleKey);

    response.stream().setStatus(HTTP_CONFLICT);
    Rules.CreateResponse createResponse = Rules.CreateResponse.newBuilder()
      .setRule(mapping.buildRuleResponse(rule, null /* TODO replace by SearchOptions immutable constant */))
      .build();

    writeProtobuf(createResponse, request, response);
  }
}
