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

import com.google.common.base.Strings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.rule.NewRule;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.search.BaseDoc;

/**
 * @since 4.4
 */
public class CreateAction implements RequestHandler {

  public static final String PARAM_KEY = "key";
  public static final String PARAM_NAME = "name";
  public static final String PARAM_DESCRIPTION = "html_description";
  public static final String PARAM_SEVERITY = "severity";
  public static final String PARAM_STATUS = "status";
  public static final String PARAM_TEMPLATE_KEY = "template_key";
  public static final String PARAMS = "params";

  private final RuleService service;
  private final RuleMapping mapping;

  public CreateAction(RuleService service, RuleMapping mapping) {
    this.service = service;
    this.mapping = mapping;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("create")
      .setDescription("Create a custom rule or a manual rule")
      .setSince("4.4")
      .setPost(true)
      .setHandler(this);

    action
      .createParam(PARAM_KEY)
      .setDescription("Key of the rule")
      .setExampleValue("Todo_should_not_be_used");

    action
      .createParam(PARAM_TEMPLATE_KEY)
      .setDescription("Key of the template rule in order to create a custom rule")
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
      .setDescription("Rule severity")
      .setRequired(true)
      .setPossibleValues(Severity.ALL);

    action
      .createParam(PARAM_STATUS)
      .setDescription("Rule status")
      .setRequired(true)
      .setDefaultValue(RuleStatus.READY)
      .setPossibleValues(RuleStatus.values());

    action.createParam(PARAMS)
      .setDescription("Parameters as semi-colon list of <key>=<value>, for example 'params=key1=v1;key2=v2'");
  }

  @Override
  public void handle(Request request, Response response) {
    String templateRuleKey = request.param(PARAM_TEMPLATE_KEY);
    NewRule newRule = new NewRule()
      .setRuleKey(request.mandatoryParam(PARAM_KEY))
      .setTemplateKey(templateRuleKey != null ? RuleKey.parse(templateRuleKey) : null)
      .setName(request.mandatoryParam(PARAM_NAME))
      .setHtmlDescription(request.mandatoryParam(PARAM_DESCRIPTION))
      .setSeverity(request.mandatoryParam(PARAM_SEVERITY))
      .setStatus(RuleStatus.valueOf(request.mandatoryParam(PARAM_STATUS)));
    String params = request.param(PARAMS);
    if (!Strings.isNullOrEmpty(params)) {
      newRule.setParameters(KeyValueFormat.parse(params));
    }
    writeResponse(response, service.create(newRule));
  }

  private void writeResponse(Response response, RuleKey ruleKey) {
    Rule rule = service.getByKey(ruleKey);
    JsonWriter json = response.newJsonWriter().beginObject().name("rule");
    mapping.write((BaseDoc) rule, json);
    json.endObject().close();
  }
}
