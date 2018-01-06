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

import java.io.Closeable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.commons.dbutils.DbUtils;

/**
 * Forward-only {@link java.util.Iterator} over a {@link java.sql.ResultSet}. Rows are
 * lazily loaded. The underlying ResultSet must be closed by calling the method
 * {@link #close()}
 * <p/>
 * As a safeguard, the ResultSet is automatically closed after the last element has
 * been retrieved via {@link #next()} or {@link #hasNext()} is called (which will return false).
 * This automagic behavior is not enough to remove explicit calls to {@link #close()}
 * from caller methods. Errors raised before end of traversal must still be handled.
 */
public abstract class ResultSetIterator<E> implements Iterator<E>, Closeable {

  private final ResultSet rs;
  private final PreparedStatement stmt;

  private volatile boolean didNext = false;
  private volatile boolean hasNext = false;
  private volatile boolean closed = false;

  public ResultSetIterator(PreparedStatement stmt) throws SQLException {
    this.stmt = stmt;
    this.rs = stmt.executeQuery();
  }

  protected ResultSetIterator(ResultSet rs) {
    this.stmt = null;
    this.rs = rs;
  }

  @Override
  public boolean hasNext() {
    if (closed) {
      return false;
    }
    if (!didNext) {
      hasNext = doNextQuietly();
      if (hasNext) {
        didNext = true;
      } else {
        close();
      }
    }
    return hasNext;
  }

  @Override
  public E next() {
    if (!hasNext()) {
      close();
      throw new NoSuchElementException();
    }
    try {
      return read(rs);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to read result set row", e);
    } finally {
      hasNext = doNextQuietly();
      if (!hasNext) {
        close();
      }
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    closed = true;
    DbUtils.closeQuietly(rs);
    DbUtils.closeQuietly(stmt);
  }

  protected abstract E read(ResultSet rs) throws SQLException;

  private boolean doNextQuietly() {
    try {
      return rs.next();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to read row of JDBC result set", e);
    }
  }
}
