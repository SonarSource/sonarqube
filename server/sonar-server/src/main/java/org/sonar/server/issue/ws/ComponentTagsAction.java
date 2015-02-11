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
package org.sonar.server.issue.ws;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueQueryService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.filter.IssueFilterParameters;

import java.util.Map;

/**
 * List issue tags matching a given query.
 * @since 5.1
 */
public class ComponentTagsAction implements BaseIssuesWsAction {

  private static final String PARAM_COMPONENT_UUID = "componentUuid";
  private static final String PARAM_CREATED_AT = "createdAfter";
  private static final String PARAM_PAGE_SIZE = "ps";
  private final IssueService service;
  private final IssueQueryService queryService;

  public ComponentTagsAction(IssueService service, IssueQueryService queryService) {
    this.service = service;
    this.queryService = queryService;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("component_tags")
      .setHandler(this)
      .setSince("5.1")
      .setInternal(true)
      .setDescription("List tags for the issues under a given component (including issues on the descendants of the component)")
      .setResponseExample(Resources.getResource(getClass(), "example-component-tags.json"));
    action.createParam(PARAM_COMPONENT_UUID)
      .setDescription("A component UUID")
      .setRequired(true)
      .setExampleValue("7d8749e8-3070-4903-9188-bdd82933bb92");
    action.createParam(PARAM_CREATED_AT)
      .setDescription("To retrieve tags on issues created after the given date (inclusive). Format: date or datetime ISO formats")
      .setExampleValue("2013-05-01 (or 2013-05-01T13:00:00+0100)");
    action.createParam(PARAM_PAGE_SIZE)
      .setDescription("The maximum size of the list to return")
      .setExampleValue("25")
      .setDefaultValue("10");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Builder<String, Object> paramBuilder = ImmutableMap.<String, Object>builder()
      .put(IssueFilterParameters.COMPONENT_UUIDS, request.mandatoryParam(PARAM_COMPONENT_UUID))
      .put(IssueFilterParameters.RESOLVED, false);
    if (request.hasParam(PARAM_CREATED_AT)) {
      paramBuilder.put(IssueFilterParameters.CREATED_AFTER, request.param(PARAM_CREATED_AT));
    }
    IssueQuery query = queryService.createFromMap(paramBuilder.build());
    int pageSize = request.mandatoryParamAsInt(PARAM_PAGE_SIZE);
    JsonWriter json = response.newJsonWriter().beginObject().name("tags").beginArray();
    for (Map.Entry<String, Long> tag : service.listTagsForComponent(query, pageSize).entrySet()) {
      json.beginObject()
        .prop("key", tag.getKey())
        .prop("value", tag.getValue())
        .endObject();
    }
    json.endArray()
      .endObject()
      .close();
  }

}
