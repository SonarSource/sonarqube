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

package org.sonar.server.db.migrations.v44;

import org.apache.commons.lang.StringUtils;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.MassUpdater;
import org.sonar.server.db.migrations.SqlUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * SONAR-5249
 * Merge measure data table into project_measure
 *
 * Used in the Active Record Migration 530.
 * @since 4.4
 */
public class MeasureDataMigration implements DatabaseMigration {

  private final Database db;

  public MeasureDataMigration(Database database) {
    this.db = database;
  }

  @Override
  public void execute() {
    final List<Long> ids = new ArrayList<Long>();
    new MassUpdater(db, 50).execute(
      new MassUpdater.InputLoader<Row>() {
        @Override
        public String selectSql() {
          return "SELECT md.id, md.measure_id FROM measure_data md";
        }

        @Override
        public Row load(ResultSet rs) throws SQLException {
          Row row = new Row();
          row.id = SqlUtil.getLong(rs, 1);
          row.measure_id = SqlUtil.getLong(rs, 2);
          return row;
        }
      },
      new MassUpdater.InputConverter<Row>() {

        @Override
        public String updateSql() {
          return "UPDATE project_measures m SET m.measure_data = (SELECT md.data FROM measure_data md WHERE md.id = ?) WHERE m.id=?";
        }

        @Override
        public boolean convert(Row row, PreparedStatement updateStatement) throws SQLException {
          ids.add(row.id);
          updateStatement.setLong(1, row.id);
          updateStatement.setLong(2, row.measure_id);
          return true;
        }
      },
      new MassUpdater.PeriodicUpdater() {

        @Override
        public boolean update(Connection connection) throws SQLException {
          if (ids.size() > 0) {
            String deleteSql = new StringBuilder().append("DELETE FROM measure_data WHERE id IN (")
              .append(StringUtils.repeat("?", ",", ids.size())).append(")").toString();
            PreparedStatement s = connection.prepareStatement(deleteSql);
            int i = 1;
            for (Long id : ids) {
              s.setLong(i++, id);
            }
            s.executeUpdate();
            s.close();
            ids.clear();
            return true;
          }
          return false;
        }

      }
      );
  }

  private static class Row {
    private Long id;
    private Long measure_id;
  }

}
