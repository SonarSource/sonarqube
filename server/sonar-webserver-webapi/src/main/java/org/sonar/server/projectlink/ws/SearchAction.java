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
package org.sonar.server.projectlink.ws;

import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.ProjectLinks.Link;
import org.sonarqube.ws.ProjectLinks.SearchWsResponse;

import static java.util.Optional.ofNullable;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.projectlink.ws.ProjectLinksWs.checkProject;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.ACTION_SEARCH;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_PROJECT_ID;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAction implements ProjectLinksWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public SearchAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SEARCH)
      .setDescription("List links of a project.<br>" +
        "The '%s' or '%s' must be provided.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "<li>'Browse' on the specified project</li>" +
        "</ul>",
        PARAM_PROJECT_ID, PARAM_PROJECT_KEY)
      .setHandler(this)
      .setResponseExample(getClass().getResource("search-example.json"))
      .setSince("6.1");

    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project Id")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project Key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchRequest searchWsRequest = toSearchWsRequest(request);
    SearchWsResponse searchWsResponse = doHandle(searchWsRequest);

    writeProtobuf(searchWsResponse, request, response);
  }

  private SearchWsResponse doHandle(SearchRequest searchWsRequest) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = getComponentByUuidOrKey(dbSession, searchWsRequest);
      List<ProjectLinkDto> links = dbClient.projectLinkDao()
        .selectByProjectUuid(dbSession, component.uuid());
      return buildResponse(links);
    }
  }

  private static SearchWsResponse buildResponse(List<ProjectLinkDto> links) {
    return SearchWsResponse.newBuilder()
      .addAllLinks(links.stream()
        .map(SearchAction::buildLink)
        .collect(Collectors.toList()))
      .build();
  }

  private static Link buildLink(ProjectLinkDto link) {
    Link.Builder builder = Link.newBuilder()
      .setId(String.valueOf(link.getUuid()))
      .setType(link.getType())
      .setUrl(link.getHref());
    ofNullable(link.getName()).ifPresent(builder::setName);
    return builder.build();
  }

  private ComponentDto getComponentByUuidOrKey(DbSession dbSession, SearchRequest request) {
    ComponentDto component = componentFinder.getByUuidOrKey(
      dbSession,
      request.getProjectId(),
      request.getProjectKey(),
      ComponentFinder.ParamNames.PROJECT_ID_AND_KEY);
    if (!userSession.hasComponentPermission(UserRole.ADMIN, component) &&
      !userSession.hasComponentPermission(UserRole.USER, component)) {
      throw insufficientPrivilegesException();
    }
    return checkProject(component);
  }

  private static SearchRequest toSearchWsRequest(Request request) {
    return new SearchRequest()
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY));
  }

  private static class SearchRequest {

    private String projectId;
    private String projectKey;

    public SearchRequest setProjectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public String getProjectId() {
      return projectId;
    }

    public SearchRequest setProjectKey(String projectKey) {
      this.projectKey = projectKey;
      return this;
    }

    public String getProjectKey() {
      return projectKey;
    }
  }
}
