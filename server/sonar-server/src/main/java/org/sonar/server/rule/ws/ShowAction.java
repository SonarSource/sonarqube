/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.util.Collections;
import java.util.List;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonarqube.ws.Rules.ShowResponse;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

/**
 * @since 4.4
 */
public class ShowAction implements RulesWsAction {

  public static final String PARAM_KEY = "key";
  public static final String PARAM_ACTIVES = "actives";
  public static final String PARAM_ORGANIZATION = "organization";

  private final DbClient dbClient;
  private final RuleMapper mapper;
  private final ActiveRuleCompleter activeRuleCompleter;
  private final RuleWsSupport ruleWsSupport;

  public ShowAction(DbClient dbClient, RuleMapper mapper, ActiveRuleCompleter activeRuleCompleter, RuleWsSupport ruleWsSupport) {
    this.dbClient = dbClient;
    this.activeRuleCompleter = activeRuleCompleter;
    this.mapper = mapper;
    this.ruleWsSupport = ruleWsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("show")
      .setDescription("Get detailed information about a rule<br>" +
        "Since 5.5, following fields in the response have been deprecated :" +
        "<ul>" +
        "<li>\"effortToFixDescription\" becomes \"gapDescription\"</li>" +
        "<li>\"debtRemFnCoeff\" becomes \"remFnGapMultiplier\"</li>" +
        "<li>\"defaultDebtRemFnCoeff\" becomes \"defaultRemFnGapMultiplier\"</li>" +
        "<li>\"debtRemFnOffset\" becomes \"remFnBaseEffort\"</li>" +
        "<li>\"defaultDebtRemFnOffset\" becomes \"defaultRemFnBaseEffort\"</li>" +
        "<li>\"debtOverloaded\" becomes \"remFnOverloaded\"</li>" +
        "</ul>" +
        "In 7.1, the field 'scope' has been added.")
      .setSince("4.2")
      .setResponseExample(Resources.getResource(getClass(), "show-example.json"))
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

    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setRequired(false)
      .setInternal(true)
      .setExampleValue("my-org")
      .setSince("6.4");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    RuleKey key = RuleKey.parse(request.mandatoryParam(PARAM_KEY));
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = ruleWsSupport.getOrganizationByKey(dbSession, request.param(PARAM_ORGANIZATION));
      RuleDto rule = dbClient.ruleDao().selectByKey(dbSession, organization.getUuid(), key)
        .orElseThrow(() -> new NotFoundException(String.format("Rule not found: %s", key)));

      List<RuleDefinitionDto> templateRules = ofNullable(rule.getTemplateId())
        .flatMap(templateId -> dbClient.ruleDao().selectDefinitionById(rule.getTemplateId(), dbSession))
        .map(Collections::singletonList).orElseGet(Collections::emptyList);

      List<RuleParamDto> ruleParameters = dbClient.ruleDao().selectRuleParamsByRuleIds(dbSession, singletonList(rule.getId()));
      ShowResponse showResponse = buildResponse(dbSession, organization, request,
        new SearchAction.SearchResult()
          .setRules(singletonList(rule))
          .setTemplateRules(templateRules)
          .setRuleParameters(ruleParameters)
          .setTotal(1L));
      writeProtobuf(showResponse, request, response);
    }
  }

  private ShowResponse buildResponse(DbSession dbSession, OrganizationDto organization, Request request, SearchAction.SearchResult searchResult) {
    ShowResponse.Builder responseBuilder = ShowResponse.newBuilder();
    RuleDto rule = searchResult.getRules().get(0);
    responseBuilder.setRule(mapper.toWsRule(rule.getDefinition(), searchResult, Collections.emptySet(), rule.getMetadata(),
      ruleWsSupport.getUsersByUuid(dbSession, searchResult.getRules())));
    if (request.mandatoryParamAsBoolean(PARAM_ACTIVES) && ruleWsSupport.areActiveRulesVisible(organization)) {
      activeRuleCompleter.completeShow(dbSession, organization, rule.getDefinition()).forEach(responseBuilder::addActives);
    }
    return responseBuilder.build();
  }

}
