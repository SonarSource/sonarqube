/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.database.configuration;

import org.apache.commons.configuration.BaseConfiguration;
import org.sonar.api.database.DatabaseSession;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.List;

/**
 * IMPORTANT : This class can't be moved to org.sonar.jpa.dao for backward-compatibility reasons.
 * This class is still used in some plugins.
 *
 * @since 1.10
 */
public class DatabaseConfiguration extends BaseConfiguration {
  private DatabaseSessionFactory sessionFactory;
  private DatabaseSession session;

  public DatabaseConfiguration(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
    load();
  }

  public DatabaseConfiguration(DatabaseSession session) {
    this.session = session;
    load();
  }

  public final void load() {
    clear();

    // Ugly workaround before the move to myBatis
    // Session is not up-to-date when Ruby on Rails inserts new rows in its own transaction. Seems like
    // Hibernate keeps a cache...
    getSession().commit();

    List<Property> properties = getSession()
        .createQuery("from " + Property.class.getSimpleName() + " p where p.resourceId is null and p.userId is null")
        .getResultList();

    if (properties != null) {
      for (Property property : properties) {
        setProperty(property.getKey(), property.getValue());
      }
    }
  }

  private DatabaseSession getSession() {
    if (session != null) {
      return session;
    }
    return sessionFactory.getSession();
  }
}