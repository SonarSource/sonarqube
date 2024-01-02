/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.issue.ws;

import org.sonar.db.user.UserDto;
import org.sonar.server.issue.AvatarResolver;
import org.sonarqube.ws.Common;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.nullToEmpty;
import static java.util.Optional.ofNullable;

public class UserResponseFormatter {
  private final AvatarResolver avatarResolver;

  public UserResponseFormatter(AvatarResolver avatarResolver) {
    this.avatarResolver = avatarResolver;
  }

  public Common.User formatUser(Common.User.Builder builder, UserDto user) {
    builder
      .clear()
      .setLogin(user.getLogin())
      .setName(nullToEmpty(user.getName()))
      .setActive(user.isActive());
    ofNullable(emptyToNull(user.getEmail())).ifPresent(email -> builder.setAvatar(avatarResolver.create(user)));
    return builder.build();
  }
}
