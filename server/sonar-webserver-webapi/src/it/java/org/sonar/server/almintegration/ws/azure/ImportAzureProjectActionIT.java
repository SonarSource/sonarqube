/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.almintegration.ws.azure;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureProject;
import org.sonar.alm.client.azure.GsonAzureRepo;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almintegration.ws.ImportHelper;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.almsettings.DevOpsProjectCreatorFactory;
import org.sonar.server.common.almsettings.azuredevops.AzureDevOpsProjectCreatorFactory;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver;
import org.sonar.server.common.permission.PermissionTemplateService;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.common.project.ImportProjectService;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.es.TestIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Projects;

import static java.util.Objects.requireNonNull;
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
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_TYPE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_VALUE;

public class ImportAzureProjectActionIT {

  private static final String GENERATED_PROJECT_KEY = "TEST_PROJECT_KEY";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public final I18nRule i18n = new I18nRule();

  private final AzureDevOpsHttpClient azureDevOpsHttpClient = mock(AzureDevOpsHttpClient.class);

  private final DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);

  private final ComponentUpdater componentUpdater = new ComponentUpdater(db.getDbClient(), i18n, System2.INSTANCE,
    mock(PermissionTemplateService.class), new FavoriteUpdater(db.getDbClient()), new TestIndexers(), new SequenceUuidFactory(),
    defaultBranchNameResolver, mock(PermissionUpdater.class), mock(PermissionService.class));

  private final Encryption encryption = mock(Encryption.class);
  private final ImportHelper importHelper = new ImportHelper(db.getDbClient(), userSession);
  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);
  private final ProjectKeyGenerator projectKeyGenerator = mock(ProjectKeyGenerator.class);

  private final PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private final NewCodeDefinitionResolver newCodeDefinitionResolver = new NewCodeDefinitionResolver(db.getDbClient(), editionProvider);
  private final ProjectCreator projectCreator = new ProjectCreator(db.getDbClient(), userSession, projectDefaultVisibility, componentUpdater);
  private final DevOpsProjectCreatorFactory devOpsProjectCreatorFactory = new AzureDevOpsProjectCreatorFactory(db.getDbClient(), userSession, azureDevOpsHttpClient, projectCreator,
    projectKeyGenerator);

  private final ImportProjectService importProjectService = new ImportProjectService(db.getDbClient(), devOpsProjectCreatorFactory, userSession, componentUpdater,
    newCodeDefinitionResolver);
  private final ImportAzureProjectAction importAzureProjectAction = new ImportAzureProjectAction(
    importHelper, importProjectService);
  private final WsActionTester ws = new WsActionTester(importAzureProjectAction);

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PRIVATE);
    when(projectKeyGenerator.generateUniqueProjectKey(any(), any())).thenReturn(GENERATED_PROJECT_KEY);
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void import_project() {
    AlmSettingDto almSetting = configureUserAndAlmSettings();
    GsonAzureRepo repo = mockAzureInteractions(almSetting);

    Projects.CreateWsResponse.Project result = callWebserviceAndEnsureProjectIsCreated(almSetting, repo);

    ProjectDto projectDto = getProjectDto(result);

    Optional<ProjectAlmSettingDto> projectAlmSettingDto = db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto);

    assertThat(projectAlmSettingDto.get().getAlmRepo()).isEqualTo("repo-name");
    assertThat(projectAlmSettingDto.get().getAlmSettingUuid()).isEqualTo(almSetting.getUuid());
    assertThat(projectAlmSettingDto.get().getAlmSlug()).isEqualTo("project-name");

    Optional<BranchDto> mainBranch = db.getDbClient()
      .branchDao()
      .selectByProject(db.getSession(), projectDto)
      .stream()
      .filter(BranchDto::isMain)
      .findFirst();
    assertThat(mainBranch).isPresent();
    assertThat(mainBranch.get().getKey()).hasToString("repo-default-branch");

    verify(projectKeyGenerator).generateUniqueProjectKey(repo.getProject().getName(), repo.getName());
  }

  @Test
  public void importProject_whenCallIsNotFromBrowser_shouldFlagTheProjectAsCreatedFromApi() {
    AlmSettingDto almSetting = configureUserAndAlmSettings();
    GsonAzureRepo repo = mockAzureInteractions(almSetting);

    Projects.CreateWsResponse.Project result = callWebserviceAndEnsureProjectIsCreated(almSetting, repo);

    assertThat(getProjectDto(result).getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_API);
  }

  @Test
  public void importProject_whenCallIsFromBrowser_shouldFlagTheProjectAsCreatedFromBrowser() {
    AlmSettingDto almSetting = configureUserAndAlmSettings();
    userSession.flagSessionAsGui();

    GsonAzureRepo repo = mockAzureInteractions(almSetting);

    Projects.CreateWsResponse.Project result = callWebserviceAndEnsureProjectIsCreated(almSetting, repo);

    assertThat(getProjectDto(result).getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_BROWSER);
  }

  private Projects.CreateWsResponse.Project callWebserviceAndEnsureProjectIsCreated(AlmSettingDto almSetting, GsonAzureRepo repo) {
    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repo.getName());
    return result;
  }

  @Test
  public void import_project_with_NCD_developer_edition() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    AlmSettingDto almSetting = configureUserAndAlmSettings();
    mockAzureInteractions(almSetting);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "NUMBER_OF_DAYS")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

    ProjectDto projectDto = getProjectDto(result);
    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue, NewCodePeriodDto::getBranchUuid)
      .containsExactly(NUMBER_OF_DAYS, "30", null);
  }

  @Test
  public void import_project_with_NCD_community_edition() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));

    AlmSettingDto almSetting = configureUserAndAlmSettings();
    mockAzureInteractions(almSetting);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "NUMBER_OF_DAYS")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    BranchDto branchDto = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), projectDto.get().getUuid()).orElseThrow();
    assertThat(projectDto).isPresent();

    String projectUuid = projectDto.get().getUuid();
    assertThat(db.getDbClient().newCodePeriodDao().selectByBranch(db.getSession(), projectUuid, branchDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue, NewCodePeriodDto::getBranchUuid)
      .containsExactly(NUMBER_OF_DAYS, "30", branchDto.getUuid());
  }

  @Test
  public void import_project_throw_IAE_when_newCodeDefinitionValue_provided_and_no_newCodeDefinitionType() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    AlmSettingDto almSetting = configureUserAndAlmSettings();
    mockAzureInteractions(almSetting);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "NUMBER_OF_DAYS")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

    ProjectDto projectDto = getProjectDto(result);
    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue, NewCodePeriodDto::getBranchUuid)
      .containsExactly(NUMBER_OF_DAYS, "30", null);
  }

  @Test
  public void import_project_reference_branch_ncd_no_default_branch_name() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    AlmSettingDto almSetting = configureUserAndAlmSettings();
    GsonAzureRepo repo = getEmptyGsonAzureRepo();
    when(azureDevOpsHttpClient.getRepo(almSetting.getUrl(), almSetting.getDecryptedPersonalAccessToken(encryption),
      "project-name", "repo-name"))
        .thenReturn(repo);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "REFERENCE_BRANCH")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

    ProjectDto projectDto = getProjectDto(result);
    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue)
      .containsExactly(REFERENCE_BRANCH, DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void import_project_reference_branch_ncd() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    AlmSettingDto almSetting = configureUserAndAlmSettings();
    mockAzureInteractions(almSetting);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "REFERENCE_BRANCH")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

    ProjectDto projectDto = getProjectDto(result);
    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue)
      .containsExactly(REFERENCE_BRANCH, "repo-default-branch");
  }

  @Test
  public void import_project_from_empty_repo() {
    AlmSettingDto almSetting = configureUserAndAlmSettings();
    GsonAzureRepo repo = getEmptyGsonAzureRepo();
    when(azureDevOpsHttpClient.getRepo(almSetting.getUrl(), almSetting.getDecryptedPersonalAccessToken(encryption),
      "project-name", "repo-name"))
        .thenReturn(repo);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30");

    assertThatThrownBy(() -> request.executeProtobuf(Projects.CreateWsResponse.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("New code definition type is required when new code definition value is provided");
  }

  @Test
  public void fail_when_not_logged_in() {
    TestRequest request = ws.newRequest()
      .setParam("almSetting", "azure")
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name");

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_missing_project_creator_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(SCAN);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "azure")
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void check_pat_is_missing() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("personal access token for '" + almSetting.getKey() + "' is missing");
  }

  @Test
  public void fail_project_already_exists() {
    AlmSettingDto almSetting = configureUserAndAlmSettings();
    GsonAzureRepo repo = getGsonAzureRepo();
    db.components().insertPublicProject(p -> p.setKey(GENERATED_PROJECT_KEY)).getMainBranchComponent();

    when(azureDevOpsHttpClient.getRepo(almSetting.getUrl(), almSetting.getDecryptedPersonalAccessToken(encryption),
      "project-name", "repo-name")).thenReturn(repo);
    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"%s\". A similar key already exists: \"%s\"", GENERATED_PROJECT_KEY,
        GENERATED_PROJECT_KEY);
  }

  @Test
  public void importProject_whenAlmSettingKeyDoesNotExist_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "unknown")
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform configuration 'unknown' not found.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndNoConfig_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    TestRequest request = ws.newRequest()
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("There is no AZURE_DEVOPS configuration for DevOps Platform. Please add one.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndMultipleConfigs_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    db.almSettings().insertAzureAlmSetting();
    db.almSettings().insertAzureAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter almSetting is required as there are multiple DevOps Platform configurations.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndOnlyOneConfig_shouldImport() {
    AlmSettingDto almSetting = configureUserAndAlmSettings();
    mockAzureInteractions(almSetting);

    TestRequest request = ws.newRequest()
      .setParam("projectName", "project-name")
      .setParam("repositoryName", "repo-name");

    assertThatNoException().isThrownBy(request::execute);
  }

  @Test
  public void define() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.6");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("almSetting", false),
        tuple("projectName", true),
        tuple("repositoryName", true),
        tuple(PARAM_NEW_CODE_DEFINITION_TYPE, false),
        tuple(PARAM_NEW_CODE_DEFINITION_VALUE, false));
    assertThat(def.deprecatedSince()).isEqualTo("10.5");
  }

  private AlmSettingDto configureUserAndAlmSettings() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertAzureAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setPersonalAccessToken(requireNonNull(almSetting.getDecryptedPersonalAccessToken(encryption)));
      dto.setUserUuid(user.getUuid());
    });
    return almSetting;
  }

  private GsonAzureRepo mockAzureInteractions(AlmSettingDto almSetting) {
    GsonAzureRepo repo = getGsonAzureRepo();
    when(azureDevOpsHttpClient.getRepo(almSetting.getUrl(), almSetting.getDecryptedPersonalAccessToken(encryption),
      "project-name", "repo-name"))
        .thenReturn(repo);
    return repo;
  }

  private ProjectDto getProjectDto(Projects.CreateWsResponse.Project result) {
    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    return projectDto.get();
  }

  private GsonAzureRepo getGsonAzureRepo() {
    return new GsonAzureRepo("repo-id", "repo-name", "repo-url", "repo-web-url",
      new GsonAzureProject("project-name", "project-description"),
      "refs/heads/repo-default-branch");
  }

  private GsonAzureRepo getEmptyGsonAzureRepo() {
    return new GsonAzureRepo("repo-id", "repo-name", "repo-url", "repo-web-url",
      new GsonAzureProject("project-name", "project-description"), null);
  }

}
