/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

public class DbSessionImpl implements DbSession {

  private SqlSession session;

  public DbSessionImpl(SqlSession session) {
    this.session = session;
  }

  @Override
  public void commit() {
    session.commit();
  }

  @Override
  public void commit(boolean force) {
    session.commit(force);
  }

  /**
   * We only care about the the commit section.
   * The rest is simply passed to its parent.
   */

  @Override
  public <T> Cursor<T> selectCursor(String statement) {
    return session.selectCursor(statement);
  }

  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter) {
    return session.selectCursor(statement, parameter);
  }

  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
    return session.selectCursor(statement, parameter, rowBounds);
  }

  @Override
  public <T> T selectOne(String statement) {
    return session.selectOne(statement);
  }

  @Override
  public <T> T selectOne(String statement, Object parameter) {
    return session.selectOne(statement, parameter);
  }

  @Override
  public <E> List<E> selectList(String statement) {
    return session.selectList(statement);
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter) {
    return session.selectList(statement, parameter);
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    return session.selectList(statement, parameter, rowBounds);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    return session.selectMap(statement, mapKey);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    return session.selectMap(statement, parameter, mapKey);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    return session.selectMap(statement, parameter, mapKey, rowBounds);
  }

  @Override
  public void select(String statement, Object parameter, ResultHandler handler) {
    session.select(statement, parameter, handler);
  }

  @Override
  public void select(String statement, ResultHandler handler) {
    session.select(statement, handler);
  }

  @Override
  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    session.select(statement, parameter, rowBounds, handler);
  }

  @Override
  public int insert(String statement) {
    return session.insert(statement);
  }

  @Override
  public int insert(String statement, Object parameter) {
    return session.insert(statement, parameter);
  }

  @Override
  public int update(String statement) {
    return session.update(statement);
  }

  @Override
  public int update(String statement, Object parameter) {
    return session.update(statement, parameter);
  }

  @Override
  public int delete(String statement) {
    return session.delete(statement);
  }

  @Override
  public int delete(String statement, Object parameter) {
    return session.delete(statement, parameter);
  }

  @Override
  public void rollback() {
    session.rollback();
  }

  @Override
  public void rollback(boolean force) {
    session.rollback(force);
  }

  @Override
  public List<BatchResult> flushStatements() {
    return session.flushStatements();
  }

  @Override
  public void close() {
    session.close();
  }

  @Override
  public void clearCache() {
    session.clearCache();
  }

  @Override
  public Configuration getConfiguration() {
    return session.getConfiguration();
  }

  @Override
  public <T> T getMapper(Class<T> type) {
    return session.getMapper(type);
  }

  @Override
  public Connection getConnection() {
    return session.getConnection();
  }

  @Override
  public SqlSession getSqlSession() {
    return session;
  }
}
