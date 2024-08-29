/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;
import org.sonar.ce.task.projectanalysis.duplication.ComputeDuplicationDataMeasure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.project.Project;

import static java.util.Collections.emptyList;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.sonar.api.measures.CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.api.measures.CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION_KEY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;

class PersistMeasuresStepTest {

  private static final Metric<?> STRING_METRIC = new Metric.Builder("string-metric", "String metric", Metric.ValueType.STRING).create();
  private static final Metric<?> INT_METRIC = new Metric.Builder("int-metric", "int metric", Metric.ValueType.INT).create();
  private static final Metric<?> METRIC_WITH_BEST_VALUE = new Metric.Builder("best-value-metric", "best value metric", Metric.ValueType.INT)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  private static final int REF_1 = 1;
  private static final int REF_2 = 2;
  private static final int REF_3 = 3;
  private static final int REF_4 = 4;

  @RegisterExtension
  public DbTester db = DbTester.create(System2.INSTANCE);
  @RegisterExtension
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @RegisterExtension
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule();
  @RegisterExtension
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @RegisterExtension
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();
  private final ComputeDuplicationDataMeasure computeDuplicationDataMeasure = mock(ComputeDuplicationDataMeasure.class);
  private final TestComputationStepContext context = new TestComputationStepContext();

  private final DbClient dbClient = db.getDbClient();

  @BeforeEach
  public void setUp() {
    MetricDto stringMetricDto =
      db.measures().insertMetric(m -> m.setKey(STRING_METRIC.getKey()).setValueType(Metric.ValueType.STRING.name()));
    MetricDto intMetricDto = db.measures().insertMetric(m -> m.setKey(INT_METRIC.getKey()).setValueType(Metric.ValueType.INT.name()));
    MetricDto bestValueMetricDto = db.measures()
      .insertMetric(m -> m.setKey(METRIC_WITH_BEST_VALUE.getKey()).setValueType(Metric.ValueType.INT.name()).setOptimizedBestValue(true).setBestValue(0.0));
    metricRepository.add(stringMetricDto.getUuid(), STRING_METRIC);
    metricRepository.add(intMetricDto.getUuid(), INT_METRIC);
    metricRepository.add(bestValueMetricDto.getUuid(), METRIC_WITH_BEST_VALUE);
  }

  @Test
  void description() {
    assertThat(step().getDescription()).isEqualTo("Persist measures");
  }

  @Test
  void persist_measures_of_project_analysis() {
    prepareProject();

    // the computed measures
    measureRepository.addRawMeasure(REF_1, STRING_METRIC.getKey(), newMeasureBuilder().create("project-value"));
    measureRepository.addRawMeasure(REF_3, STRING_METRIC.getKey(), newMeasureBuilder().create("dir-value"));
    measureRepository.addRawMeasure(REF_4, STRING_METRIC.getKey(), newMeasureBuilder().create("file-value"));

    step().execute(context);

    // all measures are persisted, from project to file
    assertThat(db.countRowsOfTable("measures")).isEqualTo(3);
    assertThat(selectMeasure("project-uuid"))
      .hasValueSatisfying(measure -> assertThat(measure.getMetricValues()).containsEntry(STRING_METRIC.getKey(), "project-value"));
    assertThat(selectMeasure("dir-uuid"))
      .hasValueSatisfying(measure -> assertThat(measure.getMetricValues()).containsEntry(STRING_METRIC.getKey(), "dir-value"));
    assertThat(selectMeasure("file-uuid"))
      .hasValueSatisfying(measure -> assertThat(measure.getMetricValues()).containsEntry(STRING_METRIC.getKey(), "file-value"));
    verifyInsertsOrUpdates(3);
  }

  @Test
  void persist_measures_of_portfolio_analysis() {
    preparePortfolio();

    // the computed measures
    measureRepository.addRawMeasure(REF_1, STRING_METRIC.getKey(), newMeasureBuilder().create("view-value"));
    measureRepository.addRawMeasure(REF_2, STRING_METRIC.getKey(), newMeasureBuilder().create("subview-value"));
    measureRepository.addRawMeasure(REF_3, STRING_METRIC.getKey(), newMeasureBuilder().create("project-value"));

    step().execute(context);

    assertThat(db.countRowsOfTable("measures")).isEqualTo(3);
    assertThat(selectMeasure("view-uuid"))
      .hasValueSatisfying(measure -> assertThat(measure.getMetricValues()).containsEntry(STRING_METRIC.getKey(), "view-value"));
    assertThat(selectMeasure("subview-uuid"))
      .hasValueSatisfying(measure -> assertThat(measure.getMetricValues()).containsEntry(STRING_METRIC.getKey(), "subview-value"));
    assertThat(selectMeasure("project-uuid"))
      .hasValueSatisfying(measure -> assertThat(measure.getMetricValues()).containsEntry(STRING_METRIC.getKey(), "project-value"));
    verifyInsertsOrUpdates(3);
  }

  @Test
  void persists_large_number_of_measures() {
    int num = 11;
    List<ReportComponent> files = new LinkedList<>();
    String valuePrefix = "value".repeat(10);

    for (int i = 0; i < num; i++) {
      files.add(ReportComponent.builder(FILE, i).setUuid("file-uuid" + i).build());
    }
    Component project = ReportComponent.builder(Component.Type.PROJECT, -1).setUuid("project-uuid")
      .addChildren(files.toArray(Component[]::new))
      .build();
    treeRootHolder.setRoot(project);
    analysisMetadataHolder.setBaseAnalysis(new Analysis.Builder().setUuid("uuid").setCreatedAt(1L).build());
    insertMeasure("file-uuid0", "project-uuid", STRING_METRIC, valuePrefix + "0");

    for (int i = 0; i < num; i++) {
      measureRepository.addRawMeasure(i, STRING_METRIC.getKey(), newMeasureBuilder().create(valuePrefix + i));
    }
    db.getSession().commit();

    PersistMeasuresStep step = new PersistMeasuresStep(dbClient, metricRepository, treeRootHolder, measureRepository,
      computeDuplicationDataMeasure, 100);
    step.execute(context);

    // all measures are persisted, for project and all files
    assertThat(db.countRowsOfTable("measures")).isEqualTo(num + 1);
    verifyInsertsOrUpdates(num - 1);
    verifyUnchanged(1);
    verify(computeDuplicationDataMeasure, times(num)).compute(any(Component.class));
  }

  @Test
  void do_not_persist_excluded_file_metrics() {
    MetricDto fileComplexityMetric =
      db.measures().insertMetric(m -> m.setKey(FILE_COMPLEXITY_DISTRIBUTION_KEY).setValueType(Metric.ValueType.STRING.name()));
    MetricDto functionComplexityMetric =
      db.measures().insertMetric(m -> m.setKey(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY).setValueType(Metric.ValueType.INT.name()));
    metricRepository.add(fileComplexityMetric.getUuid(), new Metric.Builder(FILE_COMPLEXITY_DISTRIBUTION_KEY, "File Distribution / " +
      "Complexity",
      Metric.ValueType.DISTRIB).create());
    metricRepository.add(functionComplexityMetric.getUuid(), new Metric.Builder(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, "Function " +
      "Distribution / Complexity",
      Metric.ValueType.DISTRIB).create());

    prepareProject();

    // the computed measures
    measureRepository.addRawMeasure(REF_1, FILE_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("project-value"));
    measureRepository.addRawMeasure(REF_3, FILE_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("dir-value"));
    measureRepository.addRawMeasure(REF_4, FILE_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("file-value"));

    measureRepository.addRawMeasure(REF_1, FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("project-value"));
    measureRepository.addRawMeasure(REF_3, FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("dir-value"));
    measureRepository.addRawMeasure(REF_4, FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, newMeasureBuilder().create("file-value"));

    step().execute(context);

    // all measures are persisted, from project to file
    assertThat(db.countRowsOfTable("measures")).isEqualTo(3);

    assertThat(selectMeasure("project-uuid"))
      .hasValueSatisfying(measure -> assertThat(measure.getMetricValues()).contains(
        entry(FILE_COMPLEXITY_DISTRIBUTION_KEY, "project-value"),
        entry(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, "project-value")));
    assertThat(selectMeasure("dir-uuid"))
      .hasValueSatisfying(measure -> assertThat(measure.getMetricValues()).contains(
        entry(FILE_COMPLEXITY_DISTRIBUTION_KEY, "dir-value"),
        entry(FUNCTION_COMPLEXITY_DISTRIBUTION_KEY, "dir-value")));
    assertThat(selectMeasure("file-uuid"))
      .hasValueSatisfying(measure -> assertThat(measure.getMetricValues())
        .doesNotContainKeys(FILE_COMPLEXITY_DISTRIBUTION_KEY, FUNCTION_COMPLEXITY_DISTRIBUTION_KEY));

    verifyInsertsOrUpdates(4);
  }

  @Test
  void measures_without_value_are_not_persisted() {
    prepareProject();
    measureRepository.addRawMeasure(REF_1, STRING_METRIC.getKey(), newMeasureBuilder().createNoValue());
    measureRepository.addRawMeasure(REF_1, INT_METRIC.getKey(), newMeasureBuilder().createNoValue());

    step().execute(context);

    assertThat(selectMeasure("project-uuid")).hasValueSatisfying(measureDto -> assertThat(measureDto.getMetricValues()).isEmpty());
    verifyInsertsOrUpdates(0);
  }

  @Test
  void measures_on_leak_period_are_persisted() {
    prepareProject();
    measureRepository.addRawMeasure(REF_1, INT_METRIC.getKey(), newMeasureBuilder().create(42.0));

    step().execute(context);

    assertThat(selectMeasure("project-uuid"))
      .hasValueSatisfying(persistedMeasure -> assertThat(persistedMeasure.getMetricValues()).containsEntry(INT_METRIC.getKey(), 42.0));
    verifyInsertsOrUpdates(1);
  }

  @Test
  void do_not_persist_file_measures_with_best_value() {
    prepareProject();
    // measure to be deleted because new value matches the metric best value
    insertMeasure("file-uuid", "project-uuid", METRIC_WITH_BEST_VALUE, 123.0);
    db.commit();

    // project measure with metric best value -> persist with value 0
    measureRepository.addRawMeasure(REF_1, METRIC_WITH_BEST_VALUE.getKey(), newMeasureBuilder().create(0));
    // file measure with metric best value -> do not persist
    measureRepository.addRawMeasure(REF_4, METRIC_WITH_BEST_VALUE.getKey(), newMeasureBuilder().create(0));

    step().execute(context);

    assertThatMeasureDoesNotExist("file-uuid", METRIC_WITH_BEST_VALUE.getKey());

    Optional<MeasureDto> persisted = dbClient.measureDao().selectMeasure(db.getSession(), "project-uuid");
    assertThat(persisted).isPresent();
    assertThat(persisted.get().getMetricValues()).containsEntry(METRIC_WITH_BEST_VALUE.getKey(), (double) 0);

    verifyInsertsOrUpdates(1);
  }

  @Test
  void do_not_persist_if_value_hash_unchanged() {
    prepareProject();
    insertMeasure("file-uuid", "project-uuid", INT_METRIC, 123.0);
    db.commit();

    measureRepository.addRawMeasure(REF_4, INT_METRIC.getKey(), newMeasureBuilder().create(123L));

    step().execute(context);

    verifyInsertsOrUpdates(0);
    verifyUnchanged(1);
  }

  @Test
  void persist_if_value_hash_changed() {
    prepareProject();
    insertMeasure("file-uuid", "project-uuid", INT_METRIC, 123.0);
    db.commit();

    measureRepository.addRawMeasure(REF_4, INT_METRIC.getKey(), newMeasureBuilder().create(124L));

    step().execute(context);

    verifyInsertsOrUpdates(1);
    verifyUnchanged(0);
  }

  private void insertMeasure(String componentUuid, String projectUuid, Metric<?> metric, Object obj) {
    MeasureDto measure = new MeasureDto()
      .setComponentUuid(componentUuid)
      .setBranchUuid(projectUuid)
      .addValue(metric.getKey(), obj);
    measure.computeJsonValueHash();
    dbClient.measureDao().insert(db.getSession(), measure);
  }

  private void assertThatMeasureDoesNotExist(String componentUuid, String metricKey) {
    assertThat(dbClient.measureDao().selectMeasure(db.getSession(), componentUuid))
      .hasValueSatisfying(measureDto -> assertThat(measureDto.getMetricValues()).doesNotContainKey(metricKey));
  }

  private void prepareProject() {
    // tree of components as defined by scanner report
    Component project = ReportComponent.builder(PROJECT, REF_1).setUuid("project-uuid")
      .addChildren(
        ReportComponent.builder(DIRECTORY, REF_3).setUuid("dir-uuid")
          .addChildren(
            ReportComponent.builder(FILE, REF_4).setUuid("file-uuid")
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(project);
    analysisMetadataHolder.setProject(new Project(project.getUuid(), project.getKey(), project.getName(), project.getDescription(),
      emptyList()));

    // components as persisted in db
    insertComponent("project-key", "project-uuid");
    insertComponent("dir-key", "dir-uuid");
    insertComponent("file-key", "file-uuid");
  }

  private void preparePortfolio() {
    // tree of components
    Component portfolio = ViewsComponent.builder(VIEW, REF_1).setUuid("view-uuid")
      .addChildren(
        ViewsComponent.builder(SUBVIEW, REF_2).setUuid("subview-uuid")
          .addChildren(
            ViewsComponent.builder(PROJECT_VIEW, REF_3).setUuid("project-uuid")
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(portfolio);

    // components as persisted in db
    ComponentDto portfolioDto = insertComponent("view-key", "view-uuid");
    insertComponent("subview-key", "subview-uuid");
    insertComponent("project-key", "project-uuid");
    analysisMetadataHolder.setProject(Project.from(portfolioDto));
  }

  private Optional<MeasureDto> selectMeasure(String componentUuid) {
    return dbClient.measureDao().selectMeasure(db.getSession(), componentUuid);
  }

  private ComponentDto insertComponent(String key, String uuid) {
    ComponentDto componentDto = new ComponentDto()
      .setKey(key)
      .setUuid(uuid)
      .setUuidPath(uuid + ".")
      .setBranchUuid(uuid);
    db.components().insertComponent(componentDto);
    return componentDto;
  }

  private PersistMeasuresStep step() {
    return new PersistMeasuresStep(dbClient, metricRepository, treeRootHolder, measureRepository, computeDuplicationDataMeasure);
  }

  private void verifyInsertsOrUpdates(int expectedInsertsOrUpdates) {
    context.getStatistics().assertValue("insertsOrUpdates", expectedInsertsOrUpdates);
  }

  private void verifyUnchanged(int expectedUnchanged) {
    context.getStatistics().assertValue("unchanged", expectedUnchanged);
  }
}
