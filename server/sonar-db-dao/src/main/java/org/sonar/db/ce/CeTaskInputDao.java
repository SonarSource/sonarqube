/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbInputStream;
import org.sonar.db.DbSession;

public class CeTaskInputDao implements Dao {

  private final System2 system;

  public CeTaskInputDao(System2 system) {
    this.system = system;
  }

  public void insert(DbSession dbSession, String taskUuid, long partNumber, InputStream data) {
    long now = system.now();
    Connection connection = dbSession.getConnection();
    try (PreparedStatement stmt = connection.prepareStatement(
      "INSERT INTO ce_task_input (task_uuid, part_number, created_at, updated_at, input_data) VALUES (?,?, ?, ?, ?)")) {
      stmt.setString(1, taskUuid);
      stmt.setLong(2, partNumber);
      stmt.setLong(3, now);
      stmt.setLong(4, now);
      stmt.setBinaryStream(5, data);
      stmt.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to insert data of CE task " + taskUuid, e);
    }
  }

  public Optional<DbInputStream> selectData(DbSession dbSession, String taskUuid, int partNumber) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    DbInputStream result = null;
    try {
      stmt = dbSession.getConnection().prepareStatement("SELECT input_data FROM ce_task_input WHERE task_uuid=? AND part_number=? AND input_data IS NOT NULL");
      stmt.setString(1, taskUuid);
      stmt.setInt(2, partNumber);
      rs = stmt.executeQuery();
      if (rs.next()) {
        result = new DbInputStream(stmt, rs, rs.getBinaryStream(1));
        return Optional.of(result);
      }
      return Optional.empty();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to select data of CE task " + taskUuid + " part " + partNumber, e);
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
}
