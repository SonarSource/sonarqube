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
package org.sonar.ce.task.projectanalysis.history;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDao;
import org.sonar.db.metric.MetricDto;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.model.MeasureHistoryRow;
import org.sonarsource.history.model.MeasureKeyMapping;
import org.sonarsource.history.server.db.HistoryDbClient;
import org.sonarsource.history.server.db.repository.MeasureHistoryRepository;
import org.sonarsource.history.server.db.repository.MeasureKeyMappingRepository;
import org.sonarsource.history.server.service.IssueCountHistoryRecordingService;
import org.sonarsource.history.server.service.MeasuresHistoryRecordingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecordHistoryWriteOrderTest {
  @ParameterizedTest
  @EnumSource(value = EntityType.class, names = {"PORTFOLIO", "APPLICATION"})
  void recordHistory_whenMetricOrderChanges_shouldKeepStableHistoryWriteOrder(EntityType entityType) {
    DbClient core = mock(DbClient.class);
    DbSession coreSession = mock(DbSession.class);
    IssueDao issues = mock(IssueDao.class);
    MeasureDao measures = mock(MeasureDao.class);
    MetricDao metrics = mock(MetricDao.class);
    MeasureDto values = mock(MeasureDto.class);
    when(core.openSession(false)).thenReturn(coreSession);
    when(core.issueDao()).thenReturn(issues);
    when(core.measureDao()).thenReturn(measures);
    when(core.metricDao()).thenReturn(metrics);
    when(issues.selectIssueCountDimensionsForBranches(any(), any())).thenReturn(Map.of());
    when(measures.selectByComponentUuid(coreSession, "portfolio")).thenReturn(Optional.of(values));
    when(metrics.selectByKeys(eq(coreSession), any())).thenReturn(List.of(
      new MetricDto().setKey("ncloc").setValueType("INT"), new MetricDto().setKey("lines").setValueType("INT")));

    HistoryDbClient history = mock(HistoryDbClient.class);
    DbSession historySession = mock(DbSession.class);
    MeasureHistoryRepository rows = mock(MeasureHistoryRepository.class);
    MeasureKeyMappingRepository keys = mock(MeasureKeyMappingRepository.class);
    when(history.openReadWriteSessionWithBatchMode()).thenReturn(historySession);
    when(history.measureHistoryRepository()).thenReturn(rows);
    when(history.measureKeyMappingRepository()).thenReturn(keys);
    when(keys.findByMetricNames(any(), any())).thenReturn(Set.of(
      new MeasureKeyMapping(1, "ncloc", "INT"), new MeasureKeyMapping(17, "lines", "INT")));
    when(rows.findLatestMeasureValuesForEntity(any(), any(), any())).thenReturn(Map.of());
    var recorder = new MeasuresHistoryRecordingService(history, List.of(key -> true));
    var delegate = new RecordHistoryDelegateImpl(core, mock(IssueCountHistoryRecordingService.class), recorder,
      mock(IssueTtrHistoryRecorder.class), mock(ScaTtrHistoryRecorder.class));

    for (boolean reverse : List.of(false, true)) {
      Map<String, Object> input = new LinkedHashMap<>();
      input.put(reverse ? "lines" : "ncloc", reverse ? 20 : 10);
      input.put(reverse ? "ncloc" : "lines", reverse ? 10 : 20);
      when(values.getMetricValues()).thenReturn(input);
      clearInvocations(rows);

      delegate.recordHistory("portfolio", entityType, List.of("branch"));

      var captured = ArgumentCaptor.forClass(MeasureHistoryRow.class);
      verify(rows, times(2)).upsert(eq(historySession), captured.capture());
      assertThat(captured.getAllValues()).extracting(MeasureHistoryRow::metricId).containsExactly(1, 17);
      assertThat(captured.getAllValues()).extracting(MeasureHistoryRow::textValue).containsExactly("10", "20");
      assertThat(captured.getAllValues()).extracting(MeasureHistoryRow::entityType).containsOnly(entityType);
    }
  }
}
