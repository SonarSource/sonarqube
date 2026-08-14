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
import org.sonar.api.server.ServerSide;
import org.sonar.core.scm.ScmAccessToken;
import org.sonar.core.scm.ScmAccessTokenProvider;
import org.sonar.core.scm.github.GithubInstallationToken;
import org.sonar.core.scm.github.GithubInstallationTokenProvider;
import org.sonar.db.alm.setting.ALM;

/**
 * Adapts the existing, GitHub-only {@link GithubInstallationTokenProvider} (SONAR-30903) to the
 * DOP-agnostic {@link ScmAccessTokenProvider} SPI (SONAR-31165), without touching the working, tested
 * minting logic in {@link GithubInstallationTokenProviderImpl}. GitHub App installation tokens
 * authenticate over git as {@code x-access-token:<token>@host}, so that literal is the credential's
 * {@code username}.
 */
@ServerSide
public class GithubScmAccessTokenProviderAdapter implements ScmAccessTokenProvider {

  private static final String GIT_USERNAME = "x-access-token";

  private final GithubInstallationTokenProvider githubInstallationTokenProvider;

  public GithubScmAccessTokenProviderAdapter(GithubInstallationTokenProvider githubInstallationTokenProvider) {
    this.githubInstallationTokenProvider = githubInstallationTokenProvider;
  }

  @Override
  public Optional<ScmAccessToken> mint(String projectKey) {
    return githubInstallationTokenProvider.mint(projectKey)
      .map(GithubScmAccessTokenProviderAdapter::toScmAccessToken);
  }

  private static ScmAccessToken toScmAccessToken(GithubInstallationToken token) {
    return new ScmAccessToken(ALM.GITHUB.getId(), GIT_USERNAME, token.token(), token.expiresAt());
  }
}
