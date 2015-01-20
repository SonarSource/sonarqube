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
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.ResultSetIterator;
import org.sonar.server.db.migrations.SqlUtil;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Scroll over table FILE_SOURCES and directly parse CSV field required to
 * populate the index sourcelines
 */
public class SourceLineResultSetIterator extends ResultSetIterator<SourceLineResultSetIterator.SourceFile> {

  public static class SourceFile {
    private final String fileUuid;
    private final long updatedAt;
    private final List<SourceLineDoc> lines = Lists.newArrayList();

    public SourceFile(String fileUuid, long updatedAt) {
      this.fileUuid = fileUuid;
      this.updatedAt = updatedAt;
    }

    public String getFileUuid() {
      return fileUuid;
    }

    public long getUpdatedAt() {
      return updatedAt;
    }

    public List<SourceLineDoc> getLines() {
      return lines;
    }

    public void addLine(SourceLineDoc line) {
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

  public static SourceLineResultSetIterator create(DbClient dbClient, Connection connection, long afterDate) {
    try {
      String sql = afterDate > 0L ? SQL_AFTER_DATE : SQL_ALL;
      // rows are big, so they are scrolled once at a time (one row in memory at a time)
      PreparedStatement stmt = dbClient.newScrollingSingleRowSelectStatement(connection, sql);
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

    Reader csv = rs.getCharacterStream(4);
    if (csv == null) {
      return result;
    }

    int line = 1;
    CSVParser csvParser = null;
    try {
      csvParser = new CSVParser(csv, CSVFormat.DEFAULT);

      for (CSVRecord csvRecord : csvParser) {
        SourceLineDoc doc = new SourceLineDoc(Maps.<String, Object>newHashMap());

        doc.setProjectUuid(projectUuid);
        doc.setFileUuid(fileUuid);
        doc.setLine(line);
        doc.setUpdateDate(updatedDate);
        doc.setScmRevision(csvRecord.get(FileSourceDto.CSV_INDEX_SCM_REVISION));
        doc.setScmAuthor(csvRecord.get(FileSourceDto.CSV_INDEX_SCM_AUTHOR));
        doc.setScmDate(DateUtils.parseDateTimeQuietly(csvRecord.get(FileSourceDto.CSV_INDEX_SCM_DATE)));
        // UT
        doc.setUtLineHits(parseIntegerFromRecord(csvRecord.get(FileSourceDto.CSV_INDEX_UT_LINE_HITS)));
        doc.setUtConditions(parseIntegerFromRecord(csvRecord.get(FileSourceDto.CSV_INDEX_UT_CONDITIONS)));
        doc.setUtCoveredConditions(parseIntegerFromRecord(csvRecord.get(FileSourceDto.CSV_INDEX_UT_COVERED_CONDITIONS)));
        // IT
        doc.setItLineHits(parseIntegerFromRecord(csvRecord.get(FileSourceDto.CSV_INDEX_IT_LINE_HITS)));
        doc.setItConditions(parseIntegerFromRecord(csvRecord.get(FileSourceDto.CSV_INDEX_IT_CONDITIONS)));
        doc.setItCoveredConditions(parseIntegerFromRecord(csvRecord.get(FileSourceDto.CSV_INDEX_IT_COVERED_CONDITIONS)));
        // OVERALL
        doc.setOverallLineHits(parseIntegerFromRecord(csvRecord.get(FileSourceDto.CSV_INDEX_OVERALL_LINE_HITS)));
        doc.setOverallConditions(parseIntegerFromRecord(csvRecord.get(FileSourceDto.CSV_INDEX_OVERALL_CONDITIONS)));
        doc.setOverallCoveredConditions(parseIntegerFromRecord(csvRecord.get(FileSourceDto.CSV_INDEX_OVERALL_COVERED_CONDITIONS)));

        doc.setHighlighting(csvRecord.get(FileSourceDto.CSV_INDEX_HIGHLIGHTING));
        doc.setSymbols(csvRecord.get(FileSourceDto.CSV_INDEX_SYMBOLS));

        doc.setDuplications(parseDuplications(csvRecord.get(FileSourceDto.CSV_INDEX_DUPLICATIONS)));
        doc.setSource(csvRecord.get(FileSourceDto.CSV_INDEX_SOURCE));

        result.addLine(doc);

        line++;
      }
    } catch (IOException ioError) {
      throw new IllegalStateException("Impossible to open stream for file_sources.data with file_uuid " + fileUuid, ioError);
    } catch (ArrayIndexOutOfBoundsException lineError) {
      throw new IllegalStateException(
        String.format("Impossible to parse source line data, stuck at line %d", line), lineError);
    } finally {
      IOUtils.closeQuietly(csv);
      IOUtils.closeQuietly(csvParser);
    }

    return result;
  }

  private List<Integer> parseDuplications(@Nullable String duplications) {
    List<Integer> dups = Lists.newArrayList();
    if (StringUtils.isNotEmpty(duplications)) {
      StringTokenizer tokenizer = new StringTokenizer(duplications, ",", false);
      while (tokenizer.hasMoreTokens()) {
        dups.add(NumberUtils.toInt(tokenizer.nextToken(), -1));
      }
    }
    return dups;
  }

  @CheckForNull
  private Integer parseIntegerFromRecord(@Nullable String cellValue) {
    if (cellValue == null || cellValue.isEmpty()) {
      return null;
    } else {
      return Integer.parseInt(cellValue);
    }
  }
}
