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
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.cluster.WorkQueue;

import java.io.Serializable;

public abstract class BaseDao<E extends Dto<K>, K extends Serializable> implements Dao<E, K> {

  private MyBatis myBatis;
  private WorkQueue workQueue;

  protected BaseDao(WorkQueue workQueue, MyBatis myBatis) {
    this.myBatis = myBatis;
    this.workQueue = workQueue;
  }

  protected abstract String getIndexName();

  protected void enqueInsert(K key) {
    this.workQueue.enqueInsert(this.getIndexName(), key);
  }

  protected void enqueUpdate(K key) {
    this.workQueue.enqueUpdate(this.getIndexName(), key);
  }

  protected void enqueDelete(K key) {
    this.workQueue.enqueDelete(this.getIndexName(), key);
  }

  protected MyBatis getMyBatis(){
    return this.myBatis;
  }

  @Override
  @SuppressWarnings("unchecked")
  public E getByKey(K key) {
    E item = null;
    SqlSession session = getMyBatis().openSession();
    item = (E) session.getMapper(this.getClass()).getByKey(key);
    MyBatis.closeQuietly(session);
    return item;
  }

  @Override
  public E update(E item) {
    SqlSession session = getMyBatis().openSession();
    E result = null;
    try {
      result = (E) session.getMapper(this.getClass()).update(item);
      session.commit();
    } finally {
      this.enqueUpdate(item.getKey());
      MyBatis.closeQuietly(session);
      return result;
    }
  }

  @Override
  public E insert(E item) {
    SqlSession session = getMyBatis().openSession();
    E result = null;
    try {
      result = (E) session.getMapper(this.getClass()).insert(item);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
      this.enqueInsert(item.getKey());
      return result;
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
      session.getMapper(this.getClass()).deleteByKey(key);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
      this.enqueDelete(key);
    }
  }
}
