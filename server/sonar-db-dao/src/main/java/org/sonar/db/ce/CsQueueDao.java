/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.System2;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.Dao;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.DbSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

public class CsQueueDao implements Dao {

  private final System2 system;

  public CsQueueDao(System2 system) {
    this.system = system;
  }

  /**
   * @throws IllegalArgumentException if {@code csUniqueKey} is empty or null.
   */
  public void updateTaskUuid(DbSession dbSession, String taskUuid, String csUniqueKey) {
    checkArgument(csUniqueKey != null, "cs_queue uniquekey can not be empty");
    long now = system.now();
    Connection connection = dbSession.getConnection();
    String query = "UPDATE cs_queue SET ce_task_id = ? WHERE uniquekey = ?";
    try (PreparedStatement stmt = connection.prepareStatement(query)) {
      stmt.setString(1, taskUuid);
      stmt.setString(2, csUniqueKey);
      stmt.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to update task_id in cs_queue for task " + taskUuid, e);
    }
  }
}
