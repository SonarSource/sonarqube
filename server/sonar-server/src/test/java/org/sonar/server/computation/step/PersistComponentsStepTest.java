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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ComponentTreeBuilders;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.db.DbClient;
import org.sonar.test.DbTests;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Category(DbTests.class)
public class PersistComponentsStepTest extends BaseStepTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @ClassRule
  public static DbTester dbTester = new DbTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File reportDir;

  DbSession session;

  DbClient dbClient;

  Settings projectSettings;
  LanguageRepository languageRepository;
  DbComponentsRefCache dbComponentsRefCache;

  PersistComponentsStep sut;

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();
    session = dbTester.myBatis().openSession(false);
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new ComponentDao());

    reportDir = temp.newFolder();

    projectSettings = new Settings();
    languageRepository = mock(LanguageRepository.class);
    dbComponentsRefCache = new DbComponentsRefCache();
    sut = new PersistComponentsStep(dbClient, dbComponentsRefCache);
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
  public void persist_components() throws Exception {
    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("Project")
      .setDescription("Project description")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .setName("Module")
      .setDescription("Module description")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .setLanguage("java")
      .build());

    DumbComponent root = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY,
      new DumbComponent(Component.Type.MODULE, 2, "BCDE", "MODULE_KEY",
        new DumbComponent(Component.Type.DIRECTORY, 3, "CDEF", "MODULE_KEY:src/main/java/dir",
          new DumbComponent(Component.Type.FILE, 4, "DEFG", "MODULE_KEY:src/main/java/dir/Foo.java"))));
    sut.execute(new ComputationContext(new BatchReportReader(reportDir), PROJECT_KEY, projectSettings,
      dbClient, ComponentTreeBuilders.from(root), languageRepository));

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(4);

    ComponentDto project = dbClient.componentDao().selectNullableByKey(session, PROJECT_KEY);
    assertThat(project).isNotNull();
    assertThat(project.name()).isEqualTo("Project");
    assertThat(project.description()).isEqualTo("Project description");
    assertThat(project.uuid()).isEqualTo("ABCD");
    assertThat(project.moduleUuid()).isNull();
    assertThat(project.moduleUuidPath()).isEqualTo("." + project.uuid() + ".");
    assertThat(project.projectUuid()).isEqualTo(project.uuid());
    assertThat(project.qualifier()).isEqualTo("TRK");
    assertThat(project.scope()).isEqualTo("PRJ");
    assertThat(project.parentProjectId()).isNull();

    ComponentDto module = dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY");
    assertThat(module).isNotNull();
    assertThat(module.name()).isEqualTo("Module");
    assertThat(module.description()).isEqualTo("Module description");
    assertThat(module.uuid()).isEqualTo("BCDE");
    assertThat(module.moduleUuid()).isEqualTo(project.uuid());
    assertThat(module.moduleUuidPath()).isEqualTo(project.moduleUuidPath() + module.uuid() + ".");
    assertThat(module.projectUuid()).isEqualTo(project.uuid());
    assertThat(module.qualifier()).isEqualTo("BRC");
    assertThat(module.scope()).isEqualTo("PRJ");
    assertThat(module.parentProjectId()).isEqualTo(project.getId());

    ComponentDto directory = dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY:src/main/java/dir");
    assertThat(directory).isNotNull();
    assertThat(directory.name()).isEqualTo("src/main/java/dir");
    assertThat(directory.path()).isEqualTo("src/main/java/dir");
    assertThat(directory.uuid()).isEqualTo("CDEF");
    assertThat(directory.moduleUuid()).isEqualTo(module.uuid());
    assertThat(directory.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(directory.projectUuid()).isEqualTo(project.uuid());
    assertThat(directory.qualifier()).isEqualTo("DIR");
    assertThat(directory.scope()).isEqualTo("DIR");
    assertThat(directory.parentProjectId()).isEqualTo(module.getId());

    ComponentDto file = dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY:src/main/java/dir/Foo.java");
    assertThat(file).isNotNull();
    assertThat(file.name()).isEqualTo("Foo.java");
    assertThat(file.path()).isEqualTo("src/main/java/dir/Foo.java");
    assertThat(file.language()).isEqualTo("java");
    assertThat(file.uuid()).isEqualTo("DEFG");
    assertThat(file.moduleUuid()).isEqualTo(module.uuid());
    assertThat(file.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(file.projectUuid()).isEqualTo(project.uuid());
    assertThat(file.qualifier()).isEqualTo("FIL");
    assertThat(file.scope()).isEqualTo("FIL");
    assertThat(file.parentProjectId()).isEqualTo(module.getId());

    assertThat(dbComponentsRefCache.getByRef(1).getId()).isEqualTo(project.getId());
    assertThat(dbComponentsRefCache.getByRef(2).getId()).isEqualTo(module.getId());
    assertThat(dbComponentsRefCache.getByRef(3).getId()).isEqualTo(directory.getId());
    assertThat(dbComponentsRefCache.getByRef(4).getId()).isEqualTo(file.getId());
  }

  @Test
  public void persist_file_directly_attached_on_root_directory() throws Exception {
    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("Project")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("/")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setPath("pom.xml")
      .build());

    DumbComponent root = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY,
      new DumbComponent(Component.Type.DIRECTORY, 2, "CDEF", PROJECT_KEY + ":/",
        new DumbComponent(Component.Type.FILE, 3, "DEFG", PROJECT_KEY + ":pom.xml")));
    sut.execute(new ComputationContext(new BatchReportReader(reportDir), PROJECT_KEY, projectSettings,
      dbClient, ComponentTreeBuilders.from(root), languageRepository));

    ComponentDto directory = dbClient.componentDao().selectNullableByKey(session, "PROJECT_KEY:/");
    assertThat(directory).isNotNull();
    assertThat(directory.name()).isEqualTo("/");
    assertThat(directory.path()).isEqualTo("/");

    ComponentDto file = dbClient.componentDao().selectNullableByKey(session, "PROJECT_KEY:pom.xml");
    assertThat(file).isNotNull();
    assertThat(file.name()).isEqualTo("pom.xml");
    assertThat(file.path()).isEqualTo("pom.xml");
  }

  @Test
  public void persist_unit_test() throws Exception {
    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("Project")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/test/java/dir")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/test/java/dir/FooTest.java")
      .setIsTest(true)
      .build());

    DumbComponent root = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY,
      new DumbComponent(Component.Type.DIRECTORY, 2, "CDEF", PROJECT_KEY + ":src/test/java/dir",
        new DumbComponent(Component.Type.FILE, 3, "DEFG", PROJECT_KEY + ":src/test/java/dir/FooTest.java")));
    sut.execute(new ComputationContext(new BatchReportReader(reportDir), PROJECT_KEY, projectSettings,
      dbClient, ComponentTreeBuilders.from(root), languageRepository));

    ComponentDto file = dbClient.componentDao().selectNullableByKey(session, PROJECT_KEY + ":src/test/java/dir/FooTest.java");
    assertThat(file).isNotNull();
    assertThat(file.name()).isEqualTo("FooTest.java");
    assertThat(file.path()).isEqualTo("src/test/java/dir/FooTest.java");
    assertThat(file.qualifier()).isEqualTo("UTS");
    assertThat(file.scope()).isEqualTo("FIL");
  }

  @Test
  public void persist_only_new_components() throws Exception {
    // Project amd module already exists
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(session, project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(session, module);
    session.commit();

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("Project")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .setName("Module")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());

    DumbComponent root = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY,
      new DumbComponent(Component.Type.MODULE, 2, "BCDE", "MODULE_KEY",
        new DumbComponent(Component.Type.DIRECTORY, 3, "CDEF", "MODULE_KEY:src/main/java/dir",
          new DumbComponent(Component.Type.FILE, 4, "DEFG", "MODULE_KEY:src/main/java/dir/Foo.java"))));
    sut.execute(new ComputationContext(new BatchReportReader(reportDir), PROJECT_KEY, projectSettings,
      dbClient, ComponentTreeBuilders.from(root), languageRepository));

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(4);

    ComponentDto projectReloaded = dbClient.componentDao().selectNullableByKey(session, PROJECT_KEY);
    assertThat(projectReloaded.getId()).isEqualTo(project.getId());
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());

    ComponentDto moduleReloaded = dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY");
    assertThat(moduleReloaded.getId()).isEqualTo(module.getId());
    assertThat(moduleReloaded.uuid()).isEqualTo(module.uuid());
    assertThat(moduleReloaded.moduleUuid()).isEqualTo(module.moduleUuid());
    assertThat(moduleReloaded.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(moduleReloaded.projectUuid()).isEqualTo(module.projectUuid());
    assertThat(moduleReloaded.parentProjectId()).isEqualTo(module.parentProjectId());

    ComponentDto directory = dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY:src/main/java/dir");
    assertThat(directory).isNotNull();
    assertThat(directory.moduleUuid()).isEqualTo(module.uuid());
    assertThat(directory.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(directory.projectUuid()).isEqualTo(project.uuid());
    assertThat(directory.parentProjectId()).isEqualTo(module.getId());

    ComponentDto file = dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY:src/main/java/dir/Foo.java");
    assertThat(file).isNotNull();
    assertThat(file.moduleUuid()).isEqualTo(module.uuid());
    assertThat(file.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(file.projectUuid()).isEqualTo(project.uuid());
    assertThat(file.parentProjectId()).isEqualTo(module.getId());
  }

  @Test
  public void compute_parent_project_id() throws Exception {
    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("Project")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .setName("Module")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.MODULE)
      .setKey("SUB_MODULE_1_KEY")
      .setName("Sub Module 1")
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.MODULE)
      .setKey("SUB_MODULE_2_KEY")
      .setName("Sub Module 2")
      .addChildRef(5)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(5)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .build());

    DumbComponent root = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY,
      new DumbComponent(Component.Type.MODULE, 2, "BCDE", "MODULE_KEY",
        new DumbComponent(Component.Type.MODULE, 3, "CDEF", "SUB_MODULE_1_KEY",
          new DumbComponent(Component.Type.MODULE, 4, "DEFG", "SUB_MODULE_2_KEY",
            new DumbComponent(Component.Type.DIRECTORY, 5, "EFGH", "SUB_MODULE_2_KEY:src/main/java/dir")))));
    sut.execute(new ComputationContext(new BatchReportReader(reportDir), PROJECT_KEY, projectSettings,
      dbClient, ComponentTreeBuilders.from(root), languageRepository));

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(5);

    ComponentDto project = dbClient.componentDao().selectNullableByKey(session, PROJECT_KEY);
    assertThat(project).isNotNull();
    assertThat(project.parentProjectId()).isNull();

    ComponentDto module = dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY");
    assertThat(module).isNotNull();
    assertThat(module.parentProjectId()).isEqualTo(project.getId());

    ComponentDto subModule1 = dbClient.componentDao().selectNullableByKey(session, "SUB_MODULE_1_KEY");
    assertThat(subModule1).isNotNull();
    assertThat(subModule1.parentProjectId()).isEqualTo(project.getId());

    ComponentDto subModule2 = dbClient.componentDao().selectNullableByKey(session, "SUB_MODULE_2_KEY");
    assertThat(subModule2).isNotNull();
    assertThat(subModule2.parentProjectId()).isEqualTo(project.getId());

    ComponentDto directory = dbClient.componentDao().selectNullableByKey(session, "SUB_MODULE_2_KEY:src/main/java/dir");
    assertThat(directory).isNotNull();
    assertThat(directory.parentProjectId()).isEqualTo(subModule2.getId());
  }

  @Test
  public void nothing_to_persist() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(session, project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(session, module);
    ComponentDto directory = ComponentTesting.newDirectory(module, "src/main/java/dir").setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir");
    ComponentDto file = ComponentTesting.newFileDto(module, "DEFG").setPath("src/main/java/dir/Foo.java").setName("Foo.java").setKey("MODULE_KEY:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(session, directory, file);
    session.commit();

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("Project")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .setName("Module")
      .addChildRef(3)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());

    DumbComponent root = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY,
      new DumbComponent(Component.Type.MODULE, 2, "BCDE", "MODULE_KEY",
        new DumbComponent(Component.Type.DIRECTORY, 3, "CDEF", "MODULE_KEY:src/main/java/dir",
          new DumbComponent(Component.Type.FILE, 4, "DEFG", "MODULE_KEY:src/main/java/dir/Foo.java"))));
    sut.execute(new ComputationContext(new BatchReportReader(reportDir), PROJECT_KEY, projectSettings,
      dbClient, ComponentTreeBuilders.from(root), languageRepository));

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(4);
    assertThat(dbClient.componentDao().selectNullableByKey(session, PROJECT_KEY).getId()).isEqualTo(project.getId());
    assertThat(dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY").getId()).isEqualTo(module.getId());
    assertThat(dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY:src/main/java/dir").getId()).isEqualTo(directory.getId());
    assertThat(dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY:src/main/java/dir/Foo.java").getId()).isEqualTo(file.getId());

    ComponentDto projectReloaded = dbClient.componentDao().selectNullableByKey(session, PROJECT_KEY);
    assertThat(projectReloaded.getId()).isEqualTo(project.getId());
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());
    assertThat(projectReloaded.moduleUuid()).isEqualTo(project.moduleUuid());
    assertThat(projectReloaded.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(projectReloaded.projectUuid()).isEqualTo(project.projectUuid());
    assertThat(projectReloaded.parentProjectId()).isEqualTo(project.parentProjectId());

    ComponentDto moduleReloaded = dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY");
    assertThat(moduleReloaded.getId()).isEqualTo(module.getId());
    assertThat(moduleReloaded.uuid()).isEqualTo(module.uuid());
    assertThat(moduleReloaded.moduleUuid()).isEqualTo(module.moduleUuid());
    assertThat(moduleReloaded.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(moduleReloaded.projectUuid()).isEqualTo(module.projectUuid());
    assertThat(moduleReloaded.parentProjectId()).isEqualTo(module.parentProjectId());

    ComponentDto directoryReloaded = dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY:src/main/java/dir");
    assertThat(directoryReloaded).isNotNull();
    assertThat(directoryReloaded.uuid()).isEqualTo(directory.uuid());
    assertThat(directoryReloaded.moduleUuid()).isEqualTo(directory.moduleUuid());
    assertThat(directoryReloaded.moduleUuidPath()).isEqualTo(directory.moduleUuidPath());
    assertThat(directoryReloaded.projectUuid()).isEqualTo(directory.projectUuid());
    assertThat(directoryReloaded.parentProjectId()).isEqualTo(directory.parentProjectId());
    assertThat(directoryReloaded.name()).isEqualTo(directory.name());
    assertThat(directoryReloaded.path()).isEqualTo(directory.path());

    ComponentDto fileReloaded = dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY:src/main/java/dir/Foo.java");
    assertThat(fileReloaded).isNotNull();
    assertThat(fileReloaded.uuid()).isEqualTo(file.uuid());
    assertThat(fileReloaded.moduleUuid()).isEqualTo(file.moduleUuid());
    assertThat(fileReloaded.moduleUuidPath()).isEqualTo(file.moduleUuidPath());
    assertThat(fileReloaded.projectUuid()).isEqualTo(file.projectUuid());
    assertThat(fileReloaded.parentProjectId()).isEqualTo(file.parentProjectId());
    assertThat(fileReloaded.name()).isEqualTo(file.name());
    assertThat(fileReloaded.path()).isEqualTo(file.path());
  }

  @Test
  public void update_name_and_description() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(session, project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(session, module);
    session.commit();

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("New project name")
      .setDescription("New project description")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_KEY")
      .setName("New module name")
      .setDescription("New module description")
      .build());

    DumbComponent root = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY,
      new DumbComponent(Component.Type.MODULE, 2, "BCDE", "MODULE_KEY"));
    sut.execute(new ComputationContext(new BatchReportReader(reportDir), PROJECT_KEY, projectSettings,
      dbClient, ComponentTreeBuilders.from(root), languageRepository));

    ComponentDto projectReloaded = dbClient.componentDao().selectNullableByKey(session, PROJECT_KEY);
    assertThat(projectReloaded.name()).isEqualTo("New project name");
    assertThat(projectReloaded.description()).isEqualTo("New project description");

    ComponentDto moduleReloaded = dbClient.componentDao().selectNullableByKey(session, "MODULE_KEY");
    assertThat(moduleReloaded.name()).isEqualTo("New module name");
    assertThat(moduleReloaded.description()).isEqualTo("New module description");
  }

  @Test
  public void update_module_uuid_when_moving_a_module() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(session, project);
    ComponentDto moduleA = ComponentTesting.newModuleDto("EDCB", project).setKey("MODULE_A").setName("Module A");
    ComponentDto moduleB = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_B").setName("Module B");
    dbClient.componentDao().insert(session, moduleA, moduleB);
    ComponentDto directory = ComponentTesting.newDirectory(moduleB, "src/main/java/dir").setUuid("CDEF").setKey("MODULE_B:src/main/java/dir");
    ComponentDto file = ComponentTesting.newFileDto(moduleB, "DEFG").setPath("src/main/java/dir/Foo.java").setName("Foo.java").setKey("MODULE_B:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(session, directory, file);
    session.commit();

    File reportDir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());

    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT)
      .setKey(PROJECT_KEY)
      .setName("Project")
      .addChildRef(2)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(2)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_A")
      .setName("Module A")
      .addChildRef(3)
      .build());
    // Module B is now a sub module of module A
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(3)
      .setType(Constants.ComponentType.MODULE)
      .setKey("MODULE_B")
      .setName("Module B")
      .addChildRef(4)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(4)
      .setType(Constants.ComponentType.DIRECTORY)
      .setPath("src/main/java/dir")
      .addChildRef(5)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(5)
      .setType(Constants.ComponentType.FILE)
      .setPath("src/main/java/dir/Foo.java")
      .build());

    DumbComponent root = new DumbComponent(Component.Type.PROJECT, 1, "ABCD", PROJECT_KEY,
      new DumbComponent(Component.Type.MODULE, 2, "EDCB", "MODULE_A",
        new DumbComponent(Component.Type.MODULE, 3, "BCDE", "MODULE_B",
          new DumbComponent(Component.Type.DIRECTORY, 4, "CDEF", "MODULE_B:src/main/java/dir",
            new DumbComponent(Component.Type.FILE, 5, "DEFG", "MODULE_B:src/main/java/dir/Foo.java")))));
    sut.execute(new ComputationContext(new BatchReportReader(reportDir), PROJECT_KEY, projectSettings,
      dbClient, ComponentTreeBuilders.from(root), languageRepository));

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(5);

    ComponentDto moduleAreloaded = dbClient.componentDao().selectNullableByKey(session, "MODULE_A");
    assertThat(moduleAreloaded).isNotNull();

    ComponentDto moduleBReloaded = dbClient.componentDao().selectNullableByKey(session, "MODULE_B");
    assertThat(moduleBReloaded).isNotNull();
    assertThat(moduleBReloaded.uuid()).isEqualTo(moduleB.uuid());
    assertThat(moduleBReloaded.moduleUuid()).isEqualTo(moduleAreloaded.uuid());
    assertThat(moduleBReloaded.moduleUuidPath()).isEqualTo(moduleAreloaded.moduleUuidPath() + moduleBReloaded.uuid() + ".");
    assertThat(moduleBReloaded.projectUuid()).isEqualTo(project.uuid());
    assertThat(moduleBReloaded.parentProjectId()).isEqualTo(project.getId());

    ComponentDto directoryReloaded = dbClient.componentDao().selectNullableByKey(session, "MODULE_B:src/main/java/dir");
    assertThat(directoryReloaded).isNotNull();
    assertThat(directoryReloaded.uuid()).isEqualTo(directory.uuid());
    assertThat(directoryReloaded.moduleUuid()).isEqualTo(moduleBReloaded.uuid());
    assertThat(directoryReloaded.moduleUuidPath()).isEqualTo(moduleBReloaded.moduleUuidPath());
    assertThat(directoryReloaded.projectUuid()).isEqualTo(project.uuid());
    assertThat(directoryReloaded.parentProjectId()).isEqualTo(moduleBReloaded.getId());

    ComponentDto fileReloaded = dbClient.componentDao().selectNullableByKey(session, "MODULE_B:src/main/java/dir/Foo.java");
    assertThat(fileReloaded).isNotNull();
    assertThat(fileReloaded.uuid()).isEqualTo(file.uuid());
    assertThat(fileReloaded.moduleUuid()).isEqualTo(moduleBReloaded.uuid());
    assertThat(fileReloaded.moduleUuidPath()).isEqualTo(moduleBReloaded.moduleUuidPath());
    assertThat(fileReloaded.projectUuid()).isEqualTo(project.uuid());
    assertThat(fileReloaded.parentProjectId()).isEqualTo(moduleBReloaded.getId());
  }

}
