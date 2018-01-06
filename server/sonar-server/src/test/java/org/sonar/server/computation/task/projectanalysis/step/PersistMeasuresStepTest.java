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

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.computation.task.projectanalysis.analysis.MutableAnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.ViewsComponent;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureToMeasureDto;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.MODULE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT_VIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.SUBVIEW;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.VIEW;
import static org.sonar.server.computation.task.projectanalysis.measure.Measure.newMeasureBuilder;

public class PersistMeasuresStepTest extends BaseStepTest {

  private static final Metric STRING_METRIC = new Metric.Builder("string-metric", "String metric", Metric.ValueType.STRING).create();
  private static final Metric INT_METRIC = new Metric.Builder("int-metric", "int metric", Metric.ValueType.INT).create();

  private static final String ANALYSIS_UUID = "a1";

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
    analysisMetadataHolder.setUuid(ANALYSIS_UUID);
    MetricDto stringMetricDto = db.measures().insertMetric(m -> m.setKey(STRING_METRIC.getKey()).setValueType(Metric.ValueType.STRING.name()));
    MetricDto intMetricDto = db.measures().insertMetric(m -> m.setKey(INT_METRIC.getKey()).setValueType(Metric.ValueType.INT.name()));
    metricRepository.add(stringMetricDto.getId(), STRING_METRIC);
    metricRepository.add(intMetricDto.getId(), INT_METRIC);
  }

  @Test
  public void persist_measures_of_project_analysis() {
    prepareProject();

    // the computed measures
    measureRepository.addRawMeasure(REF_1, STRING_METRIC.getKey(), newMeasureBuilder().create("project-value"));
    measureRepository.addRawMeasure(REF_2, STRING_METRIC.getKey(), newMeasureBuilder().create("module-value"));
    measureRepository.addRawMeasure(REF_3, STRING_METRIC.getKey(), newMeasureBuilder().create("dir-value"));
    measureRepository.addRawMeasure(REF_4, STRING_METRIC.getKey(), newMeasureBuilder().create("file-value"));

    execute(true);

    // project, module and dir measures are persisted, but not file measures
    assertThat(db.countRowsOfTable("project_measures")).isEqualTo(3);
    assertThat(selectMeasure("project-uuid", STRING_METRIC).get().getData()).isEqualTo("project-value");
    assertThat(selectMeasure("module-uuid", STRING_METRIC).get().getData()).isEqualTo("module-value");
    assertThat(selectMeasure("dir-uuid", STRING_METRIC).get().getData()).isEqualTo("dir-value");
    assertThatMeasuresAreNotPersisted("file-uuid");
  }

  @Test
  public void persist_measures_of_project_analysis_excluding_directories() {
    prepareProject();

    // the computed measures
    measureRepository.addRawMeasure(REF_1, STRING_METRIC.getKey(), newMeasureBuilder().create("project-value"));
    measureRepository.addRawMeasure(REF_2, STRING_METRIC.getKey(), newMeasureBuilder().create("module-value"));
    measureRepository.addRawMeasure(REF_3, STRING_METRIC.getKey(), newMeasureBuilder().create("dir-value"));
    measureRepository.addRawMeasure(REF_4, STRING_METRIC.getKey(), newMeasureBuilder().create("file-value"));

    execute(false);

    // project, module and dir measures are persisted, but not file measures
    assertThat(db.countRowsOfTable("project_measures")).isEqualTo(2);
    assertThat(selectMeasure("project-uuid", STRING_METRIC).get().getData()).isEqualTo("project-value");
    assertThat(selectMeasure("module-uuid", STRING_METRIC).get().getData()).isEqualTo("module-value");
    assertThatMeasuresAreNotPersisted("dir-uuid");
    assertThatMeasuresAreNotPersisted("file-uuid");
  }

  @Test
  public void measures_without_value_are_not_persisted() {
    prepareProject();
    measureRepository.addRawMeasure(REF_1, STRING_METRIC.getKey(), newMeasureBuilder().createNoValue());
    measureRepository.addRawMeasure(REF_1, INT_METRIC.getKey(), newMeasureBuilder().createNoValue());

    execute(false);

    assertThatMeasureIsNotPersisted("project-uuid", STRING_METRIC);
    assertThatMeasureIsNotPersisted("project-uuid", INT_METRIC);
  }

  @Test
  public void measures_on_leak_period_are_persisted() {
    prepareProject();
    measureRepository.addRawMeasure(REF_1, INT_METRIC.getKey(), newMeasureBuilder().setVariation(42.0).createNoValue());

    execute(false);

    MeasureDto persistedMeasure = selectMeasure("project-uuid", INT_METRIC).get();
    assertThat(persistedMeasure.getValue()).isNull();
    assertThat(persistedMeasure.getVariation()).isEqualTo(42.0);
  }

  @Test
  public void persist_all_measures_of_portfolio_analysis() {
    preparePortfolio();

    // the computed measures
    measureRepository.addRawMeasure(REF_1, STRING_METRIC.getKey(), newMeasureBuilder().create("view-value"));
    measureRepository.addRawMeasure(REF_2, STRING_METRIC.getKey(), newMeasureBuilder().create("subview-value"));
    measureRepository.addRawMeasure(REF_3, STRING_METRIC.getKey(), newMeasureBuilder().create("project-value"));

    execute(true);

    assertThat(db.countRowsOfTable("project_measures")).isEqualTo(3);
    assertThat(selectMeasure("view-uuid", STRING_METRIC).get().getData()).isEqualTo("view-value");
    assertThat(selectMeasure("subview-uuid", STRING_METRIC).get().getData()).isEqualTo("subview-value");
    assertThat(selectMeasure("project-uuid", STRING_METRIC).get().getData()).isEqualTo("project-value");
  }

  private void prepareProject() {
    // tree of components as defined by scanner report
    Component project = ReportComponent.builder(PROJECT, REF_1).setUuid("project-uuid")
      .addChildren(
        ReportComponent.builder(MODULE, REF_2).setUuid("module-uuid")
          .addChildren(
            ReportComponent.builder(DIRECTORY, REF_3).setUuid("dir-uuid")
              .addChildren(
                ReportComponent.builder(FILE, REF_4).setUuid("file-uuid")
                  .build())
              .build())
          .build())
      .build();
    treeRootHolder.setRoot(project);

    // components as persisted in db
    ComponentDto projectDto = insertComponent("project-key", "project-uuid");
    ComponentDto moduleDto = insertComponent("module-key", "module-uuid");
    ComponentDto dirDto = insertComponent("dir-key", "dir-uuid");
    ComponentDto fileDto = insertComponent("file-key", "file-uuid");
    db.components().insertSnapshot(projectDto, s -> s.setUuid(ANALYSIS_UUID));
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
    ComponentDto viewDto = insertComponent("view-key", "view-uuid");
    ComponentDto subViewDto = insertComponent("subview-key", "subview-uuid");
    ComponentDto projectDto = insertComponent("project-key", "project-uuid");
    db.components().insertSnapshot(viewDto, s -> s.setUuid(ANALYSIS_UUID));
  }

  private void assertThatMeasureIsNotPersisted(String componentUuid, Metric metric) {
    assertThat(selectMeasure(componentUuid, metric)).isEmpty();
  }

  private void assertThatMeasuresAreNotPersisted(String componentUuid) {
    assertThatMeasureIsNotPersisted(componentUuid, STRING_METRIC);
    assertThatMeasureIsNotPersisted(componentUuid, INT_METRIC);
  }

  private void execute(boolean persistDirectories) {
    new PersistMeasuresStep(dbClient, metricRepository, new MeasureToMeasureDto(analysisMetadataHolder, treeRootHolder), treeRootHolder, measureRepository, persistDirectories)
      .execute();
  }

  private Optional<MeasureDto> selectMeasure(String componentUuid, Metric metric) {
    return dbClient.measureDao().selectMeasure(db.getSession(), ANALYSIS_UUID, componentUuid, metric.getKey());
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
    return new PersistMeasuresStep(dbClient, metricRepository, new MeasureToMeasureDto(analysisMetadataHolder, treeRootHolder), treeRootHolder, measureRepository, true);
  }
}
