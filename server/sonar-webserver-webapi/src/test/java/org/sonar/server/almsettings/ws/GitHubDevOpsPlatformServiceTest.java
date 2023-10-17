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
package org.sonar.server.almsettings.ws;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.alm.client.github.AppInstallationToken;
import org.sonar.alm.client.github.GithubApplicationClient;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.github.api.GsonRepositoryCollaborator;
import org.sonar.alm.client.github.api.GsonRepositoryTeam;
import org.sonar.alm.client.github.config.GithubAppConfiguration;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.web.UserRole;
import org.sonar.auth.github.GitHubSettings;
import org.sonar.auth.github.GithubPermissionConverter;
import org.sonar.auth.github.GsonRepositoryPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.provisioning.GithubPermissionsMappingDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.component.ComponentCreationParameters;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.NewComponent;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.almsettings.ws.GitHubDevOpsPlatformService.DEVOPS_PLATFORM_PROJECT_IDENTIFIER;
import static org.sonar.server.almsettings.ws.GitHubDevOpsPlatformService.DEVOPS_PLATFORM_URL;

@RunWith(MockitoJUnitRunner.class)
public class GitHubDevOpsPlatformServiceTest {
  @Rule
  public LogTester logTester = new LogTester().setLevel(LoggerLevel.WARN);

  private static final DevOpsProjectDescriptor GITHUB_PROJECT_DESCRIPTOR = new DevOpsProjectDescriptor(ALM.GITHUB, "url", "repo");
  private static final long APP_INSTALLATION_ID = 534534534543L;
  private static final String USER_LOGIN = "user-login-1";
  private static final String USER_UUID = "user-uuid-1";
  private static final String PROJECT_KEY = "projectKey";
  private static final String PROJECT_NAME = "projectName";
  private static final String MAIN_BRANCH_NAME = "defaultBranch";
  private static final String ORGANIZATION_NAME = "orgname";
  private static final String GITHUB_REPO_FULL_NAME = ORGANIZATION_NAME + "/" + PROJECT_NAME;
  private static final String GITHUB_API_URL = "https://api.toto.com";
  private static final DevOpsProjectDescriptor DEV_OPS_PROJECT_DESCRIPTOR = new DevOpsProjectDescriptor(ALM.GITHUB, GITHUB_API_URL, GITHUB_REPO_FULL_NAME);

  @Mock
  private DbSession dbSession;
  @Mock
  private GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  @Mock
  private GithubApplicationClient githubApplicationClient;

  @Mock
  private ComponentUpdater componentUpdater;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock
  private UserSession userSession;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ProjectDefaultVisibility projectDefaultVisibility;

  @Mock
  private ProjectKeyGenerator projectKeyGenerator;

  @Mock
  private GitHubSettings gitHubSettings;

  @Mock
  private GithubPermissionConverter githubPermissionConverter;

  @InjectMocks
  private GitHubDevOpsPlatformService gitHubDevOpsPlatformService;

  @Captor
  ArgumentCaptor<ComponentCreationParameters> componentCreationParametersCaptor;
  @Captor
  ArgumentCaptor<ProjectAlmSettingDto> projectAlmSettingDtoCaptor;

  @Before
  public void setup() {
    when(userSession.getLogin()).thenReturn(USER_LOGIN);
    when(userSession.getUuid()).thenReturn(USER_UUID);
  }

  @Test
  public void getDevOpsPlatform_shouldReturnGitHub() {
    assertThat(gitHubDevOpsPlatformService.getDevOpsPlatform())
      .isEqualTo(ALM.GITHUB);
  }

  @Test
  public void getDevOpsProjectDescriptor_whenNoCharacteristics_shouldReturnEmpty() {
    Optional<DevOpsProjectDescriptor> devOpsProjectDescriptor = gitHubDevOpsPlatformService.getDevOpsProjectDescriptor(Map.of());

    assertThat(devOpsProjectDescriptor).isEmpty();
  }

  @Test
  public void getDevOpsProjectDescriptor_whenValidCharacteristics_shouldReturn() {
    Optional<DevOpsProjectDescriptor> devOpsProjectDescriptor = gitHubDevOpsPlatformService.getDevOpsProjectDescriptor(Map.of(
      DEVOPS_PLATFORM_URL, GITHUB_PROJECT_DESCRIPTOR.url(),
      DEVOPS_PLATFORM_PROJECT_IDENTIFIER, GITHUB_PROJECT_DESCRIPTOR.projectIdentifier()
    ));

    assertThat(devOpsProjectDescriptor)
      .isPresent()
      .get().usingRecursiveComparison().isEqualTo(GITHUB_PROJECT_DESCRIPTOR);
  }

  @Test
  public void getValidAlmSettingDto_whenNoAlmSetting_shouldReturnEmpty() {
    Optional<AlmSettingDto> almSettingDto = gitHubDevOpsPlatformService.getValidAlmSettingDto(dbSession, GITHUB_PROJECT_DESCRIPTOR);

    assertThat(almSettingDto).isEmpty();
  }

  @Test
  public void getValidAlmSettingDto_whenMultipleAlmSetting_shouldReturnTheRightOne() {
    AlmSettingDto mockGitHubAlmSettingDtoNoAccess = mockGitHubAlmSettingDto(false);
    AlmSettingDto mockGitHubAlmSettingDtoAccess = mockGitHubAlmSettingDto(true);
    when(dbClient.almSettingDao().selectByAlm(dbSession, ALM.GITHUB)).thenReturn(List.of(mockGitHubAlmSettingDtoNoAccess, mockGitHubAlmSettingDtoAccess));

    Optional<AlmSettingDto> almSettingDto = gitHubDevOpsPlatformService.getValidAlmSettingDto(dbSession, GITHUB_PROJECT_DESCRIPTOR);

    assertThat(almSettingDto)
      .isPresent()
      .get().isEqualTo(mockGitHubAlmSettingDtoAccess);
  }

  private AlmSettingDto mockGitHubAlmSettingDto(boolean repoAccess) {
    AlmSettingDto mockAlmSettingDto = mock();
    when(mockAlmSettingDto.getUrl()).thenReturn(GITHUB_PROJECT_DESCRIPTOR.url());
    GithubAppConfiguration mockGithubAppConfiguration = mock(GithubAppConfiguration.class);
    when(githubGlobalSettingsValidator.validate(mockAlmSettingDto)).thenReturn(mockGithubAppConfiguration);
    when(githubApplicationClient.getInstallationId(eq(mockGithubAppConfiguration), any())).thenReturn(repoAccess ? Optional.of(1L) : Optional.empty());
    return mockAlmSettingDto;
  }

  @Test
  public void createProjectAndBindToDevOpsPlatform_whenRepoNotFound_throws() {

    AlmSettingDto almSettingDto = mockAlmSettingDto();
    GithubAppConfiguration githubAppConfiguration = mockGitHubAppConfiguration(almSettingDto);
    when(githubApplicationClient.getInstallationId(githubAppConfiguration, GITHUB_REPO_FULL_NAME)).thenReturn(Optional.empty());

    assertThatIllegalStateException().isThrownBy(
        () -> gitHubDevOpsPlatformService.createProjectAndBindToDevOpsPlatform(dbSession, PROJECT_KEY, almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR))
      .withMessage("Impossible to find the repository orgname/projectName on GitHub, using the devops config devops-platform-config-1.");
  }

  @Test
  public void createProjectAndBindToDevOpsPlatform_whenRepoFoundOnGitHub_successfullyCreatesProject() {
    // given
    AlmSettingDto almSettingDto = mockAlmSettingDto();
    mockExistingGitHubRepository(almSettingDto);

    ComponentCreationData componentCreationData = mockProjectCreation();
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

    // when
    ComponentCreationData actualComponentCreationData = gitHubDevOpsPlatformService.createProjectAndBindToDevOpsPlatform(dbSession, PROJECT_KEY, almSettingDto,
      DEV_OPS_PROJECT_DESCRIPTOR);

    // then
    assertThat(actualComponentCreationData).isEqualTo(componentCreationData);

    ComponentCreationParameters componentCreationParameters = componentCreationParametersCaptor.getValue();
    assertComponentCreationParametersContainsCorrectInformation(componentCreationParameters);
    assertThat(componentCreationParameters.isManaged()).isFalse();
    assertThat(componentCreationParameters.newComponent().isPrivate()).isFalse();

    verify(projectAlmSettingDao).insertOrUpdate(eq(dbSession), projectAlmSettingDtoCaptor.capture(), eq("devops-platform-config-1"), eq(PROJECT_NAME), eq(PROJECT_KEY));
    ProjectAlmSettingDto projectAlmSettingDto = projectAlmSettingDtoCaptor.getValue();
    assertAlmSettingsDtoContainsCorrectInformation(almSettingDto, requireNonNull(componentCreationData.projectDto()), projectAlmSettingDto);

    assertThat(logTester.getLogs()).isEmpty();
  }

  @Test
  public void createProjectAndBindToDevOpsPlatform_whenRepoFoundOnGitHubAndAutoProvisioningOn_successfullyCreatesProject() {
    // given
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PRIVATE);
    when(gitHubSettings.isProvisioningEnabled()).thenReturn(true);

    AlmSettingDto almSettingDto = mockAlmSettingDto();
    mockExistingGitHubRepository(almSettingDto);

    ComponentCreationData componentCreationData = mockProjectCreation();
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

    // when
    ComponentCreationData actualComponentCreationData = gitHubDevOpsPlatformService.createProjectAndBindToDevOpsPlatform(dbSession, PROJECT_KEY, almSettingDto,
      DEV_OPS_PROJECT_DESCRIPTOR);

    // then
    assertThat(actualComponentCreationData).isEqualTo(componentCreationData);

    ComponentCreationParameters componentCreationParameters = componentCreationParametersCaptor.getValue();
    assertComponentCreationParametersContainsCorrectInformation(componentCreationParameters);
    assertThat(componentCreationParameters.isManaged()).isTrue();
    assertThat(componentCreationParameters.newComponent().isPrivate()).isTrue();

    verify(projectAlmSettingDao).insertOrUpdate(eq(dbSession), projectAlmSettingDtoCaptor.capture(), eq("devops-platform-config-1"), eq(PROJECT_NAME), eq(PROJECT_KEY));
    ProjectAlmSettingDto projectAlmSettingDto = projectAlmSettingDtoCaptor.getValue();
    assertAlmSettingsDtoContainsCorrectInformation(almSettingDto, requireNonNull(componentCreationData.projectDto()), projectAlmSettingDto);

    assertThat(logTester.getLogs()).isEmpty();
  }

  @Test
  public void createProjectAndBindToDevOpsPlatform_whenWrongToken_throws() {
    AlmSettingDto almSettingDto = mockAlmSettingDto();
    mockExistingGitHubRepository(almSettingDto);

    when(githubApplicationClient.createAppInstallationToken(any(), anyLong())).thenReturn(Optional.empty());

    assertThatIllegalStateException().isThrownBy(
        () -> gitHubDevOpsPlatformService.createProjectAndBindToDevOpsPlatform(dbSession, PROJECT_KEY, almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR))
      .withMessage("Error while generating token for GitHub Api Url https://api.toto.com (installation id: 534534534543)");
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenUserIsCollaboratorWithScan_returnsTrue() {
    // given
    AlmSettingDto almSettingDto = mockAlmSettingDto();

    GsonRepositoryCollaborator collaborator1 = mockCollaborator("collaborator1", 1, "role1");
    GsonRepositoryCollaborator collaborator2 = mockCollaborator("collaborator2", 2, "role2");
    mockGithubCollaboratorsFromApi(almSettingDto, collaborator1, collaborator2);
    bindSessionToCollaborator(collaborator2);

    mockPermissionsConversion(collaborator2, "another_perm", UserRole.SCAN);

    // when
    boolean isGranted = gitHubDevOpsPlatformService.isScanAllowedUsingPermissionsFromDevopsPlatform(almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR);

    // then
    assertThat(isGranted).isTrue();
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenUserIsCollaboratorButWithoutScan_returnsFalse() {
    // given
    AlmSettingDto almSettingDto = mockAlmSettingDto();

    GsonRepositoryCollaborator collaborator1 = mockCollaborator("collaborator1", 1, "role1");
    mockGithubCollaboratorsFromApi(almSettingDto, collaborator1);
    bindSessionToCollaborator(collaborator1);

    mockPermissionsConversion(collaborator1, "admin");

    // when
    boolean isGranted = gitHubDevOpsPlatformService.isScanAllowedUsingPermissionsFromDevopsPlatform(almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR);

    // then
    assertThat(isGranted).isFalse();
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenRepoFoundOnGitHubAndUserIsCollaboratorButWithoutScan_returnsFalse() {
    // given
    AlmSettingDto almSettingDto = mockAlmSettingDto();

    GsonRepositoryCollaborator collaborator1 = mockCollaborator("collaborator1", 1, "role1");
    mockGithubCollaboratorsFromApi(almSettingDto, collaborator1);
    bindSessionToCollaborator(collaborator1);

    mockPermissionsConversion(collaborator1, "admin");

    // when
    boolean isGranted = gitHubDevOpsPlatformService.isScanAllowedUsingPermissionsFromDevopsPlatform(almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR);

    // then
    assertThat(isGranted).isFalse();
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenRepoFoundOnGitHubAndUserIsNotExternal_returnsFalse() {
    // given
    AlmSettingDto almSettingDto = mockAlmSettingDto();

    GsonRepositoryCollaborator collaborator1 = mockCollaborator("collaborator1", 1, "role1");
    mockGithubCollaboratorsFromApi(almSettingDto, collaborator1);
    mockPermissionsConversion(collaborator1, "admin");

    // when
    boolean isGranted = gitHubDevOpsPlatformService.isScanAllowedUsingPermissionsFromDevopsPlatform(almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR);

    // then
    assertThat(isGranted).isFalse();
  }

  private void mockPermissionsConversion(GsonRepositoryCollaborator collaborator, String... sqPermissions) {
    Set<GithubPermissionsMappingDto> githubPermissionsMappingDtos = mockPermissionsMappingsDtos();
    when(githubPermissionConverter.toSonarqubeRolesWithFallbackOnRepositoryPermissions(githubPermissionsMappingDtos, collaborator.roleName(), collaborator.permissions()))
      .thenReturn(Arrays.stream(sqPermissions).collect(toSet()));
  }

  private void bindSessionToCollaborator(GsonRepositoryCollaborator collaborator1) {
    UserSession.ExternalIdentity externalIdentity = new UserSession.ExternalIdentity(String.valueOf(collaborator1.id()), collaborator1.name());
    when(userSession.getExternalIdentity()).thenReturn(Optional.of(externalIdentity));
  }

  private static GsonRepositoryCollaborator mockCollaborator(String collaborator1, int id, String role1) {
    return new GsonRepositoryCollaborator(collaborator1, id, role1,
      new GsonRepositoryPermissions(false, false, false, false, false));
  }

  private void mockGithubCollaboratorsFromApi(AlmSettingDto almSettingDto, GsonRepositoryCollaborator... repositoryCollaborators) {
    GithubAppConfiguration githubAppConfiguration = mockGitHubAppConfiguration(almSettingDto);
    when(githubApplicationClient.getInstallationId(githubAppConfiguration, GITHUB_REPO_FULL_NAME)).thenReturn(Optional.of(APP_INSTALLATION_ID));
    AppInstallationToken appInstallationToken = mockAppInstallationToken(githubAppConfiguration, APP_INSTALLATION_ID);
    when(githubApplicationClient.getRepositoryCollaborators(GITHUB_API_URL, appInstallationToken, ORGANIZATION_NAME, PROJECT_NAME))
      .thenReturn(Arrays.stream(repositoryCollaborators).collect(toSet()));
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenInstallationIdNotFound_throws() {
    // given
    AlmSettingDto almSettingDto = mockAlmSettingDto();

    when(githubApplicationClient.getInstallationId(any(), any())).thenReturn(Optional.empty());

    // when
    assertThatIllegalStateException()
      .isThrownBy(() -> gitHubDevOpsPlatformService.isScanAllowedUsingPermissionsFromDevopsPlatform(almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR))
        .withMessage("Impossible to find the repository orgname/projectName on GitHub, using the devops config devops-platform-config-1.");
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenUserIsMemberOfAGroupWithScan_returnsTrue() {
    // given
    AlmSettingDto almSettingDto = mockAlmSettingDto();

    GsonRepositoryTeam team1 = mockGithubTeam("team1", 1, "role1");
    GsonRepositoryTeam team2 = mockGithubTeam("team2", 2, "role2");
    mockTeamsFromApi(almSettingDto, team1, team2);
    bindGroupsToUser(team1.name(), team2.name());

    mockPermissionsConversion(team1, "another_perm", UserRole.SCAN);

    // when
    boolean isGranted = gitHubDevOpsPlatformService.isScanAllowedUsingPermissionsFromDevopsPlatform(almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR);

    // then
    assertThat(isGranted).isTrue();
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenUserIsMemberOfAGroupWithoutScanPermission_returnsFalse() {
    // given
    AlmSettingDto almSettingDto = mockAlmSettingDto();

    GsonRepositoryTeam team1 = mockGithubTeam("team1", 1, "role1");
    mockTeamsFromApi(almSettingDto, team1);
    bindGroupsToUser(team1.name(), "another_local_team");

    mockPermissionsConversion(team1, "another_perm", "admin");

    // when
    boolean isGranted = gitHubDevOpsPlatformService.isScanAllowedUsingPermissionsFromDevopsPlatform(almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR);

    // then
    assertThat(isGranted).isFalse();
  }

  @Test
  public void isScanAllowedUsingPermissionsFromDevopsPlatform_whenUserIsNotMemberOfAGroupWithScanPermission_returnsFalse() {
    // given
    AlmSettingDto almSettingDto = mockAlmSettingDto();

    GsonRepositoryTeam team1 = mockGithubTeam("team1", 1, "role1");
    mockTeamsFromApi(almSettingDto, team1);
    bindGroupsToUser("another_local_team");

    mockPermissionsConversion(team1, "another_perm", UserRole.SCAN);

    // when
    boolean isGranted = gitHubDevOpsPlatformService.isScanAllowedUsingPermissionsFromDevopsPlatform(almSettingDto, DEV_OPS_PROJECT_DESCRIPTOR);

    // then
    assertThat(isGranted).isFalse();
  }

  private void bindGroupsToUser(String... groupNames) {
    Set<GroupDto> groupDtos = Arrays.stream(groupNames)
      .map(groupName -> new GroupDto().setName(ORGANIZATION_NAME + "/" + groupName).setUuid("uuid_" + groupName))
      .collect(toSet());
    when(userSession.getGroups()).thenReturn(groupDtos);
  }

  private static GsonRepositoryTeam mockGithubTeam(String name, int id, String role) {
    return new GsonRepositoryTeam(name, id, name + "slug", role, new GsonRepositoryPermissions(false, false, false, false, false));
  }

  private void mockTeamsFromApi(AlmSettingDto almSettingDto, GsonRepositoryTeam... repositoryTeams) {
    GithubAppConfiguration githubAppConfiguration = mockGitHubAppConfiguration(almSettingDto);
    when(githubApplicationClient.getInstallationId(githubAppConfiguration, GITHUB_REPO_FULL_NAME)).thenReturn(Optional.of(APP_INSTALLATION_ID));
    AppInstallationToken appInstallationToken = mockAppInstallationToken(githubAppConfiguration, APP_INSTALLATION_ID);
    when(githubApplicationClient.getRepositoryTeams(GITHUB_API_URL, appInstallationToken, ORGANIZATION_NAME, PROJECT_NAME))
      .thenReturn(Arrays.stream(repositoryTeams).collect(toSet()));
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

  private void mockExistingGitHubRepository(AlmSettingDto almSettingDto) {
    GithubAppConfiguration githubAppConfiguration = mockGitHubAppConfiguration(almSettingDto);
    when(githubApplicationClient.getInstallationId(githubAppConfiguration, GITHUB_REPO_FULL_NAME)).thenReturn(Optional.of(APP_INSTALLATION_ID));
    AppInstallationToken appInstallationToken = mockAppInstallationToken(githubAppConfiguration, APP_INSTALLATION_ID);
    mockGitHubRepository(appInstallationToken);
  }

  private GithubAppConfiguration mockGitHubAppConfiguration(AlmSettingDto almSettingDto) {
    GithubAppConfiguration githubAppConfiguration = mock();
    when(githubGlobalSettingsValidator.validate(almSettingDto)).thenReturn(githubAppConfiguration);
    when(githubAppConfiguration.getApiEndpoint()).thenReturn(GITHUB_API_URL);
    return githubAppConfiguration;
  }

  private void mockGitHubRepository(AppInstallationToken appInstallationToken) {
    GithubApplicationClient.Repository repository = mock();
    when(repository.getDefaultBranch()).thenReturn(MAIN_BRANCH_NAME);
    when(repository.getName()).thenReturn(PROJECT_NAME);
    when(repository.getFullName()).thenReturn(GITHUB_REPO_FULL_NAME);
    when(githubApplicationClient.getRepository(GITHUB_API_URL, appInstallationToken, GITHUB_REPO_FULL_NAME)).thenReturn(Optional.of(repository));
    when(projectKeyGenerator.generateUniqueProjectKey(repository.getFullName())).thenReturn("generated_" + PROJECT_KEY);
  }

  private AppInstallationToken mockAppInstallationToken(GithubAppConfiguration githubAppConfiguration, long appInstallationId) {
    AppInstallationToken appInstallationToken = mock();
    when(githubApplicationClient.createAppInstallationToken(githubAppConfiguration, appInstallationId)).thenReturn(Optional.of(appInstallationToken));
    return appInstallationToken;
  }

  private static AlmSettingDto mockAlmSettingDto() {
    AlmSettingDto almSettingDto = mock();
    when(almSettingDto.getUuid()).thenReturn("almsetting-uuid-1");
    when(almSettingDto.getKey()).thenReturn("devops-platform-config-1");
    return almSettingDto;
  }

  private ComponentCreationData mockProjectCreation() {
    ComponentCreationData componentCreationData = mock();
    mockProjectDto(componentCreationData);
    when(componentUpdater.createWithoutCommit(eq(dbSession), componentCreationParametersCaptor.capture())).thenReturn(componentCreationData);
    return componentCreationData;
  }

  private static ProjectDto mockProjectDto(ComponentCreationData componentCreationData) {
    ProjectDto projectDto = mock();
    when(projectDto.getName()).thenReturn(PROJECT_NAME);
    when(projectDto.getKey()).thenReturn(PROJECT_KEY);
    when(projectDto.getUuid()).thenReturn("project-uuid-1");
    when(componentCreationData.projectDto()).thenReturn(projectDto);
    return projectDto;
  }

  private static void assertComponentCreationParametersContainsCorrectInformation(ComponentCreationParameters componentCreationParameters) {
    assertThat(componentCreationParameters.creationMethod()).isEqualTo(CreationMethod.SCANNER_API_DEVOPS_AUTO_CONFIG);
    assertThat(componentCreationParameters.mainBranchName()).isEqualTo(MAIN_BRANCH_NAME);
    assertThat(componentCreationParameters.userLogin()).isEqualTo(USER_LOGIN);
    assertThat(componentCreationParameters.userUuid()).isEqualTo(USER_UUID);

    NewComponent newComponent = componentCreationParameters.newComponent();
    assertThat(newComponent.isProject()).isTrue();
    assertThat(newComponent.qualifier()).isEqualTo(Qualifiers.PROJECT);
    assertThat(newComponent.key()).isEqualTo(PROJECT_KEY);
    assertThat(newComponent.name()).isEqualTo(PROJECT_NAME);
  }

  private static void assertAlmSettingsDtoContainsCorrectInformation(AlmSettingDto almSettingDto, ProjectDto projectDto, ProjectAlmSettingDto projectAlmSettingDto) {
    assertThat(projectAlmSettingDto.getAlmRepo()).isEqualTo(GITHUB_REPO_FULL_NAME);
    assertThat(projectAlmSettingDto.getAlmSlug()).isNull();
    assertThat(projectAlmSettingDto.getAlmSettingUuid()).isEqualTo(almSettingDto.getUuid());
    assertThat(projectAlmSettingDto.getProjectUuid()).isEqualTo(projectDto.getUuid());
    assertThat(projectAlmSettingDto.getMonorepo()).isFalse();
    assertThat(projectAlmSettingDto.getSummaryCommentEnabled()).isTrue();
  }
}
