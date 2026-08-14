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
import org.junit.Test;
import org.sonar.core.scm.ScmAccessToken;
import org.sonar.core.scm.ScmAccessTokenProvider;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DelegatingScmAccessTokenProviderTest {

  private static final String PROJECT_KEY = "my-project";

  @Test
  public void mint_whenNoDelegates_shouldReturnEmptyOptional() {
    DelegatingScmAccessTokenProvider noDelegates = new DelegatingScmAccessTokenProvider(emptySet());

    assertThat(noDelegates.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenNoDelegateMintsAToken_shouldReturnEmptyOptional() {
    DelegatingScmAccessTokenProvider delegates = new DelegatingScmAccessTokenProvider(Set.of(mock(), mock()));

    assertThat(delegates.mint(PROJECT_KEY)).isEmpty();
  }

  @Test
  public void mint_whenOneDelegateMintsAToken_shouldReturnIt() {
    ScmAccessTokenProvider successfulDelegate = mock();
    ScmAccessToken token = new ScmAccessToken("gitlab", "sonarqube-remediation-agent", "glpat-abc123", "2026-07-16");
    when(successfulDelegate.mint(PROJECT_KEY)).thenReturn(Optional.of(token));
    DelegatingScmAccessTokenProvider delegates = new DelegatingScmAccessTokenProvider(Set.of(mock(), successfulDelegate));

    assertThat(delegates.mint(PROJECT_KEY)).contains(token);
  }
}
