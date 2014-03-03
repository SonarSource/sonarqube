/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.db.migrations.debt;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.util.SqlUtil;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Used in the Active Record Migration 513
 */
public class IssueMigration implements DatabaseMigration {

  private static final String ID = "id";
  private static final String DEBT = "debt";

  private static final String SELECT_SQL = "SELECT i.id AS " + ID + ", i.technical_debt AS " + DEBT +
    " FROM issues i WHERE i.technical_debt IS NOT NULL";
  private static final String UPDATE_SQL = "UPDATE issues SET technical_debt=?,updated_at=? WHERE id=?";

  private final WorkDurationConvertor workDurationConvertor;
  private final System2 system2;
  private final Database db;

  public IssueMigration(Database database, Settings settings) {
    this(database, settings, System2.INSTANCE);
  }

  @VisibleForTesting
  IssueMigration(Database database, Settings settings, System2 system2) {
    this.db = database;
    this.workDurationConvertor = new WorkDurationConvertor(settings);
    this.system2 = system2;
  }

  @Override
  public void execute() {
    new MassUpdater(db).execute(
      new MassUpdater.InputLoader<Row>() {
        @Override
        public String selectSql() {
          return SELECT_SQL;
        }

        @Override
        public Row load(ResultSet rs) throws SQLException {
          Row row = new Row();
          row.id = SqlUtil.getLong(rs, ID);
          row.debt = SqlUtil.getLong(rs, DEBT);
          return row;
        }
      },
      new MassUpdater.InputConverter<Row>() {
        @Override
        public String updateSql() {
          return UPDATE_SQL;
        }

        @Override
        public void convert(Row row, PreparedStatement statement) throws SQLException {
          statement.setLong(1, workDurationConvertor.createFromLong(row.debt));
          statement.setDate(2, new Date(system2.now()));
          statement.setLong(3, row.id);
        }
      }
    );
  }

  private static class Row {
    private Long id;
    private Long debt;
  }

}
