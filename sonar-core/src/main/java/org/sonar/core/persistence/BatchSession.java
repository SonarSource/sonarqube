/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.core.persistence;

import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

public final class BatchSession implements SqlSession {

  public static final int MAX_BATCH_SIZE = 1000;

  private final SqlSession session;
  private final int batchSize;
  private int count = 0;

  BatchSession(SqlSession session) {
    this(session, MAX_BATCH_SIZE);
  }

  BatchSession(SqlSession session, int batchSize) {
    this.session = session;
    this.batchSize = batchSize;
  }

  public void select(String statement, Object parameter, ResultHandler handler) {
    reset();
    session.select(statement, parameter, handler);
  }

  public void select(String statement, ResultHandler handler) {
    reset();
    session.select(statement, handler);
  }

  public Object selectOne(String statement) {
    reset();
    return session.selectOne(statement);
  }

  public Object selectOne(String statement, Object parameter) {
    reset();
    return session.selectOne(statement, parameter);
  }

  public List selectList(String statement) {
    reset();
    return session.selectList(statement);
  }

  public List selectList(String statement, Object parameter) {
    reset();
    return session.selectList(statement, parameter);
  }

  public List selectList(String statement, Object parameter, RowBounds rowBounds) {
    reset();
    return session.selectList(statement, parameter, rowBounds);
  }

  public Map selectMap(String statement, String mapKey) {
    reset();
    return session.selectMap(statement, mapKey);
  }

  public Map selectMap(String statement, Object parameter, String mapKey) {
    reset();
    return session.selectMap(statement, parameter, mapKey);
  }

  public Map selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    reset();
    return session.selectMap(statement, parameter, mapKey, rowBounds);
  }

  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    reset();
    session.select(statement, parameter, rowBounds, handler);
  }

  public int insert(String statement) {
    increment();
    return session.insert(statement);
  }

  public int insert(String statement, Object parameter) {
    increment();
    return session.insert(statement, parameter);
  }

  public int update(String statement) {
    increment();
    return session.update(statement);
  }

  public int update(String statement, Object parameter) {
    increment();
    return session.update(statement, parameter);
  }

  public int delete(String statement) {
    increment();
    return session.delete(statement);
  }

  public int delete(String statement, Object parameter) {
    increment();
    return session.delete(statement, parameter);
  }

  public void commit() {
    session.commit();
    reset();
  }

  public void commit(boolean force) {
    session.commit(force);
    reset();
  }

  public void rollback() {
    session.rollback();
    reset();
  }

  public void rollback(boolean force) {
    session.rollback(force);
    reset();
  }

  public List<BatchResult> flushStatements() {
    List<BatchResult> batchResults = session.flushStatements();
    reset();
    return batchResults;
  }

  public void close() {
    session.close();
  }

  public void clearCache() {
    session.clearCache();
  }

  public Configuration getConfiguration() {
    return session.getConfiguration();
  }

  public <T> T getMapper(Class<T> type) {
    return getConfiguration().getMapper(type, this);
  }

  public Connection getConnection() {
    return session.getConnection();
  }

  private BatchSession increment() {
    count += 1;
    if (count >= batchSize) {
      commit();
    }
    return this;
  }

  private void reset() {
    count = 0;
  }
}
