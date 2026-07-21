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
package org.sonar.server.v2.api.github.installationtoken.controller;

import org.sonar.core.scm.github.GithubInstallationToken;
import org.sonar.core.scm.github.GithubInstallationTokenProvider;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.github.installationtoken.response.GithubInstallationTokenRestResponse;

import static java.lang.String.format;

public class DefaultGithubInstallationTokenController implements GithubInstallationTokenController {

  private final UserSession userSession;
  private final GithubInstallationTokenProvider tokenProvider;

  public DefaultGithubInstallationTokenController(UserSession userSession, GithubInstallationTokenProvider tokenProvider) {
    this.userSession = userSession;
    this.tokenProvider = tokenProvider;
  }

  @Override
  public GithubInstallationTokenRestResponse generateInstallationToken(String projectKey) {
    // Minting yields a token that can push branches and open pull requests on the bound
    // repository — a materially more sensitive operation than reading binding metadata (which
    // only requires 'Browse', see GithubConfigurationController). Restricting to system
    // administrators is a deliberately conservative default until the orchestrator↔SonarQube
    // trust model is hardened (tracked as a SONAR-30903 follow-up).
    userSession.checkIsSystemAdministrator();

    GithubInstallationToken token = tokenProvider.mint(projectKey)
      .orElseThrow(() -> new NotFoundException(format(
        "Failed to mint a GitHub installation token for project '%s' — is it bound to a working GitHub App installation?", projectKey)));

    return new GithubInstallationTokenRestResponse(token.token(), token.expiresAt());
  }
}
