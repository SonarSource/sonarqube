/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.junit.experimental.categories.Category;
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
import org.sonar.server.computation.component.DumbDeveloper;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.SnapshotTesting.createForComponent;
import static org.sonar.db.component.SnapshotTesting.newSnapshotForProject;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;

@Category(DbTests.class)
public class ReportComputeMeasureVariationsStepTest {

  static final Metric ISSUES_METRIC = new MetricImpl(1, "violations", "violations", Metric.MetricType.INT);
  static final Metric DEBT_METRIC = new MetricImpl(2, "sqale_index", "sqale_index", Metric.MetricType.WORK_DUR);
  static final Metric FILE_COMPLEXITY_METRIC = new MetricImpl(3, "file_complexity", "file_complexity", Metric.MetricType.FLOAT);
  static final Metric BUILD_BREAKER_METRIC = new MetricImpl(4, "build_breaker", "build_breaker", Metric.MetricType.BOOL);

  static final ComponentDto PROJECT_DTO = ComponentTesting.newProjectDto();

  static final int PROJECT_REF = 1;
  static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, PROJECT_REF).setUuid(PROJECT_DTO.uuid()).build();

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
    dbClient.componentDao().insert(session, PROJECT_DTO);
    session.commit();

    underTest = new ComputeMeasureVariationsStep(dbClient, treeRootHolder, periodsHolder, metricRepository, measureRepository);
  }

  @Test
  public void do_nothing_when_no_raw_measure() {
    SnapshotDto period1ProjectSnapshot = newSnapshotForProject(PROJECT_DTO);
    dbClient.snapshotDao().insert(session, period1ProjectSnapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), PROJECT_DTO.getId(), period1ProjectSnapshot.getId(), 60d));
    session.commit();

    periodsHolder.setPeriods(newPeriod(1, period1ProjectSnapshot));

    treeRootHolder.setRoot(PROJECT);

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(PROJECT).keys()).isEmpty();
  }

  @Test
  public void do_nothing_when_no_period() {
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_DTO.uuid()).build();
    treeRootHolder.setRoot(project);
    periodsHolder.setPeriods();

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(project).keys()).isEmpty();
  }

  @Test
  public void set_variation() {
    // Project
    SnapshotDto period1ProjectSnapshot = newSnapshotForProject(PROJECT_DTO);
    dbClient.snapshotDao().insert(session, period1ProjectSnapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), PROJECT_DTO.getId(), period1ProjectSnapshot.getId(), 60d));

    // Directory
    ComponentDto directoryDto = ComponentTesting.newDirectory(PROJECT_DTO, "dir");
    dbClient.componentDao().insert(session, directoryDto);
    SnapshotDto period1DirectorySnapshot = createForComponent(directoryDto, period1ProjectSnapshot);
    dbClient.snapshotDao().insert(session, period1DirectorySnapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), directoryDto.getId(), period1DirectorySnapshot.getId(), 10d));
    session.commit();

    periodsHolder.setPeriods(newPeriod(1, period1ProjectSnapshot));

    Component directory = ReportComponent.builder(Component.Type.DIRECTORY, 2).setUuid(directoryDto.uuid()).build();
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_DTO.uuid()).addChildren(directory).build();
    treeRootHolder.setRoot(project);

    addRawMeasure(project, ISSUES_METRIC, newMeasureBuilder().create(80, null));
    addRawMeasure(directory, ISSUES_METRIC, newMeasureBuilder().create(20, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasure(project, ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(20d);
    assertThat(measureRepository.getRawMeasure(directory, ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(10d);
  }

  @Test
  public void set_variations_on_all_periods() {
    SnapshotDto period1ProjectSnapshot = newSnapshotForProject(PROJECT_DTO).setLast(false);
    SnapshotDto period2ProjectSnapshot = newSnapshotForProject(PROJECT_DTO).setLast(false);
    SnapshotDto period3ProjectSnapshot = newSnapshotForProject(PROJECT_DTO).setLast(false);
    SnapshotDto period4ProjectSnapshot = newSnapshotForProject(PROJECT_DTO).setLast(false);
    SnapshotDto period5ProjectSnapshot = newSnapshotForProject(PROJECT_DTO).setLast(false);
    dbClient.snapshotDao().insert(session, period1ProjectSnapshot, period2ProjectSnapshot, period3ProjectSnapshot, period4ProjectSnapshot, period5ProjectSnapshot);

    dbClient.measureDao().insert(session,
      newMeasureDto(ISSUES_METRIC.getId(), PROJECT_DTO.getId(), period1ProjectSnapshot.getId(), 0d),
      newMeasureDto(ISSUES_METRIC.getId(), PROJECT_DTO.getId(), period2ProjectSnapshot.getId(), 20d),
      newMeasureDto(ISSUES_METRIC.getId(), PROJECT_DTO.getId(), period3ProjectSnapshot.getId(), 40d),
      newMeasureDto(ISSUES_METRIC.getId(), PROJECT_DTO.getId(), period4ProjectSnapshot.getId(), 80d),
      newMeasureDto(ISSUES_METRIC.getId(), PROJECT_DTO.getId(), period5ProjectSnapshot.getId(), 100d));
    session.commit();

    periodsHolder.setPeriods(newPeriod(1, period1ProjectSnapshot),
      newPeriod(2, period2ProjectSnapshot),
      newPeriod(3, period3ProjectSnapshot),
      newPeriod(4, period4ProjectSnapshot),
      newPeriod(5, period5ProjectSnapshot));

    treeRootHolder.setRoot(PROJECT);

    addRawMeasure(PROJECT, ISSUES_METRIC, newMeasureBuilder().create(80, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(PROJECT).keys()).hasSize(1);

    Measure measure = measureRepository.getRawMeasure(PROJECT, ISSUES_METRIC).get();
    assertThat(measure.hasVariations()).isTrue();
    assertThat(measure.getVariations().getVariation1()).isEqualTo(80d);
    assertThat(measure.getVariations().getVariation2()).isEqualTo(60d);
    assertThat(measure.getVariations().getVariation3()).isEqualTo(40d);
    assertThat(measure.getVariations().getVariation4()).isEqualTo(0d);
    assertThat(measure.getVariations().getVariation5()).isEqualTo(-20d);
  }

  @Test
  public void set_variation_on_all_numeric_metrics() {
    SnapshotDto period1ProjectSnapshot = newSnapshotForProject(PROJECT_DTO);
    dbClient.snapshotDao().insert(session, period1ProjectSnapshot);
    dbClient.measureDao().insert(session,
      newMeasureDto(ISSUES_METRIC.getId(), PROJECT_DTO.getId(), period1ProjectSnapshot.getId(), 60d),
      newMeasureDto(DEBT_METRIC.getId(), PROJECT_DTO.getId(), period1ProjectSnapshot.getId(), 10d),
      newMeasureDto(FILE_COMPLEXITY_METRIC.getId(), PROJECT_DTO.getId(), period1ProjectSnapshot.getId(), 2d),
      newMeasureDto(BUILD_BREAKER_METRIC.getId(), PROJECT_DTO.getId(), period1ProjectSnapshot.getId(), 1d)
      );
    session.commit();

    periodsHolder.setPeriods(newPeriod(1, period1ProjectSnapshot));

    treeRootHolder.setRoot(PROJECT);

    addRawMeasure(PROJECT, ISSUES_METRIC, newMeasureBuilder().create(80, null));
    addRawMeasure(PROJECT, DEBT_METRIC, newMeasureBuilder().create(5L, null));
    addRawMeasure(PROJECT, FILE_COMPLEXITY_METRIC, newMeasureBuilder().create(3d, 1, null));
    addRawMeasure(PROJECT, BUILD_BREAKER_METRIC, newMeasureBuilder().create(false, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(PROJECT).keys()).hasSize(4);

    assertThat(measureRepository.getRawMeasure(PROJECT, ISSUES_METRIC).get().getVariations().getVariation1()).isEqualTo(20d);
    assertThat(measureRepository.getRawMeasure(PROJECT, DEBT_METRIC).get().getVariations().getVariation1()).isEqualTo(-5d);
    assertThat(measureRepository.getRawMeasure(PROJECT, FILE_COMPLEXITY_METRIC).get().getVariations().getVariation1()).isEqualTo(1d);
    assertThat(measureRepository.getRawMeasure(PROJECT, BUILD_BREAKER_METRIC).get().getVariations().getVariation1()).isEqualTo(-1d);
  }

  @Test
  public void do_not_set_variations_on_numeric_metric_for_developer() {
    SnapshotDto period1ProjectSnapshot = newSnapshotForProject(PROJECT_DTO);
    dbClient.snapshotDao().insert(session, period1ProjectSnapshot);
    dbClient.measureDao().insert(session,
      newMeasureDto(ISSUES_METRIC.getId(), PROJECT_DTO.getId(), period1ProjectSnapshot.getId(), 60d)
      );
    session.commit();

    periodsHolder.setPeriods(newPeriod(1, period1ProjectSnapshot));

    treeRootHolder.setRoot(PROJECT);

    DumbDeveloper developer = new DumbDeveloper("a");
    measureRepository.addRawMeasure(PROJECT_REF, ISSUES_METRIC.getKey(), newMeasureBuilder().forDeveloper(developer).create(80, null));

    underTest.execute();

    assertThat(measureRepository.getRawMeasures(PROJECT).keys()).hasSize(1);

    assertThat(measureRepository.getRawMeasure(PROJECT, ISSUES_METRIC, developer).get().hasVariations()).isFalse();
  }

  private static MeasureDto newMeasureDto(int metricId, long projectId, long snapshotId, double value) {
    return new MeasureDto().setMetricId(metricId).setComponentId(projectId).setSnapshotId(snapshotId).setValue(value);
  }

  private static Period newPeriod(int index, SnapshotDto snapshotDto) {
    return new Period(index, "mode", null, snapshotDto.getCreatedAt(), snapshotDto.getId());
  }

  private void addRawMeasure(Component component, Metric metric, Measure measure) {
    measureRepository.addRawMeasure(component.getReportAttributes().getRef(), metric.getKey(), measure);
  }
}
