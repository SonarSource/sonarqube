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
import com.google.common.base.Strings;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.util.SqlUtil;

import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used in the Active Record Migration 514
 */
public class IssueChangelogMigration implements DatabaseMigration {

  private static final String ID = "id";
  private static final String CHANGE_DATA = "changeData";

  private static final String SELECT_SQL = "SELECT ic.id AS " + ID + ", ic.change_data AS " + CHANGE_DATA +
    " FROM issue_changes ic " +
    " WHERE ic.change_type = 'diff' and ic.change_data LIKE '%technicalDebt%'";

  private static final String UPDATE_SQL = "UPDATE issue_changes SET change_data=?,updated_at=? WHERE id=?";

  private final WorkDurationConvertor workDurationConvertor;
  private final System2 system2;
  private final Database db;

  public IssueChangelogMigration(Database database, Settings settings) {
    this(database, settings, System2.INSTANCE);
  }

  @VisibleForTesting
  IssueChangelogMigration(Database database, Settings settings, System2 system2) {
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
          row.changeData = rs.getString(CHANGE_DATA);
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
          if (row.changeData != null) {
            statement.setString(1, convertChangelog(row.changeData));
          } else {
            statement.setNull(1, Types.VARCHAR);
          }
          statement.setDate(2, new Date(system2.now()));
          statement.setLong(3, row.id);
        }
      }
    );
  }

  @VisibleForTesting
  String convertChangelog(String data) {
    Pattern pattern = Pattern.compile("technicalDebt=(\\d*)\\|(\\d*)", Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(data);
    StringBuffer sb = new StringBuffer();
    if (matcher.find()) {
      String replacement = "technicalDebt=";
      String oldValue = matcher.group(1);
      if (!Strings.isNullOrEmpty(oldValue)) {
        long oldDebt = workDurationConvertor.createFromLong(Long.parseLong(oldValue));
        replacement += Long.toString(oldDebt);
      }
      replacement += "|";
      String newValue = matcher.group(2);
      if (!Strings.isNullOrEmpty(newValue)) {
        long newDebt = workDurationConvertor.createFromLong(Long.parseLong(newValue));
        replacement += Long.toString(newDebt);
      }
      matcher.appendReplacement(sb, replacement);
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  private static class Row {
    Long id;
    String changeData;
  }

}
