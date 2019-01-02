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
package org.sonar.server.issue.ws;

import com.google.common.io.Resources;
import java.util.Map;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.issue.index.IssueQuery;
import org.sonar.server.issue.index.IssueQueryFactory;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.SearchRequest;

import static java.util.Collections.singletonList;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_COMPONENT_TAGS;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_COMPONENT_UUID;
import static org.sonarqube.ws.client.issue.IssuesWsParameters.PARAM_CREATED_AFTER;

/**
 * List issue tags matching a given query.
 */
public class ComponentTagsAction implements IssuesWsAction {

  private final IssueIndex issueIndex;
  private final IssueQueryFactory queryService;

  public ComponentTagsAction(IssueIndex issueIndex, IssueQueryFactory queryService) {
    this.issueIndex = issueIndex;
    this.queryService = queryService;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction(ACTION_COMPONENT_TAGS)
      .setHandler(this)
      .setSince("5.1")
      .setInternal(true)
      .setDescription("List tags for the issues under a given component (including issues on the descendants of the component)")
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
    SearchRequest searchRequest = new SearchRequest()
      .setComponentUuids(singletonList(request.mandatoryParam(PARAM_COMPONENT_UUID)))
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

}
