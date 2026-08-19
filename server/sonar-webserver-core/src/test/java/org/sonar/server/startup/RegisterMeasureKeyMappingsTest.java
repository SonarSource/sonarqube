/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.startup;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDao;
import org.sonar.db.metric.MetricDto;
import org.sonarsource.history.server.db.HistoryDbClient;
import org.sonarsource.history.server.db.repository.MeasureKeyMappingRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterMeasureKeyMappingsTest {

  private final DbClient dbClient = mock(DbClient.class);

  private final HistoryDbClient historyDbClient = mock(HistoryDbClient.class);

  private final MeasureKeyMappingRepository measureKeyMappingRepository = mock(MeasureKeyMappingRepository.class);

  private final MetricDao metricDao = mock(MetricDao.class);

  private final DbSession dbSession = mock(DbSession.class);

  @Test
  void start_should_register_all_metrics_with_their_types() {
    MetricDto enabledMetric = new MetricDto()
      .setKey("enabled_metric")
      .setValueType("INT")
      .setEnabled(true);
    MetricDto disabledMetric = new MetricDto()
      .setKey("disabled_metric")
      .setValueType("STRING")
      .setEnabled(false);
    MetricDto metricWithoutType = new MetricDto()
      .setKey("metric_without_type")
      .setEnabled(true);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.metricDao()).thenReturn(metricDao);
    when(metricDao.selectAll(dbSession)).thenReturn(List.of(enabledMetric, disabledMetric, metricWithoutType));
    when(historyDbClient.measureKeyMappingRepository()).thenReturn(measureKeyMappingRepository);

    new RegisterMeasureKeyMappings(dbClient, historyDbClient).start();

    verify(measureKeyMappingRepository).getOrCreate(dbSession, Map.of(
      "enabled_metric", "INT",
      "disabled_metric", "STRING",
      "metric_without_type", "STRING"));
    verify(dbSession).commit();
  }
}
