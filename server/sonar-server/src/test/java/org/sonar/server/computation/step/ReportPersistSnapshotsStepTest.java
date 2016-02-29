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
package org.sonar.server.computation.step;

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
import org.sonar.server.computation.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepositoryImpl;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_DATE;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS;


public class ReportPersistSnapshotsStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  @Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();

  System2 system2 = mock(System2.class);

  DbIdsRepositoryImpl dbIdsRepository;

  DbClient dbClient = dbTester.getDbClient();

  long analysisDate;

  long now;

  PersistSnapshotsStep underTest;

  @Before
  public void setup() {
    dbTester.truncateTables();
    analysisDate = DateUtils.parseDateQuietly("2015-06-01").getTime();
    analysisMetadataHolder.setAnalysisDate(analysisDate);
    dbIdsRepository = new DbIdsRepositoryImpl();

    now = DateUtils.parseDateQuietly("2015-06-02").getTime();

    when(system2.now()).thenReturn(now);

    underTest = new PersistSnapshotsStep(system2, dbClient, treeRootHolder, analysisMetadataHolder, dbIdsRepository, periodsHolder);

    // initialize PeriodHolder to empty by default
    periodsHolder.setPeriods();
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void persist_snapshots() {
    ComponentDto projectDto = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), projectDto);
    ComponentDto moduleDto = ComponentTesting.newModuleDto("BCDE", projectDto).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(dbTester.getSession(), moduleDto);
    ComponentDto directoryDto = ComponentTesting.newDirectory(moduleDto, "CDEF", "MODULE_KEY:src/main/java/dir").setKey("MODULE_KEY:src/main/java/dir");
    dbClient.componentDao().insert(dbTester.getSession(), directoryDto);
    ComponentDto fileDto = ComponentTesting.newFileDto(moduleDto, "DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java");
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

    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(4);

    SnapshotDto projectSnapshot = getUnprocessedSnapshot(projectDto.getId());
    assertThat(projectSnapshot.getComponentId()).isEqualTo(projectDto.getId());
    assertThat(projectSnapshot.getRootProjectId()).isEqualTo(projectDto.getId());
    assertThat(projectSnapshot.getRootId()).isNull();
    assertThat(projectSnapshot.getParentId()).isNull();
    assertThat(projectSnapshot.getDepth()).isEqualTo(0);
    assertThat(projectSnapshot.getPath()).isNullOrEmpty();
    assertThat(projectSnapshot.getQualifier()).isEqualTo("TRK");
    assertThat(projectSnapshot.getScope()).isEqualTo("PRJ");
    assertThat(projectSnapshot.getVersion()).isEqualTo("1.0");
    assertThat(projectSnapshot.getLast()).isFalse();
    assertThat(projectSnapshot.getStatus()).isEqualTo("U");
    assertThat(projectSnapshot.getCreatedAt()).isEqualTo(analysisDate);
    assertThat(projectSnapshot.getBuildDate()).isEqualTo(now);

    SnapshotDto moduleSnapshot = getUnprocessedSnapshot(moduleDto.getId());
    assertThat(moduleSnapshot.getComponentId()).isEqualTo(moduleDto.getId());
    assertThat(moduleSnapshot.getRootProjectId()).isEqualTo(projectDto.getId());
    assertThat(moduleSnapshot.getRootId()).isEqualTo(projectSnapshot.getId());
    assertThat(moduleSnapshot.getParentId()).isEqualTo(projectSnapshot.getId());
    assertThat(moduleSnapshot.getDepth()).isEqualTo(1);
    assertThat(moduleSnapshot.getPath()).isEqualTo(projectSnapshot.getId() + ".");
    assertThat(moduleSnapshot.getQualifier()).isEqualTo("BRC");
    assertThat(moduleSnapshot.getScope()).isEqualTo("PRJ");
    assertThat(moduleSnapshot.getVersion()).isEqualTo("1.1");
    assertThat(moduleSnapshot.getLast()).isFalse();
    assertThat(moduleSnapshot.getStatus()).isEqualTo("U");
    assertThat(moduleSnapshot.getCreatedAt()).isEqualTo(analysisDate);
    assertThat(moduleSnapshot.getBuildDate()).isEqualTo(now);

    SnapshotDto directorySnapshot = getUnprocessedSnapshot(directoryDto.getId());
    assertThat(directorySnapshot.getComponentId()).isEqualTo(directoryDto.getId());
    assertThat(directorySnapshot.getRootProjectId()).isEqualTo(projectDto.getId());
    assertThat(directorySnapshot.getRootId()).isEqualTo(projectSnapshot.getId());
    assertThat(directorySnapshot.getParentId()).isEqualTo(moduleSnapshot.getId());
    assertThat(directorySnapshot.getDepth()).isEqualTo(2);
    assertThat(directorySnapshot.getPath()).isEqualTo(projectSnapshot.getId() + "." + moduleSnapshot.getId() + ".");
    assertThat(directorySnapshot.getQualifier()).isEqualTo("DIR");
    assertThat(directorySnapshot.getScope()).isEqualTo("DIR");
    assertThat(directorySnapshot.getVersion()).isNull();
    assertThat(directorySnapshot.getLast()).isFalse();
    assertThat(directorySnapshot.getStatus()).isEqualTo("U");
    assertThat(directorySnapshot.getCreatedAt()).isEqualTo(analysisDate);
    assertThat(directorySnapshot.getBuildDate()).isEqualTo(now);

    SnapshotDto fileSnapshot = getUnprocessedSnapshot(fileDto.getId());
    assertThat(fileSnapshot.getComponentId()).isEqualTo(fileDto.getId());
    assertThat(fileSnapshot.getRootProjectId()).isEqualTo(projectDto.getId());
    assertThat(fileSnapshot.getRootId()).isEqualTo(projectSnapshot.getId());
    assertThat(fileSnapshot.getParentId()).isEqualTo(directorySnapshot.getId());
    assertThat(fileSnapshot.getDepth()).isEqualTo(3);
    assertThat(fileSnapshot.getPath()).isEqualTo(projectSnapshot.getId() + "." + moduleSnapshot.getId() + "." + directorySnapshot.getId() + ".");
    assertThat(fileSnapshot.getQualifier()).isEqualTo("FIL");
    assertThat(fileSnapshot.getScope()).isEqualTo("FIL");
    assertThat(fileSnapshot.getVersion()).isNull();
    assertThat(fileSnapshot.getLast()).isFalse();
    assertThat(fileSnapshot.getStatus()).isEqualTo("U");
    assertThat(fileSnapshot.getCreatedAt()).isEqualTo(analysisDate);
    assertThat(fileSnapshot.getBuildDate()).isEqualTo(now);

    assertThat(dbIdsRepository.getSnapshotId(project)).isEqualTo(projectSnapshot.getId());
    assertThat(dbIdsRepository.getComponentId(module)).isEqualTo(moduleDto.getId());
    assertThat(dbIdsRepository.getComponentId(directory)).isEqualTo(directoryDto.getId());
    assertThat(dbIdsRepository.getComponentId(file)).isEqualTo(fileDto.getId());
  }

  @Test
  public void persist_unit_test() {
    ComponentDto projectDto = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), projectDto);
    ComponentDto moduleDto = ComponentTesting.newModuleDto("BCDE", projectDto).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(dbTester.getSession(), moduleDto);
    ComponentDto directoryDto = ComponentTesting.newDirectory(moduleDto, "CDEF", "MODULE_KEY:src/test/java/dir").setKey("MODULE_KEY:src/test/java/dir");
    dbClient.componentDao().insert(dbTester.getSession(), directoryDto);
    ComponentDto fileDto = ComponentTesting.newFileDto(moduleDto, "DEFG").setKey("MODULE_KEY:src/test/java/dir/FooTest.java").setQualifier("UTS");
    dbClient.componentDao().insert(dbTester.getSession(), fileDto);
    dbTester.getSession().commit();

    Component file = ReportComponent.builder(Component.Type.FILE, 3).setUuid("DEFG").setKey(PROJECT_KEY + ":src/main/java/dir/Foo.java")
      .setFileAttributes(new FileAttributes(true, null)).build();
    Component directory = ReportComponent.builder(Component.Type.DIRECTORY, 2).setUuid("CDEF").setKey(PROJECT_KEY + ":src/main/java/dir").addChildren(file).build();
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(directory).build();
    treeRootHolder.setRoot(project);

    dbIdsRepository.setComponentId(project, projectDto.getId());
    dbIdsRepository.setComponentId(directory, directoryDto.getId());
    dbIdsRepository.setComponentId(file, fileDto.getId());

    underTest.execute();

    SnapshotDto fileSnapshot = getUnprocessedSnapshot(fileDto.getId());
    assertThat(fileSnapshot.getQualifier()).isEqualTo("UTS");
    assertThat(fileSnapshot.getScope()).isEqualTo("FIL");
  }

  @Test
  public void persist_snapshots_on_multi_modules() {
    ComponentDto projectDto = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    dbClient.componentDao().insert(dbTester.getSession(), projectDto);
    ComponentDto moduleADto = ComponentTesting.newModuleDto("BCDE", projectDto).setKey("MODULE_A");
    dbClient.componentDao().insert(dbTester.getSession(), moduleADto);
    ComponentDto subModuleADto = ComponentTesting.newModuleDto("CDEF", moduleADto).setKey("SUB_MODULE_A");
    dbClient.componentDao().insert(dbTester.getSession(), subModuleADto);
    ComponentDto moduleBDto = ComponentTesting.newModuleDto("DEFG", projectDto).setKey("MODULE_B");
    dbClient.componentDao().insert(dbTester.getSession(), moduleBDto);
    dbTester.getSession().commit();

    Component moduleB = ReportComponent.builder(Component.Type.MODULE, 4).setUuid("DEFG").setKey("MODULE_B").build();
    Component subModuleA = ReportComponent.builder(Component.Type.MODULE, 3).setUuid("CDEF").setKey("SUB_MODULE_A").build();
    Component moduleA = ReportComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_A").addChildren(subModuleA).build();
    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(moduleA, moduleB).build();
    treeRootHolder.setRoot(project);

    dbIdsRepository.setComponentId(project, projectDto.getId());
    dbIdsRepository.setComponentId(moduleA, moduleADto.getId());
    dbIdsRepository.setComponentId(subModuleA, subModuleADto.getId());
    dbIdsRepository.setComponentId(moduleB, moduleBDto.getId());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(4);

    SnapshotDto projectSnapshot = getUnprocessedSnapshot(projectDto.getId());
    assertThat(projectSnapshot.getRootProjectId()).isEqualTo(projectDto.getId());
    assertThat(projectSnapshot.getRootId()).isNull();
    assertThat(projectSnapshot.getParentId()).isNull();
    assertThat(projectSnapshot.getDepth()).isEqualTo(0);
    assertThat(projectSnapshot.getPath()).isNullOrEmpty();

    SnapshotDto moduleASnapshot = getUnprocessedSnapshot(moduleADto.getId());
    assertThat(moduleASnapshot.getRootProjectId()).isEqualTo(projectDto.getId());
    assertThat(moduleASnapshot.getRootId()).isEqualTo(projectSnapshot.getId());
    assertThat(moduleASnapshot.getParentId()).isEqualTo(projectSnapshot.getId());
    assertThat(moduleASnapshot.getDepth()).isEqualTo(1);
    assertThat(moduleASnapshot.getPath()).isEqualTo(projectSnapshot.getId() + ".");

    SnapshotDto subModuleASnapshot = getUnprocessedSnapshot(subModuleADto.getId());
    assertThat(subModuleASnapshot.getRootProjectId()).isEqualTo(projectDto.getId());
    assertThat(subModuleASnapshot.getRootId()).isEqualTo(projectSnapshot.getId());
    assertThat(subModuleASnapshot.getParentId()).isEqualTo(moduleASnapshot.getId());
    assertThat(subModuleASnapshot.getDepth()).isEqualTo(2);
    assertThat(subModuleASnapshot.getPath()).isEqualTo(projectSnapshot.getId() + "." + moduleASnapshot.getId() + ".");

    SnapshotDto moduleBSnapshot = getUnprocessedSnapshot(moduleBDto.getId());
    assertThat(moduleBSnapshot.getRootProjectId()).isEqualTo(projectDto.getId());
    assertThat(moduleBSnapshot.getRootId()).isEqualTo(projectSnapshot.getId());
    assertThat(moduleBSnapshot.getParentId()).isEqualTo(projectSnapshot.getId());
    assertThat(moduleBSnapshot.getDepth()).isEqualTo(1);
    assertThat(moduleBSnapshot.getPath()).isEqualTo(projectSnapshot.getId() + ".");
  }

  @Test
  public void persist_snapshots_with_periods() {
    ComponentDto projectDto = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), projectDto);
    SnapshotDto snapshotDto = SnapshotTesting.newSnapshotForProject(projectDto).setCreatedAt(DateUtils.parseDateQuietly("2015-01-01").getTime());
    dbClient.snapshotDao().insert(dbTester.getSession(), snapshotDto);
    dbTester.getSession().commit();
    periodsHolder.setPeriods(new Period(1, TIMEMACHINE_MODE_DATE, "2015-01-01", analysisDate, 123L));

    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setComponentId(project, projectDto.getId());

    underTest.execute();

    SnapshotDto projectSnapshot = getUnprocessedSnapshot(projectDto.getId());
    assertThat(projectSnapshot.getPeriodMode(1)).isEqualTo(TIMEMACHINE_MODE_DATE);
    assertThat(projectSnapshot.getPeriodDate(1)).isEqualTo(analysisDate);
    assertThat(projectSnapshot.getPeriodModeParameter(1)).isNotNull();
  }

  @Test
  public void only_persist_snapshots_with_periods_on_project_and_module() {
    periodsHolder.setPeriods(new Period(1, TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, null, analysisDate, 123L));

    ComponentDto projectDto = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), projectDto);
    SnapshotDto projectSnapshot = SnapshotTesting.newSnapshotForProject(projectDto);
    dbClient.snapshotDao().insert(dbTester.getSession(), projectSnapshot);

    ComponentDto moduleDto = ComponentTesting.newModuleDto("BCDE", projectDto).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(dbTester.getSession(), moduleDto);
    SnapshotDto moduleSnapshot = SnapshotTesting.createForComponent(moduleDto, projectSnapshot);
    dbClient.snapshotDao().insert(dbTester.getSession(), moduleSnapshot);

    ComponentDto directoryDto = ComponentTesting.newDirectory(moduleDto, "CDEF", "MODULE_KEY:src/main/java/dir").setKey("MODULE_KEY:src/main/java/dir");
    dbClient.componentDao().insert(dbTester.getSession(), directoryDto);
    SnapshotDto directorySnapshot = SnapshotTesting.createForComponent(directoryDto, moduleSnapshot);
    dbClient.snapshotDao().insert(dbTester.getSession(), directorySnapshot);

    ComponentDto fileDto = ComponentTesting.newFileDto(moduleDto, "DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(dbTester.getSession(), fileDto);
    SnapshotDto fileSnapshot = SnapshotTesting.createForComponent(fileDto, directorySnapshot);
    dbClient.snapshotDao().insert(dbTester.getSession(), fileSnapshot);

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

    SnapshotDto newProjectSnapshot = getUnprocessedSnapshot(projectDto.getId());
    assertThat(newProjectSnapshot.getPeriodMode(1)).isEqualTo(TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);

    SnapshotDto newModuleSnapshot = getUnprocessedSnapshot(moduleDto.getId());
    assertThat(newModuleSnapshot.getPeriodMode(1)).isEqualTo(TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);

    SnapshotDto newDirectorySnapshot = getUnprocessedSnapshot(directoryDto.getId());
    assertThat(newDirectorySnapshot.getPeriodMode(1)).isNull();

    SnapshotDto newFileSnapshot = getUnprocessedSnapshot(fileDto.getId());
    assertThat(newFileSnapshot.getPeriodMode(1)).isNull();
  }

  @Test
  public void set_no_period_on_snapshots_when_no_period() {
    ComponentDto projectDto = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), projectDto);
    SnapshotDto snapshotDto = SnapshotTesting.newSnapshotForProject(projectDto);
    dbClient.snapshotDao().insert(dbTester.getSession(), snapshotDto);
    dbTester.getSession().commit();

    Component project = ReportComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setComponentId(project, projectDto.getId());

    underTest.execute();

    SnapshotDto projectSnapshot = getUnprocessedSnapshot(projectDto.getId());
    assertThat(projectSnapshot.getPeriodMode(1)).isNull();
    assertThat(projectSnapshot.getPeriodDate(1)).isNull();
    assertThat(projectSnapshot.getPeriodModeParameter(1)).isNull();
  }

  private SnapshotDto getUnprocessedSnapshot(long componentId) {
    List<SnapshotDto> projectSnapshots = dbClient.snapshotDao().selectSnapshotsByQuery(dbTester.getSession(),
      new SnapshotQuery().setComponentId(componentId).setIsLast(false).setStatus(SnapshotDto.STATUS_UNPROCESSED));
    assertThat(projectSnapshots).hasSize(1);
    return projectSnapshots.get(0);
  }

}
