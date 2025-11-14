/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.authentication;

import com.google.common.annotations.VisibleForTesting;
import java.security.MessageDigest;
import java.util.Optional;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.internal.Encryption;
import org.sonar.api.config.internal.Settings;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.sonar.server.user.GithubWebhookUserSession.GITHUB_WEBHOOK_USER_NAME;

public class GithubWebhookAuthentication {
  private static final Logger LOG = LoggerFactory.getLogger(GithubWebhookAuthentication.class);

  @VisibleForTesting
  static final String GITHUB_SIGNATURE_HEADER = "x-hub-signature-256";

  @VisibleForTesting
  static final String GITHUB_APP_ID_HEADER = "x-github-hook-installation-target-id";

  @VisibleForTesting
  static final String MSG_AUTHENTICATION_FAILED = "Failed to authenticate payload from Github webhook. Either the webhook was called by unexpected clients"
    + " or the webhook secret set in SonarQube does not match the one from Github.";

  @VisibleForTesting
  static final String MSG_UNAUTHENTICATED_GITHUB_CALLS_DENIED = "Unauthenticated calls from GitHub are forbidden. A webhook secret must be defined in the GitHub App with Id %s.";

  @VisibleForTesting
  static final String MSG_NO_WEBHOOK_SECRET_FOUND = "Webhook secret for your GitHub app with Id %s must be set in SonarQube.";

  @VisibleForTesting
  static final String MSG_NO_BODY_FOUND = "No body found in GitHub Webhook event.";

  private static final String SHA_256_PREFIX = "sha256=";

  private final AuthenticationEvent authenticationEvent;

  private final DbClient dbClient;

  private final Encryption encryption;

  public GithubWebhookAuthentication(AuthenticationEvent authenticationEvent, DbClient dbClient, Settings settings) {
    this.authenticationEvent = authenticationEvent;
    this.dbClient = dbClient;
    this.encryption = settings.getEncryption();
  }

  public Optional<UserAuthResult> authenticate(HttpRequest request) {
    String githubAppId = request.getHeader(GITHUB_APP_ID_HEADER);
    if (isEmpty(githubAppId)) {
      return Optional.empty();
    }

    String githubSignature = getGithubSignature(request, githubAppId);
    String body = getBody(request);
    String webhookSecret = getWebhookSecret(githubAppId);

    String computedSignature = computeSignature(body, webhookSecret);
    if (!checkEqualityTimingAttacksSafe(githubSignature, computedSignature)) {
      logAuthenticationProblemAndThrow(MSG_AUTHENTICATION_FAILED);
    }
    return createAuthResult(request);
  }

  private static String getGithubSignature(HttpRequest request, String githubAppId) {
    String githubSignature = request.getHeader(GITHUB_SIGNATURE_HEADER);
    if (isEmpty(githubSignature)) {
      logAuthenticationProblemAndThrow(format(MSG_UNAUTHENTICATED_GITHUB_CALLS_DENIED, githubAppId));
    }
    return githubSignature;
  }

  private static String getBody(HttpRequest request) {
    try {
      return request.getReader().lines().collect(joining(System.lineSeparator()));
    } catch (Exception e) {
      logAuthenticationProblemAndThrow(MSG_NO_BODY_FOUND);
      return "";
    }
  }

  private String getWebhookSecret(String githubAppId) {
    String webhookSecret = fetchWebhookSecret(githubAppId);
    if (isEmpty(webhookSecret)) {
      logAuthenticationProblemAndThrow(format(MSG_NO_WEBHOOK_SECRET_FOUND, githubAppId));
    }
    return webhookSecret;
  }

  private String fetchWebhookSecret(String appId) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.almSettingDao().selectByAlmAndAppId(dbSession, ALM.GITHUB, appId)
        .map(almSettingDto -> almSettingDto.getDecryptedWebhookSecret(encryption))
        .orElse(null);
    }
  }

  private static void logAuthenticationProblemAndThrow(String message) {
    LOG.warn(message);
    throw AuthenticationException.newBuilder()
      .setSource(AuthenticationEvent.Source.githubWebhook())
      .setMessage(message)
      .build();
  }

  private static String computeSignature(String body, String webhookSecret) {
    HmacUtils hmacUtils = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, webhookSecret);
    return SHA_256_PREFIX + hmacUtils.hmacHex(body);
  }

  private static boolean checkEqualityTimingAttacksSafe(String githubSignature, String computedSignature) {
    return MessageDigest.isEqual(githubSignature.getBytes(UTF_8), computedSignature.getBytes(UTF_8));
  }

  private Optional<UserAuthResult> createAuthResult(HttpRequest request) {
    UserAuthResult userAuthResult = UserAuthResult.withGithubWebhook();
    authenticationEvent.loginSuccess(request, GITHUB_WEBHOOK_USER_NAME, AuthenticationEvent.Source.githubWebhook());
    return Optional.of(userAuthResult);
  }
}
