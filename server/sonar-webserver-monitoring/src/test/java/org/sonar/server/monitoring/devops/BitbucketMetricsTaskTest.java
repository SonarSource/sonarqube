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
package org.sonar.server.monitoring.devops;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudValidator;
import org.sonar.alm.client.bitbucketserver.BitbucketServerSettingsValidator;
import org.sonar.api.config.Configuration;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDao;
import org.sonar.server.monitoring.ServerMonitoringMetrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BitbucketMetricsTaskTest extends AbstractDevOpsMetricsTaskTest {

  private final ServerMonitoringMetrics metrics = mock(ServerMonitoringMetrics.class);
  private final BitbucketCloudValidator bitbucketCloudValidator = mock(BitbucketCloudValidator.class);
  private final BitbucketServerSettingsValidator bitbucketServerValidator = mock(BitbucketServerSettingsValidator.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final Configuration config = mock(Configuration.class);

  private final AlmSettingDao almSettingsDao = mock(AlmSettingDao.class);
  private final DbSession dbSession = mock(DbSession.class);

  private final BitbucketMetricsTask underTest = new BitbucketMetricsTask(metrics, bitbucketCloudValidator,
    bitbucketServerValidator, dbClient, config);

  @Before
  public void before() {
    when(dbClient.almSettingDao()).thenReturn(almSettingsDao);
    when(dbClient.openSession(anyBoolean())).thenReturn(dbSession);
  }

  @Test
  public void run_bitbucketValidatorsDontThrowException_setGreenStatusInMetricsOnce() {
    when(almSettingsDao.selectByAlm(dbSession, ALM.BITBUCKET)).thenReturn(generateDtos(5, ALM.BITBUCKET));
    when(almSettingsDao.selectByAlm(dbSession, ALM.BITBUCKET_CLOUD)).thenReturn(generateDtos(5, ALM.BITBUCKET_CLOUD));

    underTest.run();

    verify(metrics, times(1)).setBitbucketStatusToGreen();
    verify(metrics, times(0)).setBitbucketStatusToRed();
  }

  @Test
  public void run_bitbucketValidatorsDoThrowException_setRedStatusInMetricsOnce() {
    when(almSettingsDao.selectByAlm(dbSession, ALM.BITBUCKET)).thenReturn(generateDtos(5, ALM.BITBUCKET));
    when(almSettingsDao.selectByAlm(dbSession, ALM.BITBUCKET_CLOUD)).thenReturn(generateDtos(5, ALM.BITBUCKET_CLOUD));

    doThrow(new RuntimeException()).when(bitbucketCloudValidator).validate(any());
    doThrow(new RuntimeException()).when(bitbucketServerValidator).validate(any());

    underTest.run();

    verify(metrics, times(0)).setBitbucketStatusToGreen();
    verify(metrics, times(1)).setBitbucketStatusToRed();
  }


  @Test
  public void run_bitbucketServerValidatorThrowExceptionCloudDoesNot_setRedStatusInMetricsOnce() {
    when(almSettingsDao.selectByAlm(dbSession, ALM.BITBUCKET)).thenReturn(generateDtos(5, ALM.BITBUCKET));
    when(almSettingsDao.selectByAlm(dbSession, ALM.BITBUCKET_CLOUD)).thenReturn(generateDtos(5, ALM.BITBUCKET_CLOUD));

    doThrow(new RuntimeException()).when(bitbucketServerValidator).validate(any());

    underTest.run();

    verify(metrics, times(0)).setBitbucketStatusToGreen();
    verify(metrics, times(1)).setBitbucketStatusToRed();
  }

  @Test
  public void run_bitbucketServerConfiguredBitbucketCloudNot_setGreenStatusInMetricsOnce() {
    when(almSettingsDao.selectByAlm(dbSession, ALM.BITBUCKET)).thenReturn(generateDtos(1, ALM.BITBUCKET));

    underTest.run();

    verify(metrics, times(1)).setBitbucketStatusToGreen();
    verify(metrics, times(0)).setBitbucketStatusToRed();
  }

  @Test
  public void run_bitbucketIntegrationNotConfigured_setRedStatusInMetricsOnce() {
    when(almSettingsDao.selectByAlm(dbSession, ALM.BITBUCKET)).thenReturn(Collections.emptyList());
    when(almSettingsDao.selectByAlm(dbSession, ALM.BITBUCKET_CLOUD)).thenReturn(Collections.emptyList());

    underTest.run();

    verify(metrics, times(0)).setBitbucketStatusToGreen();
    verify(metrics, times(1)).setBitbucketStatusToRed();
  }

}
