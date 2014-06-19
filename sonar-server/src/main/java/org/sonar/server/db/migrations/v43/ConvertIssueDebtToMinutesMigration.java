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

package org.sonar.server.db.migrations.v43;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.MassUpdater;
import org.sonar.server.db.migrations.SqlUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Used in the Active Record Migration 513
 * @since 4.3
 */
public class ConvertIssueDebtToMinutesMigration implements DatabaseMigration {

  private final WorkDurationConvertor workDurationConvertor;
  private final System2 system2;
  private final Database db;

  public ConvertIssueDebtToMinutesMigration(Database database, PropertiesDao propertiesDao) {
    this(database, propertiesDao, System2.INSTANCE);
  }

  @VisibleForTesting
  ConvertIssueDebtToMinutesMigration(Database database, PropertiesDao propertiesDao, System2 system2) {
    this.db = database;
    this.workDurationConvertor = new WorkDurationConvertor(propertiesDao);
    this.system2 = system2;
  }

  @Override
  public void execute() {
    new MassUpdater(db).execute(
      new MassUpdater.InputLoader<Row>() {
        @Override
        public String selectSql() {
          return "SELECT i.id, i.technical_debt FROM issues i";
        }

        @Override
        public Row load(ResultSet rs) throws SQLException {
          Long debt = SqlUtil.getLong(rs, 2);
          if (!rs.wasNull() && debt != null) {
            // See https://jira.codehaus.org/browse/SONAR-5394
            // The SQL request should not set the filter on technical_debt is not null. There's no index
            // on this column, so filtering is done programmatically.
            Row row = new Row();
            row.id = SqlUtil.getLong(rs, 1);
            row.debt = debt;
            return row;
          }
          return null;
        }
      },
      new MassUpdater.InputConverter<Row>() {
        @Override
        public String updateSql() {
          return "UPDATE issues SET technical_debt=?,updated_at=? WHERE id=?";
        }

        @Override
        public boolean convert(Row row, PreparedStatement updateStatement) throws SQLException {
          updateStatement.setLong(1, workDurationConvertor.createFromLong(row.debt));
          updateStatement.setTimestamp(2, new Timestamp(system2.now()));
          updateStatement.setLong(3, row.id);
          return true;
        }
      }
    );
  }

  private static class Row {
    private Long id;
    private Long debt;
  }

}
