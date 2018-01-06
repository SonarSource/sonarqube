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
package org.sonar.server.issue.ws;

import com.google.common.io.Resources;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.index.IssueIndex;

import static org.sonarqube.ws.client.issue.IssuesWsParameters.ACTION_AUTHORS;

public class AuthorsAction implements IssuesWsAction {

  private final IssueIndex issueIndex;

  public AuthorsAction(IssueIndex issueIndex) {
    this.issueIndex = issueIndex;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction(ACTION_AUTHORS)
      .setSince("5.1")
      .setDescription("Search SCM accounts which match a given query")
      .setResponseExample(Resources.getResource(this.getClass(), "authors-example.json"))
      .setHandler(this);

    action.createParam(Param.TEXT_QUERY)
      .setDescription("A pattern to match SCM accounts against")
      .setExampleValue("luke");
    action.createParam(Param.PAGE_SIZE)
      .setDescription("The size of the list to return")
      .setExampleValue("25")
      .setDefaultValue("10");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String query = request.param(Param.TEXT_QUERY);
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);

    try (JsonWriter json = response.newJsonWriter()) {
      json.beginObject()
        .name("authors")
        .beginArray();

      for (String login : listAuthors(query, pageSize)) {
        json.value(login);
      }

      json.endArray().endObject();
    }
  }

  public List<String> listAuthors(@Nullable String textQuery, int pageSize) {
    return issueIndex.listAuthors(IssueQuery.builder()
      .checkAuthorization(false)
      .build(), textQuery, pageSize);
  }
}
