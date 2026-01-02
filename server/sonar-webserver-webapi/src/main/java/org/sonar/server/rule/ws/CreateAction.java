/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.rule.ws;

import com.google.common.io.Resources;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.common.rule.ReactivationException;
import org.sonar.server.common.rule.service.NewCustomRule;
import org.sonar.server.common.rule.service.RuleInformation;
import org.sonar.server.common.rule.service.RuleService;
import org.sonarqube.ws.Rules;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateAction implements RulesWsAction {

  public static final String PARAM_CUSTOM_KEY = "customKey";
  public static final String PARAM_NAME = "name";
  public static final String PARAM_DESCRIPTION = "markdownDescription";
  public static final String PARAM_SEVERITY = "severity";
  public static final String PARAM_STATUS = "status";
  public static final String PARAM_TEMPLATE_KEY = "templateKey";
  public static final String PARAM_TYPE = "type";
  private static final String PARAM_IMPACTS = "impacts";
  private static final String PARAM_CLEAN_CODE_ATTRIBUTE = "cleanCodeAttribute";
  public static final String PARAMS = "params";

  static final int KEY_MAXIMUM_LENGTH = 200;
  static final int NAME_MAXIMUM_LENGTH = 200;

  /**
   * @deprecated since 10.4
   */
  @Deprecated(since = "10.4")
  private static final String PARAM_PREVENT_REACTIVATION = "preventReactivation";

  private final DbClient dbClient;
  private final RuleService ruleService;
  private final RuleMapper ruleMapper;
  private final RuleWsSupport ruleWsSupport;

  public CreateAction(DbClient dbClient, RuleService ruleService, RuleMapper ruleMapper, RuleWsSupport ruleWsSupport) {
    this.dbClient = dbClient;
    this.ruleService = ruleService;
    this.ruleMapper = ruleMapper;
    this.ruleWsSupport = ruleWsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("create")
      .setPost(true)
      .setDescription("Create a custom rule.<br>" +
        "Requires the 'Administer Quality Profiles' permission")
      .setResponseExample(Resources.getResource(getClass(), "create-example.json"))
      .setSince("4.4")
      .setChangelog(
        new Change("5.5", "Creating manual rule is not more possible"),
        new Change("10.0", "Drop deprecated keys: 'custom_key', 'template_key', 'markdown_description', 'prevent_reactivation'"),
        new Change("10.2", "Add 'impacts', 'cleanCodeAttribute', 'cleanCodeAttributeCategory' fields to the response"),
        new Change("10.2", "Fields 'type' and 'severity' are deprecated in the response. Use 'impacts' instead."),
        new Change("10.4", String.format("Add '%s' and '%s' parameters to the request", PARAM_IMPACTS, PARAM_CLEAN_CODE_ATTRIBUTE)),
        new Change("10.4", String.format("Parameters '%s' and '%s' are deprecated. Use '%s' instead.", PARAM_TYPE, PARAM_SEVERITY, PARAM_IMPACTS)),
        new Change("10.4", String.format("Parameter '%s' is deprecated. Use api/rules/update endpoint instead.", PARAM_PREVENT_REACTIVATION)),
        new Change("10.8", String.format("The parameters '%s' and '%s' are not deprecated anymore.", PARAM_TYPE, PARAM_SEVERITY)))
      .setHandler(this);

    action
      .createParam(PARAM_CUSTOM_KEY)
      .setRequired(true)
      .setMaximumLength(KEY_MAXIMUM_LENGTH)
      .setDescription("Key of the custom rule")
      .setExampleValue("Todo_should_not_be_used");

    action
      .createParam(PARAM_TEMPLATE_KEY)
      .setRequired(true)
      .setDescription("Key of the template rule in order to create a custom rule")
      .setExampleValue("java:XPath");

    action
      .createParam(PARAM_NAME)
      .setRequired(true)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setDescription("Rule name")
      .setExampleValue("My custom rule");

    action
      .createParam(PARAM_DESCRIPTION)
      .setRequired(true)
      .setDescription("Rule description in <a href='/formatting/help'>markdown format</a>")
      .setExampleValue("Description of my custom rule");

    action
      .createParam(PARAM_SEVERITY)
      .setPossibleValues(Severity.ALL)
      .setDescription("Rule severity");
    action
      .createParam(PARAM_STATUS)
      .setPossibleValues(
        Arrays.stream(RuleStatus.values())
          .filter(status -> RuleStatus.REMOVED != status)
          .toList())
      .setDefaultValue(RuleStatus.READY)
      .setDescription("Rule status");

    action.createParam(PARAMS)
      .setDescription("Parameters as semi-colon list of &lt;key&gt;=&lt;value&gt;")
      .setExampleValue("key1=v1;key2=v2");

    action
      .createParam(PARAM_PREVENT_REACTIVATION)
      .setDeprecatedSince("10.4")
      .setBooleanPossibleValues()
      .setDefaultValue(false)
      .setDescription("If set to true and if the rule has been deactivated (status 'REMOVED'), a status 409 will be returned");

    action.createParam(PARAM_TYPE)
      .setPossibleValues(RuleType.names())
      .setDescription("Rule type")
      .setSince("6.7");

    action.createParam(PARAM_CLEAN_CODE_ATTRIBUTE)
      .setDescription("Clean code attribute")
      .setPossibleValues(CleanCodeAttribute.values())
      .setSince("10.4");

    action.createParam(PARAM_IMPACTS)
      .setDescription("Impacts as semi-colon list of &lt;software_quality&gt;=&lt;severity&gt;")
      .setExampleValue("SECURITY=HIGH;MAINTAINABILITY=LOW")
      .setSince("10.4");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ruleWsSupport.checkQProfileAdminPermission();
    try (DbSession dbSession = dbClient.openSession(false)) {
      try {
        NewCustomRule newCustomRule = toNewCustomRule(request);
        RuleInformation customRule = ruleService.createCustomRule(newCustomRule, dbSession);
        writeResponse(dbSession, request, response, customRule.ruleDto(), customRule.params());
      } catch (ReactivationException e) {
        response.stream().setStatus(HTTP_CONFLICT);
        writeResponse(dbSession, request, response, e.ruleKey());
      }
    }
  }

  private static NewCustomRule toNewCustomRule(Request request) {
    RuleKey templateKey = RuleKey.parse(request.mandatoryParam(PARAM_TEMPLATE_KEY));
    NewCustomRule newRule = NewCustomRule.createForCustomRule(
      RuleKey.of(templateKey.repository(), request.mandatoryParam(PARAM_CUSTOM_KEY)), templateKey)
      .setName(request.mandatoryParam(PARAM_NAME))
      .setMarkdownDescription(request.mandatoryParam(PARAM_DESCRIPTION))
      .setStatus(RuleStatus.valueOf(request.mandatoryParam(PARAM_STATUS)))
      .setPreventReactivation(request.mandatoryParamAsBoolean(PARAM_PREVENT_REACTIVATION))
      .setSeverity(request.param(PARAM_SEVERITY))
      .setType(ofNullable(request.param(PARAM_TYPE)).map(RuleType::valueOf).orElse(null))
      .setCleanCodeAttribute(ofNullable(request.param(PARAM_CLEAN_CODE_ATTRIBUTE)).map(CleanCodeAttribute::valueOf).orElse(null));
    String params = request.param(PARAMS);
    if (!isNullOrEmpty(params)) {
      newRule.setParameters(KeyValueFormat.parse(params));
    }
    String impacts = request.param(PARAM_IMPACTS);
    if (!isNullOrEmpty(impacts)) {
      newRule.setImpacts(KeyValueFormat.parse(impacts).entrySet().stream()
        .map(e -> new NewCustomRule.Impact(SoftwareQuality.valueOf(e.getKey()), org.sonar.api.issue.impact.Severity.valueOf(e.getValue())))
        .toList());
    }
    return newRule;
  }

  private void writeResponse(DbSession dbSession, Request request, Response response, RuleDto rule, List<RuleParamDto> params) {
    writeProtobuf(createResponse(dbSession, rule, params), request, response);
  }

  private void writeResponse(DbSession dbSession, Request request, Response response, RuleKey ruleKey) {
    RuleDto rule = dbClient.ruleDao().selectByKey(dbSession, ruleKey)
      .orElseThrow(() -> new IllegalStateException(String.format("Cannot load rule, that has just been created '%s'", ruleKey)));
    List<RuleParamDto> ruleParameters = dbClient.ruleDao().selectRuleParamsByRuleUuids(dbSession, singletonList(rule.getUuid()));
    writeProtobuf(createResponse(dbSession, rule, ruleParameters), request, response);
  }

  private Rules.CreateResponse createResponse(DbSession dbSession, RuleDto rule, List<RuleParamDto> params) {
    List<RuleDto> templateRules = new ArrayList<>();
    if (rule.isCustomRule()) {
      Optional<RuleDto> templateRule = dbClient.ruleDao().selectByUuid(rule.getTemplateUuid(), dbSession);
      templateRule.ifPresent(templateRules::add);
    }

    RulesResponseFormatter.SearchResult searchResult = new RulesResponseFormatter.SearchResult()
      .setRuleParameters(params)
      .setTemplateRules(templateRules)
      .setTotal(1L);
    return Rules.CreateResponse.newBuilder()
      .setRule(ruleMapper.toWsRule(rule, searchResult, Collections.emptySet()))
      .build();
  }
}
