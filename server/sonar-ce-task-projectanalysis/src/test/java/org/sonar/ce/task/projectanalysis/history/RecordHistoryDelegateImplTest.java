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
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.cursor.Cursor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.issue.Rule;
import org.sonar.ce.task.projectanalysis.issue.RuleRepository;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.issue.IndexedIssueDto;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDao;
import org.sonar.db.metric.MetricDto;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.model.Impact;
import org.sonarsource.history.model.IssueDtoForHistory;
import org.sonarsource.history.model.Measure;
import org.sonarsource.history.server.service.IssueCountHistoryRecordingService;
import org.sonarsource.history.server.service.MeasuresHistoryRecordingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecordHistoryDelegateImplTest {

  private static final String ENTITY_UUID = "entity-uuid";
  private static final String RULE_UUID = "rule-uuid";

  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final IssueDao issueDao = mock(IssueDao.class);
  private final MeasureDao measureDao = mock(MeasureDao.class);
  private final MetricDao metricDao = mock(MetricDao.class);
  private final IssueCountHistoryRecordingService issueHistoryService = mock(IssueCountHistoryRecordingService.class);
  private final MeasuresHistoryRecordingService measuresHistoryService = mock(MeasuresHistoryRecordingService.class);
  private final RuleRepository ruleRepository = mock(RuleRepository.class);
  private final IssueTtrHistoryRecorder issueTtrHistoryRecorder = mock();
  private final Rule rule = mock(Rule.class);
  private final RecordHistoryDelegateImpl underTest = new RecordHistoryDelegateImpl(
    dbClient, issueHistoryService, measuresHistoryService, ruleRepository, issueTtrHistoryRecorder);

  @BeforeEach
  void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.issueDao()).thenReturn(issueDao);
    when(dbClient.measureDao()).thenReturn(measureDao);
    when(dbClient.metricDao()).thenReturn(metricDao);
    when(measureDao.selectByComponentUuid(dbSession, ENTITY_UUID)).thenReturn(Optional.empty());
    when(ruleRepository.findByUuid(RULE_UUID)).thenReturn(Optional.of(rule));
    when(rule.getKey()).thenReturn(RuleKey.of("java", "S1234"));
  }

  @Test
  void recordHistory_shouldMapNonUnitTestQualifierToMainCodeScope() {
    IndexedIssueDto issue = issueWithQualifier(ComponentQualifiers.FILE).setCodeVariants("TEST");
    givenIssueCursor(issue);

    recordBranchHistory();

    assertThat(capturedIssue().getCodeScope()).isEqualTo("MAIN");
  }

  @Test
  void recordHistory_shouldMapUnitTestQualifierToTestCodeScope() {
    IndexedIssueDto issue = issueWithQualifier(ComponentQualifiers.UNIT_TEST_FILE).setCodeVariants("MAIN");
    givenIssueCursor(issue);

    recordBranchHistory();

    assertThat(capturedIssue().getCodeScope()).isEqualTo("TEST");
  }

  @Test
  void recordHistory_shouldSkipClosedIssues() {
    IndexedIssueDto closedIssue = issueWithQualifier(ComponentQualifiers.FILE).setStatus("CLOSED");
    givenIssueCursor(closedIssue);

    recordBranchHistory();

    assertThat(capturedIssues()).isEmpty();
  }

  @Test
  void recordHistory_whenIssueCursorCannotClose_shouldIdentifyIssueSource() throws IOException {
    Cursor<IndexedIssueDto> cursor = mock(Cursor.class);
    when(cursor.iterator()).thenReturn(List.<IndexedIssueDto>of().iterator());
    when(issueDao.scrollIssuesForIndexation(dbSession, ENTITY_UUID, null)).thenReturn(cursor);
    doThrow(new IOException("close failure")).when(cursor).close();

    assertThatThrownBy(this::recordBranchHistory)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to close issue cursor for entity " + ENTITY_UUID);
  }

  @Test
  void recordHistory_shouldSkipIssuesWithMissingRules() {
    IndexedIssueDto issue = issueWithQualifier(ComponentQualifiers.FILE).setRuleUuid("missing-rule-uuid");
    givenIssueCursor(issue);
    when(ruleRepository.findByUuid("missing-rule-uuid")).thenReturn(Optional.empty());

    recordBranchHistory();

    assertThat(capturedIssues()).isEmpty();
  }

  @Test
  void recordHistory_shouldMapIssueFieldsAndImpacts() {
    IndexedIssueDto issue = issueWithQualifier(ComponentQualifiers.UNIT_TEST_FILE)
      .setIssueType(2)
      .setSeverity("CRITICAL")
      .setStatus("RESOLVED")
      .setResolution("FALSE-POSITIVE");
    issue.getImpacts().add(new ImpactDto(SoftwareQuality.SECURITY, Severity.HIGH));
    issue.getRuleDefaultImpacts().add(new ImpactDto(SoftwareQuality.RELIABILITY, Severity.MEDIUM));
    givenIssueCursor(issue);

    recordBranchHistory();

    IssueDtoForHistory mappedIssue = capturedIssue();
    assertThat(mappedIssue.getType()).isEqualTo(2);
    assertThat(mappedIssue.getSeverity()).isEqualTo("CRITICAL");
    assertThat(mappedIssue.getIssueStatus()).isEqualTo("FALSE_POSITIVE");
    assertThat(mappedIssue.getStatus()).isEqualTo("RESOLVED");
    assertThat(mappedIssue.getResolution()).isEqualTo("FALSE-POSITIVE");
    assertThat(mappedIssue.getCodeScope()).isEqualTo("TEST");
    assertThat(mappedIssue.getRuleKey()).isEqualTo("java:S1234");
    assertThat(mappedIssue.getOverriddenImpacts())
      .extracting(Impact::getSoftwareQuality, Impact::getSeverity)
      .containsExactly(tuple("SECURITY", "HIGH"));
    assertThat(mappedIssue.getRuleDefaultImpacts())
      .extracting(Impact::getSoftwareQuality, Impact::getSeverity)
      .containsExactly(tuple("RELIABILITY", "MEDIUM"));
  }

  @Test
  void recordHistory_shouldRecordIssueAndMeasureHistory() {
    givenIssueCursor(issueWithQualifier(ComponentQualifiers.FILE));
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

    assertThat(capturedIssue()).isNotNull();
    assertThat(capturedMeasures()).containsExactly(
      new Measure("alert_status", "LEVEL", "OK"),
      new Measure("coverage", "FLOAT", "85.5"),
      new Measure("ncloc", "INT", "42.0"));
  }

  @Test
  void recordHistory_whenApplication_shouldAggregateIssuesFromProjectBranchesAndRecordApplicationMeasures() {
    givenIssueCursor("branch-1", issueWithQualifier(ComponentQualifiers.FILE));
    givenIssueCursor("branch-2", issueWithQualifier(ComponentQualifiers.UNIT_TEST_FILE));
    MeasureDto measureDto = new MeasureDto().addValue("ncloc", 84.0);
    when(measureDao.selectByComponentUuid(dbSession, ENTITY_UUID)).thenReturn(Optional.of(measureDto));
    when(metricDao.selectByKeys(eq(dbSession), any())).thenReturn(List.of(
      new MetricDto().setKey("ncloc").setValueType("INT")));

    underTest.recordHistory(ENTITY_UUID, EntityType.APPLICATION, List.of("branch-1", "branch-2"));

    ArgumentCaptor<List<IssueDtoForHistory>> issuesCaptor = ArgumentCaptor.forClass(List.class);
    verify(issueHistoryService).recordIssueHistory(
      eq(ENTITY_UUID), eq(EntityType.APPLICATION), issuesCaptor.capture(), any(LocalDate.class));
    assertThat(issuesCaptor.getValue()).extracting(IssueDtoForHistory::getCodeScope).containsExactly("MAIN", "TEST");

    ArgumentCaptor<List<Measure>> measuresCaptor = ArgumentCaptor.forClass(List.class);
    verify(measuresHistoryService).recordMeasureHistory(
      eq(ENTITY_UUID), eq(EntityType.APPLICATION), measuresCaptor.capture(), any(LocalDate.class));
    assertThat(measuresCaptor.getValue()).containsExactly(new Measure("ncloc", "INT", "84.0"));
  }

  @Test
  void recordHistory_whenApplication_shouldCollectEveryProjectBranchIssueIntoOneApplicationSnapshot() {
    givenIssueCursor("branch-1",
      issueWithQualifier(ComponentQualifiers.FILE),
      issueWithQualifier(ComponentQualifiers.FILE));
    givenIssueCursor("branch-2",
      issueWithQualifier(ComponentQualifiers.UNIT_TEST_FILE).setSeverity("CRITICAL"));

    underTest.recordHistory(ENTITY_UUID, EntityType.APPLICATION, List.of("branch-1", "branch-2"));

    ArgumentCaptor<List<IssueDtoForHistory>> issuesCaptor = ArgumentCaptor.forClass(List.class);
    verify(issueHistoryService).recordIssueHistory(
      eq(ENTITY_UUID), eq(EntityType.APPLICATION), issuesCaptor.capture(), any(LocalDate.class));
    assertThat(issuesCaptor.getValue())
      .extracting(IssueDtoForHistory::getCodeScope, IssueDtoForHistory::getSeverity, IssueDtoForHistory::getRuleKey)
      .containsExactly(
        tuple("MAIN", "MAJOR", "java:S1234"),
        tuple("MAIN", "MAJOR", "java:S1234"),
        tuple("TEST", "CRITICAL", "java:S1234"));
    verify(issueDao).scrollIssuesForIndexation(dbSession, "branch-1", null);
    verify(issueDao).scrollIssuesForIndexation(dbSession, "branch-2", null);
  }

  @Test
  void recordHistory_shouldRecordMeasureHistoryWhenMetricValueTypeIsNull() {
    givenIssueCursor(issueWithQualifier(ComponentQualifiers.FILE));
    MeasureDto measureDto = new MeasureDto().addValue("ncloc", 42.0);
    when(measureDao.selectByComponentUuid(dbSession, ENTITY_UUID)).thenReturn(Optional.of(measureDto));
    when(metricDao.selectByKeys(eq(dbSession), any())).thenReturn(List.of(
      new MetricDto().setKey("ncloc").setValueType(null)));

    recordBranchHistory();

    assertThat(capturedMeasures()).containsExactly(new Measure("ncloc", null, "42.0"));
  }

  @Test
  void recordHistory_shouldNotRecordMeasureHistoryWhenMeasureDataIsMissing() {
    givenIssueCursor(issueWithQualifier(ComponentQualifiers.FILE));

    recordBranchHistory();

    assertThat(capturedIssue()).isNotNull();
    verifyNoInteractions(measuresHistoryService);
  }

  @Test
  void recordHistory_shouldNotRecordMeasureHistoryWhenMetricValuesAreEmpty() {
    givenIssueCursor(issueWithQualifier(ComponentQualifiers.FILE));
    when(measureDao.selectByComponentUuid(dbSession, ENTITY_UUID)).thenReturn(Optional.of(new MeasureDto()));

    recordBranchHistory();

    assertThat(capturedIssue()).isNotNull();
    verifyNoInteractions(measuresHistoryService);
  }

  @Test
  void recordHistory_shouldRecordIssueTtrHistory() {
    IndexedIssueDto issue = issueWithQualifier(ComponentQualifiers.FILE).setCodeVariants("TEST");
    givenIssueCursor(issue);

    underTest.recordHistory(ENTITY_UUID, EntityType.PROJECT_BRANCH, List.of(ENTITY_UUID));

    verify(issueTtrHistoryRecorder).recordTtrHistory(ENTITY_UUID);
  }

  private IndexedIssueDto issueWithQualifier(String qualifier) {
    return new IndexedIssueDto()
      .setIssueKey("issue-key")
      .setRuleUuid(RULE_UUID)
      .setIssueType(1)
      .setSeverity("MAJOR")
      .setStatus("OPEN")
      .setQualifier(qualifier);
  }

  private void recordBranchHistory() {
    underTest.recordHistory(ENTITY_UUID, EntityType.PROJECT_BRANCH, List.of(ENTITY_UUID));
  }

  private void givenIssueCursor(IndexedIssueDto... issues) {
    givenIssueCursor(ENTITY_UUID, issues);
  }

  @SuppressWarnings("unchecked")
  private void givenIssueCursor(String entityUuid, IndexedIssueDto... issues) {
    Cursor<IndexedIssueDto> cursor = mock(Cursor.class);
    when(cursor.iterator()).thenReturn(List.of(issues).iterator());
    when(issueDao.scrollIssuesForIndexation(dbSession, entityUuid, null)).thenReturn(cursor);
  }

  private List<IssueDtoForHistory> capturedIssues() {
    ArgumentCaptor<List<IssueDtoForHistory>> issuesCaptor = ArgumentCaptor.forClass(List.class);
    verify(issueHistoryService).recordIssueHistory(
      eq(ENTITY_UUID), eq(EntityType.PROJECT_BRANCH), issuesCaptor.capture(), any(LocalDate.class));
    return issuesCaptor.getValue();
  }

  private IssueDtoForHistory capturedIssue() {
    return capturedIssues().get(0);
  }

  private List<Measure> capturedMeasures() {
    ArgumentCaptor<List<Measure>> measuresCaptor = ArgumentCaptor.forClass(List.class);
    verify(measuresHistoryService).recordMeasureHistory(
      eq(ENTITY_UUID), eq(EntityType.PROJECT_BRANCH), measuresCaptor.capture(), any(LocalDate.class));
    return measuresCaptor.getValue();
  }
}
