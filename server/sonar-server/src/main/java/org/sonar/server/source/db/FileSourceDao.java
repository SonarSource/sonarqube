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

package org.sonar.server.source.db;

import com.google.common.base.Function;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.IOUtils;
import org.sonar.api.ServerSide;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.core.source.db.FileSourceDto.Type;
import org.sonar.core.source.db.FileSourceMapper;

import javax.annotation.CheckForNull;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@ServerSide
public class FileSourceDao implements DaoComponent {

  private final MyBatis mybatis;

  public FileSourceDao(MyBatis myBatis) {
    this.mybatis = myBatis;
  }

  @CheckForNull
  public FileSourceDto selectSource(String fileUuid) {
    DbSession session = mybatis.openSession(false);
    try {
      return mapper(session).select(fileUuid, Type.SOURCE);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @CheckForNull
  public FileSourceDto selectTest(String fileUuid) {
    DbSession session = mybatis.openSession(false);
    try {
      return mapper(session).select(fileUuid, Type.TEST);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public <T> void readDataStream(String fileUuid, Function<InputStream, T> function) {
    DbSession dbSession = mybatis.openSession(false);
    Connection connection = dbSession.getConnection();
    PreparedStatement pstmt = null;
    ResultSet rs = null;
    InputStream input = null;
    try {
      pstmt = connection.prepareStatement("SELECT binary_data FROM file_sources WHERE file_uuid=?");
      pstmt.setString(1, fileUuid);
      rs = pstmt.executeQuery();
      if (rs.next()) {
        input = rs.getBinaryStream(1);
        function.apply(input);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to read FILE_SOURCES.BINARY_DATA of file " + fileUuid, e);
    } finally {
      IOUtils.closeQuietly(input);
      DbUtils.closeQuietly(connection, pstmt, rs);
      MyBatis.closeQuietly(dbSession);
    }
  }

  public <T> void readLineHashesStream(DbSession dbSession, String fileUuid, Function<Reader, T> function) {
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
        function.apply(reader);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to read FILE_SOURCES.LINE_HASHES of file " + fileUuid, e);
    } finally {
      IOUtils.closeQuietly(reader);
      DbUtils.closeQuietly(connection, pstmt, rs);
    }
  }

  public void insert(FileSourceDto dto) {
    DbSession session = mybatis.openSession(false);
    try {
      insert(session, dto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void insert(DbSession session, FileSourceDto dto) {
    mapper(session).insert(dto);
  }

  public void update(FileSourceDto dto) {
    DbSession session = mybatis.openSession(false);
    try {
      update(session, dto);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void update(DbSession session, FileSourceDto dto) {
    mapper(session).update(dto);
  }

  public void updateDateWhenUpdatedDateIsZero(DbSession session, String projectUuid, long updateDate) {
    mapper(session).updateDateWhenUpdatedDateIsZero(projectUuid, updateDate);
  }

  private FileSourceMapper mapper(DbSession session) {
    return session.getMapper(FileSourceMapper.class);
  }
}
