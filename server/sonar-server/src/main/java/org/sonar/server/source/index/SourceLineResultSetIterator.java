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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.ResultSetIterator;
import org.sonar.server.db.migrations.SqlUtil;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Scroll over table FILE_SOURCES and directly parse CSV field required to
 * populate the index sourcelines
 */
class SourceLineResultSetIterator extends ResultSetIterator<SourceLineResultSetIterator.SourceFile> {

  static class SourceFile {
    private final String fileUuid;
    private final long updatedAt;
    private final List<SourceLineDoc> lines = Lists.newArrayList();

    SourceFile(String fileUuid, long updatedAt) {
      this.fileUuid = fileUuid;
      this.updatedAt = updatedAt;
    }

    String getFileUuid() {
      return fileUuid;
    }

    long getUpdatedAt() {
      return updatedAt;
    }

    List<SourceLineDoc> getLines() {
      return lines;
    }

    void addLine(SourceLineDoc line) {
      this.lines.add(line);
    }
  }

  private static final String[] FIELDS = {
    // column 1
    "project_uuid",
    "file_uuid",
    "updated_at",
    "data"
  };

  private static final String SQL_ALL = "select " + StringUtils.join(FIELDS, ",") + " from file_sources";

  private static final String SQL_AFTER_DATE = SQL_ALL + " where updated_at>?";

  static SourceLineResultSetIterator create(DbClient dbClient, Connection connection, long afterDate) {
    try {
      String sql = afterDate > 0L ? SQL_AFTER_DATE : SQL_ALL;
      PreparedStatement stmt = dbClient.newScrollingSelectStatement(connection, sql);
      if (afterDate > 0L) {
        stmt.setLong(1, afterDate);
      }
      return new SourceLineResultSetIterator(stmt);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all file sources", e);
    }
  }

  private SourceLineResultSetIterator(PreparedStatement stmt) throws SQLException {
    super(stmt);
  }

  @Override
  protected SourceFile read(ResultSet rs) throws SQLException {
    String projectUuid = rs.getString(1);
    String fileUuid = rs.getString(2);
    Long updatedAt = SqlUtil.getLong(rs, 3);
    if (updatedAt == null) {
      updatedAt = System.currentTimeMillis();
    }
    Date updatedDate = new Date(updatedAt);
    SourceFile result = new SourceFile(fileUuid, updatedAt);

    String csv = rs.getString(4);

    if (StringUtils.isNotEmpty(csv)) {
      int line = 1;
      CSVParser csvParser = null;
      try {
        csvParser = new CSVParser(new StringReader(csv), CSVFormat.DEFAULT);

        for (CSVRecord csvRecord : csvParser) {
          SourceLineDoc doc = new SourceLineDoc(Maps.<String, Object>newHashMapWithExpectedSize(9));

          doc.setProjectUuid(projectUuid);
          doc.setFileUuid(fileUuid);
          doc.setLine(line);
          doc.setUpdateDate(updatedDate);
          doc.setScmRevision(csvRecord.get(0));
          doc.setScmAuthor(csvRecord.get(1));
          doc.setScmDate(DateUtils.parseDateTimeQuietly(csvRecord.get(2)));
          // UT
          doc.setUtLineHits(parseIntegerFromRecord(csvRecord, 3));
          doc.setUtConditions(parseIntegerFromRecord(csvRecord, 4));
          doc.setUtCoveredConditions(parseIntegerFromRecord(csvRecord, 5));
          // IT
          doc.setItLineHits(parseIntegerFromRecord(csvRecord, 6));
          doc.setItConditions(parseIntegerFromRecord(csvRecord, 7));
          doc.setItCoveredConditions(parseIntegerFromRecord(csvRecord, 8));
          // OVERALL
          doc.setOverallLineHits(parseIntegerFromRecord(csvRecord, 9));
          doc.setOverallConditions(parseIntegerFromRecord(csvRecord, 10));
          doc.setOverallCoveredConditions(parseIntegerFromRecord(csvRecord, 11));
          doc.setHighlighting(csvRecord.get(12));
          doc.setSource(csvRecord.get(csvRecord.size() - 1));

          result.addLine(doc);

          line++;
        }
      } catch (IOException ioError) {
        throw new IllegalStateException("Impossible to open stream for file_sources.data with file_uuid " + fileUuid, ioError);
      } catch (ArrayIndexOutOfBoundsException lineError) {
        throw new IllegalStateException(
          String.format("Impossible to parse source line data, stuck at line %d", line), lineError);
      } finally {
        IOUtils.closeQuietly(csvParser);
      }
    }

    return result;
  }

  private Integer parseIntegerFromRecord(CSVRecord record, int column) {
    String cellValue = record.get(column);
    if (cellValue == null || cellValue.isEmpty()) {
      return null;
    } else {
      return NumberUtils.createInteger(cellValue);
    }
  }
}
