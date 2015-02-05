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

import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.issue.IssueService;

import java.util.Map;

/**
 * List issue tags matching a given query.
 * @since 5.1
 */
public class ComponentTagsAction implements BaseIssuesWsAction {

  private final IssueService service;

  public ComponentTagsAction(IssueService service) {
    this.service = service;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("component_tags")
      .setHandler(this)
      .setSince("5.1")
      .setInternal(true)
      .setDescription("List tags for the issues under a given component (including issues on the descendants of the component)")
      .setResponseExample(Resources.getResource(getClass(), "example-component-tags.json"));
    action.createParam("componentUuid")
      .setDescription("A component UUID")
      .setRequired(true)
      .setExampleValue("7d8749e8-3070-4903-9188-bdd82933bb92");
    action.createParam("ps")
      .setDescription("The maximum size of the list to return")
      .setExampleValue("25")
      .setDefaultValue("10");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String componentUuid = request.mandatoryParam("componentUuid");
    int pageSize = request.mandatoryParamAsInt("ps");
    JsonWriter json = response.newJsonWriter().beginObject().name("tags").beginArray();
    for (Map.Entry<String, Long> tag : service.listTagsForComponent(componentUuid, pageSize).entrySet()) {
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
