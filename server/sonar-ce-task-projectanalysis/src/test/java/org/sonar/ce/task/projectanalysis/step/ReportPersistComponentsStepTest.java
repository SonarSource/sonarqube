/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.step;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.BranchPersister;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.MutableDbIdsRepositoryRule;
import org.sonar.ce.task.projectanalysis.component.MutableDisabledComponentsHolder;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.component.ComponentKeys;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.project.Project;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;
import static org.sonar.db.component.ComponentDto.UUID_PATH_SEPARATOR;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newModuleDto;

public class ReportPersistComponentsStepTest extends BaseStepTest {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private static final String PROJECT_KEY = "PROJECT_KEY";
  private static final String ORGANIZATION_UUID = "org1";
  private static final String QUALITY_GATE_UUID = "gg1";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public MutableDbIdsRepositoryRule dbIdsRepository = MutableDbIdsRepositoryRule.create(treeRootHolder);
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule()
    .setOrganizationUuid(ORGANIZATION_UUID, QUALITY_GATE_UUID);

  private System2 system2 = mock(System2.class);
  private DbClient dbClient = db.getDbClient();
  private Date now;
  private MutableDisabledComponentsHolder disabledComponentsHolder = mock(MutableDisabledComponentsHolder.class, RETURNS_DEEP_STUBS);
  private PersistComponentsStep underTest;
  private BranchPersister branchPersister;

  @Before
  public void setup() throws Exception {
    now = DATE_FORMAT.parse("2015-06-02");
    when(system2.now()).thenReturn(now.getTime());

    db.organizations().insertForUuid(ORGANIZATION_UUID);
    branchPersister = mock(BranchPersister.class);
    underTest = new PersistComponentsStep(dbClient, treeRootHolder, dbIdsRepository, system2, disabledComponentsHolder, analysisMetadataHolder, branchPersister);
  }

  @Override
  protected ComputationStep step() {
    return underTest;
  }

  @Test
  public void persist_components() {
    ComponentDto projectDto = prepareProject();
    Component file = builder(FILE, 4).setUuid("DEFG").setKey("PROJECT_KEY:src/main/java/dir/Foo.java")
      .setName("src/main/java/dir/Foo.java")
      .setShortName("Foo.java")
      .setFileAttributes(new FileAttributes(false, "java", 1))
      .build();
    Component directory = builder(DIRECTORY, 3).setUuid("CDEF").setKey("PROJECT_KEY:src/main/java/dir")
      .setName("src/main/java/dir")
      .setShortName("dir")
      .addChildren(file)
      .build();
    Component treeRoot = asTreeRoot(projectDto)
      .addChildren(directory)
      .build();
    treeRootHolder.setRoot(treeRoot);

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("projects")).isEqualTo(3);

    ComponentDto directoryDto = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get();
    assertThat(directoryDto.getOrganizationUuid()).isEqualTo(ORGANIZATION_UUID);
    assertThat(directoryDto.name()).isEqualTo("dir");
    assertThat(directoryDto.longName()).isEqualTo("src/main/java/dir");
    assertThat(directoryDto.description()).isNull();
    assertThat(directoryDto.path()).isEqualTo("src/main/java/dir");
    assertThat(directoryDto.uuid()).isEqualTo("CDEF");
    assertThat(directoryDto.getUuidPath()).isEqualTo(UUID_PATH_SEPARATOR + projectDto.uuid() + UUID_PATH_SEPARATOR);
    assertThat(directoryDto.moduleUuid()).isEqualTo(projectDto.uuid());
    assertThat(directoryDto.moduleUuidPath()).isEqualTo(projectDto.moduleUuidPath());
    assertThat(directoryDto.getMainBranchProjectUuid()).isNull();
    assertThat(directoryDto.projectUuid()).isEqualTo(projectDto.uuid());
    assertThat(directoryDto.qualifier()).isEqualTo("DIR");
    assertThat(directoryDto.scope()).isEqualTo("DIR");
    assertThat(directoryDto.getRootUuid()).isEqualTo(projectDto.uuid());
    assertThat(directoryDto.getCreatedAt()).isEqualTo(now);

    ComponentDto fileDto = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileDto.getOrganizationUuid()).isEqualTo(ORGANIZATION_UUID);
    assertThat(fileDto.name()).isEqualTo("Foo.java");
    assertThat(fileDto.longName()).isEqualTo("src/main/java/dir/Foo.java");
    assertThat(fileDto.description()).isNull();
    assertThat(fileDto.path()).isEqualTo("src/main/java/dir/Foo.java");
    assertThat(fileDto.language()).isEqualTo("java");
    assertThat(fileDto.uuid()).isEqualTo("DEFG");
    assertThat(fileDto.getUuidPath()).isEqualTo(directoryDto.getUuidPath() + directoryDto.uuid() + UUID_PATH_SEPARATOR);
    assertThat(fileDto.moduleUuid()).isEqualTo(projectDto.uuid());
    assertThat(fileDto.moduleUuidPath()).isEqualTo(projectDto.moduleUuidPath());
    assertThat(fileDto.getMainBranchProjectUuid()).isNull();
    assertThat(fileDto.projectUuid()).isEqualTo(projectDto.uuid());
    assertThat(fileDto.qualifier()).isEqualTo("FIL");
    assertThat(fileDto.scope()).isEqualTo("FIL");
    assertThat(fileDto.getRootUuid()).isEqualTo(projectDto.uuid());
    assertThat(fileDto.getCreatedAt()).isEqualTo(now);

    assertThat(dbIdsRepository.getComponentId(directory)).isEqualTo(directoryDto.getId());
    assertThat(dbIdsRepository.getComponentId(file)).isEqualTo(fileDto.getId());
  }

  @Test
  public void persist_components_of_existing_branch() {
    ComponentDto project = prepareBranch("feature/foo");
    Component file = builder(FILE, 4).setUuid("DEFG").setKey("PROJECT_KEY:src/main/java/dir/Foo.java")
      .setName("src/main/java/dir/Foo.java")
      .setShortName("Foo.java")
      .setFileAttributes(new FileAttributes(false, "java", 1))
      .build();
    Component directory = builder(DIRECTORY, 3).setUuid("CDEF")
      .setKey("PROJECT_KEY:src/main/java/dir")
      .setName("src/main/java/dir")
      .setShortName("dir")
      .addChildren(file)
      .build();
    Component treeRoot = asTreeRoot(project)
      .addChildren(directory)
      .build();
    treeRootHolder.setRoot(treeRoot);

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("projects")).isEqualTo(3);

    ComponentDto directoryDto = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get();
    assertThat(directoryDto.getOrganizationUuid()).isEqualTo(ORGANIZATION_UUID);
    assertThat(directoryDto.name()).isEqualTo("dir");
    assertThat(directoryDto.longName()).isEqualTo("src/main/java/dir");
    assertThat(directoryDto.description()).isNull();
    assertThat(directoryDto.path()).isEqualTo("src/main/java/dir");
    assertThat(directoryDto.uuid()).isEqualTo("CDEF");
    assertThat(directoryDto.getUuidPath()).isEqualTo(UUID_PATH_SEPARATOR + project.uuid() + UUID_PATH_SEPARATOR);
    assertThat(directoryDto.moduleUuid()).isEqualTo(project.uuid());
    assertThat(directoryDto.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(directoryDto.getMainBranchProjectUuid()).isEqualTo(project.uuid());
    assertThat(directoryDto.projectUuid()).isEqualTo(project.uuid());
    assertThat(directoryDto.qualifier()).isEqualTo("DIR");
    assertThat(directoryDto.scope()).isEqualTo("DIR");
    assertThat(directoryDto.getRootUuid()).isEqualTo(project.uuid());
    assertThat(directoryDto.getCreatedAt()).isEqualTo(now);

    ComponentDto fileDto = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileDto.getOrganizationUuid()).isEqualTo(ORGANIZATION_UUID);
    assertThat(fileDto.name()).isEqualTo("Foo.java");
    assertThat(fileDto.longName()).isEqualTo("src/main/java/dir/Foo.java");
    assertThat(fileDto.description()).isNull();
    assertThat(fileDto.path()).isEqualTo("src/main/java/dir/Foo.java");
    assertThat(fileDto.language()).isEqualTo("java");
    assertThat(fileDto.uuid()).isEqualTo("DEFG");
    assertThat(fileDto.getUuidPath()).isEqualTo(directoryDto.getUuidPath() + directoryDto.uuid() + UUID_PATH_SEPARATOR);
    assertThat(fileDto.moduleUuid()).isEqualTo(project.uuid());
    assertThat(fileDto.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(fileDto.getMainBranchProjectUuid()).isEqualTo(project.uuid());
    assertThat(fileDto.projectUuid()).isEqualTo(project.uuid());
    assertThat(fileDto.qualifier()).isEqualTo("FIL");
    assertThat(fileDto.scope()).isEqualTo("FIL");
    assertThat(fileDto.getRootUuid()).isEqualTo(project.uuid());
    assertThat(fileDto.getCreatedAt()).isEqualTo(now);

    assertThat(dbIdsRepository.getComponentId(directory)).isEqualTo(directoryDto.getId());
    assertThat(dbIdsRepository.getComponentId(file)).isEqualTo(fileDto.getId());
  }

  @Test
  public void persist_file_directly_attached_on_root_directory() {
    ComponentDto projectDto = prepareProject();
    treeRootHolder.setRoot(
      asTreeRoot(projectDto)
        .addChildren(
          builder(FILE, 2).setUuid("DEFG").setKey(projectDto.getDbKey() + ":pom.xml")
            .setName("pom.xml")
            .build())
        .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(dbClient.componentDao().selectByKey(db.getSession(), projectDto.getDbKey() + ":/")).isNotPresent();

    ComponentDto file = dbClient.componentDao().selectByKey(db.getSession(), projectDto.getDbKey() + ":pom.xml").get();
    assertThat(file.name()).isEqualTo("pom.xml");
    assertThat(file.path()).isEqualTo("pom.xml");
  }

  @Test
  public void persist_unit_test() {
    ComponentDto projectDto = prepareProject();
    treeRootHolder.setRoot(
      asTreeRoot(projectDto)
        .addChildren(
          builder(DIRECTORY, 2).setUuid("CDEF").setKey(PROJECT_KEY + ":src/test/java/dir")
            .setName("src/test/java/dir")
            .addChildren(
              builder(FILE, 3).setUuid("DEFG").setKey(PROJECT_KEY + ":src/test/java/dir/FooTest.java")
                .setName("src/test/java/dir/FooTest.java")
                .setShortName("FooTest.java")
                .setFileAttributes(new FileAttributes(true, null, 1))
                .build())
            .build())
        .build());

    underTest.execute(new TestComputationStepContext());

    ComponentDto file = dbClient.componentDao().selectByKey(db.getSession(), PROJECT_KEY + ":src/test/java/dir/FooTest.java").get();
    assertThat(file.name()).isEqualTo("FooTest.java");
    assertThat(file.longName()).isEqualTo("src/test/java/dir/FooTest.java");
    assertThat(file.path()).isEqualTo("src/test/java/dir/FooTest.java");
    assertThat(file.qualifier()).isEqualTo("UTS");
    assertThat(file.scope()).isEqualTo("FIL");
  }

  @Test
  public void update_file_to_directory_change_scope() {
    ComponentDto project = prepareProject();
    ComponentDto directory = ComponentTesting.newDirectory(project, "src").setUuid("CDEF").setDbKey("PROJECT_KEY:src");
    ComponentDto file = ComponentTesting.newFileDto(project, directory, "DEFG").setPath("src/foo").setName("foo")
      .setDbKey("PROJECT_KEY:src/foo");
    dbClient.componentDao().insert(db.getSession(), directory, file);
    db.getSession().commit();

    assertThat(dbClient.componentDao().selectByKey(db.getSession(), PROJECT_KEY + ":src/foo").get().scope()).isEqualTo("FIL");

    treeRootHolder.setRoot(
      asTreeRoot(project)
        .addChildren(
          builder(DIRECTORY, 2).setUuid("CDEF").setKey(PROJECT_KEY + ":src")
            .setName("src")
            .addChildren(
              builder(DIRECTORY, 3).setUuid("DEFG").setKey(PROJECT_KEY + ":src/foo")
                .setName("foo")
                .addChildren(
                  builder(FILE, 4).setUuid("HIJK").setKey(PROJECT_KEY + ":src/foo/FooTest.java")
                    .setName("src/foo/FooTest.java")
                    .setShortName("FooTest.java")
                    .setFileAttributes(new FileAttributes(false, null, 1))
                    .build())
                .build())
            .build())
        .build());

    underTest.execute(new TestComputationStepContext());

    // commit the functional transaction
    dbClient.componentDao().applyBChangesForRootComponentUuid(db.getSession(), project.uuid());
    db.commit();

    assertThat(dbClient.componentDao().selectByKey(db.getSession(), PROJECT_KEY + ":src/foo").get().scope()).isEqualTo("DIR");
  }

  @Test
  public void update_module_to_directory_change_scope() {
    ComponentDto project = prepareProject();
    ComponentDto module = ComponentTesting.newModuleDto(project).setUuid("CDEF").setDbKey("MODULE_KEY").setPath("module");
    dbClient.componentDao().insert(db.getSession(), module);
    db.getSession().commit();

    assertThat(dbClient.componentDao().selectByUuid(db.getSession(), "CDEF").get().scope()).isEqualTo("PRJ");

    treeRootHolder.setRoot(
      asTreeRoot(project)
        .addChildren(
          builder(DIRECTORY, 2).setUuid("CDEF").setKey(PROJECT_KEY + ":module")
            .setName("module")
            .addChildren(
              builder(FILE, 3).setUuid("HIJK").setKey(PROJECT_KEY + ":module/FooTest.java")
                .setName("module/FooTest.java")
                .setShortName("FooTest.java")
                .setFileAttributes(new FileAttributes(false, null, 1))
                .build())
            .build())
        .build());

    underTest.execute(new TestComputationStepContext());

    // commit the functional transaction
    dbClient.componentDao().applyBChangesForRootComponentUuid(db.getSession(), project.uuid());
    db.commit();

    assertThat(dbClient.componentDao().selectByUuid(db.getSession(), "CDEF").get().scope()).isEqualTo("DIR");
  }

  @Test
  public void persist_only_new_components() {
    // Project and module already exists
    ComponentDto project = prepareProject();
    db.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid(project.uuid()).setKey(project.getDbKey())
        .setName("Project")
        .addChildren(
          builder(DIRECTORY, 3).setUuid("CDEF").setKey("PROJECT_KEY:src/main/java/dir")
            .setName("src/main/java/dir")
            .addChildren(
              builder(FILE, 4).setUuid("DEFG").setKey("PROJECT_KEY:src/main/java/dir/Foo.java")
                .setName("src/main/java/dir/Foo.java")
                .setShortName("Foo.java")
                .build())
            .build())
        .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("projects")).isEqualTo(3);

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(db.getSession(), project.getDbKey()).get();
    assertThat(projectReloaded.getId()).isEqualTo(project.getId());
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());
    assertThat(projectReloaded.getUuidPath()).isEqualTo(UUID_PATH_OF_ROOT);
    assertThat(projectReloaded.getMainBranchProjectUuid()).isNull();

    ComponentDto directory = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get();
    assertThat(directory.getUuidPath()).isEqualTo(directory.getUuidPath());
    assertThat(directory.moduleUuid()).isEqualTo(project.uuid());
    assertThat(directory.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(directory.projectUuid()).isEqualTo(project.uuid());
    assertThat(directory.getRootUuid()).isEqualTo(project.uuid());
    assertThat(directory.getMainBranchProjectUuid()).isNull();

    ComponentDto file = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get();
    assertThat(file.getUuidPath()).isEqualTo(file.getUuidPath());
    assertThat(file.moduleUuid()).isEqualTo(project.uuid());
    assertThat(file.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(file.projectUuid()).isEqualTo(project.uuid());
    assertThat(file.getRootUuid()).isEqualTo(project.uuid());
    assertThat(file.getMainBranchProjectUuid()).isNull();
  }

  @Test
  public void nothing_to_persist() {
    ComponentDto project = prepareProject();
    ComponentDto directory = ComponentTesting.newDirectory(project, "src/main/java/dir").setUuid("CDEF").setDbKey("PROJECT_KEY:src/main/java/dir");
    ComponentDto file = ComponentTesting.newFileDto(project, directory, "DEFG").setPath("src/main/java/dir/Foo.java").setName("Foo.java")
      .setDbKey("PROJECT_KEY:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(db.getSession(), directory, file);
    db.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid(project.uuid()).setKey(project.getDbKey())
        .setName("Project")
        .addChildren(
          builder(DIRECTORY, 3).setUuid("CDEF").setKey("PROJECT_KEY:src/main/java/dir")
            .setName("src/main/java/dir")
            .addChildren(
              builder(FILE, 4).setUuid("DEFG").setKey("PROJECT_KEY:src/main/java/dir/Foo.java")
                .setName("src/main/java/dir/Foo.java")
                .build())
            .build())
        .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("projects")).isEqualTo(3);
    assertThat(dbClient.componentDao().selectByKey(db.getSession(), project.getDbKey()).get().getId()).isEqualTo(project.getId());
    assertThat(dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get().getId()).isEqualTo(directory.getId());
    assertThat(dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get().getId()).isEqualTo(file.getId());

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(db.getSession(), project.getDbKey()).get();
    assertThat(projectReloaded.getId()).isEqualTo(project.getId());
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());
    assertThat(projectReloaded.moduleUuid()).isEqualTo(project.moduleUuid());
    assertThat(projectReloaded.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(projectReloaded.projectUuid()).isEqualTo(project.projectUuid());
    assertThat(projectReloaded.getRootUuid()).isEqualTo(project.getRootUuid());

    ComponentDto directoryReloaded = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get();
    assertThat(directoryReloaded.uuid()).isEqualTo(directory.uuid());
    assertThat(directoryReloaded.getUuidPath()).isEqualTo(directory.getUuidPath());
    assertThat(directoryReloaded.moduleUuid()).isEqualTo(directory.moduleUuid());
    assertThat(directoryReloaded.moduleUuidPath()).isEqualTo(directory.moduleUuidPath());
    assertThat(directoryReloaded.projectUuid()).isEqualTo(directory.projectUuid());
    assertThat(directoryReloaded.getRootUuid()).isEqualTo(directory.getRootUuid());
    assertThat(directoryReloaded.name()).isEqualTo(directory.name());
    assertThat(directoryReloaded.path()).isEqualTo(directory.path());

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get();
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
  public void update_module_uuid_when_moving_a_module() {
    ComponentDto project = prepareProject();
    ComponentDto directory = ComponentTesting.newDirectory(project, "src/main/java/dir").setUuid("CDEF").setDbKey("PROJECT_KEY:src/main/java/dir");
    ComponentDto file = ComponentTesting.newFileDto(project, directory, "DEFG").setPath("src/main/java/dir/Foo.java").setName("Foo.java")
      .setDbKey("PROJECT_KEY:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(db.getSession(), directory, file);
    db.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid(project.uuid()).setKey(project.getDbKey())
        .setName("Project")
        .addChildren(
          builder(DIRECTORY, 4).setUuid("CDEF").setKey("PROJECT_KEY:src/main/java/dir")
            .setName("src/main/java/dir")
            .addChildren(
              builder(FILE, 5).setUuid("DEFG").setKey("PROJECT_KEY:src/main/java/dir/Foo.java")
                .setName("src/main/java/dir/Foo.java")
                .build())
            .build())
        .build());

    underTest.execute(new TestComputationStepContext());

    // commit the functional transaction
    dbClient.componentDao().applyBChangesForRootComponentUuid(db.getSession(), project.uuid());
    db.commit();

    assertThat(db.countRowsOfTable("projects")).isEqualTo(3);

    ComponentDto directoryReloaded = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get();
    assertThat(directoryReloaded).isNotNull();
    assertThat(directoryReloaded.uuid()).isEqualTo(directory.uuid());
    assertThat(directoryReloaded.getUuidPath()).isEqualTo(directoryReloaded.getUuidPath());
    assertThat(directoryReloaded.moduleUuid()).isEqualTo(project.uuid());
    assertThat(directoryReloaded.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(directoryReloaded.projectUuid()).isEqualTo(project.uuid());
    assertThat(directoryReloaded.getRootUuid()).isEqualTo(project.uuid());

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileReloaded).isNotNull();
    assertThat(fileReloaded.uuid()).isEqualTo(file.uuid());
    assertThat(fileReloaded.getUuidPath()).isEqualTo(fileReloaded.getUuidPath());
    assertThat(fileReloaded.moduleUuid()).isEqualTo(project.uuid());
    assertThat(fileReloaded.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(fileReloaded.projectUuid()).isEqualTo(project.uuid());
    assertThat(fileReloaded.getRootUuid()).isEqualTo(project.uuid());
  }

  @Test
  public void do_not_update_created_at_on_existing_component() {
    Date oldDate = DateUtils.parseDate("2015-01-01");
    ComponentDto project = prepareProject(p -> p.setCreatedAt(oldDate));
    db.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid(project.uuid()).setKey(project.getDbKey())
        .build());

    underTest.execute(new TestComputationStepContext());

    Optional<ComponentDto> projectReloaded = dbClient.componentDao().selectByUuid(db.getSession(), project.uuid());
    assertThat(projectReloaded.get().getCreatedAt()).isNotEqualTo(now);
  }

  @Test
  public void persist_components_that_were_previously_removed() {
    ComponentDto project = prepareProject();
    ComponentDto removedDirectory = ComponentTesting.newDirectory(project, "src/main/java/dir")
      .setLongName("src/main/java/dir")
      .setName("dir")
      .setUuid("CDEF")
      .setDbKey("PROJECT_KEY:src/main/java/dir")
      .setEnabled(false);
    ComponentDto removedFile = ComponentTesting.newFileDto(project, removedDirectory, "DEFG")
      .setPath("src/main/java/dir/Foo.java")
      .setLongName("src/main/java/dir/Foo.java")
      .setName("Foo.java")
      .setDbKey("PROJECT_KEY:src/main/java/dir/Foo.java")
      .setEnabled(false);
    dbClient.componentDao().insert(db.getSession(), removedDirectory, removedFile);
    db.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid(project.uuid()).setKey(project.getDbKey())
        .setName("Project")
        .addChildren(
          builder(DIRECTORY, 3).setUuid("CDEF").setKey("PROJECT_KEY:src/main/java/dir")
            .setName("src/main/java/dir")
            .setShortName("dir")
            .addChildren(
              builder(FILE, 4).setUuid("DEFG").setKey("PROJECT_KEY:src/main/java/dir/Foo.java")
                .setName("src/main/java/dir/Foo.java")
                .setShortName("Foo.java")
                .build())
            .build())
        .build());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("projects")).isEqualTo(3);
    assertThat(dbClient.componentDao().selectByKey(db.getSession(), project.getDbKey()).get().getId()).isEqualTo(project.getId());
    assertThat(dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get().getId()).isEqualTo(removedDirectory.getId());
    assertThat(dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get().getId()).isEqualTo(removedFile.getId());
    assertExistButDisabled(removedDirectory.getDbKey(), removedFile.getDbKey());

    // commit the functional transaction
    dbClient.componentDao().applyBChangesForRootComponentUuid(db.getSession(), project.uuid());

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(db.getSession(), project.getDbKey()).get();
    assertThat(projectReloaded.getId()).isEqualTo(project.getId());
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());
    assertThat(projectReloaded.getUuidPath()).isEqualTo(project.getUuidPath());
    assertThat(projectReloaded.moduleUuid()).isEqualTo(project.moduleUuid());
    assertThat(projectReloaded.moduleUuidPath()).isEqualTo(project.moduleUuidPath());
    assertThat(projectReloaded.projectUuid()).isEqualTo(project.projectUuid());
    assertThat(projectReloaded.getRootUuid()).isEqualTo(project.getRootUuid());
    assertThat(projectReloaded.isEnabled()).isTrue();

    ComponentDto directoryReloaded = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get();
    assertThat(directoryReloaded.getId()).isEqualTo(removedDirectory.getId());
    assertThat(directoryReloaded.uuid()).isEqualTo(removedDirectory.uuid());
    assertThat(directoryReloaded.getUuidPath()).isEqualTo(removedDirectory.getUuidPath());
    assertThat(directoryReloaded.moduleUuid()).isEqualTo(removedDirectory.moduleUuid());
    assertThat(directoryReloaded.moduleUuidPath()).isEqualTo(removedDirectory.moduleUuidPath());
    assertThat(directoryReloaded.projectUuid()).isEqualTo(removedDirectory.projectUuid());
    assertThat(directoryReloaded.getRootUuid()).isEqualTo(removedDirectory.getRootUuid());
    assertThat(directoryReloaded.name()).isEqualTo(removedDirectory.name());
    assertThat(directoryReloaded.longName()).isEqualTo(removedDirectory.longName());
    assertThat(directoryReloaded.path()).isEqualTo(removedDirectory.path());
    assertThat(directoryReloaded.isEnabled()).isTrue();

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get();
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
      ComponentDto dto = dbClient.componentDao().selectByKey(db.getSession(), key).get();
      assertThat(dto.isEnabled()).isFalse();
    }
  }

  @Test
  public void persists_existing_components_with_visibility_of_root_in_db_out_of_functional_transaction() {
    ComponentDto project = prepareProject(p -> p.setPrivate(true));
    ComponentDto module = newModuleDto(project).setPrivate(false);
    db.components().insertComponent(module);
    ComponentDto dir = db.components().insertComponent(newDirectory(module, "DEFG", "Directory").setDbKey("DIR").setPrivate(true));
    treeRootHolder.setRoot(createSampleProjectComponentTree(project));

    underTest.execute(new TestComputationStepContext());

    Stream.of(project.uuid(), module.uuid(), dir.uuid())
      .forEach(uuid -> assertThat(dbClient.componentDao().selectByUuid(db.getSession(), uuid).get().isPrivate())
        .describedAs("for uuid " + uuid)
        .isEqualTo(true));
  }

  private ReportComponent createSampleProjectComponentTree(ComponentDto project) {
    return createSampleProjectComponentTree(project.uuid(), project.getDbKey());
  }

  private ReportComponent createSampleProjectComponentTree(String projectUuid, String projectKey) {
    return builder(PROJECT, 1).setUuid(projectUuid).setKey(projectKey)
      .setName("Project")
      .addChildren(
        builder(Component.Type.DIRECTORY, 3).setUuid("DEFG").setKey("DIR")
          .setName("Directory")
          .addChildren(
            builder(FILE, 4).setUuid("CDEF").setKey("FILE")
              .setName("file")
              .build())
          .build())
      .build();
  }

  private ReportComponent.Builder asTreeRoot(ComponentDto project) {
    return builder(PROJECT, 1).setUuid(project.uuid()).setKey(project.getDbKey()).setName(project.name());
  }

  private ComponentDto prepareProject(Consumer<ComponentDto>... populators) {
    ComponentDto dto = db.components().insertPrivateProject(db.organizations().insert(), populators);
    analysisMetadataHolder.setProject(Project.from(dto));
    analysisMetadataHolder.setBranch(new DefaultBranchImpl());
    return dto;
  }

  private ComponentDto prepareBranch(String branchName, Consumer<ComponentDto>... populators) {
    ComponentDto dto = db.components().insertPrivateProject(db.organizations().insert(), populators);
    analysisMetadataHolder.setProject(Project.from(dto));
    analysisMetadataHolder.setBranch(new TestBranch(branchName));
    return dto;
  }

  private static class TestBranch implements Branch {
    private final String name;

    public TestBranch(String name) {
      this.name = name;
    }

    @Override
    public BranchType getType() {
      return BranchType.LONG;
    }

    @Override
    public boolean isMain() {
      return false;
    }

    @Override
    public java.util.Optional<String> getMergeBranchUuid() {
      return java.util.Optional.empty();
    }

    @Override
    public boolean isLegacyFeature() {
      return false;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean supportsCrossProjectCpd() {
      return false;
    }

    @Override
    public String getPullRequestKey() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String generateKey(String projectKey, @Nullable String fileOrDirPath) {
      if (isEmpty(fileOrDirPath)) {
        return projectKey;
      }
      return ComponentKeys.createEffectiveKey(projectKey, trimToNull(fileOrDirPath));
    }
  }
}
