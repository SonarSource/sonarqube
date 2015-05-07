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
package org.sonar.api.database;

import org.sonar.api.BatchSide;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.List;

/**
 * This component should not be accessed by plugins. Database is not an API.
 *
 * @since 1.10
 */
@BatchSide
public abstract class DatabaseSession {

  // IMPORTANT : this value must be the same than the property
  // hibernate.jdbc.batch_size from /META-INF/persistence.xml (module sonar-database)
  public static final int BATCH_SIZE = 30;

  public abstract EntityManager getEntityManager();

  public abstract void start();

  public abstract void stop();

  public abstract void commit();

  /**
   * This method should be called before a long period were database will not be accessed
   * in order to close database connection and avoid timeout. Next use of the
   * database will automatically open a new connection.
   */
  public abstract void commitAndClose();

  public abstract void rollback();

  public abstract <T> T save(T entity);

  public abstract Object saveWithoutFlush(Object entity);

  public abstract boolean contains(Object entity);

  public abstract void save(Object... entities);

  public abstract Object merge(Object entity);

  public abstract void remove(Object entity);

  public abstract void removeWithoutFlush(Object entity);

  public abstract <T> T reattach(Class<T> entityClass, Object primaryKey);

  public abstract Query createQuery(String hql);

  public abstract Query createNativeQuery(String sql);

  public abstract <T> T getSingleResult(Query query, T defaultValue);

  public abstract <T> T getEntity(Class<T> entityClass, Object id);

  public abstract <T> T getSingleResult(Class<T> entityClass, Object... criterias);

  public abstract <T> List<T> getResults(Class<T> entityClass, Object... criterias);

  public abstract <T> List<T> getResults(Class<T> entityClass);
}
