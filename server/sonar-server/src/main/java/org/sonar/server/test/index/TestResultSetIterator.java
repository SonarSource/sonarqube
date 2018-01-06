/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.test.index;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ResultSetIterator;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.es.EsUtils;
import org.sonar.server.source.index.FileSourcesUpdaterHelper.Row;

import static org.sonar.server.source.index.FileSourcesUpdaterHelper.preparedStatementToSelectFileSources;
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
import static org.sonar.server.test.index.TestIndexDefinition.FIELD_UPDATED_AT;
import static org.sonar.server.test.index.TestIndexDefinition.INDEX_TYPE_TEST;

/**
 * Scroll over table FILE_SOURCES of test type and directly parse data required to
 * populate the index tests/test
 */
public class TestResultSetIterator extends ResultSetIterator<Row> {

  private TestResultSetIterator(PreparedStatement stmt) throws SQLException {
    super(stmt);
  }

  public static TestResultSetIterator create(DbClient dbClient, DbSession session, @Nullable String projectUuid) {
    try {
      return new TestResultSetIterator(preparedStatementToSelectFileSources(dbClient, session, FileSourceDto.Type.TEST, projectUuid));
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all tests", e);
    }
  }

  @Override
  protected Row read(ResultSet rs) throws SQLException {
    String projectUuid = rs.getString(1);
    String fileUuid = rs.getString(2);
    Date updatedAt = new Date(rs.getLong(3));
    List<DbFileSources.Test> tests = parseData(fileUuid, rs.getBinaryStream(4));
    return toRow(projectUuid, fileUuid, updatedAt, tests);
  }

  private static List<DbFileSources.Test> parseData(String fileUuid, @Nullable InputStream dataInput) {
    List<DbFileSources.Test> tests = Collections.emptyList();
    if (dataInput != null) {
      try {
        tests = FileSourceDto.decodeTestData(dataInput);
      } catch (Exception e) {
        Loggers.get(TestResultSetIterator.class).warn(String.format("Invalid file_sources.binary_data on row with file_uuid='%s', test file will be ignored", fileUuid), e);
      }
    }
    return tests;
  }

  /**
   * Convert protobuf message to tests required for Elasticsearch indexing
   */
  public static Row toRow(String projectUuid, String fileUuid, Date updatedAt, List<DbFileSources.Test> tests) {
    Row result = new Row(projectUuid, fileUuid, updatedAt.getTime());
    for (DbFileSources.Test test : tests) {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();

      // all the fields must be present, even if value is null
      try (JsonWriter writer = JsonWriter.of(new OutputStreamWriter(bytes, StandardCharsets.UTF_8)).setSerializeNulls(true)) {
        writer.beginObject();
        writer.prop(FIELD_PROJECT_UUID, projectUuid);
        writer.prop(FIELD_FILE_UUID, fileUuid);
        writer.prop(FIELD_TEST_UUID, test.getUuid());
        writer.prop(FIELD_NAME, test.getName());
        writer.prop(FIELD_STATUS, test.hasStatus() ? test.getStatus().toString() : null);
        writer.prop(FIELD_DURATION_IN_MS, test.hasExecutionTimeMs() ? test.getExecutionTimeMs() : null);
        writer.prop(FIELD_MESSAGE, test.hasMsg() ? test.getMsg() : null);
        writer.prop(FIELD_STACKTRACE, test.hasStacktrace() ? test.getStacktrace() : null);
        writer.prop(FIELD_UPDATED_AT, EsUtils.formatDateTime(updatedAt));
        writer.name(FIELD_COVERED_FILES);
        writer.beginArray();
        for (DbFileSources.Test.CoveredFile coveredFile : test.getCoveredFileList()) {
          writer.beginObject();
          writer.prop(FIELD_COVERED_FILE_UUID, coveredFile.getFileUuid());
          writer.name(FIELD_COVERED_FILE_LINES).valueObject(coveredFile.getCoveredLineList());
          writer.endObject();
        }
        writer.endArray();
        writer.endObject();
      }
      // This is an optimization to reduce memory consumption and multiple conversions from Map to JSON.
      // UpdateRequest#doc() and #upsert() take the same parameter values, so:
      // - passing the same Map would execute two JSON serializations
      // - Map is a useless temporarily structure: read JDBC result set -> convert to map -> convert to JSON. Generating
      // directly JSON from result set is more efficient.
      byte[] jsonDoc = bytes.toByteArray();
      UpdateRequest updateRequest = new UpdateRequest(INDEX_TYPE_TEST.getIndex(), INDEX_TYPE_TEST.getType(), test.getUuid())
        .routing(projectUuid)
        .doc(jsonDoc, XContentType.JSON)
        .upsert(jsonDoc, XContentType.JSON);
      result.getUpdateRequests().add(updateRequest);
    }
    return result;
  }
}
