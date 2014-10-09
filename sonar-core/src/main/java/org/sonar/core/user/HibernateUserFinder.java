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

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.User;
import org.sonar.api.security.UserFinder;
import org.sonar.jpa.session.DatabaseSessionFactory;

/**
 * @since 2.10
 */
public class HibernateUserFinder implements UserFinder {

  private DatabaseSessionFactory sessionFactory;

  public HibernateUserFinder(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public User findById(int id) {
    DatabaseSession session = sessionFactory.getSession();
    return session.getSingleResult(User.class, "id", id);
  }

  @Override
  public User findByLogin(String login) {
    DatabaseSession session = sessionFactory.getSession();
    return session.getSingleResult(User.class, "login", login);
  }

}
