/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.user;

import org.apache.ibatis.session.SqlSession;
import org.sonar.core.persistence.MyBatis;

/**
 * @since 3.2
 */
public class UserDao {
  private final MyBatis mybatis;

  public UserDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  /**
   * Search for user by login. Disabled users are ignored.
   *
   * @return the user, null if user not found
   */

  public UserDto selectUserByLogin(String login) {
    SqlSession session = mybatis.openSession();
    try {
      UserMapper mapper = session.getMapper(UserMapper.class);
      return mapper.selectUserByLogin(login);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  /**
   * Search for group by name.
   *
   * @return the group, null if group not found
   */
  public GroupDto selectGroupByName(String name) {
    SqlSession session = mybatis.openSession();
    try {
      UserMapper mapper = session.getMapper(UserMapper.class);
      return mapper.selectGroupByName(name);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
