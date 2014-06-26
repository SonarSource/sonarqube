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

import org.apache.commons.lang.RandomStringUtils;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.MassUpdater;
import org.sonar.server.db.migrations.SqlUtil;
import org.sonar.server.util.Slug;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Feed the new columns RULES_PROFILES.KEE and PARENT_KEE.
 * 
 * @since 4.4
 */
public class FeedQProfileKeysMigration implements DatabaseMigration {

  private final Database db;

  public FeedQProfileKeysMigration(Database database) {
    this.db = database;
  }

  @Override
  public void execute() {
    updateKeys();
    updateParentKeys();
  }

  private void updateKeys() {
    new MassUpdater(db, 100).execute(
      new MassUpdater.InputLoader<Row>() {
        @Override
        public String selectSql() {
          return "SELECT id,language,name FROM rules_profiles";
        }

        @Override
        public Row load(ResultSet rs) throws SQLException {
          Row row = new Row();
          row.id = SqlUtil.getLong(rs, 1);
          row.lang = rs.getString(2);
          row.name = rs.getString(3);
          return row;
        }
      },
      new MassUpdater.InputConverter<Row>() {

        @Override
        public String updateSql() {
          return "UPDATE rules_profiles SET kee=? WHERE id=?";
        }

        @Override
        public boolean convert(Row row, PreparedStatement updateStatement) throws SQLException {
          updateStatement.setString(1, Slug.slugify(String.format("%s %s %s", row.lang, row.name, RandomStringUtils.randomNumeric(5))));
          updateStatement.setLong(2, row.id);
          return true;
        }
      }
      );
  }

  private void updateParentKeys() {
    new MassUpdater(db, 100).execute(
      new MassUpdater.InputLoader<Row>() {
        @Override
        public String selectSql() {
          return "SELECT child.id,parent.kee FROM rules_profiles child, rules_profiles parent WHERE child.parent_name=parent.name " +
            "and child.language=parent.language AND child.parent_name IS NOT NULL";
        }

        @Override
        public Row load(ResultSet rs) throws SQLException {
          Row row = new Row();
          row.id = SqlUtil.getLong(rs, 1);
          row.parentKey = rs.getString(2);
          return row;
        }
      },
      new MassUpdater.InputConverter<Row>() {

        @Override
        public String updateSql() {
          return "UPDATE rules_profiles SET parent_kee=? WHERE id=?";
        }

        @Override
        public boolean convert(Row row, PreparedStatement updateStatement) throws SQLException {
          updateStatement.setString(1, row.parentKey);
          updateStatement.setLong(2, row.id);
          return true;
        }
      }
      );
  }

  private static class Row {
    private Long id;
    private String lang, name, parentKey;
  }
}
