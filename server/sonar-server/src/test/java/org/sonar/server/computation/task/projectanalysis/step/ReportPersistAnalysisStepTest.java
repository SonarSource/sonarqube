/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.component.SnapshotQuery;
import org.sonar.db.component.SnapshotTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.DbIdsRepositoryImpl;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_DATE;
import static org.sonar.core.config.CorePropertyDefinitions.LEAK_PERIOD_MODE_PREVIOUS_ANALYSIS;

public class ReportPersistAnalysisStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String ANALYSIS_UUID = "U1";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public PeriodHolderRule periodsHolder = new PeriodHolderRule();

  private System2 system2 = mock(System2.class);

  private DbIdsRepositoryImpl dbIdsRepository;

  private DbClient dbClient = dbTester.getDbClient();

  private long analysisDate;

  private long now;

  PersistAnalysisStep underTest;

  @Before
  public void setup() {
    analysisDate = DateUtils.parseDateQuietly("2015-06-01").getTime();
    analysisMetadataHolder.setUuid(ANALYSIS_UUID);
    analysisMetadataHolder.setAnalysisDate(analysisDate);
    dbIdsRepository = new DbIdsRepositoryImpl();

    now = DateUtils.parseDateQuietly("2015-06-02").getTime();

    when(system2.now()).thenReturn(now);

    underTest = new PersistAnalysisStep(system2, dbClient, treeRootHolder, analysisMetadataHolder, periodsHolder);

    // initialize PeriodHolder to empty by default
    periodsHolder.setPeriod(null);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void persist_analysis() {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto projectDto = ComponentTesting.newProjectDto(organizationDto, "ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), projectDto);
    ComponentDto moduleDto = ComponentTesting.newModuleDto("BCDE", projectDto).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(dbTester.getSession(), moduleDto);
    ComponentDto directoryDto = ComponentTesting.newDirectory(moduleDto, "CDEF", "MODULE_KEY:src/main/java/dir").setKey("MODULE_KEY:src/main/java/dir");
    dbClient.componentDao().insert(dbTester.getSession(), directoryDto);
    ComponentDto fileDto = ComponentTesting.newFileDto(moduleDto, directoryDto, "DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(dbTester.getSession(), fileDto);
    dbTester.getSession().commit();

    Component file = ReportComponent.builder(Component.Type.FILE, 4).setUuid("DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java").build();
    Component directory = ReportComponent.builder(Component.Type.DIRECTORY, 3).setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir").addChildren(file).build();
    Component module = ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY").setVersion("1.1").addChildren(directory).build();
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).setVersion("1.0").addChildren(module).build();
    treeRootHolder.setRoot(project);

    dbIdsRepository.setComponentId(project, projectDto.getId());
    dbIdsRepository.setComponentId(module, moduleDto.getId());
    dbIdsRepository.setComponentId(directory, directoryDto.getId());
    dbIdsRepository.setComponentId(file, fileDto.getId());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(1);

    SnapshotDto projectSnapshot = getUnprocessedSnapshot(projectDto.uuid());
    assertThat(projectSnapshot.getUuid()).isEqualTo(ANALYSIS_UUID);
    assertThat(projectSnapshot.getComponentUuid()).isEqualTo(project.getUuid());
    assertThat(projectSnapshot.getVersion()).isEqualTo("1.0");
    assertThat(projectSnapshot.getLast()).isFalse();
    assertThat(projectSnapshot.getStatus()).isEqualTo("U");
    assertThat(projectSnapshot.getCreatedAt()).isEqualTo(analysisDate);
    assertThat(projectSnapshot.getBuildDate()).isEqualTo(now);

    assertThat(dbIdsRepository.getComponentId(module)).isEqualTo(moduleDto.getId());
    assertThat(dbIdsRepository.getComponentId(directory)).isEqualTo(directoryDto.getId());
    assertThat(dbIdsRepository.getComponentId(file)).isEqualTo(fileDto.getId());
  }

  @Test
  public void persist_snapshots_with_leak_period() {
    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto projectDto = ComponentTesting.newProjectDto(organizationDto, "ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), projectDto);
    SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(projectDto).setCreatedAt(DateUtils.parseDateQuietly("2015-01-01").getTime());
    dbClient.snapshotDao().insert(dbTester.getSession(), snapshotDto);
    dbTester.getSession().commit();
    periodsHolder.setPeriod(new Period(LEAK_PERIOD_MODE_DATE, "2015-01-01", analysisDate, "u1"));

    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setComponentId(project, projectDto.getId());

    underTest.execute();

    SnapshotDto projectSnapshot = getUnprocessedSnapshot(projectDto.uuid());
    assertThat(projectSnapshot.getPeriodMode()).isEqualTo(LEAK_PERIOD_MODE_DATE);
    assertThat(projectSnapshot.getPeriodDate()).isEqualTo(analysisDate);
    assertThat(projectSnapshot.getPeriodModeParameter()).isNotNull();
  }

  @Test
  public void only_persist_snapshots_with_leak_period_on_project_and_module() {
    periodsHolder.setPeriod(new Period(LEAK_PERIOD_MODE_PREVIOUS_ANALYSIS, null, analysisDate, "u1"));

    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto projectDto = ComponentTesting.newProjectDto(organizationDto, "ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), projectDto);
    SnapshotDto projectSnapshot = SnapshotTesting.newAnalysis(projectDto);
    dbClient.snapshotDao().insert(dbTester.getSession(), projectSnapshot);

    ComponentDto moduleDto = ComponentTesting.newModuleDto("BCDE", projectDto).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(dbTester.getSession(), moduleDto);

    ComponentDto directoryDto = ComponentTesting.newDirectory(moduleDto, "CDEF", "MODULE_KEY:src/main/java/dir").setKey("MODULE_KEY:src/main/java/dir");
    dbClient.componentDao().insert(dbTester.getSession(), directoryDto);

    ComponentDto fileDto = ComponentTesting.newFileDto(moduleDto, directoryDto, "DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(dbTester.getSession(), fileDto);

    dbTester.getSession().commit();

    Component file = ReportComponent.builder(Component.Type.FILE, 4).setUuid("DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java").build();
    Component directory = ReportComponent.builder(Component.Type.DIRECTORY, 3).setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir").addChildren(file).build();
    Component module = ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY").addChildren(directory).build();
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(module).build();
    treeRootHolder.setRoot(project);

    dbIdsRepository.setComponentId(project, projectDto.getId());
    dbIdsRepository.setComponentId(module, moduleDto.getId());
    dbIdsRepository.setComponentId(directory, directoryDto.getId());
    dbIdsRepository.setComponentId(file, fileDto.getId());

    underTest.execute();

    SnapshotDto newProjectSnapshot = getUnprocessedSnapshot(projectDto.uuid());
    assertThat(newProjectSnapshot.getPeriodMode()).isEqualTo(LEAK_PERIOD_MODE_PREVIOUS_ANALYSIS);
  }

  @Test
  public void set_no_period_on_snapshots_when_no_period() {
    ComponentDto projectDto = ComponentTesting.newProjectDto(dbTester.organizations().insert(), "ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), projectDto);
    SnapshotDto snapshotDto = SnapshotTesting.newAnalysis(projectDto);
    dbClient.snapshotDao().insert(dbTester.getSession(), snapshotDto);
    dbTester.getSession().commit();

    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setComponentId(project, projectDto.getId());

    underTest.execute();

    SnapshotDto projectSnapshot = getUnprocessedSnapshot(projectDto.uuid());
    assertThat(projectSnapshot.getPeriodMode()).isNull();
    assertThat(projectSnapshot.getPeriodDate()).isNull();
    assertThat(projectSnapshot.getPeriodModeParameter()).isNull();
  }

  private SnapshotDto getUnprocessedSnapshot(String componentUuid) {
    List<SnapshotDto> projectSnapshots = dbClient.snapshotDao().selectAnalysesByQuery(dbTester.getSession(),
      new SnapshotQuery().setComponentUuid(componentUuid).setIsLast(false).setStatus(SnapshotDto.STATUS_UNPROCESSED));
    assertThat(projectSnapshots).hasSize(1);
    return projectSnapshots.get(0);
  }

}
