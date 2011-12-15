/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.jpa.session;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.database.DatabaseSession;

import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.util.*;

public class JpaDatabaseSession extends DatabaseSession {

  private final DatabaseConnector connector;
  private EntityManager entityManager = null;
  private int index = 0;
  private boolean inTransaction = false;

  public JpaDatabaseSession(DatabaseConnector connector) {
    this.connector = connector;
  }

  /**
   * Note that usage of this method is discouraged, because it allows to construct and execute queries without additional exception handling,
   * which done in methods of this class.
   */
  public EntityManager getEntityManager() {
    return entityManager;
  }

  public void start() {
    entityManager = connector.createEntityManager();
    index = 0;
  }

  public void stop() {
    commit();
    if (entityManager != null && entityManager.isOpen()) {
      entityManager.close();
      entityManager = null;
    }
  }

  public void commit() {
    if (entityManager != null && inTransaction) {
      if (entityManager.isOpen()) {
        if (entityManager.getTransaction().getRollbackOnly()) {
          entityManager.getTransaction().rollback();
        } else {
          entityManager.getTransaction().commit();
        }
        entityManager.clear();
        index = 0;
      }
      inTransaction = false;
    }
  }

  public void rollback() {
    if (entityManager != null && inTransaction) {
      entityManager.getTransaction().rollback();
      inTransaction = false;
    }
  }

  public <T> T save(T model) {
    startTransaction();
    internalSave(model, true);
    return model;
  }

  public Object saveWithoutFlush(Object model) {
    startTransaction();
    internalSave(model, false);
    return model;
  }

  public boolean contains(Object model) {
    startTransaction();
    return entityManager.contains(model);
  }

  public void save(Object... models) {
    startTransaction();
    for (Object model : models) {
      save(model);
    }
  }

  private void internalSave(Object model, boolean flushIfNeeded) {
    try {
      entityManager.persist(model);
    } catch (PersistenceException e) {
      /*
       * See http://jira.codehaus.org/browse/SONAR-2234
       * In some cases Hibernate can throw exceptions without meaningful information about context, so we improve them here.
       */
      throw new PersistenceException("Unable to persist : " + model, e);
    }
    if (flushIfNeeded && (++index % BATCH_SIZE == 0)) {
      commit();
    }
  }

  public Object merge(Object model) {
    startTransaction();
    return entityManager.merge(model);
  }

  public void remove(Object model) {
    startTransaction();
    entityManager.remove(model);
    if (++index % BATCH_SIZE == 0) {
      commit();
    }
  }

  public void removeWithoutFlush(Object model) {
    startTransaction();
    entityManager.remove(model);
  }

  public <T> T reattach(Class<T> entityClass, Object primaryKey) {
    startTransaction();
    return entityManager.getReference(entityClass, primaryKey);
  }

  private void startTransaction() {
    if (!inTransaction) {
      entityManager.getTransaction().begin();
      inTransaction = true;
    }
  }

  /**
   * Note that not recommended to directly execute {@link Query#getSingleResult()}, because it will bypass exception handling,
   * which done in {@link #getSingleResult(Query, Object)}.
   */
  public Query createQuery(String hql) {
    startTransaction();
    return entityManager.createQuery(hql);
  }

  @Override
  public Query createNativeQuery(String sql) {
    startTransaction();
    return entityManager.createNativeQuery(sql);
  }

  /**
   * @return the result or <code>defaultValue</code>, if not found
   * @throws NonUniqueResultException if more than one result
   */
  public <T> T getSingleResult(Query query, T defaultValue) {
    /*
     * See http://jira.codehaus.org/browse/SONAR-2225
     * By default Hibernate throws NonUniqueResultException without meaningful information about context,
     * so we improve it here by adding all results in error message.
     * Note that in some rare situations we can receive too many results, which may lead to OOME,
     * but actually it will mean that database is corrupted as we don't expect more than one result
     * and in fact org.hibernate.ejb.QueryImpl#getSingleResult() anyway does loading of several results under the hood.
     */
    List<T> result = query.getResultList();

    if (result.size() == 1) {
      return result.get(0);

    } else if (result.isEmpty()) {
      return defaultValue;

    } else {
      Set<T> uniqueResult = new HashSet<T>(result);
      if (uniqueResult.size() > 1) {
        throw new NonUniqueResultException("Expected single result, but got : " + result.toString());
      } else {
        return uniqueResult.iterator().next();
      }
    }
  }

  public <T> T getEntity(Class<T> entityClass, Object id) {
    startTransaction();
    return getEntityManager().find(entityClass, id);
  }

  /**
   * @return the result or <code>null</code>, if not found
   * @throws NonUniqueResultException if more than one result
   */
  public <T> T getSingleResult(Class<T> entityClass, Object... criterias) {
    try {
      return getSingleResult(getQueryForCriterias(entityClass, true, criterias), (T) null);

    } catch (NonUniqueResultException ex) {
      NonUniqueResultException e = new NonUniqueResultException("Expected single result for entitiy " + entityClass.getSimpleName()
          + " with criterias : " + StringUtils.join(criterias, ","));
      throw (NonUniqueResultException) e.initCause(ex);
    }
  }

  public <T> List<T> getResults(Class<T> entityClass, Object... criterias) {
    return getQueryForCriterias(entityClass, true, criterias).getResultList();
  }

  public <T> List<T> getResults(Class<T> entityClass) {
    return getQueryForCriterias(entityClass, false, (Object[]) null).getResultList();
  }

  private Query getQueryForCriterias(Class<?> entityClass, boolean raiseError, Object... criterias) {
    if (criterias == null && raiseError) {
      throw new IllegalStateException("criterias parameter must be provided");
    }
    startTransaction();
    StringBuilder hql = new StringBuilder("SELECT o FROM ").append(entityClass.getSimpleName()).append(" o");
    if (criterias != null) {
      hql.append(" WHERE ");
      Map<String, Object> mappedCriterias = Maps.newHashMap();
      for (int i = 0; i < criterias.length; i += 2) {
        mappedCriterias.put((String) criterias[i], criterias[i + 1]);
      }
      buildCriteriasHQL(hql, mappedCriterias);
      Query query = getEntityManager().createQuery(hql.toString());

      for (Map.Entry<String, Object> entry : mappedCriterias.entrySet()) {
        if (entry.getValue() != null) {
          query.setParameter(entry.getKey(), entry.getValue());
        }
      }
      return query;
    }
    return getEntityManager().createQuery(hql.toString());
  }

  @VisibleForTesting
  void buildCriteriasHQL(StringBuilder hql, Map<String, Object> mappedCriterias) {
    for (Iterator<Map.Entry<String, Object>> i = mappedCriterias.entrySet().iterator(); i.hasNext();) {
      Map.Entry<String, Object> entry = i.next();
      hql.append("o.").append(entry.getKey());
      if (entry.getValue() == null) {
        hql.append(" IS NULL");
      } else {
        hql.append("=:").append(entry.getKey());
      }
      if (i.hasNext()) {
        hql.append(" AND ");
      }
    }
  }

}
