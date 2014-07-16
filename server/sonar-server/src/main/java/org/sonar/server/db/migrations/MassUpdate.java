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
package org.sonar.server.db.migrations;

import org.sonar.core.persistence.Database;

import java.sql.Connection;
import java.sql.SQLException;

public class MassUpdate {

  public static interface Handler {
    /**
     * Convert some column values of a given row.
     * @return true if the row must be updated, else false. If false, then the update parameter must not be touched.
     */
    boolean handle(Select.Row row, SqlStatement update) throws SQLException;
  }

  private final Database db;
  private final Connection connection;

  private Select select;
  private Upsert update;

  MassUpdate(Database db, Connection connection) {
    this.db = db;
    this.connection = connection;
  }

  public SqlStatement select(String sql) throws SQLException {
    this.select = SelectImpl.create(db, connection, sql);
    return this.select;
  }

  public MassUpdate update(String sql) throws SQLException {
    this.update = UpsertImpl.create(connection, sql);
    return this;
  }

  public void execute(final Handler handler) throws SQLException {
    if (select == null || update==null) {
      throw new IllegalStateException("SELECT or UPDATE requests are not defined");
    }

    select.scroll(new Select.RowHandler() {
      @Override
      public void handle(Select.Row row) throws SQLException {
        if (handler.handle(row, update)) {
          update.addBatch();
        }
      }
    });
    if (((UpsertImpl)update).getBatchCount()>0L) {
      update.execute().commit();
    }
    update.close();
  }
}
