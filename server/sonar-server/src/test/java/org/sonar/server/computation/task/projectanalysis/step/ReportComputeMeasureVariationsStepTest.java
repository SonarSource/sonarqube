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
import org.sonar.server.computation.task.projectanalysis.component.DumbDeveloper;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;
import org.sonar.server.computation.task.projectanalysis.metric.MetricImpl;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class ReportComputeMeasureVariationsStepTest {

  private static final Metric ISSUES_METRIC = new MetricImpl(1, "violations", "violations", Metric.MetricType.INT);
  private static final Metric DEBT_METRIC = new MetricImpl(2, "sqale_index", "sqale_index", Metric.MetricType.WORK_DUR);
  private static final Metric FILE_COMPLEXITY_METRIC = new MetricImpl(3, "file_complexity", "file_complexity", Metric.MetricType.FLOAT);
  private static final Metric BUILD_BREAKER_METRIC = new MetricImpl(4, "build_breaker", "build_breaker", Metric.MetricType.BOOL);
  private static final Metric NEW_DEBT = new MetricImpl(5, "new_debt", "new_debt", Metric.MetricType.WORK_DUR);
  private static final String PROJECT_UUID = "prj uuid";
  private static final int PROJECT_REF = 1;
  private static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, PROJECT_REF).setUuid(PROJECT_UUID).build();

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
    .add(BUILD_BREAKER_METRIC)
    .add(NEW_DEBT);
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);

  private ComponentDto project;

  private DbSession session = dbTester.getSession();

  private DbClient dbClient = dbTester.getDbClient();

  private ComputeMeasureVariationsStep underTest = new ComputeMeasureVariationsStep(dbClient, treeRootHolder, periodsHolder, metricRepository, measureRepository);

  @Before
  public void setUp() {
    project = dbTester.components().insertPrivateProject(dbTester.organizations().insert(), PROJECT_UUID);
  }

  @Test
  public void do_nothing_when_no_raw_measure() {
    SnapshotDto period1ProjectSnapshot = newAnalysis(project);
    dbClient.snapshotDao().insert(session, period1ProjectSnapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), PROJECT_UUID, period1ProjectSnapshot.getUuid(), 60d));
    session.commit();

    periodsHolder.setPeriod(newPeriod(period1ProjectSnapshot));

    treeRootHolder.setRoot(PROJECT);

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(PROJECT).keys()).isEmpty();
  }

  @Test
  public void do_nothing_when_no_period() {
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_UUID).build();
    treeRootHolder.setRoot(project);
    periodsHolder.setPeriod(null);

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(project).keys()).isEmpty();
  }

  @Test
  public void set_variation() {
    // Project
    SnapshotDto period1Snapshot = newAnalysis(project);
    dbClient.snapshotDao().insert(session, period1Snapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), PROJECT_UUID, period1Snapshot.getUuid(), 60d));

    // Directory
    ComponentDto directoryDto = ComponentTesting.newDirectory(project, "dir");
    dbClient.componentDao().insert(session, directoryDto);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), directoryDto.uuid(), period1Snapshot.getUuid(), 10d));
    session.commit();

    periodsHolder.setPeriod(newPeriod(period1Snapshot));

    Component directory = ReportComponent.builder(Component.Type.DIRECTORY, 2).setUuid(directoryDto.uuid()).build();
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_UUID).addChildren(directory).build();
    treeRootHolder.setRoot(project);

    addRawMeasure(project, ISSUES_METRIC, newMeasureBuilder().create(80, null));
    addRawMeasure(directory, ISSUES_METRIC, newMeasureBuilder().create(20, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasure(project, ISSUES_METRIC).get().getVariation()).isEqualTo(20d);
    assertThat(measureRepository.getRawMeasure(directory, ISSUES_METRIC).get().getVariation()).isEqualTo(10d);
  }

  @Test
  public void set_zero_variation_when_no_change() {
    // Project
    SnapshotDto period1Snapshot = newAnalysis(project);
    dbClient.snapshotDao().insert(session, period1Snapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), PROJECT_UUID, period1Snapshot.getUuid(), 60d));

    // Directory
    ComponentDto directoryDto = ComponentTesting.newDirectory(project, "dir");
    dbClient.componentDao().insert(session, directoryDto);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), directoryDto.uuid(), period1Snapshot.getUuid(), 10d));
    session.commit();

    periodsHolder.setPeriod(newPeriod(period1Snapshot));

    Component directory = ReportComponent.builder(Component.Type.DIRECTORY, 2).setUuid(directoryDto.uuid()).build();
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_UUID).addChildren(directory).build();
    treeRootHolder.setRoot(project);

    addRawMeasure(project, ISSUES_METRIC, newMeasureBuilder().create(60, null));
    addRawMeasure(directory, ISSUES_METRIC, newMeasureBuilder().create(10, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasure(project, ISSUES_METRIC).get().getVariation()).isEqualTo(0d);
    assertThat(measureRepository.getRawMeasure(directory, ISSUES_METRIC).get().getVariation()).isEqualTo(0d);
  }

  @Test
  public void set_variation_to_raw_value_on_new_component() {
    // Project
    SnapshotDto past1ProjectSnapshot = newAnalysis(project).setCreatedAt(1000_000_000L);
    SnapshotDto currentProjectSnapshot = newAnalysis(project).setCreatedAt(2000_000_000L);
    dbClient.snapshotDao().insert(session, past1ProjectSnapshot);
    dbClient.snapshotDao().insert(session, currentProjectSnapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), PROJECT_UUID, past1ProjectSnapshot.getUuid(), 60d));
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), PROJECT_UUID, currentProjectSnapshot.getUuid(), 60d));
    session.commit();

    periodsHolder.setPeriod(newPeriod(past1ProjectSnapshot));

    // Directory has just been added => no snapshot
    Component directory = ReportComponent.builder(Component.Type.DIRECTORY, 2).setUuid("DIRECTORY").build();
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_UUID).addChildren(directory).build();
    treeRootHolder.setRoot(project);

    addRawMeasure(project, ISSUES_METRIC, newMeasureBuilder().create(90, null));
    addRawMeasure(directory, ISSUES_METRIC, newMeasureBuilder().create(10, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasure(project, ISSUES_METRIC).get().getVariation()).isEqualTo(30d);
    // Variation should be the raw value
    assertThat(measureRepository.getRawMeasure(directory, ISSUES_METRIC).get().getVariation()).isEqualTo(10d);
  }

  @Test
  public void set_variation_on_all_numeric_metrics() {
    SnapshotDto period1ProjectSnapshot = newAnalysis(project);
    dbClient.snapshotDao().insert(session, period1ProjectSnapshot);
    dbClient.measureDao().insert(session,
      newMeasureDto(ISSUES_METRIC.getId(), PROJECT_UUID, period1ProjectSnapshot.getUuid(), 60d),
      newMeasureDto(DEBT_METRIC.getId(), PROJECT_UUID, period1ProjectSnapshot.getUuid(), 10d),
      newMeasureDto(FILE_COMPLEXITY_METRIC.getId(), PROJECT_UUID, period1ProjectSnapshot.getUuid(), 2d),
      newMeasureDto(BUILD_BREAKER_METRIC.getId(), PROJECT_UUID, period1ProjectSnapshot.getUuid(), 1d));
    session.commit();

    periodsHolder.setPeriod(newPeriod(period1ProjectSnapshot));

    treeRootHolder.setRoot(PROJECT);

    addRawMeasure(PROJECT, ISSUES_METRIC, newMeasureBuilder().create(80, null));
    addRawMeasure(PROJECT, DEBT_METRIC, newMeasureBuilder().create(5L, null));
    addRawMeasure(PROJECT, FILE_COMPLEXITY_METRIC, newMeasureBuilder().create(3d, 1, null));
    addRawMeasure(PROJECT, BUILD_BREAKER_METRIC, newMeasureBuilder().create(false, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(PROJECT).keys()).hasSize(4);

    assertThat(measureRepository.getRawMeasure(PROJECT, ISSUES_METRIC).get().getVariation()).isEqualTo(20d);
    assertThat(measureRepository.getRawMeasure(PROJECT, DEBT_METRIC).get().getVariation()).isEqualTo(-5d);
    assertThat(measureRepository.getRawMeasure(PROJECT, FILE_COMPLEXITY_METRIC).get().getVariation()).isEqualTo(1d);
    assertThat(measureRepository.getRawMeasure(PROJECT, BUILD_BREAKER_METRIC).get().getVariation()).isEqualTo(-1d);
  }

  @Test
  public void do_not_set_variation_on_numeric_metric_for_developer() {
    SnapshotDto period1ProjectSnapshot = newAnalysis(project);
    dbClient.snapshotDao().insert(session, period1ProjectSnapshot);
    dbClient.measureDao().insert(session,
      newMeasureDto(ISSUES_METRIC.getId(), PROJECT_UUID, period1ProjectSnapshot.getUuid(), 60d));
    session.commit();

    periodsHolder.setPeriod(newPeriod(period1ProjectSnapshot));

    treeRootHolder.setRoot(PROJECT);

    DumbDeveloper developer = new DumbDeveloper("a");
    measureRepository.addRawMeasure(PROJECT_REF, ISSUES_METRIC.getKey(), newMeasureBuilder().forDeveloper(developer).create(80, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(PROJECT).keys()).hasSize(1);

    assertThat(measureRepository.getRawMeasure(PROJECT, ISSUES_METRIC, developer).get().hasVariation()).isFalse();
  }

  @Test
  public void does_not_update_existing_variation() {
    SnapshotDto period1ProjectSnapshot = newAnalysis(project);
    dbClient.snapshotDao().insert(session, period1ProjectSnapshot);
    dbClient.measureDao().insert(session, newMeasureDto(NEW_DEBT.getId(), PROJECT_UUID, period1ProjectSnapshot.getUuid(), 60d));
    session.commit();
    periodsHolder.setPeriod(newPeriod(period1ProjectSnapshot));
    treeRootHolder.setRoot(PROJECT);

    addRawMeasure(PROJECT, NEW_DEBT, newMeasureBuilder().setVariation(10d).createNoValue());

    underTest.execute();

    // As the measure has already variations it has been ignored, then variations will be the same
    assertThat(measureRepository.getRawMeasure(PROJECT, NEW_DEBT).get().getVariation()).isEqualTo(10d);
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
    measureRepository.addRawMeasure(component.getReportAttributes().getRef(), metric.getKey(), measure);
  }
}
