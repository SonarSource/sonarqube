/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.common.user.service;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserCreateRequestTest {
  @Test
  public void build_whenNoPasswordAndUserLocal_shouldThrow() {
    UserCreateRequest.Builder requestBuilder = UserCreateRequest.builder()
      .setPassword(null)
      .setLocal(true);

    assertThatThrownBy(requestBuilder::build)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Password is mandatory and must not be empty");
  }

  @Test
  public void build_whenPasswordSetButUserNonLocal_shouldThrow() {
    UserCreateRequest.Builder requestBuilder = UserCreateRequest.builder()
      .setPassword("password")
      .setLocal(false);

    assertThatThrownBy(requestBuilder::build)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Password should only be set on local user");
  }

}
