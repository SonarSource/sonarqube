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
package org.sonar.server.platform.monitoring;

import org.apache.commons.dbcp.BasicDataSource;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.db.DbClient;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Section;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

/**
 * Information about database connection pool
 */
public class DbConnectionSection extends BaseSectionMBean implements DbConnectionSectionMBean {

  private final DatabaseVersion dbVersion;
  private final DbClient dbClient;
  private final SonarRuntime runtime;

  public DbConnectionSection(DatabaseVersion dbVersion, DbClient dbClient, SonarRuntime runtime) {
    this.dbVersion = dbVersion;
    this.dbClient = dbClient;
    this.runtime = runtime;
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
    String side = runtime.getSonarQubeSide() == SonarQubeSide.COMPUTE_ENGINE ? "Compute Engine" : "Web";
    protobuf.setName(side + " Database Connection");
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
}
