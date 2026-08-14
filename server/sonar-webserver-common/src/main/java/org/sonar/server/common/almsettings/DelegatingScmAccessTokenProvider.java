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
package org.sonar.server.common.almsettings;

import java.util.Optional;
import java.util.Set;
import jakarta.annotation.Priority;
import org.sonar.api.server.ServerSide;
import org.sonar.core.scm.ScmAccessToken;
import org.sonar.core.scm.ScmAccessTokenProvider;

/**
 * Fans a single {@link #mint} call out to every registered per-ALM {@link ScmAccessTokenProvider} and
 * returns the first non-empty result — the same self-filtering-delegate convention as {@link
 * DelegatingDevOpsProjectCreatorFactory}. This is the extensibility point (SONAR-31165): adding a new
 * DevOps Platform means registering one more {@link ScmAccessTokenProvider} bean, not modifying this
 * class.
 */
@ServerSide
@Priority(1)
public class DelegatingScmAccessTokenProvider implements ScmAccessTokenProvider {

  private final Set<ScmAccessTokenProvider> delegates;

  public DelegatingScmAccessTokenProvider(Set<ScmAccessTokenProvider> delegates) {
    this.delegates = delegates;
  }

  @Override
  public Optional<ScmAccessToken> mint(String projectKey) {
    return delegates.stream()
      .flatMap(delegate -> delegate.mint(projectKey).stream())
      .findFirst();
  }
}
