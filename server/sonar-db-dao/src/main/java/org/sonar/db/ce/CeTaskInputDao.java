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
package org.sonar.db.ce;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;

public class CeTaskInputDao implements Dao {

  private final System2 system;

  public CeTaskInputDao(System2 system) {
    this.system = system;
  }

  public void insert(DbSession dbSession, String taskUuid, InputStream data) {
    long now = system.now();
    Connection connection = dbSession.getConnection();
    try (PreparedStatement stmt = connection.prepareStatement(
      "INSERT INTO ce_task_input (task_uuid, created_at, updated_at, input_data) VALUES (?, ?, ?, ?)")) {
      stmt.setString(1, taskUuid);
      stmt.setLong(2, now);
      stmt.setLong(3, now);
      stmt.setBinaryStream(4, data);
      stmt.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to insert data of CE task " + taskUuid, e);
    }
  }

  public Optional<DataStream> selectData(DbSession dbSession, String taskUuid) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    DataStream result = null;
    try {
      stmt = dbSession.getConnection().prepareStatement("SELECT input_data FROM ce_task_input WHERE task_uuid=? AND input_data IS NOT NULL");
      stmt.setString(1, taskUuid);
      rs = stmt.executeQuery();
      if (rs.next()) {
        result = new DataStream(stmt, rs, rs.getBinaryStream(1));
        return Optional.of(result);
      }
      return Optional.empty();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to select data of CE task " + taskUuid, e);
    } finally {
      if (result == null) {
        DatabaseUtils.closeQuietly(rs);
        DatabaseUtils.closeQuietly(stmt);
      }
    }
  }

  public List<String> selectUuidsNotInQueue(DbSession dbSession) {
    return dbSession.getMapper(CeTaskInputMapper.class).selectUuidsNotInQueue();
  }

  public void deleteByUuids(DbSession dbSession, Collection<String> uuids) {
    CeTaskInputMapper mapper = dbSession.getMapper(CeTaskInputMapper.class);
    DatabaseUtils.executeLargeUpdates(uuids, mapper::deleteByUuids);
  }

  public static class DataStream implements AutoCloseable {
    private final PreparedStatement stmt;
    private final ResultSet rs;
    private final InputStream stream;

    private DataStream(PreparedStatement stmt, ResultSet rs, InputStream stream) {
      this.stmt = stmt;
      this.rs = rs;
      this.stream = stream;
    }

    public InputStream getInputStream() {
      return stream;
    }

    @Override
    public void close() {
      IOUtils.closeQuietly(stream);
      DatabaseUtils.closeQuietly(rs);
      DatabaseUtils.closeQuietly(stmt);
    }
  }
}
