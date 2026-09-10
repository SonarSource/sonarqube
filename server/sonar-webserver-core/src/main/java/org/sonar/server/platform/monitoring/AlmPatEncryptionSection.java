/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

/**
 * Reports how many user-scoped personal access tokens are stored as clear text, so that the exposure is visible
 * without reading the logs of the last startup.
 * <p>
 * Kept out of {@link AlmConfigurationSection} because every attribute there is keyed by the key an administrator
 * gave a DevOps platform configuration, and nothing stops them from choosing the one used here. Two attributes with
 * the same key collapse into one on serialisation, which would silently drop either the count or a configuration.
 * Section names are ours, so this cannot happen to a section of its own.
 */
@ServerSide
public class AlmPatEncryptionSection implements SystemInfoSection {

  private final DbClient dbClient;

  public AlmPatEncryptionSection(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("DevOps Platform Personal Access Tokens");

    try (DbSession dbSession = dbClient.openSession(false)) {
      // a non-zero count means no secret key is configured, since tokens are otherwise encrypted as they are written
      setAttribute(protobuf, "Stored As Clear Text", dbClient.almPatDao().countNotEncryptedPersonalAccessTokens(dbSession));
    }

    return protobuf.build();
  }
}
