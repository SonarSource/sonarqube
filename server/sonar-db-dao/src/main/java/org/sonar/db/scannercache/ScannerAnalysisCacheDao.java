/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.scannercache;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.annotation.CheckForNull;
import org.sonar.db.Dao;
import org.sonar.db.DbInputStream;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;

public class ScannerAnalysisCacheDao implements Dao {
  public void removeAll(DbSession session) {
    mapper(session).removeAll();
  }

  public void remove(DbSession session, String branchUuid) {
    mapper(session).remove(branchUuid);
  }

  public void insert(DbSession dbSession, String branchUuid, InputStream data) {
    Connection connection = dbSession.getConnection();
    try (PreparedStatement stmt = connection.prepareStatement(
      "INSERT INTO scanner_analysis_cache (branch_uuid, data) VALUES (?, ?)")) {
      stmt.setString(1, branchUuid);
      stmt.setBinaryStream(2, data);
      stmt.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to insert cache for branch " + branchUuid, e);
    }
  }

  public void cleanOlderThan7Days(DbSession session) {
    long timestamp = LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.UTC).toEpochMilli();
    mapper(session).cleanOlderThan(timestamp);
  }

  @CheckForNull
  public DbInputStream selectData(DbSession dbSession, String branchUuid) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    DbInputStream result = null;
    try {
      stmt = dbSession.getConnection().prepareStatement("SELECT data FROM scanner_analysis_cache WHERE branch_uuid=?");
      stmt.setString(1, branchUuid);
      rs = stmt.executeQuery();
      if (rs.next()) {
        result = new DbInputStream(stmt, rs, rs.getBinaryStream(1));
        return result;
      }
      return null;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to select cache for branch " + branchUuid, e);
    } finally {
      if (result == null) {
        DatabaseUtils.closeQuietly(rs);
        DatabaseUtils.closeQuietly(stmt);
      }
    }
  }

  private static ScannerAnalysisCacheMapper mapper(DbSession session) {
    return session.getMapper(ScannerAnalysisCacheMapper.class);
  }
}
