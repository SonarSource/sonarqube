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
package org.sonar.server.common.almsettings.gitlab;

import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabProjectAccessToken;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.core.scm.ScmAccessToken;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GitlabScmAccessTokenProviderTest {

  private static final String PROJECT_KEY = "my-project";
  private static final String ALM_SETTING_UUID = "almSettingUuid";
  private static final String GITLAB_PROJECT_ID = "12345";
  private static final String GITLAB_URL = "https://gitlab.example.com/api/v4";
  private static final String DECRYPTED_PAT = "glpat-admin-token";

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private DbClient dbClient;
  @Mock
  private GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator;
  @Mock
  private GitlabApplicationClient gitlabApplicationClient;
  @Mock
  private Settings settings;
  @Mock
  private Encryption encryption;

  private GitlabScmAccessTokenProvider underTest;

  @org.junit.Before
  public void setUp() {
    when(settings.getEncryption()).thenReturn(encryption);
    underTest = new GitlabScmAccessTokenProvider(dbClient, gitlabGlobalSettingsValidator, gitlabApplicationClient, settings);
  }

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
  public void mint_whenAlmSettingIsNotGitlab_shouldReturnEmpty() {
    mockProjectAlmSetting(GITLAB_PROJECT_ID);
    AlmSettingDto almSetting = mock();
    when(almSetting.getAlm()).thenReturn(ALM.GITHUB);
    when(dbClient.almSettingDao().selectByUuid(any(), eq(ALM_SETTING_UUID))).thenReturn(Optional.of(almSetting));

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenAlmRepoIsBlank_shouldReturnEmpty() {
    mockProjectAlmSetting(" ");
    mockGitlabAlmSetting();

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenAlmRepoIsNotNumeric_shouldReturnEmpty() {
    mockProjectAlmSetting("acme/widgets");
    mockGitlabAlmSetting();

    assertThat(underTest.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenGitlabConfigurationIsInvalid_shouldThrow() {
    mockProjectAlmSetting(GITLAB_PROJECT_ID);
    AlmSettingDto almSetting = mockGitlabAlmSetting();
    IllegalArgumentException cause = new IllegalArgumentException("bad config");
    doThrow(cause).when(gitlabGlobalSettingsValidator).validate(almSetting);

    assertThatThrownBy(() -> underTest.mint(PROJECT_KEY))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Cannot mint a GitLab access token for project '%s'".formatted(PROJECT_KEY))
      .hasCause(cause);
  }

  @Test
  public void mint_whenEverythingSucceeds_shouldReturnToken() {
    mockProjectAlmSetting(GITLAB_PROJECT_ID);
    mockGitlabAlmSetting();

    GitlabProjectAccessToken token = mock();
    when(token.getName()).thenReturn("sonarqube-remediation-agent");
    when(token.getToken()).thenReturn("glpat-abc123");
    when(token.getExpiresAt()).thenReturn("2026-08-06");
    when(gitlabApplicationClient.createProjectAccessToken(eq(GITLAB_URL), eq(DECRYPTED_PAT), eq(12345L),
      eq("sonarqube-remediation-agent"), eq(List.of("api", "write_repository")), any())).thenReturn(token);

    Optional<ScmAccessToken> result = underTest.mint(PROJECT_KEY);

    assertThat(result).contains(new ScmAccessToken("gitlab", "sonarqube-remediation-agent", "glpat-abc123", "2026-08-06"));
  }

  private ProjectDto mockProject() {
    ProjectDto project = mock();
    when(dbClient.projectDao().selectProjectByKey(any(), eq(PROJECT_KEY))).thenReturn(Optional.of(project));
    return project;
  }

  private ProjectAlmSettingDto mockProjectAlmSetting(String almRepo) {
    ProjectDto project = mockProject();
    ProjectAlmSettingDto projectAlmSetting = mock();
    when(projectAlmSetting.getAlmSettingUuid()).thenReturn(ALM_SETTING_UUID);
    when(projectAlmSetting.getAlmRepo()).thenReturn(almRepo);
    when(dbClient.projectAlmSettingDao().selectByProject(any(), eq(project))).thenReturn(Optional.of(projectAlmSetting));
    return projectAlmSetting;
  }

  private AlmSettingDto mockGitlabAlmSetting() {
    AlmSettingDto almSetting = mock();
    when(almSetting.getAlm()).thenReturn(ALM.GITLAB);
    when(almSetting.getUrl()).thenReturn(GITLAB_URL);
    when(almSetting.getDecryptedPersonalAccessToken(encryption)).thenReturn(DECRYPTED_PAT);
    when(dbClient.almSettingDao().selectByUuid(any(), eq(ALM_SETTING_UUID))).thenReturn(Optional.of(almSetting));
    return almSetting;
  }
}
