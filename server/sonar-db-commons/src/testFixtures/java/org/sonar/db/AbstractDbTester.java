/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.sql.SQLException;

/**
 * Base class for database test fixtures that provides both SQL utilities (from parent)
 * and MyBatis session management.
 *
 * <p>For database testing WITHOUT MyBatis, extend {@link AbstractSqlDbTester} instead.
 *
 * @param <T> concrete subtype of TestDb
 */
public abstract class AbstractDbTester<T extends TestDb> extends AbstractSqlDbTester<T> {

  private ThreadLocal<DbSessionContext> session = new ThreadLocal<>();

  protected AbstractDbTester(T db) {
    super(db);
  }

  /**
   * Opens a MyBatis session. Subclasses must implement this to provide
   * the actual MyBatis session creation logic.
   *
   * @param batched whether to open a batch session
   * @return a new DbSession
   */
  protected abstract DbSession openSession(boolean batched);

  /**
   * Returns the same session every time it is called, so the result should NOT be closed by the caller.
   */
  public DbSession getSession() {
    return getSession(false);
  }

  /**
   * Returns the same session every time it is called, so the result should NOT be closed by the caller.
   *
   * @param batched whether to use batch mode
   */
  public DbSession getSession(boolean batched) {
    if (session.get() == null) {
      session.set(new DbSessionContext(openSession(batched), batched));
    }
    return session.get().dbSession;
  }

  /**
   * Closes and clears the cached session. Should be called in after() lifecycle method.
   */
  protected void closeSession() {
    if (session.get() != null) {
      session.get().dbSession().rollback();
      session.get().dbSession().close();
      session.remove();
    }
  }

  /**
   * JUnit 4 lifecycle hook - closes session then stops database.
   */
  @Override
  protected void after() {
    closeSession();
    super.after();
  }

  /**
   * Commits the current session if it's not in batch mode.
   */
  public void commit() {
    if (session.get() != null && !session.get().isBatched()) {
      getSession().commit();
    }
  }

  /**
   * Forces a commit on the current session even in batch mode.
   */
  public void forceCommit() {
    getSession().commit(true);
  }

  /**
   * Overload of countRowsOfTable that uses a DbSession's connection.
   */
  public int countRowsOfTable(DbSession dbSession, String tableName) {
    return countRowsOfTable(tableName, new ConnectionSupplier() {
      @Override
      public Connection get() throws SQLException {
        return dbSession.getConnection();
      }

      @Override
      public void close() {
        // we shouldn't be closing the session due to counting table rows
      }
    });
  }

  private record DbSessionContext(DbSession dbSession, boolean isBatched) {
  }
}
