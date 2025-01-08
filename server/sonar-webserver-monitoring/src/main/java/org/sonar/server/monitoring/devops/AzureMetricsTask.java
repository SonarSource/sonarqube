/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.api.config.Configuration;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.monitoring.ServerMonitoringMetrics;

public class AzureMetricsTask extends DevOpsMetricsTask {

  private final AzureDevOpsValidator azureDevOpsValidator;

  public AzureMetricsTask(ServerMonitoringMetrics metrics, AzureDevOpsValidator azureDevOpsValidator,
    DbClient dbClient, Configuration config) {
    super(dbClient, metrics, config);
    this.azureDevOpsValidator = azureDevOpsValidator;
  }

  @Override
  public void run() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<AlmSettingDto> azureSettingsDtos = dbClient.almSettingDao().selectByAlm(dbSession, ALM.AZURE_DEVOPS);

      if (azureSettingsDtos.isEmpty()) {
        metrics.setAzureStatusToRed();
        return;
      }

      validate(azureSettingsDtos);
    }
  }

  private void validate(List<AlmSettingDto> almSettingDtos) {
    try {
      for (AlmSettingDto dto : almSettingDtos) {
        azureDevOpsValidator.validate(dto);
      }
      metrics.setAzureStatusToGreen();
    } catch (Exception e) {
      metrics.setAzureStatusToRed();
    }
  }
}
