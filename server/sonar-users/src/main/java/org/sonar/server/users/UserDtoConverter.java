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
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.avatar.AvatarResolver;
import org.sonarsource.users.api.model.User;

class UserDtoConverter {

  private UserDtoConverter() {
    // Utility class
  }

  static User toApiUser(UserDto dto, AvatarResolver avatarResolver) {
    return new User(
      dto.getUuid(),
      dto.getLogin(),
      dto.getName(),
      dto.getEmail(),
      dto.getExternalIdentityProvider(),
      avatarResolver.create(dto),
      toOffsetDateTime(dto.getLastConnectionDate()),
      toOffsetDateTime(dto.getCreatedAt()),
      dto.getExternalLogin(),
      null,
      dto.isActive(),
      false,
      dto.isLocal()
    );
  }

  @CheckForNull
  private static OffsetDateTime toOffsetDateTime(@Nullable Long timestampMillis) {
    if (timestampMillis == null) {
      return null;
    }
    return OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestampMillis), ZoneOffset.UTC);
  }
}
