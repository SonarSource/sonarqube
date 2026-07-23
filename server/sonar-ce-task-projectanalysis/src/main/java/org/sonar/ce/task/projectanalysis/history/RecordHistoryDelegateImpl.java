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

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.ibatis.cursor.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.ce.task.projectanalysis.issue.RuleRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.issue.IndexedIssueDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarsource.history.model.Impact;
import org.sonarsource.history.model.IssueDtoForHistory;
import org.sonarsource.history.model.Measure;
import org.sonarsource.history.server.service.IssueCountHistoryRecordingService;
import org.sonarsource.history.server.service.MeasuresHistoryRecordingService;

/**
 * Records issue-count history and measure history for the analysed branch
 * by reading current data from the SonarQube DB and forwarding it to the History services.
 */
@ComputeEngineSide
public class RecordHistoryDelegateImpl implements RecordHistoryDelegate {

  private static final Logger LOG = LoggerFactory.getLogger(RecordHistoryDelegateImpl.class);
  private static final String CLOSED_STATUS = "CLOSED";

  private final DbClient dbClient;
  private final IssueCountHistoryRecordingService issueHistoryService;
  private final MeasuresHistoryRecordingService measuresHistoryService;
  private final RuleRepository ruleRepository;

  public RecordHistoryDelegateImpl(
    DbClient dbClient,
    IssueCountHistoryRecordingService issueHistoryService,
    MeasuresHistoryRecordingService measuresHistoryService,
    RuleRepository ruleRepository) {
    this.dbClient = dbClient;
    this.issueHistoryService = issueHistoryService;
    this.measuresHistoryService = measuresHistoryService;
    this.ruleRepository = ruleRepository;
  }

  @Override
  public void recordHistory(String entityUuid) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    LOG.info("Recording History for entity {} on {}", entityUuid, today);
    recordIssueHistory(entityUuid, today);
    recordMeasureHistory(entityUuid, today);
    LOG.info("History recording complete for entity {}", entityUuid);
  }

  // -------------------------------------------------------------------------
  // Issue history
  // -------------------------------------------------------------------------

  private void recordIssueHistory(String entityUuid, LocalDate today) {
    List<IssueDtoForHistory> issues = fetchIssues(entityUuid);
    issueHistoryService.recordIssueHistoryForBranch(entityUuid, issues, today);
  }

  private List<IssueDtoForHistory> fetchIssues(String entityUuid) {
    List<IssueDtoForHistory> result = new ArrayList<>();
    try (DbSession dbSession = dbClient.openSession(false)) {
      try (Cursor<IndexedIssueDto> cursor = dbClient.issueDao().scrollIssuesForIndexation(dbSession, entityUuid, null)) {
        for (IndexedIssueDto indexed : cursor) {
          if (CLOSED_STATUS.equals(indexed.getStatus())) {
            continue;
          }
          IssueDtoForHistory dto = toIssueDtoForHistory(indexed);
          if (dto != null) {
            result.add(dto);
          }
        }
      } catch (IOException e) {
        throw new IllegalStateException("Failed to close issue cursor for entity " + entityUuid, e);
      }
    }
    return result;
  }

  private IssueDtoForHistory toIssueDtoForHistory(IndexedIssueDto indexed) {
    String ruleKeyString;
    try {
      ruleKeyString = ruleRepository.findByUuid(indexed.getRuleUuid())
        .map(rule -> rule.getKey().toString())
        .orElseGet(() -> {
          LOG.warn("Rule not found for UUID '{}' on issue '{}'; skipping issue.", indexed.getRuleUuid(), indexed.getIssueKey());
          return null;
        });
    } catch (Exception e) {
      LOG.warn("Failed to resolve rule UUID '{}' for issue '{}'; skipping issue.", indexed.getRuleUuid(), indexed.getIssueKey(), e);
      return null;
    }
    if (ruleKeyString == null) {
      return null;
    }

    List<Impact> overriddenImpacts = indexed.getImpacts().stream()
      .map(RecordHistoryDelegateImpl::toImpact)
      .toList();

    List<Impact> ruleDefaultImpacts = indexed.getRuleDefaultImpacts().stream()
      .map(RecordHistoryDelegateImpl::toImpact)
      .toList();

    return new IssueDtoForHistory(
      indexed.getIssueType() != null ? indexed.getIssueType() : 0,
      indexed.getSeverity(),
      indexed.getIssueStatus(),
      indexed.getStatus(),
      indexed.getResolution(),
      ComponentQualifiers.UNIT_TEST_FILE.equals(indexed.getQualifier()) ? "TEST" : "MAIN",
      ruleKeyString,
      overriddenImpacts,
      ruleDefaultImpacts);
  }

  private static Impact toImpact(ImpactDto dto) {
    return new Impact(
      dto.getSoftwareQuality() != null ? dto.getSoftwareQuality().name() : null,
      dto.getSeverity() != null ? dto.getSeverity().name() : null);
  }

  // -------------------------------------------------------------------------
  // Measure history
  // -------------------------------------------------------------------------

  private void recordMeasureHistory(String entityUuid, LocalDate today) {
    List<Measure> measures = fetchMeasures(entityUuid);
    if (!measures.isEmpty()) {
      measuresHistoryService.recordMeasureHistoryForBranch(entityUuid, measures, today);
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
