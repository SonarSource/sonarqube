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
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.auth.github.ExpiringAppInstallationToken;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonarsource.onboarding.shared.ciconfiguration.port.GitHubReadTokenPort;

/**
 * Provides a short-lived installation token of the GitHub App of a DevOps platform configuration, narrowed to a single
 * repository and to the {@code pull_requests: read} permission, to read the merge request state of that repository.
 *
 * <p>The repository must be bound, on this instance, to the configuration: the port is not a way to obtain a token for an
 * arbitrary repository. That check is made on every call; only the token is cached, in memory, until five minutes before
 * it expires. Whatever goes wrong, the answer is empty and nothing sensitive is logged: neither the token, nor the private
 * key, nor any response body, nor the message of an exception.
 */
class GitHubInstallationReadTokenPort implements GitHubReadTokenPort {

  private static final Logger LOG = LoggerFactory.getLogger(GitHubInstallationReadTokenPort.class);
  private static final Map<String, String> READ_PULL_REQUESTS = Map.of("pull_requests", "read");
  private static final Pattern REPOSITORY = Pattern.compile("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+");
  private static final Pattern CRLF = Pattern.compile("[\r\n]");
  private static final int MAX_CACHED_TOKENS = 1_000;
  private static final long REFRESH_MARGIN_MINUTES = 5;
  private static final int STRIPES = 32;

  private final DbClient dbClient;
  private final GithubGlobalSettingsValidator settingsValidator;
  private final GithubApplicationClient githubClient;
  private final Clock clock;
  private final Map<CacheKey, CachedToken> cache = new ConcurrentHashMap<>();
  private final Object[] stripeLocks = new Object[STRIPES];

  GitHubInstallationReadTokenPort(DbClient dbClient, GithubGlobalSettingsValidator settingsValidator, GithubApplicationClient githubClient) {
    this(dbClient, settingsValidator, githubClient, Clock.systemUTC());
  }

  GitHubInstallationReadTokenPort(DbClient dbClient, GithubGlobalSettingsValidator settingsValidator, GithubApplicationClient githubClient, Clock clock) {
    this.dbClient = dbClient;
    this.settingsValidator = settingsValidator;
    this.githubClient = githubClient;
    this.clock = clock;
    for (int i = 0; i < STRIPES; i++) {
      stripeLocks[i] = new Object();
    }
  }

  @Override
  public Optional<String> readToken(String dopSettingId, String repositoryId) {
    String safeSetting = sanitize(dopSettingId);
    String safeRepository = sanitize(repositoryId);
    if (!REPOSITORY.matcher(repositoryId).matches()) {
      LOG.debug("No GitHub read token for '{}': '{}' is not an owner/repository", safeSetting, safeRepository);
      return Optional.empty();
    }
    try {
      return boundSetting(dopSettingId, repositoryId)
        .flatMap(setting -> cachedOrMinted(setting, repositoryId))
        .or(() -> {
          LOG.debug("No GitHub read token for repository '{}' of '{}'", safeRepository, safeSetting);
          return Optional.empty();
        });
    } catch (RuntimeException e) {
      LOG.warn("Cannot obtain a GitHub read token for repository '{}' of '{}' ({} thrown at {})", safeRepository, safeSetting, e.getClass().getSimpleName(), throwSite(e));
      return Optional.empty();
    }
  }

  private Optional<AlmSettingDto> boundSetting(String settingKey, String repositoryId) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.almSettingDao().selectByKey(dbSession, settingKey)
        .filter(setting -> setting.getAlm() == ALM.GITHUB)
        .filter(setting -> !dbClient.projectAlmSettingDao().selectByAlmSettingAndRepoIgnoringCase(dbSession, setting, repositoryId).isEmpty());
    }
  }

  private Optional<String> cachedOrMinted(AlmSettingDto setting, String repositoryId) {
    CacheKey key = new CacheKey(setting.getUuid(), setting.getUpdatedAt(), repositoryId.toLowerCase(Locale.ROOT));
    Optional<String> cached = fresh(cache.get(key));
    if (cached.isPresent()) {
      return cached;
    }
    synchronized (stripeLocks[Math.floorMod(key.hashCode(), STRIPES)]) {
      cached = fresh(cache.get(key));
      if (cached.isPresent()) {
        return cached;
      }
      return mint(setting, repositoryId).map(token -> {
        store(key, token);
        return token.getValue();
      });
    }
  }

  private Optional<String> fresh(CachedToken cached) {
    return Optional.ofNullable(cached)
      .filter(token -> token.refreshAt().isAfter(OffsetDateTime.now(clock)))
      .map(CachedToken::value);
  }

  private Optional<ExpiringAppInstallationToken> mint(AlmSettingDto setting, String repositoryId) {
    GithubAppConfiguration configuration = settingsValidator.validateApiEndpoint(setting);
    Optional<Long> installationId = githubClient.getInstallationId(configuration, repositoryId);
    if (installationId.isEmpty()) {
      return Optional.empty();
    }
    String repositoryName = repositoryId.substring(repositoryId.indexOf('/') + 1);
    return githubClient.createAppInstallationToken(configuration, installationId.get(), repositoryName, READ_PULL_REQUESTS);
  }

  private void store(CacheKey key, ExpiringAppInstallationToken token) {
    OffsetDateTime refreshAt = token.getExpiresAt().minusMinutes(REFRESH_MARGIN_MINUTES);
    if (cache.size() >= MAX_CACHED_TOKENS) {
      evict();
    }
    cache.put(key, new CachedToken(token.getValue(), refreshAt));
  }

  private void evict() {
    OffsetDateTime now = OffsetDateTime.now(clock);
    cache.values().removeIf(cached -> !cached.refreshAt().isAfter(now));
    if (cache.size() >= MAX_CACHED_TOKENS) {
      cache.entrySet().stream()
        .min(Comparator.comparing(entry -> entry.getValue().refreshAt()))
        .map(Map.Entry::getKey)
        .ifPresent(cache::remove);
    }
  }

  /**
   * Where the failure was thrown, which tells what failed without the message, that may carry a secret.
   */
  private static String throwSite(RuntimeException e) {
    StackTraceElement[] stackTrace = e.getStackTrace();
    return stackTrace.length == 0 ? "an unknown place" : stackTrace[0].toString();
  }

  private static String sanitize(String value) {
    return CRLF.matcher(value).replaceAll("_");
  }

  private record CacheKey(String settingUuid, long settingUpdatedAt, String repository) {
  }

  private record CachedToken(String value, OffsetDateTime refreshAt) {
    @Override
    public String toString() {
      return "CachedToken[refreshAt=" + refreshAt + "]";
    }
  }
}
