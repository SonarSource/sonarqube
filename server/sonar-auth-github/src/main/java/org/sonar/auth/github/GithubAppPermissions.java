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
package org.sonar.auth.github;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for the GitHub App permissions and webhook events used by SonarQube.
 * <p>
 * {@link #REQUIRED_PERMISSIONS} is the minimal set validated against an already-configured app
 * (see {@code GithubApplicationClientImpl.checkAppPermissions}). {@link #MANIFEST_PERMISSIONS} is
 * the full set requested when registering a new app through the
 * <a href="https://docs.github.com/en/apps/sharing-github-apps/registering-a-github-app-from-a-manifest">GitHub App Manifest flow</a>.
 * <p>
 * {@link #MANIFEST_PERMISSIONS} MUST always be a superset of {@link #REQUIRED_PERMISSIONS} so that an
 * app created from the manifest passes validation. This invariant holds by construction (the manifest
 * set is built from the required set) and is additionally guarded by a unit test.
 */
public final class GithubAppPermissions {

  public static final String READ = "read";
  public static final String WRITE = "write";

  /**
   * Permissions that must be granted on any GitHub App bound to SonarQube. Validated on every
   * configuration check.
   */
  public static final Map<String, String> REQUIRED_PERMISSIONS = Map.of(
    "checks", WRITE,
    "pull_requests", WRITE,
    "metadata", READ);

  /**
   * Full permission set requested when registering a GitHub App from a manifest. Mirrors the
   * permissions documented for setting up a GitHub App for SonarQube Server, covering pull request
   * analysis/decoration and authentication & provisioning.
   * <p>
   * Built on top of {@link #REQUIRED_PERMISSIONS} so the two cannot drift apart: every required
   * permission is inherited as-is, and the manifest only adds extra scopes on top.
   *
   * @see <a href="https://docs.sonarsource.com/sonarqube-server/instance-administration/devops-platforms/github/setting-up-github-app">Setting up a GitHub App</a>
   */
  public static final Map<String, String> MANIFEST_PERMISSIONS = Map.copyOf(manifestPermissions());

  /**
   * Webhook events the app subscribes to by default. Empty: pull request decoration is performed
   * through the GitHub API and provisioning through a scheduled sync, so no webhook events are
   * required. (Code scanning alert reporting can be enabled manually afterwards.)
   */
  public static final List<String> MANIFEST_EVENTS = List.of();

  private GithubAppPermissions() {
    // utility class
  }

  private static Map<String, String> manifestPermissions() {
    // Seed from the required permissions so the manifest can never request less than what
    // checkAppPermissions validates; only additional scopes are layered on top.
    Map<String, String> permissions = new LinkedHashMap<>(REQUIRED_PERMISSIONS);
    // pull request analysis & decoration
    permissions.put("contents", READ);
    // authentication & provisioning
    permissions.put("administration", READ);
    permissions.put("members", READ);
    permissions.put("organization_administration", READ);
    permissions.put("organization_projects", READ);
    permissions.put("emails", READ);
    return permissions;
  }
}
