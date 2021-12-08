/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import org.picocontainer.Startable;
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudValidator;
import org.sonar.alm.client.bitbucketserver.BitbucketServerSettingsValidator;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.monitoring.ServerMonitoringMetrics;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@ServerSide
public class DevOpsPlatformsMetricsCollector implements Startable {

  private static final String DELAY_IN_MILISECONDS_PROPERTY = "sonar.server.monitoring.devops.initial.delay";
  private static final String PERIOD_IN_MILISECONDS_PROPERTY = "sonar.server.monitoring.devops.period";

  private final Configuration config;

  private final BitbucketServerSettingsValidator bitbucketServerValidator;
  private final GithubGlobalSettingsValidator githubValidator;
  private final GitlabGlobalSettingsValidator gitlabValidator;
  private final BitbucketCloudValidator bitbucketCloudValidator;
  private final AzureDevOpsValidator azureDevOpsValidator;

  private final DbClient dbClient;
  private final ServerMonitoringMetrics metrics;

  private ScheduledExecutorService scheduledExecutorService;

  public DevOpsPlatformsMetricsCollector(ServerMonitoringMetrics metrics, DbClient dbClient,
    BitbucketServerSettingsValidator bitbucketServerValidator, GithubGlobalSettingsValidator githubValidator,
    GitlabGlobalSettingsValidator gitlabValidator, BitbucketCloudValidator bitbucketCloudValidator,
    AzureDevOpsValidator azureDevOpsValidator, Configuration config) {
    this.bitbucketCloudValidator = bitbucketCloudValidator;
    this.bitbucketServerValidator = bitbucketServerValidator;
    this.githubValidator = githubValidator;
    this.azureDevOpsValidator = azureDevOpsValidator;
    this.gitlabValidator = gitlabValidator;
    this.metrics = metrics;
    this.dbClient = dbClient;
    this.config = config;
  }

  @Override
  public void start() {
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder()
      .setDaemon(true)
      .setNameFormat(getClass().getCanonicalName() + "-thread-%d")
      .build());
    long delayInMilliseconds = config.getLong(DELAY_IN_MILISECONDS_PROPERTY).orElse(10_000L);
    long periodInMilliseconds = config.getLong(PERIOD_IN_MILISECONDS_PROPERTY).orElse(300_000L);
    scheduledExecutorService.scheduleWithFixedDelay(createTask(), delayInMilliseconds, periodInMilliseconds, MILLISECONDS);
  }

  @Override
  public void stop() {
    scheduledExecutorService.shutdown();
  }

  @VisibleForTesting
  Runnable createTask() {
    return () -> {
      try (DbSession dbSession = dbClient.openSession(false)) {
        List<AlmSettingDto> almSettingDtos = dbClient.almSettingDao().selectAll(dbSession);
        validateBitbucket(getALMsDTOs(almSettingDtos, ALM.BITBUCKET));
        validateBitbucketCloud(getALMsDTOs(almSettingDtos, ALM.BITBUCKET_CLOUD));
        validateGithub(getALMsDTOs(almSettingDtos, ALM.GITHUB));
        validateGitlab(getALMsDTOs(almSettingDtos, ALM.GITLAB));
        validateAzure(getALMsDTOs(almSettingDtos, ALM.AZURE_DEVOPS));
      }
    };
  }

  private static List<AlmSettingDto> getALMsDTOs(List<AlmSettingDto> almSettingDtos, ALM alm) {
    return almSettingDtos.stream().filter(dto -> dto.getAlm() == alm).collect(Collectors.toList());
  }

  private void validateGithub(List<AlmSettingDto> almSettingDtos) {
    try {
      for (AlmSettingDto dto : almSettingDtos) {
        githubValidator.validate(dto);
      }
      metrics.setGithubStatusToGreen();
    } catch (RuntimeException e) {
      metrics.setGithubStatusToRed();
    }
  }

  private void validateBitbucket(List<AlmSettingDto> almSettingDtos) {
    try {
      for (AlmSettingDto dto : almSettingDtos) {
        bitbucketServerValidator.validate(dto);
      }
      metrics.setBitbucketStatusToGreen();
    } catch (Exception e) {
      metrics.setBitbucketStatusToRed();
    }
  }

  private void validateBitbucketCloud(List<AlmSettingDto> almSettingDtos) {
    try {
      for (AlmSettingDto dto : almSettingDtos) {
        bitbucketCloudValidator.validate(dto);
      }
      metrics.setBitbucketStatusToGreen();
    } catch (Exception e) {
      metrics.setBitbucketStatusToRed();
    }
  }

  private void validateGitlab(List<AlmSettingDto> almSettingDtos) {
    try {
      for (AlmSettingDto dto : almSettingDtos) {
        gitlabValidator.validate(dto);
      }
      metrics.setGitlabStatusToGreen();
    } catch (Exception e) {
      metrics.setGitlabStatusToRed();
    }
  }

  private void validateAzure(List<AlmSettingDto> almSettingDtos) {
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
