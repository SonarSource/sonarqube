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
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.api.config.Configuration;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.monitoring.ServerMonitoringMetrics;

public class GitlabMetricsTask extends DevOpsMetricsTask {

  private final GitlabGlobalSettingsValidator gitlabValidator;

  public GitlabMetricsTask(ServerMonitoringMetrics metrics, GitlabGlobalSettingsValidator gitlabValidator,
    DbClient dbClient, Configuration config) {
    super(dbClient, metrics, config);
    this.gitlabValidator = gitlabValidator;
  }

  @Override
  public void run() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<AlmSettingDto> gitlabSettingsDtos = dbClient.almSettingDao().selectByAlm(dbSession, ALM.GITLAB);

      if (gitlabSettingsDtos.isEmpty()) {
        metrics.setGitlabStatusToRed();
        return;
      }

      validateGitlab(gitlabSettingsDtos);
    }
  }

  private void validateGitlab(List<AlmSettingDto> almSettingDtos) {
    try {
      for (AlmSettingDto dto : almSettingDtos) {
        gitlabValidator.validate(dto);
      }
      metrics.setGitlabStatusToGreen();
    } catch (RuntimeException e) {
      metrics.setGitlabStatusToRed();
    }
  }
}
