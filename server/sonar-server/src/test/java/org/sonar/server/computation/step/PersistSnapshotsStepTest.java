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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.component.db.SnapshotQuery;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.SnapshotTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class PersistSnapshotsStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  @Rule
  public PeriodsHolderRule periodsHolderRule = new PeriodsHolderRule();

  System2 system2 = mock(System2.class);

  DbIdsRepository dbIdsRepository;

  DbSession session;

  DbClient dbClient;

  long analysisDate;

  long now;

  PersistSnapshotsStep sut;

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao(), new SnapshotDao());

    analysisDate = DateUtils.parseDateQuietly("2015-06-01").getTime();
    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setAnalysisDate(analysisDate)
      .build());
    dbIdsRepository = new DbIdsRepository();

    now = DateUtils.parseDateQuietly("2015-06-02").getTime();

    when(system2.now()).thenReturn(now);

    sut = new PersistSnapshotsStep(system2, dbClient, treeRootHolder, reportReader, dbIdsRepository, periodsHolderRule);
  }

  @Override
  protected ComputationStep step() {
    return sut;
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void persist_snapshots() throws Exception {
    ComponentDto projectDto = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(session, projectDto);
    ComponentDto moduleDto = ComponentTesting.newModuleDto("BCDE", projectDto).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(session, moduleDto);
    ComponentDto directoryDto = ComponentTesting.newDirectory(moduleDto, "CDEF", "MODULE_KEY:src/main/java/dir").setKey("MODULE_KEY:src/main/java/dir");
    dbClient.componentDao().insert(session, directoryDto);
    ComponentDto fileDto = ComponentTesting.newFileDto(moduleDto, "DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(session, fileDto);
    session.commit();

    Component file = DumbComponent.builder(Component.Type.FILE, 4).setUuid("DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java").build();
    Component directory = DumbComponent.builder(Component.Type.DIRECTORY, 3).setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir").addChildren(file).build();
    Component module = DumbComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY").setVersion("1.1").addChildren(directory).build();
    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).setVersion("1.0").addChildren(module).build();
    treeRootHolder.setRoot(project);

    dbIdsRepository.setComponentId(project, projectDto.getId());
    dbIdsRepository.setComponentId(module, moduleDto.getId());
    dbIdsRepository.setComponentId(directory, directoryDto.getId());
    dbIdsRepository.setComponentId(file, fileDto.getId());

    sut.execute();

    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(4);

    SnapshotDto projectSnapshot = getUnprocessedSnapshot(projectDto.getId());
    assertThat(projectSnapshot.getComponentId()).isEqualTo(projectDto.getId());
    assertThat(projectSnapshot.getRootProjectId()).isEqualTo(projectDto.getId());
    assertThat(projectSnapshot.getRootId()).isNull();
    assertThat(projectSnapshot.getParentId()).isNull();
    assertThat(projectSnapshot.getDepth()).isEqualTo(0);
    assertThat(projectSnapshot.getPath()).isEqualTo("");
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
  public void persist_unit_test() throws Exception {
    ComponentDto projectDto = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(session, projectDto);
    ComponentDto moduleDto = ComponentTesting.newModuleDto("BCDE", projectDto).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(session, moduleDto);
    ComponentDto directoryDto = ComponentTesting.newDirectory(moduleDto, "CDEF", "MODULE_KEY:src/test/java/dir").setKey("MODULE_KEY:src/test/java/dir");
    dbClient.componentDao().insert(session, directoryDto);
    ComponentDto fileDto = ComponentTesting.newFileDto(moduleDto, "DEFG").setKey("MODULE_KEY:src/test/java/dir/FooTest.java").setQualifier("UTS");
    dbClient.componentDao().insert(session, fileDto);
    session.commit();

    Component file = DumbComponent.builder(Component.Type.FILE, 3).setUuid("DEFG").setKey(PROJECT_KEY + ":src/main/java/dir/Foo.java").setUnitTest(true).build();
    Component directory = DumbComponent.builder(Component.Type.DIRECTORY, 2).setUuid("CDEF").setKey(PROJECT_KEY + ":src/main/java/dir").addChildren(file).build();
    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(directory).build();
    treeRootHolder.setRoot(project);

    dbIdsRepository.setComponentId(project, projectDto.getId());
    dbIdsRepository.setComponentId(directory, directoryDto.getId());
    dbIdsRepository.setComponentId(file, fileDto.getId());

    sut.execute();

    SnapshotDto fileSnapshot = getUnprocessedSnapshot(fileDto.getId());
    assertThat(fileSnapshot.getQualifier()).isEqualTo("UTS");
    assertThat(fileSnapshot.getScope()).isEqualTo("FIL");
  }

  @Test
  public void persist_snapshots_on_multi_modules() throws Exception {
    ComponentDto projectDto = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY);
    dbClient.componentDao().insert(session, projectDto);
    ComponentDto moduleADto = ComponentTesting.newModuleDto("BCDE", projectDto).setKey("MODULE_A");
    dbClient.componentDao().insert(session, moduleADto);
    ComponentDto subModuleADto = ComponentTesting.newModuleDto("CDEF", moduleADto).setKey("SUB_MODULE_A");
    dbClient.componentDao().insert(session, subModuleADto);
    ComponentDto moduleBDto = ComponentTesting.newModuleDto("DEFG", projectDto).setKey("MODULE_B");
    dbClient.componentDao().insert(session, moduleBDto);
    session.commit();

    Component moduleB = DumbComponent.builder(Component.Type.MODULE, 4).setUuid("DEFG").setKey("MODULE_B").build();
    Component subModuleA = DumbComponent.builder(Component.Type.MODULE, 3).setUuid("CDEF").setKey("SUB_MODULE_A").build();
    Component moduleA = DumbComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_A").addChildren(subModuleA).build();
    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(moduleA, moduleB).build();
    treeRootHolder.setRoot(project);

    dbIdsRepository.setComponentId(project, projectDto.getId());
    dbIdsRepository.setComponentId(moduleA, moduleADto.getId());
    dbIdsRepository.setComponentId(subModuleA, subModuleADto.getId());
    dbIdsRepository.setComponentId(moduleB, moduleBDto.getId());

    sut.execute();

    assertThat(dbTester.countRowsOfTable("snapshots")).isEqualTo(4);

    SnapshotDto projectSnapshot = getUnprocessedSnapshot(projectDto.getId());
    assertThat(projectSnapshot.getRootProjectId()).isEqualTo(projectDto.getId());
    assertThat(projectSnapshot.getRootId()).isNull();
    assertThat(projectSnapshot.getParentId()).isNull();
    assertThat(projectSnapshot.getDepth()).isEqualTo(0);
    assertThat(projectSnapshot.getPath()).isEqualTo("");

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
  public void persist_snapshots_with_periods() throws Exception {
    ComponentDto projectDto = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(session, projectDto);
    SnapshotDto snapshotDto = SnapshotTesting.createForProject(projectDto).setCreatedAt(DateUtils.parseDateQuietly("2015-01-01").getTime());
    dbClient.snapshotDao().insert(session, snapshotDto);
    session.commit();
    periodsHolderRule.addPeriod(new Period(1, CoreProperties.TIMEMACHINE_MODE_DATE, "2015-01-01", analysisDate));

    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setComponentId(project, projectDto.getId());

    sut.execute();

    SnapshotDto projectSnapshot = getUnprocessedSnapshot(projectDto.getId());
    assertThat(projectSnapshot.getPeriodMode(1)).isEqualTo(CoreProperties.TIMEMACHINE_MODE_DATE);
    assertThat(projectSnapshot.getPeriodDate(1)).isEqualTo(analysisDate);
    assertThat(projectSnapshot.getPeriodModeParameter(1)).isNotNull();
  }

  @Test
  public void only_persist_snapshots_with_periods_on_project_and_module() throws Exception {
    periodsHolderRule.addPeriod(new Period(1, CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, null, analysisDate));

    ComponentDto projectDto = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(session, projectDto);
    SnapshotDto projectSnapshot = SnapshotTesting.createForProject(projectDto);
    dbClient.snapshotDao().insert(session, projectSnapshot);

    ComponentDto moduleDto = ComponentTesting.newModuleDto("BCDE", projectDto).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(session, moduleDto);
    SnapshotDto moduleSnapshot = SnapshotTesting.createForComponent(moduleDto, projectSnapshot);
    dbClient.snapshotDao().insert(session, moduleSnapshot);

    ComponentDto directoryDto = ComponentTesting.newDirectory(moduleDto, "CDEF", "MODULE_KEY:src/main/java/dir").setKey("MODULE_KEY:src/main/java/dir");
    dbClient.componentDao().insert(session, directoryDto);
    SnapshotDto directorySnapshot = SnapshotTesting.createForComponent(directoryDto, moduleSnapshot);
    dbClient.snapshotDao().insert(session, directorySnapshot);

    ComponentDto fileDto = ComponentTesting.newFileDto(moduleDto, "DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(session, fileDto);
    SnapshotDto fileSnapshot = SnapshotTesting.createForComponent(fileDto, directorySnapshot);
    dbClient.snapshotDao().insert(session, fileSnapshot);

    session.commit();

    Component file = DumbComponent.builder(Component.Type.FILE, 4).setUuid("DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java").build();
    Component directory = DumbComponent.builder(Component.Type.DIRECTORY, 3).setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir").addChildren(file).build();
    Component module = DumbComponent.builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY").addChildren(directory).build();
    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).addChildren(module).build();
    treeRootHolder.setRoot(project);

    dbIdsRepository.setComponentId(project, projectDto.getId());
    dbIdsRepository.setComponentId(module, moduleDto.getId());
    dbIdsRepository.setComponentId(directory, directoryDto.getId());
    dbIdsRepository.setComponentId(file, fileDto.getId());

    sut.execute();

    SnapshotDto newProjectSnapshot = getUnprocessedSnapshot(projectDto.getId());
    assertThat(newProjectSnapshot.getPeriodMode(1)).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);

    SnapshotDto newModuleSnapshot = getUnprocessedSnapshot(moduleDto.getId());
    assertThat(newModuleSnapshot.getPeriodMode(1)).isEqualTo(CoreProperties.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS);

    SnapshotDto newDirectorySnapshot = getUnprocessedSnapshot(directoryDto.getId());
    assertThat(newDirectorySnapshot.getPeriodMode(1)).isNull();

    SnapshotDto newFileSnapshot = getUnprocessedSnapshot(fileDto.getId());
    assertThat(newFileSnapshot.getPeriodMode(1)).isNull();
  }

  @Test
  public void set_no_period_on_snapshots_when_no_period() throws Exception {
    ComponentDto projectDto = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(session, projectDto);
    SnapshotDto snapshotDto = SnapshotTesting.createForProject(projectDto);
    dbClient.snapshotDao().insert(session, snapshotDto);
    session.commit();

    Component project = DumbComponent.builder(Component.Type.PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).build();
    treeRootHolder.setRoot(project);
    dbIdsRepository.setComponentId(project, projectDto.getId());

    sut.execute();

    SnapshotDto projectSnapshot = getUnprocessedSnapshot(projectDto.getId());
    assertThat(projectSnapshot.getPeriodMode(1)).isNull();
    assertThat(projectSnapshot.getPeriodDate(1)).isNull();
    assertThat(projectSnapshot.getPeriodModeParameter(1)).isNull();
  }

  private SnapshotDto getUnprocessedSnapshot(long componentId) {
    List<SnapshotDto> projectSnapshots = dbClient.snapshotDao().selectSnapshotsByQuery(session,
      new SnapshotQuery().setComponentId(componentId).setIsLast(false).setStatus(SnapshotDto.STATUS_UNPROCESSED));
    assertThat(projectSnapshots).hasSize(1);
    return projectSnapshots.get(0);
  }

}
