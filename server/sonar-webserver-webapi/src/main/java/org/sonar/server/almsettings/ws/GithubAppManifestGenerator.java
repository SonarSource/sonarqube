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

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.github.GithubAppPermissions;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.Strings.CS;

/**
 * Builds the JSON manifest and the GitHub URL used to register a GitHub App through the
 * <a href="https://docs.github.com/en/apps/sharing-github-apps/registering-a-github-app-from-a-manifest">App Manifest flow</a>.
 * Only github.com is supported (personal and organization accounts).
 */
@ServerSide
public class GithubAppManifestGenerator {

  static final String GITHUB_DOTCOM_BASE_URL = "https://github.com";
  static final String GITHUB_DOTCOM_API_URL = "https://api.github.com";
  // These are fixed internal SonarQube routes, not configurable endpoints, so S1075 does not apply.
  @SuppressWarnings("java:S1075")
  static final String CALLBACK_PATH = "/github/manifest/callback";
  // DevOps Platform integration settings page the admin started from; users are returned here after
  // installing the App.
  @SuppressWarnings("java:S1075")
  static final String SETTINGS_PATH = "/admin/settings?category=almintegration";
  // GitHub authentication settings page.
  @SuppressWarnings("java:S1075")
  static final String AUTH_SETTINGS_PATH = "/admin/settings?category=authentication&tab=github";

  private static final Gson GSON = new Gson();

  private final Server server;

  public GithubAppManifestGenerator(Server server) {
    this.server = server;
  }

  public String generateManifest(String appName, String setupPath) {
    String baseUrl = baseUrl();
    Map<String, Object> manifest = new LinkedHashMap<>();
    manifest.put("name", appName);
    manifest.put("url", baseUrl);
    // No webhook by default: SonarQube does not need GitHub to push events for PR analysis or
    // provisioning. Admins can enable a webhook manually for code scanning alert reporting.
    manifest.put("redirect_url", baseUrl + CALLBACK_PATH);
    // A single base-URL callback is enough: GitHub matches OAuth redirect_uri values against the
    // registered callback URLs by path prefix, so both the sign-in callback (/oauth2/callback/github)
    // and the project import redirect (/projects/create) are covered.
    manifest.put("callback_urls", List.of(baseUrl));
    // After installation, GitHub returns the user to the setup URL (the settings page they started
    // from), appending installation_id/setup_action.
    manifest.put("setup_url", baseUrl + setupPath);
    // Public ("Any account"), as recommended by the SonarQube docs. This also makes the public
    // installation URL (github.com/apps/{slug}/installations/new) resolve; private apps 404 there.
    manifest.put("public", true);
    // sorted for deterministic output (eases testing and review)
    manifest.put("default_permissions", new TreeMap<>(GithubAppPermissions.MANIFEST_PERMISSIONS));
    manifest.put("default_events", GithubAppPermissions.MANIFEST_EVENTS);
    manifest.put("request_oauth_on_install", true);
    return GSON.toJson(manifest);
  }

  public String githubAppCreationUrl(@Nullable String organization) {
    if (isBlank(organization)) {
      return GITHUB_DOTCOM_BASE_URL + "/settings/apps/new";
    }
    String encodedOrg = UrlEscapers.urlPathSegmentEscaper().escape(organization);
    return GITHUB_DOTCOM_BASE_URL + "/organizations/" + encodedOrg + "/settings/apps/new";
  }

  public String baseUrl() {
    return CS.removeEnd(server.getPublicRootUrl(), "/");
  }
}
