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

package org.sonar.server.platform.monitoring;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.sonar.core.persistence.DatabaseVersion;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Information about database and connection pool
 */
public class DatabaseMonitor extends BaseMonitorMBean implements DatabaseMonitorMBean {

  private final DatabaseVersion dbVersion;
  private final DbClient dbClient;

  public DatabaseMonitor(DatabaseVersion dbVersion, DbClient dbClient) {
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
  public LinkedHashMap<String, Object> attributes() {
    LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
    completeDbAttributes(attributes);
    completePoolAttributes(attributes);
    return attributes;
  }

  private void completePoolAttributes(Map<String, Object> attributes) {
    attributes.put("Pool Active Connections", getPoolActiveConnections());
    attributes.put("Pool Max Connections", getPoolMaxActiveConnections());
    attributes.put("Pool Initial Size", getPoolInitialSize());
    attributes.put("Pool Idle Connections", getPoolIdleConnections());
    attributes.put("Pool Min Idle Connections", getPoolMinIdleConnections());
    attributes.put("Pool Max Idle Connections", getPoolMaxIdleConnections());
    attributes.put("Pool Max Wait (ms)", getPoolMaxWaitMillis());
    attributes.put("Pool Remove Abandoned", getPoolRemoveAbandoned());
    attributes.put("Pool Remove Abandoned Timeout (seconds)", getPoolRemoveAbandonedTimeoutSeconds());
  }

  private BasicDataSource commonsDbcp() {
    return (BasicDataSource) dbClient.database().getDataSource();
  }

  private void completeDbAttributes(Map<String, Object> attributes) {
    DbSession dbSession = dbClient.openSession(false);
    Connection connection = dbSession.getConnection();
    try {
      DatabaseMetaData metadata = connection.getMetaData();
      attributes.put("Database", metadata.getDatabaseProductName());
      attributes.put("Database Version", metadata.getDatabaseProductVersion());
      attributes.put("Username", metadata.getUserName());
      attributes.put("URL", metadata.getURL());
      attributes.put("Driver", metadata.getDriverName());
      attributes.put("Driver Version", metadata.getDriverVersion());
      attributes.put("Version Status", getMigrationStatus());
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to get DB metadata", e);

    } finally {
      DbUtils.closeQuietly(connection);
      MyBatis.closeQuietly(dbSession);
    }
  }
}
