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
package org.sonar.server.common.almsettings.gitlab;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.Striped;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabProjectAccessToken;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.core.scm.ScmAccessToken;
import org.sonar.core.scm.ScmAccessTokenProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Provides a scoped GitLab project access token for a project's bound GitLab repository. GitLab
 * creates a project bot member whenever a project access token is created, so tokens are cached in
 * memory per SonarQube Server node and refreshed before their expiry. The credential is never
 * persisted in the database.
 */
@ServerSide
public class GitlabScmAccessTokenProvider implements ScmAccessTokenProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GitlabScmAccessTokenProvider.class);
  private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n]");
  private static final String REMEDIATION_AGENT_NAME = "sonarqube-remediation-agent";
  private static final String PROJECT_ACCESS_TOKEN_NULL_MESSAGE = "GitLab project access token cannot be null";
  private static final List<String> TOKEN_MINTING_SCOPES = List.of("api", "write_repository");
  private static final int TOKEN_LIFETIME_DAYS = 180;
  private static final int TOKEN_ROTATION_MARGIN_DAYS = 7;
  private static final long MAX_CACHE_ENTRIES = 10_000;

  private final DbClient dbClient;
  private final GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator;
  private final GitlabApplicationClient gitlabApplicationClient;
  private final Encryption encryption;
  private final Cache<TokenCacheKey, ScmAccessToken> tokenCache = CacheBuilder.<TokenCacheKey, ScmAccessToken>newBuilder()
    .expireAfterWrite(Duration.ofDays((long) TOKEN_LIFETIME_DAYS - TOKEN_ROTATION_MARGIN_DAYS))
    .maximumSize(MAX_CACHE_ENTRIES)
    .build();
  private final Striped<Lock> tokenRefreshLocks = Striped.lazyWeakLock(128);

  public GitlabScmAccessTokenProvider(DbClient dbClient, GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator,
    GitlabApplicationClient gitlabApplicationClient, Settings settings) {
    this.dbClient = dbClient;
    this.gitlabGlobalSettingsValidator = gitlabGlobalSettingsValidator;
    this.gitlabApplicationClient = gitlabApplicationClient;
    this.encryption = settings.getEncryption();
  }

  @Override
  public Optional<ScmAccessToken> mint(String projectKey) {
    String safeProjectKey = sanitizeForLog(projectKey);
    TokenMintRequest request;
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ProjectDto> project = dbClient.projectDao().selectProjectByKey(dbSession, projectKey);
      if (project.isEmpty()) {
        LOG.warn("Cannot mint a GitLab access token: unknown project '{}'", safeProjectKey);
        return Optional.empty();
      }
      Optional<ProjectAlmSettingDto> projectAlmSetting = dbClient.projectAlmSettingDao().selectByProject(dbSession, project.get());
      if (projectAlmSetting.isEmpty()) {
        LOG.warn("Cannot mint a GitLab access token: project '{}' is not bound to any DevOps Platform", safeProjectKey);
        return Optional.empty();
      }
      Optional<AlmSettingDto> almSetting = dbClient.almSettingDao().selectByUuid(dbSession, projectAlmSetting.get().getAlmSettingUuid());
      if (almSetting.isEmpty() || almSetting.get().getAlm() != ALM.GITLAB) {
        return Optional.empty();
      }
      Long gitlabProjectId = parseGitlabProjectId(projectAlmSetting.get().getAlmRepo(), safeProjectKey);
      if (gitlabProjectId == null) {
        return Optional.empty();
      }
      request = new TokenMintRequest(new TokenCacheKey(requireNonNull(project.get().getUuid(), "Project UUID cannot be null"),
        requireNonNull(almSetting.get().getUuid(), "ALM setting UUID cannot be null"), gitlabProjectId, almSetting.get().getUpdatedAt()), safeProjectKey,
        almSetting.get());
    }

    // GitLab API calls below are network I/O, deliberately made outside the DbSession above, so a
    // pooled DB connection is not held for their duration.
    return Optional.of(getOrCreateToken(request));
  }

  private ScmAccessToken getOrCreateToken(TokenMintRequest request) {
    Optional<ScmAccessToken> cachedToken = getCachedToken(request.cacheKey);
    if (cachedToken.isPresent()) {
      return cachedToken.get();
    }
    Lock refreshLock = tokenRefreshLocks.get(request.cacheKey);
    refreshLock.lock();
    try {
      return getCachedToken(request.cacheKey).orElseGet(() -> {
        ScmAccessToken token = createToken(request);
        if (isExpiring(token)) {
          LOG.warn("GitLab returned an access token for project '{}' expiring on '{}', within the {}-day rotation margin: it will not be reused",
            request.safeProjectKey, token.expiresAt(), TOKEN_ROTATION_MARGIN_DAYS);
          return token;
        }
        tokenCache.put(request.cacheKey, token);
        return token;
      });
    } finally {
      refreshLock.unlock();
    }
  }

  private Optional<ScmAccessToken> getCachedToken(TokenCacheKey cacheKey) {
    ScmAccessToken token = tokenCache.getIfPresent(cacheKey);
    if (token == null) {
      return Optional.empty();
    }
    if (isExpiring(token)) {
      tokenCache.invalidate(cacheKey);
      return Optional.empty();
    }
    return Optional.of(token);
  }

  private ScmAccessToken createToken(TokenMintRequest request) {
    try {
      gitlabGlobalSettingsValidator.validate(request.almSetting);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(format("Cannot mint a GitLab access token for project '%s': invalid GitLab configuration: %s",
        request.safeProjectKey, e.getMessage()), e);
    }
    String gitlabUrl = requireNonNull(request.almSetting.getUrl(), "GitLab url cannot be null");
    String personalAccessToken = requireNonNull(request.almSetting.getDecryptedPersonalAccessToken(encryption), "GitLab personal access token cannot be null");
    LocalDate expiresAt = LocalDate.now(ZoneOffset.UTC).plusDays(TOKEN_LIFETIME_DAYS);
    GitlabProjectAccessToken token = gitlabApplicationClient.createProjectAccessToken(gitlabUrl, personalAccessToken,
      request.cacheKey.gitlabProjectId, REMEDIATION_AGENT_NAME, TOKEN_MINTING_SCOPES, expiresAt);
    return new ScmAccessToken(ALM.GITLAB.getId(), REMEDIATION_AGENT_NAME,
      requireNonNull(token.getToken(), PROJECT_ACCESS_TOKEN_NULL_MESSAGE), formatExpiresAt(token.getExpiresAt(), expiresAt));
  }

  private static boolean isExpiring(ScmAccessToken token) {
    try {
      return token.expiresAt() == null || !LocalDate.parse(token.expiresAt()).isAfter(LocalDate.now(ZoneOffset.UTC).plusDays(TOKEN_ROTATION_MARGIN_DAYS));
    } catch (DateTimeParseException e) {
      return true;
    }
  }

  @Nullable
  private static Long parseGitlabProjectId(@Nullable String almRepo, String safeProjectKey) {
    if (almRepo == null || almRepo.isBlank()) {
      LOG.warn("Cannot mint a GitLab access token: project '{}' has no repository configured on its DevOps Platform binding", safeProjectKey);
      return null;
    }
    try {
      return Long.parseLong(almRepo);
    } catch (NumberFormatException e) {
      LOG.warn("Cannot mint a GitLab access token: project '{}' has a non-numeric GitLab repository identifier '{}'", safeProjectKey, sanitizeForLog(almRepo));
      return null;
    }
  }

  private static String formatExpiresAt(@Nullable String responseExpiresAt, LocalDate requestedExpiresAt) {
    if (responseExpiresAt != null && !responseExpiresAt.isBlank()) {
      try {
        return LocalDate.parse(responseExpiresAt.trim()).format(DateTimeFormatter.ISO_LOCAL_DATE);
      } catch (DateTimeParseException e) {
        LOG.warn("GitLab returned an unparseable token expiry '{}', falling back to the requested date", sanitizeForLog(responseExpiresAt));
      }
    }
    return requestedExpiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  private static String sanitizeForLog(String value) {
    return CRLF_PATTERN.matcher(value).replaceAll("_");
  }

  private record TokenCacheKey(String projectUuid, String almSettingUuid, long gitlabProjectId, long almSettingUpdatedAt) {
  }

  private record TokenMintRequest(TokenCacheKey cacheKey, String safeProjectKey, AlmSettingDto almSetting) {
  }
}
