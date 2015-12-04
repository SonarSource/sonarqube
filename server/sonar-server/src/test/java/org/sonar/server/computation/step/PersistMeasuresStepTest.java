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

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.Developer;
import org.sonar.server.computation.component.DumbDeveloper;
import org.sonar.server.computation.component.MutableDbIdsRepositoryRule;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.component.ViewsComponent;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.measure.MeasureToMeasureDto;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.period.Period;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.MODULE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.component.Component.Type.VIEW;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.measure.MeasureVariations.newMeasureVariationsBuilder;

@Category(DbTests.class)
public class PersistMeasuresStepTest extends BaseStepTest {

  private static final String STRING_METRIC_KEY = "string-metric-key";
  private static final String DOUBLE_METRIC_KEY = "double-metric-key";
  private static final String INT_METRIC_KEY = "int-metric-key";
  private static final String LONG_METRIC_KEY = "long-metric-key";
  private static final String OPTIMIZED_METRIC_KEY = "optimized-metric-key";

  private static final Metric STRING_METRIC = new Metric.Builder(STRING_METRIC_KEY, "String metric", Metric.ValueType.STRING).create();
  private static final Metric DOUBLE_METRIC = new Metric.Builder(DOUBLE_METRIC_KEY, "Double metric", Metric.ValueType.FLOAT).create();
  private static final Metric INT_METRIC = new Metric.Builder(INT_METRIC_KEY, "int metric", Metric.ValueType.INT).create();
  private static final Metric LONG_METRIC = new Metric.Builder(LONG_METRIC_KEY, "long metric", Metric.ValueType.WORK_DUR).create();

  private static final int ROOT_REF = 1;
  private static final int INTERMEDIATE_1_REF = 2;
  private static final int INTERMEDIATE_2_REF = 3;
  private static final int LEAF_REF = 4;
  private static final long ROOT_SNAPSHOT_ID = 3L;
  private static final long INTERMEDIATE_1_SNAPSHOT_ID = 4L;
  private static final long INTERMEDIATE_2_SNAPSHOT_ID = 5L;
  private static final long LEAF_SNAPSHOT_ID = 6L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule();
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public MutableDbIdsRepositoryRule dbIdsRepository = MutableDbIdsRepositoryRule.create(treeRootHolder);

  DbClient dbClient = dbTester.getDbClient();
  RuleDto rule;
  ComponentDto rootDto;
  ComponentDto intermediate1Dto;
  ComponentDto intermediate2Dto;
  ComponentDto leafDto;

  PersistMeasuresStep underTest;

  @Before
  public void setUp() {
    dbTester.truncateTables();

    underTest = new PersistMeasuresStep(dbClient, metricRepository, new MeasureToMeasureDto(dbIdsRepository), treeRootHolder, measureRepository);
  }

  private void setupReportComponents() {
    Component project = ReportComponent.builder(PROJECT, ROOT_REF)
      .addChildren(
        ReportComponent.builder(MODULE, INTERMEDIATE_1_REF)
          .addChildren(
            ReportComponent.builder(DIRECTORY, INTERMEDIATE_2_REF)
              .addChildren(
                ReportComponent.builder(FILE, LEAF_REF)
                  .build())
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(project);

    setupDbIds();
  }

  private void setupViewsComponents() {
    Component view = ViewsComponent.builder(VIEW, ROOT_REF)
      .addChildren(
        ViewsComponent.builder(SUBVIEW, INTERMEDIATE_1_REF)
          .addChildren(
            ViewsComponent.builder(SUBVIEW, INTERMEDIATE_2_REF)
              .addChildren(
                ViewsComponent.builder(PROJECT_VIEW, LEAF_REF)
                  .build())
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(view);

    setupDbIds();
  }

  private void setupDbIds() {
    rootDto = addComponent("root-key");
    intermediate1Dto = addComponent("intermediate1-key");
    intermediate2Dto = addComponent("intermediate2-key");
    leafDto = addComponent("leaf-key");

    setDbIds(ROOT_REF, rootDto.getId(), ROOT_SNAPSHOT_ID);
    setDbIds(INTERMEDIATE_1_REF, intermediate1Dto.getId(), INTERMEDIATE_1_SNAPSHOT_ID);
    setDbIds(INTERMEDIATE_2_REF, intermediate2Dto.getId(), INTERMEDIATE_2_SNAPSHOT_ID);
    setDbIds(LEAF_REF, leafDto.getId(), LEAF_SNAPSHOT_ID);
  }

  private void setDbIds(int componentRef, Long dbId, long snapshotId) {
    dbIdsRepository.setComponentId(componentRef, dbId);
    dbIdsRepository.setSnapshotId(componentRef, snapshotId);
  }

  @Test
  public void insert_measures_from_report() {
    setupReportComponents();

    insertMeasures();
  }

  @Test
  public void insert_measures_from_views() {
    setupViewsComponents();

    insertMeasures();
  }

  private void insertMeasures() {
    int stringMetricId = 1;
    int doubleMetricId = 2;
    int intMetricId = 3;
    int longMetricId = 4;
    metricRepository.add(stringMetricId, STRING_METRIC);
    metricRepository.add(doubleMetricId, DOUBLE_METRIC);
    metricRepository.add(intMetricId, INT_METRIC);
    metricRepository.add(longMetricId, LONG_METRIC);

    measureRepository.addRawMeasure(ROOT_REF, STRING_METRIC_KEY, newMeasureBuilder().create("measure-data"));
    measureRepository.addRawMeasure(INTERMEDIATE_1_REF, INT_METRIC_KEY, newMeasureBuilder().create(12));
    measureRepository.addRawMeasure(INTERMEDIATE_2_REF, LONG_METRIC_KEY, newMeasureBuilder().create(9635L));
    measureRepository.addRawMeasure(LEAF_REF, DOUBLE_METRIC_KEY, newMeasureBuilder().create(123.123d, 1));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(4);

    List<Map<String, Object>> dtos = selectSnapshots();

    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("snapshotId")).isEqualTo(ROOT_SNAPSHOT_ID);
    assertThat(dto.get("componentId")).isEqualTo(rootDto.getId());
    assertThat(dto.get("metricId")).isEqualTo((long) stringMetricId);
    assertThat(dto.get("value")).isNull();
    assertThat(dto.get("textValue")).isEqualTo("measure-data");
    assertThat(dto.get("severity")).isNull();

    dto = dtos.get(1);
    assertThat(dto.get("snapshotId")).isEqualTo(INTERMEDIATE_1_SNAPSHOT_ID);
    assertThat(dto.get("componentId")).isEqualTo(intermediate1Dto.getId());
    assertThat(dto.get("metricId")).isEqualTo((long) intMetricId);
    assertValue(dto, 12d);
    assertThat(dto.get("textValue")).isNull();
    assertThat(dto.get("severity")).isNull();

    dto = dtos.get(2);
    assertThat(dto.get("snapshotId")).isEqualTo(INTERMEDIATE_2_SNAPSHOT_ID);
    assertThat(dto.get("componentId")).isEqualTo(intermediate2Dto.getId());
    assertThat(dto.get("metricId")).isEqualTo((long) longMetricId);
    assertValue(dto, 9635d);
    assertThat(dto.get("textValue")).isNull();
    assertThat(dto.get("severity")).isNull();

    dto = dtos.get(3);
    assertThat(dto.get("snapshotId")).isEqualTo(LEAF_SNAPSHOT_ID);
    assertThat(dto.get("componentId")).isEqualTo(leafDto.getId());
    assertThat(dto.get("metricId")).isEqualTo((long) doubleMetricId);
    assertValue(dto, 123.1d);
    assertThat(dto.get("textValue")).isNull();
    assertThat(dto.get("severity")).isNull();
  }

  /**
   * Horrible trick to support oracle retuning number as BigDecimal and DbTester#select converting BigDecimal with no
   * scale to Long instead of Double when all other DBs will return a Double anyway.
   */
  private static void assertValue(Map<String, Object> dto, double expected) {
    Object actual = dto.get("value");
    if (expected % 1 == 0d && actual instanceof Long) {
      assertThat(actual).isEqualTo((long) expected);
    } else {
      assertThat(actual).isEqualTo(expected);
    }
  }

  @Test
  public void insert_measure_with_variations_from_report() {
    setupReportComponents();

    insertMeasureWithVariations();
  }

  @Test
  public void insert_measure_with_variations_from_views() {
    setupViewsComponents();

    insertMeasureWithVariations();
  }

  private void insertMeasureWithVariations() {
    metricRepository.add(1, DOUBLE_METRIC);

    measureRepository.addRawMeasure(ROOT_REF, DOUBLE_METRIC_KEY,
      newMeasureBuilder()
        .setVariations(
          newMeasureVariationsBuilder()
            .setVariation(createPeriod(1), 1.1d)
            .setVariation(createPeriod(2), 2.2d)
            .setVariation(createPeriod(3), 3.3d)
            .setVariation(createPeriod(4), 4.4d)
            .setVariation(createPeriod(5), 5.5d)
            .build())
        .create(10d, 1));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);
    List<Map<String, Object>> dtos = selectSnapshots();
    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("variation_value_1")).isEqualTo(1.1d);
    assertThat(dto.get("variation_value_2")).isEqualTo(2.2d);
    assertThat(dto.get("variation_value_3")).isEqualTo(3.3d);
    assertThat(dto.get("variation_value_4")).isEqualTo(4.4d);
    assertThat(dto.get("variation_value_5")).isEqualTo(5.5d);
  }

  @Test
  public void insert_rule_measure_from_report() {
    setupReportComponents();

    insertRuleMeasure();
  }

  @Test
  public void insert_rule_measure_from_view() {
    setupViewsComponents();

    insertRuleMeasure();
  }

  private void insertRuleMeasure() {
    metricRepository.add(1, INT_METRIC);

    measureRepository.addRawMeasure(ROOT_REF, INT_METRIC_KEY, newMeasureBuilder().forRule(10).create(1));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);
    List<Map<String, Object>> dtos = selectSnapshots();
    Map<String, Object> dto = dtos.get(0);

    assertValue(dto, 1d);
    assertThat(dto.get("ruleId")).isEqualTo(10L);
  }

  @Test
  public void insert_characteristic_measure_from_report() {
    setupReportComponents();

    insertCharacteristicMeasure();
  }

  @Test
  public void insert_characteristic__measure_from_view() {
    setupViewsComponents();

    insertCharacteristicMeasure();
  }

  private void insertCharacteristicMeasure() {
    metricRepository.add(1, INT_METRIC);

    measureRepository.addRawMeasure(ROOT_REF, INT_METRIC_KEY, newMeasureBuilder().forCharacteristic(10).create(1));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);
    List<Map<String, Object>> dtos = selectSnapshots();
    Map<String, Object> dto = dtos.get(0);

    assertValue(dto, 1d);
    assertThat(dto.get("characteristicId")).isEqualTo(10L);
  }

  @Test
  public void bestValue_measure_of_bestValueOptimized_metrics_are_not_persisted() {
    setupReportComponents();

    metricRepository.add(1, new Metric.Builder(OPTIMIZED_METRIC_KEY, "Optimized metric", Metric.ValueType.BOOL).setOptimizedBestValue(true).setBestValue(1d).create());

    measureRepository.addRawMeasure(LEAF_REF, OPTIMIZED_METRIC_KEY, newMeasureBuilder().create(true));

    underTest.execute();

    assertThat(selectSnapshots()).isEmpty();
  }

  @Test
  public void empty_values_are_not_persisted() {
    setupReportComponents();

    metricRepository.add(1, STRING_METRIC);
    metricRepository.add(2, DOUBLE_METRIC);

    measureRepository.addRawMeasure(LEAF_REF, STRING_METRIC_KEY, newMeasureBuilder().createNoValue());
    measureRepository.addRawMeasure(LEAF_REF, DOUBLE_METRIC_KEY, newMeasureBuilder().createNoValue());

    underTest.execute();

    assertThat(selectSnapshots()).isEmpty();
  }

  @Test
  public void do_not_insert_file_complexity_distribution_metric_on_files() {
    setupReportComponents();

    metricRepository.add(1, FILE_COMPLEXITY_DISTRIBUTION);

    measureRepository.addRawMeasure(ROOT_REF, FILE_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0=1;2=10"));
    measureRepository.addRawMeasure(LEAF_REF, FILE_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0=1;2=10"));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    List<Map<String, Object>> dtos = selectSnapshots();

    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("snapshotId")).isEqualTo(ROOT_SNAPSHOT_ID);
    assertThat(dto.get("componentId")).isEqualTo(rootDto.getId());
    assertThat(dto.get("textValue")).isEqualTo("0=1;2=10");
  }

  @Test
  public void do_not_insert_function_complexity_distribution_metric_on_files() {
    setupReportComponents();

    metricRepository.add(1, FUNCTION_COMPLEXITY_DISTRIBUTION);

    measureRepository.addRawMeasure(ROOT_REF, FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0=1;2=10"));
    measureRepository.addRawMeasure(LEAF_REF, FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0=1;2=10"));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    List<Map<String, Object>> dtos = selectSnapshots();

    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("snapshotId")).isEqualTo(ROOT_SNAPSHOT_ID);
    assertThat(dto.get("componentId")).isEqualTo(rootDto.getId());
    assertThat(dto.get("textValue")).isEqualTo("0=1;2=10");
  }

  @Test
  public void do_not_insert_class_complexity_distribution_metric_on_files() {
    setupReportComponents();

    metricRepository.add(1, CLASS_COMPLEXITY_DISTRIBUTION);

    measureRepository.addRawMeasure(ROOT_REF, CLASS_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0=1;2=10"));
    measureRepository.addRawMeasure(LEAF_REF, CLASS_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("0=1;2=10"));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);

    List<Map<String, Object>> dtos = selectSnapshots();

    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("snapshotId")).isEqualTo(ROOT_SNAPSHOT_ID);
    assertThat(dto.get("componentId")).isEqualTo(rootDto.getId());
    assertThat(dto.get("textValue")).isEqualTo("0=1;2=10");
  }

  @Test
  public void insert_developer_measure_from_report() {
    setupReportComponents();

    metricRepository.add(1, INT_METRIC);

    Developer developer = new DumbDeveloper("DEV1");
    dbIdsRepository.setDeveloperId(developer, 10);
    measureRepository.addRawMeasure(ROOT_REF, INT_METRIC_KEY, newMeasureBuilder().forDeveloper(developer).create(1));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);
    List<Map<String, Object>> dtos = selectSnapshots();
    Map<String, Object> dto = dtos.get(0);

    assertValue(dto, 1d);
    assertThat(dto.get("developerId")).isEqualTo(10L);
  }

  private ComponentDto addComponent(String key) {
    ComponentDto componentDto = new ComponentDto().setKey(key).setUuid(Uuids.create());
    dbClient.componentDao().insert(dbTester.getSession(), componentDto);
    return componentDto;
  }

  private static Period createPeriod(Integer index) {
    return new Period(index, "mode" + index, null, index, index);
  }

  private List<Map<String, Object>> selectSnapshots() {
    return dbTester
      .select(
      "SELECT snapshot_id as \"snapshotId\", project_id as \"componentId\", metric_id as \"metricId\", rule_id as \"ruleId\", characteristic_id as \"characteristicId\", person_id as \"developerId\", "
        +
        "value as \"value\", text_value as \"textValue\", " +
        "rule_priority as \"severity\", " +
        "variation_value_1 as \"variation_value_1\", " +
        "variation_value_2 as \"variation_value_2\", " +
        "variation_value_3 as \"variation_value_3\", " +
        "variation_value_4 as \"variation_value_4\", " +
        "variation_value_5 as \"variation_value_5\"" +
        "FROM project_measures " +
        "ORDER by snapshot_id asc");
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }
}
