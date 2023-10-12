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

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.sonar.alm.client.github.config.GithubAppConfiguration;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.auth.github.GitHubSettings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.component.ComponentCreationParameters;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.NewComponent;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;
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
    DevOpsProjectDescriptor devOpsProjectDescriptor = new DevOpsProjectDescriptor(ALM.GITHUB, GITHUB_API_URL, GITHUB_REPO_FULL_NAME);

    AlmSettingDto almSettingDto = mockAlmSettingDto(devOpsProjectDescriptor);
    GithubAppConfiguration githubAppConfiguration = mockGitHubAppConfiguration(almSettingDto);
    when(githubApplicationClient.getInstallationId(githubAppConfiguration, GITHUB_REPO_FULL_NAME)).thenReturn(Optional.empty());

    assertThatIllegalStateException().isThrownBy(
        () -> gitHubDevOpsPlatformService.createProjectAndBindToDevOpsPlatform(dbSession, PROJECT_KEY, almSettingDto, devOpsProjectDescriptor))
      .withMessage("Impossible to find the repository orgname/projectName on GitHub, using the devops config devops-platform-config-1.");
  }

  @Test
  public void createProjectAndBindToDevOpsPlatform_whenRepoFoundOnGitHub_successfullyCreatesProject() {
    // given
    DevOpsProjectDescriptor devOpsProjectDescriptor = new DevOpsProjectDescriptor(ALM.GITHUB, GITHUB_API_URL, GITHUB_REPO_FULL_NAME);

    AlmSettingDto almSettingDto = mockAlmSettingDto(devOpsProjectDescriptor);
    mockExistingGitHubRepository(almSettingDto);

    ComponentCreationData componentCreationData = mockProjectCreation();
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

    // when
    ComponentCreationData actualComponentCreationData = gitHubDevOpsPlatformService.createProjectAndBindToDevOpsPlatform(dbSession, PROJECT_KEY, almSettingDto,
      devOpsProjectDescriptor);

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
    DevOpsProjectDescriptor devOpsProjectDescriptor = new DevOpsProjectDescriptor(ALM.GITHUB, GITHUB_API_URL, GITHUB_REPO_FULL_NAME);
    when(projectDefaultVisibility.get(any())).thenReturn(Visibility.PRIVATE);
    when(gitHubSettings.isProvisioningEnabled()).thenReturn(true);

    AlmSettingDto almSettingDto = mockAlmSettingDto(devOpsProjectDescriptor);
    mockExistingGitHubRepository(almSettingDto);

    ComponentCreationData componentCreationData = mockProjectCreation();
    ProjectAlmSettingDao projectAlmSettingDao = mock();
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);

    // when
    ComponentCreationData actualComponentCreationData = gitHubDevOpsPlatformService.createProjectAndBindToDevOpsPlatform(dbSession, PROJECT_KEY, almSettingDto,
      devOpsProjectDescriptor);

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
    DevOpsProjectDescriptor devOpsProjectDescriptor = new DevOpsProjectDescriptor(ALM.GITHUB, GITHUB_API_URL, GITHUB_REPO_FULL_NAME);
    AlmSettingDto almSettingDto = mockAlmSettingDto(devOpsProjectDescriptor);
    mockExistingGitHubRepository(almSettingDto);

    when(githubApplicationClient.createAppInstallationToken(any(), anyLong())).thenReturn(Optional.empty());

    assertThatIllegalStateException().isThrownBy(
        () -> gitHubDevOpsPlatformService.createProjectAndBindToDevOpsPlatform(dbSession, PROJECT_KEY, almSettingDto, devOpsProjectDescriptor))
      .withMessage("Error while generating token for GitHub Api Url https://api.toto.com (installation id: 534534534543)");
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

  private static AlmSettingDto mockAlmSettingDto(DevOpsProjectDescriptor devOpsProjectDescriptor) {
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
