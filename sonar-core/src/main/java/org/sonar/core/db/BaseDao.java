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
import org.sonar.core.persistence.MyBatis;

import java.io.Serializable;

public abstract class BaseDao<E extends Dto<K>, K extends Serializable>
  implements Dao<E, K> {

  protected MyBatis mybatis;

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
    SqlSession session = getMyBatis().openSession();
    E item = this.doGetByKey(key, session);
    MyBatis.closeQuietly(session);
    return item;
  }

  @Override
  public E update(E item, SqlSession session) {
    //TODO add update record to session */
    return this.doUpdate(item, session);
  }

  @Override
  public E update(E item) {
    SqlSession session = getMyBatis().openSession();
    try {
      this.update(item, session);
      session.commit();
      return item;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public E insert(E item, SqlSession session) {
    //TODO add insert record to session */
    return this.doInsert(item, session);
  }

  @Override
  public E insert(E item) {
    SqlSession session = getMyBatis().openSession();
    try {
      this.insert(item, session);
      session.commit();
      return item;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public void delete(E item, SqlSession session) {
    //TODO add delete record to session */
    this.doDelete(item, session);
  }

  @Override
  public void delete(E item) {
    SqlSession session = getMyBatis().openSession();
    try {
      this.delete(item, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @Override
  public void deleteByKey(K key, SqlSession session) {
    //TODO add delete record to session */
    this.doDeleteByKey(key, session);
  }

  @Override
  public void deleteByKey(K key) {
    SqlSession session = getMyBatis().openSession();
    try {
      this.doDeleteByKey(key, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
