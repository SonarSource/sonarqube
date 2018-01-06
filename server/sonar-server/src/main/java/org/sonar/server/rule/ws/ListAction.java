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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.Rules.ListResponse;

import static com.google.common.base.Strings.nullToEmpty;

public class ListAction implements RulesWsAction {

  private final DbClient dbClient;

  public ListAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller
      .createAction("list")
      .setDescription("List rules, excluding the manual rules and the rules with status REMOVED. JSON format is not supported for response.")
      .setSince("5.2")
      .setInternal(true)
      .setResponseExample(getClass().getResource("list-example.txt"))
      .setHandler(this);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    final ListResponse.Builder listResponseBuilder = ListResponse.newBuilder();
    final ListResponse.Rule.Builder ruleBuilder = ListResponse.Rule.newBuilder();
    try (DbSession dbSession = dbClient.openSession(false)) {
      dbClient.ruleDao().selectEnabled(dbSession, resultContext -> {
        RuleDefinitionDto dto = resultContext.getResultObject();
        ruleBuilder
          .clear()
          .setRepository(dto.getRepositoryKey())
          .setKey(dto.getRuleKey())
          .setName(nullToEmpty(dto.getName()))
          .setInternalKey(nullToEmpty(dto.getConfigKey()));
        listResponseBuilder.addRules(ruleBuilder.build());
      });
    }

    // JSON response is voluntarily not supported. This WS is for internal use.
    wsResponse.stream().setMediaType(MediaTypes.PROTOBUF);
    listResponseBuilder.build().writeTo(wsResponse.stream().output());
  }

}
