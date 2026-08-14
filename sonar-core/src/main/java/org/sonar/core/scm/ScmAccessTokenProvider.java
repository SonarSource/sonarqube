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
package org.sonar.core.scm;

import java.util.Optional;

/**
 * DOP-agnostic SPI (generalises the GitHub-only {@code GithubInstallationTokenProvider}, SONAR-31165)
 * for minting a scoped, short-lived git credential for a project's bound DevOps Platform.
 *
 * <p>One {@code @ServerSide} implementation per ALM, each responsible for recognising whether it
 * applies to the given project (returning {@code Optional.empty()} otherwise) — the same
 * self-filtering-delegate pattern already used by {@code DevOpsProjectCreatorFactory}/
 * {@code DelegatingDevOpsProjectCreatorFactory} in sonar-webserver-common. A {@code
 * DelegatingScmAccessTokenProvider} fans a single {@link #mint} call out to every registered
 * implementation and returns the first non-empty result. Adding a new platform means adding a new
 * implementation, not modifying the delegate.
 */
public interface ScmAccessTokenProvider {

  /**
   * @param projectKey key of the SonarQube project to mint a token for
   * @return the minted credential, or {@code Optional.empty()} if the project is not bound to this
   *   provider's ALM, or the binding is otherwise not usable (e.g. missing repository configuration)
   * @throws IllegalArgumentException if the project is bound to this provider's ALM but its DevOps
   *   Platform configuration is invalid (bad credentials, missing permissions, unreachable API, ...)
   * @throws RuntimeException if the project is otherwise correctly bound but the minting API call
   *   itself failed — a transient/upstream failure, distinct from the "not bound" cases above
   */
  Optional<ScmAccessToken> mint(String projectKey);
}
