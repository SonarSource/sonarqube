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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A single installation of the configured GitHub App, with the permissions actually granted to it.
 *
 * <p>Field names here follow GitHub's own payload ({@code account.login}, {@code account.type}), since this is a direct
 * model of it. Callers that report installations onwards use installation-neutral names, because the owner is an
 * organization on one installation and a personal account on the next.
 *
 * <p>Distinct from {@link GithubAppInstallation}, which models the same GitHub resource for the authentication and
 * provisioning flows: those only ever read the fixed {@link GithubBinding.Permissions} field set, which does not
 * model {@code pull_requests} and has no {@code html_url}. The Remediation Agent needs both — it compares granted
 * permissions against {@link GithubAppPermissions#TOKEN_MINTING_PERMISSIONS} and links administrators to the
 * installation's settings page (SONAR-32166) — so it reads the raw permission map instead of widening the existing
 * record and every one of its callers.
 *
 * @param installationId GitHub's numeric installation id
 * @param accountLogin   login of whoever the app is installed on; GitHub's own {@code account.login}
 * @param accountType    GitHub's own {@code account.type}. Exactly {@code Organization} or {@code User}: the
 *                       conversion boundary rejects any other value rather than guessing at it.
 * @param htmlUrl        the settings page as reported by GitHub, or {@code null} when absent. Not used to build the
 *                       settings link the Remediation Agent shows — see {@code GithubRemediationPermissionChecker}.
 * @param suspended      whether the installation is currently suspended; unrelated to which permissions it granted
 * @param permissions    permissions granted to this installation, keyed by GitHub permission name
 */
public record GithubAppInstallationDetails(
  long installationId,
  String accountLogin,
  String accountType,
  @Nullable String htmlUrl,
  boolean suspended,
  Map<String, String> permissions) {

  private static final String ORGANIZATION_ACCOUNT_TYPE = "Organization";
  private static final String USER_ACCOUNT_TYPE = "User";

  public GithubAppInstallationDetails {
    // Defensive unmodifiable copy rather than Map.copyOf: an unexpected null value in the payload must not blow up
    // here, it is caught by the permission comparison, which treats an absent level as "not granted".
    permissions = permissions == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(permissions));
  }

  /**
   * Whether {@code accountType} is one of the two values GitHub documents. Code converting a GitHub payload has to
   * check this before building an instance, and treat a failure as a failed verification: {@link #isOrganization()}
   * answers with a plain {@code false} for anything it does not recognise, so a missing or unexpected type would
   * quietly become a personal-account installation — and a personal-account settings link that an organization's
   * owner cannot approve anything on.
   */
  public static boolean isKnownAccountType(@Nullable String accountType) {
    return ORGANIZATION_ACCOUNT_TYPE.equals(accountType) || USER_ACCOUNT_TYPE.equals(accountType);
  }

  /**
   * Exact match rather than case-insensitive: the conversion boundary has already rejected anything that is not
   * exactly {@code Organization} or {@code User}, so a differently-cased value is a payload that never reaches here,
   * not one to interpret.
   */
  public boolean isOrganization() {
    return ORGANIZATION_ACCOUNT_TYPE.equals(accountType);
  }
}
