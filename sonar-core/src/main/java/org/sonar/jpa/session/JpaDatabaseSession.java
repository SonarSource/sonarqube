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
package org.sonar.jpa.session;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JpaDatabaseSession extends DatabaseSession {

  private final DatabaseConnector connector;
  private EntityManager entityManager = null;
  private int index = 0;
  private boolean inTransaction = false;

  public JpaDatabaseSession(DatabaseConnector connector) {
    this.connector = connector;
  }

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
      entityManager.clear();
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
      }
      inTransaction = false;
      index = 0;
    }
  }

  public void rollback() {
    if (entityManager != null && inTransaction) {
      entityManager.getTransaction().rollback();
      inTransaction = false;
      index = 0;
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
    entityManager.persist(model);
    if (flushIfNeeded && (++index % BATCH_SIZE == 0)) {
      flush();
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
      flush();
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

  public void flush() {
    entityManager.flush();
    entityManager.clear();
  }

  public Query createQuery(String hql) {
    startTransaction();
    return entityManager.createQuery(hql);
  }

  public <T> T getSingleResult(Query query, T defaultValue) {
    try {
      return (T) query.getSingleResult();
    } catch (NoResultException ex) {
      return defaultValue;
    }
  }

  public <T> T getEntity(Class<T> entityClass, Object id) {
    startTransaction();
    return getEntityManager().find(entityClass, id);
  }

  public <T> T getSingleResult(Class<T> entityClass, Object... criterias) {
    try {
      return getSingleResult(getQueryForCriterias(entityClass, true, criterias), (T) null);

    } catch (NonUniqueResultException ex) {
      LoggerFactory.getLogger(JpaDatabaseSession.class).warn("NonUniqueResultException on entity {} with criterias : {}",
          entityClass.getSimpleName(), StringUtils.join(criterias, ","));
      throw ex;
    }
  }

  public <T> List<T> getResults(Class<T> entityClass, Object... criterias) {
    return getQueryForCriterias(entityClass, true, criterias).getResultList();
  }

  public <T> List<T> getResults(Class<T> entityClass) {
    return getQueryForCriterias(entityClass, false, null).getResultList();
  }

  private Query getQueryForCriterias(Class<?> entityClass, boolean raiseError, Object... criterias) {
    if (criterias == null && raiseError) {
      throw new IllegalStateException("criterias parameter must be provided");
    }
    startTransaction();
    StringBuilder hql = new StringBuilder("SELECT o FROM ").append(entityClass.getSimpleName()).append(" o");
    if (criterias != null) {
      hql.append(" WHERE ");
      Map<String, Object> mappedCriterias = new HashMap<String, Object>();
      for (int i = 0; i < criterias.length; i += 2) {
        mappedCriterias.put((String) criterias[i], criterias[i + 1]);
      }
      buildCriteriasHQL(hql, mappedCriterias);
      Query query = getEntityManager().createQuery(hql.toString());

      for (Map.Entry<String, Object> entry : mappedCriterias.entrySet()) {
        query.setParameter(entry.getKey(), entry.getValue());
      }
      return query;
    }
    return getEntityManager().createQuery(hql.toString());
  }

  private void buildCriteriasHQL(StringBuilder hql, Map<String, Object> mappedCriterias) {
    for (Iterator<String> i = mappedCriterias.keySet().iterator(); i.hasNext();) {
      String criteria = i.next();
      hql.append("o.").append(criteria).append("=:").append(criteria);
      if (i.hasNext()) {
        hql.append(" AND ");
      }
    }
  }

}
