/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.google.common.base.Splitter;
import com.google.common.io.Resources;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.RuleUpdate;
import org.sonar.server.rule.RuleUpdater;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Rules.UpdateResponse;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.server.rule.ws.CreateAction.KEY_MAXIMUM_LENGTH;
import static org.sonar.server.rule.ws.CreateAction.NAME_MAXIMUM_LENGTH;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class UpdateAction implements RulesWsAction {

  public static final String PARAM_KEY = "key";
  public static final String PARAM_TAGS = "tags";
  public static final String PARAM_MARKDOWN_NOTE = "markdown_note";
  public static final String PARAM_REMEDIATION_FN_TYPE = "remediation_fn_type";
  public static final String PARAM_REMEDIATION_FN_BASE_EFFORT = "remediation_fn_base_effort";
  public static final String PARAM_REMEDIATION_FN_GAP_MULTIPLIER = "remediation_fy_gap_multiplier";
  public static final String PARAM_NAME = "name";
  public static final String PARAM_DESCRIPTION = "markdownDescription";
  public static final String PARAM_SEVERITY = "severity";
  public static final String PARAM_STATUS = "status";
  public static final String PARAMS = "params";

  private final DbClient dbClient;
  private final RuleUpdater ruleUpdater;
  private final RuleMapper mapper;
  private final UserSession userSession;
  private final RuleWsSupport ruleWsSupport;

  public UpdateAction(DbClient dbClient, RuleUpdater ruleUpdater, RuleMapper mapper, UserSession userSession, RuleWsSupport ruleWsSupport) {
    this.dbClient = dbClient;
    this.ruleUpdater = ruleUpdater;
    this.mapper = mapper;
    this.userSession = userSession;
    this.ruleWsSupport = ruleWsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("update")
      .setPost(true)
      .setResponseExample(Resources.getResource(getClass(), "update-example.json"))
      .setDescription("Update an existing rule.<br>" +
        "Requires the 'Administer Quality Profiles' permission")
      .setChangelog(
        new Change("10.2", "The field 'severity' and 'type' in the response have been deprecated, use 'impacts' instead.")
      )
      .setSince("4.4")
      .setChangelog(
        new Change("10.2", "Add 'impacts', 'cleanCodeAttribute', 'cleanCodeAttributeCategory' fields to the response"))
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setMaximumLength(KEY_MAXIMUM_LENGTH)
      .setDescription("Key of the rule to update")
      .setExampleValue("javascript:NullCheck");

    action.createParam(PARAM_TAGS)
      .setDescription("Optional comma-separated list of tags to set. Use blank value to remove current tags. Tags " +
        "are not changed if the parameter is not set.")
      .setExampleValue("java8,security");

    action.createParam(PARAM_MARKDOWN_NOTE)
      .setDescription("Optional note in <a href='/formatting/help'>markdown format</a>. Use empty value to remove current note. Note is not changed " +
        "if the parameter is not set.")
      .setExampleValue("my *note*");

    action.createParam(PARAM_REMEDIATION_FN_TYPE)
      .setDescription("Type of the remediation function of the rule")
      .setPossibleValues(DebtRemediationFunction.Type.values())
      .setSince("5.5");

    action.createParam(PARAM_REMEDIATION_FN_BASE_EFFORT)
      .setDescription("Base effort of the remediation function of the rule")
      .setExampleValue("1d")
      .setSince("5.5");

    action.createParam(PARAM_REMEDIATION_FN_GAP_MULTIPLIER)
      .setDescription("Gap multiplier of the remediation function of the rule")
      .setExampleValue("3min")
      .setSince("5.5");

    action
      .createParam(PARAM_NAME)
      .setMaximumLength(NAME_MAXIMUM_LENGTH)
      .setDescription("Rule name (mandatory for custom rule)")
      .setExampleValue("My custom rule");

    action
      .createParam(PARAM_DESCRIPTION)
      .setDescription("Rule description (mandatory for custom rule and manual rule) in <a href='/formatting/help'>markdown format</a>")
      .setExampleValue("Description of my custom rule")
      .setDeprecatedKey("markdown_description", "10.2");

    action
      .createParam(PARAM_SEVERITY)
      .setDescription("Rule severity (Only when updating a custom rule)")
      .setPossibleValues(Severity.ALL);

    action
      .createParam(PARAM_STATUS)
      .setPossibleValues(RuleStatus.values())
      .setDescription("Rule status (Only when updating a custom rule)");

    action.createParam(PARAMS)
      .setDescription("Parameters as semi-colon list of <key>=<value>, for example 'params=key1=v1;key2=v2' (Only when updating a custom rule)");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      ruleWsSupport.checkQProfileAdminPermission();
      RuleUpdate update = readRequest(dbSession, request);
      ruleUpdater.update(dbSession, update, userSession);
      UpdateResponse updateResponse = buildResponse(dbSession, update.getRuleKey());

      writeProtobuf(updateResponse, request, response);
    }
  }

  private RuleUpdate readRequest(DbSession dbSession, Request request) {
    RuleKey key = RuleKey.parse(request.mandatoryParam(PARAM_KEY));
    RuleUpdate update = createRuleUpdate(dbSession, key);
    readTags(request, update);
    readMarkdownNote(request, update);
    readDebt(request, update);

    String name = request.param(PARAM_NAME);
    if (name != null) {
      update.setName(name);
    }
    String description = request.param(PARAM_DESCRIPTION);
    if (description != null) {
      update.setMarkdownDescription(description);
    }
    String severity = request.param(PARAM_SEVERITY);
    if (severity != null) {
      update.setSeverity(severity);
    }
    String status = request.param(PARAM_STATUS);
    if (status != null) {
      update.setStatus(RuleStatus.valueOf(status));
    }
    String params = request.param(PARAMS);
    if (params != null) {
      update.setParameters(KeyValueFormat.parse(params));
    }
    return update;
  }

  private RuleUpdate createRuleUpdate(DbSession dbSession, RuleKey key) {
    RuleDto rule = dbClient.ruleDao().selectByKey(dbSession, key)
      .orElseThrow(() -> new NotFoundException(format("This rule does not exist: %s", key)));
    return ofNullable(rule.getTemplateUuid())
      .map(x -> RuleUpdate.createForCustomRule(key))
      .orElseGet(() -> RuleUpdate.createForPluginRule(key));
  }

  private static void readTags(Request request, RuleUpdate update) {
    String value = request.param(PARAM_TAGS);
    if (value != null) {
      if (StringUtils.isBlank(value)) {
        update.setTags(null);
      } else {
        update.setTags(newHashSet(Splitter.on(',').omitEmptyStrings().trimResults().split(value)));
      }
    }
    // else do not touch this field
  }

  private static void readMarkdownNote(Request request, RuleUpdate update) {
    String value = request.param(PARAM_MARKDOWN_NOTE);
    if (value != null) {
      update.setMarkdownNote(value);
    }
    // else do not touch this field
  }

  private static void readDebt(Request request, RuleUpdate update) {
    String value = request.param(PARAM_REMEDIATION_FN_TYPE);
    if (value != null) {
      if (StringUtils.isBlank(value)) {
        update.setDebtRemediationFunction(null);
      } else {
        DebtRemediationFunction fn = new DefaultDebtRemediationFunction(
          DebtRemediationFunction.Type.valueOf(value),
          request.param(PARAM_REMEDIATION_FN_GAP_MULTIPLIER),
          request.param(PARAM_REMEDIATION_FN_BASE_EFFORT));
        update.setDebtRemediationFunction(fn);
      }
    }
  }

  private UpdateResponse buildResponse(DbSession dbSession, RuleKey key) {
    RuleDto rule = dbClient.ruleDao().selectByKey(dbSession, key)
      .orElseThrow(() -> new NotFoundException(format("Rule not found: %s", key)));
    List<RuleDto> templateRules = new ArrayList<>(1);
    if (rule.isCustomRule()) {
      dbClient.ruleDao().selectByUuid(rule.getTemplateUuid(), dbSession).ifPresent(templateRules::add);
    }
    List<RuleParamDto> ruleParameters = dbClient.ruleDao().selectRuleParamsByRuleUuids(dbSession, singletonList(rule.getUuid()));
    UpdateResponse.Builder responseBuilder = UpdateResponse.newBuilder();
    RulesResponseFormatter.SearchResult searchResult = new RulesResponseFormatter.SearchResult()
      .setRules(singletonList(rule))
      .setTemplateRules(templateRules)
      .setRuleParameters(ruleParameters)
      .setTotal(1L);
    responseBuilder
      .setRule(mapper.toWsRule(rule, searchResult, Collections.emptySet(),
        ruleWsSupport.getUsersByUuid(dbSession, singletonList(rule)), emptyMap()));

    return responseBuilder.build();
  }
}
