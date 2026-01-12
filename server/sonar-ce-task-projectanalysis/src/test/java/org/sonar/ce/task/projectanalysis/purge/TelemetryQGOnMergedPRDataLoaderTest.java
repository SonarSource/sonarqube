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
package org.sonar.ce.task.projectanalysis.purge;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.config.Configuration;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.purge.PurgeConfiguration;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.purge.PurgeMapper;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;
import org.sonar.telemetry.core.schema.Metric;
import org.sonar.telemetry.core.schema.ProjectMetric;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

@ExtendWith(MockitoExtension.class)
class TelemetryQGOnMergedPRDataLoaderTest {
  private static final MeasureDto OK_QG_MEASURE = createQgMeasure("OK");
  private static final MeasureDto ERROR_QG_MEASURE = createQgMeasure("ERROR");
  private static final String PR_UUID = "pr-uuid-1";
  private static final String PR_UUID_2 = "pr-uuid-2";
  private static final String PR_UUID_3 = "pr-uuid-3";
  private static final String BRANCH_UUID = "branch-uuid-1";
  private static final String ROOT_UUID = "root-uuid";
  private static final String PROJECT_UUID = "project-uuid";

  @Mock
  private PurgeDao purgeDao;
  @Mock
  private MeasureDao measureDao;
  @Mock
  private DbSession session;
  @Mock
  private PurgeConfiguration conf;
  @Mock
  private PurgeMapper purgeMapper;
  @Mock
  private Configuration configuration;

  @InjectMocks
  private TelemetryQGOnMergedPRDataLoader underTest;

  @BeforeEach
  void setUp() {
    lenient().when(session.getMapper(PurgeMapper.class)).thenReturn(purgeMapper);
    lenient().when(conf.rootUuid()).thenReturn(ROOT_UUID);
    lenient().when(conf.projectUuid()).thenReturn(PROJECT_UUID);
    when(configuration.getBoolean(SONAR_TELEMETRY_ENABLE.getKey())).thenReturn(Optional.of(true));
  }

  @ParameterizedTest
  @MethodSource("sonarTelemetryDisabled")
  void sendTelemetry_whenTelemetryDisabled_doesNotSendMessage(Boolean disabledPropertyValue) {
    when(configuration.getBoolean(SONAR_TELEMETRY_ENABLE.getKey())).thenReturn(Optional.ofNullable(disabledPropertyValue));

    underTest.calculateMetrics(session, conf);

    verifyNoInteractions(purgeDao);
    verifyNoInteractions(measureDao);
    assertThat(underTest.getMetrics()).isEmpty();
  }

  private static Stream<Boolean> sonarTelemetryDisabled() {
    return Stream.of(false, null);
  }

  @ParameterizedTest
  @MethodSource("provideQgStatusScenarios")
  void calculateMetrics_whenSinglePullRequestWithQgStatus_correctMetricCreated(MeasureDto qgMeasure, String expectedMetricKey,
    long expectedValue) {
    BranchDto prBranch = createBranchDto(PR_UUID, BranchType.PULL_REQUEST);
    when(purgeDao.getStaleBranchesToPurge(conf, purgeMapper, ROOT_UUID)).thenReturn(singletonList(prBranch));
    when(measureDao.selectByComponentUuidsAndMetricKeys(session, List.of(PR_UUID), singletonList(ALERT_STATUS_KEY))).thenReturn(List.of(qgMeasure));

    underTest.calculateMetrics(session, conf);

    assertThat(underTest.getMetrics()).hasSize(1);
    Metric metric = underTest.getMetrics().iterator().next();
    assertThat(metric.getKey()).isEqualTo(expectedMetricKey);
    assertThat(metric.getValue()).isEqualTo(expectedValue);
    assertThat(metric).isInstanceOf(ProjectMetric.class);
    assertThat(((ProjectMetric) metric).getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(metric.getType()).isEqualTo(TelemetryDataType.INTEGER);
    assertThat(metric.getGranularity()).isEqualTo(Granularity.ADHOC);
  }

  @ParameterizedTest
  @MethodSource("provideInvalidQgDataScenarios")
  void calculateMetrics_whenPullRequestHasInvalidOrMissingQgData_noMetricsCreated(List<MeasureDto> measures) {
    BranchDto prBranch = createBranchDto(PR_UUID, BranchType.PULL_REQUEST);
    when(purgeDao.getStaleBranchesToPurge(conf, purgeMapper, ROOT_UUID)).thenReturn(singletonList(prBranch));
    when(measureDao.selectByComponentUuidsAndMetricKeys(session, List.of(PR_UUID), singletonList(ALERT_STATUS_KEY))).thenReturn(measures);

    underTest.calculateMetrics(session, conf);

    assertThat(underTest.getMetrics()).isEmpty();
  }

  @Test
  void calculateMetrics_whenNoStaleBranchesExist_noMetricsCreated() {
    when(purgeDao.getStaleBranchesToPurge(conf, purgeMapper, ROOT_UUID)).thenReturn(Collections.emptyList());

    underTest.calculateMetrics(session, conf);

    assertThat(underTest.getMetrics()).isEmpty();
  }

  @Test
  void calculateMetrics_whenStaleBranchesAreNotPullRequests_noMetricsCreated() {
    BranchDto branch1 = createBranchDto(BRANCH_UUID, BranchType.BRANCH);
    BranchDto branch2 = createBranchDto("branch-uuid-2", BranchType.BRANCH);
    when(purgeDao.getStaleBranchesToPurge(conf, purgeMapper, ROOT_UUID)).thenReturn(List.of(branch1, branch2));

    underTest.calculateMetrics(session, conf);

    assertThat(underTest.getMetrics()).isEmpty();
  }

  @Test
  void calculateMetrics_whenMultiplePullRequestsAndBranchesWithMixedStatuses_bothMetricsCreatedWithCorrectCounts() {
    BranchDto regularBranch = createBranchDto(BRANCH_UUID, BranchType.BRANCH);
    BranchDto prBranch1 = createBranchDto(PR_UUID, BranchType.PULL_REQUEST);
    BranchDto prBranch2 = createBranchDto(PR_UUID_2, BranchType.PULL_REQUEST);
    BranchDto prBranch3 = createBranchDto(PR_UUID_3, BranchType.PULL_REQUEST);
    when(purgeDao.getStaleBranchesToPurge(conf, purgeMapper, ROOT_UUID)).thenReturn(List.of(prBranch1, prBranch2, prBranch3,
      regularBranch));
    when(measureDao.selectByComponentUuidsAndMetricKeys(session, List.of(PR_UUID, PR_UUID_2, PR_UUID_3), singletonList(ALERT_STATUS_KEY))).thenReturn(List.of(OK_QG_MEASURE, ERROR_QG_MEASURE, OK_QG_MEASURE));
    underTest.calculateMetrics(session, conf);

    assertThat(underTest.getMetrics())
      .hasSize(2)
      .extracting(Metric::getKey)
      .containsExactlyInAnyOrder("project_pr_qg_passed_count", "project_pr_qg_failed_count");

    Metric passedMetric = underTest.getMetrics().stream()
      .filter(m -> m.getKey().equals("project_pr_qg_passed_count"))
      .findFirst()
      .orElseThrow();
    assertThat(passedMetric.getValue()).isEqualTo(2L);

    Metric failedMetric = underTest.getMetrics().stream()
      .filter(m -> m.getKey().equals("project_pr_qg_failed_count"))
      .findFirst()
      .orElseThrow();
    assertThat(failedMetric.getValue()).isEqualTo(1L);
  }

  @Test
  void resetMetrics_whenCalled_clearsAllMetrics() {
    BranchDto prBranch = createBranchDto(PR_UUID, BranchType.PULL_REQUEST);
    when(purgeDao.getStaleBranchesToPurge(conf, purgeMapper, ROOT_UUID)).thenReturn(singletonList(prBranch));
    when(measureDao.selectByComponentUuidsAndMetricKeys(session, List.of(PR_UUID), singletonList(ALERT_STATUS_KEY))).thenReturn(List.of(OK_QG_MEASURE));

    underTest.calculateMetrics(session, conf);
    assertThat(underTest.getMetrics()).isNotEmpty();

    underTest.resetMetrics();

    assertThat(underTest.getMetrics()).isEmpty();
  }

  private static Stream<Arguments> provideQgStatusScenarios() {
    return Stream.of(
      arguments(OK_QG_MEASURE, "project_pr_qg_passed_count", 1L),
      arguments(ERROR_QG_MEASURE, "project_pr_qg_failed_count", 1L)
    );
  }

  private static Stream<Arguments> provideInvalidQgDataScenarios() {
    return Stream.of(
      arguments(List.of()),
      arguments(List.of(createQgMeasure(null))),
      arguments(List.of(createQgMeasure("UNKNOWN_STATUS")))
    );
  }

  private BranchDto createBranchDto(String uuid, BranchType branchType) {
    BranchDto branchDto = mock(BranchDto.class);
    lenient().when(branchDto.getUuid()).thenReturn(uuid);
    when(branchDto.getBranchType()).thenReturn(branchType);
    return branchDto;
  }

  private static MeasureDto createQgMeasure(String qgStatus) {
    MeasureDto measureDto = mock(MeasureDto.class);
    when(measureDto.getString(ALERT_STATUS_KEY)).thenReturn(qgStatus);
    return measureDto;
  }
}
