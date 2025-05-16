/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import static com.google.common.base.Preconditions.checkArgument;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class CsQueueDao implements Dao {

  private static final Logger log = LoggerFactory.getLogger(CsQueueDao.class);
  private final System2 system;

  public CsQueueDao(System2 system) {
    this.system = system;
  }

  /**
   * @throws IllegalArgumentException if {@code csUniqueKey} is empty or null.
   */
  public void updateTaskUuid(DbSession dbSession, String taskUuid, String csUniqueKey) {
    checkArgument(StringUtils.isNotBlank(csUniqueKey), "cs_queue uniquekey can not be empty");
    Connection connection = dbSession.getConnection();
    String query = "UPDATE cs_queue SET ce_task_id = ? WHERE uniquekey = ?";
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, taskUuid);
      stmt.setString(2, csUniqueKey);
      int rows = stmt.executeUpdate();
      if (rows == 0) {
        log.warn("No job found for key {} while updating for task {}", csUniqueKey, taskUuid);
      }
      connection.commit();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to update task_id in cs_queue for task " + taskUuid, e);
    }
  }
}
