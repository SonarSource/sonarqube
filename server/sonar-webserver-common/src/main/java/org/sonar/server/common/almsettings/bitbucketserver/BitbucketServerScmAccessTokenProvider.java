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
package org.sonar.server.common.almsettings.bitbucketserver;

import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.bitbucketserver.BitbucketServerSettingsValidator;
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
 * Provides the personal access token stored in a project's Bitbucket Server / Data Center DevOps
 * Platform configuration, after validating it — the same pass-through approach as
 * {@link org.sonar.server.common.almsettings.azuredevops.AzureDevOpsScmAccessTokenProvider}.
 */
@ServerSide
public class BitbucketServerScmAccessTokenProvider implements ScmAccessTokenProvider {

  private static final Logger LOG = LoggerFactory.getLogger(BitbucketServerScmAccessTokenProvider.class);
  private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n]");

  /**
   * Bitbucket Data Center only accepts a personal access token over HTTP Basic together with the
   * token owner's username, which is not stored. Callers therefore send the token as a Bearer token,
   * and no username is provided.
   */
  private static final String GIT_USERNAME = "";

  private final DbClient dbClient;
  private final BitbucketServerSettingsValidator bitbucketServerSettingsValidator;
  private final Encryption encryption;

  public BitbucketServerScmAccessTokenProvider(DbClient dbClient, BitbucketServerSettingsValidator bitbucketServerSettingsValidator, Settings settings) {
    this.dbClient = dbClient;
    this.bitbucketServerSettingsValidator = bitbucketServerSettingsValidator;
    this.encryption = settings.getEncryption();
  }

  @Override
  public Optional<ScmAccessToken> mint(String projectKey) {
    String safeProjectKey = sanitizeForLog(projectKey);
    Optional<AlmSettingDto> resolvedAlmSetting;
    try (DbSession dbSession = dbClient.openSession(false)) {
      resolvedAlmSetting = resolveBitbucketServerAlmSetting(dbSession, projectKey, safeProjectKey);
    }

    // Validation below is network I/O (Bitbucket Server API calls), deliberately made outside the
    // DbSession above — see GitlabScmAccessTokenProvider for the same rationale.
    return resolvedAlmSetting.map(almSetting -> passThrough(safeProjectKey, almSetting));
  }

  /**
   * Walks project -&gt; its DevOps Platform binding -&gt; the bound {@link AlmSettingDto}, short-circuiting
   * to {@link Optional#empty()} (with a warning) at whichever step is missing, or once the binding
   * turns out not to be Bitbucket Server.
   */
  private Optional<AlmSettingDto> resolveBitbucketServerAlmSetting(DbSession dbSession, String projectKey, String safeProjectKey) {
    Optional<ProjectDto> project = dbClient.projectDao().selectProjectByKey(dbSession, projectKey);
    if (project.isEmpty()) {
      LOG.warn("Cannot provide a Bitbucket Server access token: unknown project '{}'", safeProjectKey);
      return Optional.empty();
    }

    Optional<ProjectAlmSettingDto> projectAlmSetting = dbClient.projectAlmSettingDao().selectByProject(dbSession, project.get());
    if (projectAlmSetting.isEmpty()) {
      LOG.warn("Cannot provide a Bitbucket Server access token: project '{}' is not bound to any DevOps Platform", safeProjectKey);
      return Optional.empty();
    }

    return dbClient.almSettingDao().selectByUuid(dbSession, projectAlmSetting.get().getAlmSettingUuid())
      .filter(almSetting -> almSetting.getAlm() == ALM.BITBUCKET);
  }

  private ScmAccessToken passThrough(String safeProjectKey, AlmSettingDto almSetting) {
    // BitbucketServerSettingsValidator.validate() fails with IllegalArgumentException (including its
    // BitbucketServerException subclass) for an incomplete configuration, an unreachable server, or a
    // token that is rejected or lacks repository read permission.
    try {
      bitbucketServerSettingsValidator.validate(almSetting);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
        format("Cannot provide a Bitbucket Server access token for project '%s': invalid Bitbucket Server configuration: %s", safeProjectKey, e.getMessage()), e);
    }

    String personalAccessToken = requireNonNull(almSetting.getDecryptedPersonalAccessToken(encryption), "Bitbucket Server personal access token cannot be null");
    // Bitbucket Server personal access tokens are long-lived, admin-managed credentials — not minted per
    // call — so there is no per-request expiry to report here.
    return new ScmAccessToken(ALM.BITBUCKET.getId(), GIT_USERNAME, personalAccessToken, null);
  }

  /**
   * Strips CR/LF from the user-controlled project key before logging it, so a crafted value cannot
   * forge extra log lines/entries (CWE-117).
   */
  private static String sanitizeForLog(String value) {
    return CRLF_PATTERN.matcher(value).replaceAll("_");
  }
}
