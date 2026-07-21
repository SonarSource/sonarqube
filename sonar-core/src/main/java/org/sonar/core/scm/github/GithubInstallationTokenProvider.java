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
package org.sonar.core.scm.github;

import java.util.Optional;

/**
 * Deliberately narrow SPI (SONAR-30903) letting a SonarQube capability mint a GitHub App
 * installation token for a project, without depending on the ALM/DB internals (private key
 * decryption, {@code AlmSettingDao}, {@code GithubApplicationClient}, ...) that only sonar-enterprise
 * modules may compile against. The GitHub App's private key never leaves the implementation.
 *
 * <p>Implemented by a {@code @ServerSide} bean in sonar-enterprise; consumed here as a plain
 * dependency-injected collaborator — the caller and the implementation run in the same JVM (the
 * unification capability's Spring context is a child of the platform's own context), so this is an
 * ordinary in-process call, not a network hop.
 */
public interface GithubInstallationTokenProvider {

  /**
   * @param projectKey key of the SonarQube project to mint a token for
   * @return the minted token and repository metadata, or {@code Optional.empty()} if the project
   *   is not bound to a GitHub App, or the App is not installed on the bound repository
   * @throws IllegalArgumentException if the project is bound to a GitHub App but its configuration
   *   is invalid (bad credentials, missing permissions, unreachable API, ...)
   * @throws RuntimeException (a {@code ServerException} in the sonar-enterprise implementation) if
   *   the project is otherwise correctly bound but the GitHub App API call to mint the token itself
   *   failed — a transient/upstream failure, distinct from the "not bound" cases above
   */
  Optional<GithubInstallationToken> mint(String projectKey);
}
