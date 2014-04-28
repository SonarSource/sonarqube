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
package org.sonar.core.db;

import org.apache.ibatis.session.SqlSession;
import org.sonar.core.cluster.WorkQueue;
import org.sonar.core.persistence.MyBatis;

import java.io.Serializable;

public abstract class BaseDao<E extends Dto<K>, K extends Serializable, M extends Dao<E,K>>
  implements Dao<E, K> {

  protected MyBatis mybatis;
  private WorkQueue queue;

  protected BaseDao(WorkQueue workQueue, MyBatis myBatis) {
    this.mybatis = myBatis;
    this.queue = workQueue;
  }

  protected abstract String getIndexName();

  protected abstract Class<M> getMapperClass();

  private M getMapper(SqlSession session) {
    return session.getMapper(getMapperClass());
  }

  protected void enqueInsert(K key) {
    this.queue.enqueInsert(this.getIndexName(), key);
  }

  protected void enqueUpdate(K key) {
    this.queue.enqueUpdate(this.getIndexName(), key);
  }

  protected void enqueDelete(K key) {
    this.queue.enqueDelete(this.getIndexName(), key);
  }

  protected MyBatis getMyBatis(){
    return this.mybatis;
  }

  @Override
  @SuppressWarnings("unchecked")
  public E getByKey(K key) {
    E item = null;
    SqlSession session = getMyBatis().openSession();
    item = getMapper(session).getByKey(key);
    MyBatis.closeQuietly(session);
    return item;
  }

  @Override
  public void update(E item) {
    SqlSession session = getMyBatis().openSession();
    try {
      getMapper(session).update(item);
      session.commit();
      this.enqueUpdate(item.getKey());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public void insert(E item) {
    SqlSession session = getMyBatis().openSession();
    try {
      getMapper(session).insert(item);
      session.commit();
      this.enqueInsert(item.getKey());
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public void delete(E item) {
    this.deleteByKey(item.getKey());
  }

  @Override
  public void deleteByKey(K key) {
    SqlSession session = getMyBatis().openSession();
    try {
      getMapper(session).deleteByKey(key);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
      this.enqueDelete(key);
    }
  }
}
