/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.rule.NewCustomRule;
import org.sonar.server.rule.ReactivationException;
import org.sonar.server.rule.RuleCreator;
import org.sonarqube.ws.Rules;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.util.Collections.singletonList;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateAction implements RulesWsAction {

  public static final String PARAM_CUSTOM_KEY = "custom_key";
  public static final String PARAM_NAME = "name";
  public static final String PARAM_DESCRIPTION = "markdown_description";
  public static final String PARAM_SEVERITY = "severity";
  public static final String PARAM_STATUS = "status";
  public static final String PARAM_TEMPLATE_KEY = "template_key";
  public static final String PARAM_TYPE = "type";
  public static final String PARAMS = "params";

  public static final String PARAM_PREVENT_REACTIVATION = "prevent_reactivation";
  static final int KEY_MAXIMUM_LENGTH = 200;
  static final int NAME_MAXIMUM_LENGTH = 200;

  private final DbClient dbClient;
  private final RuleCreator ruleCreator;
  private final RuleMapper ruleMapper;
  private final RuleWsSupport ruleWsSupport;

  public CreateAction(DbClient dbClient, RuleCreator ruleCreator, RuleMapper ruleMapper, RuleWsSupport ruleWsSupport) {
    this.dbClient = dbClient;
    this.ruleCreator = ruleCreator;
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
      .setSince("4.4")
      .setChangelog(
        new Change("5.5", "Creating manual rule is not more possible"))
      .setHandler(this);

    action
      .createParam(PARAM_CUSTOM_KEY)
      .setRequired(true)
      .setMaximumLength(KEY_MAXIMUM_LENGTH)
      .setDescription("Key of the custom rule")
      .setExampleValue("Todo_should_not_be_used");

    action
      .createParam("manual_key")
      .setDescription("Manual rules are no more supported. This parameter is ignored")
      .setExampleValue("Error_handling")
      .setDeprecatedSince("5.5");

    action
      .createParam(PARAM_TEMPLATE_KEY)
      .setDescription("Key of the template rule in order to create a custom rule (mandatory for custom rule)")
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
      .setDescription("Rule description")
      .setExampleValue("Description of my custom rule");

    action
      .createParam(PARAM_SEVERITY)
      .setPossibleValues(Severity.ALL)
      .setDescription("Rule severity");

    action
      .createParam(PARAM_STATUS)
      .setPossibleValues(RuleStatus.values())
      .setDefaultValue(RuleStatus.READY)
      .setDescription("Rule status");

    action.createParam(PARAMS)
      .setDescription("Parameters as semi-colon list of <key>=<value>, for example 'params=key1=v1;key2=v2' (Only for custom rule)");

    action
      .createParam(PARAM_PREVENT_REACTIVATION)
      .setBooleanPossibleValues()
      .setDefaultValue(false)
      .setDescription("If set to true and if the rule has been deactivated (status 'REMOVED'), a status 409 will be returned");

    action.createParam(PARAM_TYPE)
      .setPossibleValues(RuleType.names())
      .setDescription("Rule type")
      .setSince("6.7");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ruleWsSupport.checkQProfileAdminPermissionOnDefaultOrganization();
    String customKey = request.mandatoryParam(PARAM_CUSTOM_KEY);
    try (DbSession dbSession = dbClient.openSession(false)) {
      try {
        NewCustomRule newRule = NewCustomRule.createForCustomRule(customKey, RuleKey.parse(request.mandatoryParam(PARAM_TEMPLATE_KEY)))
          .setName(request.mandatoryParam(PARAM_NAME))
          .setMarkdownDescription(request.mandatoryParam(PARAM_DESCRIPTION))
          .setSeverity(request.mandatoryParam(PARAM_SEVERITY))
          .setStatus(RuleStatus.valueOf(request.mandatoryParam(PARAM_STATUS)))
          .setPreventReactivation(request.mandatoryParamAsBoolean(PARAM_PREVENT_REACTIVATION));
        String params = request.param(PARAMS);
        if (!isNullOrEmpty(params)) {
          newRule.setParameters(KeyValueFormat.parse(params));
        }
        setNullable(request.param(PARAM_TYPE), t -> newRule.setType(RuleType.valueOf(t)));
        writeResponse(dbSession, request, response, ruleCreator.create(dbSession, newRule));
      } catch (ReactivationException e) {
        response.stream().setStatus(HTTP_CONFLICT);
        writeResponse(dbSession, request, response, e.ruleKey());
      }
    }
  }

  private void writeResponse(DbSession dbSession, Request request, Response response, RuleKey ruleKey) {
    writeProtobuf(createResponse(dbSession, ruleKey), request, response);
  }

  private Rules.CreateResponse createResponse(DbSession dbSession, RuleKey ruleKey) {
    RuleDefinitionDto rule = dbClient.ruleDao().selectDefinitionByKey(dbSession, ruleKey)
      .orElseThrow(() -> new IllegalStateException(String.format("Cannot load rule, that has just been created '%s'", ruleKey)));
    List<RuleDefinitionDto> templateRules = new ArrayList<>();
    if (rule.isCustomRule()) {
      Optional<RuleDefinitionDto> templateRule = dbClient.ruleDao().selectDefinitionById(rule.getTemplateId(), dbSession);
      templateRule.ifPresent(templateRules::add);
    }
    List<RuleParamDto> ruleParameters = dbClient.ruleDao().selectRuleParamsByRuleIds(dbSession, singletonList(rule.getId()));
    SearchAction.SearchResult searchResult = new SearchAction.SearchResult()
      .setRuleParameters(ruleParameters)
      .setTemplateRules(templateRules)
      .setTotal(1L);
    return Rules.CreateResponse.newBuilder()
      .setRule(ruleMapper.toWsRule(rule, searchResult, Collections.emptySet()))
      .build();
  }
}
