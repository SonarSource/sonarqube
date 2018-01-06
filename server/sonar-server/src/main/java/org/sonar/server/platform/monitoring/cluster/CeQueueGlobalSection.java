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
package org.sonar.server.platform.monitoring.cluster;

import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.ce.configuration.WorkerCountProvider;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.process.systeminfo.Global;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

@ServerSide
public class CeQueueGlobalSection implements SystemInfoSection, Global {

  private static final int DEFAULT_NB_OF_WORKERS = 1;

  private final DbClient dbClient;
  @Nullable
  private final WorkerCountProvider workerCountProvider;

  public CeQueueGlobalSection(DbClient dbClient, @Nullable WorkerCountProvider workerCountProvider) {
    this.dbClient = dbClient;
    this.workerCountProvider = workerCountProvider;
  }

  public CeQueueGlobalSection(DbClient dbClient) {
    this(dbClient, null);
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("Compute Engine Tasks");
    try (DbSession dbSession = dbClient.openSession(false)) {
      setAttribute(protobuf, "Total Pending", dbClient.ceQueueDao().countByStatus(dbSession, CeQueueDto.Status.PENDING));
      setAttribute(protobuf, "Total In Progress", dbClient.ceQueueDao().countByStatus(dbSession, CeQueueDto.Status.IN_PROGRESS));
      setAttribute(protobuf, "Max Workers per Node", workerCountProvider == null ? DEFAULT_NB_OF_WORKERS : workerCountProvider.get());
    }
    return protobuf.build();
  }
}
