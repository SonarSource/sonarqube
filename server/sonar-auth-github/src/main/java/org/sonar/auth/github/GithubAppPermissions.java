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
 * {@link #TOKEN_MINTING_PERMISSIONS} is the (larger) set validated specifically before minting an
 * installation token for the orchestrator (SONAR-30903/SONAR-31023), since pushing a remediation
 * commit needs {@code contents: write} that most already-installed apps don't have yet.
 * <p>
 * {@link #MANIFEST_PERMISSIONS} and {@link #TOKEN_MINTING_PERMISSIONS} MUST always be supersets of
 * {@link #REQUIRED_PERMISSIONS} so that an app created from the manifest passes validation. This
 * invariant holds by construction (both sets are built from the required set) and is additionally
 * guarded by a unit test.
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
   * Permissions validated specifically before minting an orchestrator installation token
   * (SONAR-31023), instead of the plain {@link #REQUIRED_PERMISSIONS}: minting is pointless if the
   * resulting token can't push the remediation commit it's minted for. Kept separate from
   * {@link #REQUIRED_PERMISSIONS} so this stricter check only applies to that one flow — it must
   * not become a blanket requirement re-validated on every existing GitHub ALM integration
   * (PR decoration, provisioning, ...), which would break every already-configured app that
   * doesn't have {@code contents: write} yet.
   */
  public static final Map<String, String> TOKEN_MINTING_PERMISSIONS = Map.copyOf(tokenMintingPermissions());

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
    // pull request analysis & decoration, and pushing remediation commits (SONAR-31023)
    permissions.put("contents", WRITE);
    // authentication & provisioning
    permissions.put("administration", READ);
    permissions.put("members", READ);
    permissions.put("organization_administration", READ);
    permissions.put("organization_projects", READ);
    permissions.put("emails", READ);
    return permissions;
  }

  private static Map<String, String> tokenMintingPermissions() {
    Map<String, String> permissions = new LinkedHashMap<>(REQUIRED_PERMISSIONS);
    permissions.put("contents", WRITE);
    return permissions;
  }
}
