/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.core.plugin;

import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.database.DatabaseSession;
import org.sonar.jpa.session.DatabaseSessionFactory;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 2.2
 */
public class JpaPluginDao implements BatchComponent, ServerComponent {

  private DatabaseSessionFactory sessionFactory;

  public JpaPluginDao(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public List<JpaPlugin> getPlugins() {
    DatabaseSession session = sessionFactory.getSession();
    Query query = session.createQuery("FROM " + JpaPlugin.class.getSimpleName());
    return (List<JpaPlugin>) query.getResultList();
  }

  public List<JpaPluginFile> getPluginFiles() {
    DatabaseSession session = sessionFactory.getSession();
    Query query = session.createQuery("FROM " + JpaPluginFile.class.getSimpleName());
    return (List<JpaPluginFile>) query.getResultList();
  }

  public void register(List<JpaPlugin> plugins) {
    DatabaseSession session = sessionFactory.getSession();
    List<Integer> ids = new ArrayList<Integer>();
    for (JpaPlugin plugin : plugins) {
      session.saveWithoutFlush(plugin);
      ids.add(plugin.getId());
    }
    session.commit();

    if (ids.isEmpty()) {
      session.createQuery("DELETE " + JpaPluginFile.class.getSimpleName()).executeUpdate();
      session.createQuery("DELETE " + JpaPlugin.class.getSimpleName()).executeUpdate();

    } else {
      Query query = session.createQuery("DELETE " + JpaPluginFile.class.getSimpleName() + " WHERE plugin.id NOT IN (:ids)");
      query.setParameter("ids", ids);
      query.executeUpdate();

      query = session.createQuery("DELETE " + JpaPlugin.class.getSimpleName() + " WHERE id NOT IN (:ids)");
      query.setParameter("ids", ids);
      query.executeUpdate();

    }
    session.commit();
  }
}
