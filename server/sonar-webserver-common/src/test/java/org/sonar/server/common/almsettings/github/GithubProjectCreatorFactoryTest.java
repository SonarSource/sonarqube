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
package org.sonar.server.common.almsettings.github;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.github.GithubPermissionConverter;
import org.sonar.auth.github.AppInstallationToken;
import org.sonar.auth.github.ExpiringAppInstallationToken;
import org.sonar.auth.github.GitHubSettings;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.almsettings.DevOpsProjectCreationContext;
import org.sonar.server.common.almsettings.DevOpsProjectCreator;
import org.sonar.server.common.almsettings.DevOpsProjectDescriptor;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.common.permission.UserPermissionChange;
import org.sonar.server.common.project.ProjectCreator;
import org.sonar.server.exceptions.BadConfigurationException;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.project.ProjectDefaultVisibility;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.ce.CeTaskCharacteristics.DEVOPS_PLATFORM_PROJECT_IDENTIFIER;
import static org.sonar.core.ce.CeTaskCharacteristics.DEVOPS_PLATFORM_URL;

@RunWith(MockitoJUnitRunner.class)
public class GithubProjectCreatorFactoryTest {
  private static final String PROJECT_NAME = "projectName";
  private static final String ORGANIZATION_NAME = "orgname";
  private static final String GITHUB_REPO_FULL_NAME = ORGANIZATION_NAME + "/" + PROJECT_NAME;
  private static final String GITHUB_API_URL = "https://api.toto.com";

  private static final DevOpsProjectDescriptor GITHUB_PROJECT_DESCRIPTOR = new DevOpsProjectDescriptor(ALM.GITHUB, GITHUB_API_URL, GITHUB_REPO_FULL_NAME, null);
  private static final Map<String, String> VALID_GITHUB_PROJECT_COORDINATES = Map.of(
    DEVOPS_PLATFORM_URL, GITHUB_PROJECT_DESCRIPTOR.url(),
    DEVOPS_PLATFORM_PROJECT_IDENTIFIER, GITHUB_PROJECT_DESCRIPTOR.repositoryIdentifier());
  private static final long APP_INSTALLATION_ID = 534534534543L;
  private static final String USER_ACCESS_TOKEN = "userPat";
  private static final DevOpsProjectCreationContext DEV_OPS_PROJECT = mock();

  @Mock
  private DbSession dbSession;
  @Mock
  private GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  @Mock
  private GithubApplicationClient githubApplicationClient;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ProjectDefaultVisibility projectDefaultVisibility;
  @Mock
  private ProjectKeyGenerator projectKeyGenerator;
  @Mock
  private GitHubSettings gitHubSettings;
  @Mock
  private GithubPermissionConverter githubPermissionConverter;
  @Mock
  private ExpiringAppInstallationToken appInstallationToken;
  @Mock
  private ExpiringAppInstallationToken authAppInstallationToken;
  @Mock
  private PermissionService permissionService;
  @Mock
  private PermissionUpdater<UserPermissionChange> permissionUpdater;
  @Mock
  private ManagedProjectService managedProjectService;
  @Mock
  private ProjectCreator projectCreator;
  @Mock
  private GithubDevOpsProjectCreationContextService devOpsProjectService;

  @InjectMocks
  private GithubProjectCreatorFactory githubProjectCreatorFactory;

  @Test
  public void getDevOpsProjectCreator_whenNoCharacteristics_shouldReturnEmpty() {
    Optional<DevOpsProjectCreator> devOpsProjectCreator = githubProjectCreatorFactory.getDevOpsProjectCreator(dbSession, Map.of());

    assertThat(devOpsProjectCreator).isEmpty();
  }

  @Test
  public void getDevOpsProjectCreator_whenValidCharacteristicsButNoAlmSettingDao_shouldReturnEmpty() {
    Optional<DevOpsProjectCreator> devOpsProjectCreator = githubProjectCreatorFactory.getDevOpsProjectCreator(dbSession, VALID_GITHUB_PROJECT_COORDINATES);
    assertThat(devOpsProjectCreator).isEmpty();
  }

  @Test
  public void getDevOpsProjectCreator_whenValidCharacteristicsButInvalidAlmSettingDto_shouldThrow() {
    AlmSettingDto almSettingDto = mockAlmSettingDto(true);
    IllegalArgumentException error = new IllegalArgumentException("error happened");
    when(githubGlobalSettingsValidator.validate(almSettingDto)).thenThrow(error);

    assertThatIllegalArgumentException().isThrownBy(() -> githubProjectCreatorFactory.getDevOpsProjectCreator(dbSession, VALID_GITHUB_PROJECT_COORDINATES))
      .isSameAs(error);
  }

  @Test
  public void getDevOpsProjectCreator_whenAppHasNoAccessToRepo_shouldReturnEmpty() {
    mockAlmSettingDto(true);
    when(githubApplicationClient.getInstallationId(any(), eq(GITHUB_REPO_FULL_NAME))).thenReturn(Optional.empty());

    Optional<DevOpsProjectCreator> devOpsProjectCreator = githubProjectCreatorFactory.getDevOpsProjectCreator(dbSession, VALID_GITHUB_PROJECT_COORDINATES);
    assertThat(devOpsProjectCreator).isEmpty();
  }

  @Test
  public void getDevOpsProjectCreator_whenNotPossibleToGenerateToken_shouldThrow() {
    AlmSettingDto almSettingDto = mockAlmSettingDto(true);
    when(githubApplicationClient.getInstallationId(any(), eq(GITHUB_REPO_FULL_NAME))).thenReturn(Optional.of(APP_INSTALLATION_ID));
    when(githubGlobalSettingsValidator.validate(almSettingDto)).thenReturn(mock());
    when(githubApplicationClient.createAppInstallationToken(any(), eq(APP_INSTALLATION_ID))).thenReturn(Optional.empty());

    assertThatIllegalStateException().isThrownBy(() -> githubProjectCreatorFactory.getDevOpsProjectCreator(dbSession, VALID_GITHUB_PROJECT_COORDINATES))
      .withMessage("Error while generating token for GitHub Api Url null (installation id: 534534534543)");
  }

  @Test
  public void getDevOpsProjectCreator_whenOneValidAlmSetting_shouldInstantiateDevOpsProjectCreator() {
    AlmSettingDto almSettingDto = mockAlmSettingDto(true);
    mockSuccessfulGithubInteraction();

    when(devOpsProjectService.createDevOpsProject(almSettingDto, GITHUB_PROJECT_DESCRIPTOR, appInstallationToken)).thenReturn(DEV_OPS_PROJECT);
    DevOpsProjectCreator devOpsProjectCreator = githubProjectCreatorFactory.getDevOpsProjectCreator(dbSession, VALID_GITHUB_PROJECT_COORDINATES).orElseThrow();

    GithubProjectCreator expectedGithubProjectCreator = getExpectedGithubProjectCreator(false);
    assertThat(devOpsProjectCreator).usingRecursiveComparison().isEqualTo(expectedGithubProjectCreator);
  }

  @Test
  public void getDevOpsProjectCreator_whenOneValidAlmSettingAndPublicByDefaultAndAutoProvisioningEnabled_shouldInstantiateDevOpsProjectCreatorAndDefineAnAuthAppToken() {
    AlmSettingDto almSettingDto = mockAlmSettingDto(true);
    mockSuccessfulGithubInteraction();

    when(projectDefaultVisibility.get(any()).isPrivate()).thenReturn(true);
    mockValidGitHubSettings();

    long authAppInstallationId = 32;
    when(githubApplicationClient.getInstallationId(any(), eq(GITHUB_REPO_FULL_NAME))).thenReturn(Optional.of(authAppInstallationId));
    when(githubApplicationClient.createAppInstallationToken(any(), eq(authAppInstallationId))).thenReturn(Optional.of(authAppInstallationToken));
    when(DEV_OPS_PROJECT.devOpsPlatformIdentifier()).thenReturn(GITHUB_REPO_FULL_NAME);

    when(devOpsProjectService.createDevOpsProject(almSettingDto, GITHUB_PROJECT_DESCRIPTOR, authAppInstallationToken)).thenReturn(DEV_OPS_PROJECT);
    DevOpsProjectCreator devOpsProjectCreator = githubProjectCreatorFactory.getDevOpsProjectCreator(dbSession, VALID_GITHUB_PROJECT_COORDINATES).orElseThrow();

    GithubProjectCreator expectedGithubProjectCreator = getExpectedGithubProjectCreator(true);
    assertThat(devOpsProjectCreator).usingRecursiveComparison().isEqualTo(expectedGithubProjectCreator);
  }

  @Test
  public void getDevOpsProjectCreator_whenOneMatchingAndOneNotMatchingAlmSetting_shouldInstantiateDevOpsProjectCreator() {
    AlmSettingDto matchingAlmSettingDto = mockAlmSettingDto(true);
    AlmSettingDto notMatchingAlmSettingDto = mockAlmSettingDto(false);
    when(dbClient.almSettingDao().selectByAlm(dbSession, ALM.GITHUB)).thenReturn(List.of(notMatchingAlmSettingDto, matchingAlmSettingDto));

    mockSuccessfulGithubInteraction();

    when(devOpsProjectService.createDevOpsProject(matchingAlmSettingDto, GITHUB_PROJECT_DESCRIPTOR, appInstallationToken)).thenReturn(DEV_OPS_PROJECT);
    DevOpsProjectCreator devOpsProjectCreator = githubProjectCreatorFactory.getDevOpsProjectCreator(dbSession, VALID_GITHUB_PROJECT_COORDINATES).orElseThrow();

    GithubProjectCreator expectedGithubProjectCreator = getExpectedGithubProjectCreator(false);
    assertThat(devOpsProjectCreator).usingRecursiveComparison().isEqualTo(expectedGithubProjectCreator);
  }

  @Test
  public void getDevOpsProjectCreatorFromImport_shouldInstantiateDevOpsProjectCreator() {
    AlmSettingDto mockAlmSettingDto = mockAlmSettingDto(true);
    mockAlmPatDto(mockAlmSettingDto);

    mockSuccessfulGithubInteraction();

    when(devOpsProjectService.create(mockAlmSettingDto, GITHUB_PROJECT_DESCRIPTOR)).thenReturn(DEV_OPS_PROJECT);
    DevOpsProjectCreator devOpsProjectCreator = githubProjectCreatorFactory.getDevOpsProjectCreator(mockAlmSettingDto, GITHUB_PROJECT_DESCRIPTOR).orElseThrow();

    GithubProjectCreator expectedGithubProjectCreator = getExpectedGithubProjectCreator(false);
    assertThat(devOpsProjectCreator).usingRecursiveComparison().isEqualTo(expectedGithubProjectCreator);
  }

  @Test
  public void getDevOpsProjectCreatorFromImport_whenGitHubConfigDoesNotAllowAccessToRepo_shouldThrow() {
    AlmSettingDto mockAlmSettingDto = mockAlmSettingDto(false);
    mockAlmPatDto(mockAlmSettingDto);

    mockValidGitHubSettings();

    when(githubApplicationClient.getInstallationId(any(), eq(GITHUB_REPO_FULL_NAME))).thenReturn(Optional.empty());
    when(devOpsProjectService.create(mockAlmSettingDto, GITHUB_PROJECT_DESCRIPTOR)).thenReturn(DEV_OPS_PROJECT);
    when(DEV_OPS_PROJECT.devOpsPlatformIdentifier()).thenReturn(GITHUB_PROJECT_DESCRIPTOR.repositoryIdentifier());

    assertThatThrownBy(() -> githubProjectCreatorFactory.getDevOpsProjectCreator(mockAlmSettingDto, GITHUB_PROJECT_DESCRIPTOR))
      .isInstanceOf(BadConfigurationException.class)
      .hasMessage(format("GitHub auto-provisioning is activated. However the repo %s is not in the scope of the authentication application. "
        + "The permissions can't be checked, and the project can not be created.",
        GITHUB_REPO_FULL_NAME));
  }

  private void mockValidGitHubSettings() {
    when(gitHubSettings.appId()).thenReturn("4324");
    when(gitHubSettings.privateKey()).thenReturn("privateKey");
    when(gitHubSettings.apiURL()).thenReturn(GITHUB_API_URL);
    when(gitHubSettings.isProvisioningEnabled()).thenReturn(true);
  }

  private void mockSuccessfulGithubInteraction() {
    when(githubApplicationClient.getInstallationId(any(), eq(GITHUB_REPO_FULL_NAME))).thenReturn(Optional.of(APP_INSTALLATION_ID));
    when(githubApplicationClient.createAppInstallationToken(any(), eq(APP_INSTALLATION_ID))).thenReturn(Optional.of(appInstallationToken));
  }

  private GithubProjectCreator getExpectedGithubProjectCreator(boolean isInstanceManaged) {
    AppInstallationToken authAppInstallToken = isInstanceManaged ? authAppInstallationToken : null;
    return new GithubProjectCreator(dbClient, DEV_OPS_PROJECT,
      projectKeyGenerator, gitHubSettings, projectCreator, permissionService, permissionUpdater, managedProjectService, githubApplicationClient,
      githubPermissionConverter, authAppInstallToken);
  }

  private AlmSettingDto mockAlmSettingDto(boolean repoAccess) {
    AlmSettingDto almSettingDto = mock();
    when(almSettingDto.getUrl()).thenReturn(repoAccess ? GITHUB_PROJECT_DESCRIPTOR.url() : "anotherUrl");
    when(almSettingDto.getAlm()).thenReturn(ALM.GITHUB);

    when(dbClient.almSettingDao().selectByAlm(dbSession, ALM.GITHUB)).thenReturn(List.of(almSettingDto));
    return almSettingDto;
  }

  private void mockAlmPatDto(AlmSettingDto almSettingDto) {
    when(dbClient.almPatDao().selectByUserAndAlmSetting(any(), eq("userUuid"), eq(almSettingDto)))
      .thenReturn(Optional.of(new AlmPatDto().setPersonalAccessToken(USER_ACCESS_TOKEN)));
  }

}
