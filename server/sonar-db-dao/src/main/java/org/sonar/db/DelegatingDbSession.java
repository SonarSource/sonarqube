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

/**
 * A wrapper of a {@link DbSession} instance which does not call the wrapped {@link DbSession}'s
 * {@link DbSession#close() close} method but throws a {@link UnsupportedOperationException} instead.
 */
abstract class DelegatingDbSession implements DbSession {
  private final DbSession delegate;

  DelegatingDbSession(DbSession delegate) {
    this.delegate = delegate;
  }

  public DbSession getDelegate() {
    return delegate;
  }

  ///////////////////////
  // overridden with change of behavior
  ///////////////////////
  @Override
  public void close() {
    doClose();
  }

  protected abstract void doClose();

  ///////////////////////
  // overridden with NO change of behavior
  ///////////////////////

  @Override
  public <T> Cursor<T> selectCursor(String statement) {
    return delegate.selectCursor(statement);
  }

  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter) {
    return delegate.selectCursor(statement, parameter);
  }

  @Override
  public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
    return delegate.selectCursor(statement, parameter, rowBounds);
  }

  @Override
  public <T> T selectOne(String statement) {
    return delegate.selectOne(statement);
  }

  @Override
  public <T> T selectOne(String statement, Object parameter) {
    return delegate.selectOne(statement, parameter);
  }

  @Override
  public <E> List<E> selectList(String statement) {
    return delegate.selectList(statement);
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter) {
    return delegate.selectList(statement, parameter);
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    return delegate.selectList(statement, parameter, rowBounds);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    return delegate.selectMap(statement, mapKey);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    return delegate.selectMap(statement, parameter, mapKey);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    return delegate.selectMap(statement, parameter, mapKey, rowBounds);
  }

  @Override
  public void select(String statement, Object parameter, ResultHandler handler) {
    delegate.select(statement, parameter, handler);
  }

  @Override
  public void select(String statement, ResultHandler handler) {
    delegate.select(statement, handler);
  }

  @Override
  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    delegate.select(statement, parameter, rowBounds, handler);
  }

  @Override
  public int insert(String statement) {
    return delegate.insert(statement);
  }

  @Override
  public int insert(String statement, Object parameter) {
    return delegate.insert(statement, parameter);
  }

  @Override
  public int update(String statement) {
    return delegate.update(statement);
  }

  @Override
  public int update(String statement, Object parameter) {
    return delegate.update(statement, parameter);
  }

  @Override
  public int delete(String statement) {
    return delegate.delete(statement);
  }

  @Override
  public int delete(String statement, Object parameter) {
    return delegate.delete(statement, parameter);
  }

  @Override
  public void commit() {
    delegate.commit();
  }

  @Override
  public void commit(boolean force) {
    delegate.commit(force);
  }

  @Override
  public void rollback() {
    delegate.rollback();
  }

  @Override
  public void rollback(boolean force) {
    delegate.rollback(force);
  }

  @Override
  public List<BatchResult> flushStatements() {
    return delegate.flushStatements();
  }

  @Override
  public void clearCache() {
    delegate.clearCache();
  }

  @Override
  public Configuration getConfiguration() {
    return delegate.getConfiguration();
  }

  @Override
  public <T> T getMapper(Class<T> type) {
    return delegate.getMapper(type);
  }

  @Override
  public Connection getConnection() {
    return delegate.getConnection();
  }

  @Override
  public SqlSession getSqlSession() {
    return delegate.getSqlSession();
  }
}
