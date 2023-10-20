/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects.CreateWsResponse;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.core.component.ComponentKeys.MAX_COMPONENT_KEY_LENGTH;
import static org.sonar.db.component.ComponentValidator.MAX_COMPONENT_NAME_LENGTH;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.exceptions.BadRequestException.throwBadRequestException;
import static org.sonar.server.project.ws.ProjectsWsSupport.PARAM_ORGANIZATION;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_MAIN_BRANCH;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class CreateAction implements ProjectsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentUpdater componentUpdater;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final ProjectsWsSupport support;
  private static final Logger logger = Loggers.get(CreateAction.class);

  public CreateAction(DbClient dbClient, UserSession userSession, ComponentUpdater componentUpdater,
    ProjectDefaultVisibility projectDefaultVisibility, ProjectsWsSupport support) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentUpdater = componentUpdater;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.support = support;
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
      new Change("9.8", "Field 'mainBranch' added to the request"),
      new Change("7.1", "The 'visibility' parameter is public"));

    action.createParam(PARAM_PROJECT)
      .setDescription("Key of the project")
      .setRequired(true)
      .setMaximumLength(MAX_COMPONENT_KEY_LENGTH)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);

    action.createParam(PARAM_NAME)
      .setDescription("Name of the project. If name is longer than %d, it is abbreviated.", MAX_COMPONENT_NAME_LENGTH)
      .setRequired(true)
      .setExampleValue("SonarQube");

    action.createParam(PARAM_MAIN_BRANCH)
      .setDescription("Key of the main branch of the project. If not provided, the default main branch key will be used.")
      .setRequired(false)
      .setSince("9.8")
      .setExampleValue("develop");

    action.createParam(PARAM_VISIBILITY)
      .setDescription("Whether the created project should be visible to everyone, or only specific user/groups.<br/>" +
        "If no visibility is specified, the default project visibility will be used.")
      .setRequired(false)
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
      OrganizationDto organization = support.getOrganization(dbSession, request.getOrganization());
      logger.info("Create Project Action request:: organization :{}, orgId: {}, projectName: {}, projectKey: {}, user: {}", organization.getKey(),
              organization.getUuid(), request.getName(), request.getProjectKey(), userSession.getLogin());
      userSession.checkPermission(OrganizationPermission.PROVISION_PROJECTS, organization);
      String visibility = request.getVisibility();
      if (visibility != null && "public".equals(visibility)) {
        throwBadRequestException("Users are not allowed to create project with public visibility");
      }

      ComponentDto componentDto = componentUpdater.create(dbSession, newComponentBuilder()
        .setOrganizationUuid(organization.getUuid())
        .setKey(request.getProjectKey())
        .setName(request.getName())
        .setPrivate(true)
        .setQualifier(PROJECT)
        .build(),
        userSession.isLoggedIn() ? userSession.getUuid() : null,
        userSession.isLoggedIn() ? userSession.getLogin() : null,
        request.getMainBranchKey());
      return toCreateResponse(componentDto);
    }
  }

  private static CreateRequest toCreateRequest(Request request) {
    return CreateRequest.builder()
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setProjectKey(request.mandatoryParam(PARAM_PROJECT))
      .setName(abbreviate(request.mandatoryParam(PARAM_NAME), MAX_COMPONENT_NAME_LENGTH))
      .setVisibility(request.param(PARAM_VISIBILITY))
      .setMainBranchKey(request.param(PARAM_MAIN_BRANCH))
      .build();
  }

  private static CreateWsResponse toCreateResponse(ComponentDto componentDto) {
    return CreateWsResponse.newBuilder()
      .setProject(CreateWsResponse.Project.newBuilder()
        .setKey(componentDto.getKey())
        .setName(componentDto.name())
        .setQualifier(componentDto.qualifier())
        .setVisibility(Visibility.getLabel(componentDto.isPrivate())))
      .build();
  }

  static class CreateRequest {
    private final String organization;
    private final String projectKey;
    private final String name;
    private final String mainBranchKey;
    @CheckForNull
    private final String visibility;

    private CreateRequest(Builder builder) {
      this.organization = builder.organization;
      this.projectKey = builder.projectKey;
      this.name = builder.name;
      this.visibility = builder.visibility;
      this.mainBranchKey = builder.mainBranchKey;
    }

    @CheckForNull
    public String getOrganization() {
      return organization;
    }

    public String getProjectKey() {
      return projectKey;
    }

    public String getName() {
      return name;
    }

    @CheckForNull
    public String getVisibility() {
      return visibility;
    }

    public String getMainBranchKey() {
      return mainBranchKey;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  static class Builder {
    private String organization;
    private String projectKey;
    private String name;
    private String mainBranchKey;

    @CheckForNull
    private String visibility;

    private Builder() {
    }

    public Builder setOrganization(@Nullable String organization) {
      this.organization = organization;
      return this;
    }

    public Builder setProjectKey(String projectKey) {
      requireNonNull(projectKey);
      this.projectKey = projectKey;
      return this;
    }

    public Builder setName(String name) {
      requireNonNull(name);
      this.name = name;
      return this;
    }

    public Builder setVisibility(@Nullable String visibility) {
      this.visibility = visibility;
      return this;
    }

    public Builder setMainBranchKey(@Nullable String mainBranchKey) {
      this.mainBranchKey = mainBranchKey;
      return this;
    }

    public CreateRequest build() {
      requireNonNull(projectKey);
      requireNonNull(name);
      return new CreateRequest(this);
    }
  }
}
