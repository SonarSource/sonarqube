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

import org.sonar.api.config.Settings;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.MassUpdater;
import org.sonar.server.db.migrations.SqlUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Used in the Active Record Migration 515
 * @since 4.3
 */
public class TechnicalDebtMeasuresMigration implements DatabaseMigration {

  private static final String SELECT_SQL = "SELECT pm.id, pm.value " +
    ", pm.variation_value_1 , pm.variation_value_2, pm.variation_value_3 " +
    ", pm.variation_value_4 , pm.variation_value_5 " +
    " FROM project_measures pm INNER JOIN metrics m on m.id=pm.metric_id " +
    " WHERE (m.name='sqale_index' or m.name='new_technical_debt' " +
    // SQALE measures
    " or m.name='sqale_effort_to_grade_a' or m.name='sqale_effort_to_grade_b' or m.name='sqale_effort_to_grade_c' or m.name='sqale_effort_to_grade_d' " +
    " or m.name='blocker_remediation_cost' or m.name='critical_remediation_cost' or m.name='major_remediation_cost' or m.name='minor_remediation_cost' " +
    " or m.name='info_remediation_cost' " +
    ")";

  private static final String UPDATE_SQL = "UPDATE project_measures SET value=?," +
    "variation_value_1=?,variation_value_2=?,variation_value_3=?,variation_value_4=?,variation_value_5=? WHERE id=?";

  private final WorkDurationConvertor workDurationConvertor;
  private final Database db;

  public TechnicalDebtMeasuresMigration(Database database, Settings settings) {
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
          row.id = SqlUtil.getLong(rs, 1);
          row.value = SqlUtil.getDouble(rs, 2);
          row.var1 = SqlUtil.getDouble(rs, 3);
          row.var2 = SqlUtil.getDouble(rs, 4);
          row.var3 = SqlUtil.getDouble(rs, 5);
          row.var4 = SqlUtil.getDouble(rs, 6);
          row.var5 = SqlUtil.getDouble(rs, 7);
          return row;
        }
      },
      new MassUpdater.InputConverter<Row>() {
        @Override
        public String updateSql() {
          return UPDATE_SQL;
        }

        @Override
        public void convert(Row row, PreparedStatement updateStatement) throws SQLException {
          setDouble(updateStatement, 1, row.value);
          setDouble(updateStatement, 2, row.var1);
          setDouble(updateStatement, 3, row.var2);
          setDouble(updateStatement, 4, row.var3);
          setDouble(updateStatement, 5, row.var4);
          setDouble(updateStatement, 6, row.var5);
          updateStatement.setLong(7, row.id);
        }
      }
    );
  }

  private void setDouble(PreparedStatement statement, int index, Double value) throws SQLException {
    if (value != null) {
      statement.setDouble(index, convertDebtForDays(value));
    } else {
      statement.setNull(index, Types.DOUBLE);
    }
  }

  private Long convertDebtForDays(Double data) {
    return workDurationConvertor.createFromDays(data);
  }

  private static class Row {
    private Long id;
    private Double value;
    private Double var1;
    private Double var2;
    private Double var3;
    private Double var4;
    private Double var5;
  }

}
