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

import java.util.List;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

@ServerSide
public class AlmConfigurationSection implements SystemInfoSection {
  private final DbClient dbClient;

  public AlmConfigurationSection(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public ProtobufSystemInfo.Section toProtobuf() {
    ProtobufSystemInfo.Section.Builder protobuf = ProtobufSystemInfo.Section.newBuilder();
    protobuf.setName("ALMs");

    try (DbSession dbSession = dbClient.openSession(false)) {
      List<AlmSettingDto> almSettingDtos = dbClient.almSettingDao().selectAll(dbSession);

      for (AlmSettingDto almSettingDto : almSettingDtos) {
        setAttribute(protobuf, almSettingDto.getKey(), buildValue(almSettingDto));
      }
    }

    return protobuf.build();
  }

  private static String buildValue(AlmSettingDto almSettingDto) {
    String value = String.format("Alm:%s", almSettingDto.getRawAlm());
    if (almSettingDto.getUrl() != null) {
      value += String.format(", Url:%s", almSettingDto.getUrl());
    }
    switch (almSettingDto.getAlm()) {
      case GITHUB:
        // add APP_ID and CLIENT_ID
        value += String.format(", App Id:%s, Client Id:%s", almSettingDto.getAppId(), almSettingDto.getClientId());
        break;
      case BITBUCKET_CLOUD:
        // WORKSPACE ID & OAuth key
        value += String.format(", Workspace Id:%s, OAuth Key:%s", almSettingDto.getAppId(), almSettingDto.getClientId());
        break;
      default:
        // no additional information for the other ALMs
        break;
    }
    return value;
  }
}
