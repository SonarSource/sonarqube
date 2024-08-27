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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectexport.component.ComponentRepositoryImpl;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.measure.ProjectMeasureDto;
import org.sonar.db.metric.MetricDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;
import static org.sonar.db.component.ComponentDto.UUID_PATH_SEPARATOR;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;

public class ExportMeasuresStepIT {

  private static final ComponentDto PROJECT = new ComponentDto()
    .setKey("project_key")
    .setUuid("project_uuid")
    .setBranchUuid("project_uuid")
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setEnabled(true);
  private static final ComponentDto FILE = new ComponentDto()
    .setKey("file_key")
    .setUuid("file_uuid")
    .setBranchUuid("project_uuid")
    .setUuidPath(UUID_PATH_OF_ROOT + PROJECT.uuid() + UUID_PATH_SEPARATOR)
    .setEnabled(true);
  private static final ComponentDto ANOTHER_PROJECT = new ComponentDto()
    .setKey("another_project_key")
    .setUuid("another_project_uuid")
    .setBranchUuid("another_project_uuid")
    .setUuidPath(UUID_PATH_OF_ROOT)
    .setEnabled(true);

  private static final MetricDto NCLOC = new MetricDto()
    .setUuid("3")
    .setKey("ncloc")
    .setShortName("Lines of code")
    .setEnabled(true);

  private static final MetricDto DISABLED_METRIC = new MetricDto()
    .setUuid("4")
    .setKey("coverage")
    .setShortName("Coverage")
    .setEnabled(false);

  private static final MetricDto NEW_NCLOC = new MetricDto()
    .setUuid("5")
    .setKey("new_ncloc")
    .setShortName("New Lines of code")
    .setEnabled(true);

  private static final List<BranchDto> BRANCHES = newArrayList(
    new BranchDto()
      .setBranchType(BranchType.BRANCH)
      .setKey("master")
      .setUuid(PROJECT.uuid())
      .setProjectUuid(PROJECT.uuid()));


  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private ComponentRepositoryImpl componentRepository = new ComponentRepositoryImpl();
  private MutableMetricRepository metricRepository = new MutableMetricRepositoryImpl();
  private ProjectHolder projectHolder = mock(ProjectHolder.class);
  private FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private ExportMeasuresStep underTest = new ExportMeasuresStep(dbTester.getDbClient(), projectHolder, componentRepository, metricRepository, dumpWriter);

  @Before
  public void setUp() {
    logTester.setLevel(Level.DEBUG);
    String projectUuid = dbTester.components().insertPublicProject(PROJECT).getMainBranchComponent().uuid();
    componentRepository.register(1, projectUuid, false);
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), List.of(FILE, ANOTHER_PROJECT), true);
    dbTester.getDbClient().metricDao().insert(dbTester.getSession(), NCLOC, DISABLED_METRIC, NEW_NCLOC);
    dbTester.commit();
    when(projectHolder.projectDto()).thenReturn(dbTester.components().getProjectDtoByMainBranch(PROJECT));
    when(projectHolder.branches()).thenReturn(BRANCHES);
  }

  @Test
  public void export_zero_measures() {
    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.MEASURES)).isEmpty();
    assertThat(logTester.logs(Level.DEBUG)).contains("0 measures exported");
    assertThat(metricRepository.getRefByUuid()).isEmpty();
  }

  @Test
  public void export_measures() {
    SnapshotDto firstAnalysis = insertSnapshot("U_1", PROJECT, STATUS_PROCESSED);
    insertMeasure(firstAnalysis, PROJECT, new ProjectMeasureDto().setValue(100.0).setMetricUuid(NCLOC.getUuid()));
    SnapshotDto secondAnalysis = insertSnapshot("U_2", PROJECT, STATUS_PROCESSED);
    insertMeasure(secondAnalysis, PROJECT, new ProjectMeasureDto().setValue(110.0).setMetricUuid(NCLOC.getUuid()));
    SnapshotDto anotherProjectAnalysis = insertSnapshot("U_3", ANOTHER_PROJECT, STATUS_PROCESSED);
    insertMeasure(anotherProjectAnalysis, ANOTHER_PROJECT, new ProjectMeasureDto().setValue(500.0).setMetricUuid(NCLOC.getUuid()));
    dbTester.commit();

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.Measure> exportedMeasures = dumpWriter.getWrittenMessagesOf(DumpElement.MEASURES);
    assertThat(exportedMeasures).hasSize(2);
    assertThat(exportedMeasures).extracting(ProjectDump.Measure::getAnalysisUuid).containsOnly(firstAnalysis.getUuid(), secondAnalysis.getUuid());
    assertThat(exportedMeasures).extracting(ProjectDump.Measure::getMetricRef).containsOnly(0);
    assertThat(logTester.logs(Level.DEBUG)).contains("2 measures exported");
    assertThat(metricRepository.getRefByUuid()).containsOnlyKeys(NCLOC.getUuid());
  }

  @Test
  public void do_not_export_measures_on_unprocessed_snapshots() {
    SnapshotDto firstAnalysis = insertSnapshot("U_1", PROJECT, STATUS_UNPROCESSED);
    insertMeasure(firstAnalysis, PROJECT, new ProjectMeasureDto().setValue(100.0).setMetricUuid(NCLOC.getUuid()));
    dbTester.commit();

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.Measure> exportedMeasures = dumpWriter.getWrittenMessagesOf(DumpElement.MEASURES);
    assertThat(exportedMeasures).isEmpty();
  }

  @Test
  public void do_not_export_measures_on_disabled_metrics() {
    SnapshotDto firstAnalysis = insertSnapshot("U_1", PROJECT, STATUS_PROCESSED);
    insertMeasure(firstAnalysis, PROJECT, new ProjectMeasureDto().setValue(100.0).setMetricUuid(DISABLED_METRIC.getUuid()));
    dbTester.commit();

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.Measure> exportedMeasures = dumpWriter.getWrittenMessagesOf(DumpElement.MEASURES);
    assertThat(exportedMeasures).isEmpty();
  }

  @Test
  public void test_exported_fields() {
    SnapshotDto analysis = insertSnapshot("U_1", PROJECT, STATUS_PROCESSED);
    ProjectMeasureDto dto = new ProjectMeasureDto()
      .setMetricUuid(NCLOC.getUuid())
      .setValue(100.0)
      .setData("data")
      .setAlertStatus("OK")
      .setAlertText("alert text");
    insertMeasure(analysis, PROJECT, dto);
    dbTester.commit();

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.Measure> exportedMeasures = dumpWriter.getWrittenMessagesOf(DumpElement.MEASURES);
    ProjectDump.Measure measure = exportedMeasures.get(0);
    assertThat(measure.getAlertStatus()).isEqualTo(dto.getAlertStatus());
    assertThat(measure.getAlertText()).isEqualTo(dto.getAlertText());
    assertThat(measure.getDoubleValue().getValue()).isEqualTo(dto.getValue());
    assertThat(measure.getTextValue()).isEqualTo(dto.getData());
    assertThat(measure.getMetricRef()).isZero();
    assertThat(measure.getAnalysisUuid()).isEqualTo(analysis.getUuid());
    assertThat(measure.getVariation1().getValue()).isZero();
  }

  @Test
  public void test_exported_fields_new_metric() {
    SnapshotDto analysis = insertSnapshot("U_1", PROJECT, STATUS_PROCESSED);
    ProjectMeasureDto dto = new ProjectMeasureDto()
      .setMetricUuid(NEW_NCLOC.getUuid())
      .setValue(100.0)
      .setData("data")
      .setAlertStatus("OK")
      .setAlertText("alert text");
    insertMeasure(analysis, PROJECT, dto);
    dbTester.commit();

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.Measure> exportedMeasures = dumpWriter.getWrittenMessagesOf(DumpElement.MEASURES);
    ProjectDump.Measure measure = exportedMeasures.get(0);
    assertThat(measure.getAlertStatus()).isEqualTo(dto.getAlertStatus());
    assertThat(measure.getAlertText()).isEqualTo(dto.getAlertText());
    assertThat(measure.getDoubleValue().getValue()).isZero();
    assertThat(measure.getTextValue()).isEqualTo(dto.getData());
    assertThat(measure.getMetricRef()).isZero();
    assertThat(measure.getAnalysisUuid()).isEqualTo(analysis.getUuid());
    assertThat(measure.getVariation1().getValue()).isEqualTo(dto.getValue());
  }

  @Test
  public void test_null_exported_fields() {
    SnapshotDto analysis = insertSnapshot("U_1", PROJECT, STATUS_PROCESSED);
    insertMeasure(analysis, PROJECT, new ProjectMeasureDto().setMetricUuid(NCLOC.getUuid()));
    dbTester.commit();

    underTest.execute(new TestComputationStepContext());

    ProjectDump.Measure measure = dumpWriter.getWrittenMessagesOf(DumpElement.MEASURES).get(0);
    assertThat(measure.getAlertStatus()).isEmpty();
    assertThat(measure.getAlertText()).isEmpty();
    assertThat(measure.hasDoubleValue()).isFalse();
    assertThat(measure.getTextValue()).isEmpty();
    assertThat(measure.hasVariation1()).isFalse();
  }

  @Test
  public void test_getDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Export measures");
  }

  private SnapshotDto insertSnapshot(String snapshotUuid, ComponentDto project, String status) {
    SnapshotDto snapshot = new SnapshotDto()
      .setUuid(snapshotUuid)
      .setRootComponentUuid(project.uuid())
      .setStatus(status)
      .setLast(true);
    dbTester.getDbClient().snapshotDao().insert(dbTester.getSession(), snapshot);
    return snapshot;
  }

  private void insertMeasure(SnapshotDto analysisDto, ComponentDto componentDto, ProjectMeasureDto projectMeasureDto) {
    projectMeasureDto
      .setAnalysisUuid(analysisDto.getUuid())
      .setComponentUuid(componentDto.uuid());
    dbTester.getDbClient().projectMeasureDao().insert(dbTester.getSession(), projectMeasureDto);
  }
}
