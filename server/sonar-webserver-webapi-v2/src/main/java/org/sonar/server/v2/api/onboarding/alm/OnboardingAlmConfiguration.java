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

import java.util.OptionalLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonarsource.onboarding.server.db.OnboardingRows;
import org.sonarsource.onboarding.server.db.OnboardingSecretDecryptor;
import org.sonarsource.onboarding.shared.port.AlmRepoCountProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link AlmRepoCountProvider} beans for each ALM type by adapting the existing
 * {@code sonar-alm-client} clients already present in the sonar-enterprise Spring context.
 * These are wired into {@link org.sonarsource.onboarding.server.ServerAlmDiscoveryProvider}
 * via the {@code List<AlmRepoCountProvider>} constructor argument.
 *
 * <p>Every implementation here answers only a repository <em>count</em> — the onboarding dashboard never
 * enumerates discovered repositories. But per-ALM call cost differs based on what each platform's API
 * actually exposes, and isn't uniformly a single call either:
 * <ul>
 *   <li>GitLab: a single {@code pageSize=1} call whose response carries a genuine total-across-all-pages
 *   field. Only one real repository object is ever returned.</li>
 *   <li>GitHub: also count-only, but the total is scoped per GitHub App installation, so the real cost is
 *   one installations call plus a token exchange and a {@code pageSize=1} listing call for <em>each</em>
 *   installation (1+2N calls, not a single call).</li>
 *   <li>Bitbucket Cloud: also count-only, but two calls per setting rather than one. It never has a static
 *   PAT on the setting, only an OAuth consumer ({@code clientId}/{@code clientSecret}), exchanged at call
 *   time for a Bearer token (see {@link BitbucketCloudAlmRepoCountProvider}'s own Javadoc) — so this is the
 *   one ALM whose provider reaches into the database on its own rather than relying purely on the
 *   {@code OnboardingRows.AlmSetting} it's handed, which doesn't carry that OAuth consumer.</li>
 *   <li>Bitbucket Server: its paginated response envelope has no total-count field at all (only per-page
 *   {@code size} plus {@code isLastPage}), so getting a count means paginating through the full listing
 *   and summing page sizes — no artificial page cap, since a legitimately large install should still get
 *   an accurate count; the only thing that stops the loop early is a non-advancing {@code nextPageStart},
 *   which means the server is stuck rather than genuinely still paginating.</li>
 *   <li>Azure DevOps: the Git Repositories List API supports no pagination or limiting parameter
 *   whatsoever — every call returns the platform's entire repository listing, full metadata included, in
 *   one response. There is no lighter-weight endpoint to fall back to; this is a platform limitation, not
 *   an implementation gap.</li>
 * </ul>
 */
@Configuration
public class OnboardingAlmConfiguration {

  private static final Logger LOG = LoggerFactory.getLogger(OnboardingAlmConfiguration.class);
  private static final int PAGE_SIZE = 100;

  @Bean
  public AlmRepoCountProvider githubAlmRepoCountProvider(GithubApplicationClient githubClient) {
    return new GithubAlmRepoCountProvider(githubClient);
  }

  @Bean
  public AlmRepoCountProvider gitlabAlmRepoCountProvider(GitlabApplicationClient gitlabClient) {
    return new GitlabAlmRepoCountProvider(gitlabClient);
  }

  @Bean
  public AlmRepoCountProvider azureDevOpsAlmRepoCountProvider(AzureDevOpsHttpClient azureClient) {
    return new AzureDevOpsAlmRepoCountProvider(azureClient);
  }

  @Bean
  public AlmRepoCountProvider bitbucketServerAlmRepoCountProvider(BitbucketServerRestClient bbServerClient) {
    return new BitbucketServerAlmRepoCountProvider(bbServerClient);
  }

  @Bean
  public AlmRepoCountProvider bitbucketCloudAlmRepoCountProvider(BitbucketCloudRestClient bbCloudClient, DbClient dbClient) {
    return new BitbucketCloudAlmRepoCountProvider(bbCloudClient, dbClient);
  }

  private static final class GithubAlmRepoCountProvider implements AlmRepoCountProvider {

    private final GithubApplicationClient client;

    private GithubAlmRepoCountProvider(GithubApplicationClient client) {
      this.client = client;
    }

    @Override
    public String supportedAlm() {
      return "github";
    }

    @Override
    public OptionalLong fetchTotalCount(OnboardingRows.AlmSetting setting, OnboardingSecretDecryptor decryptor) {
      String rawAppId = setting.appId();
      String rawPrivateKey = setting.privateKey();
      String url = setting.url();
      if (rawAppId == null || rawPrivateKey == null || url == null) {
        return OptionalLong.empty();
      }
      try {
        long appId = Long.parseLong(rawAppId.trim());
        String privateKey = decryptor.decrypt(rawPrivateKey);
        if (privateKey == null) {
          return OptionalLong.empty();
        }
        var config = new GithubAppConfiguration(appId, privateKey, url);
        long total = 0;
        for (var installation : client.getWhitelistedGithubAppInstallations(config)) {
          var token = client.createAppInstallationToken(config, Long.parseLong(installation.installationId()));
          if (token.isEmpty()) {
            continue;
          }
          var repos = client.listRepositories(url, token.get(), installation.organizationName(), null, 1, 1);
          if (repos != null) {
            total += repos.getTotal();
          }
        }
        return OptionalLong.of(total);
      } catch (Exception e) {
        LOG.warn("Failed to count GitHub repos for ALM setting '{}'", setting.key(), e);
        return OptionalLong.empty();
      }
    }
  }

  /**
   * Common shape shared by every ALM whose count is fetched with a single access-token-bearing
   * client call, where that token comes straight off the setting itself: check the two required
   * setting fields once, decrypt the secret, then delegate to {@link #countRepos}. Fields are
   * captured into locals exactly once and never re-read off {@code setting} afterward, since a
   * value re-read after crossing another method call (the decrypt) can no longer be proven
   * non-null by the caller. Bitbucket Cloud does NOT extend this: its token never comes from the
   * setting at all (see {@link BitbucketCloudAlmRepoCountProvider}), so it implements
   * {@link AlmRepoCountProvider} directly instead.
   */
  private abstract static class PatAlmRepoCountProvider implements AlmRepoCountProvider {

    protected String primaryField(OnboardingRows.AlmSetting setting) {
      return setting.url();
    }

    protected String secretField(OnboardingRows.AlmSetting setting) {
      return setting.personalAccessToken();
    }

    protected abstract OptionalLong countRepos(String primary, String pat);

    @Override
    public OptionalLong fetchTotalCount(OnboardingRows.AlmSetting setting, OnboardingSecretDecryptor decryptor) {
      String primary = primaryField(setting);
      String secret = secretField(setting);
      if (primary == null || secret == null) {
        return OptionalLong.empty();
      }
      try {
        String pat = decryptor.decrypt(secret);
        if (pat == null) {
          return OptionalLong.empty();
        }
        return countRepos(primary, pat);
      } catch (Exception e) {
        LOG.warn("Failed to count {} repos for ALM setting '{}'", supportedAlm(), setting.key(), e);
        return OptionalLong.empty();
      }
    }
  }

  private static final class GitlabAlmRepoCountProvider extends PatAlmRepoCountProvider {

    private final GitlabApplicationClient client;

    private GitlabAlmRepoCountProvider(GitlabApplicationClient client) {
      this.client = client;
    }

    @Override
    public String supportedAlm() {
      return "gitlab";
    }

    @Override
    protected OptionalLong countRepos(String url, String pat) {
      var result = client.searchProjects(url, pat, null, 1, 1);
      return result.getTotal() != null ? OptionalLong.of(result.getTotal()) : OptionalLong.empty();
    }
  }

  private static final class AzureDevOpsAlmRepoCountProvider extends PatAlmRepoCountProvider {

    private final AzureDevOpsHttpClient client;

    private AzureDevOpsAlmRepoCountProvider(AzureDevOpsHttpClient client) {
      this.client = client;
    }

    @Override
    public String supportedAlm() {
      return "azure_devops";
    }

    @Override
    protected OptionalLong countRepos(String url, String pat) {
      var repoList = client.getRepos(url, pat, null);
      return repoList != null ? OptionalLong.of(repoList.getValues().size()) : OptionalLong.empty();
    }
  }

  private static final class BitbucketServerAlmRepoCountProvider extends PatAlmRepoCountProvider {

    private final BitbucketServerRestClient client;

    private BitbucketServerAlmRepoCountProvider(BitbucketServerRestClient client) {
      this.client = client;
    }

    @Override
    public String supportedAlm() {
      return "bitbucket";
    }

    @Override
    protected OptionalLong countRepos(String url, String pat) {
      long total = 0;
      int start = 0;
      while (true) {
        var page = client.getRepos(url, pat, null, null, start, PAGE_SIZE);
        total += page.getValues().size();
        if (page.isLastPage() || page.getNextPageStart() <= start) {
          break;
        }
        start = page.getNextPageStart();
      }
      return OptionalLong.of(total);
    }
  }

  /**
   * Unlike every other ALM, Bitbucket Cloud settings never carry a static PAT — only an OAuth consumer
   * ({@code clientId}/{@code clientSecret}), exchanged at call time for a short-lived Bearer token via
   * {@link BitbucketCloudRestClient#createAccessToken}. This is the same workspace-wide, no-user-context
   * mechanism already used by PR decoration and live binding resolution elsewhere in this codebase — see
   * {@code BitbucketCloudPrDecoratorFactory} and {@code ProjectBindingsServiceServerImpl.resolveBitbucketCloud}.
   * {@code OnboardingRows.AlmSetting} doesn't carry that OAuth consumer (it only exists on the native
   * {@link AlmSettingDto}), so this provider can't extend {@link PatAlmRepoCountProvider} — it resolves
   * the real setting by key and reads the consumer off it directly.
   */
  private static final class BitbucketCloudAlmRepoCountProvider implements AlmRepoCountProvider {

    private final BitbucketCloudRestClient client;
    private final DbClient dbClient;

    private BitbucketCloudAlmRepoCountProvider(BitbucketCloudRestClient client, DbClient dbClient) {
      this.client = client;
      this.dbClient = dbClient;
    }

    @Override
    public String supportedAlm() {
      return "bitbucket_cloud";
    }

    @Override
    public OptionalLong fetchTotalCount(OnboardingRows.AlmSetting setting, OnboardingSecretDecryptor decryptor) {
      String workspace = setting.appId();
      if (workspace == null) {
        return OptionalLong.empty();
      }
      try {
        AlmSettingDto almSettingDto;
        try (DbSession dbSession = dbClient.openSession(false)) {
          almSettingDto = dbClient.almSettingDao().selectByKey(dbSession, setting.key()).orElse(null);
        }
        if (almSettingDto == null) {
          return OptionalLong.empty();
        }
        String clientId = almSettingDto.getClientId();
        String clientSecret = decryptor.decrypt(almSettingDto.getClientSecret());
        if (clientId == null || clientSecret == null) {
          return OptionalLong.empty();
        }
        String accessToken = client.createAccessToken(clientId, clientSecret);
        var repoList = client.searchReposWithAccessToken(accessToken, workspace, null, 1, 1);
        Integer size = repoList != null ? repoList.getSize() : null;
        return size != null ? OptionalLong.of(size) : OptionalLong.empty();
      } catch (Exception e) {
        LOG.warn("Failed to count bitbucket_cloud repos for ALM setting '{}'", setting.key(), e);
        return OptionalLong.empty();
      }
    }
  }
}
