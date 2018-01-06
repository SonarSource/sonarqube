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
package org.sonar.db.user;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface UserTokenMapper {
  void insert(UserTokenDto userToken);

  UserTokenDto selectByTokenHash(String tokenHash);

  UserTokenDto selectByLoginAndName(@Param("login") String login, @Param("name") String name);

  List<UserTokenDto> selectByLogin(String login);

  void deleteByLogin(String login);

  void deleteByLoginAndName(@Param("login") String login, @Param("name") String name);

  List<UserTokenCount> countTokensByLogins(@Param("logins") List<String> logins);
}
