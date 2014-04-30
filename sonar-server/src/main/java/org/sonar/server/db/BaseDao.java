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
package org.sonar.server.db;

import org.apache.ibatis.session.SqlSession;
import org.sonar.core.db.Dao;
import org.sonar.core.db.Dto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.search.IndexAction;

import java.io.Serializable;

public abstract class BaseDao<E extends Dto<K>, K extends Serializable>
  implements Dao<E, K> {

  protected final MyBatis mybatis;

  protected BaseDao(MyBatis myBatis) {
    this.mybatis = myBatis;
  }

  protected abstract String getIndexName();

  protected abstract E doGetByKey(K key, SqlSession session);

  protected abstract E doInsert(E item, SqlSession session);

  protected abstract E doUpdate(E item, SqlSession session);

  protected abstract void doDelete(E item, SqlSession session);

  protected abstract void doDeleteByKey(K key, SqlSession session);

  protected MyBatis getMyBatis() {
    return this.mybatis;
  }

  @Override
  public E getByKey(K key) {
    DbSession session = getMyBatis().openSession(false);
    try {
      return this.doGetByKey(key, session);
    } finally {
      session.close();
    }
  }

  @Override
  public E update(E item, DbSession session) {
    session.enqueue(new IndexAction(this.getIndexName(),
      IndexAction.Method.UPDATE, item.getKey()));
    return this.doUpdate(item, session);
  }

  @Override
  public E update(E item) {
    DbSession session = getMyBatis().openSession(false);
    try {
      this.update(item, session);
      session.commit();
      return item;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public E insert(E item, DbSession session) {
    IndexAction action = new IndexAction(this.getIndexName(),
      IndexAction.Method.INSERT, item.getKey());
    session.enqueue(action);
    this.doInsert(item, session);
    return item;
  }

  @Override
  public E insert(E item) {
    DbSession session = getMyBatis().openSession(false);
    try {
      this.insert(item, session);
      session.commit();
      return item;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public void delete(E item, DbSession session) {
    session.enqueue(new IndexAction(this.getIndexName(),
      IndexAction.Method.DELETE, item.getKey()));
    this.doDelete(item, session);
  }

  @Override
  public void delete(E item) {
    DbSession session = getMyBatis().openSession(false);
    try {
      this.delete(item, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public void deleteByKey(K key, DbSession session) {
    session.enqueue(new IndexAction(this.getIndexName(),
      IndexAction.Method.DELETE, key));
    this.doDeleteByKey(key, session);
  }

  @Override
  public void deleteByKey(K key) {
    DbSession session = getMyBatis().openSession(false);
    try {
      this.doDeleteByKey(key, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
