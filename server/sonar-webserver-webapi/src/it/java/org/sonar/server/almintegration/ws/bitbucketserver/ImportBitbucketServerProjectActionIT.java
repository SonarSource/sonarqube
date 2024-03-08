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
package org.sonar.server.almintegration.ws.bitbucketserver;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.bitbucketserver.Branch;
import org.sonar.alm.client.bitbucketserver.BranchesList;
import org.sonar.alm.client.bitbucketserver.Project;
import org.sonar.alm.client.bitbucketserver.Repository;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
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
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.favorite.FavoriteUpdater;
import org.sonar.server.l18n.I18nRule;
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

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
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

public class ImportBitbucketServerProjectActionIT {
  private static final String GENERATED_PROJECT_KEY = "TEST_PROJECT_KEY";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public final I18nRule i18n = new I18nRule();

  private final ProjectDefaultVisibility projectDefaultVisibility = mock(ProjectDefaultVisibility.class);
  private final BitbucketServerRestClient bitbucketServerRestClient = mock(BitbucketServerRestClient.class);
  private final DefaultBranchNameResolver defaultBranchNameResolver = mock(DefaultBranchNameResolver.class);
  private PlatformEditionProvider editionProvider = mock(PlatformEditionProvider.class);
  private NewCodeDefinitionResolver newCodeDefinitionResolver = new NewCodeDefinitionResolver(db.getDbClient(), editionProvider);

  private final ComponentUpdater componentUpdater = new ComponentUpdater(db.getDbClient(), i18n, System2.INSTANCE,
    mock(PermissionTemplateService.class), new FavoriteUpdater(db.getDbClient()), new TestIndexers(), new SequenceUuidFactory(),
    defaultBranchNameResolver, mock(PermissionUpdater.class), mock(PermissionService.class));

  private final Random random = new SecureRandom();

  private final ImportHelper importHelper = new ImportHelper(db.getDbClient(), userSession);
  private final ProjectKeyGenerator projectKeyGenerator = mock(ProjectKeyGenerator.class);
  private final WsActionTester ws = new WsActionTester(new ImportBitbucketServerProjectAction(db.getDbClient(), userSession,
    bitbucketServerRestClient, projectDefaultVisibility, componentUpdater, importHelper, projectKeyGenerator, newCodeDefinitionResolver, defaultBranchNameResolver));

  private static BranchesList defaultBranchesList;

  @BeforeClass
  public static void beforeAll() {
    Branch defaultBranch = new Branch("default", true);
    defaultBranchesList = new BranchesList(Collections.singletonList(defaultBranch));
  }

  @Before
  public void before() {
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PRIVATE);
    when(projectKeyGenerator.generateUniqueProjectKey(any(), any())).thenReturn(GENERATED_PROJECT_KEY);
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void import_project() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = getGsonBBSProject();
    Repository repo = mockBitbucketServerRepo(project);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repo.getName());

    ProjectDto projectDto = getProjectDto(result);
    assertThat(projectDto.getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_API);

    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto)).isPresent();
    verify(projectKeyGenerator).generateUniqueProjectKey(requireNonNull(project.getKey()), repo.getSlug());
  }

  @Test
  public void importProject_whenCallIsNotFromBrowser_shouldFlagTheProjectAsCreatedFromApi() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = getGsonBBSProject();
    mockBitbucketServerRepo(project);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug")
      .executeProtobuf(Projects.CreateWsResponse.class);

    ProjectDto projectDto = getProjectDto(response.getProject());
    assertThat(projectDto.getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_API);
  }

  @Test
  public void importProject_whenCallIsFromBrowser_shouldFlagTheProjectAsCreatedFromBrowser() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    userSession.flagSessionAsGui();
    Project project = getGsonBBSProject();
    mockBitbucketServerRepo(project);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug")
      .executeProtobuf(Projects.CreateWsResponse.class);

    ProjectDto projectDto = getProjectDto(response.getProject());
    assertThat(projectDto.getCreationMethod()).isEqualTo(CreationMethod.ALM_IMPORT_BROWSER);
  }

  @Test
  public void import_project_with_NCD_developer_edition_sets_project_NCD() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = getGsonBBSProject();
    Repository repo = mockBitbucketServerRepo(project);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "NUMBER_OF_DAYS")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();
    assertThat(result.getKey()).isEqualTo(GENERATED_PROJECT_KEY);
    assertThat(result.getName()).isEqualTo(repo.getName());

    ProjectDto projectDto = getProjectDto(result);
    assertThat(db.getDbClient().projectAlmSettingDao().selectByProject(db.getSession(), projectDto)).isPresent();
    verify(projectKeyGenerator).generateUniqueProjectKey(requireNonNull(project.getKey()), repo.getSlug());

    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectDto.getUuid()))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue, NewCodePeriodDto::getBranchUuid)
      .containsExactly(NUMBER_OF_DAYS, "30", null);
  }

  @Test
  public void import_project_with_NCD_community_edition_sets_branch_NCD() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.COMMUNITY));

    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = getGsonBBSProject();
    mockBitbucketServerRepo(project);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "NUMBER_OF_DAYS")
      .setParam(PARAM_NEW_CODE_DEFINITION_VALUE, "30")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

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
  public void import_project_reference_branch_ncd_no_default_branch() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));
    when(defaultBranchNameResolver.getEffectiveMainBranchName()).thenReturn("default-branch");

    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = getGsonBBSProject();
    mockBitbucketServerRepo(project, new BranchesList());

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "REFERENCE_BRANCH")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

    ProjectDto projectDto = getProjectDto(result);

    String projectUuid = projectDto.getUuid();
    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectUuid))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue)
      .containsExactly(REFERENCE_BRANCH, "default-branch");
  }

  @Test
  public void import_project_reference_branch_ncd() {
    when(editionProvider.get()).thenReturn(Optional.of(EditionProvider.Edition.DEVELOPER));

    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = getGsonBBSProject();
    mockBitbucketServerRepo(project);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug")
      .setParam(PARAM_NEW_CODE_DEFINITION_TYPE, "REFERENCE_BRANCH")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

    ProjectDto projectDto = getProjectDto(result);

    String projectUuid = projectDto.getUuid();
    assertThat(db.getDbClient().newCodePeriodDao().selectByProject(db.getSession(), projectUuid))
      .isPresent()
      .get()
      .extracting(NewCodePeriodDto::getType, NewCodePeriodDto::getValue)
      .containsExactly(REFERENCE_BRANCH, "default");
  }

  @Test
  public void fail_project_already_exist() {
    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = getGsonBBSProject();
    mockBitbucketServerRepo(project);
    db.components().insertPublicProject(p -> p.setKey(GENERATED_PROJECT_KEY)).getMainBranchComponent();

    assertThatThrownBy(() -> {

      ws.newRequest()
        .setParam("almSetting", almSetting.getKey())
        .setParam("projectKey", "projectKey")
        .setParam("repositorySlug", "repo-slug")
        .execute();
    })
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Could not create Project with key: \"%s\". A similar key already exists: \"%s\"", GENERATED_PROJECT_KEY, GENERATED_PROJECT_KEY);
  }

  @Test
  public void fail_when_not_logged_in() {
    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", "sdgfdshfjztutz")
        .setParam("projectKey", "projectKey")
        .setParam("repositorySlug", "repo-slug")
        .execute();
    })
      .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  public void fail_when_missing_project_creator_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(SCAN);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", "sdgfdshfjztutz")
        .setParam("projectKey", "projectKey")
        .setParam("repositorySlug", "repo-slug")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void check_pat_is_missing() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertGitHubAlmSetting();

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", almSetting.getKey())
        .execute();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("personal access token for '" + almSetting.getKey() + "' is missing");
  }

  @Test
  public void fail_when_no_creation_project_permission() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);

    assertThatThrownBy(() -> {
      ws.newRequest()
        .setParam("almSetting", "anyvalue")
        .execute();
    })
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void handle_givenNoDefaultBranchFound_doNotUpdateDefaultBranchName() {
    BranchesList branchesList = new BranchesList();
    Branch branch = new Branch("not_a_master", false);
    branchesList.addBranch(branch);

    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = getGsonBBSProject();
    mockBitbucketServerRepo(project, branchesList);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

    ProjectDto projectDto = getProjectDto(result);
    Collection<BranchDto> branchDtos = db.getDbClient().branchDao().selectByProject(db.getSession(), projectDto);
    List<BranchDto> collect = branchDtos.stream().filter(BranchDto::isMain).toList();
    String mainBranchName = collect.iterator().next().getKey();
    assertThat(mainBranchName).isEqualTo(DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void handle_givenDefaultBranchNamedDefault_updateDefaultBranchNameToDefault() {
    BranchesList branchesList = new BranchesList();
    Branch branch = new Branch("default", true);
    branchesList.addBranch(branch);

    AlmSettingDto almSetting = configureUserAndPatAndAlmSettings();
    Project project = getGsonBBSProject();
    mockBitbucketServerRepo(project, branchesList);

    Projects.CreateWsResponse response = ws.newRequest()
      .setParam("almSetting", almSetting.getKey())
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug")
      .executeProtobuf(Projects.CreateWsResponse.class);

    Projects.CreateWsResponse.Project result = response.getProject();

    ProjectDto projectDto = getProjectDto(result);
    Collection<BranchDto> branchDtos = db.getDbClient().branchDao().selectByProject(db.getSession(), projectDto);
    List<BranchDto> collect = branchDtos.stream().filter(BranchDto::isMain).toList();
    String mainBranchName = collect.iterator().next().getKey();
    assertThat(mainBranchName).isEqualTo("default");
  }

  @Test
  public void importProject_whenAlmSettingKeyDoesNotExist_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    TestRequest request = ws.newRequest()
      .setParam("almSetting", "unknown")
      .setParam("projectKey", "projectKey")
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
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug");

    assertThatThrownBy(request::execute)
      .isInstanceOf(NotFoundException.class)
      .hasMessage("There is no BITBUCKET configuration for DevOps Platform. Please add one.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndMultipleConfigs_shouldThrow() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);

    db.almSettings().insertBitbucketAlmSetting();
    db.almSettings().insertBitbucketAlmSetting();

    TestRequest request = ws.newRequest()
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug");

    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Parameter almSetting is required as there are multiple DevOps Platform configurations.");
  }

  @Test
  public void importProject_whenNoAlmSettingKeyAndOnlyOneConfig_shouldImport() {
    configureUserAndPatAndAlmSettings();
    Project project = getGsonBBSProject();
    mockBitbucketServerRepo(project);

    TestRequest request = ws.newRequest()
      .setParam("projectKey", "projectKey")
      .setParam("repositorySlug", "repo-slug");

    assertThatNoException().isThrownBy(request::execute);
  }

  @Test
  public void definition() {
    WebService.Action def = ws.getDef();

    assertThat(def.since()).isEqualTo("8.2");
    assertThat(def.isPost()).isTrue();
    assertThat(def.params())
      .extracting(WebService.Param::key, WebService.Param::isRequired)
      .containsExactlyInAnyOrder(
        tuple("almSetting", false),
        tuple("repositorySlug", true),
        tuple("projectKey", true),
        tuple(PARAM_NEW_CODE_DEFINITION_TYPE, false),
        tuple(PARAM_NEW_CODE_DEFINITION_VALUE, false));
  }

  private AlmSettingDto configureUserAndPatAndAlmSettings() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).addPermission(PROVISION_PROJECTS);
    AlmSettingDto almSetting = db.almSettings().insertBitbucketAlmSetting();
    db.almPats().insert(dto -> {
      dto.setAlmSettingUuid(almSetting.getUuid());
      dto.setUserUuid(user.getUuid());
    });
    return almSetting;
  }

  private Repository mockBitbucketServerRepo(Project project) {
    return mockBitbucketServerRepo(project, defaultBranchesList);
  }

  private Repository mockBitbucketServerRepo(Project project, BranchesList branchesList) {
    Repository bbsResult = new Repository();
    bbsResult.setProject(project);
    bbsResult.setSlug(randomAlphanumeric(5));
    bbsResult.setName(randomAlphanumeric(5));
    bbsResult.setId(random.nextLong(100));
    when(bitbucketServerRestClient.getRepo(any(), any(), any(), any())).thenReturn(bbsResult);
    when(bitbucketServerRestClient.getBranches(any(), any(), any(), any())).thenReturn(branchesList);
    return bbsResult;
  }

  private Project getGsonBBSProject() {
    return new Project()
      .setKey(randomAlphanumeric(5))
      .setId(random.nextLong(100))
      .setName(randomAlphanumeric(5));
  }

  private ProjectDto getProjectDto(Projects.CreateWsResponse.Project result) {
    Optional<ProjectDto> projectDto = db.getDbClient().projectDao().selectProjectByKey(db.getSession(), result.getKey());
    assertThat(projectDto).isPresent();
    return projectDto.orElseThrow();
  }

}
