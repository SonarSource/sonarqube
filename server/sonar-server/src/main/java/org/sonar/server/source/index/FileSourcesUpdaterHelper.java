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

package org.sonar.server.source.index;

import com.google.common.base.Joiner;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.server.db.DbClient;

import javax.annotation.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FileSourcesUpdaterHelper {

  private static final String SQL_ALL = "SELECT %s FROM file_sources WHERE data_type='%s' ";
  private static final String AFTER_DATE_FILTER = " AND updated_at>?";
  private static final String PROJECT_FILTER = " AND project_uuid=?";

  private static final String[] FIELDS = {
    "project_uuid",
    "file_uuid",
    "updated_at",
    "binary_data"
  };
  private static final String FIELDS_ONE_LINE = Joiner.on(",").join(FIELDS);

  private FileSourcesUpdaterHelper() {
    // only static stuff
  }

  public static PreparedStatement preparedStatementToSelectFileSources(DbClient dbClient, Connection connection, String dataType, long afterDate, @Nullable String projectUuid)
    throws SQLException {
    String sql = createSQL(dataType, afterDate, projectUuid);
    // rows are big, so they are scrolled once at a time (one row in memory at a time)
    PreparedStatement stmt = dbClient.newScrollingSingleRowSelectStatement(connection, sql);
    int index = 1;
    if (afterDate > 0L) {
      stmt.setLong(index, afterDate);
      index++;
    }
    if (projectUuid != null) {
      stmt.setString(index, projectUuid);
    }
    return stmt;
  }

  private static String createSQL(String dataType, long afterDate, @Nullable String projectUuid) {
    StringBuilder sql = new StringBuilder(String.format(SQL_ALL, FIELDS_ONE_LINE, dataType));
    if (afterDate > 0L || projectUuid != null) {
      if (afterDate > 0L) {
        sql.append(AFTER_DATE_FILTER);
      }
      if (projectUuid != null) {
        sql.append(PROJECT_FILTER);
      }
    }
    return sql.toString();
  }

  public static class Row {
    private final String fileUuid, projectUuid;
    private final long updatedAt;
    private final List<UpdateRequest> updateRequests = new ArrayList<>();

    public Row(String projectUuid, String fileUuid, long updatedAt) {
      this.projectUuid = projectUuid;
      this.fileUuid = fileUuid;
      this.updatedAt = updatedAt;
    }

    public String getProjectUuid() {
      return projectUuid;
    }

    public String getFileUuid() {
      return fileUuid;
    }

    public long getUpdatedAt() {
      return updatedAt;
    }

    public List<UpdateRequest> getUpdateRequests() {
      return updateRequests;
    }
  }
}
