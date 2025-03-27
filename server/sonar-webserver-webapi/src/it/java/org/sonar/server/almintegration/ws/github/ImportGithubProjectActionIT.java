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
package org.sonar.server.almintegration.ws.github;

import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.alm.client.github.GithubPermissionConverter;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.auth.github.ExpiringAppInstallationToken;
import org.sonar.auth.github.GitHubSettings;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.auth.github.GsonRepositoryCollaborator;
import org.sonar.auth.github.GsonRepositoryPermissions;
import org.sonar.core.i18n.I18n;
import org.sonar.core.platform.EditionProvider;
import org.sonar.core.platform.PlatformEditionProvider;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.almintegration.ws.ImportHelper;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.almsettings.github.GithubDevOpsProjectCreationContextService;
import org.sonar.server.common.almsettings.github.GithubProjectCreatorFactory;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver;
import org.sonar.server.common.permission.GroupPermissionChanger;
import org.sonar.server.common.permission.PermissionTemplateService;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.common.permission.UserPermissionChange;
import org.sonar.server.common.permission.UserPermissionChanger;
import org.sonar.server.common.project.ImportProjectService;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.component.ComponentTypesRule;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.IndexersImpl;
import org.sonar.server.es.TestIndexers;
import org.sonar.server.exceptions.BadConfigurationException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.index.FooIndexDefinition;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Projects;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.NUMBER_OF_DAYS;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.server.almintegration.ws.ImportHelper.PARAM_ALM_SETTING;
import static org.sonar.server.almintegration.ws.github.ImportGithubProjectAction.PARAM_REPOSITORY_KEY;
import static org.sonar.server.tester.UserSessionRule.standalone;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_TYPE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_VALUE;

public class ImportGithubProjectActionIT {

  private static final String PROJECT_KEY_NAME = "PROJECT_NAME";
  private static final String GENERATED_PROJECT_KEY = "generated_" + PROJECT_KEY_NAME;

  @Rule
  public UserSessionRule userSession = standalone();

  private final System2 system2 = mock(System2.class);
  private final GithubApplicationClientImpl appClient = mock(GithubApplicationClientImpl.class);
  private final DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);

  @Rule
  public DbTester db = DbTester.create(system2);
  private final PermissionTemplateService permissionTemplateService = mock(PermissionTemplateService.class);
  public EsTester es = EsTester.createCustom(new FooIndexDefinition());
  private final PermissionUpdater<UserPermissionChange> userPermissionUpdater = new PermissionUpdater(
    new IndexersImpl(new PermissionIndexer(db.getDbClient(), es.client())),
    Set.of(new UserPermissionChanger(db.getDbClient(), new SequenceUuidFactory()),
      new GroupPermissionChanger(db.getDbClient(), new SequenceUuidFactory())));
  private final PermissionService permissionService = new PermissionServiceImpl(new ComponentTypesRule().setRootQualifiers(ComponentQualifiers.PROJECT));
  private final ComponentUpdater componentUpdater = new ComponentUpdater(db.getDbClient(), mock(I18n.class), System2.INSTANCE,
    permissionTemplateService, new FavoriteUpdater(db.getDbClient()), new TestIndexers(), new SequenceUuidFactory(),
    defaultBranchNameResolver, userPermissionUpdater, permissionService);

  private final ImportHelper importHelper = new ImportHelper(db.getDbClient(), userSession);
  private final ProjectKeyGenerator projectKeyGenerator = mock(ProjectKeyGenerator.class);
  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);
  private final PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);

  private final GitHubSettings gitHubSettings = mock(GitHubSettings.class);
  private final NewCodeDefinitionResolver newCodeDefinitionResolver = new NewCodeDefinitionResolver(db.getDbClient(), editionProvider);

  private final ManagedProjectService managedProjectService = mock(ManagedProjectService.class);

  private final GithubPermissionConverter githubPermissionConverter = mock();

  private final GithubDevOpsProjectCreationContextService githubDevOpsProjectService = new GithubDevOpsProjectCreationContextService(db.getDbClient(), userSession, appClient);

  private final ProjectCreator projectCreator = new ProjectCreator(userSession, projectDefaultVisibility, componentUpdater);

  private final GithubProjectCreatorFactory gitHubProjectCreatorFactory = new GithubProjectCreatorFactory(db.getDbClient(),
    null, appClient, projectKeyGenerator, projectCreator, gitHubSettings, githubPermissionConverter, userPermissionUpdater, permissionService,
    managedProjectService, githubDevOpsProjectService);

  private final ImportProjectService importProjectService = new ImportProjectService(db.getDbClient(), gitHubProjectCreatorFactory, userSession, componentUpdater,
    newCodeDefinitionResolver);
  private final WsActionTester ws = new WsActionTester(new ImportGithubProjectAction(importProjectService, importHelper));

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PUBLIC);
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void importProject_ifProjectWithSameNameDoesNotExist_importSucceed() {
    AlmSettingDto githubAlmSetting = setupUserWithPatAndAlmSettings();

    GithubApplicationClient.Repository repository = mockGithubDevOpsAppInteractions();
    mockGithubAuthAppInteractions();
    when(gitHubSettings.isProvisioningEnabled()).thenReturn(true);

    Projects.CreateWsResponse response = callWebService(githubAlmSetting);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repository.getName());

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto.get())).isPresent();
    Optional<BranchDto> mainBranch = db.getDbClient().branchDao().selectByProject(db.getSession(), projectDto.get()).stream().filter(BranchDto::isMain).findAny();
    assertThat(mainBranch).isPresent();
    assertThat(mainBranch.get().getKey()).isEqualTo("default-branch");

    verify(managedProjectService).queuePermissionSyncTask(userSession.getUuid(), mainBranch.get().getUuid(), projectDto.get().getUuid());
  }

  @Test
  public void importProject_withNCD_developer_edition() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    AlmSettingDto githubAlmSetting = setupUserWithPatAndAlmSettings();

    mockGithubDevOpsAppInteractions();

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_REPOSITORY_KEY, "octocat/" + PROJECT_KEY_NAME)
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "NUMBER_OF_DAYS")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();

    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.get().getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue, NewCodePeriodDto::getBranchUuid)
      .containsExactly(NUMBER_OF_DAYS, "30", null);
  }

  @Test
  public void importProject_withNCD_community_edition() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));

    AlmSettingDto githubAlmSetting = setupUserWithPatAndAlmSettings();

    mockGithubDevOpsAppInteractions();

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_REPOSITORY_KEY, "octocat/" + PROJECT_KEY_NAME)
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "NUMBER_OF_DAYS")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    BranchDto branchDto = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), projectDto.get().getUuid()).orElseThrow();

    String projectUuid = projectDto.get().getUuid();
    assertThat(db.getDbClient().newCodePeriodDao().selectByBranch(db.getSession(), projectUuid, branchDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue, NewCodePeriodDto::getBranchUuid)
      .containsExactly(NUMBER_OF_DAYS, "30", branchDto.getUuid());
  }

  @Test
  public void importProject_reference_branch_ncd_no_default_branch() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn("default-branch");

    AlmSettingDto githubAlmSetting = setupUserWithPatAndAlmSettings();

    mockGithubDevOpsAppInteractions();

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_REPOSITORY_KEY, "octocat/" + PROJECT_KEY_NAME)
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "reference_branch")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();

    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.get().getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue)
      .containsExactly(REFERENCE_BRANCH, "default-branch");
  }

  @Test
  public void importProject_reference_branch_ncd() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    AlmSettingDto githubAlmSetting = setupUserWithPatAndAlmSettings();

    mockGithubDevOpsAppInteractions();

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_REPOSITORY_KEY, "octocat/" + PROJECT_KEY_NAME)
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "reference_branch")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);

    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();

    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.get().getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue)
      .containsExactly(REFERENCE_BRANCH, "default-branch");
  }

  @Test
  public void importProject_ifProjectWithSameNameAlreadyExists_importSucceed() {
    AlmSettingDto githubAlmSetting = setupUserWithPatAndAlmSettings();
    db.components().insertPublicProject(p -> p.setKey("Hello-World")).getMainBranchComponent();

    GithubApplicationClient.Repository repository = mockGithubDevOpsAppInteractions();

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_REPOSITORY_KEY, "Hello-World")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repository.getName());
  }

  @Test
  public void importProject_whenGithubProvisioningIsDisabled_shouldApplyPermissionTemplateAndSetDefaultVisibility() {
    AlmSettingDto githubAlmSetting = setupUserWithPatAndAlmSettings();

    mockGithubDevOpsAppInteractions();
    when(gitHubSettings.isProvisioningEnabled()).thenReturn(false);

    ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_REPOSITORY_KEY, "octocat/" + PROJECT_KEY_NAME)
      .executeProtobuf(Projects.CreateWsResponse.class);

    ArgumentCaptor<EntityDto> projectDtoArgumentCaptor = ArgumentCaptor.forClass(EntityDto.class);
    verify(permissionTemplateService).applyDefaultToNewComponent(any(DbSession.class), projectDtoArgumentCaptor.capture(), eq(userSession.getUuid()));
    EntityDto capturedProjectDto = projectDtoArgumentCaptor.getValue();
    assertThat(capturedProjectDto.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(capturedProjectDto.isPrivate()).isFalse();

  }

  @Test
  public void importProject_whenGithubProvisioningIsEnabled_shouldNotApplyPermissionTemplate() {
    AlmSettingDto githubAlmSetting = setupUserWithPatAndAlmSettings();

    when(gitHubSettings.isProvisioningEnabled()).thenReturn(true);
    mockGithubDevOpsAppInteractions();
    mockGithubAuthAppInteractions();

    ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_REPOSITORY_KEY, "octocat/" + PROJECT_KEY_NAME)
      .executeProtobuf(Projects.CreateWsResponse.class);

    verify(permissionTemplateService, never()).applyDefaultToNewComponent(any(), any(), any());

  }

  private void mockGithubAuthAppInteractions() {
    when(gitHubSettings.appId()).thenReturn("432");
    when(gitHubSettings.privateKey()).thenReturn("private key");
    when(gitHubSettings.apiURL()).thenReturn("http://www.url.com");

    ExpiringAppInstallationToken appInstallationToken = mock();

    when(appClient.getInstallationId(any(), any())).thenReturn(Optional.of(321L));
    when(appClient.createAppInstallationToken(any(), eq(321L))).thenReturn(Optional.of(appInstallationToken));

    GsonRepositoryCollaborator gsonRepositoryCollaborator = new GsonRepositoryCollaborator("toto", 2, "admin", new GsonRepositoryPermissions(true, true, true, true, true));
    when(appClient.getRepositoryCollaborators(gitHubSettings.apiURL(), appInstallationToken, "octocat", PROJECT_KEY_NAME)).thenReturn(Set.of(gsonRepositoryCollaborator));

    String role = gsonRepositoryCollaborator.roleName();
    GsonRepositoryPermissions permissions = gsonRepositoryCollaborator.permissions();
    when(githubPermissionConverter.toSonarqubeRolesWithFallbackOnRepositoryPermissions(Set.of(), role, permissions)).thenReturn(Set.of(ProjectPermission.SCAN));

  }

  @Test
  public void importProject_shouldSetCreationMethodToApi_ifNonBrowserRequest() {
    AlmSettingDto githubAlmSetting = setupUserWithPatAndAlmSettings();
    mockGithubDevOpsAppInteractions();

    Projects.CreateWsResponse response = callWebService(githubAlmSetting);

    assertThat(response.getProject().getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), response.getProject().getKey());
    assertThat(projectDto.orElseThrow().getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_API);
  }

  @Test
  public void importProject_shouldSetCreationMethodToBrowser_ifBrowserRequest() {
    AlmSettingDto githubAlmSetting = setupUserWithPatAndAlmSettings();
    userSession.flagSessionAsGui();
    mockGithubDevOpsAppInteractions();

    Projects.CreateWsResponse response = callWebService(githubAlmSetting);

    assertThat(response.getProject().getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), response.getProject().getKey());
    assertThat(projectDto.orElseThrow().getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_BROWSER);
  }

  @Test
  public void importProject_whenAlmSettingKeyDoesNotExist_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, "unknown")
      .setParam(PARAM_REPOSITORY_KEY, "test/repo");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform configuration 'unknown' not found.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndNoConfig_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);

    TestRequest request = ws.newRequest()
      .setParam(PARAM_REPOSITORY_KEY, "test/repo");
    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("There is no GITHUB configuration for DevOps Platform. Please add one.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndMultipleConfigs_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);
    db.almSettings().insertGitHubAlmSetting();
    db.almSettings().insertGitHubAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_REPOSITORY_KEY, "test/repo");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter almSetting is required as there are multiple DevOps Platform configurations.");
  }

  @Test
  public void importProject_whenProvisioningIsEnabledButConfigDoesNotAllowAccessToRepo_shouldThrow() {
    AlmSettingDto githubAlmSetting = setupUserWithPatAndAlmSettings();

    when(gitHubSettings.isProvisioningEnabled()).thenReturn(true);
    mockGithubDevOpsAppInteractions();
    mockGithubAuthAppInteractions();

    when(appClient.getInstallationId(any(), any())).thenReturn(Optional.empty());

    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_REPOSITORY_KEY, "octocat/" + PROJECT_KEY_NAME);

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadConfigurationException.class)
      .hasMessage(format("GitHub auto-provisioning is activated. However the repo %s is not in the scope of the authentication application. "
        + "The permissions can't be checked, and the project can not be created.",
        "octocat/" + PROJECT_KEY_NAME));
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndOnlyOneConfig_shouldImport() {
    AlmSettingDto githubAlmSetting = setupUserWithPatAndAlmSettings();

    mockGithubDevOpsAppInteractions();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_REPOSITORY_KEY, "octocat/" + PROJECT_KEY_NAME);

    assertThatNoException().isThrownBy(request::execute);
  }

  private Projects.CreateWsResponse callWebService(AlmSettingDto githubAlmSetting) {
    return ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_REPOSITORY_KEY, "octocat/" + PROJECT_KEY_NAME)
      .executeProtobuf(Projects.CreateWsResponse.class);
  }

  private GithubApplicationClient.Repository mockGithubDevOpsAppInteractions() {
    GithubApplicationClient.Repository repository = new GithubApplicationClient.Repository(1L, PROJECT_KEY_NAME, false,
      "octocat/" + PROJECT_KEY_NAME,
      "https://github.sonarsource.com/api/v3/repos/octocat/" + PROJECT_KEY_NAME, "default-branch");
    when(appClient.getRepository(any(), any(), any())).thenReturn(Optional.of(repository));
    when(projectKeyGenerator.generateUniqueProjectKey(repository.getFullName())).thenReturn(GENERATED_PROJECT_KEY);
    return repository;
  }

  @Test
  public void fail_when_not_logged_in() {
    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, "asdfghjkl")
      .setParam(PARAM_REPOSITORY_KEY, "test/repo");
    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_missing_create_project_permission() {
    TestRequest request = ws.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_personal_access_token_doesnt_exist() {
    AlmSettingDto githubAlmSetting = setupUserAndAlmSettings();

    TestRequest request = ws.newRequest()
      .setParam(PARAM_ALM_SETTING, githubAlmSetting.getKey())
      .setParam(PARAM_REPOSITORY_KEY, "test/repo");
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("No personal access token found");
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.4");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple(PARAM_ALM_SETTING, false),
        tuple(PARAM_REPOSITORY_KEY, true),
        tuple(PARAM_NEW_CODE_DEFINITION_TYPE, false),
        tuple(PARAM_NEW_CODE_DEFINITION_VALUE, false));
    assertThat(def.deprecatedSince()).isEqualTo("10.5");
  }

  private AlmSettingDto setupUserWithPatAndAlmSettings() {
    AlmSettingDto almSettings = setupUserAndAlmSettings();
    db.almPats().insert(p -> p.setAlmSettingUuid(almSettings.getUuid()).setUserUuid(requireNonNull(userSession.getUuid())));
    return almSettings;
  }

  private AlmSettingDto setupUserAndAlmSettings() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(GlobalPermission.PROVISION_PROJECTS);
    return db.almSettings().insertGitHubAlmSetting(alm -> alm.setClientId("client_123").setClientSecret("client_secret_123"));
  }
}
