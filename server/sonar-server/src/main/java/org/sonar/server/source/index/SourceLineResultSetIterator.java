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
import org.sonar.api.utils.DateUtils;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.ResultSetIterator;
import org.sonar.server.db.migrations.SqlUtil;

import java.io.IOException;
import java.io.Reader;
import java.sql.*;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Scroll over table ISSUES and directly read the maps required to
 * post index requests
 */
class SourceLineResultSetIterator extends ResultSetIterator<Collection<SourceLineDoc>> {

  private static final String[] FIELDS = {
    // column 1
    "project_uuid",
    "file_uuid",
    "created_at",
    "updated_at",
    "data",
    "data_hash"
  };

  private static final String SQL_ALL = "select " + StringUtils.join(FIELDS, ",") + " from file_sources";

  private static final String SQL_AFTER_DATE = SQL_ALL + " where i.updated_at>=?";

  static SourceLineResultSetIterator create(DbClient dbClient, Connection connection, long afterDate) {
    try {
      String sql = afterDate > 0L ? SQL_AFTER_DATE : SQL_ALL;
      PreparedStatement stmt = dbClient.newScrollingSelectStatement(connection, sql);
      if (afterDate > 0L) {
        stmt.setTimestamp(0, new Timestamp(afterDate));
      }
      return new SourceLineResultSetIterator(stmt);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all issues", e);
    }
  }

  private SourceLineResultSetIterator(PreparedStatement stmt) throws SQLException {
    super(stmt);
  }

  @Override
  protected Collection<SourceLineDoc> read(ResultSet rs) throws SQLException {

    String projectUuid = rs.getString(1);
    String fileUuid = rs.getString(2);
    // createdAt = rs.getDate(3);
    Date updatedAt = SqlUtil.getDate(rs, 4);
    Reader dataStream = rs.getClob(5).getCharacterStream();
    // String dataHash = rs.getString(6);

    int line = 1;
    List<SourceLineDoc> lines = Lists.newArrayList();
    CSVParser csvParser = null;
    try {
      csvParser = new CSVParser(dataStream, CSVFormat.DEFAULT);

      for(CSVRecord record: csvParser) {
        SourceLineDoc doc = new SourceLineDoc(Maps.<String, Object>newHashMapWithExpectedSize(8));
  
        doc.setProjectUuid(projectUuid);
        doc.setFileUuid(fileUuid);
        doc.setLine(line ++);
        doc.setUpdateDate(updatedAt);
        doc.setScmRevision(record.get(0));
        doc.setScmAuthor(record.get(1));
        doc.setScmDate(DateUtils.parseDateTimeQuietly(record.get(2)));
        doc.setHighlighting(record.get(3));
        doc.setSource(record.get(4));
      }
    } catch(IOException ioError) {
      throw new IllegalStateException(
        String.format("Impossible to parse source line data, stuck at line %d", line), ioError);
    } finally {
      IOUtils.closeQuietly(csvParser);
      IOUtils.closeQuietly(dataStream);
    }

    return lines;
  }
}
