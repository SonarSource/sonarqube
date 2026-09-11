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
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabServerException;
import org.sonar.api.utils.System2;
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
  private GithubRemediationPermissionChecker githubRemediationPermissionChecker;
  @Mock
  private GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator;
  @Mock
  private AzureDevOpsValidator azureDevOpsValidator;
  @Mock
  private System2 system2;

  private static final Duration CACHE_TTL = Duration.ofSeconds(60);

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
    underTest = DopPermissionValidationService.createForTesting(githubRemediationPermissionChecker, gitlabGlobalSettingsValidator, azureDevOpsValidator, system2,
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
  void check_whenGithub_returnsTheInstallationAwareVerdictWithItsDetail() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    DopPermissionCheck githubCheck = DopPermissionCheck.githubComplete(
      List.of(),
      3,
      List.of(new AffectedInstallation("1", "acme", "https://github.com/organizations/acme/settings/installations/1",
        List.of(new PermissionDeficit("contents", "write", "read")))));
    when(githubRemediationPermissionChecker.checkConnection(almSetting)).thenReturn(githubCheck);

    DopPermissionCheck result = underTest.check(almSetting);

    assertThat(result).isEqualTo(githubCheck);
    assertThat(result.status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(result.affectedInstallations()).extracting(AffectedInstallation::installationOwner).containsExactly("acme");
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
    when(githubRemediationPermissionChecker.checkConnection(github)).thenReturn(DopPermissionCheck.insufficient());
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
    when(githubRemediationPermissionChecker.checkConnection(almSetting)).thenReturn(DopPermissionCheck.sufficient());

    TimestampedPermissionCheck first = underTest.checkCached(almSetting);
    TimestampedPermissionCheck second = underTest.checkCached(almSetting);

    assertThat(first.check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(first.checkedAt()).isEqualTo(second.checkedAt()).isEqualTo(1_000L);
    verify(githubRemediationPermissionChecker, times(1)).checkConnection(any());
  }

  @Test
  void checkCached_keepsTheGithubInstallationFindingsOfTheCachedResult() {
    // The widget reads its organization list off the same cached entry the status comes from.
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    AffectedInstallation affected = new AffectedInstallation("1", "acme", "https://github.com/organizations/acme/settings/installations/1",
      List.of(new PermissionDeficit("contents", "write", "read")));
    when(githubRemediationPermissionChecker.checkConnection(almSetting)).thenReturn(DopPermissionCheck.githubComplete(List.of(), 4, List.of(affected)));

    underTest.checkCached(almSetting);
    TimestampedPermissionCheck cached = underTest.checkCached(almSetting);

    assertThat(cached.check().installationCheckStatus()).isEqualTo(InstallationCheckStatus.COMPLETE);
    assertThat(cached.check().totalInstallationCount()).isEqualTo(4);
    assertThat(cached.check().affectedInstallationCount()).isEqualTo(1);
    assertThat(cached.check().affectedInstallations()).containsExactly(affected);
    verify(githubRemediationPermissionChecker, times(1)).checkConnection(any());
  }

  @Test
  void checkCached_afterTtlExpires_revalidates() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubRemediationPermissionChecker.checkConnection(almSetting)).thenReturn(DopPermissionCheck.sufficient());

    underTest.checkCached(almSetting);
    currentTimeMillis.set(1_000L + CACHE_TTL.toMillis() + 1);
    TimestampedPermissionCheck afterExpiry = underTest.checkCached(almSetting);

    assertThat(afterExpiry.checkedAt()).isEqualTo(1_000L + CACHE_TTL.toMillis() + 1);
    verify(githubRemediationPermissionChecker, times(2)).checkConnection(any());
  }

  @Test
  void checkCached_whenCheckFailed_cachesTheFailureForTheTtl() {
    // A GitHub check walks every installation page. Re-checking on every read while the platform is unreachable or
    // rate-limiting would make the outage worse, so the failure is cached and checkRefreshed() is the way past it.
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubRemediationPermissionChecker.checkConnection(almSetting)).thenReturn(DopPermissionCheck.checkFailed());

    TimestampedPermissionCheck first = underTest.checkCached(almSetting);
    TimestampedPermissionCheck second = underTest.checkCached(almSetting);

    assertThat(first.check().status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    assertThat(second.check().status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    verify(githubRemediationPermissionChecker, times(1)).checkConnection(any());
  }

  @Test
  void checkCached_afterACachedFailureExpires_checksAgain() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubRemediationPermissionChecker.checkConnection(almSetting))
      .thenReturn(DopPermissionCheck.checkFailed())
      .thenReturn(DopPermissionCheck.sufficient());

    assertThat(underTest.checkCached(almSetting).check().status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    currentTimeMillis.set(1_000L + CACHE_TTL.toMillis() + 1);

    assertThat(underTest.checkCached(almSetting).check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
  }

  @Test
  void checkAllCached_cachesEachConfigurationIndependently() {
    AlmSettingDto github = almSetting(ALM.GITHUB);
    AlmSettingDto gitlab = almSetting(ALM.GITLAB);
    when(githubRemediationPermissionChecker.checkConnection(github)).thenReturn(DopPermissionCheck.insufficient());
    when(gitlabGlobalSettingsValidator.hasApiScope(gitlab)).thenReturn(true);

    List<TimestampedPermissionCheck> first = underTest.checkAllCached(List.of(github, gitlab));
    List<TimestampedPermissionCheck> second = underTest.checkAllCached(List.of(github, gitlab));

    assertThat(first).extracting(r -> r.check().status()).containsExactly(PermissionCheckStatus.INSUFFICIENT, PermissionCheckStatus.SUFFICIENT);
    assertThat(second).extracting(TimestampedPermissionCheck::checkedAt).containsExactly(1_000L, 1_000L);
    verify(githubRemediationPermissionChecker, times(1)).checkConnection(any());
    verify(gitlabGlobalSettingsValidator, times(1)).hasApiScope(gitlab);
  }

  @Test
  void checkRefreshed_runsALiveCheckAndReplacesTheCachedResult() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubRemediationPermissionChecker.checkConnection(almSetting))
      .thenReturn(DopPermissionCheck.insufficient())
      .thenReturn(DopPermissionCheck.sufficient());

    assertThat(underTest.checkCached(almSetting).check().status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    currentTimeMillis.set(2_000L);

    TimestampedPermissionCheck refreshed = underTest.checkRefreshed(almSetting);

    assertThat(refreshed.check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(refreshed.checkedAt()).isEqualTo(2_000L);
    // The refreshed verdict, not the stale one, is what the next reader gets.
    TimestampedPermissionCheck afterRefresh = underTest.checkCached(almSetting);
    assertThat(afterRefresh.check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(afterRefresh.checkedAt()).isEqualTo(2_000L);
    verify(githubRemediationPermissionChecker, times(2)).checkConnection(any());
  }

  @Test
  void checkRefreshed_whenTheLiveCheckFails_cachesTheFailureLikeAnyOtherOutcome() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubRemediationPermissionChecker.checkConnection(almSetting)).thenReturn(DopPermissionCheck.checkFailed());

    assertThat(underTest.checkRefreshed(almSetting).check().status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);

    // Readers see the refreshed failure rather than each paying for a fresh scan of every installation page.
    assertThat(underTest.checkCached(almSetting).check().status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    verify(githubRemediationPermissionChecker, times(1)).checkConnection(any());
  }

  @Test
  void checkRefreshed_getsPastACachedFailure_onceTheCauseIsFixed() {
    // The point of the explicit re-check: an administrator who has just fixed the configuration does not wait the TTL.
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubRemediationPermissionChecker.checkConnection(almSetting))
      .thenReturn(DopPermissionCheck.checkFailed())
      .thenReturn(DopPermissionCheck.sufficient());

    assertThat(underTest.checkCached(almSetting).check().status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    assertThat(underTest.checkRefreshed(almSetting).check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);

    assertThat(underTest.checkCached(almSetting).check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    verify(githubRemediationPermissionChecker, times(2)).checkConnection(any());
  }

  @Test
  void checkRefreshed_whenSuccessful_dropsTheProjectEntriesOfThatConfiguration() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubRemediationPermissionChecker.checkConnection(almSetting)).thenReturn(DopPermissionCheck.sufficient());
    when(githubRemediationPermissionChecker.checkProject(almSetting, "acme/widgets"))
      .thenReturn(DopPermissionCheck.githubProject(PermissionCheckStatus.INSUFFICIENT, List.of(), InstallationCheckStatus.COMPLETE))
      .thenReturn(DopPermissionCheck.githubProject(PermissionCheckStatus.SUFFICIENT, List.of(), InstallationCheckStatus.COMPLETE));

    assertThat(underTest.checkForProject(almSetting, "acme/widgets").check().status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    underTest.checkRefreshed(almSetting);

    // The administrator has just fixed the app: the project must not keep reporting the old verdict for its TTL.
    assertThat(underTest.checkForProject(almSetting, "acme/widgets").check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    verify(githubRemediationPermissionChecker, times(2)).checkProject(almSetting, "acme/widgets");
  }

  @Test
  void checkRefreshed_whenSuccessful_leavesTheProjectEntriesOfOtherConfigurationsAlone() {
    AlmSettingDto refreshed = new AlmSettingDto().setAlm(ALM.GITHUB).setKey("github-one");
    AlmSettingDto untouched = new AlmSettingDto().setAlm(ALM.GITHUB).setKey("github-two");
    when(githubRemediationPermissionChecker.checkConnection(refreshed)).thenReturn(DopPermissionCheck.sufficient());
    mockProjectCheck(untouched, "acme/widgets", PermissionCheckStatus.INSUFFICIENT);

    underTest.checkForProject(untouched, "acme/widgets");
    underTest.checkRefreshed(refreshed);
    underTest.checkForProject(untouched, "acme/widgets");

    verify(githubRemediationPermissionChecker, times(1)).checkProject(untouched, "acme/widgets");
  }

  @Test
  void checkRefreshed_whenCheckFailed_keepsTheProjectEntries() {
    // Dropping them would send every project reading its permissions back to a platform that has just proven to be
    // unreachable — the call storm the cache exists to prevent.
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubRemediationPermissionChecker.checkConnection(almSetting)).thenReturn(DopPermissionCheck.checkFailed());
    mockProjectCheck(almSetting, "acme/widgets", PermissionCheckStatus.SUFFICIENT);

    underTest.checkForProject(almSetting, "acme/widgets");
    assertThat(underTest.checkRefreshed(almSetting).check().status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    assertThat(underTest.checkForProject(almSetting, "acme/widgets").check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);

    verify(githubRemediationPermissionChecker, times(1)).checkProject(almSetting, "acme/widgets");
  }

  @Test
  void checkForProject_onGithub_asksTheProjectsOwnInstallation() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubRemediationPermissionChecker.checkProject(almSetting, "acme/widgets"))
      .thenReturn(DopPermissionCheck.githubProject(PermissionCheckStatus.INSUFFICIENT, List.of(), InstallationCheckStatus.COMPLETE));

    TimestampedPermissionCheck result = underTest.checkForProject(almSetting, "acme/widgets");

    assertThat(result.check().status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(result.checkedAt()).isEqualTo(1_000L);
    verify(githubRemediationPermissionChecker, never()).checkConnection(any());
  }

  @Test
  void checkForProject_onGithub_onSecondCallForTheSameRepository_returnsTheCachedResult() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    mockProjectCheck(almSetting, "acme/widgets", PermissionCheckStatus.SUFFICIENT);

    TimestampedPermissionCheck first = underTest.checkForProject(almSetting, "acme/widgets");
    TimestampedPermissionCheck second = underTest.checkForProject(almSetting, "acme/widgets");

    assertThat(first.check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(first.checkedAt()).isEqualTo(second.checkedAt()).isEqualTo(1_000L);
    verify(githubRemediationPermissionChecker, times(1)).checkProject(any(), any());
  }

  @Test
  void checkForProject_onGithub_cachesEachRepositoryOfOneConfigurationIndependently() {
    // Two projects on one configuration can be covered by installations that approved different permissions, so one
    // cached verdict must never answer for the other.
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    mockProjectCheck(almSetting, "acme/widgets", PermissionCheckStatus.SUFFICIENT);
    mockProjectCheck(almSetting, "globex/gadgets", PermissionCheckStatus.INSUFFICIENT);

    assertThat(underTest.checkForProject(almSetting, "acme/widgets").check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(underTest.checkForProject(almSetting, "globex/gadgets").check().status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(underTest.checkForProject(almSetting, "acme/widgets").check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(underTest.checkForProject(almSetting, "globex/gadgets").check().status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);

    verify(githubRemediationPermissionChecker, times(1)).checkProject(almSetting, "acme/widgets");
    verify(githubRemediationPermissionChecker, times(1)).checkProject(almSetting, "globex/gadgets");
  }

  @Test
  void checkForProject_onGithub_cachesTheSameRepositoryOfTwoConfigurationsIndependently() {
    AlmSettingDto first = new AlmSettingDto().setAlm(ALM.GITHUB).setKey("github-one");
    AlmSettingDto second = new AlmSettingDto().setAlm(ALM.GITHUB).setKey("github-two");
    mockProjectCheck(first, "acme/widgets", PermissionCheckStatus.SUFFICIENT);
    mockProjectCheck(second, "acme/widgets", PermissionCheckStatus.INSUFFICIENT);

    assertThat(underTest.checkForProject(first, "acme/widgets").check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(underTest.checkForProject(second, "acme/widgets").check().status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);

    verify(githubRemediationPermissionChecker, times(1)).checkProject(first, "acme/widgets");
    verify(githubRemediationPermissionChecker, times(1)).checkProject(second, "acme/widgets");
  }

  @Test
  void checkForProject_onGithub_afterTtlExpires_checksAgain() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    mockProjectCheck(almSetting, "acme/widgets", PermissionCheckStatus.SUFFICIENT);

    underTest.checkForProject(almSetting, "acme/widgets");
    currentTimeMillis.set(1_000L + CACHE_TTL.toMillis() + 1);
    TimestampedPermissionCheck afterExpiry = underTest.checkForProject(almSetting, "acme/widgets");

    assertThat(afterExpiry.checkedAt()).isEqualTo(1_000L + CACHE_TTL.toMillis() + 1);
    verify(githubRemediationPermissionChecker, times(2)).checkProject(any(), any());
  }

  @Test
  void checkForProject_onGithub_whenTheProjectHasNoRepository_isItsOwnCacheEntry() {
    // A null slug is a key in its own right, not a prefix that could collide with a bound project's entry.
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    mockProjectCheck(almSetting, null, PermissionCheckStatus.INSUFFICIENT);
    mockProjectCheck(almSetting, "acme/widgets", PermissionCheckStatus.SUFFICIENT);

    assertThat(underTest.checkForProject(almSetting, null).check().status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(underTest.checkForProject(almSetting, "acme/widgets").check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(underTest.checkForProject(almSetting, null).check().status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);

    verify(githubRemediationPermissionChecker, times(1)).checkProject(almSetting, null);
    verify(githubRemediationPermissionChecker, times(1)).checkProject(almSetting, "acme/widgets");
  }

  @Test
  void checkForProject_onGithub_whenCheckFailed_cachesTheFailureForTheTtl() {
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    mockProjectCheck(almSetting, "acme/widgets", PermissionCheckStatus.CHECK_FAILED);

    assertThat(underTest.checkForProject(almSetting, "acme/widgets").check().status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);
    assertThat(underTest.checkForProject(almSetting, "acme/widgets").check().status()).isEqualTo(PermissionCheckStatus.CHECK_FAILED);

    verify(githubRemediationPermissionChecker, times(1)).checkProject(any(), any());
  }

  @Test
  void checkForProject_onGithub_neitherReadsNorWritesThePerConfigurationCache() {
    // The two caches answer different questions: the instance-wide verdict must not stand in for a project's own.
    AlmSettingDto almSetting = almSetting(ALM.GITHUB);
    when(githubRemediationPermissionChecker.checkConnection(almSetting)).thenReturn(DopPermissionCheck.insufficient());
    mockProjectCheck(almSetting, "acme/widgets", PermissionCheckStatus.SUFFICIENT);

    assertThat(underTest.checkForProject(almSetting, "acme/widgets").check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(underTest.checkCached(almSetting).check().status()).isEqualTo(PermissionCheckStatus.INSUFFICIENT);
    assertThat(underTest.checkForProject(almSetting, "acme/widgets").check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);

    verify(githubRemediationPermissionChecker, times(1)).checkConnection(almSetting);
    verify(githubRemediationPermissionChecker, times(1)).checkProject(almSetting, "acme/widgets");
  }

  @Test
  void checkForProject_onGitlab_keepsUsingTheSharedCachedVerdict() {
    // A GitLab credential is the same whatever the project, so there is nothing per-project to look up.
    AlmSettingDto almSetting = almSetting(ALM.GITLAB);
    when(gitlabGlobalSettingsValidator.hasApiScope(almSetting)).thenReturn(true);

    assertThat(underTest.checkForProject(almSetting, "42").check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);
    assertThat(underTest.checkForProject(almSetting, "42").check().status()).isEqualTo(PermissionCheckStatus.SUFFICIENT);

    verify(gitlabGlobalSettingsValidator, times(1)).hasApiScope(almSetting);
  }

  private void mockProjectCheck(AlmSettingDto almSetting, @Nullable String repositorySlug, PermissionCheckStatus status) {
    when(githubRemediationPermissionChecker.checkProject(almSetting, repositorySlug))
      .thenReturn(DopPermissionCheck.githubProject(status, List.of(), InstallationCheckStatus.COMPLETE));
  }

  private static AlmSettingDto almSetting(ALM alm) {
    return new AlmSettingDto().setAlm(alm).setKey("my-" + alm.getId());
  }
}
