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
package org.sonar.ce.task.projectexport.steps;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.task.projectexport.component.ComponentRepositoryImpl;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.project.ProjectDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.Metric.ValueType.INT;

public class ExportLiveMeasuresStepTest {
  @Rule
  public LogTester logTester = new LogTester();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private ComponentRepositoryImpl componentRepository = new ComponentRepositoryImpl();
  private MutableMetricRepository metricRepository = new MutableMetricRepositoryImpl();
  private ProjectHolder projectHolder = mock(ProjectHolder.class);
  private FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private ExportLiveMeasuresStep underTest = new ExportLiveMeasuresStep(dbTester.getDbClient(), projectHolder, componentRepository, metricRepository, dumpWriter);

  @Test
  public void export_zero_measures() {
    when(projectHolder.branches()).thenReturn(newArrayList());
    when(projectHolder.projectDto()).thenReturn(new ProjectDto().setUuid("does not exist"));

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.LIVE_MEASURES)).isEmpty();
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("0 live measures exported");
    assertThat(metricRepository.getRefByUuid()).isEmpty();
  }

  @Test
  public void export_measures() {
    ComponentDto project = createProject(true);
    componentRepository.register(1, project.uuid(), false);
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setKey("metric1").setValueType(INT.name()));
    dbTester.measures().insertLiveMeasure(project, metric, m -> m.setValue(4711.0d));
    when(projectHolder.projectDto()).thenReturn(dbTester.components().getProjectDto(project));
    when(projectHolder.branches()).thenReturn(newArrayList(new BranchDto()
      .setProjectUuid(project.uuid())
      .setUuid(project.uuid())
      .setKey("master")
      .setBranchType(BranchType.BRANCH)));
    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.LiveMeasure> exportedMeasures = dumpWriter.getWrittenMessagesOf(DumpElement.LIVE_MEASURES);
    assertThat(exportedMeasures).hasSize(1);
    assertThat(exportedMeasures)
      .extracting(ProjectDump.LiveMeasure::getMetricRef, m -> m.getDoubleValue().getValue(), ProjectDump.LiveMeasure::hasVariation)
      .containsOnly(tuple(0, 4711.0d, false));
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("1 live measures exported");
    assertThat(metricRepository.getRefByUuid()).containsOnlyKeys(metric.getUuid());
  }

  @Test
  public void do_not_export_measures_on_disabled_projects() {
    ComponentDto project = createProject(false);
    componentRepository.register(1, project.uuid(), false);
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()));
    dbTester.measures().insertLiveMeasure(project, metric, m -> m.setValue(4711.0d));
    when(projectHolder.projectDto()).thenReturn(dbTester.components().getProjectDto(project));
    when(projectHolder.branches()).thenReturn(newArrayList(new BranchDto()
      .setProjectUuid(project.uuid())
      .setUuid(project.uuid())
      .setKey("master")
      .setBranchType(BranchType.BRANCH)));

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.LiveMeasure> exportedMeasures = dumpWriter.getWrittenMessagesOf(DumpElement.LIVE_MEASURES);
    assertThat(exportedMeasures).isEmpty();
  }

  @Test
  public void do_not_export_measures_on_disabled_metrics() {
    ComponentDto project = createProject(true);
    componentRepository.register(1, project.uuid(), false);
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()).setEnabled(false));
    dbTester.measures().insertLiveMeasure(project, metric, m -> m.setValue(4711.0d));
    when(projectHolder.projectDto()).thenReturn(dbTester.components().getProjectDto(project));
    when(projectHolder.branches()).thenReturn(newArrayList(new BranchDto()
      .setProjectUuid(project.uuid())
      .setUuid(project.uuid())
      .setKey("master")
      .setBranchType(BranchType.BRANCH)));

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.LiveMeasure> exportedMeasures = dumpWriter.getWrittenMessagesOf(DumpElement.LIVE_MEASURES);
    assertThat(exportedMeasures).isEmpty();
  }

  @Test
  public void test_exported_fields() {
    ComponentDto project = createProject(true);
    componentRepository.register(1, project.uuid(), false);
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setKey("new_metric").setValueType(INT.name()));
    dbTester.measures().insertLiveMeasure(project, metric, m -> m.setProjectUuid(project.uuid()).setValue(7.0d).setData("test"));
    when(projectHolder.projectDto()).thenReturn(dbTester.components().getProjectDto(project));
    when(projectHolder.branches()).thenReturn(newArrayList(new BranchDto()
      .setProjectUuid(project.uuid())
      .setUuid(project.uuid())
      .setKey("master")
      .setBranchType(BranchType.BRANCH)));

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.LiveMeasure> exportedMeasures = dumpWriter.getWrittenMessagesOf(DumpElement.LIVE_MEASURES);
    assertThat(exportedMeasures).hasSize(1);
    assertThat(exportedMeasures)
      .extracting(
        ProjectDump.LiveMeasure::getComponentRef,
        ProjectDump.LiveMeasure::getMetricRef,
        m -> m.getDoubleValue().getValue(),
        ProjectDump.LiveMeasure::getTextValue,
        m -> m.getVariation().getValue())
      .containsOnly(tuple(
        1L,
        0,
        0.0d,
        "test",
        7.0d));
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("1 live measures exported");
    assertThat(metricRepository.getRefByUuid()).containsOnlyKeys(metric.getUuid());
  }

  @Test
  public void test_null_exported_fields() {
    ComponentDto project = createProject(true);
    componentRepository.register(1, project.uuid(), false);
    MetricDto metric = dbTester.measures().insertMetric(m -> m.setValueType(INT.name()));
    dbTester.measures().insertLiveMeasure(project, metric, m -> m.setProjectUuid(project.uuid()).setValue(null).setData((String) null));
    when(projectHolder.projectDto()).thenReturn(dbTester.components().getProjectDto(project));
    when(projectHolder.branches()).thenReturn(newArrayList(new BranchDto()
      .setProjectUuid(project.uuid())
      .setUuid(project.uuid())
      .setKey("master")
      .setBranchType(BranchType.BRANCH)));

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.LiveMeasure> exportedMeasures = dumpWriter.getWrittenMessagesOf(DumpElement.LIVE_MEASURES);
    assertThat(exportedMeasures).hasSize(1);
    assertThat(exportedMeasures)
      .extracting(
        ProjectDump.LiveMeasure::hasDoubleValue,
        ProjectDump.LiveMeasure::getTextValue,
        ProjectDump.LiveMeasure::hasVariation)
      .containsOnly(tuple(
        false,
        "",
        false));
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("1 live measures exported");
    assertThat(metricRepository.getRefByUuid()).containsOnlyKeys(metric.getUuid());
  }

  @Test
  public void test_getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Export live measures");
  }

  private ComponentDto createProject(boolean enabled) {
    return dbTester.components().insertPrivateProject(p -> p.setEnabled(enabled));
  }
}
