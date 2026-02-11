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
package org.sonar.server.users;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonarsource.users.api.UsersQuery;
import org.sonarsource.users.api.UsersQueryBuilder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsersQueryValidatorTest {

  private static final String USER_ID = "bcadf593-b507-479b-b19f-ef6f014558f8";

  @Test
  void validateQuery_shouldAcceptIdsOnly() {
    UsersQuery query = UsersQueryBuilder.builder()
      .ids(List.of(USER_ID))
      .build();

    assertThatCode(() -> UsersQueryValidator.validateQuery(query))
      .doesNotThrowAnyException();
  }

  @Test
  void validateQuery_shouldAcceptLoginsOnly() {
    UsersQuery query = UsersQueryBuilder.builder()
      .logins(List.of("user1", "user2"))
      .build();

    assertThatCode(() -> UsersQueryValidator.validateQuery(query))
      .doesNotThrowAnyException();
  }

  @Test
  void validateQuery_shouldRejectBothIdsAndLogins() {
    UsersQuery query = UsersQueryBuilder.builder()
      .ids(List.of(USER_ID))
      .logins(List.of("user1"))
      .build();

    assertThatThrownBy(() -> UsersQueryValidator.validateQuery(query))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Cannot specify both ids and logins");
  }

  @Test
  void validateQuery_shouldRejectNeitherIdsNorLogins() {
    UsersQuery query = UsersQueryBuilder.builder().build();

    assertThatThrownBy(() -> UsersQueryValidator.validateQuery(query))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Must specify either ids or logins");
  }

  @Test
  void validateQuery_shouldRejectEmptyLists() {
    UsersQuery query = UsersQueryBuilder.builder()
      .ids(List.of())
      .logins(List.of())
      .build();

    assertThatThrownBy(() -> UsersQueryValidator.validateQuery(query))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Must specify either ids or logins");
  }
}
