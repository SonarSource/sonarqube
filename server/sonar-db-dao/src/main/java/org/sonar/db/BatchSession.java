/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.List;
import java.util.Map;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

public class BatchSession extends DbSessionImpl {

  public static final int MAX_BATCH_SIZE = 250;

  private final int batchSize;
  private int count = 0;

  public BatchSession(SqlSession session) {
    this(session, MAX_BATCH_SIZE);
  }

  BatchSession(SqlSession session, int batchSize) {
    super(session);
    this.batchSize = batchSize;
  }

  @Override
  public void select(String statement, Object parameter, ResultHandler handler) {
    reset();
    super.select(statement, parameter, handler);
  }

  @Override
  public void select(String statement, ResultHandler handler) {
    reset();
    super.select(statement, handler);
  }

  @Override
  public <T> T selectOne(String statement) {
    reset();
    return super.selectOne(statement);
  }

  @Override
  public <T> T selectOne(String statement, Object parameter) {
    reset();
    return super.selectOne(statement, parameter);
  }

  @Override
  public <E> List<E> selectList(String statement) {
    reset();
    return super.selectList(statement);
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter) {
    reset();
    return super.selectList(statement, parameter);
  }

  @Override
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    reset();
    return super.selectList(statement, parameter, rowBounds);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    reset();
    return super.selectMap(statement, mapKey);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    reset();
    return super.selectMap(statement, parameter, mapKey);
  }

  @Override
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    reset();
    return super.selectMap(statement, parameter, mapKey, rowBounds);
  }

  @Override
  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    reset();
    super.select(statement, parameter, rowBounds, handler);
  }

  @Override
  public int insert(String statement) {
    makeSureGeneratedKeysAreNotUsedInBatchInserts(statement);
    increment();
    return super.insert(statement);
  }

  @Override
  public int insert(String statement, Object parameter) {
    makeSureGeneratedKeysAreNotUsedInBatchInserts(statement);
    increment();
    return super.insert(statement, parameter);
  }

  private void makeSureGeneratedKeysAreNotUsedInBatchInserts(String statement) {
    Configuration configuration = super.getConfiguration();
    if (null != configuration) {
      MappedStatement mappedStatement = configuration.getMappedStatement(statement);
      if (null != mappedStatement) {
        KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
        if (keyGenerator instanceof Jdbc3KeyGenerator) {
          throw new IllegalStateException("Batch inserts cannot use generated keys");
        }
      }
    }
  }

  @Override
  public int update(String statement) {
    increment();
    return super.update(statement);
  }

  @Override
  public int update(String statement, Object parameter) {
    increment();
    return super.update(statement, parameter);
  }

  @Override
  public int delete(String statement) {
    increment();
    return super.delete(statement);
  }

  @Override
  public int delete(String statement, Object parameter) {
    increment();
    return super.delete(statement, parameter);
  }

  @Override
  public void commit() {
    super.commit();
    reset();
  }

  @Override
  public void commit(boolean force) {
    super.commit(force);
    reset();
  }

  @Override
  public void rollback() {
    super.rollback();
    reset();
  }

  @Override
  public void rollback(boolean force) {
    super.rollback(force);
    reset();
  }

  @Override
  public List<BatchResult> flushStatements() {
    List<BatchResult> batchResults = super.flushStatements();
    reset();
    return batchResults;
  }

  @Override
  public <T> T getMapper(Class<T> type) {
    return getConfiguration().getMapper(type, this);
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
