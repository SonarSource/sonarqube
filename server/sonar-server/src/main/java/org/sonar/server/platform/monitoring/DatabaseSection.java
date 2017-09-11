/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.apache.commons.dbcp.BasicDataSource;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Section;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

/**
 * Information about database and connection pool
 */
public class DatabaseSection extends BaseSectionMBean implements DatabaseSectionMBean {

  private final DatabaseVersion dbVersion;
  private final DbClient dbClient;

  public DatabaseSection(DatabaseVersion dbVersion, DbClient dbClient) {
    this.dbVersion = dbVersion;
    this.dbClient = dbClient;
  }

  @Override
  public String name() {
    return "Database";
  }

  @Override
  public String getMigrationStatus() {
    return dbVersion.getStatus().name();
  }

  @Override
  public int getPoolActiveConnections() {
    return commonsDbcp().getNumActive();
  }

  @Override
  public int getPoolMaxActiveConnections() {
    return commonsDbcp().getMaxActive();
  }

  @Override
  public int getPoolIdleConnections() {
    return commonsDbcp().getNumIdle();
  }

  @Override
  public int getPoolMaxIdleConnections() {
    return commonsDbcp().getMaxIdle();
  }

  @Override
  public int getPoolMinIdleConnections() {
    return commonsDbcp().getMinIdle();
  }

  @Override
  public int getPoolInitialSize() {
    return commonsDbcp().getInitialSize();
  }

  @Override
  public long getPoolMaxWaitMillis() {
    return commonsDbcp().getMaxWait();
  }

  @Override
  public boolean getPoolRemoveAbandoned() {
    return commonsDbcp().getRemoveAbandoned();
  }

  @Override
  public int getPoolRemoveAbandonedTimeoutSeconds() {
    return commonsDbcp().getRemoveAbandonedTimeout();
  }

  @Override
  public Section toProtobuf() {
    Section.Builder protobuf = Section.newBuilder();
    protobuf.setName(name());
    completeDbAttributes(protobuf);
    completePoolAttributes(protobuf);
    return protobuf.build();
  }

  private void completePoolAttributes(Section.Builder protobuf) {
    setAttribute(protobuf, "Pool Active Connections", getPoolActiveConnections());
    setAttribute(protobuf, "Pool Max Connections", getPoolMaxActiveConnections());
    setAttribute(protobuf, "Pool Initial Size", getPoolInitialSize());
    setAttribute(protobuf, "Pool Idle Connections", getPoolIdleConnections());
    setAttribute(protobuf, "Pool Min Idle Connections", getPoolMinIdleConnections());
    setAttribute(protobuf, "Pool Max Idle Connections", getPoolMaxIdleConnections());
    setAttribute(protobuf, "Pool Max Wait (ms)", getPoolMaxWaitMillis());
    setAttribute(protobuf, "Pool Remove Abandoned", getPoolRemoveAbandoned());
    setAttribute(protobuf, "Pool Remove Abandoned Timeout (seconds)", getPoolRemoveAbandonedTimeoutSeconds());
  }

  private BasicDataSource commonsDbcp() {
    return (BasicDataSource) dbClient.getDatabase().getDataSource();
  }

  private void completeDbAttributes(Section.Builder protobuf) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      DatabaseMetaData metadata = dbSession.getConnection().getMetaData();
      setAttribute(protobuf, "Database", metadata.getDatabaseProductName());
      setAttribute(protobuf, "Database Version", metadata.getDatabaseProductVersion());
      setAttribute(protobuf, "Username", metadata.getUserName());
      setAttribute(protobuf, "URL", metadata.getURL());
      setAttribute(protobuf, "Driver", metadata.getDriverName());
      setAttribute(protobuf, "Driver Version", metadata.getDriverVersion());
      setAttribute(protobuf, "Version Status", getMigrationStatus());
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to get DB metadata", e);
    }
  }
}
