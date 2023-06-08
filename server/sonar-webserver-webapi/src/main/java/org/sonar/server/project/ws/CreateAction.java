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

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.newcodeperiod.CaycUtils;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects.CreateWsResponse;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.core.component.ComponentKeys.MAX_COMPONENT_KEY_LENGTH;
import static org.sonar.db.component.ComponentValidator.MAX_COMPONENT_NAME_LENGTH;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.newcodeperiod.NewCodePeriodUtils.NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.newcodeperiod.NewCodePeriodUtils.NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.newcodeperiod.NewCodePeriodUtils.getNewCodeDefinitionValueProjectCreation;
import static org.sonar.server.newcodeperiod.NewCodePeriodUtils.validateType;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.ACTION_CREATE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_MAIN_BRANCH;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_TYPE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_VALUE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class CreateAction implements ProjectsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentUpdater componentUpdater;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final PlatformEditionProvider editionProvider;
  private final DefaultBranchNameResolver defaultBranchNameResolver;

  public CreateAction(DbClient dbClient, UserSession userSession, ComponentUpdater componentUpdater,
    ProjectDefaultVisibility projectDefaultVisibility, PlatformEditionProvider editionProvider,
    DefaultBranchNameResolver defaultBranchNameResolver) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentUpdater = componentUpdater;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.editionProvider = editionProvider;
    this.defaultBranchNameResolver = defaultBranchNameResolver;
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
      .setSince("9.8")
      .setExampleValue("develop");

    action.createParam(PARAM_VISIBILITY)
      .setDescription("Whether the created project should be visible to everyone, or only specific user/groups.<br/>" +
        "If no visibility is specified, the default project visibility will be used.")
      .setSince("6.4")
      .setPossibleValues(Visibility.getLabels());

    action.createParam(PARAM_NEW_CODE_DEFINITION_TYPE)
      .setDescription(NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION)
      .setSince("10.1");

    action.createParam(PARAM_NEW_CODE_DEFINITION_VALUE)
      .setDescription(NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION)
      .setSince("10.1");

  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    CreateRequest createRequest = toCreateRequest(request);
    writeProtobuf(doHandle(createRequest), request, response);
  }

  private CreateWsResponse doHandle(CreateRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkPermission(PROVISION_PROJECTS);
      checkNewCodeDefinitionParam(request);
      ComponentDto componentDto = createProject(request, dbSession);
      if(request.getNewCodeDefinitionType() != null) {
        createNewCodeDefinition(dbSession, request, componentDto.uuid());
      }
      componentUpdater.commitAndIndex(dbSession, componentDto);
      return toCreateResponse(componentDto);
    }
  }

  private static void checkNewCodeDefinitionParam(CreateRequest request) {
    if (request.getNewCodeDefinitionType() == null && request.getNewCodeDefinitionValue() != null) {
      throw new IllegalArgumentException("New code definition type is required when new code definition value is provided");
    }
  }
  private ComponentDto createProject(CreateRequest request, DbSession dbSession) {
    String visibility = request.getVisibility();
    boolean changeToPrivate = visibility == null ? projectDefaultVisibility.get(dbSession).isPrivate() : "private".equals(visibility);

    return componentUpdater.createWithoutCommit(dbSession, newComponentBuilder()
        .setKey(request.getProjectKey())
        .setName(request.getName())
        .setPrivate(changeToPrivate)
        .setQualifier(PROJECT)
        .build(),
      userSession.isLoggedIn() ? userSession.getUuid() : null,
      userSession.isLoggedIn() ? userSession.getLogin() : null,
      request.getMainBranchKey(), s -> {}).mainBranchComponent();

  }
  private void createNewCodeDefinition(DbSession dbSession, CreateRequest request, String projectUuid) {

    boolean isCommunityEdition = editionProvider.get().filter(EditionProvider.Edition.COMMUNITY::equals).isPresent();
    NewCodePeriodType newCodePeriodType = validateType(request.getNewCodeDefinitionType(), false, isCommunityEdition);
    String newCodePeriodValue = request.getNewCodeDefinitionValue();
    String defaultBranchName = Optional.ofNullable(request.getMainBranchKey()).orElse(defaultBranchNameResolver.getEffectiveMainBranchName());

    NewCodePeriodDto dto = new NewCodePeriodDto();
    dto.setType(newCodePeriodType);
    dto.setProjectUuid(projectUuid);

    if (isCommunityEdition) {
      dto.setBranchUuid(projectUuid);
    }

    getNewCodeDefinitionValueProjectCreation(newCodePeriodType, newCodePeriodValue, defaultBranchName).ifPresent(dto::setValue);

    if (!CaycUtils.isNewCodePeriodCompliant(dto.getType(), dto.getValue())) {
      throw new IllegalArgumentException("Failed to set the New Code Definition. The given value is not compatible with the Clean as You Code methodology. "
        + "Please refer to the documentation for compliant options.");
    }

    dbClient.newCodePeriodDao().insert(dbSession, dto);
  }

  private static CreateRequest toCreateRequest(Request request) {
    return CreateRequest.builder()
      .setProjectKey(request.mandatoryParam(PARAM_PROJECT))
      .setName(abbreviate(request.mandatoryParam(PARAM_NAME), MAX_COMPONENT_NAME_LENGTH))
      .setVisibility(request.param(PARAM_VISIBILITY))
      .setMainBranchKey(request.param(PARAM_MAIN_BRANCH))
      .setNewCodeDefinitionType(request.param(PARAM_NEW_CODE_DEFINITION_TYPE))
      .setNewCodeDefinitionValue(request.param(PARAM_NEW_CODE_DEFINITION_VALUE))
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
    private final String projectKey;
    private final String name;
    private final String mainBranchKey;
    @CheckForNull
    private final String visibility;

    @CheckForNull
    private final String newCodeDefinitionType;

    @CheckForNull
    private final String newCodeDefinitionValue;

    private CreateRequest(Builder builder) {
      this.projectKey = builder.projectKey;
      this.name = builder.name;
      this.visibility = builder.visibility;
      this.mainBranchKey = builder.mainBranchKey;
      this.newCodeDefinitionType = builder.newCodeDefinitionType;
      this.newCodeDefinitionValue = builder.newCodeDefinitionValue;
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

    @CheckForNull
    public String getNewCodeDefinitionType() {
      return newCodeDefinitionType;
    }

    @CheckForNull
    public String getNewCodeDefinitionValue() {
      return newCodeDefinitionValue;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  static class Builder {
    private String projectKey;
    private String name;
    private String mainBranchKey;

    @CheckForNull
    private String visibility;
    @CheckForNull
    private String newCodeDefinitionType;

    @CheckForNull
    private String newCodeDefinitionValue;

    private Builder() {
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

    public Builder setNewCodeDefinitionType(@Nullable String newCodeDefinitionType) {
      this.newCodeDefinitionType = newCodeDefinitionType;
      return this;
    }

    public Builder setNewCodeDefinitionValue(@Nullable String newCodeDefinitionValue) {
      this.newCodeDefinitionValue = newCodeDefinitionValue;
      return this;
    }

    public CreateRequest build() {
      requireNonNull(projectKey);
      requireNonNull(name);
      return new CreateRequest(this);
    }
  }
}
