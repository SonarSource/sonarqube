/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.auth.github.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccessTokenTest {

  @Test
  void equals_whenSameInstance_shouldReturnTrue() {
    UserAccessToken userAccessToken = new UserAccessToken("token");

    assertThat(userAccessToken.equals(userAccessToken)).isTrue();
  }

  @Test
  void equals_whenAnotherInstanceButSameToken_shouldReturnTrue() {
    UserAccessToken userAccessToken1 = new UserAccessToken("token");
    UserAccessToken userAccessToken2 = new UserAccessToken("token");

    assertThat(userAccessToken1.equals(userAccessToken2)).isTrue();
    assertThat(userAccessToken1).hasSameHashCodeAs(userAccessToken2);
  }

  @Test
  void equals_whenAnotherInstanceAndDifferentToken_shouldReturnFalse() {
    UserAccessToken userAccessToken1 = new UserAccessToken("token1");
    UserAccessToken userAccessToken2 = new UserAccessToken("token2");

    assertThat(userAccessToken1.equals(userAccessToken2)).isFalse();
  }

}
