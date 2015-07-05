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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.batch.protocol.Constants.MeasureValueType;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.measure.MeasureRepositoryImpl;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.db.DbClient;
import org.sonar.db.measure.MeasureDao;
import org.sonar.server.metric.persistence.MetricDao;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.DUPLICATIONS_DATA;
import static org.sonar.api.measures.CoreMetrics.DUPLICATIONS_DATA_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;

@Category(DbTests.class)
public class PersistMeasuresStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String STRING_METRIC_KEY = "string-metric-key";
  private static final String DOUBLE_METRIC_KEY = "double-metric-key";
  private static final String OPTIMIZED_METRIC_KEY = "optimized-metric-key";

  private static final Metric STRING_METRIC = new Metric.Builder(STRING_METRIC_KEY, "String metric", Metric.ValueType.STRING).create();
  private static final Metric DOUBLE_METRIC = new Metric.Builder(DOUBLE_METRIC_KEY, "Double metric", Metric.ValueType.FLOAT).create();

  private static final int PROJECT_REF = 1;
  private static final int FILE_REF = 2;

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule();

  DbClient dbClient;
  DbSession session;
  DbIdsRepository dbIdsRepository = new DbIdsRepository();
  RuleDto rule;
  ComponentDto projectDto;
  ComponentDto fileDto;

  PersistMeasuresStep sut;

  @Before
  public void setUp() {
    dbTester.truncateTables();

    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new MeasureDao(), new ComponentDao(), new MetricDao(), new RuleDao(System2.INSTANCE));
    session = dbClient.openSession(false);

    MeasureRepository measureRepository = new MeasureRepositoryImpl(dbClient, reportReader, metricRepository);
    session.commit();

    sut = new PersistMeasuresStep(dbClient, metricRepository, dbIdsRepository, treeRootHolder, measureRepository);

    projectDto = addComponent("project-key");
    fileDto = addComponent("file-key");

    Component file = DumbComponent.builder(Component.Type.FILE, FILE_REF).setUuid("CDEF").setKey("MODULE_KEY:file").build();
    Component project = DumbComponent.builder(Component.Type.PROJECT, PROJECT_REF).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(file).build();
    treeRootHolder.setRoot(project);

    dbIdsRepository.setComponentId(project, projectDto.getId());
    dbIdsRepository.setSnapshotId(project, 3L);
    dbIdsRepository.setComponentId(file, fileDto.getId());
    dbIdsRepository.setSnapshotId(file, 4L);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void insert_measures_from_report() throws Exception {
    metricRepository.add(1, STRING_METRIC);
    metricRepository.add(2, DOUBLE_METRIC);

    reportReader.putMeasures(PROJECT_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.STRING)
        .setStringValue("measure-data")
        .setVariationValue1(1.1d)
        .setVariationValue2(2.2d)
        .setVariationValue3(3.3d)
        .setVariationValue4(4.4d)
        .setVariationValue5(5.5d)
        .setDescription("measure-description")
        .setMetricKey(STRING_METRIC_KEY)
        .build()));

    reportReader.putMeasures(FILE_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.DOUBLE)
        .setDoubleValue(123.123d)
        .setVariationValue1(1.1d)
        .setVariationValue2(2.2d)
        .setVariationValue3(3.3d)
        .setVariationValue4(4.4d)
        .setVariationValue5(5.5d)
        .setDescription("measure-description")
        .setMetricKey(DOUBLE_METRIC_KEY)
        .build()));

    sut.execute();
    session.commit();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(2);

    List<Map<String, Object>> dtos = retrieveDtos();

    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("snapshotId")).isEqualTo(3L);
    assertThat(dto.get("componentId")).isEqualTo(projectDto.getId());
    assertThat(dto.get("metricId")).isEqualTo(1L);
    assertThat(dto.get("textValue")).isEqualTo("measure-data");
    assertThat(dto.get("severity")).isNull();

    dto = dtos.get(PROJECT_REF);
    assertThat(dto.get("snapshotId")).isEqualTo(4L);
    assertThat(dto.get("componentId")).isEqualTo(fileDto.getId());
    assertThat(dto.get("metricId")).isEqualTo(2L);
    assertThat(dto.get("value")).isEqualTo(123.1d);
    assertThat(dto.get("severity")).isNull();
  }

  private List<Map<String, Object>> retrieveDtos() {
    return dbTester.select(
      "select snapshot_id as \"snapshotId\", project_id as \"componentId\", metric_id as \"metricId\", rule_id as \"ruleId\", value as \"value\", text_value as \"textValue\", " +
        "rule_priority as \"severity\" from project_measures");
  }

  @Test
  public void bestValue_measure_of_bestValueOptimized_metrics_are_not_persisted() {
    metricRepository.add(1, new Metric.Builder(OPTIMIZED_METRIC_KEY, "Optimized metric", Metric.ValueType.BOOL).setOptimizedBestValue(true).setBestValue(1d).create());

    reportReader.putMeasures(FILE_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.BOOLEAN)
        .setBooleanValue(true)
        .setMetricKey(OPTIMIZED_METRIC_KEY)
        .build()));

    sut.execute();
    session.commit();

    assertThat(retrieveDtos()).isEmpty();
  }

  @Test
  public void empty_values_are_not_persisted() {
    metricRepository.add(1, STRING_METRIC);
    metricRepository.add(2, DOUBLE_METRIC);

    reportReader.putMeasures(FILE_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.STRING)
        .setMetricKey(STRING_METRIC_KEY)
        .build(),
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.DOUBLE)
        .setMetricKey(DOUBLE_METRIC_KEY)
        .build()
      ));

    sut.execute();
    session.commit();

    assertThat(retrieveDtos()).isEmpty();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_with_ISE_when_trying_to_insert_forbidden_measures() throws Exception {
    metricRepository.add(1, DUPLICATIONS_DATA);

    reportReader.putMeasures(FILE_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.STRING)
        .setStringValue("{duplications}")
        .setMetricKey(DUPLICATIONS_DATA_KEY)
        .build()));

    sut.execute();
  }

  @Test
  public void do_not_insert_file_complexity_distribution_metric_on_files() throws Exception {
    metricRepository.add(1, FILE_COMPLEXITY_DISTRIBUTION);

    reportReader.putMeasures(PROJECT_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.STRING)
        .setStringValue("0=1;2=10")
        .setMetricKey(FILE_COMPLEXITY_DISTRIBUTION_KEY)
        .build()));

    // Should not be persisted
    reportReader.putMeasures(FILE_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.STRING)
        .setStringValue("0=1;2=10")
        .setMetricKey(FILE_COMPLEXITY_DISTRIBUTION_KEY)
        .build()));

    sut.execute();

    session.commit();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    List<Map<String, Object>> dtos = retrieveDtos();

    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("snapshotId")).isEqualTo(3L);
    assertThat(dto.get("componentId")).isEqualTo(projectDto.getId());
    assertThat(dto.get("textValue")).isEqualTo("0=1;2=10");
  }

  @Test
  public void do_not_insert_function_complexity_distribution_metric_on_files() throws Exception {
    metricRepository.add(1, FUNCTION_COMPLEXITY_DISTRIBUTION);

    reportReader.putMeasures(PROJECT_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.STRING)
        .setStringValue("0=1;2=10")
        .setMetricKey(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY)
        .build()));

    // Should not be persisted
    reportReader.putMeasures(FILE_REF, Arrays.asList(
      BatchReport.Measure.newBuilder()
        .setValueType(MeasureValueType.STRING)
        .setStringValue("0=1;2=10")
        .setMetricKey(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY)
        .build()));

    sut.execute();

    session.commit();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    List<Map<String, Object>> dtos = retrieveDtos();

    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("snapshotId")).isEqualTo(3L);
    assertThat(dto.get("componentId")).isEqualTo(projectDto.getId());
    assertThat(dto.get("textValue")).isEqualTo("0=1;2=10");
  }

  private ComponentDto addComponent(String key) {
    ComponentDto componentDto = new ComponentDto().setKey(key).setUuid(Uuids.create());
    dbClient.componentDao().insert(session, componentDto);
    session.commit();
    return componentDto;
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }
}
