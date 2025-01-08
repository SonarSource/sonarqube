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
package org.sonar.server.issue.notification;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.notifications.Notification;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.qualitygate.QualityGateConditionsValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;

class NewModesNotificationsSenderTest {
  private final NotificationManager notificationManager = mock(NotificationManager.class);
  private final Configuration configuration = mock(Configuration.class);
  private final MigrationHistory migrationHistory = mock(MigrationHistory.class);

  private final QualityGateConditionsValidator qualityGateConditionsValidator = mock(QualityGateConditionsValidator.class);
  private final NewModesNotificationsSender underTest = new NewModesNotificationsSender(notificationManager, configuration, migrationHistory, qualityGateConditionsValidator);

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
  void start_whenOldInstanceAndConditionsMismatch_shouldSendQualityGateUpdateNotification() {
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(false));
    when(migrationHistory.getInitialDbVersion()).thenReturn(9999L); // 9.9
    when(qualityGateConditionsValidator.hasConditionsMismatch(false)).thenReturn(true);

    underTest.start();

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationManager, times(2)).scheduleForSending(captor.capture());

    assertThat(captor.getAllValues())
      .filteredOn(
        QualityGateMetricsUpdateNotification.class::isInstance)
      .map(
        QualityGateMetricsUpdateNotification.class::cast)
      .hasSize(1)
      .extracting(QualityGateMetricsUpdateNotification::isMQRModeEnabled).isEqualTo(List.of(false));
  }

  @Test
  void start_whenOldInstanceAndNoConditionsMismatch_shouldNotSendQualityGateUpdateNotification() {
    when(configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED)).thenReturn(Optional.of(false));
    when(migrationHistory.getInitialDbVersion()).thenReturn(9999L); // 9.9
    when(qualityGateConditionsValidator.hasConditionsMismatch(false)).thenReturn(false);

    underTest.start();

    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationManager, times(1)).scheduleForSending(captor.capture());

    assertThat(captor.getAllValues())
      .filteredOn(
        QualityGateMetricsUpdateNotification.class::isInstance)
      .map(QualityGateMetricsUpdateNotification.class::cast)
      .isEmpty();
  }
}
