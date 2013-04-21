/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.user;

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;

/**
 * @since 3.0
 */
public class AuthorDao implements BatchComponent, ServerComponent {

  private final MyBatis mybatis;

  public AuthorDao(MyBatis mybatis) {
    this.mybatis = mybatis;
  }

  public AuthorDto selectByLogin(String login) {
    SqlSession session = mybatis.openSession();
    try {
      AuthorMapper mapper = session.getMapper(AuthorMapper.class);
      return mapper.selectByLogin(login);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int countDeveloperLogins(long developerId) {
    SqlSession session = mybatis.openSession();
    try {
      AuthorMapper mapper = session.getMapper(AuthorMapper.class);
      return mapper.countDeveloperLogins(developerId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(AuthorDto authorDto) {
    SqlSession session = mybatis.openSession();
    AuthorMapper mapper = session.getMapper(AuthorMapper.class);
    try {
      mapper.insert(authorDto);
      session.commit();
    } catch (RuntimeException e) {
      // break the unique index on LOGIN ?
      session.rollback();
      AuthorDto persistedAuthor = mapper.selectByLogin(authorDto.getLogin());
      if (persistedAuthor != null) {
        authorDto.setId(persistedAuthor.getId());
        authorDto.setPersonId(persistedAuthor.getPersonId());
      } else {
        throw e;
      }

    } finally {
      MyBatis.closeQuietly(session);
    }
  }

}
