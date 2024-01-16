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
package org.sonar.server.almsettings.ws;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.auth.github.AppInstallationToken;
import org.sonar.auth.github.client.GithubApplicationClient;
import org.sonar.auth.github.GsonRepositoryCollaborator;
import org.sonar.auth.github.GsonRepositoryTeam;
import org.sonar.auth.github.security.AccessToken;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.web.UserRole;
import org.sonar.alm.client.github.GithubPermissionConverter;
import org.sonar.auth.github.GsonRepositoryPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.provisioning.GithubPermissionsMappingDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.component.ComponentCreationParameters;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.NewComponent;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionServiceImpl;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.UserPermissionChange;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.ws.ProjectCreator;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.project.CreationMethod.ALM_IMPORT_API;
import static org.sonar.db.project.CreationMethod.SCANNER_API_DEVOPS_AUTO_CONFIG;

@RunWith(MockitoJUnitRunner.class)
public class GithubProjectCreatorTest {

  private static final String ORGANIZATION_NAME = "orga2";
  private static final String REPOSITORY_NAME = "repo1";

  private static final String MAIN_BRANCH_NAME = "defaultBranch";
  private static final DevOpsProjectDescriptor DEVOPS_PROJECT_DESCRIPTOR = new DevOpsProjectDescriptor(ALM.GITHUB, "http://api.com", ORGANIZATION_NAME + "/" + REPOSITORY_NAME);
  private static final String ALM_SETTING_KEY = "github_config_1";
  private static final String USER_LOGIN = "userLogin";
  private static final String USER_UUID = "userUuid";
  private static final String BRANCH_UUID = "branchUuid1";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock
  private GithubApplicationClient githubApplicationClient;
  @Mock
  private GithubPermissionConverter githubPermissionConverter;
  @Mock
  private ProjectKeyGenerator projectKeyGenerator;
  @Mock
  private ComponentUpdater componentUpdater;
  @Mock
  private GithubProjectCreationParameters githubProjectCreationParameters;
  @Mock
  private AccessToken devOpsAppInstallationToken;
  @Mock
  private AppInstallationToken authAppInstallationToken;
  @Mock
  private UserSession userSession;
  @Mock
  private AlmSettingDto almSettingDto;
  private final PermissionService permissionService = new PermissionServiceImpl(mock());
  @Mock
  private PermissionUpdater<UserPermissionChange> permissionUpdater;
  @Mock
  private ManagedProjectService managedProjectService;
  @Mock
  private ManagedInstanceService managedInstanceService;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ProjectDefaultVisibility projectDefaultVisibility;
  private ProjectCreator projectCreator;

  private GithubProjectCreator githubProjectCreator;

  @Captor
  ArgumentCaptor<ComponentCreationParameters> componentCreationParametersCaptor;
  @Captor
  ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingDtoCaptor;

  @Before
  public void setup() {
    when(userSession.getLogin()).thenReturn(USER_LOGIN);
    when(userSession.getUuid()).thenReturn(USER_UUID);

    when(almSettingDto.getUrl()).thenReturn(DEVOPS_PROJECT_DESCRIPTOR.url());
    when(almSettingDto.getKey()).thenReturn(ALM_SETTING_KEY);

    when(githubProjectCreationParameters.devOpsProjectDescriptor()).thenReturn(DEVOPS_PROJECT_DESCRIPTOR);
    when(githubProjectCreationParameters.userSession()).thenReturn(userSession);
    when(githubProjectCreationParameters.devOpsAppInstallationToken()).thenReturn(devOpsAppInstallationToken);
    when(githubProjectCreationParameters.authAppInstallationToken()).thenReturn(authAppInstallationToken);
    when(githubProjectCreationParameters.almSettingDto()).thenReturn(almSettingDto);

    projectCreator = new ProjectCreator(userSession, projectDefaultVisibility, managedInstanceService, componentUpdater);
    githubProjectCreator = new GithubProjectCreator(dbClient, githubApplicationClient, githubPermissionConverter, projectKeyGenerator,
      permissionUpdater, permissionService, managedProjectService, projectCreator, githubProjectCreationParameters);

  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenNoAuthToken_throws() {
    when(githubProjectCreationParameters.authAppInstallationToken()).thenReturn(null);

    assertThatIllegalStateException().isThrownBy(() -> githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform())
      .withMessage("An auth app token is required in case repository permissions checking is necessary.");
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenUserIsNotAGitHubUser_returnsFalse() {
    assertThat(githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform()).isFalse();
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenCollaboratorHasDirectAccessButNoScanPermissions_returnsFalse() {
    GsonRepositoryCollaborator collaborator1 = mockCollaborator("collaborator1", 1, "role1", "read", "admin");
    mockGithubCollaboratorsFromApi(collaborator1);
    bindSessionToCollaborator(collaborator1);

    assertThat(githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform()).isFalse();
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenCollaboratorHasDirectAccess_returnsTrue() {
    GsonRepositoryCollaborator collaborator1 = mockCollaborator("collaborator1", 1, "role1", "read", "admin");
    GsonRepositoryCollaborator collaborator2 = mockCollaborator("collaborator2", 2, "role2", "read", "scan");
    mockGithubCollaboratorsFromApi(collaborator1, collaborator2);
    bindSessionToCollaborator(collaborator2);

    assertThat(githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform()).isTrue();
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenAccessViaTeamButNoScanPermissions_returnsFalse() {
    GsonRepositoryTeam team2 = mockGithubTeam("team2", 2, "role2", "another_perm", UserRole.ADMIN);
    mockTeamsFromApi(team2);
    bindGroupsToUser(team2.name());

    assertThat(githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform()).isFalse();
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenAccessViaTeam_returnsTrue() {
    GsonRepositoryTeam team1 = mockGithubTeam("team1", 1, "role1", "read", "another_perm");
    GsonRepositoryTeam team2 = mockGithubTeam("team2", 2, "role2", "another_perm", UserRole.SCAN);
    mockTeamsFromApi(team1, team2);
    bindGroupsToUser(team1.name(), team2.name());

    assertThat(githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform()).isTrue();
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenAccessViaTeamButUserNotInTeam_returnsFalse() {
    GsonRepositoryTeam team1 = mockGithubTeam("team1", 1, "role1", "read", "another_perm");
    GsonRepositoryTeam team2 = mockGithubTeam("team2", 2, "role2", "another_perm", UserRole.SCAN);
    mockTeamsFromApi(team1, team2);
    bindGroupsToUser(team1.name());

    assertThat(githubProjectCreator.isScanAllowedUsingPermissionsFromDevopsPlatform()).isFalse();
  }

  private void bindSessionToCollaborator(GsonRepositoryCollaborator collaborator1) {
    UserSession.ExternalIdentity externalIdentity = new UserSession.ExternalIdentity(String.valueOf(collaborator1.id()), collaborator1.name());
    when(userSession.getExternalIdentity()).thenReturn(Optional.of(externalIdentity));
  }

  private GsonRepositoryCollaborator mockCollaborator(String collaboratorLogin, int id, String role1, String... sqPermissions) {
    GsonRepositoryCollaborator collaborator = new GsonRepositoryCollaborator(collaboratorLogin, id, role1,
      new GsonRepositoryPermissions(false, false, false, false, false));
    mockPermissionsConversion(collaborator, sqPermissions);
    return collaborator;
  }

  private void mockGithubCollaboratorsFromApi(GsonRepositoryCollaborator... repositoryCollaborators) {
    Set<GsonRepositoryCollaborator> collaborators = Arrays.stream(repositoryCollaborators).collect(toSet());
    when(githubApplicationClient.getRepositoryCollaborators(DEVOPS_PROJECT_DESCRIPTOR.url(), authAppInstallationToken, ORGANIZATION_NAME, REPOSITORY_NAME)).thenReturn(
      collaborators);
  }

  private GsonRepositoryTeam mockGithubTeam(String name, int id, String role, String... sqPermissions) {
    GsonRepositoryTeam gsonRepositoryTeam = new GsonRepositoryTeam(name, id, name + "slug", role, new GsonRepositoryPermissions(false, false, false, false, false));
    mockPermissionsConversion(gsonRepositoryTeam, sqPermissions);
    return gsonRepositoryTeam;
  }

  private void mockTeamsFromApi(GsonRepositoryTeam... repositoryTeams) {
    when(githubApplicationClient.getRepositoryTeams(DEVOPS_PROJECT_DESCRIPTOR.url(), authAppInstallationToken, ORGANIZATION_NAME, REPOSITORY_NAME))
      .thenReturn(Arrays.stream(repositoryTeams).collect(toSet()));
  }

  private void mockPermissionsConversion(GsonRepositoryCollaborator collaborator, String... sqPermissions) {
    Set<GithubPermissionsMappingDto> githubPermissionsMappingDtos = mockPermissionsMappingsDtos();
    when(githubPermissionConverter.toSonarqubeRolesWithFallbackOnRepositoryPermissions(githubPermissionsMappingDtos, collaborator.roleName(), collaborator.permissions()))
      .thenReturn(Arrays.stream(sqPermissions).collect(toSet()));
  }

  private void mockPermissionsConversion(GsonRepositoryTeam team, String... sqPermissions) {
    Set<GithubPermissionsMappingDto> githubPermissionsMappingDtos = mockPermissionsMappingsDtos();
    when(githubPermissionConverter.toSonarqubeRolesWithFallbackOnRepositoryPermissions(githubPermissionsMappingDtos, team.permission(), team.permissions()))
      .thenReturn(Arrays.stream(sqPermissions).collect(toSet()));
  }

  private Set<GithubPermissionsMappingDto> mockPermissionsMappingsDtos() {
    Set<GithubPermissionsMappingDto> githubPermissionsMappingDtos = Set.of(mock(GithubPermissionsMappingDto.class));
    when(dbClient.githubPermissionsMappingDao().findAll(any())).thenReturn(githubPermissionsMappingDtos);
    return githubPermissionsMappingDtos;
  }

  private void bindGroupsToUser(String... groupNames) {
    Set<GroupDto> groupDtos = Arrays.stream(groupNames)
      .map(groupName -> new GroupDto().setName(ORGANIZATION_NAME + "/" + groupName).setUuid("uuid_" + groupName))
      .collect(toSet());
    when(userSession.getGroups()).thenReturn(groupDtos);
  }

  @Test
  public void createProjectAndBindToDevOpsPlatform_whenRepoNotFound_throws() {
    assertThatIllegalStateException().isThrownBy(
        () -> githubProjectCreator.createProjectAndBindToDevOpsPlatform(mock(), SCANNER_API_DEVOPS_AUTO_CONFIG, null))
      .withMessage("Impossible to find the repository 'orga2/repo1' on GitHub, using the devops config " + ALM_SETTING_KEY);
  }

  @Test
  public void createProjectAndBindToDevOpsPlatformFromScanner_whenRepoFoundOnGitHub_successfullyCreatesProject() {
    // given
    mockGitHubRepository();

    ComponentCreationData componentCreationData = mockProjectCreation("generated_orga2/repo1");
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

    // when
    ComponentCreationData actualComponentCreationData = githubProjectCreator.createProjectAndBindToDevOpsPlatform(dbClient.openSession(true),
      SCANNER_API_DEVOPS_AUTO_CONFIG, null);

    // then
    assertThat(actualComponentCreationData).isEqualTo(componentCreationData);

    ComponentCreationParameters componentCreationParameters = componentCreationParametersCaptor.getValue();
    assertComponentCreationParametersContainsCorrectInformation(componentCreationParameters, "generated_orga2/repo1", SCANNER_API_DEVOPS_AUTO_CONFIG);
    assertThat(componentCreationParameters.isManaged()).isFalse();
    assertThat(componentCreationParameters.newComponent().isPrivate()).isTrue();

    verify(projectAlmSettingDao).insertOrUpdate(any(), projectAlmSettingDtoCaptor.capture(), eq(ALM_SETTING_KEY), eq(REPOSITORY_NAME), eq("generated_orga2/repo1"));
    ProjectAlmSettingDto projectAlmSettingDto = projectAlmSettingDtoCaptor.getValue();
    assertAlmSettingsDtoContainsCorrectInformation(almSettingDto, requireNonNull(componentCreationData.projectDto()), projectAlmSettingDto);
  }

  @Test
  public void createProjectAndBindToDevOpsPlatformFromScanner_whenRepoFoundOnGitHubAndVisibilitySynchronizationEnabled_successfullyCreatesProjectAndSetsVisibility() {
    // given
    mockPublicGithubRepository();

    ComponentCreationData componentCreationData = mockProjectCreation("generated_orga2/repo1");
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
    when(managedProjectService.isProjectVisibilitySynchronizationActivated()).thenReturn(true);

    // when
    ComponentCreationData actualComponentCreationData = githubProjectCreator.createProjectAndBindToDevOpsPlatform(dbClient.openSession(true),
      SCANNER_API_DEVOPS_AUTO_CONFIG, null);

    // then
    assertThat(actualComponentCreationData).isEqualTo(componentCreationData);

    ComponentCreationParameters componentCreationParameters = componentCreationParametersCaptor.getValue();
    assertThat(componentCreationParameters.newComponent().isPrivate()).isFalse();
  }

  @Test
  public void createProjectAndBindToDevOpsPlatformFromScanner_whenRepoFoundOnGitHubAndVisibilitySynchronizationDisabled_successfullyCreatesProjectAndMakesProjectPrivate() {
    // given
    mockGitHubRepository();

    ComponentCreationData componentCreationData = mockProjectCreation("generated_orga2/repo1");
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
    when(managedProjectService.isProjectVisibilitySynchronizationActivated()).thenReturn(false);

    // when
    ComponentCreationData actualComponentCreationData = githubProjectCreator.createProjectAndBindToDevOpsPlatform(dbClient.openSession(true),
      SCANNER_API_DEVOPS_AUTO_CONFIG, null);

    // then
    assertThat(actualComponentCreationData).isEqualTo(componentCreationData);

    ComponentCreationParameters componentCreationParameters = componentCreationParametersCaptor.getValue();
    assertThat(componentCreationParameters.newComponent().isPrivate()).isTrue();
  }

  @Test
  public void createProjectAndBindToDevOpsPlatformFromApi_whenRepoFoundOnGitHub_successfullyCreatesProject() {
    // given
    String projectKey = "customProjectKey";
    mockGitHubRepository();

    ComponentCreationData componentCreationData = mockProjectCreation(projectKey);
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

    // when
    ComponentCreationData actualComponentCreationData = githubProjectCreator.createProjectAndBindToDevOpsPlatform(dbClient.openSession(true), ALM_IMPORT_API, projectKey);

    // then
    assertThat(actualComponentCreationData).isEqualTo(componentCreationData);

    ComponentCreationParameters componentCreationParameters = componentCreationParametersCaptor.getValue();
    assertComponentCreationParametersContainsCorrectInformation(componentCreationParameters, projectKey, ALM_IMPORT_API);
    assertThat(componentCreationParameters.isManaged()).isFalse();
    assertThat(componentCreationParameters.newComponent().isPrivate()).isTrue();

    verify(projectAlmSettingDao).insertOrUpdate(any(), projectAlmSettingDtoCaptor.capture(), eq(ALM_SETTING_KEY), eq(REPOSITORY_NAME), eq(projectKey));
    ProjectAlmSettingDto projectAlmSettingDto = projectAlmSettingDtoCaptor.getValue();
    assertAlmSettingsDtoContainsCorrectInformation(almSettingDto, requireNonNull(componentCreationData.projectDto()), projectAlmSettingDto);
  }

  @Captor
  private ArgumentCaptor<Collection<UserPermissionChange>> permissionChangesCaptor;

  @Test
  public void createProjectAndBindToDevOpsPlatformFromApi_whenRepoFoundOnGitHubAutoProvisioningOnAndRepoPrivate_successfullyCreatesProject() {
    // given
    when(projectDefaultVisibility.get(any()).isPrivate()).thenReturn(true);
    when(managedInstanceService.isInstanceExternallyManaged()).thenReturn(true);

    String projectKey = "customProjectKey";
    mockGitHubRepository();

    ComponentCreationData componentCreationData = mockProjectCreation(projectKey);
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

    // when
    ComponentCreationData actualComponentCreationData = githubProjectCreator.createProjectAndBindToDevOpsPlatform(dbClient.openSession(true), ALM_IMPORT_API, projectKey);

    // then
    assertThat(actualComponentCreationData).isEqualTo(componentCreationData);

    ComponentCreationParameters componentCreationParameters = componentCreationParametersCaptor.getValue();
    assertComponentCreationParametersContainsCorrectInformation(componentCreationParameters, projectKey, ALM_IMPORT_API);
    assertThat(componentCreationParameters.isManaged()).isTrue();
    assertThat(componentCreationParameters.newComponent().isPrivate()).isTrue();

    verifyScanPermissionWasAddedToUser(actualComponentCreationData);
    verifyProjectSyncTaskWasCreated(actualComponentCreationData);

    verify(projectAlmSettingDao).insertOrUpdate(any(), projectAlmSettingDtoCaptor.capture(), eq(ALM_SETTING_KEY), eq(REPOSITORY_NAME), eq(projectKey));
    ProjectAlmSettingDto projectAlmSettingDto = projectAlmSettingDtoCaptor.getValue();
    assertAlmSettingsDtoContainsCorrectInformation(almSettingDto, requireNonNull(componentCreationData.projectDto()), projectAlmSettingDto);
  }

  private void verifyProjectSyncTaskWasCreated(ComponentCreationData componentCreationData) {
    String projectUuid = requireNonNull(componentCreationData.projectDto()).getUuid();
    String mainBranchUuid = requireNonNull(componentCreationData.mainBranchDto()).getUuid();
    verify(managedProjectService).queuePermissionSyncTask(USER_UUID, mainBranchUuid, projectUuid);
  }

  private void verifyScanPermissionWasAddedToUser(ComponentCreationData actualComponentCreationData) {
    verify(permissionUpdater).apply(any(), permissionChangesCaptor.capture());
    UserPermissionChange permissionChange = permissionChangesCaptor.getValue().iterator().next();
    assertThat(permissionChange.getUserId().getUuid()).isEqualTo(userSession.getUuid());
    assertThat(permissionChange.getUserId().getLogin()).isEqualTo(userSession.getLogin());
    assertThat(permissionChange.getPermission()).isEqualTo(UserRole.SCAN);
    assertThat(permissionChange.getProjectUuid()).isEqualTo(actualComponentCreationData.projectDto().getUuid());
  }

  private void mockPublicGithubRepository() {
    GithubApplicationClient.Repository repository =mockGitHubRepository();
    when(repository.isPrivate()).thenReturn(false);
  }

  private GithubApplicationClient.Repository mockGitHubRepository() {
    GithubApplicationClient.Repository repository = mock();
    when(repository.getDefaultBranch()).thenReturn(MAIN_BRANCH_NAME);
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getFullName()).thenReturn(DEVOPS_PROJECT_DESCRIPTOR.projectIdentifier());
    when(repository.isPrivate()).thenReturn(true);
    when(githubApplicationClient.getRepository(DEVOPS_PROJECT_DESCRIPTOR.url(), devOpsAppInstallationToken, DEVOPS_PROJECT_DESCRIPTOR.projectIdentifier())).thenReturn(
      Optional.of(repository));
    when(projectKeyGenerator.generateUniqueProjectKey(repository.getFullName())).thenReturn("generated_" + DEVOPS_PROJECT_DESCRIPTOR.projectIdentifier());
    return  repository;
  }



  private ComponentCreationData mockProjectCreation(String projectKey) {
    ComponentCreationData componentCreationData = mock();
    ProjectDto projectDto = mockProjectDto(projectKey);
    when(componentCreationData.projectDto()).thenReturn(projectDto);
    BranchDto branchDto = mock();
    when(branchDto.getUuid()).thenReturn(BRANCH_UUID);
    when(componentCreationData.mainBranchDto()).thenReturn(branchDto);
    when(componentUpdater.createWithoutCommit(any(), componentCreationParametersCaptor.capture())).thenReturn(componentCreationData);
    return componentCreationData;
  }

  private static ProjectDto mockProjectDto(String projectKey) {
    ProjectDto projectDto = mock();
    when(projectDto.getName()).thenReturn(REPOSITORY_NAME);
    when(projectDto.getKey()).thenReturn(projectKey);
    when(projectDto.getUuid()).thenReturn("project-uuid-1");
    return projectDto;
  }

  private static void assertComponentCreationParametersContainsCorrectInformation(ComponentCreationParameters componentCreationParameters, String expectedKey,
    CreationMethod expectedCreationMethod) {
    assertThat(componentCreationParameters.creationMethod()).isEqualTo(expectedCreationMethod);
    assertThat(componentCreationParameters.mainBranchName()).isEqualTo(MAIN_BRANCH_NAME);
    assertThat(componentCreationParameters.userLogin()).isEqualTo(USER_LOGIN);
    assertThat(componentCreationParameters.userUuid()).isEqualTo(USER_UUID);

    NewComponent newComponent = componentCreationParameters.newComponent();
    assertThat(newComponent.isProject()).isTrue();
    assertThat(newComponent.qualifier()).isEqualTo(Qualifiers.PROJECT);
    assertThat(newComponent.key()).isEqualTo(expectedKey);
    assertThat(newComponent.name()).isEqualTo(REPOSITORY_NAME);
  }

  private static void assertAlmSettingsDtoContainsCorrectInformation(AlmSettingDto almSettingDto, ProjectDto projectDto, ProjectAlmSettingDto projectAlmSettingDto) {
    assertThat(projectAlmSettingDto.getAlmRepo()).isEqualTo(DEVOPS_PROJECT_DESCRIPTOR.projectIdentifier());
    assertThat(projectAlmSettingDto.getAlmSlug()).isNull();
    assertThat(projectAlmSettingDto.getAlmSettingUuid()).isEqualTo(almSettingDto.getUuid());
    assertThat(projectAlmSettingDto.getProjectUuid()).isEqualTo(projectDto.getUuid());
    assertThat(projectAlmSettingDto.getMonorepo()).isFalse();
    assertThat(projectAlmSettingDto.getSummaryCommentEnabled()).isTrue();
  }
}
