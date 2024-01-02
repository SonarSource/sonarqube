/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.utils.System2;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.es.TestProjectIndexers;
import org.sonar.server.qualitygate.EvaluatedQualityGate;
import org.sonar.server.qualitygate.QualityGate;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.setting.ProjectConfigurationLoader;
import org.sonar.server.setting.TestProjectConfigurationLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;

@RunWith(DataProviderRunner.class)
public class LiveMeasureComputerImplTest {

  @Rule
  public DbTester db = DbTester.create();

  private final TestProjectIndexers projectIndexer = new TestProjectIndexers();
  private MetricDto metric1;
  private MetricDto metric2;
  private ComponentDto project;

  private final LiveQualityGateComputer qGateComputer = mock(LiveQualityGateComputer.class);
  private final QualityGate qualityGate = mock(QualityGate.class);
  private final EvaluatedQualityGate newQualityGate = mock(EvaluatedQualityGate.class);
  private final Configuration configuration = new MapSettings(new PropertyDefinitions(System2.INSTANCE, CorePropertyDefinitions.all())).asConfig();
  private final ProjectConfigurationLoader configurationLoader = new TestProjectConfigurationLoader(configuration);
  private final MeasureUpdateFormulaFactory measureUpdateFormulaFactory = mock(MeasureUpdateFormulaFactory.class);
  private final ComponentIndexFactory componentIndexFactory = mock(ComponentIndexFactory.class);
  private final ComponentIndex componentIndex = mock(ComponentIndex.class);
  private final FakeLiveMeasureTreeUpdater treeUpdater = new FakeLiveMeasureTreeUpdater();
  private final LiveMeasureComputerImpl liveMeasureComputer = new LiveMeasureComputerImpl(db.getDbClient(), measureUpdateFormulaFactory, componentIndexFactory,
    qGateComputer, configurationLoader, projectIndexer, treeUpdater);
  private BranchDto branch;

  @Before
  public void setUp() {
    metric1 = db.measures().insertMetric();
    metric2 = db.measures().insertMetric();

    project = db.components().insertPublicProject();
    branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.uuid()).get();
    db.measures().insertLiveMeasure(project, metric2, lm -> lm.setValue(1d));

    when(componentIndexFactory.create(any(), any())).thenReturn(componentIndex);
    when(measureUpdateFormulaFactory.getFormulaMetrics()).thenReturn(Set.of(toMetric(metric1), toMetric(metric2)));
    when(componentIndex.getBranch()).thenReturn(project);
  }

  @Test
  public void loads_measure_matrix_and_calls_tree_updater() {
    SnapshotDto snapshot = markProjectAsAnalyzed(project);
    when(componentIndex.getAllUuids()).thenReturn(Set.of(project.uuid()));

    liveMeasureComputer.refresh(db.getSession(), List.of(project));

    // tree updater was called
    assertThat(treeUpdater.getMeasureMatrix()).isNotNull();

    // measure matrix was loaded with formula's metrics and measures
    assertThat(treeUpdater.getMeasureMatrix().getMetricByUuid(metric2.getUuid())).isNotNull();
    assertThat(treeUpdater.getMeasureMatrix().getMeasure(project, metric2.getKey()).get().getValue()).isEqualTo(1d);

    // new measures were persisted
    assertThat(db.getDbClient().liveMeasureDao().selectMeasure(db.getSession(), project.uuid(), metric1.getKey()).get().getValue()).isEqualTo(2d);
  }

  @Test
  public void refreshes_quality_gate() {
    SnapshotDto snapshot = markProjectAsAnalyzed(project);
    when(componentIndex.getAllUuids()).thenReturn(Set.of(project.uuid()));
    when(qGateComputer.loadQualityGate(db.getSession(), db.components().getProjectDto(project), branch)).thenReturn(qualityGate);

    liveMeasureComputer.refresh(db.getSession(), List.of(project));

    verify(qGateComputer).refreshGateStatus(eq(project), eq(qualityGate), any(MeasureMatrix.class), eq(configuration));
  }

  @Test
  public void return_if_no_analysis_found() {
    liveMeasureComputer.refresh(db.getSession(), List.of(project));
    assertThat(treeUpdater.getMeasureMatrix()).isNull();
  }

  @Test
  public void returns_qgate_event() {
    SnapshotDto snapshot = markProjectAsAnalyzed(project);
    when(componentIndex.getAllUuids()).thenReturn(Set.of(project.uuid()));

    MetricDto alertStatusMetric = db.measures().insertMetric(m -> m.setKey(ALERT_STATUS_KEY));
    db.measures().insertLiveMeasure(project, alertStatusMetric, lm -> lm.setData("OK"));

    when(qGateComputer.loadQualityGate(db.getSession(), db.components().getProjectDto(project), branch)).thenReturn(qualityGate);
    when(qGateComputer.refreshGateStatus(eq(project), eq(qualityGate), any(MeasureMatrix.class), eq(configuration))).thenReturn(newQualityGate);

    List<QGChangeEvent> qgChangeEvents = liveMeasureComputer.refresh(db.getSession(), List.of(project));

    assertThat(qgChangeEvents).hasSize(1);
    assertThat(qgChangeEvents.get(0).getBranch()).isEqualTo(branch);
    assertThat(qgChangeEvents.get(0).getAnalysis()).isEqualTo(snapshot);
    assertThat(qgChangeEvents.get(0).getProject()).isEqualTo(db.components().getProjectDto(project));
    assertThat(qgChangeEvents.get(0).getPreviousStatus()).contains(Metric.Level.OK);
    assertThat(qgChangeEvents.get(0).getProjectConfiguration()).isEqualTo(configuration);
    assertThat(qgChangeEvents.get(0).getQualityGateSupplier().get()).contains(newQualityGate);
  }

  private SnapshotDto markProjectAsAnalyzed(ComponentDto p) {
    return markProjectAsAnalyzed(p, 1_490_000_000L);
  }

  private SnapshotDto markProjectAsAnalyzed(ComponentDto p, @Nullable Long periodDate) {
    assertThat(p.qualifier()).isEqualTo(Qualifiers.PROJECT);
    return db.components().insertSnapshot(p, s -> s.setPeriodDate(periodDate));
  }

  private static Metric<?> toMetric(MetricDto metric) {
    return new Metric.Builder(metric.getKey(), metric.getShortName(), Metric.ValueType.valueOf(metric.getValueType())).create();
  }

  private class FakeLiveMeasureTreeUpdater implements LiveMeasureTreeUpdater {
    private MeasureMatrix measureMatrix;

    @Override
    public void update(DbSession dbSession, SnapshotDto lastAnalysis, Configuration config, ComponentIndex components, BranchDto branch, MeasureMatrix measures) {
      this.measureMatrix = measures;
      measures.setValue(project, metric1.getKey(), 2d);
    }

    public MeasureMatrix getMeasureMatrix() {
      return measureMatrix;
    }
  }

}
