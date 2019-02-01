/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.source;

import com.google.common.base.Splitter;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.CheckForNull;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.ResultHandler;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

import static org.sonar.db.DatabaseUtils.toUniqueAndSortedPartitions;

public class FileSourceDao implements Dao {

  private static final Splitter END_OF_LINE_SPLITTER = Splitter.on('\n');

  @CheckForNull
  public FileSourceDto selectByFileUuid(DbSession session, String fileUuid) {
    return mapper(session).selectByFileUuid(fileUuid);
  }

  @CheckForNull
  public LineHashVersion selectLineHashesVersion(DbSession dbSession, String fileUuid) {
    Integer version = mapper(dbSession).selectLineHashesVersion(fileUuid);
    return version == null ? LineHashVersion.WITHOUT_SIGNIFICANT_CODE : LineHashVersion.valueOf(version);
  }

  @CheckForNull
  public List<String> selectLineHashes(DbSession dbSession, String fileUuid) {
    Connection connection = dbSession.getConnection();
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    try {
      pstmt = connection.prepareStatement("SELECT line_hashes FROM file_sources WHERE file_uuid=?");
      pstmt.setString(1, fileUuid);
      rs = pstmt.executeQuery();
      if (rs.next()) {
        String string = rs.getString(1);
        if (string == null) {
          return Collections.emptyList();
        }
        return END_OF_LINE_SPLITTER.splitToList(string);
      }
      return null;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to read FILE_SOURCES.LINE_HASHES of file " + fileUuid, e);
    } finally {
      DbUtils.closeQuietly(connection, pstmt, rs);
    }
  }

  /**
   * Scroll line hashes of all <strong>enabled</strong> components (should be files, but not enforced) with specified
   * uuids in no specific order with 'SOURCE' source and a non null path.
   */
  public void scrollLineHashes(DbSession dbSession, Collection<String> fileUUids, ResultHandler<LineHashesWithUuidDto> rowHandler) {
    for (List<String> partition : toUniqueAndSortedPartitions(fileUUids)) {
      mapper(dbSession).scrollLineHashes(partition, rowHandler);
    }
  }

  public void readLineHashesStream(DbSession dbSession, String fileUuid, Consumer<Reader> consumer) {
    Connection connection = dbSession.getConnection();
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    Reader reader = null;
    try {
      pstmt = connection.prepareStatement("SELECT line_hashes FROM file_sources WHERE file_uuid=?");
      pstmt.setString(1, fileUuid);
      rs = pstmt.executeQuery();
      if (rs.next()) {
        reader = rs.getCharacterStream(1);
        if (reader != null) {
          consumer.accept(reader);
        }
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to read FILE_SOURCES.LINE_HASHES of file " + fileUuid, e);
    } finally {
      IOUtils.closeQuietly(reader);
      DbUtils.closeQuietly(connection, pstmt, rs);
    }
  }

  public void insert(DbSession session, FileSourceDto dto) {
    mapper(session).insert(dto);
  }

  public void update(DbSession session, FileSourceDto dto) {
    mapper(session).update(dto);
  }

  private static FileSourceMapper mapper(DbSession session) {
    return session.getMapper(FileSourceMapper.class);
  }
}
