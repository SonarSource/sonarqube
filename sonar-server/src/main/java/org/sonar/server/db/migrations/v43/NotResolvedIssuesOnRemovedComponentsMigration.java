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
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.db.migrations.MassUpdater;
import org.sonar.server.db.migrations.SqlUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Used in the Active Record Migration 525
 *
 * @since 4.3
 */
public class NotResolvedIssuesOnRemovedComponentsMigration implements DatabaseMigration {

  private final System2 system2;
  private final Database db;

  public NotResolvedIssuesOnRemovedComponentsMigration(Database database) {
    this(database, System2.INSTANCE);
  }

  @VisibleForTesting
  NotResolvedIssuesOnRemovedComponentsMigration(Database database, System2 system2) {
    this.db = database;
    this.system2 = system2;
  }

  @Override
  public void execute() {
    new MassUpdater(db).execute(
      new MassUpdater.InputLoader<Row>() {
        @Override
        public String selectSql() {
          return "SELECT i.id FROM issues i " +
            "INNER JOIN projects p on p.id=i.component_id " +
            "WHERE p.enabled=${_false} AND i.resolution IS NULL ";
        }

        @Override
        public Row load(ResultSet rs) throws SQLException {
          Row row = new Row();
          row.id = SqlUtil.getLong(rs, 1);
          return row;
        }
      },
      new MassUpdater.InputConverter<Row>() {
        @Override
        public String updateSql() {
          return "UPDATE issues SET status=?,resolution=?,updated_at=? WHERE id=?";
        }

        @Override
        public boolean convert(Row row, PreparedStatement updateStatement) throws SQLException {
          updateStatement.setString(1, Issue.STATUS_CLOSED);
          updateStatement.setString(2, Issue.RESOLUTION_REMOVED);
          updateStatement.setTimestamp(3, new Timestamp(system2.now()));
          updateStatement.setLong(4, row.id);
          return true;
        }
      }
    );
  }

  private static class Row {
    private Long id;
  }

}
