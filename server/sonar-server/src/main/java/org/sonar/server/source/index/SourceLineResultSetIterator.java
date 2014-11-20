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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

  private static final String SQL_AFTER_DATE = SQL_ALL + " where updated_at>=?";

  static SourceLineResultSetIterator create(DbClient dbClient, Connection connection, long afterDate) {
    try {
      String sql = afterDate > 0L ? SQL_AFTER_DATE : SQL_ALL;
      PreparedStatement stmt = dbClient.newScrollingSelectStatement(connection, sql);
      if (afterDate > 0L) {
        stmt.setTimestamp(1, new Timestamp(afterDate));
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
  protected Collection<SourceLineDoc> read(ResultSet rs) throws SQLException {
    String projectUuid = rs.getString(1);
    String fileUuid = rs.getString(2);
    Date updatedAt = SqlUtil.getDate(rs, 4);
    Reader dataStream = new InputStreamReader(new ByteArrayInputStream(rs.getBytes(5)));

    int line = 1;
    List<SourceLineDoc> lines = Lists.newArrayList();
    CSVParser csvParser = null;
    try {
      csvParser = new CSVParser(dataStream, CSVFormat.DEFAULT);

      for(CSVRecord csvRecord: csvParser) {
        SourceLineDoc doc = new SourceLineDoc(Maps.<String, Object>newHashMapWithExpectedSize(9));
  
        doc.setProjectUuid(projectUuid);
        doc.setFileUuid(fileUuid);
        doc.setLine(line);
        doc.setUpdateDate(updatedAt);
        doc.setScmRevision(csvRecord.get(0));
        doc.setScmAuthor(csvRecord.get(1));
        doc.setScmDate(DateUtils.parseDateTimeQuietly(csvRecord.get(2)));
        doc.setHighlighting(csvRecord.get(3));
        doc.setSource(csvRecord.get(4));

        lines.add(doc);

        line ++;
      }
    } catch(IOException ioError) {
      throw new IllegalStateException("Impossible to open stream for file_sources.data with file_uuid " + fileUuid);
    } catch(ArrayIndexOutOfBoundsException lineError) {
      throw new IllegalStateException(
        String.format("Impossible to parse source line data, stuck at line %d", line), lineError);
    } finally {
      IOUtils.closeQuietly(csvParser);
      IOUtils.closeQuietly(dataStream);
    }

    return lines;
  }
}
