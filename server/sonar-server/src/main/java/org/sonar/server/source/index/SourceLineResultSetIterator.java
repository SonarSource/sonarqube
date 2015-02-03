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
import org.apache.commons.lang.StringUtils;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.ResultSetIterator;
import org.sonar.server.source.db.FileSourceDb;

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
    "binary_data"
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
    long updatedAt = rs.getLong(3);
    Date updatedDate = new Date(updatedAt);

    SourceFile result = new SourceFile(fileUuid, updatedAt);
    FileSourceDb.Data data = FileSourceDto.decodeData(rs.getBinaryStream(4));
    for (FileSourceDb.Line line : data.getLinesList()) {
      SourceLineDoc doc = new SourceLineDoc();
      doc.setProjectUuid(projectUuid);
      doc.setFileUuid(fileUuid);
      doc.setLine(line.getLine());
      doc.setUpdateDate(updatedDate);
      doc.setScmRevision(line.getScmRevision());
      doc.setScmAuthor(line.getScmAuthor());
      doc.setScmDate(line.hasScmDate() ? new Date(line.getScmDate()) : null);
      // UT
      doc.setUtLineHits(line.hasUtLineHits() ? line.getUtLineHits() : null);
      doc.setUtConditions(line.hasUtConditions() ? line.getUtConditions() : null);
      doc.setUtCoveredConditions(line.hasUtCoveredConditions() ? line.getUtCoveredConditions() : null);
      // IT
      doc.setItLineHits(line.hasItLineHits() ? line.getItLineHits() : null);
      doc.setItConditions(line.hasItConditions() ? line.getItConditions() : null);
      doc.setItCoveredConditions(line.hasItCoveredConditions() ? line.getItCoveredConditions() : null);
      // OVERALL
      doc.setOverallLineHits(line.hasOverallLineHits() ? line.getOverallLineHits() : null);
      doc.setOverallConditions(line.hasOverallConditions() ? line.getOverallConditions() : null);
      doc.setOverallCoveredConditions(line.hasOverallCoveredConditions() ? line.getOverallCoveredConditions() : null);

      doc.setHighlighting(line.hasHighlighting() ? line.getHighlighting() : null);
      doc.setSymbols(line.hasSymbols() ? line.getSymbols() : null);
      doc.setDuplications(line.getDuplicationsList());

      // source is always the latest field. All future fields will be added between duplications (14) and source.
      doc.setSource(line.hasSource() ? line.getSource() : null);

      result.addLine(doc);
    }
    return result;
  }
}
