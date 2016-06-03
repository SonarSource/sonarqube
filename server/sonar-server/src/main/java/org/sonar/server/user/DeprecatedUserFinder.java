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
package org.sonar.server.user;

import javax.annotation.Nullable;
import org.sonar.api.database.model.User;
import org.sonar.api.security.UserFinder;
import org.sonar.db.user.UserDao;
import org.sonar.db.user.UserDto;

/**
 * @since 2.10
 */
public class DeprecatedUserFinder implements UserFinder {

  private final UserDao userDao;

  public DeprecatedUserFinder(UserDao userDao) {
    this.userDao = userDao;
  }

  @Override
  public User findById(int id) {
    return copy(userDao.selectUserById(id));
  }

  @Override
  public User findByLogin(String login) {
    return copy(userDao.selectActiveUserByLogin(login));
  }

  private static User copy(@Nullable UserDto dto) {
    if (dto != null) {
      User user = new User().setEmail(dto.getEmail()).setLogin(dto.getLogin()).setName(dto.getName());
      user.setId(dto.getId().intValue());
      return user;
    }
    return null;
  }

}
