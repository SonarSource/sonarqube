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
import java.util.Collections;
import java.util.List;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonarqube.ws.Rules.ShowResponse;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.INFO;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

/**
 * @since 4.4
 */
public class ShowAction implements RulesWsAction {

  public static final String PARAM_KEY = "key";
  public static final String PARAM_ACTIVES = "actives";

  private final DbClient dbClient;
  private final RulesResponseFormatter rulesResponseFormatter;

  public ShowAction(DbClient dbClient, RulesResponseFormatter rulesResponseFormatter) {
    this.dbClient = dbClient;
    this.rulesResponseFormatter = rulesResponseFormatter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("show")
      .setDescription("Get detailed information about a rule<br>")
      .setSince("4.2")
      .setResponseExample(Resources.getResource(getClass(), "show-example.json"))
      .setHandler(this)
      .setChangelog(
        new Change("5.5", "The field 'effortToFixDescription' in the response has been deprecated, it becomes 'gapDescription'."),
        new Change("5.5", "The field 'debtRemFnCoeff' in the response has been deprecated, it becomes 'remFnGapMultiplier'."),
        new Change("5.5", "The field 'defaultDebtRemFnCoeff' in the response has been deprecated, it becomes 'defaultRemFnGapMultiplier'."),
        new Change("5.5", "The field 'debtRemFnOffset' in the response has been deprecated, it becomes 'remFnBaseEffort'."),
        new Change("5.5", "The field 'defaultDebtRemFnOffset' in the response has been deprecated, it becomes 'defaultRemFnBaseEffort'."),
        new Change("5.5", "The field 'debtOverloaded' in the response has been deprecated, it becomes 'remFnOverloaded'."),
        new Change("7.1", "The field 'scope' has been added."),
        new Change("9.5", "The field 'htmlDesc' in the response has been deprecated, it becomes 'descriptionSections'."),
        new Change("9.5", "The field 'descriptionSections' has been added to the payload."),
        new Change("9.6", "'descriptionSections' can optionally embed a context field."),
        new Change("9.6", "'educationPrinciples' has been added."),
        new Change("10.0", "The deprecated field 'effortToFixDescription' has been removed, use 'gapDescription' instead."),
        new Change("10.0", "The deprecated field 'debtRemFnCoeff' has been removed, use 'remFnGapMultiplier' instead."),
        new Change("10.0", "The deprecated field 'defaultDebtRemFnCoeff' has been removed, use 'defaultRemFnGapMultiplier' instead."),
        new Change("10.0", "The deprecated field 'debtRemFnOffset' has been removed, use 'remFnBaseEffort' instead."),
        new Change("10.0", "The deprecated field 'defaultDebtRemFnOffset' has been removed, use 'defaultRemFnBaseEffort' instead."),
        new Change("10.0", "The deprecated field 'debtOverloaded' has been removed, use 'remFnOverloaded' instead."),
        new Change("10.0", "The field 'defaultDebtRemFnType' has been deprecated, use 'defaultRemFnType' instead"),
        new Change("10.0", "The field 'debtRemFnType' has been deprecated, use 'remFnType' instead"),
        new Change("10.2", "Add 'impacts', 'cleanCodeAttribute', 'cleanCodeAttributeCategory' fields to the response"),
        new Change("10.2", "The field 'severity' and 'type' in the response have been deprecated, use 'impacts' instead."),
        new Change("10.8", format("Possible values '%s' and '%s' for response field 'severity' of 'impacts' have been added.", INFO.name(), BLOCKER.name())),
        new Change("10.8", "The field 'severity' and 'type' in the response  are not deprecated anymore."),
        new Change("2025.1", "The deprecated field 'htmlDesc' is not returned anymore, even if specified in the 'fields' parameter."));

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
    try (DbSession dbSession = dbClient.openSession(false)) {
      RuleDto rule = dbClient.ruleDao().selectByKey(dbSession, key)
        .orElseThrow(() -> new NotFoundException(format("Rule not found: %s", key)));

      List<RuleDto> templateRules = ofNullable(rule.getTemplateUuid())
        .flatMap(templateUuid -> dbClient.ruleDao().selectByUuid(rule.getTemplateUuid(), dbSession))
        .map(Collections::singletonList).orElseGet(Collections::emptyList);

      List<RuleParamDto> ruleParameters = dbClient.ruleDao().selectRuleParamsByRuleUuids(dbSession, singletonList(rule.getUuid()));
      ShowResponse showResponse = buildResponse(dbSession, request,
        new RulesResponseFormatter.SearchResult()
          .setRules(singletonList(rule))
          .setTemplateRules(templateRules)
          .setRuleParameters(ruleParameters)
          .setTotal(1L));
      writeProtobuf(showResponse, request, response);
    }
  }

  private ShowResponse buildResponse(DbSession dbSession, Request request, RulesResponseFormatter.SearchResult searchResult) {
    ShowResponse.Builder responseBuilder = ShowResponse.newBuilder();
    RuleDto rule = searchResult.getRules().get(0);
    responseBuilder.setRule(rulesResponseFormatter.formatRule(dbSession, searchResult));
    if (request.mandatoryParamAsBoolean(PARAM_ACTIVES)) {
      responseBuilder.addAllActives(rulesResponseFormatter.formatActiveRule(dbSession, rule));
    }
    return responseBuilder.build();
  }

}
