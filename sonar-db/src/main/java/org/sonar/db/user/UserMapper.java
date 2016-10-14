/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
  UserDto selectUser(long userId);

  /**
   * Select user by login. Note that disabled users are ignored.
   */
  @CheckForNull
  UserDto selectUserByLogin(String login);

  List<UserDto> selectUsers(UserQuery query);

  List<UserDto> selectByLogins(List<String> logins);

  List<UserDto> selectByIds(@Param("ids") List<Long> ids);

  long countByEmail(String email);

  /**
   * Count actives users which are root and which login is not the specified one.
   */
  long countRootUsersButLogin(@Param("login") String login);

  void insert(UserDto userDto);

  void update(UserDto userDto);

  void setRoot(@Param("login") String login, @Param("root") boolean root, @Param("now") long now);

  void removeUserFromGroups(long userId);

  void deleteUserActiveDashboards(long userId);

  void deleteUnsharedUserDashboards(long userId);

  void deleteUnsharedUserIssueFilters(String login);

  void deleteUserIssueFilterFavourites(String login);

  void deleteUnsharedUserMeasureFilters(long userId);

  void deleteUserMeasureFilterFavourites(long userId);

  void deleteUserProperties(long userId);

  void deleteUserRoles(long userId);

  void deletePropertiesMatchingLogin(@Param("propertyKeys") List<String> propertyKeys, @Param("login") String login);

  void deactivateUser(@Param("id") long userId, @Param("now") long now);

  int updateRootUser(@Param("userId") long userId, @Param("defaultOrganizationUuid") String defaultOrganizationUuid, @Param("now") long now);

  int updateNonRootUser(@Param("userId") long userId, @Param("defaultOrganizationUuid") String defaultOrganizationUuid, @Param("now") long now);

}
