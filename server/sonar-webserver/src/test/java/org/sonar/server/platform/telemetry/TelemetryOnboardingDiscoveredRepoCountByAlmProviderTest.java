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
package org.sonar.server.platform.telemetry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureRepo;
import org.sonar.alm.client.azure.GsonAzureRepoList;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.bitbucketserver.Repository;
import org.sonar.alm.client.bitbucketserver.RepositoryList;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.ProjectList;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.auth.github.ExpiringAppInstallationToken;
import org.sonar.auth.github.GithubAppInstallation;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

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

class TelemetryOnboardingDiscoveredRepoCountByAlmProviderTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final AlmSettingDao almSettingDao = mock(AlmSettingDao.class);
  private final Encryption encryption = mock(Encryption.class);
  private final GithubApplicationClient githubClient = mock(GithubApplicationClient.class);
  private final GitlabApplicationClient gitlabClient = mock(GitlabApplicationClient.class);
  private final AzureDevOpsHttpClient azureClient = mock(AzureDevOpsHttpClient.class);
  private final BitbucketServerRestClient bitbucketServerClient = mock(BitbucketServerRestClient.class);
  private final BitbucketCloudRestClient bitbucketCloudClient = mock(BitbucketCloudRestClient.class);

  private final TelemetryOnboardingDiscoveredRepoCountByAlmProvider underTest = new TelemetryOnboardingDiscoveredRepoCountByAlmProvider(
    dbClient, mockSettings(), githubClient, gitlabClient, azureClient, bitbucketServerClient, bitbucketCloudClient);

  private Settings mockSettings() {
    Settings settings = mock(Settings.class);
    when(settings.getEncryption()).thenReturn(encryption);
    return settings;
  }

  private void withSettings(AlmSettingDto... settings) {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.almSettingDao()).thenReturn(almSettingDao);
    when(almSettingDao.selectAll(dbSession)).thenReturn(List.of(settings));
  }

  @Test
  void getMetricKey_returnsCorrectKey() {
    assertThat(underTest.getMetricKey()).isEqualTo("onboarding_discovered_repo_count_by_alm");
  }

  @Test
  void getDimension_returnsInstallation() {
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
  }

  @Test
  void getGranularity_returnsDaily() {
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.DAILY);
  }

  @Test
  void getType_returnsInteger() {
    assertThat(underTest.getType()).isEqualTo(TelemetryDataType.INTEGER);
  }

  @Test
  void getValues_whenNoAlmSettings_shouldReturnEmptyMap() {
    withSettings();

    assertThat(underTest.getValues()).isEmpty();
  }

  @Test
  void getValues_whenGithubConfigured_shouldCountRepos() {
    GithubAppInstallation installation = new GithubAppInstallation("42", "acme", null, false);
    when(githubClient.getWhitelistedGithubAppInstallations(any())).thenReturn(List.of(installation));
    when(githubClient.createAppInstallationToken(any(), anyLong())).thenReturn(Optional.of(mock(ExpiringAppInstallationToken.class)));
    when(githubClient.listRepositories(any(), any(), any(), any(), anyInt(), anyInt()))
      .thenReturn(new GithubApplicationClient.Repositories().setTotal(4));
    withSettings(almSetting(ALM.GITHUB, "https://api.github.com").setAppId("123").setPrivateKey("key"));

    assertThat(underTest.getValues()).containsExactly(Map.entry("github", 4));
  }

  @Test
  void getValues_whenGithubRequiredFieldMissing_shouldSkipSetting() {
    withSettings(almSetting(ALM.GITHUB, "https://api.github.com"));

    assertThat(underTest.getValues()).isEmpty();
  }

  @Test
  void getValues_whenGitlabConfigured_shouldCountRepos() {
    when(gitlabClient.searchProjects(any(), any(), any(), any(), any())).thenReturn(new ProjectList(List.of(), 1, 1, 12));
    withSettings(almSetting(ALM.GITLAB, "https://gitlab.com").setPersonalAccessToken("pat"));

    assertThat(underTest.getValues()).containsExactly(Map.entry("gitlab", 12));
  }

  @Test
  void getValues_whenAzureConfigured_shouldCountRepos() {
    when(azureClient.getRepos(any(), any(), any()))
      .thenReturn(new GsonAzureRepoList(List.of(mock(GsonAzureRepo.class), mock(GsonAzureRepo.class))));
    withSettings(almSetting(ALM.AZURE_DEVOPS, "https://dev.azure.com/org").setPersonalAccessToken("pat"));

    assertThat(underTest.getValues()).containsExactly(Map.entry("azure_devops", 2));
  }

  @Test
  void getValues_whenBitbucketServerConfigured_shouldSumAcrossPages() {
    var firstPage = new RepositoryList(false, 100, 100, repeatedBitbucketServerRepos(100));
    var secondPage = new RepositoryList(true, 0, 7, repeatedBitbucketServerRepos(7));
    when(bitbucketServerClient.getRepos(any(), any(), any(), any(), any(), anyInt())).thenReturn(firstPage).thenReturn(secondPage);
    withSettings(almSetting(ALM.BITBUCKET, "https://bb.example.com").setPersonalAccessToken("pat"));

    assertThat(underTest.getValues()).containsExactly(Map.entry("bitbucket", 107));
  }

  @Test
  void getValues_whenBitbucketCloudConfigured_shouldExchangeConsumerForAccessTokenThenCount() {
    when(bitbucketCloudClient.createAccessToken("client-id", "client-secret")).thenReturn("access-token");
    var repoList = new org.sonar.alm.client.bitbucket.bitbucketcloud.RepositoryList(
      null, repeatedBitbucketCloudRepos(1), 1, 1, 102);
    when(bitbucketCloudClient.searchReposWithAccessToken(any(), any(), any(), any(), any())).thenReturn(repoList);
    withSettings(almSetting(ALM.BITBUCKET_CLOUD, null).setAppId("workspace").setClientId("client-id").setClientSecret("client-secret"));

    assertThat(underTest.getValues()).containsExactly(Map.entry("bitbucket_cloud", 102));
    verify(bitbucketCloudClient, times(1)).searchReposWithAccessToken(eq("access-token"), any(), any(), eq(1), eq(1));
  }

  @Test
  void getValues_whenBitbucketCloudSizeMissing_shouldSkipSetting() {
    when(bitbucketCloudClient.createAccessToken(any(), any())).thenReturn("access-token");
    var repoList = new org.sonar.alm.client.bitbucket.bitbucketcloud.RepositoryList(null, repeatedBitbucketCloudRepos(1), 1, 1);
    when(bitbucketCloudClient.searchReposWithAccessToken(any(), any(), any(), any(), any())).thenReturn(repoList);
    withSettings(almSetting(ALM.BITBUCKET_CLOUD, null).setAppId("workspace").setClientId("client-id").setClientSecret("client-secret"));

    assertThat(underTest.getValues()).isEmpty();
  }

  @Test
  void getValues_whenBitbucketCloudConsumerMissing_shouldSkipSettingWithoutCallingClient() {
    withSettings(almSetting(ALM.BITBUCKET_CLOUD, null).setAppId("workspace"));

    assertThat(underTest.getValues()).isEmpty();
    verify(bitbucketCloudClient, never()).createAccessToken(any(), any());
  }

  @Test
  void getValues_whenBitbucketCloudWorkspaceMissing_shouldSkipSettingWithoutCallingClient() {
    withSettings(almSetting(ALM.BITBUCKET_CLOUD, null).setClientId("client-id").setClientSecret("client-secret"));

    assertThat(underTest.getValues()).isEmpty();
    verify(bitbucketCloudClient, never()).createAccessToken(any(), any());
  }

  @Test
  void getValues_whenTwoSettingsShareTheSameAlm_shouldSumThem() {
    when(gitlabClient.searchProjects(any(), any(), any(), any(), any()))
      .thenReturn(new ProjectList(List.of(), 1, 1, 3))
      .thenReturn(new ProjectList(List.of(), 1, 1, 5));
    withSettings(
      almSetting(ALM.GITLAB, "https://gitlab.com").setKey("gl1").setPersonalAccessToken("pat1"),
      almSetting(ALM.GITLAB, "https://gitlab-other.com").setKey("gl2").setPersonalAccessToken("pat2"));

    assertThat(underTest.getValues()).containsExactly(Map.entry("gitlab", 8));
  }

  @Test
  void getValues_whenOneSettingFailsAndAnotherSucceeds_shouldSkipTheFailingOneOnly() {
    when(gitlabClient.searchProjects(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("unreachable"));
    when(azureClient.getRepos(any(), any(), any())).thenReturn(new GsonAzureRepoList(List.of(mock(GsonAzureRepo.class))));
    withSettings(
      almSetting(ALM.GITLAB, "https://gitlab.com").setKey("gl1").setPersonalAccessToken("pat"),
      almSetting(ALM.AZURE_DEVOPS, "https://dev.azure.com/org").setKey("az1").setPersonalAccessToken("pat"));

    assertThat(underTest.getValues()).containsExactly(Map.entry("azure_devops", 1));
  }

  @Test
  void getValues_whenAlmSettingHasUnrecognizedAlmId_shouldSkipItWithoutThrowing() {
    when(azureClient.getRepos(any(), any(), any())).thenReturn(new GsonAzureRepoList(List.of(mock(GsonAzureRepo.class))));
    AlmSettingDto unrecognized = new AlmSettingDto().setRawAlm("some_future_alm_type").setKey("weird1").setUrl("https://example.com");
    withSettings(
      unrecognized,
      almSetting(ALM.AZURE_DEVOPS, "https://dev.azure.com/org").setKey("az1").setPersonalAccessToken("pat"));

    assertThat(underTest.getValues()).containsExactly(Map.entry("azure_devops", 1));
  }

  private static AlmSettingDto almSetting(ALM alm, String url) {
    return new AlmSettingDto().setAlm(alm).setUrl(url).setKey(alm.getId());
  }

  private static List<Repository> repeatedBitbucketServerRepos(int count) {
    return java.util.stream.Stream.generate(() -> mock(Repository.class)).limit(count).toList();
  }

  private static List<org.sonar.alm.client.bitbucket.bitbucketcloud.Repository> repeatedBitbucketCloudRepos(int count) {
    return java.util.stream.Stream.generate(() -> mock(org.sonar.alm.client.bitbucket.bitbucketcloud.Repository.class)).limit(count).toList();
  }
}
