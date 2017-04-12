/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.computation.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.Developer;
import org.sonar.server.computation.task.projectanalysis.component.DumbDeveloper;
import org.sonar.server.computation.task.projectanalysis.component.MutableDbIdsRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.ViewsComponent;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureToMeasureDto;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.CLASS_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.MODULE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

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
  private static final String ANALYSIS_UUID = "a1";

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

  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();

  DbClient dbClient = dbTester.getDbClient();
  RuleDto rule;
  ComponentDto rootDto;
  ComponentDto intermediate1Dto;
  ComponentDto intermediate2Dto;
  ComponentDto leafDto;

  PersistMeasuresStep underTest;

  @Before
  public void setUp() {
    underTest = new PersistMeasuresStep(dbClient, metricRepository, new MeasureToMeasureDto(dbIdsRepository, analysisMetadataHolder), treeRootHolder, measureRepository);
    analysisMetadataHolder.setUuid(ANALYSIS_UUID);
  }

  private void setupReportComponents() {
    Component project = ReportComponent.builder(PROJECT, ROOT_REF).setUuid("root-uuid")
      .addChildren(
        ReportComponent.builder(MODULE, INTERMEDIATE_1_REF).setUuid("intermediate1-uuid")
          .addChildren(
            ReportComponent.builder(DIRECTORY, INTERMEDIATE_2_REF).setUuid("intermediate2-uuid")
              .addChildren(
                ReportComponent.builder(FILE, LEAF_REF).setUuid("leaf-uuid")
                  .build())
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(project);

    setupDbIds();
  }

  private void setupViewsComponents() {
    Component view = ViewsComponent.builder(VIEW, ROOT_REF).setUuid("root-uuid")
      .addChildren(
        ViewsComponent.builder(SUBVIEW, INTERMEDIATE_1_REF).setUuid("intermediate1-uuid")
          .addChildren(
            ViewsComponent.builder(SUBVIEW, INTERMEDIATE_2_REF).setUuid("intermediate2-uuid")
              .addChildren(
                ViewsComponent.builder(PROJECT_VIEW, LEAF_REF).setUuid("leaf-uuid")
                  .build())
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(view);

    setupDbIds();
  }

  private void setupDbIds() {
    rootDto = addComponent("root-key", "root-uuid");
    intermediate1Dto = addComponent("intermediate1-key", "intermediate1-uuid");
    intermediate2Dto = addComponent("intermediate2-key", "intermediate2-uuid");
    leafDto = addComponent("leaf-key", "leaf-uuid");

    setDbIds(ROOT_REF, rootDto.getId());
    setDbIds(INTERMEDIATE_1_REF, intermediate1Dto.getId());
    setDbIds(INTERMEDIATE_2_REF, intermediate2Dto.getId());
    setDbIds(LEAF_REF, leafDto.getId());
  }

  private void setDbIds(int componentRef, Long dbId) {
    dbIdsRepository.setComponentId(componentRef, dbId);
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
    assertThat(dto.get("analysisUuid")).isEqualTo(ANALYSIS_UUID);
    assertThat(dto.get("componentUuid")).isEqualTo(rootDto.uuid());
    assertThat(dto.get("metricId")).isEqualTo((long) stringMetricId);
    assertThat(dto.get("value")).isNull();
    assertThat(dto.get("textValue")).isEqualTo("measure-data");
    assertThat(dto.get("severity")).isNull();

    dto = dtos.get(1);
    assertThat(dto.get("analysisUuid")).isEqualTo(ANALYSIS_UUID);
    assertThat(dto.get("componentUuid")).isEqualTo(intermediate1Dto.uuid());
    assertThat(dto.get("metricId")).isEqualTo((long) intMetricId);
    assertValue(dto, 12d);
    assertThat(dto.get("textValue")).isNull();
    assertThat(dto.get("severity")).isNull();

    dto = dtos.get(2);
    assertThat(dto.get("analysisUuid")).isEqualTo(ANALYSIS_UUID);
    assertThat(dto.get("componentUuid")).isEqualTo(intermediate2Dto.uuid());
    assertThat(dto.get("metricId")).isEqualTo((long) longMetricId);
    assertValue(dto, 9635d);
    assertThat(dto.get("textValue")).isNull();
    assertThat(dto.get("severity")).isNull();

    dto = dtos.get(3);
    assertThat(dto.get("analysisUuid")).isEqualTo(ANALYSIS_UUID);
    assertThat(dto.get("componentUuid")).isEqualTo(leafDto.uuid());
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
        .setVariation(1.1d)
        .create(10d, 1));

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("project_measures")).isEqualTo(1);
    List<Map<String, Object>> dtos = selectSnapshots();
    Map<String, Object> dto = dtos.get(0);
    assertThat(dto.get("variation_value")).isEqualTo(1.1d);
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
    assertThat(dto.get("analysisUuid")).isEqualTo(ANALYSIS_UUID);
    assertThat(dto.get("componentUuid")).isEqualTo(rootDto.uuid());
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
    assertThat(dto.get("analysisUuid")).isEqualTo(ANALYSIS_UUID);
    assertThat(dto.get("componentUuid")).isEqualTo(rootDto.uuid());
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
    assertThat(dto.get("analysisUuid")).isEqualTo(ANALYSIS_UUID);
    assertThat(dto.get("componentUuid")).isEqualTo(rootDto.uuid());
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

  private ComponentDto addComponent(String key, String uuid) {
    ComponentDto componentDto = new ComponentDto()
      .setOrganizationUuid("org1")
      .setKey(key)
      .setUuid(uuid)
      .setUuidPath(uuid + ".")
      .setRootUuid(uuid)
      .setProjectUuid(uuid);
    dbClient.componentDao().insert(dbTester.getSession(), componentDto);
    return componentDto;
  }

  private List<Map<String, Object>> selectSnapshots() {
    return dbTester
      .select(
        "SELECT analysis_uuid as \"analysisUuid\", component_uuid as \"componentUuid\", metric_id as \"metricId\", person_id as \"developerId\", "
          +
          "value as \"value\", text_value as \"textValue\", " +
          "variation_value_1 as \"variation_value\"" +
          "FROM project_measures " +
          "ORDER by id asc");
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }
}
