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

/**
 * A {@link DelegatingDbSession} subclass which tracks calls to insert/update/delete methods and commit/rollback
 */
class NonClosingDbSession extends DelegatingDbSession {
  private boolean dirty;

  NonClosingDbSession(DbSession delegate) {
    super(delegate);
  }

  @Override
  public void doClose() {
    // rollback when session is dirty so that no statement leaks from one use of the DbSession to another
    // super.close() would do such rollback before actually closing **if autocommit is true**
    // we are going to assume autocommit is true and keep this behavior
    if (dirty) {
      getDelegate().rollback();
    }
  }

  @Override
  public int insert(String statement) {
    dirty = true;
    return super.insert(statement);
  }

  @Override
  public int insert(String statement, Object parameter) {
    dirty = true;
    return super.insert(statement, parameter);
  }

  @Override
  public int update(String statement) {
    dirty = true;
    return super.update(statement);
  }

  @Override
  public int update(String statement, Object parameter) {
    dirty = true;
    return super.update(statement, parameter);
  }

  @Override
  public int delete(String statement) {
    dirty = true;
    return super.delete(statement);
  }

  @Override
  public int delete(String statement, Object parameter) {
    dirty = true;
    return super.delete(statement, parameter);
  }

  @Override
  public void commit() {
    super.commit();
    dirty = false;
  }

  @Override
  public void commit(boolean force) {
    super.commit(force);
    dirty = false;
  }

  @Override
  public void rollback() {
    super.rollback();
    dirty = false;
  }

  @Override
  public void rollback(boolean force) {
    super.rollback(force);
    dirty = false;
  }
}
