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

import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.db.DatabaseMBean;
import org.sonar.db.DbClient;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo.Section;
import org.sonar.server.platform.db.migration.version.DatabaseVersion;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

/**
 * Information about database connection pool
 */
public class DbConnectionSection extends DatabaseMBean implements DbConnectionSectionMBean {

  private final DatabaseVersion dbVersion;
  private final SonarRuntime runtime;

  public DbConnectionSection(DatabaseVersion dbVersion, DbClient dbClient, SonarRuntime runtime) {
    super(dbClient);
    this.dbVersion = dbVersion;
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
  public Section toProtobuf() {
    Section.Builder protobuf = Section.newBuilder();
    String side = runtime.getSonarQubeSide() == SonarQubeSide.COMPUTE_ENGINE ? "Compute Engine" : "Web";
    protobuf.setName(side + " Database Connection");
    completePoolAttributes(protobuf);
    return protobuf.build();
  }

  private void completePoolAttributes(Section.Builder protobuf) {
    setAttribute(protobuf, "Pool Total Connections", getPoolTotalConnections());
    setAttribute(protobuf, "Pool Active Connections", getPoolActiveConnections());
    setAttribute(protobuf, "Pool Idle Connections", getPoolIdleConnections());
    setAttribute(protobuf, "Pool Max Connections", getPoolMaxConnections());
    setAttribute(protobuf, "Pool Min Idle Connections", getPoolMinIdleConnections());
    setAttribute(protobuf, "Pool Max Wait (ms)", getPoolMaxWaitMillis());
    setAttribute(protobuf, "Pool Max Lifetime (ms)", getPoolMaxLifeTimeMillis());
  }

}
