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

import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.debt.CharacteristicDao;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.db.component.SnapshotDao;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureRepositoryImpl;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.Metric.MetricType;
import org.sonar.server.computation.metric.MetricImpl;
import org.sonar.server.computation.metric.MetricRepositoryImpl;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;
import org.sonar.server.db.DbClient;
import org.sonar.db.measure.MeasureDao;
import org.sonar.server.metric.persistence.MetricDao;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.component.SnapshotTesting.createForComponent;
import static org.sonar.server.component.SnapshotTesting.createForProject;
import static org.sonar.server.computation.metric.Metric.MetricType.BOOL;
import static org.sonar.server.computation.metric.Metric.MetricType.FLOAT;
import static org.sonar.server.computation.metric.Metric.MetricType.INT;
import static org.sonar.server.metric.ws.MetricTesting.newMetricDto;

@Category(DbTests.class)
public class FillMeasuresWithVariationsStepTest {

  static final MetricDto ISSUES_METRIC = newMetricDto().setKey("violations").setValueType(INT.name()).setEnabled(true);
  static final MetricDto DEBT_METRIC = newMetricDto().setKey("sqale_index").setValueType(MetricType.WORK_DUR.name()).setEnabled(true);
  static final MetricDto FILE_COMPLEXITY_METRIC = newMetricDto().setKey("file_complexity").setValueType(FLOAT.name()).setEnabled(true);
  static final MetricDto BUILD_BREAKER_METRIC = newMetricDto().setKey("build_breaker").setValueType(BOOL.name()).setEnabled(true);

  static final ComponentDto PROJECT_DTO = ComponentTesting.newProjectDto();

  static final Component PROJECT = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_DTO.uuid()).build();

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  MeasureRepository measureRepository;

  MetricRepositoryImpl metricRepository;

  DbSession session;

  DbClient dbClient;

  FillMeasuresWithVariationsStep sut;

  @Before
  public void setUp() throws Exception {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao(), new SnapshotDao(), new MetricDao(), new MeasureDao(), new RuleDao(System2.INSTANCE),
      new CharacteristicDao(dbTester.myBatis()));

    dbClient.metricDao().insert(session, ISSUES_METRIC, DEBT_METRIC, FILE_COMPLEXITY_METRIC, BUILD_BREAKER_METRIC);
    dbClient.componentDao().insert(session, PROJECT_DTO);
    session.commit();

    metricRepository = new MetricRepositoryImpl(dbClient);
    metricRepository.start();
    measureRepository = new MeasureRepositoryImpl(dbClient, reportReader, metricRepository);

    sut = new FillMeasuresWithVariationsStep(dbClient, treeRootHolder, periodsHolder, metricRepository, measureRepository);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void do_nothing_when_no_raw_measure() throws Exception {
    SnapshotDto period1ProjectSnapshot = createForProject(PROJECT_DTO);
    dbClient.snapshotDao().insert(session, period1ProjectSnapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), PROJECT_DTO.getId(), period1ProjectSnapshot.getId(), 60d));
    session.commit();

    periodsHolder.setPeriods(newPeriod(1, period1ProjectSnapshot));

    treeRootHolder.setRoot(PROJECT);

    sut.execute();

    assertThat(measureRepository.getRawMeasures(PROJECT).keys()).isEmpty();
  }

  @Test
  public void do_nothing_when_no_period() throws Exception {
    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_DTO.uuid()).build();
    treeRootHolder.setRoot(project);
    periodsHolder.setPeriods();

    sut.execute();

    assertThat(measureRepository.getRawMeasures(project).keys()).isEmpty();
  }

  @Test
  public void set_variation() throws Exception {
    // Project
    SnapshotDto period1ProjectSnapshot = createForProject(PROJECT_DTO);
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

    Component directory = DumbComponent.builder(Component.Type.DIRECTORY, 2).setUuid(directoryDto.uuid()).build();
    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid(PROJECT_DTO.uuid()).addChildren(directory).build();
    treeRootHolder.setRoot(project);

    addRawMeasure(project, ISSUES_METRIC, Measure.newMeasureBuilder().create(80, null));
    addRawMeasure(directory, ISSUES_METRIC, Measure.newMeasureBuilder().create(20, null));

    sut.execute();

    assertThat(measureRepository.getRawMeasure(project, toMetric(ISSUES_METRIC)).get().getVariations().getVariation1()).isEqualTo(20d);
    assertThat(measureRepository.getRawMeasure(directory, toMetric(ISSUES_METRIC)).get().getVariations().getVariation1()).isEqualTo(10d);
  }

  @Test
  public void set_variations_on_all_periods() throws Exception {
    SnapshotDto period1ProjectSnapshot = createForProject(PROJECT_DTO).setLast(false);
    SnapshotDto period2ProjectSnapshot = createForProject(PROJECT_DTO).setLast(false);
    SnapshotDto period3ProjectSnapshot = createForProject(PROJECT_DTO).setLast(false);
    SnapshotDto period4ProjectSnapshot = createForProject(PROJECT_DTO).setLast(false);
    SnapshotDto period5ProjectSnapshot = createForProject(PROJECT_DTO).setLast(false);
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

    addRawMeasure(PROJECT, ISSUES_METRIC, Measure.newMeasureBuilder().create(80, null));

    sut.execute();

    assertThat(measureRepository.getRawMeasures(PROJECT).keys()).hasSize(1);

    Measure measure = measureRepository.getRawMeasure(PROJECT, toMetric(ISSUES_METRIC)).get();
    assertThat(measure.hasVariations()).isTrue();
    assertThat(measure.getVariations().getVariation1()).isEqualTo(80d);
    assertThat(measure.getVariations().getVariation2()).isEqualTo(60d);
    assertThat(measure.getVariations().getVariation3()).isEqualTo(40d);
    assertThat(measure.getVariations().getVariation4()).isEqualTo(0d);
    assertThat(measure.getVariations().getVariation5()).isEqualTo(-20d);
  }

  @Test
  public void set_variation_on_all_numeric_metrics() throws Exception {
    SnapshotDto period1ProjectSnapshot = createForProject(PROJECT_DTO);
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

    addRawMeasure(PROJECT, ISSUES_METRIC, Measure.newMeasureBuilder().create(80, null));
    addRawMeasure(PROJECT, DEBT_METRIC, Measure.newMeasureBuilder().create(5L, null));
    addRawMeasure(PROJECT, FILE_COMPLEXITY_METRIC, Measure.newMeasureBuilder().create(3d, null));
    addRawMeasure(PROJECT, BUILD_BREAKER_METRIC, Measure.newMeasureBuilder().create(false, null));

    sut.execute();

    assertThat(measureRepository.getRawMeasures(PROJECT).keys()).hasSize(4);

    assertThat(measureRepository.getRawMeasure(PROJECT, toMetric(ISSUES_METRIC)).get().getVariations().getVariation1()).isEqualTo(20d);
    assertThat(measureRepository.getRawMeasure(PROJECT, toMetric(DEBT_METRIC)).get().getVariations().getVariation1()).isEqualTo(-5d);
    assertThat(measureRepository.getRawMeasure(PROJECT, toMetric(FILE_COMPLEXITY_METRIC)).get().getVariations().getVariation1()).isEqualTo(1d);
    assertThat(measureRepository.getRawMeasure(PROJECT, toMetric(BUILD_BREAKER_METRIC)).get().getVariations().getVariation1()).isEqualTo(-1d);
  }

  @Test
  public void read_measure_from_batch() throws Exception {
    // Project
    SnapshotDto period1ProjectSnapshot = createForProject(PROJECT_DTO);
    dbClient.snapshotDao().insert(session, period1ProjectSnapshot);
    dbClient.measureDao().insert(session, newMeasureDto(ISSUES_METRIC.getId(), PROJECT_DTO.getId(), period1ProjectSnapshot.getId(), 60d));
    session.commit();

    periodsHolder.setPeriods(newPeriod(1, period1ProjectSnapshot));

    treeRootHolder.setRoot(PROJECT);

    reportReader.putMeasures(PROJECT.getRef(), Collections.singletonList(
      BatchReport.Measure.newBuilder().setIntValue(80).setMetricKey(ISSUES_METRIC.getKey()).build())
      );

    sut.execute();

    assertThat(measureRepository.getRawMeasure(PROJECT, toMetric(ISSUES_METRIC)).get().getVariations().getVariation1()).isEqualTo(20d);
  }

  private static MeasureDto newMeasureDto(int metricId, long projectId, long snapshotId, double value) {
    return new MeasureDto().setMetricId(metricId).setComponentId(projectId).setSnapshotId(snapshotId).setValue(value);
  }

  private static Period newPeriod(int index, SnapshotDto snapshotDto) {
    return new Period(index, "mode", null, snapshotDto.getCreatedAt(), snapshotDto.getId());
  }

  private void addRawMeasure(Component component, MetricDto metric, Measure measure) {
    measureRepository.add(component, new MetricImpl(metric.getId(), metric.getKey(), metric.getShortName(), MetricType.valueOf(metric.getValueType())), measure);
  }

  private static Metric toMetric(MetricDto metric) {
    return new MetricImpl(metric.getId(), metric.getKey(), metric.getShortName(), Metric.MetricType.valueOf(metric.getValueType()));
  }
}
