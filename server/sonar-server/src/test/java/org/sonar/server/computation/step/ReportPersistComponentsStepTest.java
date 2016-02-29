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

import com.google.common.base.Optional;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.FileAttributes;
import org.sonar.server.computation.component.MutableDbIdsRepositoryRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.computation.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.component.Component.Type.FILE;
import static org.sonar.server.computation.component.Component.Type.PROJECT;
import static org.sonar.server.computation.component.ReportComponent.builder;


public class ReportPersistComponentsStepTest extends BaseStepTest {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MutableDbIdsRepositoryRule dbIdsRepository = MutableDbIdsRepositoryRule.create(treeRootHolder);

  System2 system2 = mock(System2.class);

  DbClient dbClient = dbTester.getDbClient();

  Date now;

  PersistComponentsStep underTest;

  @Before
  public void setup() throws Exception {
    dbTester.truncateTables();

    now = DATE_FORMAT.parse("2015-06-02");
    when(system2.now()).thenReturn(now.getTime());

    underTest = new PersistComponentsStep(dbClient, treeRootHolder, dbIdsRepository, system2);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void persist_components() {
    Component file = builder(FILE, 4).setUuid("DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java")
      .setPath("src/main/java/dir/Foo.java")
      .setFileAttributes(new FileAttributes(false, "java"))
      .build();
    Component directory = builder(DIRECTORY, 3).setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir")
      .setPath("src/main/java/dir")
      .addChildren(file)
      .build();
    Component module = builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY")
      .setPath("module")
      .setName("Module")
      .setDescription("Module description")
      .addChildren(directory)
      .build();
    Component project = builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
      .setName("Project")
      .setDescription("Project description")
      .addChildren(module)
      .build();
    treeRootHolder.setRoot(project);

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(4);

    ComponentDto projectDto = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY).get();
    assertThat(projectDto.name()).isEqualTo("Project");
    assertThat(projectDto.description()).isEqualTo("Project description");
    assertThat(projectDto.path()).isNull();
    assertThat(projectDto.uuid()).isEqualTo("ABCD");
    assertThat(projectDto.moduleUuid()).isNull();
    assertThat(projectDto.moduleUuidPath()).isEqualTo("." + projectDto.uuid() + ".");
    assertThat(projectDto.projectUuid()).isEqualTo(projectDto.uuid());
    assertThat(projectDto.qualifier()).isEqualTo("TRK");
    assertThat(projectDto.scope()).isEqualTo("PRJ");
    assertThat(projectDto.parentProjectId()).isNull();
    assertThat(projectDto.getCreatedAt()).isEqualTo(now);

    ComponentDto moduleDto = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY").get();
    assertThat(moduleDto.name()).isEqualTo("Module");
    assertThat(moduleDto.description()).isEqualTo("Module description");
    assertThat(moduleDto.path()).isEqualTo("module");
    assertThat(moduleDto.uuid()).isEqualTo("BCDE");
    assertThat(moduleDto.moduleUuid()).isEqualTo(projectDto.uuid());
    assertThat(moduleDto.moduleUuidPath()).isEqualTo(projectDto.moduleUuidPath() + moduleDto.uuid() + ".");
    assertThat(moduleDto.projectUuid()).isEqualTo(projectDto.uuid());
    assertThat(moduleDto.qualifier()).isEqualTo("BRC");
    assertThat(moduleDto.scope()).isEqualTo("PRJ");
    assertThat(moduleDto.parentProjectId()).isEqualTo(projectDto.getId());
    assertThat(moduleDto.getCreatedAt()).isEqualTo(now);

    ComponentDto directoryDto = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir").get();
    assertThat(directoryDto.name()).isEqualTo("src/main/java/dir");
    assertThat(directoryDto.description()).isNull();
    assertThat(directoryDto.path()).isEqualTo("src/main/java/dir");
    assertThat(directoryDto.uuid()).isEqualTo("CDEF");
    assertThat(directoryDto.moduleUuid()).isEqualTo(moduleDto.uuid());
    assertThat(directoryDto.moduleUuidPath()).isEqualTo(moduleDto.moduleUuidPath());
    assertThat(directoryDto.projectUuid()).isEqualTo(projectDto.uuid());
    assertThat(directoryDto.qualifier()).isEqualTo("DIR");
    assertThat(directoryDto.scope()).isEqualTo("DIR");
    assertThat(directoryDto.parentProjectId()).isEqualTo(moduleDto.getId());
    assertThat(directoryDto.getCreatedAt()).isEqualTo(now);

    ComponentDto fileDto = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileDto.name()).isEqualTo("Foo.java");
    assertThat(fileDto.description()).isNull();
    assertThat(fileDto.path()).isEqualTo("src/main/java/dir/Foo.java");
    assertThat(fileDto.language()).isEqualTo("java");
    assertThat(fileDto.uuid()).isEqualTo("DEFG");
    assertThat(fileDto.moduleUuid()).isEqualTo(moduleDto.uuid());
    assertThat(fileDto.moduleUuidPath()).isEqualTo(moduleDto.moduleUuidPath());
    assertThat(fileDto.projectUuid()).isEqualTo(projectDto.uuid());
    assertThat(fileDto.qualifier()).isEqualTo("FIL");
    assertThat(fileDto.scope()).isEqualTo("FIL");
    assertThat(fileDto.parentProjectId()).isEqualTo(moduleDto.getId());
    assertThat(fileDto.getCreatedAt()).isEqualTo(now);

    assertThat(dbIdsRepository.getComponentId(project)).isEqualTo(projectDto.getId());
    assertThat(dbIdsRepository.getComponentId(module)).isEqualTo(moduleDto.getId());
    assertThat(dbIdsRepository.getComponentId(directory)).isEqualTo(directoryDto.getId());
    assertThat(dbIdsRepository.getComponentId(file)).isEqualTo(fileDto.getId());
  }

  @Test
  public void persist_file_directly_attached_on_root_directory() {
    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY).setName("Project")
        .addChildren(
          builder(DIRECTORY, 2).setUuid("CDEF").setKey(PROJECT_KEY + ":/").setPath("/")
            .addChildren(
              builder(FILE, 3).setUuid("DEFG").setKey(PROJECT_KEY + ":pom.xml").setPath("pom.xml")
                .build())
            .build())
        .build());

    underTest.execute();

    ComponentDto directory = dbClient.componentDao().selectByKey(dbTester.getSession(), "PROJECT_KEY:/").get();
    assertThat(directory.name()).isEqualTo("/");
    assertThat(directory.path()).isEqualTo("/");

    ComponentDto file = dbClient.componentDao().selectByKey(dbTester.getSession(), "PROJECT_KEY:pom.xml").get();
    assertThat(file.name()).isEqualTo("pom.xml");
    assertThat(file.path()).isEqualTo("pom.xml");
  }

  @Test
  public void persist_unit_test() {
    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(DIRECTORY, 2).setUuid("CDEF").setKey(PROJECT_KEY + ":src/test/java/dir")
            .setPath("src/test/java/dir")
            .addChildren(
              builder(FILE, 3).setUuid("DEFG").setKey(PROJECT_KEY + ":src/test/java/dir/FooTest.java")
                .setPath("src/test/java/dir/FooTest.java")
                .setFileAttributes(new FileAttributes(true, null))
                .build())
            .build())
        .build());

    underTest.execute();

    ComponentDto file = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY + ":src/test/java/dir/FooTest.java").get();
    assertThat(file.name()).isEqualTo("FooTest.java");
    assertThat(file.path()).isEqualTo("src/test/java/dir/FooTest.java");
    assertThat(file.qualifier()).isEqualTo("UTS");
    assertThat(file.scope()).isEqualTo("FIL");
  }

  @Test
  public void persist_only_new_components() {
    // Project amd module already exists
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY")
            .setName("Module")
            .addChildren(
              builder(DIRECTORY, 3).setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir")
                .setPath("src/main/java/dir")
                .addChildren(
                  builder(FILE, 4).setUuid("DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java")
                    .setPath("src/main/java/dir/Foo.java")
                    .build())
                .build())
            .build())
        .build());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(4);

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY).get();
    assertThat(projectReloaded.getId()).isEqualTo(project.getId());
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());

    ComponentDto moduleReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY").get();
    assertThat(moduleReloaded.getId()).isEqualTo(module.getId());
    assertThat(moduleReloaded.uuid()).isEqualTo(module.uuid());
    assertThat(moduleReloaded.moduleUuid()).isEqualTo(module.moduleUuid());
    assertThat(moduleReloaded.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(moduleReloaded.projectUuid()).isEqualTo(module.projectUuid());
    assertThat(moduleReloaded.parentProjectId()).isEqualTo(module.parentProjectId());

    ComponentDto directory = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir").get();
    assertThat(directory.moduleUuid()).isEqualTo(module.uuid());
    assertThat(directory.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(directory.projectUuid()).isEqualTo(project.uuid());
    assertThat(directory.parentProjectId()).isEqualTo(module.getId());

    ComponentDto file = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get();
    assertThat(file.moduleUuid()).isEqualTo(module.uuid());
    assertThat(file.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(file.projectUuid()).isEqualTo(project.uuid());
    assertThat(file.parentProjectId()).isEqualTo(module.getId());
  }

  @Test
  public void compute_parent_project_id() {
    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY")
            .setName("Module")
            .addChildren(
              builder(Component.Type.MODULE, 3).setUuid("CDEF").setKey("SUB_MODULE_1_KEY")
                .setName("Sub Module 1")
                .addChildren(
                  builder(Component.Type.MODULE, 4).setUuid("DEFG").setKey("SUB_MODULE_2_KEY")
                    .setName("Sub Module 2")
                    .addChildren(
                      builder(DIRECTORY, 5).setUuid("EFGH").setKey("SUB_MODULE_2_KEY:src/main/java/dir")
                        .setPath("src/main/java/dir")
                        .build())
                    .build())
                .build())
            .build())
        .build());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(5);

    Optional<ComponentDto> project = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY);
    assertThat(project).isPresent();
    assertThat(project.get().parentProjectId()).isNull();

    Optional<ComponentDto> module = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY");
    assertThat(module).isPresent();
    assertThat(module.get().parentProjectId()).isEqualTo(project.get().getId());

    Optional<ComponentDto> subModule1 = dbClient.componentDao().selectByKey(dbTester.getSession(), "SUB_MODULE_1_KEY");
    assertThat(subModule1).isPresent();
    assertThat(subModule1.get().parentProjectId()).isEqualTo(project.get().getId());

    Optional<ComponentDto> subModule2 = dbClient.componentDao().selectByKey(dbTester.getSession(), "SUB_MODULE_2_KEY");
    assertThat(subModule2).isPresent();
    assertThat(subModule2.get().parentProjectId()).isEqualTo(project.get().getId());

    Optional<ComponentDto> directory = dbClient.componentDao().selectByKey(dbTester.getSession(), "SUB_MODULE_2_KEY:src/main/java/dir");
    assertThat(directory).isPresent();
    assertThat(directory.get().parentProjectId()).isEqualTo(subModule2.get().getId());
  }

  @Test
  public void persist_multi_modules() {
    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_A")
            .setName("Module A")
            .addChildren(
              builder(Component.Type.MODULE, 3).setUuid("DEFG").setKey("SUB_MODULE_A")
                .setName("Sub Module A")
                .build())
            .build(),
          builder(Component.Type.MODULE, 4).setUuid("CDEF").setKey("MODULE_B")
            .setName("Module B")
            .build())
        .build());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(4);

    ComponentDto project = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY).get();
    assertThat(project.moduleUuid()).isNull();
    assertThat(project.moduleUuidPath()).isEqualTo("." + project.uuid() + ".");
    assertThat(project.parentProjectId()).isNull();

    ComponentDto moduleA = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_A").get();
    assertThat(moduleA.moduleUuid()).isEqualTo(project.uuid());
    assertThat(moduleA.moduleUuidPath()).isEqualTo(project.moduleUuidPath() + moduleA.uuid() + ".");
    assertThat(moduleA.parentProjectId()).isEqualTo(project.getId());

    ComponentDto subModuleA = dbClient.componentDao().selectByKey(dbTester.getSession(), "SUB_MODULE_A").get();
    assertThat(subModuleA.moduleUuid()).isEqualTo(moduleA.uuid());
    assertThat(subModuleA.moduleUuidPath()).isEqualTo(moduleA.moduleUuidPath() + subModuleA.uuid() + ".");
    assertThat(subModuleA.parentProjectId()).isEqualTo(project.getId());

    ComponentDto moduleB = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_B").get();
    assertThat(moduleB.moduleUuid()).isEqualTo(project.uuid());
    assertThat(moduleB.moduleUuidPath()).isEqualTo(project.moduleUuidPath() + moduleB.uuid() + ".");
    assertThat(moduleB.parentProjectId()).isEqualTo(project.getId());
  }

  @Test
  public void nothing_to_persist() {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(dbTester.getSession(), module);
    ComponentDto directory = ComponentTesting.newDirectory(module, "src/main/java/dir").setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir");
    ComponentDto file = ComponentTesting.newFileDto(module, "DEFG").setPath("src/main/java/dir/Foo.java").setName("Foo.java").setKey("MODULE_KEY:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(dbTester.getSession(), directory, file);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY")
            .setName("Module")
            .addChildren(
              builder(DIRECTORY, 3).setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir")
                .setPath("src/main/java/dir")
                .addChildren(
                  builder(FILE, 4).setUuid("DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java")
                    .setPath("src/main/java/dir/Foo.java")
                    .build())
                .build())
            .build())
        .build());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(4);
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY).get().getId()).isEqualTo(project.getId());
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY").get().getId()).isEqualTo(module.getId());
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir").get().getId()).isEqualTo(directory.getId());
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get().getId()).isEqualTo(file.getId());

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY).get();
    assertThat(projectReloaded.getId()).isEqualTo(project.getId());
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());
    assertThat(projectReloaded.moduleUuid()).isEqualTo(project.moduleUuid());
    assertThat(projectReloaded.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(projectReloaded.projectUuid()).isEqualTo(project.projectUuid());
    assertThat(projectReloaded.parentProjectId()).isEqualTo(project.parentProjectId());

    ComponentDto moduleReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY").get();
    assertThat(moduleReloaded.getId()).isEqualTo(module.getId());
    assertThat(moduleReloaded.uuid()).isEqualTo(module.uuid());
    assertThat(moduleReloaded.moduleUuid()).isEqualTo(module.moduleUuid());
    assertThat(moduleReloaded.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(moduleReloaded.projectUuid()).isEqualTo(module.projectUuid());
    assertThat(moduleReloaded.parentProjectId()).isEqualTo(module.parentProjectId());

    ComponentDto directoryReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir").get();
    assertThat(directoryReloaded.uuid()).isEqualTo(directory.uuid());
    assertThat(directoryReloaded.moduleUuid()).isEqualTo(directory.moduleUuid());
    assertThat(directoryReloaded.moduleUuidPath()).isEqualTo(directory.moduleUuidPath());
    assertThat(directoryReloaded.projectUuid()).isEqualTo(directory.projectUuid());
    assertThat(directoryReloaded.parentProjectId()).isEqualTo(directory.parentProjectId());
    assertThat(directoryReloaded.name()).isEqualTo(directory.name());
    assertThat(directoryReloaded.path()).isEqualTo(directory.path());

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileReloaded.uuid()).isEqualTo(file.uuid());
    assertThat(fileReloaded.moduleUuid()).isEqualTo(file.moduleUuid());
    assertThat(fileReloaded.moduleUuidPath()).isEqualTo(file.moduleUuidPath());
    assertThat(fileReloaded.projectUuid()).isEqualTo(file.projectUuid());
    assertThat(fileReloaded.parentProjectId()).isEqualTo(file.parentProjectId());
    assertThat(fileReloaded.name()).isEqualTo(file.name());
    assertThat(fileReloaded.path()).isEqualTo(file.path());
  }

  @Test
  public void update_module_name() {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY").setName("Module").setPath("path");
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("New project name")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY")
            .setName("New module name")
            .setPath("New path")
            .build())
        .build());

    underTest.execute();

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY).get();
    assertThat(projectReloaded.name()).isEqualTo("New project name");

    ComponentDto moduleReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY").get();
    assertThat(moduleReloaded.name()).isEqualTo("New module name");
  }

  @Test
  public void update_module_description() {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project").setDescription("Project description");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY").setName("Module");
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .setDescription("New project description")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY")
            .setName("Module")
            .setDescription("New module description")
            .build())
        .build());

    underTest.execute();

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY).get();
    assertThat(projectReloaded.description()).isEqualTo("New project description");

    ComponentDto moduleReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY").get();
    assertThat(moduleReloaded.description()).isEqualTo("New module description");
  }

  @Test
  public void update_module_path() {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY").setName("Module").setPath("path");
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY")
            .setName("Module")
            .setPath("New path")
            .build())
        .build());

    underTest.execute();

    ComponentDto moduleReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY").get();
    assertThat(moduleReloaded.path()).isEqualTo("New path");
  }

  @Test
  public void update_module_uuid_when_moving_a_module() {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto moduleA = ComponentTesting.newModuleDto("EDCB", project).setKey("MODULE_A").setName("Module A");
    ComponentDto moduleB = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_B").setName("Module B");
    dbClient.componentDao().insert(dbTester.getSession(), moduleA, moduleB);
    ComponentDto directory = ComponentTesting.newDirectory(moduleB, "src/main/java/dir").setUuid("CDEF").setKey("MODULE_B:src/main/java/dir");
    ComponentDto file = ComponentTesting.newFileDto(moduleB, "DEFG").setPath("src/main/java/dir/Foo.java").setName("Foo.java").setKey("MODULE_B:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(dbTester.getSession(), directory, file);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("EDCB").setKey("MODULE_A")
            .setName("Module A")
            .addChildren(
              builder(Component.Type.MODULE, 3).setUuid("BCDE").setKey("MODULE_B")
                .setName("Module B")
                .addChildren(
                  builder(DIRECTORY, 4).setUuid("CDEF").setKey("MODULE_B:src/main/java/dir")
                    .setPath("src/main/java/dir")
                    .addChildren(
                      builder(FILE, 5).setUuid("DEFG").setKey("MODULE_B:src/main/java/dir/Foo.java")
                        .setPath("src/main/java/dir/Foo.java")
                        .build())
                    .build())
                .build())
            .build())
        .build());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(5);

    ComponentDto moduleAreloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_A").get();

    ComponentDto moduleBReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_B").get();
    assertThat(moduleBReloaded).isNotNull();
    assertThat(moduleBReloaded.uuid()).isEqualTo(moduleB.uuid());
    assertThat(moduleBReloaded.moduleUuid()).isEqualTo(moduleAreloaded.uuid());
    assertThat(moduleBReloaded.moduleUuidPath()).isEqualTo(moduleAreloaded.moduleUuidPath() + moduleBReloaded.uuid() + ".");
    assertThat(moduleBReloaded.projectUuid()).isEqualTo(project.uuid());
    assertThat(moduleBReloaded.parentProjectId()).isEqualTo(project.getId());

    ComponentDto directoryReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_B:src/main/java/dir").get();
    assertThat(directoryReloaded).isNotNull();
    assertThat(directoryReloaded.uuid()).isEqualTo(directory.uuid());
    assertThat(directoryReloaded.moduleUuid()).isEqualTo(moduleBReloaded.uuid());
    assertThat(directoryReloaded.moduleUuidPath()).isEqualTo(moduleBReloaded.moduleUuidPath());
    assertThat(directoryReloaded.projectUuid()).isEqualTo(project.uuid());
    assertThat(directoryReloaded.parentProjectId()).isEqualTo(moduleBReloaded.getId());

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_B:src/main/java/dir/Foo.java").get();
    assertThat(fileReloaded).isNotNull();
    assertThat(fileReloaded.uuid()).isEqualTo(file.uuid());
    assertThat(fileReloaded.moduleUuid()).isEqualTo(moduleBReloaded.uuid());
    assertThat(fileReloaded.moduleUuidPath()).isEqualTo(moduleBReloaded.moduleUuidPath());
    assertThat(fileReloaded.projectUuid()).isEqualTo(project.uuid());
    assertThat(fileReloaded.parentProjectId()).isEqualTo(moduleBReloaded.getId());
  }

  @Test
  public void do_not_update_created_at_on_existing_component() {
    Date oldDate = DateUtils.parseDate("2015-01-01");
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project").setCreatedAt(oldDate);
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY").setName("Module").setPath("path").setCreatedAt(oldDate);
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("New project name")
        .build());

    underTest.execute();

    Optional<ComponentDto> projectReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY);
    assertThat(projectReloaded.get().name()).isEqualTo("New project name");
    assertThat(projectReloaded.get().getCreatedAt()).isNotEqualTo(now);
  }

  @Test
  public void persist_components_that_were_previously_removed() {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto removedModule = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY").setName("Module").setEnabled(false);
    dbClient.componentDao().insert(dbTester.getSession(), removedModule);
    ComponentDto removedDirectory = ComponentTesting.newDirectory(removedModule, "src/main/java/dir").setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir").setEnabled(false);
    ComponentDto removedFile = ComponentTesting.newFileDto(removedModule, "DEFG").setPath("src/main/java/dir/Foo.java").setName("Foo.java")
      .setKey("MODULE_KEY:src/main/java/dir/Foo.java").setEnabled(false);
    dbClient.componentDao().insert(dbTester.getSession(), removedDirectory, removedFile);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY")
            .setName("Module")
            .addChildren(
              builder(DIRECTORY, 3).setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir")
                .setPath("src/main/java/dir")
                .addChildren(
                  builder(FILE, 4).setUuid("DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java")
                    .setPath("src/main/java/dir/Foo.java")
                    .build())
                .build())
            .build())
        .build());

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(4);
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY).get().getId()).isEqualTo(project.getId());
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY").get().getId()).isEqualTo(removedModule.getId());
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir").get().getId()).isEqualTo(removedDirectory.getId());
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get().getId()).isEqualTo(removedFile.getId());

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY).get();
    assertThat(projectReloaded.getId()).isEqualTo(project.getId());
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());
    assertThat(projectReloaded.moduleUuid()).isEqualTo(project.moduleUuid());
    assertThat(projectReloaded.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(projectReloaded.projectUuid()).isEqualTo(project.projectUuid());
    assertThat(projectReloaded.parentProjectId()).isEqualTo(project.parentProjectId());
    assertThat(projectReloaded.isEnabled()).isTrue();

    ComponentDto moduleReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY").get();
    assertThat(moduleReloaded.getId()).isEqualTo(removedModule.getId());
    assertThat(moduleReloaded.uuid()).isEqualTo(removedModule.uuid());
    assertThat(moduleReloaded.moduleUuid()).isEqualTo(removedModule.moduleUuid());
    assertThat(moduleReloaded.moduleUuidPath()).isEqualTo(removedModule.moduleUuidPath());
    assertThat(moduleReloaded.projectUuid()).isEqualTo(removedModule.projectUuid());
    assertThat(moduleReloaded.parentProjectId()).isEqualTo(removedModule.parentProjectId());
    assertThat(moduleReloaded.isEnabled()).isTrue();

    ComponentDto directoryReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir").get();
    assertThat(directoryReloaded.getId()).isEqualTo(removedDirectory.getId());
    assertThat(directoryReloaded.uuid()).isEqualTo(removedDirectory.uuid());
    assertThat(directoryReloaded.moduleUuid()).isEqualTo(removedDirectory.moduleUuid());
    assertThat(directoryReloaded.moduleUuidPath()).isEqualTo(removedDirectory.moduleUuidPath());
    assertThat(directoryReloaded.projectUuid()).isEqualTo(removedDirectory.projectUuid());
    assertThat(directoryReloaded.parentProjectId()).isEqualTo(removedDirectory.parentProjectId());
    assertThat(directoryReloaded.name()).isEqualTo(removedDirectory.name());
    assertThat(directoryReloaded.path()).isEqualTo(removedDirectory.path());
    assertThat(directoryReloaded.isEnabled()).isTrue();

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileReloaded.getId()).isEqualTo(fileReloaded.getId());
    assertThat(fileReloaded.uuid()).isEqualTo(removedFile.uuid());
    assertThat(fileReloaded.moduleUuid()).isEqualTo(removedFile.moduleUuid());
    assertThat(fileReloaded.moduleUuidPath()).isEqualTo(removedFile.moduleUuidPath());
    assertThat(fileReloaded.projectUuid()).isEqualTo(removedFile.projectUuid());
    assertThat(fileReloaded.parentProjectId()).isEqualTo(removedFile.parentProjectId());
    assertThat(fileReloaded.name()).isEqualTo(removedFile.name());
    assertThat(fileReloaded.path()).isEqualTo(removedFile.path());
    assertThat(fileReloaded.isEnabled()).isTrue();
  }

  @Test
  public void update_uuid_when_reactivating_removed_component() {
    ComponentDto project = ComponentTesting.newProjectDto("ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey("MODULE_KEY").setName("Module");
    ComponentDto removedModule = ComponentTesting.newModuleDto("EDCD", project).setKey("REMOVED_MODULE_KEY").setName("Removed Module").setEnabled(false);
    dbClient.componentDao().insert(dbTester.getSession(), module, removedModule);
    ComponentDto directory = ComponentTesting.newDirectory(module, "src/main/java/dir").setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir");
    // The file was attached to another module
    ComponentDto removedFile = ComponentTesting.newFileDto(removedModule, "DEFG").setPath("src/main/java/dir/Foo.java").setName("Foo.java")
      .setKey("MODULE_KEY:src/main/java/dir/Foo.java").setEnabled(false);
    dbClient.componentDao().insert(dbTester.getSession(), directory, removedFile);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE_KEY")
            .setName("Module")
            .addChildren(
              builder(DIRECTORY, 3).setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir")
                .setPath("src/main/java/dir")
                .addChildren(
                  builder(FILE, 4).setUuid("DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java")
                    .setPath("src/main/java/dir/Foo.java")
                    .build())
                .build())
            .build())
        .build());

    underTest.execute();

    // Projects contains 4 components from the report + one removed module
    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(5);

    ComponentDto moduleReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY").get();

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileReloaded.getId()).isEqualTo(removedFile.getId());
    assertThat(fileReloaded.uuid()).isEqualTo(removedFile.uuid());
    assertThat(fileReloaded.moduleUuid()).isEqualTo(moduleReloaded.uuid());
    assertThat(fileReloaded.moduleUuidPath()).isEqualTo(moduleReloaded.moduleUuidPath());
    assertThat(fileReloaded.projectUuid()).isEqualTo(moduleReloaded.projectUuid());
    assertThat(fileReloaded.parentProjectId()).isEqualTo(moduleReloaded.getId());
    assertThat(fileReloaded.name()).isEqualTo(removedFile.name());
    assertThat(fileReloaded.path()).isEqualTo(removedFile.path());
    assertThat(fileReloaded.isEnabled()).isTrue();
  }

}
