/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.monitoring.devops;

import java.util.List;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudValidator;
import org.sonar.alm.client.bitbucketserver.BitbucketServerSettingsValidator;
import org.sonar.api.config.Configuration;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.monitoring.ServerMonitoringMetrics;

public class BitbucketMetricsTask extends DevOpsMetricsTask {

  private final BitbucketCloudValidator bitbucketCloudValidator;
  private final BitbucketServerSettingsValidator bitbucketServerValidator;

  public BitbucketMetricsTask(ServerMonitoringMetrics metrics, BitbucketCloudValidator bitbucketCloudValidator,
    BitbucketServerSettingsValidator bitbucketServerSettingsValidator, DbClient dbClient, Configuration config) {
    super(dbClient, metrics, config);
    this.bitbucketCloudValidator = bitbucketCloudValidator;
    this.bitbucketServerValidator = bitbucketServerSettingsValidator;
  }

  @Override
  public void run() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<AlmSettingDto> bitbucketServerDtos = dbClient.almSettingDao().selectByAlm(dbSession, ALM.BITBUCKET);
      List<AlmSettingDto> bitbucketCloudDtos = dbClient.almSettingDao().selectByAlm(dbSession, ALM.BITBUCKET_CLOUD);

      if (bitbucketServerDtos.isEmpty() && bitbucketCloudDtos.isEmpty()) {
        metrics.setBitbucketStatusToRed();
        return;
      }

      try {
        validate(bitbucketServerDtos, bitbucketCloudDtos);
        metrics.setBitbucketStatusToGreen();
      } catch (RuntimeException e) {
        metrics.setBitbucketStatusToRed();
      }

    }
  }

  private void validate(List<AlmSettingDto> bitbucketServerDtos, List<AlmSettingDto> bitbucketCloudDtos) {
    for (AlmSettingDto dto : bitbucketServerDtos) {
      bitbucketServerValidator.validate(dto);
    }
    for (AlmSettingDto dto : bitbucketCloudDtos) {
      bitbucketCloudValidator.validate(dto);
    }
  }
}
