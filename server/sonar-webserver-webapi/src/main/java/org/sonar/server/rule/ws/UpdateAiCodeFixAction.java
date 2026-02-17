/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.rule.RuleUpdate;
import org.sonar.server.rule.RuleUpdater;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Rules.UpdateResponse;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.sonar.server.rule.ws.CreateAction.KEY_MAXIMUM_LENGTH;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class UpdateAiCodeFixAction implements RulesWsAction {

    public static final String PARAM_KEY = "key";
    public static final String PARAM_AI_CODE_FIX_ENABLED = "aiCodeFixEnabled";

    private final DbClient dbClient;
    private final RuleUpdater ruleUpdater;
    private final RuleMapper mapper;
    private final UserSession userSession;
    private final RuleWsSupport ruleWsSupport;

    public UpdateAiCodeFixAction(DbClient dbClient, RuleUpdater ruleUpdater, RuleMapper mapper, UserSession userSession,
            RuleWsSupport ruleWsSupport) {
        this.dbClient = dbClient;
        this.ruleUpdater = ruleUpdater;
        this.mapper = mapper;
        this.userSession = userSession;
        this.ruleWsSupport = ruleWsSupport;
    }

    @Override
    public void define(WebService.NewController controller) {
        WebService.NewAction action = controller.createAction("update_ai_code_fix").setPost(true)
                .setResponseExample(Resources.getResource(getClass(), "update-aicodefix-example.json")).setDescription(
                        "Enable or disable the aiCodeFixEnabled flag for an existing default rule.<br>"
                                + "Requires the following permission: 'Administer System'.").setChangelog()
                .setSince("24.12").setInternal(true).setHandler(this);

        action.createParam(PARAM_KEY).setRequired(true).setMaximumLength(KEY_MAXIMUM_LENGTH)
                .setDescription("Key of the rule to update").setExampleValue("javascript:NullCheck");

        action.createParam(PARAM_AI_CODE_FIX_ENABLED).setRequired(true)
                .setDescription("Enable or disable the aiCodeFixEnabled flag").setExampleValue("true");
    }

    @Override
    public void handle(Request request, Response response) throws Exception {
        userSession.checkIsSystemAdministrator();
        try (DbSession dbSession = dbClient.openSession(false)) {
            RuleUpdate update = readRequest(dbSession, request);
            ruleUpdater.updateAiCodeFixEnabled(dbSession, update);
            UpdateResponse updateResponse = buildResponse(dbSession, update.getRuleKey());
            writeProtobuf(updateResponse, request, response);
        }
    }

    private RuleUpdate readRequest(DbSession dbSession, Request request) {
        RuleKey key = RuleKey.parse(request.mandatoryParam(PARAM_KEY));
        boolean aiCodeFixEnabled = request.mandatoryParamAsBoolean(PARAM_AI_CODE_FIX_ENABLED);
        RuleUpdate update = createRuleUpdate(dbSession, key);
        update.setAiCodeFixEnabled(aiCodeFixEnabled);
        return update;
    }

    private RuleUpdate createRuleUpdate(DbSession dbSession, RuleKey key) {
        RuleDto rule = dbClient.ruleDao().selectByKey(dbSession, key)
                .orElseThrow(() -> new NotFoundException(format("This rule does not exist: %s", key)));
        if (rule.isCustomRule()) {
            throw new IllegalArgumentException(format("Not applicable to custom rule : %s", key));
        }
        return RuleUpdate.createForPluginRule(key);
    }

    private UpdateResponse buildResponse(DbSession dbSession, RuleKey key) {
        RuleDto rule = dbClient.ruleDao().selectByKey(dbSession, key)
                .orElseThrow(() -> new NotFoundException(format("Rule not found: %s", key)));

        List<RuleParamDto> ruleParameters = dbClient.ruleDao()
                .selectRuleParamsByRuleUuids(dbSession, singletonList(rule.getUuid()));
        UpdateResponse.Builder responseBuilder = UpdateResponse.newBuilder();
        RulesResponseFormatter.SearchResult searchResult = new RulesResponseFormatter.SearchResult().setRules(
                singletonList(rule)).setRuleParameters(ruleParameters).setTotal(1L);
        responseBuilder.setRule(mapper.toWsRule(rule, searchResult, Collections.emptySet(),
                ruleWsSupport.getUsersByUuid(dbSession, singletonList(rule)), emptyMap()));

        return responseBuilder.build();
    }

}
