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
package org.sonar.server.notification.email.telemetry;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.report.ReportSubscriptionDao;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryProjectSubscriptionsProviderTest {
  private final DbClient dbClient = mock(DbClient.class);
  private final ReportSubscriptionDao reportSubscriptionDao = mock(ReportSubscriptionDao.class);

  @Test
  void testGetters() {
    when(dbClient.reportSubscriptionDao()).thenReturn(reportSubscriptionDao);
    DbSession session = mock(DbSession.class);
    when(dbClient.openSession(false)).thenReturn(session);
    TelemetryProjectSubscriptionsProvider underTest = new TelemetryProjectSubscriptionsProvider(dbClient);

    assertThat(underTest.getMetricKey()).isEqualTo("project_report_pdf_subscriptions");
    assertThat(underTest.getDimension()).isEqualTo(Dimension.PROJECT);
    assertThat(underTest.getGranularity()).isEqualTo(Granularity.DAILY);
    Map<String, Integer> expected = Map.of("prj1", 2, "prj2", 3);
    when(reportSubscriptionDao.countPerProject(any())).thenReturn(expected);
    assertThat(underTest.getValues()).isEqualTo(expected);
  }
}
