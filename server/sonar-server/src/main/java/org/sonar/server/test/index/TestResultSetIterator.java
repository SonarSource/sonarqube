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

package org.sonar.server.test.index;

import org.apache.commons.io.Charsets;
import org.elasticsearch.action.update.UpdateRequest;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.ResultSetIterator;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.server.source.index.FileSourcesUpdaterHelper;
import org.sonar.server.source.index.FileSourcesUpdaterHelper.Row;

import javax.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.sonar.server.test.index.TestIndexDefinition.FIELD_COVERED_FILES;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_COVERED_FILE_LINES;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_COVERED_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_DURATION_IN_MS;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_FILE_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_MESSAGE;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_NAME;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_PROJECT_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_STACKTRACE;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_STATUS;
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_TEST_UUID;
import static org.sonar.server.test.index.TestIndexDefinition.INDEX;
import static org.sonar.server.test.index.TestIndexDefinition.TYPE;

/**
 * Scroll over table FILE_SOURCES of test type and directly parse data required to
 * populate the index sourcelines
 */
public class TestResultSetIterator extends ResultSetIterator<Row> {

  public static TestResultSetIterator create(DbClient dbClient, Connection connection, long afterDate, @Nullable String projectUuid) {
    try {
      return new TestResultSetIterator(FileSourcesUpdaterHelper.preparedStatementToSelectFileSources(dbClient, connection, FileSourceDto.Type.TEST, afterDate, projectUuid));
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all tests", e);
    }
  }

  private TestResultSetIterator(PreparedStatement stmt) throws SQLException {
    super(stmt);
  }

  @Override
  protected Row read(ResultSet rs) throws SQLException {
    String projectUuid = rs.getString(1);
    String fileUuid = rs.getString(2);
    Date updatedAt = new Date(rs.getLong(3));
    List<FileSourceDb.Test> data = FileSourceDto.decodeTestData(rs.getBinaryStream(4));
    return toRow(projectUuid, fileUuid, updatedAt, data);
  }

  /**
   * Convert protobuf message to tests required for Elasticsearch indexing
   */
  public static Row toRow(String projectUuid, String fileUuid, Date updatedAt, List<FileSourceDb.Test> tests) {
    Row result = new Row(projectUuid, fileUuid, updatedAt.getTime());
    for (FileSourceDb.Test test : tests) {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();

      // all the fields must be present, even if value is null
      JsonWriter writer = JsonWriter.of(new OutputStreamWriter(bytes, Charsets.UTF_8)).setSerializeNulls(true);
      writer.beginObject();
      writer.prop(FIELD_PROJECT_UUID, projectUuid);
      writer.prop(FIELD_FILE_UUID, fileUuid);
      writer.prop(FIELD_TEST_UUID, test.getUuid());
      writer.prop(FIELD_NAME, test.getName());
      writer.prop(FIELD_STATUS, test.hasStatus() ? test.getStatus().toString() : null);
      writer.prop(FIELD_DURATION_IN_MS, test.hasExecutionTimeMs() ? test.getExecutionTimeMs() : null);
      writer.prop(FIELD_MESSAGE, test.hasMsg() ? test.getMsg() : null);
      writer.prop(FIELD_STACKTRACE, test.hasStacktrace() ? test.getStacktrace() : null);
      writer.name(FIELD_COVERED_FILES);
      writer.beginArray();
      for (FileSourceDb.Test.CoveredFile coveredFile : test.getCoveredFileList()) {
        writer.beginObject();
        writer.prop(FIELD_COVERED_FILE_UUID, coveredFile.getFileUuid());
        writer.name(FIELD_COVERED_FILE_LINES).valueObject(coveredFile.getCoveredLineList());
        writer.endObject();
      }
      writer.endArray();
      writer.endObject().close();

      // This is an optimization to reduce memory consumption and multiple conversions from Map to JSON.
      // UpdateRequest#doc() and #upsert() take the same parameter values, so:
      // - passing the same Map would execute two JSON serializations
      // - Map is a useless temporarily structure: read JDBC result set -> convert to map -> convert to JSON. Generating
      // directly JSON from result set is more efficient.
      byte[] jsonDoc = bytes.toByteArray();
      UpdateRequest updateRequest = new UpdateRequest(INDEX, TYPE, test.getUuid())
        .routing(projectUuid)
        .doc(jsonDoc)
        .upsert(jsonDoc);
      result.getUpdateRequests().add(updateRequest);
    }
    return result;
  }
}
