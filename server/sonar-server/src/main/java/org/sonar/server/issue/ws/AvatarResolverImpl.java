/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.hash.Hashing;
import org.sonar.db.user.UserDto;

import static com.google.common.base.Strings.emptyToNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;

public class AvatarResolverImpl implements AvatarResolver {

  @Override
  public String create(UserDto user) {
    UserDto userDto = requireNonNull(user, "User cannot be null");
    return hash(requireNonNull(emptyToNull(userDto.getEmail()), "Email cannot be null"));
  }

  private static String hash(String text) {
    return Hashing.md5().hashString(text.toLowerCase(ENGLISH), UTF_8).toString();
  }
}
