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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.avatar.AvatarResolverImpl;
import org.sonarsource.users.api.UsersQuery;
import org.sonarsource.users.api.UsersQueryBuilder;
import org.sonarsource.users.api.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsersServiceImplTest {

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private UsersServiceImpl underTest;

  @BeforeEach
  void setUp() {
    underTest = new UsersServiceImpl(db.getDbClient(), new AvatarResolverImpl());
  }

  @Test
  void findUsers_byIds_shouldReturnMatchingUsers() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertUser(); // user3 not queried

    UsersQuery query = UsersQueryBuilder.builder()
      .ids(List.of(user1.getUuid(), user2.getUuid()))
      .build();

    List<User> users = underTest.findUsers(query);

    assertThat(users).hasSize(2);
    assertThat(users).extracting(User::login).containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin());
    assertThat(users).extracting(User::id).containsExactlyInAnyOrder(
      user1.getUuid(),
      user2.getUuid()
    );
  }

  @Test
  void findUsers_byLogins_shouldReturnMatchingUsers() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertUser(); // user3 not queried

    UsersQuery query = UsersQueryBuilder.builder()
      .logins(List.of(user1.getLogin(), user2.getLogin()))
      .build();

    List<User> users = underTest.findUsers(query);

    assertThat(users).hasSize(2);
    assertThat(users).extracting(User::login).containsExactlyInAnyOrder(user1.getLogin(), user2.getLogin());
  }

  @Test
  void findUsers_shouldMapAllFields() {
    UserDto userDto = db.users().insertUser(u -> u
      .setLogin("testuser")
      .setName("Test User")
      .setEmail("test@example.com")
      .setExternalIdentityProvider("github")
      .setExternalLogin("test-github")
      .setActive(true)
      .setLocal(false));

    UsersQuery query = UsersQueryBuilder.builder()
      .logins(List.of("testuser"))
      .build();

    List<User> users = underTest.findUsers(query);

    assertThat(users).hasSize(1);
    User user = users.getFirst();
    assertThat(user.id()).isEqualTo(userDto.getUuid());
    assertThat(user.login()).isEqualTo("testuser");
    assertThat(user.name()).isEqualTo("Test User");
    assertThat(user.email()).isEqualTo("test@example.com");
    assertThat(user.externalProvider()).isEqualTo("github");
    assertThat(user.externalLogin()).isEqualTo("test-github");
    assertThat(user.active()).isTrue();
    assertThat(user.isSsoUser()).isFalse();
    assertThat(user.avatar()).isNotNull();
    assertThat(user.createdAt()).isNotNull();
  }

  @Test
  void findUsers_shouldGenerateAvatar() {
    UserDto userDto = db.users().insertUser(u -> u.setEmail("test@example.com"));

    UsersQuery query = UsersQueryBuilder.builder()
      .logins(List.of(userDto.getLogin()))
      .build();

    List<User> users = underTest.findUsers(query);

    assertThat(users).hasSize(1);
    assertThat(users.getFirst().avatar()).isNotNull();
  }

  @Test
  void findUsers_shouldThrowWhenBothIdsAndLogins() {
    UsersQuery query = UsersQueryBuilder.builder()
      .ids(List.of("bcadf593-b507-479b-b19f-ef6f014558f8"))
      .logins(List.of("user1"))
      .build();

    assertThatThrownBy(() -> underTest.findUsers(query))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Cannot specify both ids and logins");
  }

  @Test
  void findUsers_shouldThrowWhenNeitherIdsNorLogins() {
    UsersQuery query = UsersQueryBuilder.builder().build();

    assertThatThrownBy(() -> underTest.findUsers(query))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Must specify either ids or logins");
  }

  @Test
  void findUsers_shouldReturnEmptyListWhenNoMatches() {
    UsersQuery query = UsersQueryBuilder.builder()
      .logins(List.of("nonexistent"))
      .build();

    List<User> users = underTest.findUsers(query);

    assertThat(users).isEmpty();
  }

  @Test
  void findUsers_shouldMapLocalUserCorrectly() {
    UserDto userDto = db.users().insertUser(u -> u.setLocal(true));

    UsersQuery query = UsersQueryBuilder.builder()
      .logins(List.of(userDto.getLogin()))
      .build();

    List<User> users = underTest.findUsers(query);

    assertThat(users).hasSize(1);
    assertThat(users.getFirst().local()).isTrue();
  }
}
