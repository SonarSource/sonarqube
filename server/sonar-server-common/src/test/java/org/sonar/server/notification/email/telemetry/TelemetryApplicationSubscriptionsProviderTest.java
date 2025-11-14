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
package org.sonar.server.notification.email.telemetry;

import org.junit.jupiter.api.Test;
import org.sonar.db.DbClient;
import org.sonar.db.report.ReportSubscriptionDao;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryApplicationSubscriptionsProviderTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final ReportSubscriptionDao reportSubscriptionDao = mock(ReportSubscriptionDao.class);

  @Test
  void testGetters() {
    when(dbClient.reportSubscriptionDao()).thenReturn(reportSubscriptionDao);
    TelemetryApplicationSubscriptionsProvider underTest = new TelemetryApplicationSubscriptionsProvider(dbClient);

    assertThat(underTest.getMetricKey()).isEqualTo("application_report_pdf_subscriptions");
    assertThat(underTest.getDimension()).isEqualTo(Dimension.INSTALLATION);
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.DAILY);
    when(reportSubscriptionDao.countByQualifier(any(), any())).thenReturn(42);
    assertThat(underTest.getValue()).contains(42);
    when(reportSubscriptionDao.countByQualifier(any(), any())).thenReturn(0);
    assertThat(underTest.getValue()).contains(0);
  }
}
