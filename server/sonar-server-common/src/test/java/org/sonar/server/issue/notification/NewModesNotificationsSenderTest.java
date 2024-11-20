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
package org.sonar.server.issue.notification;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.notifications.Notification;
import org.sonar.core.metric.SoftwareQualitiesMetrics;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDao;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.qualitygate.QualityGateConditionDao;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.platform.db.migration.history.MigrationHistory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;

class NewModesNotificationsSenderTest {
  private static final String METRIC_UUID = "metricUuid";
  private static final String METRIC_UUID_2 = "metricUuid2";
  private final NotificationManager notificationManager = mock(NotificationManager.class);
  private final Configuration configuration = mock(Configuration.class);
  private final MigrationHistory migrationHistory = mock(MigrationHistory.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final MetricDao metricDao = mock(MetricDao.class);
  private final QualityGateConditionDao qualityGateConditionDao = mock(QualityGateConditionDao.class);
  private final NewModesNotificationsSender underTest = new NewModesNotificationsSender(notificationManager, configuration, migrationHistory, dbClient);
  private final DbSession dbSession = mock(DbSession.class);

  @BeforeEach
  void setUp() {
    when(dbClient.metricDao()).thenReturn(metricDao);
    when(dbClient.gateConditionDao()).thenReturn(qualityGateConditionDao);
    when(dbClient.openSession(false)).thenReturn(dbSession);
  }

  @Test
  void start_whenOldInstanceAndStandardMode_shouldSendNewModesNotification() {
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(false));
    when(migrationHistory.getInitialDbVersion()).thenReturn(9999L); // 9.9
    underTest.start();

    ArgumentCaptor<MQRAndStandardModesExistNotification> captor = ArgumentCaptor.forClass(MQRAndStandardModesExistNotification.class);
    verify(notificationManager, times(1)).scheduleForSending(captor.capture());

    assertThat(captor.getValue().isMQRModeEnabled()).isFalse();
  }

  @Test
  void start_whenOldInstanceAndMQRMode_shouldSendNewModesNotification() {
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(true));
    when(migrationHistory.getInitialDbVersion()).thenReturn(102000L); // 10.2

    underTest.start();

    ArgumentCaptor<MQRAndStandardModesExistNotification> captor = ArgumentCaptor.forClass(MQRAndStandardModesExistNotification.class);
    verify(notificationManager, times(1)).scheduleForSending(captor.capture());

    assertThat(captor.getValue().isMQRModeEnabled()).isTrue();
  }

  @Test
  void start_whenNewInstance_shouldNotSendNewModesNotification() {
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(true));
    when(migrationHistory.getInitialDbVersion()).thenReturn(-1L); // New instance

    underTest.start();
    verifyNoInteractions(notificationManager);
  }

  @Test
  void start_whenOldInstanceInStandardModeWithMQRConditions_shouldSendQualityGateUpdateNotification() {
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(false));
    when(migrationHistory.getInitialDbVersion()).thenReturn(9999L); // 9.9
    when(metricDao.selectAll(dbSession)).thenReturn(List.of(new MetricDto().setKey(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY).setUuid(METRIC_UUID)));
    when(qualityGateConditionDao.selectAll(dbSession)).thenReturn(List.of(new QualityGateConditionDto().setMetricUuid(METRIC_UUID)));

    underTest.start();

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationManager, times(2)).scheduleForSending(captor.capture());

    assertThat(captor.getAllValues())
      .filteredOn(notification -> notification instanceof QualityGateMetricsUpdateNotification)
      .map(notification -> (QualityGateMetricsUpdateNotification) notification)
      .hasSize(1)
      .extracting(QualityGateMetricsUpdateNotification::isMQRModeEnabled).isEqualTo(List.of(false));

  }

  @Test
  void start_whenOldInstanceInMQRModeWithStandardConditions_shouldSendQualityGateUpdateNotification() {
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(true));
    when(migrationHistory.getInitialDbVersion()).thenReturn(9999L); // 9.9
    when(metricDao.selectAll(dbSession)).thenReturn(List.of(new MetricDto().setKey(CoreMetrics.CODE_SMELLS_KEY).setUuid(METRIC_UUID)));
    when(qualityGateConditionDao.selectAll(dbSession)).thenReturn(List.of(new QualityGateConditionDto().setMetricUuid(METRIC_UUID)));

    underTest.start();

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationManager, times(2)).scheduleForSending(captor.capture());

    assertThat(captor.getAllValues())
      .filteredOn(notification -> notification instanceof QualityGateMetricsUpdateNotification)
      .map(notification -> (QualityGateMetricsUpdateNotification) notification)
      .hasSize(1)
      .extracting(QualityGateMetricsUpdateNotification::isMQRModeEnabled).isEqualTo(List.of(true));
  }

  @Test
  void start_whenOldInstanceInMQRModeWithOtherConditions_shouldNotSendNotification() {
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(true));
    when(migrationHistory.getInitialDbVersion()).thenReturn(9999L); // 9.9
    when(metricDao.selectAll(dbSession)).thenReturn(List.of(
      new MetricDto().setKey(CoreMetrics.COVERAGE_KEY).setUuid(METRIC_UUID),
      new MetricDto().setKey(CoreMetrics.SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY).setUuid(METRIC_UUID_2)));
    when(qualityGateConditionDao.selectAll(dbSession)).thenReturn(List.of(new QualityGateConditionDto().setMetricUuid(METRIC_UUID),
      new QualityGateConditionDto().setMetricUuid(METRIC_UUID_2)));

    underTest.start();

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationManager, times(1)).scheduleForSending(captor.capture());

    assertThat(captor.getAllValues())
      .filteredOn(notification -> notification instanceof QualityGateMetricsUpdateNotification)
      .map(notification -> (QualityGateMetricsUpdateNotification) notification)
      .isEmpty();
  }
}
