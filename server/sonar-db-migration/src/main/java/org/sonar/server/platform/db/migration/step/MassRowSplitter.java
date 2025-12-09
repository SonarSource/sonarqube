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
package org.sonar.server.platform.db.migration.step;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import org.sonar.core.util.ProgressLogger;
import org.sonar.db.Database;

import static com.google.common.base.Preconditions.checkState;

public class MassRowSplitter<T> {

  private final Database db;
  private final Connection readConnection;
  private final Connection writeConnection;
  private final AtomicLong counter = new AtomicLong(0L);
  private final ProgressLogger progress = ProgressLogger.create(getClass(), counter);
  private Select select;
  private UpsertImpl insert;
  private Function<Select.Row, Set<T>> rowSplitterFunction;
  public MassRowSplitter(Database db, Connection readConnection, Connection writeConnection) {
    this.db = db;
    this.readConnection = readConnection;
    this.writeConnection = writeConnection;
  }

  public Select select(String sql) throws SQLException {
    this.select = SelectImpl.create(db, readConnection, sql);
    return this.select;
  }

  public Upsert insert(String sql) throws SQLException {
    this.insert = UpsertImpl.create(writeConnection, sql);
    return this.insert;
  }

  public void splitRow(Function<Select.Row, Set<T>> rowSplitterFunction) {
    this.rowSplitterFunction = rowSplitterFunction;
  }

  public void execute(SqlStatementPreparer<T> sqlStatementPreparer) throws SQLException {
    checkState(select != null && insert != null, "SELECT or UPDATE request not defined");
    checkState(rowSplitterFunction != null, "rowSplitterFunction not defined");
    progress.start();
    try {
      select.scroll(row -> processSingleRow(sqlStatementPreparer, row, rowSplitterFunction));
      closeStatements();

      progress.log();
    } finally {
      progress.stop();
    }
  }

  private void processSingleRow(SqlStatementPreparer<T> sqlStatementPreparer, Select.Row row,
    Function<Select.Row, Set<T>> rowNormalizer) throws SQLException {

    Set<T> data = rowNormalizer.apply(row);
    for (T datum : data) {
      if (sqlStatementPreparer.handle(datum, insert)) {
        insert.addBatch();
      }
    }
    counter.getAndIncrement();
  }

  private void closeStatements() throws SQLException {
    if (insert.getBatchCount() > 0L) {
      insert.execute().commit();
    }
    insert.close();
    select.close();
  }

  @FunctionalInterface
  public interface SqlStatementPreparer<T> {
    /**
     * Convert some column values of a given row.
     *
     * @return true if the row must be updated, else false. If false, then the update parameter must not be touched.
     */
    boolean handle(T insertData, Upsert update) throws SQLException;
  }

}
