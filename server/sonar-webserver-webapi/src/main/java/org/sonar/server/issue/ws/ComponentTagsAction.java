/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.issue.ws;

import com.google.common.io.Resources;
import java.util.Map;
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.issue.SearchRequest;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueQuery;
import org.sonar.server.issue.index.IssueQueryFactory;

import static java.util.Collections.singletonList;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.server.issue.index.IssueQueryFactory.ISSUE_TYPE_NAMES;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_COMPONENT_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_UUID;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AFTER;

/**
 * List issue tags matching a given query.
 */
public class ComponentTagsAction implements IssuesWsAction {

  private final IssueIndex issueIndex;
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker;
  private final IssueQueryFactory queryService;
  private final DbClient dbClient;

  public ComponentTagsAction(IssueIndex issueIndex,
    IssueIndexSyncProgressChecker issueIndexSyncProgressChecker,
    IssueQueryFactory queryService, DbClient dbClient) {
    this.issueIndex = issueIndex;
    this.issueIndexSyncProgressChecker = issueIndexSyncProgressChecker;
    this.queryService = queryService;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction(ACTION_COMPONENT_TAGS)
      .setHandler(this)
      .setSince("5.1")
      .setInternal(true)
      .setDescription("List tags for the issues under a given component (including issues on the descendants of the component)"
          + "<br/>When issue indexing is in progress returns 503 service unavailable HTTP code.")
      .setResponseExample(Resources.getResource(getClass(), "component-tags-example.json"));

    action.createParam(PARAM_COMPONENT_UUID)
      .setDescription("A component UUID")
      .setRequired(true)
      .setExampleValue("7d8749e8-3070-4903-9188-bdd82933bb92");

    action.createParam(PARAM_CREATED_AFTER)
      .setDescription("To retrieve tags on issues created after the given date (inclusive). <br>" +
        "Either a date (server timezone) or datetime can be provided.")
      .setExampleValue("2017-10-19 or 2017-10-19T13:00:00+0200");

    action.createParam(PAGE_SIZE)
      .setDescription("The maximum size of the list to return")
      .setExampleValue("25")
      .setDefaultValue("10");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String componentUuid = request.mandatoryParam(PARAM_COMPONENT_UUID);
    checkIfAnyComponentsNeedIssueSync(componentUuid);

    SearchRequest searchRequest = new SearchRequest()
      .setComponentUuids(singletonList(componentUuid))
      .setTypes(ISSUE_TYPE_NAMES)
      .setResolved(false)
      .setCreatedAfter(request.param(PARAM_CREATED_AFTER));

    IssueQuery query = queryService.create(searchRequest);
    int pageSize = request.mandatoryParamAsInt(PAGE_SIZE);
    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject().name("tags").beginArray();
      for (Map.Entry<String, Long> tag : issueIndex.countTags(query, pageSize).entrySet()) {
        json.beginObject()
          .prop("key", tag.getKey())
          .prop("value", tag.getValue())
          .endObject();
      }
      json.endArray().endObject();
    }
  }

  private void checkIfAnyComponentsNeedIssueSync(String componentUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ComponentDto> componentDto = dbClient.componentDao().selectByUuid(dbSession, componentUuid);
      if (componentDto.isPresent()) {
        issueIndexSyncProgressChecker.checkIfComponentNeedIssueSync(dbSession, componentDto.get().getKey());
      } else {
        issueIndexSyncProgressChecker.checkIfIssueSyncInProgress(dbSession);
      }
    }
  }

}
