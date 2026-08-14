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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
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
 * Mints a scoped, short-lived GitLab project access token (SONAR-31165) for a project's bound GitLab
 * repository — the GitLab counterpart to {@code GithubScmAccessTokenProviderAdapter}. Reuses the same
 * DB/ALM resolution shape as {@code GithubInstallationTokenProviderImpl} and the same
 * self-filtering-delegate convention as {@code GitlabProjectCreatorFactory} (returns {@code
 * Optional.empty()} whenever the project isn't bound to GitLab, rather than throwing).
 */
@ServerSide
public class GitlabScmAccessTokenProvider implements ScmAccessTokenProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GitlabScmAccessTokenProvider.class);
  private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n]");

  /**
   * A fixed, URL-safe name for the minted token: it is embedded verbatim as the git-remote username
   * ({@code https://<name>:<secret>@host}), so it must not contain characters GitLab could return
   * back to us with spaces/specials that would break that URL.
   */
  private static final String REMEDIATION_AGENT_NAME = "sonarqube-remediation-agent";

  /**
   * {@code api} is required to open a merge request via the REST API; {@code write_repository} is
   * required to push over git. Neither scope is a documented superset of the other, so both are
   * requested on the same minted token.
   */
  private static final List<String> TOKEN_MINTING_SCOPES = List.of("api", "write_repository");

  private final DbClient dbClient;
  private final GitlabGlobalSettingsValidator gitlabGlobalSettingsValidator;
  private final GitlabApplicationClient gitlabApplicationClient;
  private final Encryption encryption;

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
    AlmSettingDto resolvedAlmSetting;
    Long resolvedGitlabProjectId;
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

      String almRepo = projectAlmSetting.get().getAlmRepo();
      Long gitlabProjectId = parseGitlabProjectId(almRepo, safeProjectKey);
      if (gitlabProjectId == null) {
        return Optional.empty();
      }

      resolvedAlmSetting = almSetting.get();
      resolvedGitlabProjectId = gitlabProjectId;
    }

    // GitLab API calls below are network I/O, deliberately made outside the DbSession above — see
    // GithubInstallationTokenProviderImpl for the same rationale (a fresh token is minted before
    // every git operation, no caching, so holding a pooled DB connection for their duration would add
    // unnecessary contention under load).
    return Optional.of(mint(projectKey, resolvedAlmSetting, resolvedGitlabProjectId));
  }

  private ScmAccessToken mint(String projectKey, AlmSettingDto almSetting, Long gitlabProjectId) {
    String safeProjectKey = sanitizeForLog(projectKey);

    try {
      // Precondition check, mirroring GithubGlobalSettingsValidator's role in the GitHub path: fail
      // fast with an actionable message if the stored PAT/URL themselves are broken, rather than
      // surfacing GitLab's own opaque 401/403 straight from the mint call below.
      gitlabGlobalSettingsValidator.validate(almSetting);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
        format("Cannot mint a GitLab access token for project '%s': invalid GitLab configuration: %s", safeProjectKey, e.getMessage()), e);
    }

    // Both values are guaranteed set by the successful validate() call above, but their accessors are
    // @CheckForNull at the DTO level (the same non-null-after-validation gap the GitHub/Bitbucket/Azure
    // DevOps project creators narrow with requireNonNull on this DTO's getUrl()).
    String gitlabUrl = requireNonNull(almSetting.getUrl(), "GitLab url cannot be null");
    String personalAccessToken = requireNonNull(almSetting.getDecryptedPersonalAccessToken(encryption), "GitLab personal access token cannot be null");
    // A fixed zone (rather than the JVM default) keeps the expiry date deterministic regardless of
    // where this process runs.
    LocalDate expiresAt = LocalDate.now(ZoneOffset.UTC).plusDays(1);

    GitlabProjectAccessToken token = gitlabApplicationClient.createProjectAccessToken(
      gitlabUrl, personalAccessToken, gitlabProjectId, REMEDIATION_AGENT_NAME, TOKEN_MINTING_SCOPES, expiresAt);

    return new ScmAccessToken(ALM.GITLAB.getId(), token.getName(), token.getToken(), formatExpiresAt(token.getExpiresAt(), expiresAt));
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

  /**
   * GitLab's {@code expires_at} response field is a plain {@code YYYY-MM-DD} date, not an ISO-8601
   * timestamp — fall back to the date we requested (formatted as a full ISO-8601 timestamp for
   * consistency with the GitHub path) if GitLab's response omits it.
   */
  private static String formatExpiresAt(@Nullable String responseExpiresAt, LocalDate requestedExpiresAt) {
    if (responseExpiresAt != null && !responseExpiresAt.isBlank()) {
      return responseExpiresAt;
    }
    return requestedExpiresAt.format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  /**
   * Strips CR/LF from user-controlled values (project key, ALM repo id) before logging them, so a
   * crafted value cannot forge extra log lines/entries (CWE-117).
   */
  private static String sanitizeForLog(String value) {
    return CRLF_PATTERN.matcher(value).replaceAll("_");
  }
}
