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
package org.sonar.server.measure.live;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.setting.ProjectConfigurationLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;

@ExtendWith(MockitoExtension.class)
class LiveMeasureUpdaterWorkflowTest {

  private static final String TEST_KEY = "test_key";
  @RegisterExtension
  private final DbTester db = DbTester.create();
  @Mock
  private ProjectConfigurationLoader projectConfigurationLoader;
  @Mock
  private LiveQualityGateComputer liveQualityGateComputer;
  @Mock
  private Configuration config;
  @Mock
  private QualityGate qualityGate;
  private DbClient dbClient;
  private DbSession dbSession;
  private ComponentDto project;
  private BranchDto branch;

  @BeforeEach
  void setUp() {
    this.dbClient = db.getDbClient();
    this.dbSession = db.getSession();

    this.project = db.components().insertPublicProject().getMainBranchComponent();
    this.branch = dbClient.branchDao().selectByUuid(dbSession, project.uuid()).get();

    var projectDto = dbClient.projectDao().selectByUuid(dbSession, branch.getProjectUuid()).get();

    lenient().when(projectConfigurationLoader.loadBranchConfiguration(dbSession, branch)).thenReturn(config);
    lenient().when(liveQualityGateComputer.loadQualityGate(dbSession, projectDto, branch)).thenReturn(qualityGate);
  }

  private LiveMeasureUpdaterWorkflow underTest() {
    return LiveMeasureUpdaterWorkflow.build(
      dbClient,
      dbSession,
      project,
      projectConfigurationLoader,
      liveQualityGateComputer);
  }

  @Test
  void build_whenNoBranch_thenFails() {
    var component = new ComponentDto().setUuid("whatever");

    assertThatThrownBy(() -> LiveMeasureUpdaterWorkflow.build(
      dbClient,
      dbSession,
      component,
      projectConfigurationLoader,
      liveQualityGateComputer)).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void build_whenBranchAndProject_thenLoadsAllData() {
    var result = underTest();

    assertThat(result.getBranchDto()).isEqualTo(this.branch);
    assertThat(result.getConfig()).isEqualTo(config);
  }

  @Test
  void updateQualityGateMeasures() {
    var metric = db.measures().insertMetric(m -> m.setKey(TEST_KEY));
    var measure = db.measures().insertMeasure(project, m -> m.getMetricValues().put(TEST_KEY, 1));
    var expectedResult = mock(EvaluatedQualityGate.class);

    when(liveQualityGateComputer.refreshGateStatus(eq(project), eq(qualityGate), any(MeasureMatrix.class), eq(config))).thenReturn(expectedResult);

    var measureMatrix = new MeasureMatrix(
      List.of(project),
      List.of(metric),
      List.of(measure));

    measureMatrix.setValue(project, TEST_KEY, 2);

    assertThat(underTest().updateQualityGateMeasures(measureMatrix)).isEqualTo(expectedResult);

    var updatedMeasures = dbClient.measureDao().selectByComponentUuid(dbSession, project.uuid());

    assertThat(updatedMeasures.get().getLong(TEST_KEY)).isEqualTo(2);
  }

  @Test
  void getKeysOfAllInvolvedMetrics() {
    var metric = mock(Metric.class);
    when(metric.getKey()).thenReturn(TEST_KEY);

    var formulaFactory = mock(MeasureUpdateFormulaFactory.class);
    when(formulaFactory.getFormulaMetrics()).thenReturn(Set.of(metric));
    when(liveQualityGateComputer.getMetricsRelatedTo(qualityGate)).thenReturn(Set.of("other"));

    var result = underTest().getKeysOfAllInvolvedMetrics(formulaFactory);

    assertThat(result).containsExactlyInAnyOrder("other", TEST_KEY);
  }

  @Test
  void buildMeasureMatrix() {
    db.measures().insertMetric(m -> m.setKey(TEST_KEY));
    db.measures().insertMeasure(project, m -> m.getMetricValues().put(TEST_KEY, 1.0));

    var result = underTest().buildMeasureMatrix(
      List.of(TEST_KEY),
      Set.of(branch.getUuid()));

    assertThat(result.getMeasure(project, TEST_KEY).get().getValue()).isEqualTo(1.0);
  }

  @Test
  void loadPreviousStatus() {
    db.measures().insertMetric(m -> m.setKey(ALERT_STATUS_KEY));
    db.measures().insertMeasure(project, m -> m.getMetricValues().put(ALERT_STATUS_KEY, Metric.Level.OK));

    var result = underTest().loadPreviousStatus();

    assertThat(result).isEqualTo(Metric.Level.OK);
  }
}
