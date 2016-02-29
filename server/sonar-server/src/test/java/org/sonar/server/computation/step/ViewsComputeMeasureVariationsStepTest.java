/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.step;

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
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ViewsComponent;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.SnapshotTesting.createForComponent;
import static org.sonar.db.component.SnapshotTesting.newSnapshotForProject;
import static org.sonar.db.component.SnapshotTesting.newSnapshotForView;


public class ViewsComputeMeasureVariationsStepTest {

  static final Metric ISSUES_METRIC = new MetricImpl(1, "violations", "violations", Metric.MetricType.INT);
  static final Metric DEBT_METRIC = new MetricImpl(2, "sqale_index", "sqale_index", Metric.MetricType.WORK_DUR);
  static final Metric FILE_COMPLEXITY_METRIC = new MetricImpl(3, "file_complexity", "file_complexity", Metric.MetricType.FLOAT);
  static final Metric BUILD_BREAKER_METRIC = new MetricImpl(4, "build_breaker", "build_breaker", Metric.MetricType.BOOL);

  static final ComponentDto VIEW_DTO = ComponentTesting.newView();

  static final Component VIEW = ViewsComponent.builder(Component.Type.VIEW, 1).setUuid(VIEW_DTO.uuid()).build();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();

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

  DbSession session = dbTester.getSession();

  DbClient dbClient = dbTester.getDbClient();

  ComputeMeasureVariationsStep underTest;

  @Before
  public void setUp() {
    dbTester.truncateTables();
    dbClient.componentDao().insert(session, VIEW_DTO);
    session.commit();

    underTest = new ComputeMeasureVariationsStep(dbClient, treeRootHolder, periodsHolder, metricRepository, measureRepository);
  }

  @Test
  public void do_nothing_when_no_raw_measure() {
    SnapshotDto period1ViewSnapshot = newSnapshotForView(VIEW_DTO);
    dbClient.snapshotDao().insert(session, period1ViewSnapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), VIEW_DTO.getId(), period1ViewSnapshot.getId(), 60d));
    session.commit();

    periodsHolder.setPeriods(newPeriod(1, period1ViewSnapshot));

    treeRootHolder.setRoot(VIEW);

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(VIEW).keys()).isEmpty();
  }

  @Test
  public void do_nothing_when_no_period() {
    Component view = ViewsComponent.builder(Component.Type.VIEW, 1).setUuid(VIEW_DTO.uuid()).build();
    treeRootHolder.setRoot(view);
    periodsHolder.setPeriods();

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(view).keys()).isEmpty();
  }

  @Test
  public void set_variation() {
    // View
    SnapshotDto period1ViewSnapshot = newSnapshotForView(VIEW_DTO);
    dbClient.snapshotDao().insert(session, period1ViewSnapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), VIEW_DTO.getId(), period1ViewSnapshot.getId(), 60d));

    // SubView
    ComponentDto subviewDto = ComponentTesting.newSubView(VIEW_DTO, "dir", "key");
    dbClient.componentDao().insert(session, subviewDto);
    SnapshotDto period1SubviewSnapshot = createForComponent(subviewDto, period1ViewSnapshot);
    dbClient.snapshotDao().insert(session, period1SubviewSnapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), subviewDto.getId(), period1SubviewSnapshot.getId(), 10d));
    session.commit();

    periodsHolder.setPeriods(newPeriod(1, period1ViewSnapshot));

    Component subview = ViewsComponent.builder(Component.Type.SUBVIEW, 2).setUuid(subviewDto.uuid()).build();
    Component view = ViewsComponent.builder(Component.Type.VIEW, 1).setUuid(VIEW_DTO.uuid()).addChildren(subview).build();
    treeRootHolder.setRoot(view);

    addRawMeasure(view, ISSUES_METRIC, Measure.newMeasureBuilder().create(80, null));
    addRawMeasure(subview, ISSUES_METRIC, Measure.newMeasureBuilder().create(20, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasure(view, ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(20d);
    assertThat(measureRepository.getRawMeasure(subview, ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(10d);
  }

  @Test
  public void set_variations_on_all_periods() {
    SnapshotDto period1ViewSnapshot = newSnapshotForProject(VIEW_DTO).setLast(false);
    SnapshotDto period2ViewSnapshot = newSnapshotForProject(VIEW_DTO).setLast(false);
    SnapshotDto period3ViewSnapshot = newSnapshotForProject(VIEW_DTO).setLast(false);
    SnapshotDto period4ViewSnapshot = newSnapshotForProject(VIEW_DTO).setLast(false);
    SnapshotDto period5ViewSnapshot = newSnapshotForProject(VIEW_DTO).setLast(false);
    dbClient.snapshotDao().insert(session, period1ViewSnapshot, period2ViewSnapshot, period3ViewSnapshot, period4ViewSnapshot, period5ViewSnapshot);

    dbClient.measureDao().insert(session,
      newMeasureDto(ISSUES_METRIC.getId(), VIEW_DTO.getId(), period1ViewSnapshot.getId(), 0d),
      newMeasureDto(ISSUES_METRIC.getId(), VIEW_DTO.getId(), period2ViewSnapshot.getId(), 20d),
      newMeasureDto(ISSUES_METRIC.getId(), VIEW_DTO.getId(), period3ViewSnapshot.getId(), 40d),
      newMeasureDto(ISSUES_METRIC.getId(), VIEW_DTO.getId(), period4ViewSnapshot.getId(), 80d),
      newMeasureDto(ISSUES_METRIC.getId(), VIEW_DTO.getId(), period5ViewSnapshot.getId(), 100d));
    session.commit();

    periodsHolder.setPeriods(newPeriod(1, period1ViewSnapshot),
      newPeriod(2, period2ViewSnapshot),
      newPeriod(3, period3ViewSnapshot),
      newPeriod(4, period4ViewSnapshot),
      newPeriod(5, period5ViewSnapshot));

    treeRootHolder.setRoot(VIEW);

    addRawMeasure(VIEW, ISSUES_METRIC, Measure.newMeasureBuilder().create(80, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(VIEW).keys()).hasSize(1);

    Measure measure = measureRepository.getRawMeasure(VIEW, ISSUES_METRIC).get();
    assertThat(measure.hasVariations()).isTrue();
    assertThat(measure.getVariations().getVariation1()).isEqualTo(80d);
    assertThat(measure.getVariations().getVariation2()).isEqualTo(60d);
    assertThat(measure.getVariations().getVariation3()).isEqualTo(40d);
    assertThat(measure.getVariations().getVariation4()).isEqualTo(0d);
    assertThat(measure.getVariations().getVariation5()).isEqualTo(-20d);
  }

  @Test
  public void set_variation_on_all_numeric_metrics() {
    SnapshotDto period1ViewSnapshot = newSnapshotForProject(VIEW_DTO);
    dbClient.snapshotDao().insert(session, period1ViewSnapshot);
    dbClient.measureDao().insert(session,
      newMeasureDto(ISSUES_METRIC.getId(), VIEW_DTO.getId(), period1ViewSnapshot.getId(), 60d),
      newMeasureDto(DEBT_METRIC.getId(), VIEW_DTO.getId(), period1ViewSnapshot.getId(), 10d),
      newMeasureDto(FILE_COMPLEXITY_METRIC.getId(), VIEW_DTO.getId(), period1ViewSnapshot.getId(), 2d),
      newMeasureDto(BUILD_BREAKER_METRIC.getId(), VIEW_DTO.getId(), period1ViewSnapshot.getId(), 1d)
    );
    session.commit();

    periodsHolder.setPeriods(newPeriod(1, period1ViewSnapshot));

    treeRootHolder.setRoot(VIEW);

    addRawMeasure(VIEW, ISSUES_METRIC, Measure.newMeasureBuilder().create(80, null));
    addRawMeasure(VIEW, DEBT_METRIC, Measure.newMeasureBuilder().create(5L, null));
    addRawMeasure(VIEW, FILE_COMPLEXITY_METRIC, Measure.newMeasureBuilder().create(3d, 1));
    addRawMeasure(VIEW, BUILD_BREAKER_METRIC, Measure.newMeasureBuilder().create(false, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(VIEW).keys()).hasSize(4);

    assertThat(measureRepository.getRawMeasure(VIEW, ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(20d);
    assertThat(measureRepository.getRawMeasure(VIEW, DEBT_METRIC).get().getVariations().getVariation1()).isEqualTo(-5d);
    assertThat(measureRepository.getRawMeasure(VIEW, FILE_COMPLEXITY_METRIC).get().getVariations().getVariation1()).isEqualTo(1d);
    assertThat(measureRepository.getRawMeasure(VIEW, BUILD_BREAKER_METRIC).get().getVariations().getVariation1()).isEqualTo(-1d);
  }

  private static MeasureDto newMeasureDto(int metricId, long projectId, long snapshotId, double value) {
    return new MeasureDto().setMetricId(metricId).setComponentId(projectId).setSnapshotId(snapshotId).setValue(value);
  }

  private static Period newPeriod(int index, SnapshotDto snapshotDto) {
    return new Period(index, "mode", null, snapshotDto.getCreatedAt(), snapshotDto.getId());
  }

  private void addRawMeasure(Component component, Metric metric, Measure measure) {
    measureRepository.add(component, metric, measure);
  }
}
