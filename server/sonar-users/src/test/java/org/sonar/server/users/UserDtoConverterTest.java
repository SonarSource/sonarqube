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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.avatar.AvatarResolver;
import org.sonarsource.users.api.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDtoConverterTest {

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final AvatarResolver avatarResolver = mock(AvatarResolver.class);

  @Test
  void toApiUser_shouldMapAllFields() {
    long lastConnectionMillis = 1609459200000L; // 2021-01-01 00:00:00 UTC

    UserDto dto = db.users().insertUser(u -> u
      .setLogin("testuser")
      .setName("Test User")
      .setEmail("test@example.com")
      .setExternalIdentityProvider("github")
      .setExternalLogin("test-github")
      .setLastConnectionDate(lastConnectionMillis)
      .setActive(true)
      .setLocal(false));

    when(avatarResolver.create(dto)).thenReturn("avatar-hash-123");

    User user = UserDtoConverter.toApiUser(dto, avatarResolver);

    assertThat(user.id()).isEqualTo(dto.getUuid());
    assertThat(user.login()).isEqualTo("testuser");
    assertThat(user.name()).isEqualTo("Test User");
    assertThat(user.email()).isEqualTo("test@example.com");
    assertThat(user.externalProvider()).isEqualTo("github");
    assertThat(user.externalLogin()).isEqualTo("test-github");
    assertThat(user.avatar()).isEqualTo("avatar-hash-123");
    assertThat(user.lastConnectionDate()).isEqualTo(OffsetDateTime.ofInstant(Instant.ofEpochMilli(lastConnectionMillis), ZoneOffset.UTC));
    assertThat(user.createdAt()).isNotNull();
    assertThat(user.active()).isTrue();
    assertThat(user.isSsoUser()).isFalse();
    assertThat(user.legacyUuid()).isNull();
  }

  @Test
  void toApiUser_shouldHandleNullableFields() {
    UserDto dto = db.users().insertUser(u -> u
      .setLogin("testuser")
      .setName(null)
      .setEmail(null)
      .setExternalIdentityProvider("github")
      .setExternalLogin("test-github")
      .setLastConnectionDate(null)
      .setActive(true)
      .setLocal(true));

    when(avatarResolver.create(dto)).thenReturn(null);

    User user = UserDtoConverter.toApiUser(dto, avatarResolver);

    assertThat(user.name()).isNull();
    assertThat(user.email()).isNull();
    assertThat(user.avatar()).isNull();
    assertThat(user.lastConnectionDate()).isNull();
    assertThat(user.isSsoUser()).isFalse();
  }

  @Test
  void toApiUser_shouldMapLocalUserCorrectly() {
    UserDto dto = db.users().insertUser(u -> u
      .setLogin("localuser")
      .setName("Local User")
      .setEmail("local@example.com")
      .setExternalIdentityProvider("sonarqube")
      .setExternalLogin("localuser")
      .setActive(true)
      .setLocal(true));

    when(avatarResolver.create(dto)).thenReturn("local-avatar");

    User user = UserDtoConverter.toApiUser(dto, avatarResolver);

    assertThat(user.local()).isTrue();
  }

  @Test
  void toApiUser_shouldMapSsoUserCorrectly() {
    UserDto dto = db.users().insertUser(u -> u
      .setLogin("ssouser")
      .setName("SSO User")
      .setEmail("sso@example.com")
      .setExternalIdentityProvider("okta")
      .setExternalLogin("sso-okta")
      .setActive(true)
      .setLocal(false));

    when(avatarResolver.create(dto)).thenReturn("sso-avatar");

    User user = UserDtoConverter.toApiUser(dto, avatarResolver);

    assertThat(user.isSsoUser()).isFalse();
  }

  @Test
  void toApiUser_shouldMapInactiveUser() {
    UserDto dto = db.users().insertUser(u -> u
      .setLogin("inactiveuser")
      .setName("Inactive User")
      .setExternalIdentityProvider("github")
      .setExternalLogin("inactive-github")
      .setActive(false)
      .setLocal(true));

    when(avatarResolver.create(dto)).thenReturn("inactive-avatar");

    User user = UserDtoConverter.toApiUser(dto, avatarResolver);

    assertThat(user.active()).isFalse();
  }
}
