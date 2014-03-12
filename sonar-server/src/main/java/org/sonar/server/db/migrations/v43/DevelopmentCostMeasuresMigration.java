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

import org.sonar.api.config.Settings;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.MassUpdater;
import org.sonar.server.db.migrations.SqlUtil;

import javax.annotation.CheckForNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Used in the Active Record Migration 516
 * @since 4.3
 */
public class DevelopmentCostMeasuresMigration implements DatabaseMigration {

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
          return "SELECT pm.id, pm.value " +
            " FROM project_measures pm INNER JOIN metrics m on m.id=pm.metric_id " +
            " WHERE m.name='development_cost' AND pm.value IS NOT NULL";
        }

        @Override
        public Row load(ResultSet rs) throws SQLException {
          Row row = new Row();
          row.id = SqlUtil.getLong(rs, 1);
          row.value = SqlUtil.getDouble(rs, 2);
          return row;
        }
      },
      new MassUpdater.InputConverter<Row>() {
        @Override
        public String updateSql() {
          return "UPDATE project_measures SET value=NULL,text_value=? WHERE id=?";
        }

        @Override
        public boolean convert(Row row, PreparedStatement updateStatement) throws SQLException {
          updateStatement.setString(1, convertDebtForDays(row.value));
          updateStatement.setLong(2, row.id);
          return true;
        }
      }
    );
  }

  @CheckForNull
  private String convertDebtForDays(Double data) {
    return Long.toString(workDurationConvertor.createFromDays(data));
  }

  private static class Row {
    private Long id;
    private Double value;
  }

}
