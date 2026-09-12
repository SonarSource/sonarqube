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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.issue.IssueCountDimensionDto;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDao;
import org.sonar.db.metric.MetricDto;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.model.IssueCountDimensionKey;
import org.sonarsource.history.model.Measure;
import org.sonarsource.history.server.service.IssueCountHistoryRecordingService;
import org.sonarsource.history.server.service.MeasuresHistoryRecordingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecordHistoryDelegateImplTest {

  private static final String ENTITY_UUID = "entity-uuid";

  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final BranchDao branchDao = mock(BranchDao.class);
  private final IssueDao issueDao = mock(IssueDao.class);
  private final MeasureDao measureDao = mock(MeasureDao.class);
  private final MetricDao metricDao = mock(MetricDao.class);
  private final IssueCountHistoryRecordingService issueHistoryService = mock(IssueCountHistoryRecordingService.class);
  private final MeasuresHistoryRecordingService measuresHistoryService = mock(MeasuresHistoryRecordingService.class);
  private final IssueTtrHistoryRecorder issueTtrHistoryRecorder = mock();
  private final ScaTtrHistoryRecorder scaTtrHistoryRecorder = mock();
  private final RecordHistoryDelegateImpl underTest = new RecordHistoryDelegateImpl(
    dbClient, issueHistoryService, measuresHistoryService, issueTtrHistoryRecorder, scaTtrHistoryRecorder);

  @BeforeEach
  void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(branchDao.lockForIssueCountHistory(dbSession, ENTITY_UUID)).thenReturn(true);
    when(dbClient.issueDao()).thenReturn(issueDao);
    when(dbClient.measureDao()).thenReturn(measureDao);
    when(dbClient.metricDao()).thenReturn(metricDao);
    when(measureDao.selectByComponentUuid(dbSession, ENTITY_UUID)).thenReturn(Optional.empty());
  }

  @Test
  void branch_history_holds_lock_until_snapshot_is_recorded() {
    DbSession measuresSession = mock(DbSession.class);
    when(dbClient.openSession(false)).thenReturn(dbSession, measuresSession);
    recordBranchHistory();

    var order = inOrder(branchDao, issueDao, issueHistoryService, dbSession);
    order.verify(branchDao).lockForIssueCountHistory(dbSession, ENTITY_UUID);
    order.verify(issueDao).selectIssueCountDimensionsForBranches(dbSession, List.of(ENTITY_UUID));
    order.verify(issueHistoryService).recordIssueHistory(eq(ENTITY_UUID), eq(EntityType.PROJECT_BRANCH), any(), any());
    order.verify(dbSession).close();
  }

  @Test
  void recordHistory_shouldMapDimensionRowFieldsIntoDimensionKeyAndCount() {
    givenDimensionRows(ENTITY_UUID, new IssueCountDimensionDto(
      2, "CRITICAL", "FALSE_POSITIVE", "RESOLVED", null, "TEST", "java:S1234", null, "HIGH", "MEDIUM", 3));

    recordBranchHistory();

    Map<IssueCountDimensionKey, Integer> counts = capturedIssueCounts(EntityType.PROJECT_BRANCH);
    assertThat(counts).hasSize(1);
    Map.Entry<IssueCountDimensionKey, Integer> entry = counts.entrySet().iterator().next();
    IssueCountDimensionKey key = entry.getKey();
    assertThat(key.issueType()).isEqualTo(2);
    assertThat(key.issueSeverity()).isEqualTo("CRITICAL");
    assertThat(key.issueStatus()).isEqualTo("FALSE_POSITIVE");
    assertThat(key.status()).isEqualTo("RESOLVED");
    assertThat(key.issueCodeScope()).isEqualTo("TEST");
    assertThat(key.ruleKey()).isEqualTo("java:S1234");
    assertThat(key.hotspotResolution()).isNull();
    assertThat(key.maintainabilityRating()).isZero();
    assertThat(key.securityRating()).isEqualTo((short) 4);
    assertThat(key.reliabilityRating()).isEqualTo((short) 3);
    assertThat(entry.getValue()).isEqualTo(3);
  }

  @ParameterizedTest
  @MethodSource("historyEntityTypes")
  void recordHistory_shouldSkipSecurityHotspotsAndKeepRegularIssues(EntityType entityType) {
    IssueCountDimensionDto regularIssue = defaultDimensionRow().withIssueCount(1);
    IssueCountDimensionDto hotspot = new IssueCountDimensionDto(
      RuleType.SECURITY_HOTSPOT.getDbConstant(), "MAJOR", null, "REVIEWED", "FIXED", "MAIN", "java:S9999", null, null, null, 1);
    givenDimensionRows(ENTITY_UUID, regularIssue, hotspot);

    underTest.recordHistory(ENTITY_UUID, entityType, List.of(ENTITY_UUID));

    Map<IssueCountDimensionKey, Integer> counts = capturedIssueCounts(entityType);
    assertThat(counts).hasSize(1);
    assertThat(counts.keySet()).extracting(IssueCountDimensionKey::issueType).containsExactly(1);
  }

  @Test
  void recordHistory_shouldRecordIssueAndMeasureHistory() {
    givenDimensionRows(ENTITY_UUID, defaultDimensionRow());
    MeasureDto measureDto = new MeasureDto()
      .addValue("ncloc", 42.0)
      .addValue("coverage", 85.5)
      .addValue("alert_status", "OK")
      .addValue("missing_value", null);
    when(measureDao.selectByComponentUuid(dbSession, ENTITY_UUID)).thenReturn(Optional.of(measureDto));
    when(metricDao.selectByKeys(eq(dbSession), any())).thenReturn(List.of(
      new MetricDto().setKey("ncloc").setValueType("INT"),
      new MetricDto().setKey("coverage").setValueType("FLOAT"),
      new MetricDto().setKey("alert_status").setValueType("LEVEL")));

    recordBranchHistory();

    assertThat(capturedIssueCounts(EntityType.PROJECT_BRANCH)).isNotEmpty();
    assertThat(capturedMeasures()).containsExactly(
      new Measure("alert_status", "LEVEL", "OK"),
      new Measure("coverage", "FLOAT", "85.5"),
      new Measure("ncloc", "INT", "42.0"));
  }

  @Test
  void recordHistory_whenApplication_shouldMergeSameDimensionCountsAcrossBranches() {
    givenDimensionRows(List.of("branch-1", "branch-2"),
      defaultDimensionRow().withIssueCount(2), defaultDimensionRow().withIssueCount(3));
    MeasureDto measureDto = new MeasureDto().addValue("ncloc", 84.0);
    when(measureDao.selectByComponentUuid(dbSession, ENTITY_UUID)).thenReturn(Optional.of(measureDto));
    when(metricDao.selectByKeys(eq(dbSession), any())).thenReturn(List.of(
      new MetricDto().setKey("ncloc").setValueType("INT")));

    underTest.recordHistory(ENTITY_UUID, EntityType.APPLICATION, List.of("branch-1", "branch-2"));

    Map<IssueCountDimensionKey, Integer> counts = capturedIssueCounts(EntityType.APPLICATION);
    assertThat(counts.values()).containsExactly(5);

    ArgumentCaptor<List<Measure>> measuresCaptor = ArgumentCaptor.forClass(List.class);
    verify(measuresHistoryService).recordMeasureHistory(
      eq(ENTITY_UUID), eq(EntityType.APPLICATION), measuresCaptor.capture(), any(LocalDate.class));
    assertThat(measuresCaptor.getValue()).containsExactly(new Measure("ncloc", "INT", "84.0"));
    verify(scaTtrHistoryRecorder).recordTtrHistory(ENTITY_UUID, EntityType.APPLICATION);
  }

  @Test
  void recordHistory_whenApplication_shouldKeepDistinctBranchDimensionsSeparate() {
    givenDimensionRows(List.of("branch-1", "branch-2"),
      defaultDimensionRow().withCodeScope("MAIN"), defaultDimensionRow().withCodeScope("TEST").withSeverity("CRITICAL"));

    underTest.recordHistory(ENTITY_UUID, EntityType.APPLICATION, List.of("branch-1", "branch-2"));

    Map<IssueCountDimensionKey, Integer> counts = capturedIssueCounts(EntityType.APPLICATION);
    assertThat(counts.keySet())
      .extracting(IssueCountDimensionKey::issueCodeScope, IssueCountDimensionKey::issueSeverity)
      .containsExactlyInAnyOrder(
        tuple("MAIN", "MAJOR"),
        tuple("TEST", "CRITICAL"));
    verify(issueDao).selectIssueCountDimensionsForBranches(dbSession, List.of("branch-1", "branch-2"));
  }

  @Test
  void recordHistory_whenPortfolio_shouldRecordScaTtrHistoryForAggregation() {
    givenDimensionRows(ENTITY_UUID, defaultDimensionRow());

    underTest.recordHistory(ENTITY_UUID, EntityType.PORTFOLIO, List.of(ENTITY_UUID));

    verify(scaTtrHistoryRecorder).recordTtrHistory(ENTITY_UUID, EntityType.PORTFOLIO);
  }

  @Test
  void recordHistory_whenScaTtrHistoryRecorderIsMissing_shouldNotFail() {
    givenDimensionRows(ENTITY_UUID, defaultDimensionRow());
    RecordHistoryDelegateImpl delegateWithoutScaTtrHistoryRecorder = new RecordHistoryDelegateImpl(
      dbClient, issueHistoryService, measuresHistoryService, issueTtrHistoryRecorder, null);

    assertThatCode(() -> delegateWithoutScaTtrHistoryRecorder.recordHistory(
      ENTITY_UUID, EntityType.PORTFOLIO, List.of(ENTITY_UUID)))
      .doesNotThrowAnyException();
  }

  @Test
  void recordHistory_whenProjectBranch_shouldRecordScaTtrHistory() {
    givenDimensionRows(ENTITY_UUID, defaultDimensionRow());

    recordBranchHistory();

    verify(scaTtrHistoryRecorder).recordTtrHistory(ENTITY_UUID, EntityType.PROJECT_BRANCH);
  }

  @Test
  void recordHistory_shouldRecordMeasureHistoryWhenMetricValueTypeIsNull() {
    givenDimensionRows(ENTITY_UUID, defaultDimensionRow());
    MeasureDto measureDto = new MeasureDto().addValue("ncloc", 42.0);
    when(measureDao.selectByComponentUuid(dbSession, ENTITY_UUID)).thenReturn(Optional.of(measureDto));
    when(metricDao.selectByKeys(eq(dbSession), any())).thenReturn(List.of(
      new MetricDto().setKey("ncloc").setValueType(null)));

    recordBranchHistory();

    assertThat(capturedMeasures()).containsExactly(new Measure("ncloc", null, "42.0"));
  }

  @Test
  void recordHistory_shouldNotRecordMeasureHistoryWhenMeasureDataIsMissing() {
    givenDimensionRows(ENTITY_UUID, defaultDimensionRow());

    recordBranchHistory();

    assertThat(capturedIssueCounts(EntityType.PROJECT_BRANCH)).isNotEmpty();
    verifyNoInteractions(measuresHistoryService);
  }

  @Test
  void recordHistory_shouldNotRecordMeasureHistoryWhenMetricValuesAreEmpty() {
    givenDimensionRows(ENTITY_UUID, defaultDimensionRow());
    when(measureDao.selectByComponentUuid(dbSession, ENTITY_UUID)).thenReturn(Optional.of(new MeasureDto()));

    recordBranchHistory();

    assertThat(capturedIssueCounts(EntityType.PROJECT_BRANCH)).isNotEmpty();
    verifyNoInteractions(measuresHistoryService);
  }

  @Test
  void recordHistory_shouldRecordIssueTtrHistory() {
    givenDimensionRows(ENTITY_UUID, defaultDimensionRow());

    underTest.recordHistory(ENTITY_UUID, EntityType.PROJECT_BRANCH, List.of(ENTITY_UUID));

    verify(issueTtrHistoryRecorder).recordTtrHistory(ENTITY_UUID);
  }

  private static IssueCountDimensionDto defaultDimensionRow() {
    return new IssueCountDimensionDto(1, "MAJOR", "OPEN", "OPEN", null, "MAIN", "java:S1234", null, null, null, 0);
  }

  private void recordBranchHistory() {
    underTest.recordHistory(ENTITY_UUID, EntityType.PROJECT_BRANCH, List.of(ENTITY_UUID));
  }

  private void givenDimensionRows(String branchUuid, IssueCountDimensionDto... rows) {
    givenDimensionRows(List.of(branchUuid), rows);
  }

  private void givenDimensionRows(Collection<String> branchUuids, IssueCountDimensionDto... rows) {
    when(issueDao.selectIssueCountDimensionsForBranches(dbSession, branchUuids)).thenReturn(List.of(rows));
  }

  private Map<IssueCountDimensionKey, Integer> capturedIssueCounts(EntityType entityType) {
    ArgumentCaptor<Map<IssueCountDimensionKey, Integer>> issueCountsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(issueHistoryService).recordIssueHistory(
      eq(ENTITY_UUID), eq(entityType), issueCountsCaptor.capture(), any(LocalDate.class));
    return issueCountsCaptor.getValue();
  }

  private static Stream<EntityType> historyEntityTypes() {
    return Stream.of(EntityType.PROJECT_BRANCH, EntityType.APPLICATION, EntityType.PORTFOLIO);
  }

  private List<Measure> capturedMeasures() {
    ArgumentCaptor<List<Measure>> measuresCaptor = ArgumentCaptor.forClass(List.class);
    verify(measuresHistoryService).recordMeasureHistory(
      eq(ENTITY_UUID), eq(EntityType.PROJECT_BRANCH), measuresCaptor.capture(), any(LocalDate.class));
    return measuresCaptor.getValue();
  }
}
