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
package org.sonar.server.v2.api.onboarding.host;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.auth.github.ExpiringAppInstallationToken;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GitHubInstallationReadTokenPortTest {

  private static final String SETTING_KEY = "github-main";
  private static final String REPOSITORY = "Owner/Repo";
  private static final String TOKEN = "ghs_super-secret-installation-token";
  private static final String PRIVATE_KEY = "super-secret-private-key";
  private static final long INSTALLATION_ID = 42L;
  private static final Instant NOW = Instant.parse("2026-11-24T10:00:00Z");

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  private final MutableClock clock = new MutableClock(NOW);
  private final DbSession dbSession = mock(DbSession.class);
  private final AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
  private final ProjectAlmSettingDao projectAlmSettingDao = mock(ProjectAlmSettingDao.class);
  private final GithubApplicationClientImpl endpointChecker = mock(GithubApplicationClientImpl.class);
  private final GithubApplicationClient githubClient = mock(GithubApplicationClient.class);
  private final AlmSettingDto setting = mock(AlmSettingDto.class);
  private final DbClient dbClient = mock(DbClient.class);

  private GitHubInstallationReadTokenPort underTest;

  @BeforeEach
  void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.almSettingDao()).thenReturn(almSettingDao);
    when(dbClient.projectAlmSettingDao()).thenReturn(projectAlmSettingDao);
    when(setting.getAlm()).thenReturn(ALM.GITHUB);
    when(setting.getUuid()).thenReturn("setting-uuid");
    when(setting.getUpdatedAt()).thenReturn(1L);
    when(setting.getAppId()).thenReturn("123");
    when(setting.getClientId()).thenReturn("client-id");
    when(setting.getClientSecret()).thenReturn("client-secret");
    when(setting.getPrivateKey()).thenReturn(PRIVATE_KEY);
    when(setting.getUrl()).thenReturn("https://api.github.com");
    when(almSettingDao.selectByKey(dbSession, SETTING_KEY)).thenReturn(Optional.of(setting));
    when(projectAlmSettingDao.selectByAlmSettingAndRepoIgnoringCase(eq(dbSession), eq(setting), anyString()))
      .thenReturn(List.of(mock(ProjectAlmSettingDto.class)));
    when(githubClient.getInstallationId(any(), anyString())).thenReturn(Optional.of(INSTALLATION_ID));
    when(githubClient.createAppInstallationToken(any(), anyLong(), anyString(), anyMap())).thenAnswer(invocation -> Optional.of(token(Duration.ofHours(1))));
    underTest = new GitHubInstallationReadTokenPort(dbClient, new GithubGlobalSettingsValidator(endpointChecker, new MapSettings()), githubClient, clock);
  }

  @Test
  void readToken_whenRepositoryIsBound_shouldMintATokenNarrowedToTheRepositoryAndToReadingPullRequests() {
    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).contains(TOKEN);

    verify(githubClient).getInstallationId(any(GithubAppConfiguration.class), eq(REPOSITORY));
    verify(githubClient).createAppInstallationToken(any(GithubAppConfiguration.class), eq(INSTALLATION_ID), eq("Repo"), eq(Map.of("pull_requests", "read")));
  }

  @Test
  void readToken_whenSettingIsGhes_shouldUseTheConfiguredApiUrlAndDecryptedKey() {
    String encryptedKey = "{aes-gcm}encrypted-private-key";
    Settings settings = mock(Settings.class);
    Encryption encryption = mock(Encryption.class);
    when(settings.getEncryption()).thenReturn(encryption);
    when(encryption.isEncrypted(encryptedKey)).thenReturn(true);
    when(encryption.decrypt(encryptedKey)).thenReturn(PRIVATE_KEY);
    when(setting.getPrivateKey()).thenReturn(encryptedKey);
    when(setting.getUrl()).thenReturn("https://ghe.example.com/api/v3/");
    underTest = new GitHubInstallationReadTokenPort(dbClient, new GithubGlobalSettingsValidator(endpointChecker, settings), githubClient, clock);

    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).contains(TOKEN);

    verify(githubClient).createAppInstallationToken(
      argThat(configuration -> "https://ghe.example.com/api/v3".equals(configuration.getApiEndpoint())
        && PRIVATE_KEY.equals(configuration.getPrivateKey()) && configuration.getId() == 123L),
      eq(INSTALLATION_ID), eq("Repo"), anyMap());
  }

  @Test
  void readToken_whenRepositoryIsNotBoundToTheSetting_shouldReturnEmptyWithoutCallingGitHub() {
    when(projectAlmSettingDao.selectByAlmSettingAndRepoIgnoringCase(eq(dbSession), eq(setting), anyString())).thenReturn(List.of());

    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).isEmpty();

    verifyNoInteractions(githubClient);
  }

  @Test
  void readToken_whenSettingIsUnknown_shouldReturnEmpty() {
    assertThat(underTest.readToken("unknown", REPOSITORY)).isEmpty();

    verifyNoInteractions(githubClient);
  }

  @Test
  void readToken_whenSettingIsNotGitHub_shouldReturnEmptyWithoutCheckingBindings() {
    when(setting.getAlm()).thenReturn(ALM.GITLAB);

    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).isEmpty();

    verifyNoInteractions(githubClient, projectAlmSettingDao);
  }

  @Test
  void readToken_whenAppCredentialsAreMissing_shouldReturnEmpty() {
    when(setting.getAppId()).thenReturn(null);

    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).isEmpty();

    verifyNoInteractions(githubClient);
  }

  @Test
  void readToken_whenRepositoryIsNotOwnerSlashName_shouldReturnEmptyWithoutAnyLookup() {
    assertThat(underTest.readToken(SETTING_KEY, "../../orgs/x")).isEmpty();
    assertThat(underTest.readToken(SETTING_KEY, "no-slash")).isEmpty();
    assertThat(underTest.readToken(SETTING_KEY, "")).isEmpty();

    verifyNoInteractions(githubClient, almSettingDao);
  }

  @Test
  void readToken_whenRepositoryDiffersOnlyByCase_shouldLookItUpIgnoringCaseAndShareTheCachedToken() {
    assertThat(underTest.readToken(SETTING_KEY, "Owner/Repo")).contains(TOKEN);
    assertThat(underTest.readToken(SETTING_KEY, "owner/repo")).contains(TOKEN);

    verify(projectAlmSettingDao).selectByAlmSettingAndRepoIgnoringCase(dbSession, setting, "owner/repo");
    verify(githubClient, times(1)).createAppInstallationToken(any(), anyLong(), anyString(), anyMap());
  }

  @Test
  void readToken_whenAppIsNotInstalledOnTheRepository_shouldReturnEmpty() {
    when(githubClient.getInstallationId(any(), anyString())).thenReturn(Optional.empty());

    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).isEmpty();

    verify(githubClient, never()).createAppInstallationToken(any(), anyLong(), anyString(), anyMap());
  }

  @Test
  void readToken_whenInstallationLookupFails_shouldReturnEmpty() {
    when(githubClient.getInstallationId(any(), anyString())).thenThrow(new IllegalArgumentException("Bad credentials " + PRIVATE_KEY));

    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).isEmpty();
  }

  @Test
  void readToken_whenMintingReturnsNothing_shouldReturnEmptyAndNotCacheTheFailure() {
    when(githubClient.createAppInstallationToken(any(), anyLong(), anyString(), anyMap()))
      .thenReturn(Optional.empty())
      .thenAnswer(invocation -> Optional.of(token(Duration.ofHours(1))));

    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).isEmpty();
    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).contains(TOKEN);
  }

  @Test
  void readToken_whenMintingThrows_shouldReturnEmpty() {
    when(githubClient.createAppInstallationToken(any(), anyLong(), anyString(), anyMap())).thenThrow(new IllegalStateException(TOKEN));

    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).isEmpty();
  }

  @Test
  void readToken_whenDatabaseFails_shouldReturnEmpty() {
    when(almSettingDao.selectByKey(dbSession, SETTING_KEY)).thenThrow(new IllegalStateException("db down"));

    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).isEmpty();
  }

  @Test
  void readToken_whenReadTwice_shouldMintOnce() {
    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).contains(TOKEN);
    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).contains(TOKEN);

    verify(githubClient, times(1)).getInstallationId(any(), anyString());
    verify(githubClient, times(1)).createAppInstallationToken(any(), anyLong(), anyString(), anyMap());
  }

  @Test
  void readToken_whenCachedAndRepositoryIsNoLongerBound_shouldReturnEmpty() {
    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).contains(TOKEN);
    when(projectAlmSettingDao.selectByAlmSettingAndRepoIgnoringCase(eq(dbSession), eq(setting), anyString())).thenReturn(List.of());

    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).isEmpty();
  }

  @Test
  void readToken_whenSettingWasUpdated_shouldNotReuseTheTokenOfTheOldCredentials() {
    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).contains(TOKEN);
    when(setting.getUpdatedAt()).thenReturn(2L);

    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).contains(TOKEN);

    verify(githubClient, times(2)).createAppInstallationToken(any(), anyLong(), anyString(), anyMap());
  }

  @Test
  void readToken_whenCachedTokenIsWithinFiveMinutesOfItsExpiry_shouldMintANewOne() {
    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).contains(TOKEN);

    clock.advance(Duration.ofMinutes(54));
    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).contains(TOKEN);
    verify(githubClient, times(1)).createAppInstallationToken(any(), anyLong(), anyString(), anyMap());

    clock.advance(Duration.ofMinutes(2));
    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).contains(TOKEN);
    verify(githubClient, times(2)).createAppInstallationToken(any(), anyLong(), anyString(), anyMap());
  }

  @Test
  void readToken_whenManyRepositoriesAreRead_shouldStayUsable() {
    for (int i = 0; i < 1_200; i++) {
      assertThat(underTest.readToken(SETTING_KEY, "owner/repo-" + i)).contains(TOKEN);
    }
    clock.advance(Duration.ofMinutes(56));

    assertThat(underTest.readToken(SETTING_KEY, "owner/repo-1")).contains(TOKEN);
  }

  @Test
  void readToken_shouldNeverLogTheTokenTheKeyOrTheMessageOfAFailure() {
    underTest.readToken(SETTING_KEY, REPOSITORY);
    underTest.readToken(SETTING_KEY, REPOSITORY);
    underTest.readToken("unknown", REPOSITORY);
    underTest.readToken(SETTING_KEY, "bad repo\r\nforged line");
    doThrow(new IllegalArgumentException("Bad credentials " + PRIVATE_KEY + " " + TOKEN)).when(githubClient).getInstallationId(any(), anyString());
    underTest.readToken(SETTING_KEY, "owner/other");
    doReturn(Optional.of(INSTALLATION_ID)).when(githubClient).getInstallationId(any(), anyString());
    doThrow(new IllegalStateException(TOKEN)).when(githubClient).createAppInstallationToken(any(), anyLong(), anyString(), anyMap());
    underTest.readToken(SETTING_KEY, "owner/third");

    String logs = logTester.getLogs().stream().map(log -> log.getFormattedMsg() + " " + log.getThrowable()).collect(Collectors.joining("\n"));
    assertThat(logs)
      .isNotBlank()
      .doesNotContain(TOKEN)
      .doesNotContain("ghs_")
      .doesNotContain(PRIVATE_KEY)
      .doesNotContain("Bad credentials")
      .doesNotContain("\r")
      .doesNotContain("\nforged line")
      .contains("bad repo__forged line");
  }

  @Test
  void readToken_whenSomethingFailsUnexpectedly_shouldLogWhereItWasThrownButNotItsMessage() {
    doThrow(new IllegalArgumentException("Bad credentials " + PRIVATE_KEY)).when(githubClient).getInstallationId(any(), anyString());

    assertThat(underTest.readToken(SETTING_KEY, REPOSITORY)).isEmpty();

    String logs = logTester.getLogs(Level.WARN).stream().map(log -> log.getFormattedMsg()).collect(Collectors.joining("\n"));
    assertThat(logs)
      .contains("IllegalArgumentException thrown at ")
      .contains("GitHubInstallationReadTokenPort.mint")
      .doesNotContain("Bad credentials")
      .doesNotContain(PRIVATE_KEY);
  }

  private ExpiringAppInstallationToken token(Duration validity) {
    return new ExpiringAppInstallationToken(clock, "42", TOKEN, clock.instant().plus(validity).atOffset(ZoneOffset.UTC).toString());
  }

  private static final class MutableClock extends Clock {
    private Instant now;

    private MutableClock(Instant now) {
      this.now = now;
    }

    private void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
