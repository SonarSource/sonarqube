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
package org.sonar.server.common.almsettings.github;

import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.ExpiringAppInstallationToken;
import org.sonar.auth.github.GithubAppConfiguration;
import org.sonar.auth.github.GithubAppPermissions;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.core.scm.github.GithubInstallationToken;
import org.sonar.core.scm.github.GithubInstallationTokenProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.ServerException;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

/**
 * Implements the sonar-core {@link GithubInstallationTokenProvider} SPI (SONAR-30903), reusing the
 * same DB/ALM/GitHub-App plumbing as {@link GithubProjectCreatorFactory}. Registered as a
 * {@code @ServerSide} bean so unification capabilities (whose Spring context is a child of the
 * platform's own context) can inject the SPI interface directly — no HTTP hop, no separate
 * authentication, and the GitHub App's private key never leaves this class.
 */
@ServerSide
public class GithubInstallationTokenProviderImpl implements GithubInstallationTokenProvider {

  private static final Logger LOG = LoggerFactory.getLogger(GithubInstallationTokenProviderImpl.class);
  private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n]");

  private final DbClient dbClient;
  private final GithubGlobalSettingsValidator githubGlobalSettingsValidator;
  private final GithubApplicationClient githubApplicationClient;

  public GithubInstallationTokenProviderImpl(DbClient dbClient, GithubGlobalSettingsValidator githubGlobalSettingsValidator,
    GithubApplicationClientImpl githubApplicationClient) {
    this.dbClient = dbClient;
    this.githubGlobalSettingsValidator = githubGlobalSettingsValidator;
    this.githubApplicationClient = githubApplicationClient;
  }

  @Override
  public Optional<GithubInstallationToken> mint(String projectKey) {
    String safeProjectKey = sanitizeForLog(projectKey);
    AlmSettingDto resolvedAlmSetting;
    String resolvedAlmRepo;
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ProjectDto> project = dbClient.projectDao().selectProjectByKey(dbSession, projectKey);
      if (project.isEmpty()) {
        LOG.warn("Cannot mint a GitHub installation token: unknown project '{}'", safeProjectKey);
        return Optional.empty();
      }

      Optional<ProjectAlmSettingDto> projectAlmSetting = dbClient.projectAlmSettingDao().selectByProject(dbSession, project.get());
      if (projectAlmSetting.isEmpty()) {
        LOG.warn("Cannot mint a GitHub installation token: project '{}' is not bound to any DevOps Platform", safeProjectKey);
        return Optional.empty();
      }

      Optional<AlmSettingDto> almSetting = dbClient.almSettingDao().selectByUuid(dbSession, projectAlmSetting.get().getAlmSettingUuid());
      if (almSetting.isEmpty() || almSetting.get().getAlm() != ALM.GITHUB) {
        LOG.warn("Cannot mint a GitHub installation token: project '{}' is not bound to a GitHub App", safeProjectKey);
        return Optional.empty();
      }

      String almRepo = projectAlmSetting.get().getAlmRepo();
      if (almRepo == null || almRepo.isBlank()) {
        LOG.warn("Cannot mint a GitHub installation token: project '{}' has no repository configured on its DevOps Platform binding", safeProjectKey);
        return Optional.empty();
      }

      resolvedAlmSetting = almSetting.get();
      resolvedAlmRepo = almRepo;
    }

    // GitHub App calls below are network I/O, deliberately made outside the DbSession above: the
    // orchestrator mints a fresh token before every git operation (no caching, by design), so
    // holding a pooled DB connection for their duration would add unnecessary contention under load.
    return mint(projectKey, resolvedAlmSetting, resolvedAlmRepo);
  }

  private Optional<GithubInstallationToken> mint(String projectKey, AlmSettingDto almSetting, String almRepo) {
    String safeProjectKey = sanitizeForLog(projectKey);
    String safeAlmRepo = sanitizeForLog(almRepo);

    GithubAppConfiguration githubAppConfiguration;
    try {
      // TOKEN_MINTING_PERMISSIONS, not the plain default: a minted token is pointless if the GitHub
      // App doesn't have 'contents: write' to push the remediation commit it's minted for. Most
      // already-installed apps predate this requirement — there's no in-product way to prompt them
      // to re-approve, so the wrapped message below is the only guidance an admin gets.
      githubAppConfiguration = githubGlobalSettingsValidator.validate(almSetting, GithubAppPermissions.TOKEN_MINTING_PERMISSIONS);
    } catch (IllegalArgumentException e) {
      // Wrapped (with the project key) rather than swallowed to Optional.empty(): unlike the checks
      // above, this isn't a "not bound" case — the binding exists, its GitHub App configuration is
      // just broken (bad credentials, missing permissions, unreachable API, ...). Wrapping instead of
      // rethrowing as-is adds context in one throw (S2139) while still getting the caller a distinct
      // 400 instead of the same 404 as a genuinely unbound project.
      throw new IllegalArgumentException(
        format("Cannot mint a GitHub installation token for project '%s': invalid GitHub App configuration: %s", safeProjectKey, e.getMessage()), e);
    }

    Optional<Long> installationId = githubApplicationClient.getInstallationId(githubAppConfiguration, almRepo);
    if (installationId.isEmpty()) {
      LOG.warn("Cannot mint a GitHub installation token for project '{}': GitHub App is not installed on repository '{}'", safeProjectKey, safeAlmRepo);
      return Optional.empty();
    }

    String repositoryName = bareRepositoryName(almRepo);
    Optional<ExpiringAppInstallationToken> token = githubApplicationClient.createAppInstallationToken(githubAppConfiguration, installationId.get(), repositoryName);
    if (token.isEmpty()) {
      LOG.warn("Failed to mint a GitHub installation token for project '{}' (repository '{}')", safeProjectKey, safeAlmRepo);
      throw new ServerException(HTTP_INTERNAL_ERROR,
        format("Failed to mint a GitHub installation token for project '%s': GitHub App API call failed", safeProjectKey));
    }

    return Optional.of(new GithubInstallationToken(
      token.get().getValue(), token.get().getExpiresAt().format(ISO_OFFSET_DATE_TIME)));
  }

  /**
   * Strips CR/LF from user-controlled values (project key, ALM repo slug) before logging them, so a
   * crafted value cannot forge extra log lines/entries (CWE-117).
   */
  private static String sanitizeForLog(String value) {
    return CRLF_PATTERN.matcher(value).replaceAll("_");
  }

  /**
   * GitHub's installation-token "repositories" scoping parameter expects the bare repository name
   * (no {@code owner/} prefix), unlike {@code almRepo} which is stored as {@code owner/repo}.
   */
  private static String bareRepositoryName(String almRepo) {
    int lastSlash = almRepo.lastIndexOf('/');
    return lastSlash < 0 ? almRepo : almRepo.substring(lastSlash + 1);
  }
}
