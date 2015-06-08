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

import java.text.SimpleDateFormat;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.component.SnapshotDto;
import org.sonar.core.component.SnapshotQuery;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.component.db.SnapshotDao;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DbIdsRepository;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(DbTests.class)
public class PersistSnapshotStepTest extends BaseStepTest {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

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

    analysisDate = DATE_FORMAT.parse("2015-06-01").getTime();
    reportReader.setMetadata(BatchReport.Metadata.newBuilder()
      .setAnalysisDate(analysisDate)
      .build());
    dbIdsRepository = new DbIdsRepository();

    now = DATE_FORMAT.parse("2015-06-02").getTime();

    when(system2.now()).thenReturn(now);

    sut = new PersistSnapshotsStep(system2, dbClient, treeRootHolder, reportReader, dbIdsRepository);
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

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setVersion("1.0")
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setVersion("1.1")
      .addChildRef(3)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .addChildRef(4)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .build());

    Component file = new DumbComponent(Component.Type.FILE, 4, "DEFG", "MODULE_KEY:src/main/java/dir/Foo.java");
    Component directory = new DumbComponent(Component.Type.DIRECTORY, 3, "CDEF", "MODULE_KEY:src/main/java/dir", file);
    Component module = new DumbComponent(Component.Type.MODULE, 2, "BCDE", "MODULE_KEY", directory);
    Component project = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY, module);
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

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .addChildRef(2)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.DIRECTORY)
      .addChildRef(3)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setIsTest(true)
      .build());

    Component file = new DumbComponent(Component.Type.FILE, 3, "DEFG", PROJECT_KEY + ":src/test/java/dir/FooTest.java");
    Component directory = new DumbComponent(Component.Type.DIRECTORY, 2, "CDEF", PROJECT_KEY + ":src/test/java/dir", file);
    Component project = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY, directory);
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

    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .addChildRef(2)
      .addChildRef(4)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .addChildRef(3)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.MODULE)
      .build());
    reportReader.putComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.MODULE)
      .build());

    Component moduleB = new DumbComponent(Component.Type.MODULE, 4, "DEFG", "MODULE_B");
    Component subModuleA = new DumbComponent(Component.Type.MODULE, 3, "CDEF", "SUB_MODULE_A");
    Component moduleA = new DumbComponent(Component.Type.MODULE, 2, "BCDE", "MODULE_A", subModuleA);
    Component project = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY, moduleA, moduleB);
    treeRootHolder.setRoot(project);

    treeRootHolder.setRoot(new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY,
      new DumbComponent(Component.Type.MODULE, 2, "BCDE", "MODULE_A",
        new DumbComponent(Component.Type.MODULE, 3, "DEFG", "SUB_MODULE_A")),
      new DumbComponent(Component.Type.MODULE, 4, "CDEF", "MODULE_B")));

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
    assertThat(moduleBSnapshot.getPath()).isEqualTo(projectSnapshot.getId() + "." );
  }

  private SnapshotDto getUnprocessedSnapshot(long componentId){
    List<SnapshotDto> projectSnapshots = dbClient.snapshotDao().selectSnapshotsByQuery(session,
      new SnapshotQuery().setComponentId(componentId).setIsLast(false).setStatus(SnapshotDto.STATUS_UNPROCESSED));
    assertThat(projectSnapshots).hasSize(1);
    return projectSnapshots.get(0);
  }

}
