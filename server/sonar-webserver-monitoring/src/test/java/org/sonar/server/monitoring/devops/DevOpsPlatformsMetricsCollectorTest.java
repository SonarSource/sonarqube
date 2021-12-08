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

import org.sonar.api.config.Configuration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sonar.alm.client.azure.AzureDevOpsValidator;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudValidator;
import org.sonar.alm.client.bitbucketserver.BitbucketServerSettingsValidator;
import org.sonar.alm.client.github.GithubGlobalSettingsValidator;
import org.sonar.alm.client.gitlab.GitlabGlobalSettingsValidator;
import org.sonar.db.DbClient;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.monitoring.ServerMonitoringMetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DevOpsPlatformsMetricsCollectorTest {

  private final ServerMonitoringMetrics serverMonitoringMetrics = mock(ServerMonitoringMetrics.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final BitbucketServerSettingsValidator bitbucketServerValidator = mock(BitbucketServerSettingsValidator.class);
  private final GithubGlobalSettingsValidator githubValidator = mock(GithubGlobalSettingsValidator.class);
  private final GitlabGlobalSettingsValidator gitlabValidator = mock(GitlabGlobalSettingsValidator.class);
  private final BitbucketCloudValidator bitbucketCloudValidator = mock(BitbucketCloudValidator.class);
  private final AzureDevOpsValidator azureDevOpsValidator = mock(AzureDevOpsValidator.class);
  private final Configuration config = mock(Configuration.class);

  private DevOpsPlatformsMetricsCollector collector;

  @Before
  public void before() {
    collector = new DevOpsPlatformsMetricsCollector(serverMonitoringMetrics,
      dbClient, bitbucketServerValidator, githubValidator, gitlabValidator, bitbucketCloudValidator,
      azureDevOpsValidator, config);
  }

  @Test
  public void start_startsNewDeamonThread() {
    collector.start();

    Optional<Thread> newDeamonThread = findNewDeamonThread();

    assertThat(newDeamonThread).isPresent();
    assertThat(newDeamonThread.get().isDaemon()).isTrue();
  }

  @Test
  public void createTask_givenOneConfigForEachALM_allValidatorsAreCalled() {
    AlmSettingDao dao = mock(AlmSettingDao.class);
    List<AlmSettingDto> almSettingDtos = createAlmSettingDtos();
    when(dao.selectAll(any())).thenReturn(almSettingDtos);
    when(dbClient.almSettingDao()).thenReturn(dao);

    collector.createTask().run();

    verify(bitbucketCloudValidator, times(1)).validate(findDto(ALM.BITBUCKET_CLOUD, almSettingDtos));
    verify(bitbucketServerValidator, times(1)).validate(findDto(ALM.BITBUCKET, almSettingDtos));
    verify(azureDevOpsValidator, times(1)).validate(findDto(ALM.AZURE_DEVOPS, almSettingDtos));
    verify(gitlabValidator, times(1)).validate(findDto(ALM.GITLAB, almSettingDtos));
    verify(githubValidator, times(1)).validate(findDto(ALM.GITHUB, almSettingDtos));
  }

  @Test
  public void createTask_givenOnlyGitHubConfigured_validateOnlyGithub() {
    AlmSettingDao dao = mock(AlmSettingDao.class);
    AlmSettingDto githubDto = new AlmSettingDto();
    githubDto.setAlm(ALM.GITHUB);
    when(dao.selectAll(any())).thenReturn(List.of(githubDto));
    when(dbClient.almSettingDao()).thenReturn(dao);

    collector.createTask().run();

    verifyNoInteractions(bitbucketCloudValidator);
    verifyNoInteractions(bitbucketServerValidator);
    verifyNoInteractions(azureDevOpsValidator);
    verifyNoInteractions(gitlabValidator);

    verify(githubValidator, times(1)).validate(githubDto);
  }

  @Test
  public void createTask_givenAllValidationsFailing_setAllMetricsStatusesToFalse() {
    AlmSettingDao dao = mock(AlmSettingDao.class);
    List<AlmSettingDto> almSettingDtos = createAlmSettingDtos();
    when(dao.selectAll(any())).thenReturn(almSettingDtos);
    when(dbClient.almSettingDao()).thenReturn(dao);

    doThrow(new RuntimeException()).when(bitbucketCloudValidator).validate(any());
    doThrow(new RuntimeException()).when(bitbucketServerValidator).validate(any());
    doThrow(new RuntimeException()).when(azureDevOpsValidator).validate(any());
    doThrow(new RuntimeException()).when(gitlabValidator).validate(any());
    doThrow(new RuntimeException()).when(githubValidator).validate(any());

    collector.createTask().run();

    verify(serverMonitoringMetrics, times(2)).setBitbucketStatusToRed(); //2 validators for Bitbucket
    verify(serverMonitoringMetrics, times(1)).setAzureStatusToRed();
    verify(serverMonitoringMetrics, times(1)).setGitlabStatusToRed();
    verify(serverMonitoringMetrics, times(1)).setGithubStatusToRed();
  }

  @Test
  public void createTask_givenAllValidationsArePassing_setAllMetricsStatusesToTrue() {
    AlmSettingDao dao = mock(AlmSettingDao.class);
    List<AlmSettingDto> almSettingDtos = createAlmSettingDtos();
    when(dao.selectAll(any())).thenReturn(almSettingDtos);
    when(dbClient.almSettingDao()).thenReturn(dao);

    collector.createTask().run();

    verify(serverMonitoringMetrics, times(2)).setBitbucketStatusToGreen(); //2 validators for Bitbucket
    verify(serverMonitoringMetrics, times(1)).setAzureStatusToGreen();
    verify(serverMonitoringMetrics, times(1)).setGitlabStatusToGreen();
    verify(serverMonitoringMetrics, times(1)).setGithubStatusToGreen();
  }

  @Test
  public void createTask_givenFirstGithubValidationNotPassingAndSecondPassing_setGitHubValidationToTrue() {
    AlmSettingDao dao = mock(AlmSettingDao.class);
    List<AlmSettingDto> almSettingDtos = createAlmSettingDtos();
    when(dao.selectAll(any())).thenReturn(almSettingDtos);
    when(dbClient.almSettingDao()).thenReturn(dao);

    when(githubValidator.validate(any()))
      .thenThrow(new RuntimeException())
      .thenReturn(null);

    collector.createTask().run();

    verify(serverMonitoringMetrics, times(1)).setGithubStatusToRed();
    verify(serverMonitoringMetrics, times(0)).setGithubStatusToGreen();
  }

  private AlmSettingDto findDto(ALM alm, List<AlmSettingDto> almSettingDtos) {
    return almSettingDtos.stream().filter(d -> d.getAlm() == alm).findFirst().get();
  }

  private List<AlmSettingDto> createAlmSettingDtos() {
    List<AlmSettingDto> dtos = new ArrayList<>();
    for(ALM alm : ALM.values()) {
      AlmSettingDto almSettingDto = new AlmSettingDto();
      almSettingDto.setAlm(alm);
      dtos.add(almSettingDto);
    }
    return dtos;
  }

  private Optional<Thread> findNewDeamonThread() {
    Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
    String threadPartialName = DevOpsPlatformsMetricsCollector.class.getName();
    return threadSet.stream().filter(t -> t.getName().contains(threadPartialName)).findFirst();
  }
}
