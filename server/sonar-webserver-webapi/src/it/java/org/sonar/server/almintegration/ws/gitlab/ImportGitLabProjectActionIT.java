/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.almintegration.ws.gitlab;

import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.gitlab.GitLabBranch;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.Project;
import org.sonar.api.utils.System2;
import org.sonar.core.i18n.I18n;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almintegration.ws.ImportHelper;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.es.TestIndexers;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.newcodeperiod.NewCodeDefinitionResolver;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Projects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.NUMBER_OF_DAYS;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_TYPE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_VALUE;

public class ImportGitLabProjectActionIT {

  private static final String PROJECT_KEY_NAME = "PROJECT_NAME";

  private final System2 system2 = mock(System2.class);

  @Rule
  public UserSessionRule userSession = standalone();

  @Rule
  public DbTester db = DbTester.create(system2);

  DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);

  private final ComponentUpdater componentUpdater = new ComponentUpdater(db.getDbClient(), mock(I18n.class), System2.INSTANCE,
    mock(PermissionTemplateService.class), new FavoriteUpdater(db.getDbClient()), new TestIndexers(), new SequenceUuidFactory(),
    defaultBranchNameResolver, mock(PermissionUpdater.class), mock(PermissionService.class));

  private final GitlabApplicationClient gitlabApplicationClient = mock(GitlabApplicationClient.class);
  private final ImportHelper importHelper = new ImportHelper(db.getDbClient(), userSession);
  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);
  private final ProjectKeyGenerator projectKeyGenerator = mock(ProjectKeyGenerator.class);
  private PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private NewCodeDefinitionResolver newCodeDefinitionResolver = new NewCodeDefinitionResolver(db.getDbClient(), editionProvider);
  private final ImportGitLabProjectAction importGitLabProjectAction = new ImportGitLabProjectAction(
    db.getDbClient(), userSession, projectDefaultVisibility, gitlabApplicationClient, componentUpdater, importHelper, projectKeyGenerator, newCodeDefinitionResolver,
    defaultBranchNameResolver);
  private final WsActionTester ws = new WsActionTester(importGitLabProjectAction);

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PRIVATE);
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void import_project_developer_edition() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = mockGitlabProject(singletonList(new GitLabBranch("master", true)));

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("gitlabProjectId", "12345")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "NUMBER_OF_DAYS")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30")
      .executeProtobuf(Projects.CreateWsResponse.class);

    verify(gitlabApplicationClient).getProject(almSetting.getUrl(), "PAT", 12345L);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(PROJECT_KEY_NAME);
    assertThat(result.getName()).isEqualTo(project.getName());

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto.get())).isPresent();

    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.get().getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue, NewCodePeriodDto::getBranchUuid)
      .containsExactly(NUMBER_OF_DAYS, "30", null);
  }

  @Test
  public void import_project_community_edition() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));

    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    mockGitlabProject(singletonList(new GitLabBranch("master", true)));

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("gitlabProjectId", "12345")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "NUMBER_OF_DAYS")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    BranchDto branchDto = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), projectDto.get().getUuid()).orElseThrow();

    String projectUuid = projectDto.get().getUuid();
    assertThat(db.getDbClient().newCodePeriodDao().selectByBranch(db.getSession(), projectUuid, branchDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue, NewCodePeriodDto::getBranchUuid)
      .containsExactly(NUMBER_OF_DAYS, "30", branchDto.getUuid());
  }

  @Test
  public void import_project_with_specific_different_default_branch() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = mockGitlabProject(singletonList(new GitLabBranch("main", true)));

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("gitlabProjectId", "12345")
      .executeProtobuf(Projects.CreateWsResponse.class);

    verify(gitlabApplicationClient).getProject(almSetting.getUrl(), "PAT", 12345L);
    verify(gitlabApplicationClient).getBranches(almSetting.getUrl(), "PAT", 12345L);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(PROJECT_KEY_NAME);
    assertThat(result.getName()).isEqualTo(project.getName());

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto.get())).isPresent();

    Assertions.assertThat(db.getDbClient().branchDao().selectByProject(db.getSession(), projectDto.get()))
      .extracting(BranchDto::getKey, BranchDto::isMain)
      .containsExactlyInAnyOrder(tuple("main", true));
  }

  @Test
  public void import_project_no_gitlab_default_branch() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = mockGitlabProject(emptyList());

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("gitlabProjectId", "12345")
      .executeProtobuf(Projects.CreateWsResponse.class);

    verify(gitlabApplicationClient).getProject(almSetting.getUrl(), "PAT", 12345L);
    verify(gitlabApplicationClient).getBranches(almSetting.getUrl(), "PAT", 12345L);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(PROJECT_KEY_NAME);
    assertThat(result.getName()).isEqualTo(project.getName());

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto.get())).isPresent();

    Assertions.assertThat(db.getDbClient().branchDao().selectByProject(db.getSession(), projectDto.get()))
      .extracting(BranchDto::getKey, BranchDto::isMain)
      .containsExactlyInAnyOrder(tuple(DEFAULT_MAIN_BRANCH_NAME, true));
  }

  @Test
  public void import_project_without_NCD() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = mockGitlabProject(singletonList(new GitLabBranch("master", true)));

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("gitlabProjectId", "12345")
      .executeProtobuf(Projects.CreateWsResponse.class);

    verify(gitlabApplicationClient).getProject(almSetting.getUrl(), "PAT", 12345L);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(PROJECT_KEY_NAME);
    assertThat(result.getName()).isEqualTo(project.getName());

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto.get())).isPresent();
  }

  @Test
  public void importProject_whenNonBrowserCall_setsCreationMethodToApi() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    mockGitlabProject(singletonList(new GitLabBranch("master", true)));

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("gitlabProjectId", "12345")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), response.getProject().getKey());
    assertThat(projectDto.orElseThrow().getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_API);
  }

  @Test
  public void importProject_whenBrowserCall_setsCreationMethodToBrowser() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    userSession.flagSessionAsGui();
    mockGitlabProject(singletonList(new GitLabBranch("master", true)));

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("gitlabProjectId", "12345")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), response.getProject().getKey());
    assertThat(projectDto.orElseThrow().getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_BROWSER);
  }

  @Test
  public void importProject_whenAlmSettingKeyDoesNotExist_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "unknown")
      .setParam("gitlabProjectId", "12345");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform configuration 'unknown' not found.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndNoConfig_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    TestRequest request = ws.newRequest()
      .setParam("gitlabProjectId", "12345");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("There is no GITLAB configuration for DevOps Platform. Please add one.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndMultipleConfigs_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    db.almSettings().insertGitlabAlmSetting();
    db.almSettings().insertGitlabAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("gitlabProjectId", "12345");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter almSetting is required as there are multiple DevOps Platform configurations.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndOnlyOneConfig_shouldImport() {
    configureUserAndPatAndAlmSettings();
    mockGitlabProject(emptyList());

    TestRequest request = ws.newRequest()
      .setParam("gitlabProjectId", "12345");

    assertThatNoException().isThrownBy(request::execute);
  }


  private AlmSettingDto configureUserAndPatAndAlmSettings() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    return insertGitLabConfigurationAndPat(user);
  }

  private AlmSettingDto insertGitLabConfigurationAndPat(UserDto user) {
    AlmSettingDto almSetting = db.almSettings().insertGitlabAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
      dto.setPersonalAccessToken("PAT");
    });
    return almSetting;
  }

  private Project mockGitlabProject(List<GitLabBranch> master) {
    Project project = new Project(randomAlphanumeric(5), randomAlphanumeric(5));
    when(gitlabApplicationClient.getProject(any(), any(), any())).thenReturn(project);
    when(gitlabApplicationClient.getBranches(any(), any(), any())).thenReturn(master);
    when(projectKeyGenerator.generateUniqueProjectKey(project.getPathWithNamespace())).thenReturn(PROJECT_KEY_NAME);
    return project;
  }

}
