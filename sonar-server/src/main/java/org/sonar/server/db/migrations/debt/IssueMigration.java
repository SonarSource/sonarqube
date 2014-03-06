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
import org.sonar.server.db.migrations.MassUpdater;
import org.sonar.server.db.migrations.SqlUtil;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Used in the Active Record Migration 513
 * @since 4.3
 */
public class IssueMigration implements DatabaseMigration {

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
          return "SELECT i.id, i.technical_debt FROM issues i WHERE i.technical_debt IS NOT NULL";
        }

        @Override
        public Row load(ResultSet rs) throws SQLException {
          Row row = new Row();
          row.id = SqlUtil.getLong(rs, 1);
          row.debt = SqlUtil.getLong(rs, 2);
          return row;
        }
      },
      new MassUpdater.InputConverter<Row>() {
        @Override
        public String updateSql() {
          return "UPDATE issues SET technical_debt=?,updated_at=? WHERE id=?";
        }

        @Override
        public void convert(Row row, PreparedStatement updateStatement) throws SQLException {
          updateStatement.setLong(1, workDurationConvertor.createFromLong(row.debt));
          updateStatement.setDate(2, new Date(system2.now()));
          updateStatement.setLong(3, row.id);
        }
      }
    );
  }

  private static class Row {
    private Long id;
    private Long debt;
  }

}
