/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsProjects.CreateWsResponse;
import org.sonarqube.ws.client.project.CreateRequest;

import static java.util.Optional.ofNullable;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.db.permission.OrganizationPermission.PROVISION_PROJECTS;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.project.ws.ProjectsWsSupport.PARAM_ORGANIZATION;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_BRANCH;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class CreateAction implements ProjectsWsAction {

  private static final String DEPRECATED_PARAM_KEY = "key";

  private final ProjectsWsSupport support;
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentUpdater componentUpdater;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public CreateAction(ProjectsWsSupport support, DbClient dbClient, UserSession userSession, ComponentUpdater componentUpdater,
    DefaultOrganizationProvider defaultOrganizationProvider) {
    this.support = support;
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentUpdater = componentUpdater;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_CREATE)
      .setDescription("Create a project.<br/>" +
        "Requires 'Create Projects' permission")
      .setSince("4.0")
      .setPost(true)
      .setResponseExample(getClass().getResource("create-example.json"))
      .setHandler(this);

    action.setChangelog(
      new Change("6.3", "The response format has been updated and does not contain the database ID anymore"),
      new Change("6.3", "The 'key' parameter has been renamed 'project'"));

    action.createParam(PARAM_PROJECT)
      .setDescription("Key of the project")
      .setDeprecatedKey(DEPRECATED_PARAM_KEY, "6.3")
      .setRequired(true)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_NAME)
      .setDescription("Name of the project")
      .setRequired(true)
      .setExampleValue("SonarQube");

    action.createParam(PARAM_BRANCH)
      .setDescription("SCM Branch of the project. The key of the project will become key:branch, for instance 'SonarQube:branch-5.0'")
      .setExampleValue("branch-5.0");

    action.createParam(PARAM_VISIBILITY)
      .setDescription("Whether the created project should be visible to everyone, or only specific user/groups.<br/>" +
        "If no visibility is specified, the default project visibility of the organization will be used.")
      .setRequired(false)
      .setInternal(true)
      .setSince("6.4")
      .setPossibleValues(Visibility.getLabels());

    support.addOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    CreateRequest createRequest = toCreateRequest(request);
    writeProtobuf(doHandle(createRequest), request, response);
  }

  private CreateWsResponse doHandle(CreateRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = support.getOrganization(dbSession, ofNullable(request.getOrganization())
        .orElseGet(defaultOrganizationProvider.get()::getKey));
      userSession.checkPermission(PROVISION_PROJECTS, organization);
      String visibility = request.getVisibility();
      Boolean changeToPrivate = visibility == null ? dbClient.organizationDao().getNewProjectPrivate(dbSession, organization) : "private".equals(visibility);
      support.checkCanUpdateProjectsVisibility(organization, changeToPrivate);

      ComponentDto componentDto = componentUpdater.create(dbSession, newComponentBuilder()
        .setOrganizationUuid(organization.getUuid())
        .setKey(request.getKey())
        .setName(request.getName())
        .setBranch(request.getBranch())
        .setPrivate(changeToPrivate)
        .setQualifier(PROJECT)
        .build(),
        userSession.isLoggedIn() ? userSession.getUserId() : null);
      return toCreateResponse(componentDto);
    }
  }

  private static CreateRequest toCreateRequest(Request request) {
    return CreateRequest.builder()
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setKey(request.mandatoryParam(PARAM_PROJECT))
      .setName(request.mandatoryParam(PARAM_NAME))
      .setBranch(request.param(PARAM_BRANCH))
      .setVisibility(request.param(PARAM_VISIBILITY))
      .build();
  }

  private static CreateWsResponse toCreateResponse(ComponentDto componentDto) {
    return CreateWsResponse.newBuilder()
      .setProject(CreateWsResponse.Project.newBuilder()
        .setKey(componentDto.key())
        .setName(componentDto.name())
        .setQualifier(componentDto.qualifier())
        .setVisibility(Visibility.getLabel(componentDto.isPrivate())))
      .build();
  }

}
