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

import com.google.common.base.Ticker;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabServerException;
import org.sonar.api.utils.System2;
import org.sonar.auth.github.GithubAppPermissions;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DopPermissionValidationServiceTest {

  @Mock
  private GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  @Mock
  private GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator;
  @Mock
  private AzureDevOpsValidator azureDevOpsValidator;
  @Mock
  private System2 system2;

  private final AtomicLong currentTimeMillis = new AtomicLong(1_000L);

  private DopPermissionValidationService underTest;

  @BeforeEach
  void setUp() {
    // lenient: only the cache-related tests below actually invoke System2#now(); check()/checkAll() never do.
    lenient().when(system2.now()).thenAnswer(invocation -> currentTimeMillis.get());
    // Production defaults the cache to a real monotonic Ticker (see DopPermissionValidationService's public
    // constructor); tests use the package-private createForTesting() factory to inject a fake Ticker driven by the
    // same clock as System2#now(), so TTL expiry can be exercised deterministically without sleeping past a real TTL.
    // Must be built here, not in a field initializer: @Mock fields aren't populated until after instance field
    // initializers run.
    underTest = DopPermissionValidationService.createForTesting(githubGlobalSettingsValidator, gitlabGlobalSettingsValidator, azureDevOpsValidator, system2,
      testTicker());
  }

  private Ticker testTicker() {
    return new Ticker() {
      @Override
      public long read() {
        return TimeUnit.MILLISECONDS.toNanos(currentTimeMillis.get());
      }
    };
  }

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
      .when(gitlabGlobalSettingsValidator).hasApiScope(almSetting);

    DopPermissionCheck result = underTest.check(almSetting);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
  }

  @Test
  void check_whenGitlabTokenLacksApiScope_returnsInsufficient() {
    AlmSettingDto almSetting = almSetting(ALM.GITLAB);
    when(gitlabGlobalSettingsValidator.hasApiScope(almSetting)).thenReturn(false);

    DopPermissionCheck result = underTest.check(almSetting);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
  }

  @Test
  void check_whenGitlabTokenHasApiScope_returnsSufficient() {
    AlmSettingDto almSetting = almSetting(ALM.GITLAB);
    when(gitlabGlobalSettingsValidator.hasApiScope(almSetting)).thenReturn(true);

    DopPermissionCheck result = underTest.check(almSetting);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
  }

  @Test
  void check_whenGitlabTokenIsBot_returnsUnsupportedTokenTypeWithoutValidatingScope() {
    AlmSettingDto almSetting = almSetting(ALM.GITLAB);
    when(gitlabGlobalSettingsValidator.isBotToken(almSetting)).thenReturn(true);

    DopPermissionCheck result = underTest.check(almSetting);

    assertThat(result.status()).isEqualTo(PermissionCheckStatus.UNSUPPORTED_TOKEN_TYPE);
    verify(gitlabGlobalSettingsValidator, never()).hasApiScope(any());
  }

  @Test
  void check_whenGitlabBotCheckThrows_fallsBackToScopeCheck() {
    AlmSettingDto almSetting = almSetting(ALM.GITLAB);
    when(gitlabGlobalSettingsValidator.isBotToken(almSetting)).thenThrow(new IllegalArgumentException("boom"));
    doThrow(new GitlabServerException(HTTP_FORBIDDEN, "Your GitLab token has insufficient scope"))
      .when(gitlabGlobalSettingsValidator).hasApiScope(almSetting);

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
    when(gitlabGlobalSettingsValidator.hasApiScope(gitlab)).thenReturn(true);
    // azure: validate() succeeds (void) -> UNKNOWN advisory

    List<DopPermissionCheck> results = underTest.checkAll(List.of(github, gitlab, azure));

    assertThat(results).extracting(DopPermissionCheck::status)
      .containsExactly(PermissionCheckStatus.INSUFFICIENT, PermissionCheckStatus.SUFFICIENT, PermissionCheckStatus.UNKNOWN);
  }

  @Test
  void checkAll_withNoConfig_returnsEmpty() {
    assertThat(underTest.checkAll(List.of())).isEmpty();
  }

  @Test
  void checkCached_onSecondCall_returnsCachedResultWithoutRevalidating() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubGlobalSettingsValidator.findMissingPermissions(almSetting, GithubAppPermissions.TOKEN_MINTING_PERMISSIONS)).thenReturn(List.of());

    TimestampedPermissionCheck first = underTest.checkCached(almSetting);
    TimestampedPermissionCheck second = underTest.checkCached(almSetting);

    assertThat(first.check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(first.checkedAt()).isEqualTo(second.checkedAt()).isEqualTo(1_000L);
    verify(githubGlobalSettingsValidator, times(1)).findMissingPermissions(any(), any());
  }

  @Test
  void checkCached_afterTtlExpires_revalidates() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubGlobalSettingsValidator.findMissingPermissions(almSetting, GithubAppPermissions.TOKEN_MINTING_PERMISSIONS)).thenReturn(List.of());

    underTest.checkCached(almSetting);
    currentTimeMillis.set(1_000L + Duration.ofSeconds(60).toMillis() + 1);
    TimestampedPermissionCheck afterExpiry = underTest.checkCached(almSetting);

    assertThat(afterExpiry.checkedAt()).isEqualTo(1_000L + Duration.ofSeconds(60).toMillis() + 1);
    verify(githubGlobalSettingsValidator, times(2)).findMissingPermissions(any(), any());
  }

  @Test
  void checkCached_whenCheckFailed_isNotCached_andRevalidatesOnNextCall() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubGlobalSettingsValidator.findMissingPermissions(any(), any())).thenThrow(new IllegalArgumentException("Timed out"));

    TimestampedPermissionCheck first = underTest.checkCached(almSetting);
    TimestampedPermissionCheck second = underTest.checkCached(almSetting);

    assertThat(first.check().status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    assertThat(second.check().status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    // A transient failure must not be pinned for the full TTL: every call re-checks live instead of one call away.
    verify(githubGlobalSettingsValidator, times(2)).findMissingPermissions(any(), any());
  }

  @Test
  void checkAllCached_cachesEachConfigurationIndependently() {
    AlmSettingDto github = almSetting(ALM.GITHUB);
    AlmSettingDto gitlab = almSetting(ALM.GITLAB);
    when(githubGlobalSettingsValidator.findMissingPermissions(github, GithubAppPermissions.TOKEN_MINTING_PERMISSIONS)).thenReturn(List.of("contents"));
    when(gitlabGlobalSettingsValidator.hasApiScope(gitlab)).thenReturn(true);

    List<TimestampedPermissionCheck> first = underTest.checkAllCached(List.of(github, gitlab));
    List<TimestampedPermissionCheck> second = underTest.checkAllCached(List.of(github, gitlab));

    assertThat(first).extracting(r -> r.check().status()).containsExactly(PermissionCheckStatus.INSUFFICIENT, PermissionCheckStatus.SUFFICIENT);
    assertThat(second).extracting(TimestampedPermissionCheck::checkedAt).containsExactly(1_000L, 1_000L);
    verify(githubGlobalSettingsValidator, times(1)).findMissingPermissions(any(), any());
    verify(gitlabGlobalSettingsValidator, times(1)).hasApiScope(gitlab);
  }

  private static AlmSettingDto almSetting(ALM alm) {
    return new AlmSettingDto().setAlm(alm).setKey("my-" + alm.getId());
  }
}
