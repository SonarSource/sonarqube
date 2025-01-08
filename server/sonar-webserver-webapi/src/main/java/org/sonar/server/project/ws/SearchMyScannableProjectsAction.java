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
package org.sonar.server.project.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonarqube.ws.Projects.SearchMyScannableProjectsResponse.Project;

import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.server.project.ws.ProjectFinder.SearchResult;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Projects.SearchMyScannableProjectsResponse;

public class SearchMyScannableProjectsAction implements ProjectsWsAction {

  private final DbClient dbClient;
  private final ProjectFinder projectFinder;

  public SearchMyScannableProjectsAction(DbClient dbClient, ProjectFinder projectFinder) {
    this.dbClient = dbClient;
    this.projectFinder = projectFinder;
  }

  @Override
  public void define(NewController controller) {
    NewAction action = controller.createAction("search_my_scannable_projects")
      .setDescription("List projects that a user can scan.")
      .setSince("9.5")
      .setInternal(true)
      .setResponseExample(getClass().getResource("search-my-scannable-projects-example.json"))
      .setHandler(this);

    action.createSearchQuery("project", "project names");
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String query = request.param(TEXT_QUERY);

      SearchResult searchResult = projectFinder.search(dbSession, query);

      SearchMyScannableProjectsResponse.Builder searchProjects = SearchMyScannableProjectsResponse.newBuilder();
      searchResult.getProjects().stream()
        .map(p -> Project.newBuilder()
          .setKey(p.getKey())
          .setName(p.getName()))
        .forEach(searchProjects::addProjects);
      writeProtobuf(searchProjects.build(), request, response);
    }
  }

}
