/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.step;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.ViewsComponent;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricImpl;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

public class ViewsComputeMeasureVariationsStepTest {

  private static final Metric ISSUES_METRIC = new MetricImpl(1, "violations", "violations", Metric.MetricType.INT);
  private static final Metric DEBT_METRIC = new MetricImpl(2, "sqale_index", "sqale_index", Metric.MetricType.WORK_DUR);
  private static final Metric FILE_COMPLEXITY_METRIC = new MetricImpl(3, "file_complexity", "file_complexity", Metric.MetricType.FLOAT);
  private static final Metric BUILD_BREAKER_METRIC = new MetricImpl(4, "build_breaker", "build_breaker", Metric.MetricType.BOOL);
  private static final String VIEW_UUID = "view uuid";
  private static final Component VIEW = ViewsComponent.builder(Component.Type.VIEW, 1).setUuid(VIEW_UUID).build();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public PeriodHolderRule periodsHolder = new PeriodHolderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(ISSUES_METRIC)
    .add(DEBT_METRIC)
    .add(FILE_COMPLEXITY_METRIC)
    .add(BUILD_BREAKER_METRIC);

  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private DbSession session = dbTester.getSession();

  private DbClient dbClient = dbTester.getDbClient();

  private ComponentDto view;

  private ComputeMeasureVariationsStep underTest = new ComputeMeasureVariationsStep(dbClient, treeRootHolder, periodsHolder, metricRepository, measureRepository);

  @Before
  public void setUp() {
    view = dbTester.components().insertView(dbTester.organizations().insert(), VIEW_UUID);
  }

  @Test
  public void do_nothing_when_no_raw_measure() {
    SnapshotDto period1ViewSnapshot = newAnalysis(view);
    dbClient.snapshotDao().insert(session, period1ViewSnapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), VIEW_UUID, period1ViewSnapshot.getUuid(), 60d));
    session.commit();

    periodsHolder.setPeriod(newPeriod(period1ViewSnapshot));

    treeRootHolder.setRoot(VIEW);

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(VIEW).keys()).isEmpty();
  }

  @Test
  public void do_nothing_when_no_period() {
    Component view = ViewsComponent.builder(Component.Type.VIEW, 1).setUuid(VIEW_UUID).build();
    treeRootHolder.setRoot(view);
    periodsHolder.setPeriod(null);

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(view).keys()).isEmpty();
  }

  @Test
  public void set_variation() {
    // View
    SnapshotDto period1Snapshot = newAnalysis(view);
    dbClient.snapshotDao().insert(session, period1Snapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), VIEW_UUID, period1Snapshot.getUuid(), 60d));

    // SubView
    ComponentDto subviewDto = ComponentTesting.newSubView(view, "dir", "key");
    dbClient.componentDao().insert(session, subviewDto);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), subviewDto.uuid(), period1Snapshot.getUuid(), 10d));
    session.commit();

    periodsHolder.setPeriod(newPeriod(period1Snapshot));

    Component subview = ViewsComponent.builder(Component.Type.SUBVIEW, 2).setUuid(subviewDto.uuid()).build();
    Component view = ViewsComponent.builder(Component.Type.VIEW, 1).setUuid(VIEW_UUID).addChildren(subview).build();
    treeRootHolder.setRoot(view);

    addRawMeasure(view, ISSUES_METRIC, Measure.newMeasureBuilder().create(80, null));
    addRawMeasure(subview, ISSUES_METRIC, Measure.newMeasureBuilder().create(20, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasure(view, ISSUES_METRIC).get().getVariation()).isEqualTo(20d);
    assertThat(measureRepository.getRawMeasure(subview, ISSUES_METRIC).get().getVariation()).isEqualTo(10d);
  }

  @Test
  public void set_variation_on_all_numeric_metrics() {
    SnapshotDto period1ViewSnapshot = newAnalysis(view);
    dbClient.snapshotDao().insert(session, period1ViewSnapshot);
    dbClient.measureDao().insert(session,
      newMeasureDto(ISSUES_METRIC.getId(), VIEW_UUID, period1ViewSnapshot.getUuid(), 60d),
      newMeasureDto(DEBT_METRIC.getId(), VIEW_UUID, period1ViewSnapshot.getUuid(), 10d),
      newMeasureDto(FILE_COMPLEXITY_METRIC.getId(), VIEW_UUID, period1ViewSnapshot.getUuid(), 2d),
      newMeasureDto(BUILD_BREAKER_METRIC.getId(), VIEW_UUID, period1ViewSnapshot.getUuid(), 1d));
    session.commit();

    periodsHolder.setPeriod(newPeriod(period1ViewSnapshot));

    treeRootHolder.setRoot(VIEW);

    addRawMeasure(VIEW, ISSUES_METRIC, Measure.newMeasureBuilder().create(80, null));
    addRawMeasure(VIEW, DEBT_METRIC, Measure.newMeasureBuilder().create(5L, null));
    addRawMeasure(VIEW, FILE_COMPLEXITY_METRIC, Measure.newMeasureBuilder().create(3d, 1));
    addRawMeasure(VIEW, BUILD_BREAKER_METRIC, Measure.newMeasureBuilder().create(false, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(VIEW).keys()).hasSize(4);

    assertThat(measureRepository.getRawMeasure(VIEW, ISSUES_METRIC).get().getVariation()).isEqualTo(20d);
    assertThat(measureRepository.getRawMeasure(VIEW, DEBT_METRIC).get().getVariation()).isEqualTo(-5d);
    assertThat(measureRepository.getRawMeasure(VIEW, FILE_COMPLEXITY_METRIC).get().getVariation()).isEqualTo(1d);
    assertThat(measureRepository.getRawMeasure(VIEW, BUILD_BREAKER_METRIC).get().getVariation()).isEqualTo(-1d);
  }

  private static MeasureDto newMeasureDto(int metricId, String componentUuid, String analysisUuid, double value) {
    return new MeasureDto()
      .setMetricId(metricId)
      .setComponentUuid(componentUuid)
      .setAnalysisUuid(analysisUuid)
      .setValue(value);
  }

  private static Period newPeriod(SnapshotDto snapshotDto) {
    return new Period("mode", null, snapshotDto.getCreatedAt(), snapshotDto.getUuid());
  }

  private void addRawMeasure(Component component, Metric metric, Measure measure) {
    measureRepository.add(component, metric, measure);
  }
}
