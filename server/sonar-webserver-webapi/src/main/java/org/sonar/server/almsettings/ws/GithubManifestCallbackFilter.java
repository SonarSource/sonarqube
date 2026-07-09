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
package org.sonar.server.almsettings.ws;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.web.FilterChain;
import org.sonar.api.web.HttpFilter;
import org.sonar.api.web.UrlPattern;
import org.sonar.auth.github.GithubAppCredentials;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.almsettings.ws.GithubManifestStateStore.PendingManifest;
import org.sonar.server.common.almsettings.telemetry.DevOpsConfigurationTelemetry;
import org.sonar.server.common.github.config.GithubConfiguration;
import org.sonar.server.common.github.config.GithubConfigurationService;
import org.sonar.server.common.gitlab.config.ProvisioningType;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.sonar.server.almsettings.ws.GithubAppManifestGenerator.AUTH_SETTINGS_PATH;
import static org.sonar.server.almsettings.ws.GithubAppManifestGenerator.CALLBACK_PATH;
import static org.sonar.server.almsettings.ws.GithubAppManifestGenerator.GITHUB_DOTCOM_API_URL;
import static org.sonar.server.almsettings.ws.GithubAppManifestGenerator.GITHUB_DOTCOM_BASE_URL;
import static org.sonar.server.almsettings.ws.GithubAppManifestGenerator.SETTINGS_PATH;

/**
 * Handles GitHub's redirect at the end of the App Manifest flow. Validates the single-use {@code state},
 * exchanges the temporary {@code code} for the new App's credentials, persists the GitHub ALM setting,
 * and redirects the browser back to the DevOps integration settings page.
 */
public class GithubManifestCallbackFilter extends HttpFilter {

  private static final Logger LOG = LoggerFactory.getLogger(GithubManifestCallbackFilter.class);

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AlmSettingsSupport almSettingsSupport;
  private final GithubAppManifestGenerator manifestGenerator;
  private final GithubManifestStateStore stateStore;
  private final GithubApplicationClient githubApplicationClient;
  private final GithubConfigurationService githubConfigurationService;
  private final DevOpsConfigurationTelemetry devOpsConfigurationTelemetry;

  public GithubManifestCallbackFilter(DbClient dbClient, UserSession userSession, AlmSettingsSupport almSettingsSupport,
    GithubAppManifestGenerator manifestGenerator, GithubManifestStateStore stateStore, GithubApplicationClient githubApplicationClient,
    GithubConfigurationService githubConfigurationService, DevOpsConfigurationTelemetry devOpsConfigurationTelemetry) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.almSettingsSupport = almSettingsSupport;
    this.manifestGenerator = manifestGenerator;
    this.stateStore = stateStore;
    this.githubApplicationClient = githubApplicationClient;
    this.githubConfigurationService = githubConfigurationService;
    this.devOpsConfigurationTelemetry = devOpsConfigurationTelemetry;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create(CALLBACK_PATH);
  }

  @Override
  public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
    String code = request.getParameter("code");
    String state = request.getParameter("state");

    if (isBlank(code) || isBlank(state)) {
      redirect(response, error("GitHub did not return the expected parameters. The App was not created."));
      return;
    }

    if (!userSession.isSystemAdministrator()) {
      redirect(response, error("You must be a system administrator to finish creating the GitHub App."));
      return;
    }

    Optional<PendingManifest> pendingOpt = stateStore.consume(state);
    if (pendingOpt.isEmpty() || !Objects.equals(pendingOpt.get().userUuid(), userSession.getUuid())) {
      redirect(response, error("This GitHub App creation request is invalid or has expired. Please start again."));
      return;
    }

    PendingManifest pending = pendingOpt.get();
    try {
      GithubAppCredentials credentials = githubApplicationClient.convertAppManifest(GITHUB_DOTCOM_API_URL, code);
      PersistResult result = persistConfiguration(pending, credentials);
      sendTelemetry(result);
      // The App now exists but is installed nowhere. Send the user to GitHub to install it on an
      // account/organization so SonarQube can access repositories; without an installation, project
      // import finds no organizations.
      redirect(response, installUrl(credentials).orElseGet(() -> success(pending)));
    } catch (Exception e) {
      LOG.warn("Failed to complete the GitHub App Manifest flow", e);
      redirect(response, error(pending, "Failed to create the GitHub App. Please try again or configure it manually."));
    }
  }

  /**
   * Persists the authentication configuration and the DevOps binding in a single transaction so the two
   * can never diverge: if either write fails, nothing is committed and the user can safely restart the
   * flow. (The GitHub App created on GitHub itself cannot be rolled back; a failed flow may leave it
   * unused, but no inconsistent SonarQube state is left behind.)
   */
  private PersistResult persistConfiguration(PendingManifest pending, GithubAppCredentials credentials) {
    boolean authConfigured = false;
    boolean devopsConfigured = false;
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (pending.setupAuth()) {
        authConfigured = setupAuthentication(dbSession, credentials, pending.organization());
      }
      if (pending.setupDevops()) {
        persistDevopsBinding(dbSession, pending.settingKey(), credentials);
        devopsConfigured = true;
      }
      dbSession.commit();
    }
    return new PersistResult(devopsConfigured, authConfigured);
  }

  private void sendTelemetry(PersistResult result) {
    if (result.devopsConfigured()) {
      devOpsConfigurationTelemetry.sendAutoDevOpsConfig(result.authConfigured());
    }
    if (result.authConfigured()) {
      devOpsConfigurationTelemetry.sendAutoAuthConfig();
    }
  }

  private record PersistResult(boolean devopsConfigured, boolean authConfigured) {
  }

  private static Optional<String> installUrl(GithubAppCredentials credentials) {
    if (credentials.slug() != null) {
      return Optional.of(GITHUB_DOTCOM_BASE_URL + "/apps/" + credentials.slug() + "/installations/new");
    }
    if (credentials.htmlUrl() != null) {
      return Optional.of(credentials.htmlUrl() + "/installations/new");
    }
    return Optional.empty();
  }

  private void persistDevopsBinding(DbSession dbSession, String settingKey, GithubAppCredentials credentials) {
    almSettingsSupport.checkAlmSettingDoesNotAlreadyExist(dbSession, settingKey);
    almSettingsSupport.createGithubSetting(dbSession, new AlmSettingsSupport.NewGithubSetting(settingKey, GITHUB_DOTCOM_API_URL,
      credentials.getAppId(), credentials.pem(), credentials.clientId(), credentials.clientSecret(), credentials.webhookSecret()));
  }

  private boolean setupAuthentication(DbSession dbSession, GithubAppCredentials credentials, @Nullable String organization) {
    Set<String> allowedOrganizations = isBlank(organization) ? Set.of() : Set.of(organization);
    GithubConfiguration configuration = new GithubConfiguration(
      GithubConfigurationService.UNIQUE_GITHUB_CONFIGURATION_ID,
      true,
      credentials.clientId(),
      credentials.clientSecret(),
      credentials.getAppId(),
      credentials.pem(),
      false,
      "https://api.github.com/",
      "https://github.com/",
      allowedOrganizations,
      ProvisioningType.JIT,
      true,
      true,
      false);
    try {
      githubConfigurationService.createConfiguration(dbSession, configuration);
      return true;
    } catch (BadRequestException e) {
      // A GitHub authentication configuration already exists; leave it untouched but continue the flow.
      LOG.warn("Skipping GitHub authentication setup: {}", e.getMessage());
      return false;
    }
  }

  private String success(PendingManifest pending) {
    String url = manifestGenerator.baseUrl() + settingsPathFor(pending) + "&almManifestResult=success";
    if (pending.settingKey() != null) {
      url += "&almKey=" + encode(pending.settingKey());
    }
    return url;
  }

  // Used before the pending state is resolved (blank params, non-admin, invalid/expired state): the
  // originating page is unknown, so fall back to the DevOps integration settings page.
  private String error(String message) {
    return errorOn(SETTINGS_PATH, message);
  }

  // Used once the pending state is known, so the error lands on the page the flow originated from.
  private String error(PendingManifest pending, String message) {
    return errorOn(settingsPathFor(pending), message);
  }

  private String errorOn(String path, String message) {
    return manifestGenerator.baseUrl() + path + "&almManifestResult=error&almError=" + encode(message);
  }

  // Auth-only flows have no DevOps setting key and belong on the GitHub authentication settings page.
  private static String settingsPathFor(PendingManifest pending) {
    return pending.setupDevops() ? SETTINGS_PATH : AUTH_SETTINGS_PATH;
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private static void redirect(HttpResponse response, String url) {
    try {
      response.sendRedirect(url);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to redirect to " + url, e);
    }
  }

  @Override
  public void init() {
    // nothing to do
  }

  @Override
  public void destroy() {
    // nothing to do
  }
}
