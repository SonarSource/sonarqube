/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.component.ViewsComponent;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.measure.MeasureToMeasureDto;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.project.Project;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.db.measure.MeasureTesting.newLiveMeasure;

public class PersistLiveMeasuresStepTest extends BaseStepTest {

  private static final Metric STRING_METRIC = new Metric.Builder("string-metric", "String metric", Metric.ValueType.STRING).create();
  private static final Metric INT_METRIC = new Metric.Builder("int-metric", "int metric", Metric.ValueType.INT).create();
  private static final Metric METRIC_WITH_BEST_VALUE = new Metric.Builder("best-value-metric", "best value metric", Metric.ValueType.INT)
    .setBestValue(0.0)
    .setOptimizedBestValue(true)
    .create();

  private static final int REF_1 = 1;
  private static final int REF_2 = 2;
  private static final int REF_3 = 3;
  private static final int REF_4 = 4;

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule();
  @Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(treeRootHolder, metricRepository);
  @Rule
  public MutableAnalysisMetadataHolderRule analysisMetadataHolder = new MutableAnalysisMetadataHolderRule();

  private DbClient dbClient = db.getDbClient();

  @Before
  public void setUp() {
    MetricDto stringMetricDto = db.measures().insertMetric(m -> m.setKey(STRING_METRIC.getKey()).setValueType(Metric.ValueType.STRING.name()));
    MetricDto intMetricDto = db.measures().insertMetric(m -> m.setKey(INT_METRIC.getKey()).setValueType(Metric.ValueType.INT.name()));
    MetricDto bestValueMMetricDto = db.measures()
      .insertMetric(m -> m.setKey(METRIC_WITH_BEST_VALUE.getKey()).setValueType(Metric.ValueType.INT.name()).setOptimizedBestValue(true).setBestValue(0.0));
    metricRepository.add(stringMetricDto.getId(), STRING_METRIC);
    metricRepository.add(intMetricDto.getId(), INT_METRIC);
    metricRepository.add(bestValueMMetricDto.getId(), METRIC_WITH_BEST_VALUE);
  }

  @Test
  public void persist_live_measures_of_project_analysis() {
    prepareProject();

    // the computed measures
    measureRepository.addRawMeasure(REF_1, STRING_METRIC.getKey(), newMeasureBuilder().create("project-value"));
    measureRepository.addRawMeasure(REF_3, STRING_METRIC.getKey(), newMeasureBuilder().create("dir-value"));
    measureRepository.addRawMeasure(REF_4, STRING_METRIC.getKey(), newMeasureBuilder().create("file-value"));

    TestComputationStepContext context = new TestComputationStepContext();
    step().execute(context);

    // all measures are persisted, from project to file
    assertThat(db.countRowsOfTable("live_measures")).isEqualTo(3);
    assertThat(selectMeasure("project-uuid", STRING_METRIC).get().getDataAsString()).isEqualTo("project-value");
    assertThat(selectMeasure("dir-uuid", STRING_METRIC).get().getDataAsString()).isEqualTo("dir-value");
    assertThat(selectMeasure("file-uuid", STRING_METRIC).get().getDataAsString()).isEqualTo("file-value");
    verifyStatistics(context, 3);
  }

  @Test
  public void measures_without_value_are_not_persisted() {
    prepareProject();
    measureRepository.addRawMeasure(REF_1, STRING_METRIC.getKey(), newMeasureBuilder().createNoValue());
    measureRepository.addRawMeasure(REF_1, INT_METRIC.getKey(), newMeasureBuilder().createNoValue());

    TestComputationStepContext context = new TestComputationStepContext();
    step().execute(context);

    assertThatMeasureIsNotPersisted("project-uuid", STRING_METRIC);
    assertThatMeasureIsNotPersisted("project-uuid", INT_METRIC);
    verifyStatistics(context, 0);
  }

  @Test
  public void measures_on_leak_period_are_persisted() {
    prepareProject();
    measureRepository.addRawMeasure(REF_1, INT_METRIC.getKey(), newMeasureBuilder().setVariation(42.0).createNoValue());

    TestComputationStepContext context = new TestComputationStepContext();
    step().execute(context);

    LiveMeasureDto persistedMeasure = selectMeasure("project-uuid", INT_METRIC).get();
    assertThat(persistedMeasure.getValue()).isNull();
    assertThat(persistedMeasure.getVariation()).isEqualTo(42.0);
    verifyStatistics(context, 1);
  }

  @Test
  public void delete_measures_from_db_if_no_longer_computed() {
    prepareProject();
    // measure to be updated
    LiveMeasureDto measureOnFileInProject = insertMeasure("file-uuid", "project-uuid", INT_METRIC);
    // measure to be deleted because not computed anymore
    LiveMeasureDto otherMeasureOnFileInProject = insertMeasure("file-uuid", "project-uuid", STRING_METRIC);
    // measure in another project, not touched
    LiveMeasureDto measureInOtherProject = insertMeasure("other-file-uuid", "other-project-uuid", INT_METRIC);
    db.commit();

    measureRepository.addRawMeasure(REF_4, INT_METRIC.getKey(), newMeasureBuilder().create(42));

    TestComputationStepContext context = new TestComputationStepContext();
    step().execute(context);

    assertThatMeasureHasValue(measureOnFileInProject, 42);
    assertThatMeasureDoesNotExist(otherMeasureOnFileInProject);
    assertThatMeasureHasValue(measureInOtherProject, (int) measureInOtherProject.getValue().doubleValue());
    verifyStatistics(context, 1);
  }

  @Test
  public void do_not_persist_file_measures_with_best_value() {
    prepareProject();
    // measure to be deleted because new value matches the metric best value
    LiveMeasureDto oldMeasure = insertMeasure("file-uuid", "project-uuid", INT_METRIC);
    db.commit();

    // project measure with metric best value -> persist with value 0
    measureRepository.addRawMeasure(REF_1, METRIC_WITH_BEST_VALUE.getKey(), newMeasureBuilder().create(0));
    // file measure with metric best value -> do not persist
    measureRepository.addRawMeasure(REF_4, METRIC_WITH_BEST_VALUE.getKey(), newMeasureBuilder().create(0));

    TestComputationStepContext context = new TestComputationStepContext();
    step().execute(context);

    assertThatMeasureDoesNotExist(oldMeasure);
    assertThatMeasureHasValue("project-uuid", METRIC_WITH_BEST_VALUE, 0);
    verifyStatistics(context, 1);
  }

  @Test
  public void persist_live_measures_of_portfolio_analysis() {
    preparePortfolio();

    // the computed measures
    measureRepository.addRawMeasure(REF_1, STRING_METRIC.getKey(), newMeasureBuilder().create("view-value"));
    measureRepository.addRawMeasure(REF_2, STRING_METRIC.getKey(), newMeasureBuilder().create("subview-value"));
    measureRepository.addRawMeasure(REF_3, STRING_METRIC.getKey(), newMeasureBuilder().create("project-value"));

    TestComputationStepContext context = new TestComputationStepContext();
    step().execute(context);

    assertThat(db.countRowsOfTable("live_measures")).isEqualTo(3);
    assertThat(selectMeasure("view-uuid", STRING_METRIC).get().getDataAsString()).isEqualTo("view-value");
    assertThat(selectMeasure("subview-uuid", STRING_METRIC).get().getDataAsString()).isEqualTo("subview-value");
    assertThat(selectMeasure("project-uuid", STRING_METRIC).get().getDataAsString()).isEqualTo("project-value");
    verifyStatistics(context, 3);
  }

  private LiveMeasureDto insertMeasure(String componentUuid, String projectUuid, Metric metric) {
    LiveMeasureDto measure = newLiveMeasure()
      .setComponentUuid(componentUuid)
      .setProjectUuid(projectUuid)
      .setMetricId(metricRepository.getByKey(metric.getKey()).getId());
    dbClient.liveMeasureDao().insertOrUpdate(db.getSession(), measure);
    return measure;
  }

  private void assertThatMeasureHasValue(LiveMeasureDto template, int expectedValue) {
    Optional<LiveMeasureDto> persisted = dbClient.liveMeasureDao().selectMeasure(db.getSession(),
      template.getComponentUuid(), metricRepository.getById(template.getMetricId()).getKey());
    assertThat(persisted).isPresent();
    assertThat(persisted.get().getValue()).isEqualTo((double) expectedValue);
  }

  private void assertThatMeasureHasValue(String componentUuid, Metric metric, int expectedValue) {
    Optional<LiveMeasureDto> persisted = dbClient.liveMeasureDao().selectMeasure(db.getSession(),
      componentUuid, metric.getKey());
    assertThat(persisted).isPresent();
    assertThat(persisted.get().getValue()).isEqualTo((double) expectedValue);
  }

  private void assertThatMeasureDoesNotExist(LiveMeasureDto template) {
    assertThat(dbClient.liveMeasureDao().selectMeasure(db.getSession(),
      template.getComponentUuid(), metricRepository.getById(template.getMetricId()).getKey()))
      .isEmpty();
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
    analysisMetadataHolder.setProject(new Project(project.getUuid(), project.getDbKey(), project.getName(), project.getDescription(), emptyList()));

    // components as persisted in db
    ComponentDto projectDto = insertComponent("project-key", "project-uuid");
    ComponentDto dirDto = insertComponent("dir-key", "dir-uuid");
    ComponentDto fileDto = insertComponent("file-key", "file-uuid");
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
    ComponentDto subViewDto = insertComponent("subview-key", "subview-uuid");
    ComponentDto projectDto = insertComponent("project-key", "project-uuid");
    analysisMetadataHolder.setProject(Project.from(portfolioDto));
  }

  private void assertThatMeasureIsNotPersisted(String componentUuid, Metric metric) {
    assertThat(selectMeasure(componentUuid, metric)).isEmpty();
  }

  private Optional<LiveMeasureDto> selectMeasure(String componentUuid, Metric metric) {
    return dbClient.liveMeasureDao().selectMeasure(db.getSession(), componentUuid, metric.getKey());
  }

  private ComponentDto insertComponent(String key, String uuid) {
    ComponentDto componentDto = new ComponentDto()
      .setOrganizationUuid("org1")
      .setDbKey(key)
      .setUuid(uuid)
      .setUuidPath(uuid + ".")
      .setRootUuid(uuid)
      .setProjectUuid(uuid);
    dbClient.componentDao().insert(db.getSession(), componentDto);
    return componentDto;
  }

  @Override
  protected ComputationStep step() {
    return new PersistLiveMeasuresStep(dbClient, metricRepository, new MeasureToMeasureDto(analysisMetadataHolder, treeRootHolder), treeRootHolder, measureRepository);
  }

  private static void verifyStatistics(TestComputationStepContext context, int expectedInsertsOrUpdates) {
    context.getStatistics().assertValue("insertsOrUpdates", expectedInsertsOrUpdates);
  }
}
