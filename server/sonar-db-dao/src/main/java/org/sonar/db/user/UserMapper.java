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
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.api.user.UserQuery;

public interface UserMapper {

  @CheckForNull
  UserDto selectByLogin(String login);

  /**
   * Search for a user by SCM account, login or email.
   * Can return multiple results if an email is used by many users (For instance, technical account can use the same email as a none technical account)
   */
  @CheckForNull
  List<UserDto> selectNullableByScmAccountOrLoginOrEmail(@Param("scmAccount") String scmAccountOrLoginOrEmail, @Param("likeScmAccount") String likeScmAccount);

  @CheckForNull
  UserDto selectUser(int userId);

  /**
   * Select user by login. Note that disabled users are ignored.
   */
  @CheckForNull
  UserDto selectUserByLogin(String login);

  List<UserDto> selectUsers(UserQuery query);

  List<UserDto> selectByLogins(List<String> logins);

  List<UserDto> selectByIds(@Param("ids") List<Integer> ids);

  @CheckForNull
  UserDto selectByEmail(String email);

  void scrollAll(ResultHandler<UserDto> handler);

  /**
   * Count actives users which are root and which login is not the specified one.
   */
  long countRootUsersButLogin(@Param("login") String login);

  void insert(@Param("user") UserDto userDto, @Param("now") long now);

  void update(@Param("user") UserDto userDto, @Param("now") long now);

  void setRoot(@Param("login") String login, @Param("root") boolean root, @Param("now") long now);

  void deactivateUser(@Param("login") String login, @Param("now") long now);

  void clearHomepage(@Param("homepageType") String type, @Param("homepageParameter") String value, @Param("now") long now);
}
