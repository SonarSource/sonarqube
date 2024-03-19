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
package org.sonar.server.almintegration.ws.bitbucketcloud;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.MainBranch;
import org.sonar.alm.client.bitbucket.bitbucketcloud.Project;
import org.sonar.alm.client.bitbucket.bitbucketcloud.Repository;
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
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.es.TestIndexers;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.l18n.I18nRule;
import org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.common.permission.PermissionTemplateService;
import org.sonar.server.common.permission.PermissionUpdater;
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
import static org.sonar.db.newcodeperiod.NewCodePeriodType.NUMBER_OF_DAYS;
import static org.sonar.db.newcodeperiod.NewCodePeriodType.REFERENCE_BRANCH;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_TYPE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_VALUE;

public class ImportBitbucketCloudRepoActionIT {

  private static final String GENERATED_PROJECT_KEY = "TEST_PROJECT_KEY";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public final I18nRule i18n = new I18nRule();

  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);
  private final BitbucketCloudRestClient bitbucketCloudRestClient = mock(BitbucketCloudRestClient.class);

  DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);
  private final ComponentUpdater componentUpdater = new ComponentUpdater(db.getDbClient(), i18n, System2.INSTANCE,
    mock(PermissionTemplateService.class), new FavoriteUpdater(db.getDbClient()), new TestIndexers(), new SequenceUuidFactory(),
    defaultBranchNameResolver, mock(PermissionUpdater.class), mock(PermissionService.class));

  private final ImportHelper importHelper = new ImportHelper(db.getDbClient(), userSession);
  private final ProjectKeyGenerator projectKeyGenerator = mock(ProjectKeyGenerator.class);
  private PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private NewCodeDefinitionResolver newCodeDefinitionResolver = new NewCodeDefinitionResolver(db.getDbClient(), editionProvider);
  private final WsActionTester ws = new WsActionTester(new ImportBitbucketCloudRepoAction(db.getDbClient(), userSession,
    bitbucketCloudRestClient, projectDefaultVisibility, componentUpdater, importHelper, projectKeyGenerator, newCodeDefinitionResolver, defaultBranchNameResolver));

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PRIVATE);
    when(projectKeyGenerator.generateUniqueProjectKey(any(), any())).thenReturn(GENERATED_PROJECT_KEY);
  }

  @Test
  public void import_project() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Repository repo = mockBitbucketCloudRepo();

    Projects.CreateWsResponse.Project result = callWebServiceAndVerifyProjectCreation(almSetting, repo);

    ProjectDto projectDto = getProjectDto(result);
    assertThat(projectDto.getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_API);

    Optional<ProjectAlmSettingDto> projectAlmSettingDto = db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto);
    assertThat(projectAlmSettingDto).isPresent();
    assertThat(projectAlmSettingDto.get().getAlmRepo()).isEqualTo("repo-slug-1");

    Optional<BranchDto> branchDto = db.getDbClient().branchDao().selectByBranchKey(db.getSession(), projectDto.getUuid(), "develop");
    assertThat(branchDto).isPresent();
    assertThat(branchDto.get().isMain()).isTrue();
    verify(projectKeyGenerator).generateUniqueProjectKey(requireNonNull(almSetting.getAppId()), repo.getSlug());

    assertThat(db.getDbClient().newCodePeriodDao().selectAll(db.getSession()))
      .isEmpty();
  }

  @Test
  public void importProject_whenCallIsNotFromBrowser_shouldFlagTheProjectAsCreatedFromApi() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Repository repo = mockBitbucketCloudRepo();

    Projects.CreateWsResponse.Project result = callWebServiceAndVerifyProjectCreation(almSetting, repo);

    ProjectDto projectDto = getProjectDto(result);
    assertThat(projectDto.getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_API);
  }

  @Test
  public void importProject_whenCallIsFromBrowser_shouldFlagTheProjectAsCreatedFromBrowser() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    userSession.flagSessionAsGui();
    Repository repo = mockBitbucketCloudRepo();

    Projects.CreateWsResponse.Project result = callWebServiceAndVerifyProjectCreation(almSetting, repo);

    ProjectDto projectDto = getProjectDto(result);
    assertThat(projectDto.getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_BROWSER);
  }

  private Projects.CreateWsResponse.Project callWebServiceAndVerifyProjectCreation(AlmSettingDto almSetting, Repository repo) {
    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("repositorySlug", "repo-slug-1")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repo.getName());
    return result;
  }

  @Test
  public void import_project_with_NCD_developer_edition() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Repository repo = mockBitbucketCloudRepo();

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("repositorySlug", "repo-slug-1")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "NUMBER_OF_DAYS")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repo.getName());

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

    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Repository repo = mockBitbucketCloudRepo();

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("repositorySlug", "repo-slug-1")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "NUMBER_OF_DAYS")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repo.getName());

    ProjectDto projectDto = getProjectDto(result);
    BranchDto branchDto = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), projectDto.getUuid()).orElseThrow();

    String projectUuid = projectDto.getUuid();
    assertThat(db.getDbClient().newCodePeriodDao().selectByBranch(db.getSession(), projectUuid, branchDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue, NewCodePeriodDto::getBranchUuid)
      .containsExactly(NUMBER_OF_DAYS, "30", branchDto.getUuid());
  }

  @Test
  public void import_project_reference_branch_ncd_no_default_branch_name() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn("default-branch");

    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Repository repo = getGsonBBCRepoWithNoMainBranchName();
    when(bitbucketCloudRestClient.getRepo(any(), any(), any())).thenReturn(repo);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("repositorySlug", "repo-slug-1")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "REFERENCE_BRANCH")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repo.getName());

    ProjectDto projectDto = getProjectDto(result);
    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue)
      .containsExactly(REFERENCE_BRANCH, "default-branch");
  }

  @Test
  public void import_project_reference_branch_NCD() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Repository repo = mockBitbucketCloudRepo();

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("repositorySlug", "repo-slug-1")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "REFERENCE_BRANCH")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repo.getName());

    ProjectDto projectDto = getProjectDto(result);
    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue)
      .containsExactly(REFERENCE_BRANCH, "develop");
  }

  @Test
  public void import_project_throw_IAE_when_newCodeDefinitionValue_provided_and_no_newCodeDefinitionType() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    mockBitbucketCloudRepo();

    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("repositorySlug", "repo-slug-1")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30");

    assertThatThrownBy(() -> request.executeProtobuf(Projects.CreateWsResponse.class))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("New code definition type is required when new code definition value is provided");
  }

  @Test
  public void fail_project_already_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    Repository repo = mockBitbucketCloudRepo();
    db.components().insertPublicProject(p -> p.setKey(GENERATED_PROJECT_KEY)).getMainBranchComponent();

    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("repositorySlug", "repo-slug-1");

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"%s\". A similar key already exists: \"%s\"", GENERATED_PROJECT_KEY, GENERATED_PROJECT_KEY);
  }

  @Test
  public void fail_when_not_logged_in() {
    TestRequest request = ws.newRequest()
      .setParam("almSetting", "sdgfdshfjztutz")
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug");

    assertThatThrownBy(request::execute)
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_missing_project_creator_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(SCAN);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "sdgfdshfjztutz")
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void check_pat_is_missing() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("repositorySlug", "repo");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Username and App Password for '" + almSetting.getKey() + "' is missing");
  }

  @Test
  public void fail_when_no_creation_project_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "anyvalue");

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessageContaining("Insufficient privileges");
  }

  @Test
  public void importProject_whenAlmSettingKeyDoesNotExist_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "unknown")
      .setParam("repositorySlug", "repo-slug");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("DevOps Platform configuration 'unknown' not found.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndNoConfig_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    TestRequest request = ws.newRequest()
      .setParam("repositorySlug", "repo-slug");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("There is no BITBUCKET_CLOUD configuration for DevOps Platform. Please add one.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndMultipleConfigs_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    db.almSettings().insertBitbucketCloudAlmSetting();
    db.almSettings().insertBitbucketCloudAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("repositorySlug", "repo-slug");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter almSetting is required as there are multiple DevOps Platform configurations.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndOnlyOneConfig_shouldImport() {
    configureUserAndPatAndAlmSettings();
    mockBitbucketCloudRepo();

    TestRequest request = ws.newRequest()
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug");

    assertThatNoException().isThrownBy(request::execute);
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("9.0");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("almSetting", false),
        tuple("repositorySlug", true),
        tuple(PARAM_NEW_CODE_DEFINITION_TYPE, false),
        tuple(PARAM_NEW_CODE_DEFINITION_VALUE, false));
  }

  private AlmSettingDto configureUserAndPatAndAlmSettings() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketCloudAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    return almSetting;
  }

  private Repository mockBitbucketCloudRepo() {
    Project project1 = new Project("PROJECT-UUID-ONE", "projectKey1", "projectName1");
    MainBranch mainBranch = new MainBranch("branch", "develop");
    Repository repo = new Repository("REPO-UUID-ONE", "repo-slug-1", "repoName1", project1, mainBranch);
    when(bitbucketCloudRestClient.getRepo(any(), any(), any())).thenReturn(repo);
    return repo;
  }

  private Repository getGsonBBCRepoWithNoMainBranchName() {
    Project project1 = new Project("PROJECT-UUID-ONE", "projectKey1", "projectName1");
    MainBranch mainBranch = new MainBranch("branch", null);
    return new Repository("REPO-UUID-ONE", "repo-slug-1", "repoName1", project1, mainBranch);
  }

  private ProjectDto getProjectDto(Projects.CreateWsResponse.Project result) {
    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    return projectDto.orElseThrow();
  }

}
