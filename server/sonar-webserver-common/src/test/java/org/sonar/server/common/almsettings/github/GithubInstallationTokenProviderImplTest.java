/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.auth.github.ExpiringAppInstallationToken;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.core.scm.github.GithubInstallationToken;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.ServerException;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GithubInstallationTokenProviderImplTest {

  private static final String PROJECT_KEY = "my-project";
  private static final String ALM_SETTING_UUID = "almSettingUuid";
  private static final String ALM_REPO = "acme/widgets";
  private static final String BARE_REPO_NAME = "widgets";
  private static final long INSTALLATION_ID = 42L;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock
  private GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  @Mock
  private GithubApplicationClientImpl githubApplicationClient;

  @InjectMocks
  private GithubInstallationTokenProviderImpl underTest;

  @Test
  public void mint_whenProjectUnknown_shouldReturnEmpty() {
    when(dbClient.projectDao().selectProjectByKey(any(), eq(PROJECT_KEY))).thenReturn(Optional.empty());

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenProjectNotBoundToAnyDevOpsPlatform_shouldReturnEmpty() {
    ProjectDto project = mockProject();
    when(dbClient.projectAlmSettingDao().selectByProject(any(), eq(project))).thenReturn(Optional.empty());

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenAlmSettingIsNotGithub_shouldReturnEmpty() {
    mockProjectAlmSetting();
    AlmSettingDto almSetting = mock();
    when(almSetting.getAlm()).thenReturn(ALM.GITLAB);
    when(dbClient.almSettingDao().selectByUuid(any(), eq(ALM_SETTING_UUID))).thenReturn(Optional.of(almSetting));

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenAlmRepoIsBlank_shouldReturnEmpty() {
    ProjectAlmSettingDto projectAlmSetting = mockProjectAlmSetting();
    when(projectAlmSetting.getAlmRepo()).thenReturn(" ");
    mockGithubAlmSetting();

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenGithubAppConfigurationIsInvalid_shouldThrow() {
    mockProjectAlmSetting();
    AlmSettingDto almSetting = mockGithubAlmSetting();
    IllegalArgumentException cause = new IllegalArgumentException("bad config");
    when(githubGlobalSettingsValidator.validate(almSetting)).thenThrow(cause);

    assertThatThrownBy(() -> underTest.mint(PROJECT_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Cannot mint a GitHub installation token for project '%s': invalid GitHub App configuration: bad config", PROJECT_KEY)
      .hasCause(cause);
  }

  @Test
  public void mint_whenAppNotInstalledOnRepository_shouldReturnEmpty() {
    mockProjectAlmSetting();
    AlmSettingDto almSetting = mockGithubAlmSetting();
    GithubAppConfiguration configuration = mock();
    when(githubGlobalSettingsValidator.validate(almSetting)).thenReturn(configuration);
    when(githubApplicationClient.getInstallationId(configuration, ALM_REPO)).thenReturn(Optional.empty());

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenTokenCreationFails_shouldThrowServerException() {
    mockProjectAlmSetting();
    AlmSettingDto almSetting = mockGithubAlmSetting();
    GithubAppConfiguration configuration = mock();
    when(githubGlobalSettingsValidator.validate(almSetting)).thenReturn(configuration);
    when(githubApplicationClient.getInstallationId(configuration, ALM_REPO)).thenReturn(Optional.of(INSTALLATION_ID));
    when(githubApplicationClient.createAppInstallationToken(configuration, INSTALLATION_ID, BARE_REPO_NAME)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.mint(PROJECT_KEY))
      .isInstanceOf(ServerException.class)
      .extracting(e -> ((ServerException) e).httpCode())
      .isEqualTo(HTTP_INTERNAL_ERROR);
  }

  @Test
  public void mint_whenEverythingSucceeds_shouldReturnToken() {
    mockProjectAlmSetting();
    AlmSettingDto almSetting = mockGithubAlmSetting();
    GithubAppConfiguration configuration = mock();
    when(githubGlobalSettingsValidator.validate(almSetting)).thenReturn(configuration);
    when(githubApplicationClient.getInstallationId(configuration, ALM_REPO)).thenReturn(Optional.of(INSTALLATION_ID));

    OffsetDateTime expiresAt = OffsetDateTime.parse("2026-07-15T15:00:00Z");
    ExpiringAppInstallationToken token = mock();
    when(token.getValue()).thenReturn("ghs_abc123");
    when(token.getExpiresAt()).thenReturn(expiresAt);
    when(githubApplicationClient.createAppInstallationToken(configuration, INSTALLATION_ID, BARE_REPO_NAME)).thenReturn(Optional.of(token));

    Optional<GithubInstallationToken> result = underTest.mint(PROJECT_KEY);

    assertThat(result).contains(new GithubInstallationToken("ghs_abc123", expiresAt.format(ISO_OFFSET_DATE_TIME)));
  }

  private ProjectDto mockProject() {
    ProjectDto project = mock();
    when(dbClient.projectDao().selectProjectByKey(any(), eq(PROJECT_KEY))).thenReturn(Optional.of(project));
    return project;
  }

  private ProjectAlmSettingDto mockProjectAlmSetting() {
    ProjectDto project = mockProject();
    ProjectAlmSettingDto projectAlmSetting = mock();
    when(projectAlmSetting.getAlmSettingUuid()).thenReturn(ALM_SETTING_UUID);
    when(projectAlmSetting.getAlmRepo()).thenReturn(ALM_REPO);
    when(dbClient.projectAlmSettingDao().selectByProject(any(), eq(project))).thenReturn(Optional.of(projectAlmSetting));
    return projectAlmSetting;
  }

  private AlmSettingDto mockGithubAlmSetting() {
    AlmSettingDto almSetting = mock();
    when(almSetting.getAlm()).thenReturn(ALM.GITHUB);
    when(dbClient.almSettingDao().selectByUuid(any(), eq(ALM_SETTING_UUID))).thenReturn(Optional.of(almSetting));
    return almSetting;
  }
}
