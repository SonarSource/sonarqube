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

import org.apache.commons.dbutils.DbUtils;

import javax.annotation.CheckForNull;

import java.io.Closeable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * {@link java.util.Iterator} applied to {@link java.sql.ResultSet}
 */
public abstract class ResultSetIterator<E> implements Iterator<E>, Closeable {

  private final ResultSet rs;
  private final PreparedStatement stmt;

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
    try {
      return !rs.isLast();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to call ResultSet#isLast()", e);
    }
  }

  @Override
  @CheckForNull
  public E next() {
    try {
      rs.next();
      return read(rs);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to read row of JDBC result set", e);
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    DbUtils.closeQuietly(rs);
    DbUtils.closeQuietly(stmt);
  }

  protected abstract E read(ResultSet rs) throws SQLException;
}
