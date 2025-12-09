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
import org.sonar.core.rule.RuleType;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.rule.RuleDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.CoreProperties.RATING_GRID;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY;
import static org.sonar.api.measures.Metric.ValueType.*;

public class LiveMeasureTreeUpdaterImplIT {
  @Rule
  public DbTester db = DbTester.create();

  private final Configuration config = new MapSettings().setProperty(RATING_GRID, "0.05,0.1,0.2,0.5").asConfig();
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
    project = db.components().insertPrivateProject().getMainBranchComponent();
    branch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.uuid()).get();
    dir = db.components().insertComponent(ComponentTesting.newDirectory(project, "src/main/java"));
    file1 = db.components().insertComponent(ComponentTesting.newFileDto(project, dir));
    file2 = db.components().insertComponent(ComponentTesting.newFileDto(project, dir));

    // other needed data
    metricDto = db.measures().insertMetric(m -> m.setValueType(INT.name()));
    metric = new Metric.Builder(metricDto.getKey(), metricDto.getShortName(), valueOf(metricDto.getValueType())).create();
    matrix = new MeasureMatrix(List.of(project, dir, file1, file2), List.of(metricDto), List.of());
    componentIndex = new ComponentIndexImpl(db.getDbClient());
  }

  @Test
  public void should_aggregate_values_up_the_hierarchy() {
    snapshot = db.components().insertSnapshot(project);
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new AggregateValuesFormula());

    componentIndex.load(db.getSession(), List.of(file1));
    List<MeasureDto> initialValues = List.of(
      new MeasureDto().setComponentUuid(file1.uuid()).addValue(metricDto.getKey(), 1d),
      new MeasureDto().setComponentUuid(file2.uuid()).addValue(metricDto.getKey(), 1d),
      new MeasureDto().setComponentUuid(dir.uuid()).addValue(metricDto.getKey(), 1d),
      new MeasureDto().setComponentUuid(project.uuid()).addValue(metricDto.getKey(), 1d)
    );
    matrix = new MeasureMatrix(List.of(project, dir, file1, file2), List.of(metricDto), initialValues);
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getChanged()).extracting(MeasureMatrix.Measure::getComponentUuid).containsOnly(project.uuid(), dir.uuid());
    assertThat(matrix.getMeasure(project, metric.getKey()).get().getValue()).isEqualTo(4d);
    assertThat(matrix.getMeasure(dir, metric.getKey()).get().getValue()).isEqualTo(3d);
    assertThat(matrix.getMeasure(file1, metric.getKey()).get().getValue()).isEqualTo(1d);
    assertThat(matrix.getMeasure(file2, metric.getKey()).get().getValue()).isEqualTo(1d);
  }

  @Test
  public void should_set_values_up_the_hierarchy() {
    snapshot = db.components().insertSnapshot(project);
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new SetValuesFormula());

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getChanged()).extracting(MeasureMatrix.Measure::getComponentUuid).containsOnly(project.uuid(), dir.uuid(), file1.uuid());
    assertThat(matrix.getMeasure(project, metric.getKey()).get().getValue()).isEqualTo(1d);
    assertThat(matrix.getMeasure(dir, metric.getKey()).get().getValue()).isEqualTo(1d);
    assertThat(matrix.getMeasure(file1, metric.getKey()).get().getValue()).isEqualTo(1d);
    assertThat(matrix.getMeasure(file2, metric.getKey())).isEmpty();
  }

  @Test
  public void dont_use_leak_formulas_if_no_period() {
    snapshot = db.components().insertSnapshot(project, s -> s.setPeriodDate(null));
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new CountUnresolvedInLeak());

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getChanged()).extracting(MeasureMatrix.Measure::getComponentUuid).isEmpty();
  }

  @Test
  public void use_leak_formulas_if_pr() {
    snapshot = db.components().insertSnapshot(project, s -> s.setPeriodDate(null));
    branch.setBranchType(BranchType.PULL_REQUEST);
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new CountUnresolvedInLeak());

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getChanged()).extracting(MeasureMatrix.Measure::getComponentUuid).containsOnly(project.uuid(), dir.uuid(), file1.uuid());
  }

  @Test
  public void calculate_new_metrics_if_using_new_code_branch_reference() {
    snapshot = db.components().insertSnapshot(project, s -> s.setPeriodMode(NewCodePeriodType.REFERENCE_BRANCH.name()));
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new CountUnresolvedInLeak());

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getChanged()).extracting(MeasureMatrix.Measure::getComponentUuid).containsOnly(project.uuid(), dir.uuid(), file1.uuid());
  }

  @Test
  public void issue_counter_based_on_new_code_branch_reference() {
    snapshot = db.components().insertSnapshot(project, s -> s.setPeriodMode(NewCodePeriodType.REFERENCE_BRANCH.name()));
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new CountUnresolvedInLeak());

    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.BUG));
    IssueDto issue1 = db.issues().insertIssue(rule, project, file1);
    IssueDto issue2 = db.issues().insertIssue(rule, project, file1);
    db.issues().insertNewCodeReferenceIssue(issue1);

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);
    assertThat(matrix.getMeasure(file1, metric.getKey()).get().getValue()).isEqualTo(1d);
  }

  @Test
  public void issue_counter_uses_begin_of_leak() {
    snapshot = db.components().insertSnapshot(project, s -> s.setPeriodDate(1000L));
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new CountUnresolvedInLeak());

    db.issues().insertIssue(i -> i.setIssueCreationDate(new Date(999)).setComponentUuid(file1.uuid()));
    db.issues().insertIssue(i -> i.setIssueCreationDate(new Date(1001)).setComponentUuid(file1.uuid()));
    db.issues().insertIssue(i -> i.setIssueCreationDate(new Date(1002)).setComponentUuid(file1.uuid()));

    componentIndex.load(db.getSession(), List.of(file1));
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getMeasure(file1, metric.getKey()).get().getValue()).isEqualTo(2d);
  }

  @Test
  public void context_calculates_hotspot_counts_from_percentage() {
    List<MetricDto> metrics = List.of(
      new MetricDto().setKey(SECURITY_HOTSPOTS_KEY).setValueType(PERCENT.name()),
      new MetricDto().setKey(SECURITY_HOTSPOTS_REVIEWED_KEY).setValueType(PERCENT.name()),
      new MetricDto().setKey(SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY).setValueType(INT.name()),
      new MetricDto().setKey(SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY).setValueType(INT.name()));
    componentIndex.load(db.getSession(), List.of(file1));
    matrix = new MeasureMatrix(List.of(project, dir, file1, file2), metrics, List.of());

    LiveMeasureTreeUpdaterImpl.FormulaContextImpl context = new LiveMeasureTreeUpdaterImpl.FormulaContextImpl(matrix, componentIndex, null);
    matrix.setValue(file1, SECURITY_HOTSPOTS_KEY, 4d);
    matrix.setValue(file1, SECURITY_HOTSPOTS_REVIEWED_KEY, 33d);

    matrix.setValue(file2, SECURITY_HOTSPOTS_KEY, 2d);
    matrix.setValue(file2, SECURITY_HOTSPOTS_REVIEWED_KEY, 50d);

    context.change(dir, null);
    assertThat(context.getChildrenHotspotsToReview()).isEqualTo(6);
    assertThat(context.getChildrenHotspotsReviewed()).isEqualTo(4);
  }

  @Test
  public void context_calculates_new_hotspot_counts_from_percentage() {
    List<MetricDto> metrics = List.of(
      new MetricDto().setKey(NEW_SECURITY_HOTSPOTS_KEY).setValueType(PERCENT.name()),
      new MetricDto().setKey(NEW_SECURITY_HOTSPOTS_REVIEWED_KEY).setValueType(PERCENT.name()),
      new MetricDto().setKey(NEW_SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY).setValueType(INT.name()),
      new MetricDto().setKey(NEW_SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY).setValueType(INT.name()));
    componentIndex.load(db.getSession(), List.of(file1));
    matrix = new MeasureMatrix(List.of(project, dir, file1, file2), metrics, List.of());

    LiveMeasureTreeUpdaterImpl.FormulaContextImpl context = new LiveMeasureTreeUpdaterImpl.FormulaContextImpl(matrix, componentIndex, null);
    matrix.setValue(file1, NEW_SECURITY_HOTSPOTS_KEY, 4d);
    matrix.setValue(file1, NEW_SECURITY_HOTSPOTS_REVIEWED_KEY, 33d);

    matrix.setValue(file2, NEW_SECURITY_HOTSPOTS_KEY, 2d);
    matrix.setValue(file2, NEW_SECURITY_HOTSPOTS_REVIEWED_KEY, 50d);

    context.change(dir, null);
    assertThat(context.getChildrenNewHotspotsToReview()).isEqualTo(6);
    assertThat(context.getChildrenNewHotspotsReviewed()).isEqualTo(4);
  }

  @Test
  public void context_returns_hotspots_counts_from_measures() {
    List<MetricDto> metrics = List.of(
      new MetricDto().setKey(SECURITY_HOTSPOTS_KEY).setValueType(PERCENT.name()),
      new MetricDto().setKey(SECURITY_HOTSPOTS_REVIEWED_KEY).setValueType(PERCENT.name()),
      new MetricDto().setKey(SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY).setValueType(INT.name()),
      new MetricDto().setKey(SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY).setValueType(INT.name()));
    componentIndex.load(db.getSession(), List.of(file1));
    matrix = new MeasureMatrix(List.of(project, dir, file1, file2), metrics, List.of());

    LiveMeasureTreeUpdaterImpl.FormulaContextImpl context = new LiveMeasureTreeUpdaterImpl.FormulaContextImpl(matrix, componentIndex, null);
    matrix.setValue(file1, SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY, 5D);
    matrix.setValue(file1, SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY, 5D);
    matrix.setValue(file1, SECURITY_HOTSPOTS_KEY, 6d);
    matrix.setValue(file1, SECURITY_HOTSPOTS_REVIEWED_KEY, 33d);

    matrix.setValue(file2, SECURITY_HOTSPOTS_REVIEWED_STATUS_KEY, 5D);
    matrix.setValue(file2, SECURITY_HOTSPOTS_TO_REVIEW_STATUS_KEY, 5D);
    matrix.setValue(file2, SECURITY_HOTSPOTS_KEY, 4d);
    matrix.setValue(file2, SECURITY_HOTSPOTS_REVIEWED_KEY, 50d);

    context.change(dir, null);
    assertThat(context.getChildrenHotspotsToReview()).isEqualTo(10);
    assertThat(context.getChildrenHotspotsReviewed()).isEqualTo(10);
  }

  @Test
  public void update_whenFormulaIsOnlyIfComputedOnBranchAndMetricNotComputedOnBranch_shouldNotCompute() {
    snapshot = db.components().insertSnapshot(project);
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new AggregateValuesFormula());

    componentIndex.load(db.getSession(), List.of(file1));
    List<MeasureDto> initialValues = List.of(new MeasureDto().setComponentUuid(file1.uuid()).addValue(metricDto.getKey(), 1d));
    matrix = new MeasureMatrix(List.of(project, dir, file1, file2), List.of(metricDto), initialValues);
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getChanged()).isEmpty();
    assertThat(matrix.getMeasure(project, metric.getKey())).isEmpty();
  }

  @Test
  public void update_whenFormulaIsNotOnlyIfComputedOnBranchAndMetricNotComputedOnBranch_shouldCompute() {
    snapshot = db.components().insertSnapshot(project);
    treeUpdater = new LiveMeasureTreeUpdaterImpl(db.getDbClient(), new SetValuesFormula());

    componentIndex.load(db.getSession(), List.of(file1));
    List<MeasureDto> initialValues = List.of(new MeasureDto().setComponentUuid(file1.uuid()).addValue(metricDto.getKey(), 1d));
    matrix = new MeasureMatrix(List.of(project, dir, file1, file2), List.of(metricDto), initialValues);
    treeUpdater.update(db.getSession(), snapshot, config, componentIndex, branch, matrix);

    assertThat(matrix.getChanged()).extracting(MeasureMatrix.Measure::getComponentUuid).containsOnly(project.uuid(), dir.uuid());
    assertThat(matrix.getMeasure(project, metric.getKey()).get().getValue()).isEqualTo(1d);
    assertThat(matrix.getMeasure(dir, metric.getKey()).get().getValue()).isEqualTo(1d);
    assertThat(matrix.getMeasure(file1, metric.getKey()).get().getValue()).isEqualTo(1d);
    assertThat(matrix.getMeasure(file2, metric.getKey())).isEmpty();
  }

  private class AggregateValuesFormula implements MeasureUpdateFormulaFactory {
    @Override
    public List<MeasureUpdateFormula> getFormulas() {
      return List.of(new MeasureUpdateFormula(metric, false, true, new MeasureUpdateFormulaFactoryImpl.AddChildren(), (c, i) -> {
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
      return List.of(new MeasureUpdateFormula(metric, false, false, (c, m) -> {
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
      }, (c, i) -> c.setValue(i.countUnresolved(true))));
    }

    @Override
    public Set<Metric> getFormulaMetrics() {
      return Set.of(metric);
    }
  }
}
