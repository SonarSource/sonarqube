/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.Metric;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.sonar.api.CoreProperties.RATING_GRID;

public class LiveMeasureTreeUpdaterImplTest {
  @Rule
  public DbTester db = DbTester.create();

  private final Configuration config = new MapSettings().setProperty(RATING_GRID, "0.05,0.1,0.2,0.5").asConfig();
  private final HotspotMeasureUpdater hotspotMeasureUpdater = mock(HotspotMeasureUpdater.class);
  private LiveMeasureTreeUpdaterImpl treeUpdater;
  private ComponentIndexImpl componentIndex;
  private MeasureMatrix matrix;
  private MetricDto metricDto;
  private Metric metric;
  private ComponentDto project;
  private BranchDto branch;
  private ComponentDto dir;
  private ComponentDto file1;
  private ComponentDto file2;
  private SnapshotDto snapshot;

  @Before
  public void setUp() {
    // insert project and file structure
    project = db.components().insertPrivateProject();
    branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.uuid()).get();
    dir = db.components().insertComponent(ComponentTesting.newDirectory(project, "src/main/java"));
    file1 = db.components().insertComponent(ComponentTesting.newFileDto(project, dir));
    file2 = db.components().insertComponent(ComponentTesting.newFileDto(project, dir));

    // other needed data
    metricDto = db.measures().insertMetric(m -> m.setValueType(Metric.ValueType.INT.name()));
    metric = new Metric.Builder(metricDto.getKey(), metricDto.getShortName(), Metric.ValueType.valueOf(metricDto.getValueType())).create();
    matrix = new MeasureMatrix(List.of(project, dir, file1, file2), List.of(metricDto), List.of());
    componentIndex = new ComponentIndexImpl(db.getDbClient());
  }

  @Test
  public void should_aggregate_values_up_the_hierarchy() {
    snapshot = db.components().insertSnapshot(project);
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new AggregateValuesFormula(), hotspotMeasureUpdater);

    componentIndex.load(db.getSession(), List.of(file1));
    List<LiveMeasureDto> initialValues = List.of(
      new LiveMeasureDto().setComponentUuid(file1.uuid()).setValue(1d).setMetricUuid(metricDto.getUuid()),
      new LiveMeasureDto().setComponentUuid(file2.uuid()).setValue(1d).setMetricUuid(metricDto.getUuid()),
      new LiveMeasureDto().setComponentUuid(dir.uuid()).setValue(1d).setMetricUuid(metricDto.getUuid()),
      new LiveMeasureDto().setComponentUuid(project.uuid()).setValue(1d).setMetricUuid(metricDto.getUuid())
    );
    matrix = new MeasureMatrix(List.of(project, dir, file1, file2), List.of(metricDto), initialValues);
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getChanged()).extracting(LiveMeasureDto::getComponentUuid).containsOnly(project.uuid(), dir.uuid());
    assertThat(matrix.getMeasure(project, metric.getKey()).get().getValue()).isEqualTo(4d);
    assertThat(matrix.getMeasure(dir, metric.getKey()).get().getValue()).isEqualTo(3d);
    assertThat(matrix.getMeasure(file1, metric.getKey()).get().getValue()).isEqualTo(1d);
    assertThat(matrix.getMeasure(file2, metric.getKey()).get().getValue()).isEqualTo(1d);
  }

  @Test
  public void should_set_values_up_the_hierarchy() {
    snapshot = db.components().insertSnapshot(project);
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new SetValuesFormula(), hotspotMeasureUpdater);

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getChanged()).extracting(LiveMeasureDto::getComponentUuid).containsOnly(project.uuid(), dir.uuid(), file1.uuid());
    assertThat(matrix.getMeasure(project, metric.getKey()).get().getValue()).isEqualTo(1d);
    assertThat(matrix.getMeasure(dir, metric.getKey()).get().getValue()).isEqualTo(1d);
    assertThat(matrix.getMeasure(file1, metric.getKey()).get().getValue()).isEqualTo(1d);
    assertThat(matrix.getMeasure(file2, metric.getKey())).isEmpty();
  }

  @Test
  public void dont_use_leak_formulas_if_no_period() {
    snapshot = db.components().insertSnapshot(project, s -> s.setPeriodDate(null));
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new CountUnresolvedInLeak(), hotspotMeasureUpdater);

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getChanged()).extracting(LiveMeasureDto::getComponentUuid).isEmpty();
  }

  @Test
  public void use_leak_formulas_if_pr() {
    snapshot = db.components().insertSnapshot(project, s -> s.setPeriodDate(null));
    branch.setBranchType(BranchType.PULL_REQUEST);
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new CountUnresolvedInLeak(), hotspotMeasureUpdater);

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getChanged()).extracting(LiveMeasureDto::getComponentUuid).containsOnly(project.uuid(), dir.uuid(), file1.uuid());
  }

  @Test
  public void calculate_new_metrics_if_using_new_code_branch_reference() {
    snapshot = db.components().insertSnapshot(project, s -> s.setPeriodMode(NewCodePeriodType.REFERENCE_BRANCH.name()));
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new CountUnresolvedInLeak(), hotspotMeasureUpdater);

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getChanged()).extracting(LiveMeasureDto::getComponentUuid).containsOnly(project.uuid(), dir.uuid(), file1.uuid());
  }

  @Test
  public void issue_counter_based_on_new_code_branch_reference() {
    snapshot = db.components().insertSnapshot(project, s -> s.setPeriodMode(NewCodePeriodType.REFERENCE_BRANCH.name()));
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new CountUnresolvedInLeak(), hotspotMeasureUpdater);

    RuleDto rule = db.rules().insert();
    IssueDto issue1 = db.issues().insertIssue(rule, project, file1);
    IssueDto issue2 = db.issues().insertIssue(rule, project, file1);
    db.issues().insertNewCodeReferenceIssue(issue1);

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);
    assertThat(matrix.getMeasure(file1, metric.getKey()).get().getVariation()).isEqualTo(1d);
  }

  @Test
  public void issue_counter_uses_begin_of_leak() {
    snapshot = db.components().insertSnapshot(project, s -> s.setPeriodDate(1000L));
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new CountUnresolvedInLeak(), hotspotMeasureUpdater);

    db.issues().insertIssue(i -> i.setIssueCreationDate(new Date(999)).setComponentUuid(file1.uuid()));
    db.issues().insertIssue(i -> i.setIssueCreationDate(new Date(1001)).setComponentUuid(file1.uuid()));
    db.issues().insertIssue(i -> i.setIssueCreationDate(new Date(1002)).setComponentUuid(file1.uuid()));

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getMeasure(file1, metric.getKey()).get().getVariation()).isEqualTo(2d);
  }

  @Test
  public void calls_hotspot_updater() {
    snapshot = db.components().insertSnapshot(project, s -> s.setPeriodDate(1000L));

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new CountUnresolvedInLeak(), hotspotMeasureUpdater);
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);
    verify(hotspotMeasureUpdater).apply(eq(db.getSession()), any(), eq(componentIndex), eq(true), eq(1000L));
  }

  private class AggregateValuesFormula implements MeasureUpdateFormulaFactory {
    @Override
    public List<MeasureUpdateFormula> getFormulas() {
      return List.of(new MeasureUpdateFormula(metric, false, new MeasureUpdateFormulaFactoryImpl.AddChildren(), (c, i) -> {
      }));
    }

    @Override
    public Set<Metric> getFormulaMetrics() {
      return Set.of(metric);
    }
  }

  private class SetValuesFormula implements MeasureUpdateFormulaFactory {
    @Override
    public List<MeasureUpdateFormula> getFormulas() {
      return List.of(new MeasureUpdateFormula(metric, false, (c, m) -> {
      }, (c, i) -> c.setValue(1d)));
    }

    @Override
    public Set<Metric> getFormulaMetrics() {
      return Set.of(metric);
    }
  }

  private class CountUnresolvedInLeak implements MeasureUpdateFormulaFactory {
    @Override
    public List<MeasureUpdateFormula> getFormulas() {
      return List.of(new MeasureUpdateFormula(metric, true, (c, m) -> {
      }, (c, i) -> c.setLeakValue(i.countUnresolved(true))));
    }

    @Override
    public Set<Metric> getFormulaMetrics() {
      return Set.of(metric);
    }
  }
}
