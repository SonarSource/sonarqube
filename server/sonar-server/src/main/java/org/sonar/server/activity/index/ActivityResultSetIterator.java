/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.activity.index;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ResultSetIterator;
import org.sonar.server.es.EsUtils;
import org.sonar.server.util.DateCollector;

/**
 * Scrolls over table ACTIVITIES and reads documents to populate
 * the index "activities/activity"
 */
class ActivityResultSetIterator extends ResultSetIterator<UpdateRequest> {

  private static final String[] FIELDS = {
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

  private final DateCollector dates = new DateCollector();

  private ActivityResultSetIterator(PreparedStatement stmt) throws SQLException {
    super(stmt);
  }

  static ActivityResultSetIterator create(DbClient dbClient, DbSession session, long afterDate) {
    try {
      String sql = afterDate > 0L ? SQL_AFTER_DATE : SQL_ALL;
      PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(session, sql);
      if (afterDate > 0L) {
        stmt.setTimestamp(1, new Timestamp(afterDate));
      }
      return new ActivityResultSetIterator(stmt);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select activities", e);
    }
  }

  @Override
  protected UpdateRequest read(ResultSet rs) throws SQLException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    // all the fields must be present, even if value is null
    JsonWriter writer = JsonWriter.of(new OutputStreamWriter(bytes, StandardCharsets.UTF_8)).setSerializeNulls(true);
    writer.beginObject();
    String key = rs.getString(1);
    writer.prop(ActivityIndexDefinition.FIELD_KEY, key);
    writer.prop(ActivityIndexDefinition.FIELD_ACTION, rs.getString(2));
    writer.prop(ActivityIndexDefinition.FIELD_MESSAGE, rs.getString(3));
    writer.name(ActivityIndexDefinition.FIELD_DETAILS).valueObject(KeyValueFormat.parse(rs.getString(4)));
    writer.prop(ActivityIndexDefinition.FIELD_LOGIN, rs.getString(5));
    writer.prop(ActivityIndexDefinition.FIELD_TYPE, rs.getString(6));
    Date createdAt = rs.getTimestamp(7);
    writer.prop(ActivityIndexDefinition.FIELD_CREATED_AT, EsUtils.formatDateTime(createdAt));
    writer.endObject().close();
    byte[] jsonDoc = bytes.toByteArray();

    // it's more efficient to sort programmatically than in SQL on some databases (MySQL for instance)
    dates.add(createdAt);

    return new UpdateRequest(ActivityIndexDefinition.INDEX, ActivityIndexDefinition.TYPE, key).doc(jsonDoc).upsert(jsonDoc);
  }

  long getMaxRowDate() {
    return dates.getMax();
  }
}
