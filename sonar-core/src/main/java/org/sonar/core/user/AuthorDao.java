/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DaoUtils;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @since 3.0
 *
 * Be careful when updating this class because it's used by the Dev Cockpit plugin.
 */
@BatchSide
@ServerSide
public class AuthorDao implements DaoComponent {

  private final MyBatis mybatis;
  private final ResourceDao resourceDao;

  public AuthorDao(MyBatis mybatis, ResourceDao resourceDao) {
    this.mybatis = mybatis;
    this.resourceDao = resourceDao;
  }

  public AuthorDto selectByLogin(String login) {
    SqlSession session = mybatis.openSession(false);
    try {
      AuthorMapper mapper = session.getMapper(AuthorMapper.class);
      return mapper.selectByLogin(login);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public int countDeveloperLogins(long developerId) {
    SqlSession session = mybatis.openSession(false);
    try {
      AuthorMapper mapper = session.getMapper(AuthorMapper.class);
      return mapper.countDeveloperLogins(developerId);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insertAuthor(String login, long personId) {
    SqlSession session = mybatis.openSession(false);
    try {
      insertAuthor(login, personId, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insertAuthorAndDeveloper(String login, ResourceDto resourceDto) {
    SqlSession session = mybatis.openSession(false);
    try {
      // Hack in order to set the right module uuid path on DEVs
      if (Strings.isNullOrEmpty(resourceDto.getModuleUuidPath())) {
        resourceDto.setModuleUuidPath(ComponentDto.MODULE_UUID_PATH_SEP + resourceDto.getUuid() + ComponentDto.MODULE_UUID_PATH_SEP);
      }
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

  public List<String> selectScmAccountsByDeveloperUuids(Collection<String> developerUuid) {
    SqlSession session = mybatis.openSession(false);
    try {
      return selectScmAccountsByDeveloperUuids(session, developerUuid);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public List<String> selectScmAccountsByDeveloperUuids(final SqlSession session, Collection<String> developerUuids) {
    return DaoUtils.executeLargeInputs(developerUuids, new Function<List<String>, List<String>>() {
      @Override
      public List<String> apply(List<String> partition) {
        return session.getMapper(AuthorMapper.class).selectScmAccountsByDeveloperUuids(partition);
      }
    });
  }
}
