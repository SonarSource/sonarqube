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

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;

public final class BatchSession {

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

  /**
   * This method must be called when executing SQL requests.
   */
  public BatchSession increment(int nbSqlRequests) {
    count += nbSqlRequests;
    if (count > batchSize) {
      commit();
    }
    return this;
  }

  public BatchSession commit() {
    session.commit();
    count = 0;
    return this;
  }

  public <T> T getMapper(Class<T> type) {
    return session.getMapper(type);
  }

  public SqlSession getSqlSession() {
    return session;
  }

  public void select(String statement, Object parameter, ResultHandler handler) {
    session.select(statement, parameter, handler);
  }

  public void select(String statement, ResultHandler handler) {
    session.select(statement, handler);
  }

}
