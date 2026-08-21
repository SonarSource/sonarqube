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
package org.sonar.server.common.almsettings.permission;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabServerException;
import org.sonar.auth.github.GithubAppPermissions;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DopPermissionValidationServiceTest {

  @Mock
  private GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  @Mock
  private GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator;
  @Mock
  private AzureDevOpsValidator azureDevOpsValidator;

  @InjectMocks
  private DopPermissionValidationService underTest;

  @Test
  void check_whenGithubHasAllPermissions_returnsSufficient() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubGlobalSettingsValidator.findMissingPermissions(almSetting, GithubAppPermissions.TOKEN_MINTING_PERMISSIONS)).thenReturn(List.of());

    DopPermissionCheck result = underTest.check(almSetting);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
  }

  @Test
  void check_whenGithubMissingPermissions_returnsInsufficient() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubGlobalSettingsValidator.findMissingPermissions(almSetting, GithubAppPermissions.TOKEN_MINTING_PERMISSIONS)).thenReturn(List.of("contents"));

    DopPermissionCheck result = underTest.check(almSetting);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
  }

  @Test
  void check_whenValidationThrows_returnsCheckFailed() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubGlobalSettingsValidator.findMissingPermissions(any(), any())).thenThrow(new IllegalArgumentException("Authentication failed"));

    DopPermissionCheck result = underTest.check(almSetting);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
  }

  @Test
  void check_whenGitlabTokenHasInsufficientScope_returnsInsufficient() {
    AlmSettingDto almSetting = almSetting(ALM.GITLAB);
    doThrow(new GitlabServerException(HTTP_FORBIDDEN, "Your GitLab token has insufficient scope"))
      .when(gitlabGlobalSettingsValidator).validate(almSetting);

    DopPermissionCheck result = underTest.check(almSetting);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
  }

  @Test
  void check_whenAzureConnectivityOk_returnsUnknown() {
    AlmSettingDto almSetting = almSetting(ALM.AZURE_DEVOPS);

    DopPermissionCheck result = underTest.check(almSetting);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.UNKNOWN);
  }

  @Test
  void check_whenPlatformNotSupported_throws() {
    AlmSettingDto almSetting = almSetting(ALM.BITBUCKET);

    assertThatThrownBy(() -> underTest.check(almSetting))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("not supported");
  }

  @Test
  void checkAll_validatesEveryConfig_inInputOrder() {
    AlmSettingDto github = almSetting(ALM.GITHUB);
    AlmSettingDto gitlab = almSetting(ALM.GITLAB);
    AlmSettingDto azure = almSetting(ALM.AZURE_DEVOPS);
    when(githubGlobalSettingsValidator.findMissingPermissions(github, GithubAppPermissions.TOKEN_MINTING_PERMISSIONS)).thenReturn(List.of("contents"));
    // gitlab: validate() succeeds (void) -> SUFFICIENT; azure: validate() succeeds (void) -> UNKNOWN advisory

    List<DopPermissionCheck> results = underTest.checkAll(List.of(github, gitlab, azure));

    assertThat(results).extracting(DopPermissionCheck::status)
      .containsExactly(PermissionCheckStatus.INSUFFICIENT, PermissionCheckStatus.SUFFICIENT, PermissionCheckStatus.UNKNOWN);
  }

  @Test
  void checkAll_withNoConfig_returnsEmpty() {
    assertThat(underTest.checkAll(List.of())).isEmpty();
  }

  private static AlmSettingDto almSetting(ALM alm) {
    return new AlmSettingDto().setAlm(alm).setKey("my-" + alm.getId());
  }
}
