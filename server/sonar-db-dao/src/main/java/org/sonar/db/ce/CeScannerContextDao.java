/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.nio.charset.Charset;
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

public class CeScannerContextDao implements Dao {

  private static final Charset UTF_8 = Charset.forName("UTF-8");

  private final System2 system;

  public CeScannerContextDao(System2 system) {
    this.system = system;
  }

  /**
   * @throws IllegalArgumentException if {@code scannerContextLines} is empty or fully read.
   */
  public void insert(DbSession dbSession, String taskUuid, CloseableIterator<String> scannerContextLines) {
    checkArgument(scannerContextLines.hasNext(), "Scanner context can not be empty");
    long now = system.now();
    Connection connection = dbSession.getConnection();
    try (PreparedStatement stmt = connection.prepareStatement(
        "INSERT INTO ce_scanner_context (task_uuid, created_at, updated_at, context_data) VALUES (?, ?, ?, ?)");
      InputStream inputStream = new LogsIteratorInputStream(scannerContextLines, UTF_8)) {
      stmt.setString(1, taskUuid);
      stmt.setLong(2, now);
      stmt.setLong(3, now);
      stmt.setBinaryStream(4, inputStream);
      stmt.executeUpdate();
      connection.commit();
    } catch (SQLException | IOException e) {
      throw new IllegalStateException("Fail to insert scanner context for task " + taskUuid, e);
    }
  }

  /**
   * The scanner context is very likely to contain lines, which are forcefully separated by {@code \n} characters,
   * whichever the platform SQ is running on ({@see LogsIteratorInputStream}).
   */
  public Optional<String> selectScannerContext(DbSession dbSession, String taskUuid) {
    try (PreparedStatement stmt = dbSession.getConnection().prepareStatement("select context_data from ce_scanner_context where task_uuid=?")) {
      stmt.setString(1, taskUuid);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(IOUtils.toString(rs.getBinaryStream(1), UTF_8));
        }
        return Optional.empty();
      }
    } catch (SQLException | IOException e) {
      throw new IllegalStateException("Fail to retrieve scanner context of task " + taskUuid, e);
    }
  }

  public Set<String> selectOlderThan(DbSession dbSession, long beforeDate) {
    return mapper(dbSession).selectOlderThan(beforeDate);
  }

  public void deleteByUuids(DbSession dbSession, Collection<String> uuids) {
    DatabaseUtils.executeLargeUpdates(uuids, mapper(dbSession)::deleteByUuids);
  }

  private static CeScannerContextMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(CeScannerContextMapper.class);
  }
}
