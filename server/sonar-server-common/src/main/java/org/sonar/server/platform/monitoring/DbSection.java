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
package org.sonar.server.platform.monitoring;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.process.systeminfo.Global;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Section;

import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

/**
 * Information about database
 */
public class DbSection implements SystemInfoSection, Global {

  private static final Map<Integer, String> ISOLATION_LEVEL_BY_ID = Map.of(
    TRANSACTION_NONE, "TRANSACTION_NONE",
    TRANSACTION_READ_UNCOMMITTED, "TRANSACTION_READ_UNCOMMITTED",
    TRANSACTION_READ_COMMITTED, "TRANSACTION_READ_COMMITTED",
    TRANSACTION_REPEATABLE_READ, "TRANSACTION_REPEATABLE_READ",
    TRANSACTION_SERIALIZABLE, "TRANSACTION_SERIALIZABLE");

  private final DbClient dbClient;

  public DbSection(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public Section toProtobuf() {
    Section.Builder protobuf = Section.newBuilder();
    protobuf.setName("Database");
    try (DbSession dbSession = dbClient.openSession(false)) {
      DatabaseMetaData metadata = dbSession.getConnection().getMetaData();
      setAttribute(protobuf, "Database", metadata.getDatabaseProductName());
      setAttribute(protobuf, "Database Version", metadata.getDatabaseProductVersion());
      setAttribute(protobuf, "Username", metadata.getUserName());
      setAttribute(protobuf, "URL", metadata.getURL());
      setAttribute(protobuf, "Driver", metadata.getDriverName());
      setAttribute(protobuf, "Driver Version", metadata.getDriverVersion());
      setAttribute(protobuf, "Default transaction isolation", toTransactionIsolationLevelName(metadata.getDefaultTransactionIsolation()));
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to get DB metadata", e);
    }
    return protobuf.build();
  }

  private static String toTransactionIsolationLevelName(int level) {
    return ISOLATION_LEVEL_BY_ID.getOrDefault(level, "Unknown transaction level: " + level);
  }
}
