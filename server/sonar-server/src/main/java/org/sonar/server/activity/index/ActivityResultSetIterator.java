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
package org.sonar.server.activity.index;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.ResultSetIterator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;

/**
 * Scrolls over table ACTIVITIES and reads documents to populate
 * the index "activities/activity"
 */
class ActivityResultSetIterator extends ResultSetIterator<ActivityDoc> {

  private static final String[] FIELDS = {
    // column 1
    "log_key",
    "log_action",
    "log_message",
    "data_field",
    "user_login",
    "log_type",
    "created_at"
  };

  private static final String SQL_ALL = "select " + StringUtils.join(FIELDS, ",") + " from activities ";

  private static final String SQL_AFTER_DATE = SQL_ALL + " where created_at>=?";

  private ActivityResultSetIterator(PreparedStatement stmt) throws SQLException {
    super(stmt);
  }

  static ActivityResultSetIterator create(DbClient dbClient, Connection connection, long afterDate) {
    try {
      String sql = afterDate > 0L ? SQL_AFTER_DATE : SQL_ALL;
      PreparedStatement stmt = dbClient.newScrollingSelectStatement(connection, sql);
      if (afterDate > 0L) {
        stmt.setTimestamp(1, new Timestamp(afterDate));
      }
      return new ActivityResultSetIterator(stmt);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select activities", e);
    }
  }

  @Override
  protected ActivityDoc read(ResultSet rs) throws SQLException {
    ActivityDoc doc = new ActivityDoc(new HashMap<String, Object>(10));

    // all the fields must be present, even if value is null
    doc.setKey(rs.getString(1));
    doc.setAction(rs.getString(2));
    doc.setMessage(rs.getString(3));
    doc.setDetails(KeyValueFormat.parse(rs.getString(4)));
    doc.setLogin(rs.getString(5));
    doc.setType(rs.getString(6));
    doc.setCreatedAt(rs.getTimestamp(7));
    return doc;
  }
}
