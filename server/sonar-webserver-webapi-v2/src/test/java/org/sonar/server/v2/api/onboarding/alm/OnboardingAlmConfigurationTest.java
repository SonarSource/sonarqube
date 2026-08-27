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
package org.sonar.server.v2.api.onboarding.alm;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureRepo;
import org.sonar.alm.client.azure.GsonAzureRepoList;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.bitbucketserver.Repository;
import org.sonar.alm.client.bitbucketserver.RepositoryList;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.ProjectList;
import org.sonar.auth.github.ExpiringAppInstallationToken;
import org.sonar.auth.github.GithubAppInstallation;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonarsource.onboarding.server.db.OnboardingRows;
import org.sonarsource.onboarding.server.db.OnboardingSecretDecryptor;
import org.sonarsource.onboarding.shared.port.AlmRepoCountProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Every provider here answers only a count, never a listing (see {@link OnboardingAlmConfiguration}'s
 * class Javadoc), so these tests assert on {@code fetchTotalCount} alone: the null/absent-settings
 * short-circuits, the {@code decrypt() == null} short-circuit, pagination termination where the
 * provider paginates, and that a client exception degrades to {@link OptionalLong#empty()} rather
 * than propagating.
 */
class OnboardingAlmConfigurationTest {

  private static final OnboardingSecretDecryptor IDENTITY_DECRYPTOR = value -> value;

  private final OnboardingAlmConfiguration underTest = new OnboardingAlmConfiguration();

  // ── GitHub ────────────────────────────────────────────────────────────────────

  @Test
  void supportedAlm_whenGithubProvider_shouldReturnGithub() {
    assertThat(underTest.githubAlmRepoCountProvider(mock(GithubApplicationClient.class)).supportedAlm())
      .isEqualTo("github");
  }

  @ParameterizedTest
  @MethodSource("githubSettingsMissingARequiredField")
  void fetchTotalCount_whenGithubRequiredFieldMissing_shouldReturnEmpty(OnboardingRows.AlmSetting setting) {
    GithubApplicationClient client = mock(GithubApplicationClient.class);
    AlmRepoCountProvider provider = underTest.githubAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(setting, IDENTITY_DECRYPTOR)).isEmpty();
  }

  private static Stream<OnboardingRows.AlmSetting> githubSettingsMissingARequiredField() {
    return Stream.of(
      githubSetting(null, "key", "https://api.github.com"),
      githubSetting("123", null, "https://api.github.com"),
      githubSetting("123", "key", null));
  }

  @Test
  void fetchTotalCount_whenGithubDecryptReturnsNull_shouldReturnEmpty() {
    GithubApplicationClient client = mock(GithubApplicationClient.class);
    AlmRepoCountProvider provider = underTest.githubAlmRepoCountProvider(client);
    OnboardingRows.AlmSetting setting = githubSetting("123", "key", "https://api.github.com");

    assertThat(provider.fetchTotalCount(setting, value -> null)).isEmpty();
    verify(client, never()).getWhitelistedGithubAppInstallations(any());
  }

  @Test
  void fetchTotalCount_whenGithubHasNoInstallations_shouldReturnZero() {
    GithubApplicationClient client = mock(GithubApplicationClient.class);
    when(client.getWhitelistedGithubAppInstallations(any())).thenReturn(List.of());
    AlmRepoCountProvider provider = underTest.githubAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(githubSetting("123", "key", "https://api.github.com"), IDENTITY_DECRYPTOR))
      .isEqualTo(OptionalLong.of(0));
  }

  @Test
  void fetchTotalCount_whenGithubInstallationTokenAbsent_shouldSkipInstallation() {
    GithubApplicationClient client = mock(GithubApplicationClient.class);
    GithubAppInstallation installation = new GithubAppInstallation("42", "acme", null, false);
    when(client.getWhitelistedGithubAppInstallations(any())).thenReturn(List.of(installation));
    when(client.createAppInstallationToken(any(), anyLong())).thenReturn(Optional.empty());
    AlmRepoCountProvider provider = underTest.githubAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(githubSetting("123", "key", "https://api.github.com"), IDENTITY_DECRYPTOR))
      .isEqualTo(OptionalLong.of(0));
    verify(client, never()).listRepositories(any(), any(), any(), any(), anyInt(), anyInt());
  }

  @Test
  void fetchTotalCount_whenGithubHasMultipleInstallations_shouldSumTotals() {
    GithubApplicationClient client = mock(GithubApplicationClient.class);
    GithubAppInstallation installationOne = new GithubAppInstallation("1", "acme", null, false);
    GithubAppInstallation installationTwo = new GithubAppInstallation("2", "beta", null, false);
    ExpiringAppInstallationToken token = mock(ExpiringAppInstallationToken.class);
    when(client.getWhitelistedGithubAppInstallations(any())).thenReturn(List.of(installationOne, installationTwo));
    when(client.createAppInstallationToken(any(), anyLong())).thenReturn(Optional.of(token));
    when(client.listRepositories(any(), any(), any(), any(), anyInt(), anyInt()))
      .thenReturn(new GithubApplicationClient.Repositories().setTotal(3))
      .thenReturn(new GithubApplicationClient.Repositories().setTotal(5));
    AlmRepoCountProvider provider = underTest.githubAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(githubSetting("123", "key", "https://api.github.com"), IDENTITY_DECRYPTOR))
      .isEqualTo(OptionalLong.of(8));
  }

  @Test
  void fetchTotalCount_whenGithubClientThrows_shouldReturnEmpty() {
    GithubApplicationClient client = mock(GithubApplicationClient.class);
    when(client.getWhitelistedGithubAppInstallations(any())).thenThrow(new RuntimeException("unreachable"));
    AlmRepoCountProvider provider = underTest.githubAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(githubSetting("123", "key", "https://api.github.com"), IDENTITY_DECRYPTOR))
      .isEmpty();
  }

  // ── GitLab ──────────────────────────────────────────────────────────────────

  @Test
  void supportedAlm_whenGitlabProvider_shouldReturnGitlab() {
    assertThat(underTest.gitlabAlmRepoCountProvider(mock(GitlabApplicationClient.class)).supportedAlm())
      .isEqualTo("gitlab");
  }

  @Test
  void fetchTotalCount_whenGitlabUrlMissing_shouldReturnEmpty() {
    AlmRepoCountProvider provider = underTest.gitlabAlmRepoCountProvider(mock(GitlabApplicationClient.class));

    assertThat(provider.fetchTotalCount(gitlabSetting(null, "pat"), IDENTITY_DECRYPTOR)).isEmpty();
  }

  @Test
  void fetchTotalCount_whenGitlabPatMissing_shouldReturnEmpty() {
    AlmRepoCountProvider provider = underTest.gitlabAlmRepoCountProvider(mock(GitlabApplicationClient.class));

    assertThat(provider.fetchTotalCount(gitlabSetting("https://gitlab.com", null), IDENTITY_DECRYPTOR)).isEmpty();
  }

  @Test
  void fetchTotalCount_whenGitlabDecryptReturnsNull_shouldReturnEmpty() {
    GitlabApplicationClient client = mock(GitlabApplicationClient.class);
    AlmRepoCountProvider provider = underTest.gitlabAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(gitlabSetting("https://gitlab.com", "pat"), value -> null)).isEmpty();
    verify(client, never()).searchProjects(any(), any(), any(), any(), any());
  }

  @Test
  void fetchTotalCount_whenGitlabTotalPresent_shouldReturnTotal() {
    GitlabApplicationClient client = mock(GitlabApplicationClient.class);
    when(client.searchProjects(any(), any(), any(), any(), any())).thenReturn(new ProjectList(List.of(), 1, 1, 12));
    AlmRepoCountProvider provider = underTest.gitlabAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(gitlabSetting("https://gitlab.com", "pat"), IDENTITY_DECRYPTOR))
      .isEqualTo(OptionalLong.of(12));
  }

  /**
   * GitLab's projects endpoint has been reported to 500 when {@code membership=true} is combined with a
   * {@code per_page} under 12 (see {@link OnboardingAlmConfiguration}'s {@code GITLAB_COUNT_PAGE_SIZE} javadoc);
   * a count-only call can afford a slightly larger page since it only reads the {@code X-Total} header.
   */
  @Test
  void fetchTotalCount_whenGitlabCounting_shouldRequestSaferPageSize() {
    GitlabApplicationClient client = mock(GitlabApplicationClient.class);
    when(client.searchProjects(any(), any(), any(), any(), any())).thenReturn(new ProjectList(List.of(), 1, 1, 12));
    AlmRepoCountProvider provider = underTest.gitlabAlmRepoCountProvider(client);

    provider.fetchTotalCount(gitlabSetting("https://gitlab.com", "pat"), IDENTITY_DECRYPTOR);

    verify(client).searchProjects("https://gitlab.com", "pat", null, 1, 20);
  }

  @Test
  void fetchTotalCount_whenGitlabTotalNull_shouldReturnEmpty() {
    GitlabApplicationClient client = mock(GitlabApplicationClient.class);
    when(client.searchProjects(any(), any(), any(), any(), any())).thenReturn(new ProjectList(List.of(), 1, 1, null));
    AlmRepoCountProvider provider = underTest.gitlabAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(gitlabSetting("https://gitlab.com", "pat"), IDENTITY_DECRYPTOR)).isEmpty();
  }

  @Test
  void fetchTotalCount_whenGitlabClientThrows_shouldReturnEmpty() {
    GitlabApplicationClient client = mock(GitlabApplicationClient.class);
    when(client.searchProjects(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("unreachable"));
    AlmRepoCountProvider provider = underTest.gitlabAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(gitlabSetting("https://gitlab.com", "pat"), IDENTITY_DECRYPTOR)).isEmpty();
  }

  // ── Azure DevOps ────────────────────────────────────────────────────────────

  @Test
  void supportedAlm_whenAzureProvider_shouldReturnAzureDevops() {
    assertThat(underTest.azureDevOpsAlmRepoCountProvider(mock(AzureDevOpsHttpClient.class)).supportedAlm())
      .isEqualTo("azure_devops");
  }

  @Test
  void fetchTotalCount_whenAzureUrlMissing_shouldReturnEmpty() {
    AlmRepoCountProvider provider = underTest.azureDevOpsAlmRepoCountProvider(mock(AzureDevOpsHttpClient.class));

    assertThat(provider.fetchTotalCount(azureSetting(null, "pat"), IDENTITY_DECRYPTOR)).isEmpty();
  }

  @Test
  void fetchTotalCount_whenAzurePatMissing_shouldReturnEmpty() {
    AlmRepoCountProvider provider = underTest.azureDevOpsAlmRepoCountProvider(mock(AzureDevOpsHttpClient.class));

    assertThat(provider.fetchTotalCount(azureSetting("https://dev.azure.com/org", null), IDENTITY_DECRYPTOR)).isEmpty();
  }

  @Test
  void fetchTotalCount_whenAzureDecryptReturnsNull_shouldReturnEmpty() {
    AzureDevOpsHttpClient client = mock(AzureDevOpsHttpClient.class);
    AlmRepoCountProvider provider = underTest.azureDevOpsAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(azureSetting("https://dev.azure.com/org", "pat"), value -> null)).isEmpty();
    verify(client, never()).getRepos(any(), any(), any());
  }

  @Test
  void fetchTotalCount_whenAzureRepoListPresent_shouldReturnSize() {
    AzureDevOpsHttpClient client = mock(AzureDevOpsHttpClient.class);
    when(client.getRepos(any(), any(), any()))
      .thenReturn(new GsonAzureRepoList(List.of(mock(GsonAzureRepo.class), mock(GsonAzureRepo.class))));
    AlmRepoCountProvider provider = underTest.azureDevOpsAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(azureSetting("https://dev.azure.com/org", "pat"), IDENTITY_DECRYPTOR))
      .isEqualTo(OptionalLong.of(2));
  }

  @Test
  void fetchTotalCount_whenAzureRepoListNull_shouldReturnEmpty() {
    AzureDevOpsHttpClient client = mock(AzureDevOpsHttpClient.class);
    when(client.getRepos(any(), any(), any())).thenReturn(null);
    AlmRepoCountProvider provider = underTest.azureDevOpsAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(azureSetting("https://dev.azure.com/org", "pat"), IDENTITY_DECRYPTOR)).isEmpty();
  }

  @Test
  void fetchTotalCount_whenAzureClientThrows_shouldReturnEmpty() {
    AzureDevOpsHttpClient client = mock(AzureDevOpsHttpClient.class);
    when(client.getRepos(any(), any(), any())).thenThrow(new RuntimeException("unreachable"));
    AlmRepoCountProvider provider = underTest.azureDevOpsAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(azureSetting("https://dev.azure.com/org", "pat"), IDENTITY_DECRYPTOR)).isEmpty();
  }

  // ── Bitbucket Server ────────────────────────────────────────────────────────

  @Test
  void supportedAlm_whenBitbucketServerProvider_shouldReturnBitbucket() {
    assertThat(underTest.bitbucketServerAlmRepoCountProvider(mock(BitbucketServerRestClient.class)).supportedAlm())
      .isEqualTo("bitbucket");
  }

  @Test
  void fetchTotalCount_whenBitbucketServerUrlMissing_shouldReturnEmpty() {
    AlmRepoCountProvider provider = underTest.bitbucketServerAlmRepoCountProvider(mock(BitbucketServerRestClient.class));

    assertThat(provider.fetchTotalCount(bitbucketServerSetting(null, "pat"), IDENTITY_DECRYPTOR)).isEmpty();
  }

  @Test
  void fetchTotalCount_whenBitbucketServerPatMissing_shouldReturnEmpty() {
    AlmRepoCountProvider provider = underTest.bitbucketServerAlmRepoCountProvider(mock(BitbucketServerRestClient.class));

    assertThat(provider.fetchTotalCount(bitbucketServerSetting("https://bb.example.com", null), IDENTITY_DECRYPTOR)).isEmpty();
  }

  @Test
  void fetchTotalCount_whenBitbucketServerDecryptReturnsNull_shouldReturnEmpty() {
    BitbucketServerRestClient client = mock(BitbucketServerRestClient.class);
    AlmRepoCountProvider provider = underTest.bitbucketServerAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(bitbucketServerSetting("https://bb.example.com", "pat"), value -> null)).isEmpty();
    verify(client, never()).getRepos(any(), any(), any(), any(), any(), anyInt());
  }

  @Test
  void fetchTotalCount_whenBitbucketServerSinglePage_shouldReturnSize() {
    BitbucketServerRestClient client = mock(BitbucketServerRestClient.class);
    when(client.getRepos(any(), any(), any(), any(), any(), anyInt()))
      .thenReturn(new RepositoryList(true, 0, 2, repeatedBitbucketServerRepos(2)));
    AlmRepoCountProvider provider = underTest.bitbucketServerAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(bitbucketServerSetting("https://bb.example.com", "pat"), IDENTITY_DECRYPTOR))
      .isEqualTo(OptionalLong.of(2));
  }

  @Test
  void fetchTotalCount_whenBitbucketServerHasMultiplePages_shouldSumAcrossPagesAndStop() {
    BitbucketServerRestClient client = mock(BitbucketServerRestClient.class);
    var firstPage = new RepositoryList(false, 100, 100, repeatedBitbucketServerRepos(100));
    var secondPage = new RepositoryList(true, 0, 7, repeatedBitbucketServerRepos(7));
    when(client.getRepos(any(), any(), any(), any(), any(), anyInt())).thenReturn(firstPage).thenReturn(secondPage);
    AlmRepoCountProvider provider = underTest.bitbucketServerAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(bitbucketServerSetting("https://bb.example.com", "pat"), IDENTITY_DECRYPTOR))
      .isEqualTo(OptionalLong.of(107));
    verify(client, times(2)).getRepos(any(), any(), any(), any(), any(), anyInt());
  }

  @Test
  void fetchTotalCount_whenBitbucketServerClientThrows_shouldReturnEmpty() {
    BitbucketServerRestClient client = mock(BitbucketServerRestClient.class);
    when(client.getRepos(any(), any(), any(), any(), any(), anyInt())).thenThrow(new RuntimeException("unreachable"));
    AlmRepoCountProvider provider = underTest.bitbucketServerAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(bitbucketServerSetting("https://bb.example.com", "pat"), IDENTITY_DECRYPTOR)).isEmpty();
  }

  @Test
  void fetchTotalCount_whenBitbucketServerPaginationDoesNotAdvance_shouldStopAfterOnePage() {
    BitbucketServerRestClient client = mock(BitbucketServerRestClient.class);
    var stuckPage = new RepositoryList(false, 0, 5, repeatedBitbucketServerRepos(5));
    when(client.getRepos(any(), any(), any(), any(), any(), anyInt())).thenReturn(stuckPage);
    AlmRepoCountProvider provider = underTest.bitbucketServerAlmRepoCountProvider(client);

    assertThat(provider.fetchTotalCount(bitbucketServerSetting("https://bb.example.com", "pat"), IDENTITY_DECRYPTOR))
      .isEqualTo(OptionalLong.of(5));
    verify(client, times(1)).getRepos(any(), any(), any(), any(), any(), anyInt());
  }

  // ── Bitbucket Cloud ─────────────────────────────────────────────────────────

  @Test
  void supportedAlm_whenBitbucketCloudProvider_shouldReturnBitbucketCloud() {
    assertThat(underTest.bitbucketCloudAlmRepoCountProvider(mock(BitbucketCloudRestClient.class), mock(DbClient.class)).supportedAlm())
      .isEqualTo("bitbucket_cloud");
  }

  @Test
  void fetchTotalCount_whenBitbucketCloudWorkspaceMissing_shouldReturnEmpty() {
    AlmRepoCountProvider provider = underTest.bitbucketCloudAlmRepoCountProvider(mock(BitbucketCloudRestClient.class), mock(DbClient.class));

    assertThat(provider.fetchTotalCount(bitbucketCloudSetting(null, "pat"), IDENTITY_DECRYPTOR)).isEmpty();
  }

  /**
   * The realistic case: Bitbucket Cloud settings never carry a static PAT — only an OAuth consumer
   * stored on the native {@code AlmSettingDto}, which this provider resolves by key (since
   * {@code OnboardingRows.AlmSetting} doesn't carry a consumer at all) and exchanges for a Bearer
   * token before counting.
   */
  @Test
  void fetchTotalCount_whenBitbucketCloudConfigured_shouldExchangeConsumerForAccessTokenThenCount() {
    BitbucketCloudRestClient client = mock(BitbucketCloudRestClient.class);
    when(client.createAccessToken("client-id", "client-secret")).thenReturn("access-token");
    when(client.searchReposWithAccessToken(any(), any(), any(), any(), any()))
      .thenReturn(new org.sonar.alm.client.bitbucket.bitbucketcloud.RepositoryList(null, repeatedBitbucketCloudRepos(1), 1, 1, 55));
    DbClient dbClient = mock(DbClient.class);
    DbSession dbSession = mock(DbSession.class);
    AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.almSettingDao()).thenReturn(almSettingDao);
    AlmSettingDto storedSetting = new AlmSettingDto().setAlm(ALM.BITBUCKET_CLOUD).setKey("bbc").setAppId("workspace")
      .setClientId("client-id").setClientSecret("client-secret");
    when(almSettingDao.selectByKey(dbSession, "bbc")).thenReturn(Optional.of(storedSetting));
    AlmRepoCountProvider provider = underTest.bitbucketCloudAlmRepoCountProvider(client, dbClient);

    assertThat(provider.fetchTotalCount(bitbucketCloudSetting("workspace", null), IDENTITY_DECRYPTOR))
      .isEqualTo(OptionalLong.of(55));
    verify(client).searchReposWithAccessToken(eq("access-token"), any(), any(), eq(1), eq(1));
  }

  @Test
  void fetchTotalCount_whenBitbucketCloudSettingNotFoundInDb_shouldReturnEmpty() {
    BitbucketCloudRestClient client = mock(BitbucketCloudRestClient.class);
    DbClient dbClient = mock(DbClient.class);
    DbSession dbSession = mock(DbSession.class);
    AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.almSettingDao()).thenReturn(almSettingDao);
    when(almSettingDao.selectByKey(dbSession, "bbc")).thenReturn(Optional.empty());
    AlmRepoCountProvider provider = underTest.bitbucketCloudAlmRepoCountProvider(client, dbClient);

    assertThat(provider.fetchTotalCount(bitbucketCloudSetting("workspace", null), IDENTITY_DECRYPTOR)).isEmpty();
    verify(client, never()).createAccessToken(any(), any());
  }

  @Test
  void fetchTotalCount_whenBitbucketCloudConsumerMissing_shouldReturnEmpty() {
    BitbucketCloudRestClient client = mock(BitbucketCloudRestClient.class);
    DbClient dbClient = mock(DbClient.class);
    DbSession dbSession = mock(DbSession.class);
    AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.almSettingDao()).thenReturn(almSettingDao);
    AlmSettingDto storedSetting = new AlmSettingDto().setAlm(ALM.BITBUCKET_CLOUD).setKey("bbc").setAppId("workspace");
    when(almSettingDao.selectByKey(dbSession, "bbc")).thenReturn(Optional.of(storedSetting));
    AlmRepoCountProvider provider = underTest.bitbucketCloudAlmRepoCountProvider(client, dbClient);

    assertThat(provider.fetchTotalCount(bitbucketCloudSetting("workspace", null), IDENTITY_DECRYPTOR)).isEmpty();
    verify(client, never()).createAccessToken(any(), any());
  }

  @Test
  void fetchTotalCount_whenBitbucketCloudSizeMissing_shouldReturnEmpty() {
    BitbucketCloudRestClient client = mock(BitbucketCloudRestClient.class);
    when(client.createAccessToken(any(), any())).thenReturn("access-token");
    when(client.searchReposWithAccessToken(any(), any(), any(), any(), any()))
      .thenReturn(new org.sonar.alm.client.bitbucket.bitbucketcloud.RepositoryList(null, List.of(), 1, 1));
    DbClient dbClient = mock(DbClient.class);
    DbSession dbSession = mock(DbSession.class);
    AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.almSettingDao()).thenReturn(almSettingDao);
    AlmSettingDto storedSetting = new AlmSettingDto().setAlm(ALM.BITBUCKET_CLOUD).setKey("bbc").setAppId("workspace")
      .setClientId("client-id").setClientSecret("client-secret");
    when(almSettingDao.selectByKey(dbSession, "bbc")).thenReturn(Optional.of(storedSetting));
    AlmRepoCountProvider provider = underTest.bitbucketCloudAlmRepoCountProvider(client, dbClient);

    assertThat(provider.fetchTotalCount(bitbucketCloudSetting("workspace", null), IDENTITY_DECRYPTOR)).isEmpty();
  }

  @Test
  void fetchTotalCount_whenBitbucketCloudClientThrows_shouldReturnEmpty() {
    BitbucketCloudRestClient client = mock(BitbucketCloudRestClient.class);
    DbClient dbClient = mock(DbClient.class);
    DbSession dbSession = mock(DbSession.class);
    AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.almSettingDao()).thenReturn(almSettingDao);
    AlmSettingDto storedSetting = new AlmSettingDto().setAlm(ALM.BITBUCKET_CLOUD).setKey("bbc").setAppId("workspace")
      .setClientId("client-id").setClientSecret("client-secret");
    when(almSettingDao.selectByKey(dbSession, "bbc")).thenReturn(Optional.of(storedSetting));
    when(client.createAccessToken(any(), any())).thenThrow(new RuntimeException("unreachable"));
    AlmRepoCountProvider provider = underTest.bitbucketCloudAlmRepoCountProvider(client, dbClient);

    assertThat(provider.fetchTotalCount(bitbucketCloudSetting("workspace", null), IDENTITY_DECRYPTOR)).isEmpty();
  }

  // ── Fixtures ────────────────────────────────────────────────────────────────

  private static List<Repository> repeatedBitbucketServerRepos(int count) {
    return Stream.generate(() -> mock(Repository.class)).limit(count).toList();
  }

  private static List<org.sonar.alm.client.bitbucket.bitbucketcloud.Repository> repeatedBitbucketCloudRepos(int count) {
    return Stream.generate(() -> mock(org.sonar.alm.client.bitbucket.bitbucketcloud.Repository.class)).limit(count).toList();
  }

  private static OnboardingRows.AlmSetting githubSetting(String appId, String privateKey, String url) {
    return new OnboardingRows.AlmSetting("gh", "github", url, appId, privateKey, null);
  }

  private static OnboardingRows.AlmSetting gitlabSetting(String url, String pat) {
    return new OnboardingRows.AlmSetting("gl", "gitlab", url, null, null, pat);
  }

  private static OnboardingRows.AlmSetting azureSetting(String url, String pat) {
    return new OnboardingRows.AlmSetting("az", "azure_devops", url, null, null, pat);
  }

  private static OnboardingRows.AlmSetting bitbucketServerSetting(String url, String pat) {
    return new OnboardingRows.AlmSetting("bbs", "bitbucket", url, null, null, pat);
  }

  private static OnboardingRows.AlmSetting bitbucketCloudSetting(String workspace, String pat) {
    return new OnboardingRows.AlmSetting("bbc", "bitbucket_cloud", null, workspace, null, pat);
  }
}
