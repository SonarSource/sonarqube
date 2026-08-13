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
 * <p>Every implementation here answers only a repository <em>count</em>. The onboarding dashboard
 * never enumerates discovered repositories, so none of these beans build or return a repository
 * listing — for most ALMs that means a single {@code pageSize=1} call; Bitbucket Cloud's API has no
 * total-count field, so it still paginates, but only to count, never to collect.
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
  public AlmRepoCountProvider bitbucketCloudAlmRepoCountProvider(BitbucketCloudRestClient bbCloudClient) {
    return new BitbucketCloudAlmRepoCountProvider(bbCloudClient);
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
   * client call: check the two required setting fields once, decrypt the secret, then delegate to
   * {@link #countRepos}. Fields are captured into locals exactly once and never re-read off
   * {@code setting} afterward, since a value re-read after crossing another method call (the
   * decrypt) can no longer be proven non-null by the caller.
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
        if (page.isLastPage()) {
          break;
        }
        start = page.getNextPageStart();
      }
      return OptionalLong.of(total);
    }
  }

  private static final class BitbucketCloudAlmRepoCountProvider extends PatAlmRepoCountProvider {

    private final BitbucketCloudRestClient client;

    private BitbucketCloudAlmRepoCountProvider(BitbucketCloudRestClient client) {
      this.client = client;
    }

    @Override
    public String supportedAlm() {
      return "bitbucket_cloud";
    }

    @Override
    protected String primaryField(OnboardingRows.AlmSetting setting) {
      return setting.appId();
    }

    @Override
    protected OptionalLong countRepos(String workspace, String pat) {
      // Bitbucket Cloud's RepositoryList has no total-count field, so this still paginates —
      // but only to count pages, never to collect a listing.
      long total = 0;
      int page = 1;
      boolean hasMore = true;
      while (hasMore) {
        var repoList = client.searchRepos(pat, workspace, null, page, PAGE_SIZE);
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
}
