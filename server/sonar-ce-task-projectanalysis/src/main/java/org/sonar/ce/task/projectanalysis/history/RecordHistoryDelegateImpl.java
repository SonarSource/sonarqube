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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueCountDimensionDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.model.IssueCountDimensionKey;
import org.sonarsource.history.model.Measure;
import org.sonarsource.history.server.service.IssueCountHistoryRecordingService;
import org.sonarsource.history.server.service.MeasuresHistoryRecordingService;

/**
 * Records issue-count history and measure history for the analysed entity
 * by reading current data from the SonarQube DB and forwarding it to the History services.
 */
@ComputeEngineSide
public class RecordHistoryDelegateImpl implements RecordHistoryDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(RecordHistoryDelegateImpl.class);
  private static final ScaTtrHistoryRecorder NO_OP_SCA_TTR_HISTORY_RECORDER = (entityUuid, entityType) -> {
    // SCA history is available only when the private SCA extension provides a recorder.
  };

  private final DbClient dbClient;
  private final IssueCountHistoryRecordingService issueHistoryService;
  private final MeasuresHistoryRecordingService measuresHistoryService;
  private final IssueTtrHistoryRecorder issueTtrHistoryRecorder;
  private final ScaTtrHistoryRecorder scaTtrHistoryRecorder;

  public RecordHistoryDelegateImpl(
    DbClient dbClient,
    IssueCountHistoryRecordingService issueHistoryService,
    MeasuresHistoryRecordingService measuresHistoryService,
    IssueTtrHistoryRecorder issueTtrHistoryRecorder,
    @Nullable ScaTtrHistoryRecorder scaTtrHistoryRecorder) {
    this.dbClient = dbClient;
    this.issueHistoryService = issueHistoryService;
    this.measuresHistoryService = measuresHistoryService;
    this.issueTtrHistoryRecorder = issueTtrHistoryRecorder;
    this.scaTtrHistoryRecorder = scaTtrHistoryRecorder == null ? NO_OP_SCA_TTR_HISTORY_RECORDER : scaTtrHistoryRecorder;
  }

  @Override
  public void recordHistory(String entityUuid, EntityType entityType, Collection<String> issueSourceBranchUuids) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    LOG.info("Recording History for {} {} on {}", entityType, entityUuid, today);
    recordIssueHistory(entityUuid, entityType, issueSourceBranchUuids, today);
    recordMeasureHistory(entityUuid, entityType, today);
    issueTtrHistoryRecorder.recordTtrHistory(entityUuid);
    recordScaTtrHistory(entityUuid, entityType);
    LOG.info("History recording complete for {} {}", entityType, entityUuid);
  }

  private void recordScaTtrHistory(String entityUuid, EntityType entityType) {
    scaTtrHistoryRecorder.recordTtrHistory(entityUuid, entityType);
  }

  // -------------------------------------------------------------------------
  // Issue history
  // -------------------------------------------------------------------------

  private void recordIssueHistory(String entityUuid, EntityType entityType, Collection<String> issueSourceBranchUuids, LocalDate today) {
    Map<IssueCountDimensionKey, Integer> issueCounts = fetchIssueCounts(issueSourceBranchUuids);
    issueHistoryService.recordIssueHistory(entityUuid, entityType, issueCounts, today);
  }

  /**
   * Merges the pre-aggregated dimension counts into a single map, summing counts for dimensions shared
   * across branches (e.g. when recording history for an application) or across DB batches (branches are
   * queried in batches of at most 1000 due to DB parameter limits). The grouping itself is done in SQL,
   * so no individual issue is ever loaded into memory here (SONAR-31731).
   */
  private Map<IssueCountDimensionKey, Integer> fetchIssueCounts(Collection<String> issueSourceBranchUuids) {
    Map<IssueCountDimensionKey, Integer> issueCounts = new HashMap<>();
    try (DbSession dbSession = dbClient.openSession(false)) {
      for (IssueCountDimensionDto row : dbClient.issueDao().selectIssueCountDimensionsForBranches(dbSession, issueSourceBranchUuids)) {
        issueCounts.merge(toIssueCountDimensionKey(row), row.issueCount(), Integer::sum);
      }
    }
    return issueCounts;
  }

  private static IssueCountDimensionKey toIssueCountDimensionKey(IssueCountDimensionDto row) {
    return new IssueCountDimensionKey(
      row.hotspotResolution(),
      row.codeScope(),
      row.severity(),
      row.issueStatus(),
      row.status(),
      row.issueType(),
      row.ruleKey(),
      row.effectiveImpacts());
  }

  // -------------------------------------------------------------------------
  // Measure history
  // -------------------------------------------------------------------------

  private void recordMeasureHistory(String entityUuid, EntityType entityType, LocalDate today) {
    List<Measure> measures = fetchMeasures(entityUuid);
    if (!measures.isEmpty()) {
      measuresHistoryService.recordMeasureHistory(entityUuid, entityType, measures, today);
    }
  }

  private List<Measure> fetchMeasures(String entityUuid) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<MeasureDto> measureDtoOpt = dbClient.measureDao().selectByComponentUuid(dbSession, entityUuid);
      if (measureDtoOpt.isEmpty()) {
        return List.of();
      }
      MeasureDto measureDto = measureDtoOpt.get();
      Map<String, Object> metricValues = measureDto.getMetricValues();
      if (metricValues.isEmpty()) {
        return List.of();
      }

      // Fetch metric types for all keys present in this MeasureDto
      List<MetricDto> metricDtos = dbClient.metricDao().selectByKeys(dbSession, metricValues.keySet());
      Map<String, String> metricTypes = new HashMap<>();
      for (MetricDto metric : metricDtos) {
        metricTypes.put(metric.getKey(), metric.getValueType());
      }

      List<Measure> measures = new ArrayList<>(metricValues.size());
      for (Map.Entry<String, Object> entry : metricValues.entrySet()) {
        String metricKey = entry.getKey();
        Object value = entry.getValue();
        if (value == null) {
          continue;
        }
        String textValue = String.valueOf(value);
        String metricType = metricTypes.get(metricKey);
        measures.add(new Measure(metricKey, metricType, textValue));
      }
      return measures;
    }
  }
}
