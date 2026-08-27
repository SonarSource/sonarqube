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
 * imported into SonarQube. This mirrors the onboarding dashboard's own adapters (OnboardingAlmConfiguration,
 * server/sonar-webserver-webapi-v2), which can't be reused directly here: that configuration is a Spring bean
 * in the web-only context, while telemetry providers are resolved from the Pico container, so each side adapts
 * its own native ALM setting type independently.
 *
 * <p>Per-ALM call cost differs, based on what each platform's API actually exposes — not a uniform "count-only"
 * guarantee, and not a uniform single-call cost either:
 * <ul>
 *   <li>GitLab: a single {@code pageSize=1} call whose response carries a genuine total-across-all-pages
 *   field. Only one real repository object is ever returned.</li>
 *   <li>GitHub: also count-only, but the total is scoped per GitHub App installation, so the real cost is
 *   one {@code getWhitelistedGithubAppInstallations} call plus a token exchange and a {@code pageSize=1}
 *   listing call for <em>each</em> installation (1+2N calls, not a single call).</li>
 *   <li>Bitbucket Cloud: also count-only, but two calls per setting rather than one — it never has a static
 *   PAT on the setting, only an OAuth consumer ({@code clientId}/{@code clientSecret}), so
 *   {@link #countBitbucketCloudRepos} first exchanges that consumer for a short-lived Bearer token via
 *   {@link BitbucketCloudRestClient#createAccessToken} (the same workspace-wide, no-user-context mechanism
 *   already used by PR decoration and live binding resolution elsewhere in this codebase), then makes the
 *   {@code pagelen=1} count call with it.</li>
 *   <li>Bitbucket Server: its paginated response envelope has no total-count field at all (only per-page
 *   {@code size} plus {@code isLastPage}), so getting a count means paginating through the full listing and
 *   summing page sizes — no artificial page cap, since a legitimately large install should still get an
 *   accurate count; the only thing that stops the loop early is a non-advancing {@code nextPageStart},
 *   which means the server is stuck rather than genuinely still paginating.</li>
 *   <li>Azure DevOps: the Git Repositories List API supports no pagination or limiting parameter whatsoever —
 *   every call returns the platform's entire repository listing, full metadata included, in one response.
 *   There is no lighter-weight endpoint to fall back to; this is a platform limitation, not an implementation
 *   gap.</li>
 * </ul>
 *
 * <p>Daily granularity, per the SQS LTA Onboarding Telemetry Gap Sheet's target cadence for this metric.
 * Note the live-API-call cost above (whether 1 call or many, per the breakdown above) is paid once per
 * configured ALM setting on every daily cycle — the sheet's own open questions flag that this load hasn't
 * been confirmed safe at scale for installations with many ALM settings, so revisit this cadence if that
 * turns out to be a problem in practice.
 */
@ServerSide
public class TelemetryOnboardingDiscoveredRepoCountByAlmProvider extends AbstractTelemetryDataProvider<Integer> {

  public static final String METRIC_KEY = "onboarding_discovered_repo_count_by_alm";

  private static final Logger LOG = LoggerFactory.getLogger(TelemetryOnboardingDiscoveredRepoCountByAlmProvider.class);
  private static final int PAGE_SIZE = 100;

  /**
   * GitLab's projects listing has been reported to 500 when {@code membership=true} is combined with a
   * {@code per_page} under 12 (https://gitlab.com/gitlab-org/gitlab/-/issues/334667); the count-only call only
   * reads the {@code X-Total} header regardless of how many project objects come back in the body, so asking
   * for a page comfortably above that threshold costs nothing but may avoid the 500.
   */
  private static final int GITLAB_COUNT_PAGE_SIZE = 20;

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
      if (token.isPresent()) {
        var repos = githubClient.listRepositories(url, token.get(), installation.organizationName(), null, 1, 1);
        if (repos != null) {
          total += repos.getTotal();
        }
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
    var result = gitlabClient.searchProjects(url, pat, null, 1, GITLAB_COUNT_PAGE_SIZE);
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
    while (true) {
      var page = bitbucketServerClient.getRepos(url, pat, null, null, start, PAGE_SIZE);
      total += page.getValues().size();
      if (page.isLastPage() || page.getNextPageStart() <= start) {
        break;
      }
      start = page.getNextPageStart();
    }
    return OptionalLong.of(total);
  }

  private OptionalLong countBitbucketCloudRepos(AlmSettingDto setting) {
    String workspace = setting.getAppId();
    String clientId = setting.getClientId();
    String clientSecret = setting.getDecryptedClientSecret(encryption);
    if (workspace == null || clientId == null || clientSecret == null) {
      return OptionalLong.empty();
    }
    String accessToken = bitbucketCloudClient.createAccessToken(clientId, clientSecret);
    var repoList = bitbucketCloudClient.searchReposWithAccessToken(accessToken, workspace, null, 1, 1);
    Integer size = repoList != null ? repoList.getSize() : null;
    return size != null ? OptionalLong.of(size) : OptionalLong.empty();
  }
}
