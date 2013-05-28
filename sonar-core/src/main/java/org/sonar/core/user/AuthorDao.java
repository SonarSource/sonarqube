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
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;

import java.util.Date;

/**
 * @since 3.0
 */
public class AuthorDao implements BatchComponent, ServerComponent {

  private final MyBatis mybatis;
  private final ResourceDao resourceDao;

  public AuthorDao(MyBatis mybatis, ResourceDao resourceDao) {
    this.mybatis = mybatis;
    this.resourceDao = resourceDao;
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

  public void insertAuthor(String login, long personId) {
    SqlSession session = mybatis.openSession();
    try {
      insertAuthor(login, personId, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insertAuthorAndDeveloper(String login, ResourceDto resourceDto) {
    SqlSession session = mybatis.openSession();
    try {
      resourceDao.insertUsingExistingSession(resourceDto, session);
      insertAuthor(login, resourceDto.getId(), session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void insertAuthor(String login, long personId, SqlSession session) {
    AuthorMapper authorMapper = session.getMapper(AuthorMapper.class);
    Date now = new Date();
    AuthorDto authorDto = new AuthorDto()
      .setLogin(login)
      .setPersonId(personId)
      .setCreatedAt(now)
      .setUpdatedAt(now);

    authorMapper.insert(authorDto);
  }
}
