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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.alm.client.github.AppInstallationToken;
import org.sonar.alm.client.github.GithubApplicationClient;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.github.GithubPermissionConverter;
import org.sonar.auth.github.GitHubSettings;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.PermissionUpdater;
import org.sonar.server.permission.UserPermissionChange;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
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

  private static final DevOpsProjectDescriptor GITHUB_PROJECT_DESCRIPTOR = new DevOpsProjectDescriptor(ALM.GITHUB, GITHUB_API_URL, GITHUB_REPO_FULL_NAME);
  private static final Map<String, String> VALID_GITHUB_PROJECT_COORDINATES = Map.of(
    DEVOPS_PLATFORM_URL, GITHUB_PROJECT_DESCRIPTOR.url(),
    DEVOPS_PLATFORM_PROJECT_IDENTIFIER, GITHUB_PROJECT_DESCRIPTOR.projectIdentifier());
  private static final long APP_INSTALLATION_ID = 534534534543L;

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
  @Mock
  private AppInstallationToken appInstallationToken;
  @Mock
  private AppInstallationToken authAppInstallationToken;
  @Mock
  private PermissionService permissionService;
  @Mock
  private PermissionUpdater<UserPermissionChange> permissionUpdater;
  @Mock
  private ManagedProjectService managedProjectService;

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

    DevOpsProjectCreator devOpsProjectCreator = githubProjectCreatorFactory.getDevOpsProjectCreator(dbSession, VALID_GITHUB_PROJECT_COORDINATES).orElseThrow();

    GithubProjectCreator expectedGithubProjectCreator = getExpectedGithubProjectCreator(almSettingDto, false, false);
    assertThat(devOpsProjectCreator).usingRecursiveComparison().isEqualTo(expectedGithubProjectCreator);
  }

  @Test
  public void getDevOpsProjectCreator_whenOneValidAlmSettingAndPublicByDefaultAndAutoProvisioningEnabled_shouldInstantiateDevOpsProjectCreatorAndDefineAnAuthAppToken() {
    AlmSettingDto almSettingDto = mockAlmSettingDto(true);
    mockSuccessfulGithubInteraction();

    when(projectDefaultVisibility.get(any()).isPrivate()).thenReturn(true);
    when(gitHubSettings.isProvisioningEnabled()).thenReturn(true);
    when(gitHubSettings.appId()).thenReturn("4324");
    when(gitHubSettings.privateKey()).thenReturn("privateKey");
    when(gitHubSettings.apiURL()).thenReturn(GITHUB_API_URL);

    long authAppInstallationId = 32;
    when(githubApplicationClient.getInstallationId(any(), eq(GITHUB_REPO_FULL_NAME))).thenReturn(Optional.of(authAppInstallationId));
    when(githubApplicationClient.createAppInstallationToken(any(), eq(authAppInstallationId))).thenReturn(Optional.of(authAppInstallationToken));

    DevOpsProjectCreator devOpsProjectCreator = githubProjectCreatorFactory.getDevOpsProjectCreator(dbSession, VALID_GITHUB_PROJECT_COORDINATES).orElseThrow();

    GithubProjectCreator expectedGithubProjectCreator = getExpectedGithubProjectCreator(almSettingDto, true, true);
    assertThat(devOpsProjectCreator).usingRecursiveComparison().isEqualTo(expectedGithubProjectCreator);
  }

  @Test
  public void getDevOpsProjectCreator_whenOneMatchingAndOneNotMatchingAlmSetting_shouldInstantiateDevOpsProjectCreator() {
    AlmSettingDto matchingAlmSettingDto = mockAlmSettingDto(true);
    AlmSettingDto notMatchingAlmSettingDto = mockAlmSettingDto(false);
    when(dbClient.almSettingDao().selectByAlm(dbSession, ALM.GITHUB)).thenReturn(List.of(notMatchingAlmSettingDto, matchingAlmSettingDto));

    mockSuccessfulGithubInteraction();

    DevOpsProjectCreator devOpsProjectCreator = githubProjectCreatorFactory.getDevOpsProjectCreator(dbSession, VALID_GITHUB_PROJECT_COORDINATES).orElseThrow();

    GithubProjectCreator expectedGithubProjectCreator = getExpectedGithubProjectCreator(matchingAlmSettingDto, false, false);
    assertThat(devOpsProjectCreator).usingRecursiveComparison().isEqualTo(expectedGithubProjectCreator);
  }

  @Test
  public void getDevOpsProjectCreatorFromImport_shouldInstantiateDevOpsProjectCreator() {
    AlmSettingDto mockAlmSettingDto = mockAlmSettingDto(true);

    mockSuccessfulGithubInteraction();

    DevOpsProjectCreator devOpsProjectCreator = githubProjectCreatorFactory.getDevOpsProjectCreator(dbSession, mockAlmSettingDto, appInstallationToken, GITHUB_PROJECT_DESCRIPTOR);

    GithubProjectCreator expectedGithubProjectCreator = getExpectedGithubProjectCreator(mockAlmSettingDto, false, false);
    assertThat(devOpsProjectCreator).usingRecursiveComparison().isEqualTo(expectedGithubProjectCreator);
  }

  private void mockSuccessfulGithubInteraction() {
    when(githubApplicationClient.getInstallationId(any(), eq(GITHUB_REPO_FULL_NAME))).thenReturn(Optional.of(APP_INSTALLATION_ID));
    when(githubApplicationClient.createAppInstallationToken(any(), eq(APP_INSTALLATION_ID))).thenReturn(Optional.of(appInstallationToken));
  }

  private GithubProjectCreator getExpectedGithubProjectCreator(AlmSettingDto almSettingDto, boolean projectsArePrivateByDefault, boolean isInstanceManaged) {
    DevOpsProjectDescriptor devOpsProjectDescriptor = new DevOpsProjectDescriptor(ALM.GITHUB, almSettingDto.getUrl(), GITHUB_REPO_FULL_NAME);
    AppInstallationToken authAppInstallToken = isInstanceManaged ? authAppInstallationToken : null;
    GithubProjectCreationParameters githubProjectCreationParameters = new GithubProjectCreationParameters(devOpsProjectDescriptor,
      almSettingDto, projectsArePrivateByDefault, isInstanceManaged, userSession, appInstallationToken, authAppInstallToken);
    return new GithubProjectCreator(dbClient, githubApplicationClient, githubPermissionConverter, projectKeyGenerator, componentUpdater, permissionUpdater, permissionService,
      managedProjectService, githubProjectCreationParameters);
  }

  private AlmSettingDto mockAlmSettingDto(boolean repoAccess) {
    AlmSettingDto almSettingDto = mock();
    when(almSettingDto.getUrl()).thenReturn(repoAccess ? GITHUB_PROJECT_DESCRIPTOR.url() : "anotherUrl");

    when(dbClient.almSettingDao().selectByAlm(dbSession, ALM.GITHUB)).thenReturn(List.of(almSettingDto));
    return almSettingDto;
  }

}
