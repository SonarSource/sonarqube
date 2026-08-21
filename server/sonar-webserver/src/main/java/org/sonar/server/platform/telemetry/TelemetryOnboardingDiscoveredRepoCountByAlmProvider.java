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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.telemetry.core.AbstractTelemetryDataProvider;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

/**
 * Repository counts as reported directly by each configured DevOps platform, including repositories never
 * imported into SonarQube — one cheap, count-only API call per configured ALM setting, never a full
 * repository listing (see the per-ALM helpers below). This mirrors the onboarding dashboard's own adapters
 * (OnboardingAlmConfiguration, server/sonar-webserver-webapi-v2), which can't be reused directly here: that
 * configuration is a Spring bean in the web-only context, while telemetry providers are resolved from the
 * Pico container, so each side adapts its own native ALM setting type independently.
 *
 * <p>Daily granularity, per the SQS LTA Onboarding Telemetry Gap Sheet's target cadence for this metric.
 * Note this still makes one live API call per configured ALM setting on every daily cycle — the sheet's own
 * open questions flag that this load hasn't been confirmed safe at scale for installations with many ALM
 * settings, so revisit this cadence if that turns out to be a problem in practice.
 */
@ServerSide
public class TelemetryOnboardingDiscoveredRepoCountByAlmProvider extends AbstractTelemetryDataProvider<Integer> {

  public static final String METRIC_KEY = "onboarding_discovered_repo_count_by_alm";

  private static final Logger LOG = LoggerFactory.getLogger(TelemetryOnboardingDiscoveredRepoCountByAlmProvider.class);
  private static final int PAGE_SIZE = 100;
  /** Safety bound so a misbehaving endpoint (non-advancing pagination) can't stall the telemetry cycle indefinitely. */
  private static final int MAX_PAGES = 1000;

  private final DbClient dbClient;
  private final Encryption encryption;
  private final GithubApplicationClient githubClient;
  private final GitlabApplicationClient gitlabClient;
  private final AzureDevOpsHttpClient azureClient;
  private final BitbucketServerRestClient bitbucketServerClient;
  private final BitbucketCloudRestClient bitbucketCloudClient;

  public TelemetryOnboardingDiscoveredRepoCountByAlmProvider(DbClient dbClient, Settings settings,
    GithubApplicationClient githubClient, GitlabApplicationClient gitlabClient, AzureDevOpsHttpClient azureClient,
    BitbucketServerRestClient bitbucketServerClient, BitbucketCloudRestClient bitbucketCloudClient) {
    super(METRIC_KEY, Dimension.INSTALLATION, Granularity.DAILY, TelemetryDataType.INTEGER);
    this.dbClient = dbClient;
    this.encryption = settings.getEncryption();
    this.githubClient = githubClient;
    this.gitlabClient = gitlabClient;
    this.azureClient = azureClient;
    this.bitbucketServerClient = bitbucketServerClient;
    this.bitbucketCloudClient = bitbucketCloudClient;
  }

  @Override
  public Map<String, Integer> getValues() {
    List<AlmSettingDto> settings;
    try (DbSession dbSession = dbClient.openSession(false)) {
      settings = dbClient.almSettingDao().selectAll(dbSession);
    }

    Map<String, Long> totalByAlm = new HashMap<>();
    for (AlmSettingDto setting : settings) {
      try {
        OptionalLong count = countRepos(setting);
        if (count.isPresent()) {
          totalByAlm.merge(setting.getAlm().getId(), count.getAsLong(), Long::sum);
        }
      } catch (RuntimeException e) {
        LOG.warn("Failed to count repos for ALM setting '{}' (alm={}); skipping", setting.getKey(), setting.getRawAlm(), e);
      }
    }
    Map<String, Integer> counts = new HashMap<>();
    totalByAlm.forEach((alm, total) -> counts.put(alm, total.intValue()));
    return counts;
  }

  private OptionalLong countRepos(AlmSettingDto setting) {
    return switch (setting.getAlm()) {
      case GITHUB -> countGithubRepos(setting);
      case GITLAB -> countGitlabRepos(setting);
      case AZURE_DEVOPS -> countAzureRepos(setting);
      case BITBUCKET -> countBitbucketServerRepos(setting);
      case BITBUCKET_CLOUD -> countBitbucketCloudRepos(setting);
    };
  }

  private OptionalLong countGithubRepos(AlmSettingDto setting) {
    String rawAppId = setting.getAppId();
    String privateKey = setting.getDecryptedPrivateKey(encryption);
    String url = setting.getUrl();
    if (rawAppId == null || privateKey == null || url == null) {
      return OptionalLong.empty();
    }
    var config = new GithubAppConfiguration(Long.parseLong(rawAppId.trim()), privateKey, url);
    long total = 0;
    for (var installation : githubClient.getWhitelistedGithubAppInstallations(config)) {
      var token = githubClient.createAppInstallationToken(config, Long.parseLong(installation.installationId()));
      if (token.isEmpty()) {
        continue;
      }
      var repos = githubClient.listRepositories(url, token.get(), installation.organizationName(), null, 1, 1);
      if (repos != null) {
        total += repos.getTotal();
      }
    }
    return OptionalLong.of(total);
  }

  private OptionalLong countGitlabRepos(AlmSettingDto setting) {
    String url = setting.getUrl();
    String pat = setting.getDecryptedPersonalAccessToken(encryption);
    if (url == null || pat == null) {
      return OptionalLong.empty();
    }
    var result = gitlabClient.searchProjects(url, pat, null, 1, 1);
    return result.getTotal() != null ? OptionalLong.of(result.getTotal()) : OptionalLong.empty();
  }

  private OptionalLong countAzureRepos(AlmSettingDto setting) {
    String url = setting.getUrl();
    String pat = setting.getDecryptedPersonalAccessToken(encryption);
    if (url == null || pat == null) {
      return OptionalLong.empty();
    }
    var repoList = azureClient.getRepos(url, pat, null);
    return repoList != null ? OptionalLong.of(repoList.getValues().size()) : OptionalLong.empty();
  }

  private OptionalLong countBitbucketServerRepos(AlmSettingDto setting) {
    String url = setting.getUrl();
    String pat = setting.getDecryptedPersonalAccessToken(encryption);
    if (url == null || pat == null) {
      return OptionalLong.empty();
    }
    long total = 0;
    int start = 0;
    for (int i = 0; i < MAX_PAGES; i++) {
      var page = bitbucketServerClient.getRepos(url, pat, null, null, start, PAGE_SIZE);
      total += page.getValues().size();
      if (page.isLastPage() || page.getNextPageStart() <= start) {
        break;
      }
      start = page.getNextPageStart();
    }
    return OptionalLong.of(total);
  }

  /** Bitbucket Cloud's RepositoryList has no total-count field, so this still paginates — but only to count pages, never to collect a listing. */
  private OptionalLong countBitbucketCloudRepos(AlmSettingDto setting) {
    String workspace = setting.getAppId();
    String pat = setting.getDecryptedPersonalAccessToken(encryption);
    if (workspace == null || pat == null) {
      return OptionalLong.empty();
    }
    long total = 0;
    int page = 1;
    boolean hasMore = true;
    while (hasMore && page <= MAX_PAGES) {
      var repoList = bitbucketCloudClient.searchRepos(pat, workspace, null, page, PAGE_SIZE);
      boolean hasValues = repoList.getValues() != null && !repoList.getValues().isEmpty();
      hasMore = hasValues && repoList.getNext() != null;
      if (hasValues) {
        total += repoList.getValues().size();
      }
      page++;
    }
    return total > 0 ? OptionalLong.of(total) : OptionalLong.empty();
  }
}
