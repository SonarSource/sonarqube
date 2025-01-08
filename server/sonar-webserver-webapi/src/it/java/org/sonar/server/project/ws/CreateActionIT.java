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

import com.google.common.base.Strings;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.es.Indexers;
import org.sonar.server.es.TestIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.common.permission.PermissionTemplateService;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.project.ws.CreateAction.Builder;
import org.sonar.server.project.ws.CreateAction.CreateRequest;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Projects.CreateWsResponse;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;

import static java.util.Optional.ofNullable;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.NUMBER_OF_DAYS;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.project.Visibility.PRIVATE;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.WsRequest.Method.POST;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_MAIN_BRANCH;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_TYPE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_VALUE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_VISIBILITY;

public class CreateActionIT {

  private static final String DEFAULT_PROJECT_KEY = "project-key";
  private static final String DEFAULT_PROJECT_NAME = "project-name";
  private static final String MAIN_BRANCH = "main-branch";

  private final System2 system2 = System2.INSTANCE;

  @Rule
  public final DbTester db = DbTester.create(system2);
  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public final I18nRule i18n = new I18nRule().put("qualifier.TRK", "Project");

  private final DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);
  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);
  private final TestIndexers projectIndexers = new TestIndexers();
  private final PermissionTemplateService permissionTemplateService = mock(PermissionTemplateService.class);

  private final PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);

  private final NewCodeDefinitionResolver newCodeDefinitionResolver = new NewCodeDefinitionResolver(db.getDbClient(), editionProvider);
  private final WsActionTester ws = new WsActionTester(
    new CreateAction(
      db.getDbClient(), userSession,
      new ComponentUpdater(db.getDbClient(), i18n, system2, permissionTemplateService, new FavoriteUpdater(db.getDbClient()),
        projectIndexers, new SequenceUuidFactory(), defaultBranchNameResolver, mock(PermissionUpdater.class), mock(PermissionService.class)),
      projectDefaultVisibility, defaultBranchNameResolver, newCodeDefinitionResolver));

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PUBLIC);
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void create_project() {
    userSession.addPermission(PROVISION_PROJECTS);

    CreateWsResponse response = call(CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setMainBranchKey(MAIN_BRANCH)
      .build());

    assertThat(response.getProject())
      .extracting(Project::getKey, Project::getName, Project::getQualifier, Project::getVisibility)
      .containsOnly(DEFAULT_PROJECT_KEY, DEFAULT_PROJECT_NAME, "TRK", "public");
    ProjectDto projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), DEFAULT_PROJECT_KEY).orElseThrow();
    assertThat(projectDto)
      .extracting(ProjectDto::getKey, ProjectDto::getName, ProjectDto::getQualifier, ProjectDto::isPrivate, ProjectDto::getCreationMethod)
      .containsOnly(DEFAULT_PROJECT_KEY, DEFAULT_PROJECT_NAME, "TRK", false, CreationMethod.LOCAL_API);

    ComponentDto component = db.getDbClient().componentDao().selectByKey(db.getSession(), DEFAULT_PROJECT_KEY).orElseThrow();
    BranchDto branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), component.branchUuid()).orElseThrow();
    assertThat(branch)
      .extracting(BranchDto::getKey)
      .isEqualTo(MAIN_BRANCH);
    projectIndexers.hasBeenCalledForEntity(branch.getProjectUuid(), Indexers.EntityEvent.CREATION);
  }

  @Test
  public void apply_project_visibility_public() {
    userSession.addPermission(PROVISION_PROJECTS);

    CreateWsResponse result = ws.newRequest()
      .setParam("project", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .setParam("visibility", "public")
      .executeProtobuf(CreateWsResponse.class);

    assertThat(result.getProject().getVisibility()).isEqualTo("public");
  }

  @Test
  public void createProject_whenCalIsFromGui_setCreationMethodEqualsUi() {
    userSession.logIn().addPermission(PROVISION_PROJECTS).flagSessionAsGui();

    ws.newRequest()
      .setParam("project", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .setParam("visibility", "public")
      .executeProtobuf(CreateWsResponse.class);

    ProjectDto project = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), DEFAULT_PROJECT_KEY).get();
    assertThat(project.getCreationMethod()).isEqualTo(CreationMethod.LOCAL_BROWSER);
  }

  @Test
  public void apply_project_visibility_private() {
    userSession.addPermission(PROVISION_PROJECTS);

    CreateWsResponse result = ws.newRequest()
      .setParam("project", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .setParam("visibility", PRIVATE.getLabel())
      .executeProtobuf(CreateWsResponse.class);

    assertThat(result.getProject().getVisibility()).isEqualTo("private");
  }

  @Test
  public void apply_default_project_visibility_public() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PUBLIC);
    userSession.addPermission(PROVISION_PROJECTS);

    CreateWsResponse result = ws.newRequest()
      .setParam("project", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .executeProtobuf(CreateWsResponse.class);

    assertThat(result.getProject().getVisibility()).isEqualTo("public");
  }

  @Test
  public void apply_default_project_visibility_private() {
    when(projectDefaultVisibility.get(any())).thenReturn(PRIVATE);
    userSession.addPermission(PROVISION_PROJECTS);

    CreateWsResponse result = ws.newRequest()
      .setParam("project", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .executeProtobuf(CreateWsResponse.class);

    assertThat(result.getProject().getVisibility()).isEqualTo("private");
  }

  @Test
  public void abbreviate_project_name_if_very_long() {
    userSession.addPermission(PROVISION_PROJECTS);
    String longName = Strings.repeat("a", 1_000);

    ws.newRequest()
      .setParam("project", DEFAULT_PROJECT_KEY)
      .setParam("name", longName)
      .executeProtobuf(CreateWsResponse.class);

    assertThat(db.getDbClient().componentDao().selectByKey(db.getSession(), DEFAULT_PROJECT_KEY).get().name())
      .isEqualTo(Strings.repeat("a", 497) + "...");
  }

  @Test
  public void add_project_to_user_favorites_if_project_creator_is_defined_in_permission_template() {
    UserDto user = db.users().insertUser();
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ProjectDto.class))).thenReturn(true);
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    ws.newRequest()
      .setParam("project", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .executeProtobuf(CreateWsResponse.class);

    ProjectDto project = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), DEFAULT_PROJECT_KEY).get();
    assertThat(db.favorites().hasFavorite(project, user.getUuid())).isTrue();
  }

  @Test
  public void do_not_add_project_to_user_favorites_if_project_creator_is_defined_in_permission_template_and_already_100_favorites() {
    UserDto user = db.users().insertUser();
    when(permissionTemplateService.hasDefaultTemplateWithPermissionOnProjectCreator(any(DbSession.class), any(ProjectDto.class))).thenReturn(true);
    rangeClosed(1, 100).forEach(i -> db.favorites().add(db.components().insertPrivateProject().getProjectDto(), user.getUuid(), user.getLogin()));
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    ws.newRequest()
      .setParam("project", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .executeProtobuf(CreateWsResponse.class);

    ProjectDto project = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), DEFAULT_PROJECT_KEY).get();
    assertThat(db.favorites().hasNoFavorite(project)).isTrue();
  }

  @Test
  public void fail_when_project_already_exists() {
    db.components().insertPublicProject(project -> project.setKey(DEFAULT_PROJECT_KEY)).getMainBranchComponent();
    userSession.addPermission(PROVISION_PROJECTS);

    CreateRequest request = CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(BadRequestException.class);
  }

  @Test
  public void fail_when_invalid_project_key() {
    userSession.addPermission(PROVISION_PROJECTS);

    CreateRequest request = CreateRequest.builder()
      .setProjectKey("project%Key")
      .setName(DEFAULT_PROJECT_NAME)
      .build();
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Malformed key for Project: 'project%Key'. Allowed characters are alphanumeric, '-', '_', '.' and ':', with at least " +
        "one non-digit.");
  }

  @Test
  public void fail_when_missing_project_parameter() {
    userSession.addPermission(PROVISION_PROJECTS);

    assertThatThrownBy(() -> call(null, DEFAULT_PROJECT_NAME, null, null, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'project' parameter is missing");
  }

  @Test
  public void fail_when_missing_name_parameter() {
    userSession.addPermission(PROVISION_PROJECTS);

    assertThatThrownBy(() -> call(DEFAULT_PROJECT_KEY, null, null, null, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'name' parameter is missing");
  }

  @Test
  public void fail_when_missing_create_project_permission() {
    CreateRequest request = CreateRequest.builder().setProjectKey(DEFAULT_PROJECT_KEY).setName(DEFAULT_PROJECT_NAME)
      .build();
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void test_example() {
    userSession.addPermission(PROVISION_PROJECTS);

    String result = ws.newRequest()
      .setParam("project", DEFAULT_PROJECT_KEY)
      .setParam("name", DEFAULT_PROJECT_NAME)
      .execute().getInput();

    assertJson(result).isSimilarTo(getClass().getResource("create-example.json"));
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("create");
    assertThat(definition.since()).isEqualTo("4.0");
    assertThat(definition.isInternal()).isFalse();
    assertThat(definition.responseExampleAsString()).isNotEmpty();

    assertThat(definition.params()).extracting(WebService.Param::key).containsExactlyInAnyOrder(
      PARAM_VISIBILITY,
      PARAM_NAME,
      PARAM_PROJECT,
      PARAM_MAIN_BRANCH,
      PARAM_NEW_CODE_DEFINITION_TYPE,
      PARAM_NEW_CODE_DEFINITION_VALUE);

    WebService.Param visibilityParam = definition.param(PARAM_VISIBILITY);
    assertThat(visibilityParam.description()).isNotEmpty();
    assertThat(visibilityParam.isInternal()).isFalse();
    assertThat(visibilityParam.isRequired()).isFalse();
    assertThat(visibilityParam.since()).isEqualTo("6.4");
    assertThat(visibilityParam.possibleValues()).containsExactlyInAnyOrder("private", "public");

    WebService.Param project = definition.param(PARAM_PROJECT);
    assertThat(project.isRequired()).isTrue();
    assertThat(project.maximumLength()).isEqualTo(400);

    WebService.Param name = definition.param(PARAM_NAME);
    assertThat(name.isRequired()).isTrue();
    assertThat(name.description()).isEqualTo("Name of the project. If name is longer than 500, it is abbreviated.");

    WebService.Param ncdType = definition.param(PARAM_NEW_CODE_DEFINITION_TYPE);
    assertThat(ncdType.isRequired()).isFalse();
    assertThat(ncdType.description()).isEqualTo(NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION);

    WebService.Param ncdValue = definition.param(PARAM_NEW_CODE_DEFINITION_VALUE);
    assertThat(ncdValue.isRequired()).isFalse();
    assertThat(ncdValue.description()).isEqualTo(NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION);

  }

  @Test
  public void fail_when_set_null_project_name_on_create_request_builder() {
    Builder builder = CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY);
    assertThatThrownBy(() -> builder.setName(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void fail_when_set_null_project_key_on_create_request_builder() {
    Builder builder = CreateRequest.builder()
      .setName(DEFAULT_PROJECT_NAME);
    assertThatThrownBy(() -> builder.setProjectKey(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void fail_when_project_key_not_set_on_create_request_builder() {
    CreateRequest.builder()
      .setName(DEFAULT_PROJECT_NAME);

    Builder builder = CreateRequest.builder()
      .setName(DEFAULT_PROJECT_NAME);
    assertThatThrownBy(builder::build)
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void fail_when_project_name_not_set_on_create_request_builder() {
    Builder builder = CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY);
    assertThatThrownBy(builder::build)
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void set_default_branch_name_for_reference_branch_NCD_when_no_main_branch_provided() {
    userSession.addPermission(PROVISION_PROJECTS);

    String otherBranchName = "otherBranchName";

    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn(otherBranchName);

    call(CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setNewCodeDefinitionType(REFERENCE_BRANCH.name())
      .build());

    ProjectDto projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), DEFAULT_PROJECT_KEY).get();

    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue)
      .containsExactly(REFERENCE_BRANCH, otherBranchName);
  }

  @Test
  public void set_main_branch_name_for_reference_branch_NCD() {
    userSession.addPermission(PROVISION_PROJECTS);

    call(CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setMainBranchKey(MAIN_BRANCH)
      .setNewCodeDefinitionType(REFERENCE_BRANCH.name())
      .build());

    ProjectDto projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), DEFAULT_PROJECT_KEY).get();

    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue)
      .containsExactly(REFERENCE_BRANCH, MAIN_BRANCH);
  }
  @Test
  public void set_new_code_definition_on_project_creation() {
    userSession.addPermission(PROVISION_PROJECTS);

    call(CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setMainBranchKey(MAIN_BRANCH)
      .setNewCodeDefinitionType(NUMBER_OF_DAYS.name())
      .setNewCodeDefinitionValue("30")
      .build());

    ProjectDto projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), DEFAULT_PROJECT_KEY).get();

    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue)
      .containsExactly(NUMBER_OF_DAYS, "30");
  }

  @Test
  public void set_new_code_definition_branch_for_community_edition() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));

    userSession.addPermission(PROVISION_PROJECTS);

    call(CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setMainBranchKey(MAIN_BRANCH)
      .setNewCodeDefinitionType(NUMBER_OF_DAYS.name())
      .setNewCodeDefinitionValue("30")
      .build());

    ProjectDto projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), DEFAULT_PROJECT_KEY).get();
    ComponentDto component = db.getDbClient().componentDao().selectByKey(db.getSession(), DEFAULT_PROJECT_KEY).get();

    assertThat(db.getDbClient().newCodePeriodDao().selectByBranch(db.getSession(), projectDto.getUuid(), component.uuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue, NewCodePeriodDto::getBranchUuid)
      .containsExactly(NUMBER_OF_DAYS, "30", component.uuid());
  }

  @Test
  public void throw_IAE_if_setting_is_not_cayc_compliant() {
    userSession.addPermission(PROVISION_PROJECTS);

    CreateRequest request = CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setMainBranchKey(MAIN_BRANCH)
      .setNewCodeDefinitionType(NUMBER_OF_DAYS.name())
      .setNewCodeDefinitionValue("99")
      .build();

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Failed to set the New Code Definition. The given value is not compatible with the Clean as You Code methodology. "
        + "Please refer to the documentation for compliant options.");
    assertThat(db.getDbClient().componentDao().selectByKey(db.getSession(), DEFAULT_PROJECT_KEY))
      .isEmpty();
  }

  @Test
  public void throw_IAE_if_setting_is_new_code_definition_value_provided_without_type() {
    userSession.addPermission(PROVISION_PROJECTS);

    CreateRequest request = CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setMainBranchKey(MAIN_BRANCH)
      .setNewCodeDefinitionValue("99")
      .build();

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("New code definition type is required when new code definition value is provided");
    assertThat(db.getDbClient().componentDao().selectByKey(db.getSession(), DEFAULT_PROJECT_KEY))
      .isEmpty();
    assertThat(db.getDbClient().newCodePeriodDao().selectAll(db.getSession())).isEmpty();
  }

  @Test
  public void set_new_code_definition_for_project_for_developer_edition() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    userSession.addPermission(PROVISION_PROJECTS);

    call(CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setMainBranchKey(MAIN_BRANCH)
      .setNewCodeDefinitionType(NUMBER_OF_DAYS.name())
      .setNewCodeDefinitionValue("30")
      .build());

    ProjectDto projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), DEFAULT_PROJECT_KEY).get();

    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue, NewCodePeriodDto::getBranchUuid)
      .containsExactly(NUMBER_OF_DAYS, "30", null);
  }


  @Test
  public void do_not_create_project_when_ncdType_invalid() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    userSession.addPermission(PROVISION_PROJECTS);

    CreateRequest request = CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setMainBranchKey(MAIN_BRANCH)
      .setNewCodeDefinitionType("InvalidType")
      .build();

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class);

    assertThat(db.getDbClient().componentDao().selectByKey(db.getSession(), DEFAULT_PROJECT_KEY))
      .isEmpty();

  }

  @Test
  public void do_not_set_new_code_definition_when_ncdType_not_provided() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    userSession.addPermission(PROVISION_PROJECTS);

    call(CreateRequest.builder()
      .setProjectKey(DEFAULT_PROJECT_KEY)
      .setName(DEFAULT_PROJECT_NAME)
      .setMainBranchKey(MAIN_BRANCH)
      .build());

    ComponentDto component = db.getDbClient().componentDao().selectByKey(db.getSession(), DEFAULT_PROJECT_KEY).get();

    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), component.uuid()))
      .isEmpty();
  }

  private CreateWsResponse call(CreateRequest request) {
    return call(request.getProjectKey(), request.getName(), request.getMainBranchKey(), request.getNewCodeDefinitionType(), request.getNewCodeDefinitionValue());
  }

  private CreateWsResponse call(@Nullable String projectKey, @Nullable String projectName, @Nullable String mainBranch,
    @Nullable String newCodeDefinitionType, @Nullable String newCodeDefinitionValue) {
    TestRequest httpRequest = ws.newRequest()
      .setMethod(POST.name());
    ofNullable(projectKey).ifPresent(key -> httpRequest.setParam("project", key));
    ofNullable(projectName).ifPresent(name -> httpRequest.setParam("name", name));
    ofNullable(mainBranch).ifPresent(name -> httpRequest.setParam("mainBranch", mainBranch));
    ofNullable(newCodeDefinitionType).ifPresent(type -> httpRequest.setParam(PARAM_NEW_CODE_DEFINITION_TYPE, type));
    ofNullable(newCodeDefinitionValue).ifPresent(value -> httpRequest.setParam(PARAM_NEW_CODE_DEFINITION_VALUE, value));
    return httpRequest.executeProtobuf(CreateWsResponse.class);
  }

}
