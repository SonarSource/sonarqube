/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.executeLargeInputs;

/**
 * Be careful when updating this class because it's used by the Dev Cockpit plugin.
 */
public class AuthorDao implements Dao {

  public AuthorDto selectByLogin(DbSession session, String login) {
    return getMapper(session).selectByLogin(login);
  }

  public int countDeveloperLogins(DbSession session, long developerId) {
    return getMapper(session).countDeveloperLogins(developerId);
  }

  public void insertAuthor(DbSession session, String login, long personId) {
    Date now = new Date();
    AuthorDto authorDto = new AuthorDto()
      .setLogin(login)
      .setPersonId(personId)
      .setCreatedAt(now)
      .setUpdatedAt(now);

    getMapper(session).insert(authorDto);
  }

  public List<String> selectScmAccountsByDeveloperUuids(final DbSession session, Collection<String> developerUuids) {
    return executeLargeInputs(developerUuids, getMapper(session)::selectScmAccountsByDeveloperUuids);
  }

  private static AuthorMapper getMapper(DbSession session) {
    return session.getMapper(AuthorMapper.class);
  }
}
