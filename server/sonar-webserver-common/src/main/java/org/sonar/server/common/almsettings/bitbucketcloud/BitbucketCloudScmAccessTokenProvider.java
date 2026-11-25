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
package org.sonar.server.common.almsettings.bitbucketcloud;

import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient.BitbucketCloudException;
import org.sonar.alm.client.bitbucket.bitbucketcloud.Token;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.System2;
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
 * Provides a Bitbucket Cloud access token for a project's bound Bitbucket Cloud repository. The token
 * is minted from the OAuth consumer stored in the DevOps Platform configuration with the
 * {@code client_credentials} grant, so it carries the consumer's permissions. Bitbucket Cloud access
 * tokens are short-lived (about 2 hours) and cheap to mint, so one is minted per call and never cached
 * or persisted.
 */
@ServerSide
public class BitbucketCloudScmAccessTokenProvider implements ScmAccessTokenProvider {

  private static final Logger LOG = LoggerFactory.getLogger(BitbucketCloudScmAccessTokenProvider.class);
  private static final Pattern CRLF_PATTERN = Pattern.compile("[\r\n]");

  /**
   * Bitbucket Cloud authenticates git over HTTPS with an OAuth access token as
   * {@code x-token-auth:<token>}.
   */
  private static final String GIT_USERNAME = "x-token-auth";

  private final DbClient dbClient;
  private final BitbucketCloudRestClient bitbucketCloudRestClient;
  private final Encryption encryption;
  private final System2 system2;

  public BitbucketCloudScmAccessTokenProvider(DbClient dbClient, BitbucketCloudRestClient bitbucketCloudRestClient, Settings settings, System2 system2) {
    this.dbClient = dbClient;
    this.bitbucketCloudRestClient = bitbucketCloudRestClient;
    this.encryption = settings.getEncryption();
    this.system2 = system2;
  }

  @Override
  public Optional<ScmAccessToken> mint(String projectKey) {
    String safeProjectKey = sanitizeForLog(projectKey);
    Optional<AlmSettingDto> resolvedAlmSetting;
    try (DbSession dbSession = dbClient.openSession(false)) {
      resolvedAlmSetting = resolveBitbucketCloudAlmSetting(dbSession, projectKey, safeProjectKey);
    }

    // Minting below is network I/O (a Bitbucket Cloud API call), deliberately made outside the
    // DbSession above — see GitlabScmAccessTokenProvider for the same rationale.
    return resolvedAlmSetting.map(almSetting -> mintAccessToken(safeProjectKey, almSetting));
  }

  /**
   * Walks project -&gt; its DevOps Platform binding -&gt; the bound {@link AlmSettingDto}, short-circuiting
   * to {@link Optional#empty()} (with a warning) at whichever step is missing, or once the binding
   * turns out not to be Bitbucket Cloud.
   */
  private Optional<AlmSettingDto> resolveBitbucketCloudAlmSetting(DbSession dbSession, String projectKey, String safeProjectKey) {
    Optional<ProjectDto> project = dbClient.projectDao().selectProjectByKey(dbSession, projectKey);
    if (project.isEmpty()) {
      LOG.warn("Cannot mint a Bitbucket Cloud access token: unknown project '{}'", safeProjectKey);
      return Optional.empty();
    }

    Optional<ProjectAlmSettingDto> projectAlmSetting = dbClient.projectAlmSettingDao().selectByProject(dbSession, project.get());
    if (projectAlmSetting.isEmpty()) {
      LOG.warn("Cannot mint a Bitbucket Cloud access token: project '{}' is not bound to any DevOps Platform", safeProjectKey);
      return Optional.empty();
    }

    return dbClient.almSettingDao().selectByUuid(dbSession, projectAlmSetting.get().getAlmSettingUuid())
      .filter(almSetting -> almSetting.getAlm() == ALM.BITBUCKET_CLOUD);
  }

  private ScmAccessToken mintAccessToken(String safeProjectKey, AlmSettingDto almSetting) {
    String clientId = almSetting.getClientId();
    String clientSecret = almSetting.getDecryptedClientSecret(encryption);
    if (clientId == null || clientSecret == null) {
      throw new IllegalArgumentException(
        format("Cannot mint a Bitbucket Cloud access token for project '%s': invalid Bitbucket Cloud configuration: missing OAuth consumer key or secret",
          safeProjectKey));
    }

    Token token;
    try {
      token = bitbucketCloudRestClient.createToken(clientId, clientSecret);
    } catch (BitbucketCloudException e) {
      if (isClientError(e.getHttpCode())) {
        // The token endpoint rejected the OAuth consumer itself (bad key/secret, consumer not private, ...).
        throw new IllegalArgumentException(
          format("Cannot mint a Bitbucket Cloud access token for project '%s': invalid Bitbucket Cloud configuration: %s", safeProjectKey, e.getMessage()), e);
      }
      throw e;
    }

    String accessToken = requireNonNull(token.getAccessToken(), "Bitbucket Cloud access token cannot be null");
    return new ScmAccessToken(ALM.BITBUCKET_CLOUD.getId(), GIT_USERNAME, accessToken, toExpiresAt(token.getExpiresIn()));
  }

  @CheckForNull
  private String toExpiresAt(long expiresInSeconds) {
    if (expiresInSeconds <= 0) {
      return null;
    }
    return Instant.ofEpochMilli(system2.now()).plusSeconds(expiresInSeconds).toString();
  }

  /** A 429 is a transient rate limit rather than a configuration problem, so it is not a client error here. */
  private static boolean isClientError(int httpCode) {
    return httpCode >= 400 && httpCode < 500 && httpCode != 429;
  }

  /**
   * Strips CR/LF from the user-controlled project key before logging it, so a crafted value cannot
   * forge extra log lines/entries (CWE-117).
   */
  private static String sanitizeForLog(String value) {
    return CRLF_PATTERN.matcher(value).replaceAll("_");
  }
}
