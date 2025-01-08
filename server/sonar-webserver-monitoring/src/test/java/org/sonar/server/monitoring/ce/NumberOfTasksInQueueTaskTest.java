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
package org.sonar.server.monitoring.ce;

import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.db.DbClient;
import org.sonar.db.ce.CeQueueDao;
import org.sonar.server.monitoring.ServerMonitoringMetrics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NumberOfTasksInQueueTaskTest {

  private final DbClient dbClient = mock(DbClient.class);
  private final CeQueueDao ceQueueDao = mock(CeQueueDao.class);
  private final ServerMonitoringMetrics metrics = mock(ServerMonitoringMetrics.class);
  private final Configuration config = mock(Configuration.class);

  @Test
  public void run_setsValueInMetricsBasedOnValueReturnedFromDatabase() {
    NumberOfTasksInQueueTask task = new NumberOfTasksInQueueTask(dbClient, metrics, config);
    when(dbClient.ceQueueDao()).thenReturn(ceQueueDao);
    when(ceQueueDao.countByStatus(any(), any())).thenReturn(10);

    task.run();

    verify(metrics, times(1)).setNumberOfPendingTasks(10);
  }
}
