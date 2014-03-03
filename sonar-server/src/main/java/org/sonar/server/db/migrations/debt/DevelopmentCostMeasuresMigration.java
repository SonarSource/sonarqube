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

import org.picocontainer.annotations.Nullable;
import org.sonar.api.config.Settings;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.util.SqlUtil;

import javax.annotation.CheckForNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Used in the Active Record Migration 516
 */
public class DevelopmentCostMeasuresMigration implements DatabaseMigration {

  private static final String ID = "id";
  private static final String VALUE = "value";

  private static final String SELECT_SQL = "SELECT pm.id AS " + ID + ", pm.value AS " + VALUE +
    " FROM project_measures pm INNER JOIN metrics m on m.id=pm.metric_id " +
    " WHERE (m.name='development_cost')";

  private static final String UPDATE_SQL = "UPDATE project_measures SET value=NULL,text_value=? WHERE id=?";

  private final WorkDurationConvertor workDurationConvertor;
  private final Database db;

  public DevelopmentCostMeasuresMigration(Database database, Settings settings) {
    this.db = database;
    this.workDurationConvertor = new WorkDurationConvertor(settings);
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
          row.value = SqlUtil.getDouble(rs, VALUE);
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
          if (row.value != null) {
            statement.setString(1, convertDebtForDays(row.value));
          } else {
            statement.setNull(1, Types.VARCHAR);
          }
          statement.setLong(2, row.id);
        }
      }
    );
  }

  @CheckForNull
  private String convertDebtForDays(@Nullable Double data) {
    if (data == null) {
      return null;
    }
    return Long.toString(workDurationConvertor.createFromDays(data));
  }

  private static class Row {
    Long id;
    Double value;
  }

}
