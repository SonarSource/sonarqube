/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.base.Optional;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.FileAttributes;
import org.sonar.server.computation.task.projectanalysis.component.MutableDbIdsRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.component.MutableDisabledComponentsHolder;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.server.computation.task.step.ComputationStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;
import static org.sonar.db.component.ComponentDto.UUID_PATH_SEPARATOR;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

public class ReportPersistComponentsStepTest extends BaseStepTest {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String MODULE_KEY = "MODULE_KEY";
  private static final String ORGANIZATION_UUID = "org1";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MutableDbIdsRepositoryRule dbIdsRepository = MutableDbIdsRepositoryRule.create(treeRootHolder);
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setOrganizationUuid(ORGANIZATION_UUID);

  private System2 system2 = mock(System2.class);
  private DbClient dbClient = dbTester.getDbClient();
  private Date now;
  private MutableDisabledComponentsHolder disabledComponentsHolder = mock(MutableDisabledComponentsHolder.class, RETURNS_DEEP_STUBS);
  private PersistComponentsStep underTest;

  @Before
  public void setup() throws Exception {
    now = DATE_FORMAT.parse("2015-06-02");
    when(system2.now()).thenReturn(now.getTime());

    dbTester.organizations().insertForUuid(ORGANIZATION_UUID);
    underTest = new PersistComponentsStep(dbClient, treeRootHolder, dbIdsRepository, system2, disabledComponentsHolder, analysisMetadataHolder);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void persist_components() {
    ComponentDto projectDto = insertProject();
    Component file = builder(FILE, 4).setUuid("DEFG").setKey("MODULE_KEY:src/main/java/dir/Foo.java")
      .setPath("src/main/java/dir/Foo.java")
      .setFileAttributes(new FileAttributes(false, "java", 1))
      .build();
    Component directory = builder(DIRECTORY, 3).setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir")
      .setPath("src/main/java/dir")
      .addChildren(file)
      .build();
    Component module = builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY)
      .setPath("module")
      .setName("Module")
      .setDescription("Module description")
      .addChildren(directory)
      .build();
    Component treeRoot = asTreeRoot(projectDto)
      .addChildren(module)
      .build();
    treeRootHolder.setRoot(treeRoot);

    underTest.execute();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(4);

    ComponentDto moduleDto = dbClient.componentDao().selectByKey(dbTester.getSession(), MODULE_KEY).get();
    assertThat(moduleDto.getOrganizationUuid()).isEqualTo(ORGANIZATION_UUID);
    assertThat(moduleDto.name()).isEqualTo("Module");
    assertThat(moduleDto.description()).isEqualTo("Module description");
    assertThat(moduleDto.path()).isEqualTo("module");
    assertThat(moduleDto.uuid()).isEqualTo("BCDE");
    assertThat(moduleDto.getUuidPath()).isEqualTo(projectDto.getUuidPath() + projectDto.uuid() + UUID_PATH_SEPARATOR);
    assertThat(moduleDto.moduleUuid()).isEqualTo(projectDto.uuid());
    assertThat(moduleDto.moduleUuidPath()).isEqualTo(projectDto.moduleUuidPath() + moduleDto.uuid() + ".");
    assertThat(moduleDto.projectUuid()).isEqualTo(projectDto.uuid());
    assertThat(moduleDto.qualifier()).isEqualTo("BRC");
    assertThat(moduleDto.scope()).isEqualTo("PRJ");
    assertThat(moduleDto.getRootUuid()).isEqualTo(projectDto.uuid());
    assertThat(moduleDto.getCreatedAt()).isEqualTo(now);

    ComponentDto directoryDto = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir").get();
    assertThat(directoryDto.getOrganizationUuid()).isEqualTo(ORGANIZATION_UUID);
    assertThat(directoryDto.name()).isEqualTo("src/main/java/dir");
    assertThat(directoryDto.description()).isNull();
    assertThat(directoryDto.path()).isEqualTo("src/main/java/dir");
    assertThat(directoryDto.uuid()).isEqualTo("CDEF");
    assertThat(directoryDto.getUuidPath()).isEqualTo(moduleDto.getUuidPath() + moduleDto.uuid() + UUID_PATH_SEPARATOR);
    assertThat(directoryDto.moduleUuid()).isEqualTo(moduleDto.uuid());
    assertThat(directoryDto.moduleUuidPath()).isEqualTo(moduleDto.moduleUuidPath());
    assertThat(directoryDto.projectUuid()).isEqualTo(projectDto.uuid());
    assertThat(directoryDto.qualifier()).isEqualTo("DIR");
    assertThat(directoryDto.scope()).isEqualTo("DIR");
    assertThat(directoryDto.getRootUuid()).isEqualTo(moduleDto.uuid());
    assertThat(directoryDto.getCreatedAt()).isEqualTo(now);

    ComponentDto fileDto = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileDto.getOrganizationUuid()).isEqualTo(ORGANIZATION_UUID);
    assertThat(fileDto.name()).isEqualTo("Foo.java");
    assertThat(fileDto.description()).isNull();
    assertThat(fileDto.path()).isEqualTo("src/main/java/dir/Foo.java");
    assertThat(fileDto.language()).isEqualTo("java");
    assertThat(fileDto.uuid()).isEqualTo("DEFG");
    assertThat(fileDto.getUuidPath()).isEqualTo(directoryDto.getUuidPath() + directoryDto.uuid() + UUID_PATH_SEPARATOR);
    assertThat(fileDto.moduleUuid()).isEqualTo(moduleDto.uuid());
    assertThat(fileDto.moduleUuidPath()).isEqualTo(moduleDto.moduleUuidPath());
    assertThat(fileDto.projectUuid()).isEqualTo(projectDto.uuid());
    assertThat(fileDto.qualifier()).isEqualTo("FIL");
    assertThat(fileDto.scope()).isEqualTo("FIL");
    assertThat(fileDto.getRootUuid()).isEqualTo(moduleDto.uuid());
    assertThat(fileDto.getCreatedAt()).isEqualTo(now);

    assertThat(dbIdsRepository.getComponentId(module)).isEqualTo(moduleDto.getId());
    assertThat(dbIdsRepository.getComponentId(directory)).isEqualTo(directoryDto.getId());
    assertThat(dbIdsRepository.getComponentId(file)).isEqualTo(fileDto.getId());
  }

  @Test
  public void persist_file_directly_attached_on_root_directory() {
    ComponentDto projectDto = insertProject();
    treeRootHolder.setRoot(
      asTreeRoot(projectDto)
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
    ComponentDto projectDto = insertProject();
    treeRootHolder.setRoot(
      asTreeRoot(projectDto)
        .addChildren(
          builder(DIRECTORY, 2).setUuid("CDEF").setKey(PROJECT_KEY + ":src/test/java/dir")
            .setPath("src/test/java/dir")
            .addChildren(
              builder(FILE, 3).setUuid("DEFG").setKey(PROJECT_KEY + ":src/test/java/dir/FooTest.java")
                .setPath("src/test/java/dir/FooTest.java")
                .setFileAttributes(new FileAttributes(true, null, 1))
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
    // Project and module already exists
    ComponentDto project = newPrivateProjectDto(dbTester.getDefaultOrganization(), "ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey(MODULE_KEY).setName("Module");
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY)
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
    assertThat(projectReloaded.getUuidPath()).isEqualTo(UUID_PATH_OF_ROOT);

    ComponentDto moduleReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), MODULE_KEY).get();
    assertThat(moduleReloaded.getId()).isEqualTo(module.getId());
    assertThat(moduleReloaded.uuid()).isEqualTo(module.uuid());
    assertThat(moduleReloaded.getUuidPath()).isEqualTo(module.getUuidPath());
    assertThat(moduleReloaded.moduleUuid()).isEqualTo(module.moduleUuid());
    assertThat(moduleReloaded.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(moduleReloaded.projectUuid()).isEqualTo(module.projectUuid());
    assertThat(moduleReloaded.getRootUuid()).isEqualTo(module.getRootUuid());

    ComponentDto directory = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir").get();
    assertThat(directory.getUuidPath()).isEqualTo(directory.getUuidPath());
    assertThat(directory.moduleUuid()).isEqualTo(module.uuid());
    assertThat(directory.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(directory.projectUuid()).isEqualTo(project.uuid());
    assertThat(directory.getRootUuid()).isEqualTo(module.uuid());

    ComponentDto file = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get();
    assertThat(file.getUuidPath()).isEqualTo(file.getUuidPath());
    assertThat(file.moduleUuid()).isEqualTo(module.uuid());
    assertThat(file.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(file.projectUuid()).isEqualTo(project.uuid());
    assertThat(file.getRootUuid()).isEqualTo(module.uuid());
  }

  @Test
  public void compute_root_uuid() {
    ComponentDto project = insertProject();
    treeRootHolder.setRoot(
      asTreeRoot(project)
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY)
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

    Optional<ComponentDto> module = dbClient.componentDao().selectByKey(dbTester.getSession(), MODULE_KEY);
    assertThat(module).isPresent();
    assertThat(module.get().getRootUuid()).isEqualTo(project.uuid());

    Optional<ComponentDto> subModule1 = dbClient.componentDao().selectByKey(dbTester.getSession(), "SUB_MODULE_1_KEY");
    assertThat(subModule1).isPresent();
    assertThat(subModule1.get().getRootUuid()).isEqualTo(project.uuid());

    Optional<ComponentDto> subModule2 = dbClient.componentDao().selectByKey(dbTester.getSession(), "SUB_MODULE_2_KEY");
    assertThat(subModule2).isPresent();
    assertThat(subModule2.get().getRootUuid()).isEqualTo(project.uuid());

    Optional<ComponentDto> directory = dbClient.componentDao().selectByKey(dbTester.getSession(), "SUB_MODULE_2_KEY:src/main/java/dir");
    assertThat(directory).isPresent();
    assertThat(directory.get().getRootUuid()).isEqualTo(subModule2.get().uuid());
  }

  private ReportComponent.Builder asTreeRoot(ComponentDto project) {
    return builder(PROJECT, 1).setUuid(project.uuid()).setKey(project.key()).setName(project.name());
  }

  public ComponentDto insertProject() {
    return dbTester.components().insertPrivateProject(dbTester.organizations().insert());
  }

  @Test
  public void persist_multi_modules() {
    ComponentDto project = insertProject();
    treeRootHolder.setRoot(
      asTreeRoot(project)
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

    ComponentDto moduleA = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_A").get();
    assertThat(moduleA.getUuidPath()).isEqualTo(project.getUuidPath() + project.uuid() + UUID_PATH_SEPARATOR);
    assertThat(moduleA.moduleUuid()).isEqualTo(project.uuid());
    assertThat(moduleA.moduleUuidPath()).isEqualTo(project.moduleUuidPath() + moduleA.uuid() + ".");
    assertThat(moduleA.getRootUuid()).isEqualTo(project.uuid());

    ComponentDto subModuleA = dbClient.componentDao().selectByKey(dbTester.getSession(), "SUB_MODULE_A").get();
    assertThat(subModuleA.getUuidPath()).isEqualTo(moduleA.getUuidPath() + moduleA.uuid() + UUID_PATH_SEPARATOR);
    assertThat(subModuleA.moduleUuid()).isEqualTo(moduleA.uuid());
    assertThat(subModuleA.moduleUuidPath()).isEqualTo(moduleA.moduleUuidPath() + subModuleA.uuid() + ".");
    assertThat(subModuleA.getRootUuid()).isEqualTo(project.uuid());

    ComponentDto moduleB = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_B").get();
    assertThat(moduleB.getUuidPath()).isEqualTo(project.getUuidPath() + project.uuid() + UUID_PATH_SEPARATOR);
    assertThat(moduleB.moduleUuid()).isEqualTo(project.uuid());
    assertThat(moduleB.moduleUuidPath()).isEqualTo(project.moduleUuidPath() + moduleB.uuid() + ".");
    assertThat(moduleB.getRootUuid()).isEqualTo(project.uuid());
  }

  @Test
  public void nothing_to_persist() {
    ComponentDto project = newPrivateProjectDto(dbTester.organizations().insert(), "ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey(MODULE_KEY).setName("Module");
    dbClient.componentDao().insert(dbTester.getSession(), module);
    ComponentDto directory = ComponentTesting.newDirectory(module, "src/main/java/dir").setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir");
    ComponentDto file = ComponentTesting.newFileDto(module, directory, "DEFG").setPath("src/main/java/dir/Foo.java").setName("Foo.java")
      .setKey("MODULE_KEY:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(dbTester.getSession(), directory, file);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY)
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
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), MODULE_KEY).get().getId()).isEqualTo(module.getId());
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir").get().getId()).isEqualTo(directory.getId());
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get().getId()).isEqualTo(file.getId());

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY).get();
    assertThat(projectReloaded.getId()).isEqualTo(project.getId());
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());
    assertThat(projectReloaded.moduleUuid()).isEqualTo(project.moduleUuid());
    assertThat(projectReloaded.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(projectReloaded.projectUuid()).isEqualTo(project.projectUuid());
    assertThat(projectReloaded.getRootUuid()).isEqualTo(project.getRootUuid());

    ComponentDto moduleReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), MODULE_KEY).get();
    assertThat(moduleReloaded.getId()).isEqualTo(module.getId());
    assertThat(moduleReloaded.uuid()).isEqualTo(module.uuid());
    assertThat(moduleReloaded.getUuidPath()).isEqualTo(module.getUuidPath());
    assertThat(moduleReloaded.moduleUuid()).isEqualTo(module.moduleUuid());
    assertThat(moduleReloaded.moduleUuidPath()).isEqualTo(module.moduleUuidPath());
    assertThat(moduleReloaded.projectUuid()).isEqualTo(module.projectUuid());
    assertThat(moduleReloaded.getRootUuid()).isEqualTo(module.getRootUuid());

    ComponentDto directoryReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir").get();
    assertThat(directoryReloaded.uuid()).isEqualTo(directory.uuid());
    assertThat(directoryReloaded.getUuidPath()).isEqualTo(directory.getUuidPath());
    assertThat(directoryReloaded.moduleUuid()).isEqualTo(directory.moduleUuid());
    assertThat(directoryReloaded.moduleUuidPath()).isEqualTo(directory.moduleUuidPath());
    assertThat(directoryReloaded.projectUuid()).isEqualTo(directory.projectUuid());
    assertThat(directoryReloaded.getRootUuid()).isEqualTo(directory.getRootUuid());
    assertThat(directoryReloaded.name()).isEqualTo(directory.name());
    assertThat(directoryReloaded.path()).isEqualTo(directory.path());

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileReloaded.uuid()).isEqualTo(file.uuid());
    assertThat(fileReloaded.getUuidPath()).isEqualTo(file.getUuidPath());
    assertThat(fileReloaded.moduleUuid()).isEqualTo(file.moduleUuid());
    assertThat(fileReloaded.moduleUuidPath()).isEqualTo(file.moduleUuidPath());
    assertThat(fileReloaded.projectUuid()).isEqualTo(file.projectUuid());
    assertThat(fileReloaded.getRootUuid()).isEqualTo(file.getRootUuid());
    assertThat(fileReloaded.name()).isEqualTo(file.name());
    assertThat(fileReloaded.path()).isEqualTo(file.path());
  }

  @Test
  public void update_module_name_and_description() {
    ComponentDto project = newPrivateProjectDto(dbTester.getDefaultOrganization(), "ABCD").setKey(PROJECT_KEY).setName("Project").setDescription("Project description");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey(MODULE_KEY).setName("Module");
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("New Project")
        .setDescription("New project description")
        .addChildren(
          builder(Component.Type.MODULE, 2)
            .setUuid("BCDE")
            .setKey(MODULE_KEY)
            .setName("New Module")
            .setDescription("New module description")
            .build())
        .build());

    underTest.execute();

    // functional transaction not finished, "A-fields" are not updated yet
    assertNameAndDescription(PROJECT_KEY, "Project", "Project description");
    assertNameAndDescription(MODULE_KEY, "Module", null);

    // commit functional transaction -> copies B-fields to A-fields
    dbClient.componentDao().applyBChangesForRootComponentUuid(dbTester.getSession(), "ABCD");
    assertNameAndDescription(PROJECT_KEY, "New Project", "New project description");
    assertNameAndDescription(MODULE_KEY, "New Module", "New module description");
  }

  private void assertNameAndDescription(String key, String expectedName, String expectedDescription) {
    ComponentDto dto = dbClient.componentDao().selectByKey(dbTester.getSession(), key).get();
    assertThat(dto.name()).isEqualTo(expectedName);
    assertThat(dto.description()).isEqualTo(expectedDescription);
  }

  @Test
  public void update_module_path() {
    ComponentDto project = newPrivateProjectDto(dbTester.organizations().insert(), "ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey(MODULE_KEY).setName("Module").setPath("path");
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY)
            .setName("Module")
            .setPath("New path")
            .build())
        .build());

    underTest.execute();

    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), MODULE_KEY).get().path()).isEqualTo("path");

    // commit the functional transaction
    dbClient.componentDao().applyBChangesForRootComponentUuid(dbTester.getSession(), project.uuid());
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), MODULE_KEY).get().path()).isEqualTo("New path");
  }

  @Test
  public void update_module_uuid_when_moving_a_module() {
    ComponentDto project = newPrivateProjectDto(dbTester.getDefaultOrganization(), "ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto moduleA = ComponentTesting.newModuleDto("EDCB", project)
      .setKey("MODULE_A")
      .setName("Module A");
    ComponentDto moduleB = ComponentTesting.newModuleDto("BCDE", project)
      .setKey("MODULE_B")
      .setName("Module B");
    dbClient.componentDao().insert(dbTester.getSession(), moduleA, moduleB);
    ComponentDto directory = ComponentTesting.newDirectory(moduleB, "src/main/java/dir").setUuid("CDEF").setKey("MODULE_B:src/main/java/dir");
    ComponentDto file = ComponentTesting.newFileDto(moduleB, directory, "DEFG").setPath("src/main/java/dir/Foo.java").setName("Foo.java")
      .setKey("MODULE_B:src/main/java/dir/Foo.java");
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

    // commit the functional transaction
    dbClient.componentDao().applyBChangesForRootComponentUuid(dbTester.getSession(), project.uuid());
    dbTester.commit();

    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(5);

    ComponentDto moduleAreloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_A").get();

    ComponentDto moduleBReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_B").get();
    assertThat(moduleBReloaded).isNotNull();
    assertThat(moduleBReloaded.uuid()).isEqualTo(moduleB.uuid());
    assertThat(moduleBReloaded.getUuidPath()).isEqualTo(moduleBReloaded.getUuidPath());
    assertThat(moduleBReloaded.moduleUuid()).isEqualTo(moduleAreloaded.uuid());
    assertThat(moduleBReloaded.moduleUuidPath()).isEqualTo(moduleAreloaded.moduleUuidPath() + moduleBReloaded.uuid() + ".");
    assertThat(moduleBReloaded.projectUuid()).isEqualTo(project.uuid());
    assertThat(moduleBReloaded.getRootUuid()).isEqualTo(project.uuid());

    ComponentDto directoryReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_B:src/main/java/dir").get();
    assertThat(directoryReloaded).isNotNull();
    assertThat(directoryReloaded.uuid()).isEqualTo(directory.uuid());
    assertThat(directoryReloaded.getUuidPath()).isEqualTo(directoryReloaded.getUuidPath());
    assertThat(directoryReloaded.moduleUuid()).isEqualTo(moduleBReloaded.uuid());
    assertThat(directoryReloaded.moduleUuidPath()).isEqualTo(moduleBReloaded.moduleUuidPath());
    assertThat(directoryReloaded.projectUuid()).isEqualTo(project.uuid());
    assertThat(directoryReloaded.getRootUuid()).isEqualTo(moduleBReloaded.uuid());

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_B:src/main/java/dir/Foo.java").get();
    assertThat(fileReloaded).isNotNull();
    assertThat(fileReloaded.uuid()).isEqualTo(file.uuid());
    assertThat(fileReloaded.getUuidPath()).isEqualTo(fileReloaded.getUuidPath());
    assertThat(fileReloaded.moduleUuid()).isEqualTo(moduleBReloaded.uuid());
    assertThat(fileReloaded.moduleUuidPath()).isEqualTo(moduleBReloaded.moduleUuidPath());
    assertThat(fileReloaded.projectUuid()).isEqualTo(project.uuid());
    assertThat(fileReloaded.getRootUuid()).isEqualTo(moduleBReloaded.uuid());
  }

  @Test
  public void do_not_update_created_at_on_existing_component() {
    Date oldDate = DateUtils.parseDate("2015-01-01");
    ComponentDto project = newPrivateProjectDto(dbTester.getDefaultOrganization(), "ABCD").setKey(PROJECT_KEY).setName("Project").setCreatedAt(oldDate);
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey(MODULE_KEY).setName("Module").setPath("path").setCreatedAt(oldDate);
    dbClient.componentDao().insert(dbTester.getSession(), module);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .build());

    underTest.execute();

    Optional<ComponentDto> projectReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY);
    assertThat(projectReloaded.get().getCreatedAt()).isNotEqualTo(now);
  }

  @Test
  public void persist_components_that_were_previously_removed() {
    ComponentDto project = newPrivateProjectDto(dbTester.organizations().insert(), "ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto removedModule = ComponentTesting.newModuleDto("BCDE", project)
      .setKey(MODULE_KEY)
      .setName("Module")
      .setEnabled(false);
    dbClient.componentDao().insert(dbTester.getSession(), removedModule);
    ComponentDto removedDirectory = ComponentTesting.newDirectory(removedModule, "src/main/java/dir")
      .setUuid("CDEF")
      .setKey("MODULE_KEY:src/main/java/dir")
      .setEnabled(false);
    ComponentDto removedFile = ComponentTesting.newFileDto(removedModule, removedDirectory, "DEFG")
      .setPath("src/main/java/dir/Foo.java")
      .setName("Foo.java")
      .setKey("MODULE_KEY:src/main/java/dir/Foo.java")
      .setEnabled(false);
    dbClient.componentDao().insert(dbTester.getSession(), removedDirectory, removedFile);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY)
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
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), MODULE_KEY).get().getId()).isEqualTo(removedModule.getId());
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir").get().getId()).isEqualTo(removedDirectory.getId());
    assertThat(dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get().getId()).isEqualTo(removedFile.getId());
    assertExistButDisabled(removedModule.key(), removedDirectory.getKey(), removedFile.getKey());

    // commit the functional transaction
    dbClient.componentDao().applyBChangesForRootComponentUuid(dbTester.getSession(), project.uuid());

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), PROJECT_KEY).get();
    assertThat(projectReloaded.getId()).isEqualTo(project.getId());
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());
    assertThat(projectReloaded.getUuidPath()).isEqualTo(project.getUuidPath());
    assertThat(projectReloaded.moduleUuid()).isEqualTo(project.moduleUuid());
    assertThat(projectReloaded.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(projectReloaded.projectUuid()).isEqualTo(project.projectUuid());
    assertThat(projectReloaded.getRootUuid()).isEqualTo(project.getRootUuid());
    assertThat(projectReloaded.isEnabled()).isTrue();

    ComponentDto moduleReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), MODULE_KEY).get();
    assertThat(moduleReloaded.getId()).isEqualTo(removedModule.getId());
    assertThat(moduleReloaded.uuid()).isEqualTo(removedModule.uuid());
    assertThat(moduleReloaded.getUuidPath()).isEqualTo(removedModule.getUuidPath());
    assertThat(moduleReloaded.moduleUuid()).isEqualTo(removedModule.moduleUuid());
    assertThat(moduleReloaded.moduleUuidPath()).isEqualTo(removedModule.moduleUuidPath());
    assertThat(moduleReloaded.projectUuid()).isEqualTo(removedModule.projectUuid());
    assertThat(moduleReloaded.getRootUuid()).isEqualTo(removedModule.getRootUuid());
    assertThat(moduleReloaded.isEnabled()).isTrue();

    ComponentDto directoryReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir").get();
    assertThat(directoryReloaded.getId()).isEqualTo(removedDirectory.getId());
    assertThat(directoryReloaded.uuid()).isEqualTo(removedDirectory.uuid());
    assertThat(directoryReloaded.getUuidPath()).isEqualTo(removedDirectory.getUuidPath());
    assertThat(directoryReloaded.moduleUuid()).isEqualTo(removedDirectory.moduleUuid());
    assertThat(directoryReloaded.moduleUuidPath()).isEqualTo(removedDirectory.moduleUuidPath());
    assertThat(directoryReloaded.projectUuid()).isEqualTo(removedDirectory.projectUuid());
    assertThat(directoryReloaded.getRootUuid()).isEqualTo(removedDirectory.getRootUuid());
    assertThat(directoryReloaded.name()).isEqualTo(removedDirectory.name());
    assertThat(directoryReloaded.path()).isEqualTo(removedDirectory.path());
    assertThat(directoryReloaded.isEnabled()).isTrue();

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileReloaded.getId()).isEqualTo(fileReloaded.getId());
    assertThat(fileReloaded.uuid()).isEqualTo(removedFile.uuid());
    assertThat(fileReloaded.getUuidPath()).isEqualTo(removedFile.getUuidPath());
    assertThat(fileReloaded.moduleUuid()).isEqualTo(removedFile.moduleUuid());
    assertThat(fileReloaded.moduleUuidPath()).isEqualTo(removedFile.moduleUuidPath());
    assertThat(fileReloaded.projectUuid()).isEqualTo(removedFile.projectUuid());
    assertThat(fileReloaded.getRootUuid()).isEqualTo(removedFile.getRootUuid());
    assertThat(fileReloaded.name()).isEqualTo(removedFile.name());
    assertThat(fileReloaded.path()).isEqualTo(removedFile.path());
    assertThat(fileReloaded.isEnabled()).isTrue();
  }

  private void assertExistButDisabled(String... keys) {
    for (String key : keys) {
      ComponentDto dto = dbClient.componentDao().selectByKey(dbTester.getSession(), key).get();
      assertThat(dto.isEnabled()).isFalse();
    }
  }

  @Test
  public void update_module_uuid_when_reactivating_removed_component() {
    ComponentDto project = newPrivateProjectDto(dbTester.getDefaultOrganization(), "ABCD").setKey(PROJECT_KEY).setName("Project");
    dbClient.componentDao().insert(dbTester.getSession(), project);
    ComponentDto module = ComponentTesting.newModuleDto("BCDE", project).setKey(MODULE_KEY).setName("Module");
    ComponentDto removedModule = ComponentTesting.newModuleDto("EDCD", project).setKey("REMOVED_MODULE_KEY").setName("Removed Module").setEnabled(false);
    dbClient.componentDao().insert(dbTester.getSession(), module, removedModule);
    ComponentDto directory = ComponentTesting.newDirectory(module, "src/main/java/dir").setUuid("CDEF").setKey("MODULE_KEY:src/main/java/dir");
    // The file was attached to another module
    ComponentDto removedFile = ComponentTesting.newFileDto(removedModule, directory, "DEFG").setPath("src/main/java/dir/Foo.java").setName("Foo.java")
      .setKey("MODULE_KEY:src/main/java/dir/Foo.java").setEnabled(false);
    dbClient.componentDao().insert(dbTester.getSession(), directory, removedFile);
    dbTester.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid("ABCD").setKey(PROJECT_KEY)
        .setName("Project")
        .addChildren(
          builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey(MODULE_KEY)
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

    // commit the functional transaction
    dbClient.componentDao().applyBChangesForRootComponentUuid(dbTester.getSession(), project.uuid());
    dbTester.commit();

    // Projects contains 4 components from the report + one removed module
    assertThat(dbTester.countRowsOfTable("projects")).isEqualTo(5);

    ComponentDto moduleReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), MODULE_KEY).get();

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(dbTester.getSession(), "MODULE_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileReloaded.getId()).isEqualTo(removedFile.getId());
    assertThat(fileReloaded.uuid()).isEqualTo(removedFile.uuid());
    assertThat(fileReloaded.getUuidPath()).isEqualTo(fileReloaded.getUuidPath());
    assertThat(fileReloaded.moduleUuid()).isEqualTo(moduleReloaded.uuid());
    assertThat(fileReloaded.moduleUuidPath()).isEqualTo(moduleReloaded.moduleUuidPath());
    assertThat(fileReloaded.projectUuid()).isEqualTo(moduleReloaded.projectUuid());
    assertThat(fileReloaded.name()).isEqualTo(removedFile.name());
    assertThat(fileReloaded.path()).isEqualTo(removedFile.path());
    assertThat(fileReloaded.isEnabled()).isTrue();
  }

  @Test
  public void persists_existing_components_with_visibility_of_root_in_db_out_of_functional_transaction() {
    boolean isRootPrivate = new Random().nextBoolean();
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = newPrivateProjectDto(organization, "ABCD").setKey(PROJECT_KEY).setName("Project").setPrivate(isRootPrivate);
    dbTester.components().insertComponent(project);
    ComponentDto module = newModuleDto(project).setUuid("BCDE").setKey("MODULE").setPrivate(!isRootPrivate);
    dbTester.components().insertComponent(module);
    dbTester.components().insertComponent(newDirectory(module, "DEFG", "Directory").setKey("DIR").setPrivate(isRootPrivate));
    treeRootHolder.setRoot(createSampleProjectComponentTree(project));

    underTest.execute();

    Stream.of("ABCD", "BCDE", "BCDE", "BCDE")
      .forEach(uuid -> assertThat(dbClient.componentDao().selectByUuid(dbTester.getSession(), uuid).get().isPrivate())
        .describedAs("for uuid " + uuid)
        .isEqualTo(isRootPrivate));
  }

  private ReportComponent createSampleProjectComponentTree(ComponentDto project) {
    return createSampleProjectComponentTree(project.uuid(), project.key());
  }

  private ReportComponent createSampleProjectComponentTree(String projectUuid, String projectKey) {
    return builder(PROJECT, 1).setUuid(projectUuid).setKey(projectKey)
      .setName("Project")
      .addChildren(
        builder(Component.Type.MODULE, 2).setUuid("BCDE").setKey("MODULE")
          .setName("Module")
          .addChildren(
            builder(Component.Type.DIRECTORY, 3).setUuid("DEFG").setKey("DIR")
              .setPath("Directory")
              .addChildren(
                builder(FILE, 4).setUuid("CDEF").setKey("FILE")
                  .setPath("file")
                  .build())
              .build())
          .build())
      .build();
  }
}
