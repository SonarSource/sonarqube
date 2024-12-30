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
package org.sonar.ce.task.projectanalysis.step;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.TestBranch;
import org.sonar.ce.task.projectanalysis.component.BranchPersister;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.ce.task.projectanalysis.component.FileAttributes;
import org.sonar.ce.task.projectanalysis.component.MutableDisabledComponentsHolder;
import org.sonar.ce.task.projectanalysis.component.ProjectPersister;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.dependency.MutableProjectDependenciesHolderRule;
import org.sonar.ce.task.projectanalysis.dependency.ProjectDependencyImpl;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.DIRECTORY;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;
import static org.sonar.ce.task.projectanalysis.component.ReportComponent.builder;
import static org.sonar.db.component.BranchDto.DEFAULT_MAIN_BRANCH_NAME;
import static org.sonar.db.component.ComponentDto.UUID_PATH_OF_ROOT;
import static org.sonar.db.component.ComponentDto.UUID_PATH_SEPARATOR;
import static org.sonar.db.component.ComponentTesting.newDirectory;

public class ReportPersistComponentsStepIT extends BaseStepTest {

  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
  private static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public MutableProjectDependenciesHolderRule projectDepsHolder = new MutableProjectDependenciesHolderRule();

  private final System2 system2 = mock(System2.class);
  private final DbClient dbClient = db.getDbClient();
  private Date now;
  private final MutableDisabledComponentsHolder disabledComponentsHolder = mock(MutableDisabledComponentsHolder.class, RETURNS_DEEP_STUBS);
  private PersistComponentsStep underTest;

  @Before
  public void setup() throws Exception {
    now = DATE_FORMAT.parse("2015-06-02");
    when(system2.now()).thenReturn(now.getTime());

    BranchPersister branchPersister = mock(BranchPersister.class);
    ProjectPersister projectPersister = mock(ProjectPersister.class);
    underTest = new PersistComponentsStep(dbClient, treeRootHolder, system2, disabledComponentsHolder, branchPersister, projectPersister, projectDepsHolder);
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
    projectDepsHolder.setDependencies(List.of());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("components")).isEqualTo(3);

    ComponentDto directoryDto = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get();
    assertThat(directoryDto.name()).isEqualTo("dir");
    assertThat(directoryDto.longName()).isEqualTo("src/main/java/dir");
    assertThat(directoryDto.description()).isNull();
    assertThat(directoryDto.path()).isEqualTo("src/main/java/dir");
    assertThat(directoryDto.uuid()).isEqualTo("CDEF");
    assertThat(directoryDto.getUuidPath()).isEqualTo(UUID_PATH_SEPARATOR + projectDto.uuid() + UUID_PATH_SEPARATOR);
    assertThat(directoryDto.branchUuid()).isEqualTo(projectDto.uuid());
    assertThat(directoryDto.qualifier()).isEqualTo("DIR");
    assertThat(directoryDto.scope()).isEqualTo("DIR");
    assertThat(directoryDto.getCreatedAt()).isEqualTo(now);

    ComponentDto fileDto = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileDto.name()).isEqualTo("Foo.java");
    assertThat(fileDto.longName()).isEqualTo("src/main/java/dir/Foo.java");
    assertThat(fileDto.description()).isNull();
    assertThat(fileDto.path()).isEqualTo("src/main/java/dir/Foo.java");
    assertThat(fileDto.language()).isEqualTo("java");
    assertThat(fileDto.uuid()).isEqualTo("DEFG");
    assertThat(fileDto.getUuidPath()).isEqualTo(directoryDto.getUuidPath() + directoryDto.uuid() + UUID_PATH_SEPARATOR);
    assertThat(fileDto.branchUuid()).isEqualTo(projectDto.uuid());
    assertThat(fileDto.qualifier()).isEqualTo("FIL");
    assertThat(fileDto.scope()).isEqualTo("FIL");
    assertThat(fileDto.getCreatedAt()).isEqualTo(now);
  }

  @Test
  public void persist_components_of_existing_branch() {
    ComponentDto branch = prepareBranch("feature/foo");
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
    Component treeRoot = asTreeRoot(branch)
      .addChildren(directory)
      .build();
    treeRootHolder.setRoot(treeRoot);
    projectDepsHolder.setDependencies(List.of());

    underTest.execute(new TestComputationStepContext());

    // 3 in this branch plus the project
    assertThat(db.countRowsOfTable("components")).isEqualTo(4);

    ComponentDto directoryDto = dbClient.componentDao().selectByKeyAndBranch(db.getSession(), "PROJECT_KEY:src/main/java/dir", "feature/foo").get();
    assertThat(directoryDto.name()).isEqualTo("dir");
    assertThat(directoryDto.longName()).isEqualTo("src/main/java/dir");
    assertThat(directoryDto.description()).isNull();
    assertThat(directoryDto.path()).isEqualTo("src/main/java/dir");
    assertThat(directoryDto.uuid()).isEqualTo("CDEF");
    assertThat(directoryDto.getUuidPath()).isEqualTo(UUID_PATH_SEPARATOR + branch.uuid() + UUID_PATH_SEPARATOR);
    assertThat(directoryDto.branchUuid()).isEqualTo(branch.uuid());
    assertThat(directoryDto.qualifier()).isEqualTo("DIR");
    assertThat(directoryDto.scope()).isEqualTo("DIR");
    assertThat(directoryDto.getCreatedAt()).isEqualTo(now);

    ComponentDto fileDto = dbClient.componentDao().selectByKeyAndBranch(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java", "feature/foo").get();
    assertThat(fileDto.name()).isEqualTo("Foo.java");
    assertThat(fileDto.longName()).isEqualTo("src/main/java/dir/Foo.java");
    assertThat(fileDto.description()).isNull();
    assertThat(fileDto.path()).isEqualTo("src/main/java/dir/Foo.java");
    assertThat(fileDto.language()).isEqualTo("java");
    assertThat(fileDto.uuid()).isEqualTo("DEFG");
    assertThat(fileDto.getUuidPath()).isEqualTo(directoryDto.getUuidPath() + directoryDto.uuid() + UUID_PATH_SEPARATOR);
    assertThat(fileDto.branchUuid()).isEqualTo(branch.uuid());
    assertThat(fileDto.qualifier()).isEqualTo("FIL");
    assertThat(fileDto.scope()).isEqualTo("FIL");
    assertThat(fileDto.getCreatedAt()).isEqualTo(now);
  }

  @Test
  public void persist_file_directly_attached_on_root_directory() {
    ComponentDto projectDto = prepareProject();
    treeRootHolder.setRoot(
      asTreeRoot(projectDto)
        .addChildren(
          builder(FILE, 2).setUuid("DEFG").setKey(projectDto.getKey() + ":pom.xml")
            .setName("pom.xml")
            .build())
        .build());
    projectDepsHolder.setDependencies(List.of());

    underTest.execute(new TestComputationStepContext());

    assertThat(dbClient.componentDao().selectByKey(db.getSession(), projectDto.getKey() + ":/")).isNotPresent();

    ComponentDto file = dbClient.componentDao().selectByKey(db.getSession(), projectDto.getKey() + ":pom.xml").get();
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
    projectDepsHolder.setDependencies(List.of());

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
    ComponentDto directory = ComponentTesting.newDirectory(project, "src").setUuid("CDEF").setKey("PROJECT_KEY:src");
    ComponentDto file = ComponentTesting.newFileDto(project, directory, "DEFG").setPath("src/foo").setName("foo")
      .setKey("PROJECT_KEY:src/foo");
    dbClient.componentDao().insert(db.getSession(), Set.of(directory, file), true);
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
    projectDepsHolder.setDependencies(List.of());

    underTest.execute(new TestComputationStepContext());

    // commit the functional transaction
    dbClient.componentDao().applyBChangesForBranchUuid(db.getSession(), project.uuid());
    db.commit();

    assertThat(dbClient.componentDao().selectByKey(db.getSession(), PROJECT_KEY + ":src/foo").get().scope()).isEqualTo("DIR");
  }

  @Test
  public void persist_only_new_components() {
    // Project and module already exists
    ComponentDto project = prepareProject();
    db.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid(project.uuid()).setKey(project.getKey())
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
    projectDepsHolder.setDependencies(List.of());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("components")).isEqualTo(3);

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(db.getSession(), project.getKey()).get();
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());
    assertThat(projectReloaded.getUuidPath()).isEqualTo(UUID_PATH_OF_ROOT);

    ComponentDto directory = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get();
    assertThat(directory.getUuidPath()).isEqualTo(directory.getUuidPath());
    assertThat(directory.branchUuid()).isEqualTo(project.uuid());

    ComponentDto file = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get();
    assertThat(file.getUuidPath()).isEqualTo(file.getUuidPath());
    assertThat(file.branchUuid()).isEqualTo(project.uuid());
  }

  @Test
  public void nothing_to_persist() {
    ComponentDto project = prepareProject();
    ComponentDto directory = ComponentTesting.newDirectory(project, "src/main/java/dir").setUuid("CDEF").setKey("PROJECT_KEY:src/main/java/dir");
    ComponentDto file = ComponentTesting.newFileDto(project, directory, "DEFG").setPath("src/main/java/dir/Foo.java").setName("Foo.java")
      .setKey("PROJECT_KEY:src/main/java/dir/Foo.java");
    dbClient.componentDao().insert(db.getSession(), Set.of(directory, file), true);
    db.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid(project.uuid()).setKey(project.getKey())
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
    projectDepsHolder.setDependencies(List.of());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("components")).isEqualTo(3);
    assertThat(dbClient.componentDao().selectByKey(db.getSession(), project.getKey()).get().uuid()).isEqualTo(project.uuid());
    assertThat(dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get().uuid()).isEqualTo(directory.uuid());
    assertThat(dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get().uuid()).isEqualTo(file.uuid());

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(db.getSession(), project.getKey()).get();
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());
    assertThat(projectReloaded.branchUuid()).isEqualTo(project.branchUuid());

    ComponentDto directoryReloaded = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get();
    assertThat(directoryReloaded.uuid()).isEqualTo(directory.uuid());
    assertThat(directoryReloaded.getUuidPath()).isEqualTo(directory.getUuidPath());
    assertThat(directoryReloaded.branchUuid()).isEqualTo(directory.branchUuid());
    assertThat(directoryReloaded.name()).isEqualTo(directory.name());
    assertThat(directoryReloaded.path()).isEqualTo(directory.path());

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileReloaded.uuid()).isEqualTo(file.uuid());
    assertThat(fileReloaded.getUuidPath()).isEqualTo(file.getUuidPath());
    assertThat(fileReloaded.branchUuid()).isEqualTo(file.branchUuid());
    assertThat(fileReloaded.name()).isEqualTo(file.name());
    assertThat(fileReloaded.path()).isEqualTo(file.path());
  }

  @Test
  public void do_not_update_created_at_on_existing_component() {
    Date oldDate = DateUtils.parseDate("2015-01-01");
    ComponentDto project = prepareProject(p -> p.setCreatedAt(oldDate));
    db.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid(project.uuid()).setKey(project.getKey())
        .build());
    projectDepsHolder.setDependencies(List.of());

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
      .setKey("PROJECT_KEY:src/main/java/dir")
      .setEnabled(false);
    ComponentDto removedFile = ComponentTesting.newFileDto(project, removedDirectory, "DEFG")
      .setPath("src/main/java/dir/Foo.java")
      .setLongName("src/main/java/dir/Foo.java")
      .setName("Foo.java")
      .setKey("PROJECT_KEY:src/main/java/dir/Foo.java")
      .setEnabled(false);
    dbClient.componentDao().insert(db.getSession(), Set.of(removedDirectory, removedFile), true);
    db.getSession().commit();

    treeRootHolder.setRoot(
      builder(PROJECT, 1).setUuid(project.uuid()).setKey(project.getKey())
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
    projectDepsHolder.setDependencies(List.of());

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable("components")).isEqualTo(3);
    assertThat(dbClient.componentDao().selectByKey(db.getSession(), project.getKey()).get().uuid()).isEqualTo(project.uuid());
    assertThat(dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get().uuid()).isEqualTo(removedDirectory.uuid());
    assertThat(dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get().uuid()).isEqualTo(removedFile.uuid());
    assertExistButDisabled(removedDirectory.getKey(), removedFile.getKey());

    // commit the functional transaction
    dbClient.componentDao().applyBChangesForBranchUuid(db.getSession(), project.uuid());

    ComponentDto projectReloaded = dbClient.componentDao().selectByKey(db.getSession(), project.getKey()).get();
    assertThat(projectReloaded.uuid()).isEqualTo(project.uuid());
    assertThat(projectReloaded.getUuidPath()).isEqualTo(project.getUuidPath());
    assertThat(projectReloaded.branchUuid()).isEqualTo(project.branchUuid());
    assertThat(projectReloaded.isEnabled()).isTrue();

    ComponentDto directoryReloaded = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir").get();
    assertThat(directoryReloaded.uuid()).isEqualTo(removedDirectory.uuid());
    assertThat(directoryReloaded.getUuidPath()).isEqualTo(removedDirectory.getUuidPath());
    assertThat(directoryReloaded.branchUuid()).isEqualTo(removedDirectory.branchUuid());
    assertThat(directoryReloaded.name()).isEqualTo(removedDirectory.name());
    assertThat(directoryReloaded.longName()).isEqualTo(removedDirectory.longName());
    assertThat(directoryReloaded.path()).isEqualTo(removedDirectory.path());
    assertThat(directoryReloaded.isEnabled()).isTrue();

    ComponentDto fileReloaded = dbClient.componentDao().selectByKey(db.getSession(), "PROJECT_KEY:src/main/java/dir/Foo.java").get();
    assertThat(fileReloaded.uuid()).isEqualTo(removedFile.uuid());
    assertThat(fileReloaded.getUuidPath()).isEqualTo(removedFile.getUuidPath());
    assertThat(fileReloaded.branchUuid()).isEqualTo(removedFile.branchUuid());
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
    ComponentDto dir = db.components().insertComponent(newDirectory(project, "DEFG", "Directory").setKey("DIR").setPrivate(true));
    treeRootHolder.setRoot(createSampleProjectComponentTree(project));
    projectDepsHolder.setDependencies(List.of());

    underTest.execute(new TestComputationStepContext());

    Stream.of(project.uuid(), dir.uuid())
      .forEach(uuid -> assertThat(dbClient.componentDao().selectByUuid(db.getSession(), uuid).get().isPrivate())
        .describedAs("for uuid " + uuid)
        .isTrue());
  }

  @Test
  public void persist_dependencies() {
    ComponentDto projectDto = prepareProject();
    treeRootHolder.setRoot(asTreeRoot(projectDto).build());
    projectDepsHolder.setDependencies(List.of(ProjectDependencyImpl.builder()
        .setUuid("DEFG")
        .setKey(PROJECT_KEY + ":mvn+com.google.guava:guava$28.2-jre")
        .setName("guava")
        .setFullName("com.google.guava:guava")
        .setDescription("Some long description about Guava")
        .setVersion("28.2-jre")
        .setPackageManager("mvn")
      .build()));

    underTest.execute(new TestComputationStepContext());

    ComponentDto depByKey = dbClient.componentDao().selectByKey(db.getSession(), PROJECT_KEY + ":mvn+com.google.guava:guava$28.2-jre").get();
    assertThat(depByKey.getKey()).isEqualTo(PROJECT_KEY + ":mvn+com.google.guava:guava$28.2-jre");
    assertThat(depByKey.name()).isEqualTo("guava");
    assertThat(depByKey.longName()).isEqualTo("com.google.guava:guava");
    assertThat(depByKey.description()).isEqualTo("Some long description about Guava");
    assertThat(depByKey.path()).isNull();
    assertThat(depByKey.qualifier()).isEqualTo("DEP");
    assertThat(depByKey.scope()).isEqualTo("DEP");
  }

  private ReportComponent createSampleProjectComponentTree(ComponentDto project) {
    return createSampleProjectComponentTree(project.uuid(), project.getKey());
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
    return builder(PROJECT, 1).setUuid(project.uuid()).setKey(project.getKey()).setName(project.name());
  }

  private ComponentDto prepareProject() {
    return prepareProject(defaults());
  }

  private ComponentDto prepareProject(Consumer<ComponentDto> populators) {
    ComponentDto dto = db.components().insertPrivateProject(populators).getMainBranchComponent();
    analysisMetadataHolder.setProject(Project.from(dto));
    analysisMetadataHolder.setBranch(new DefaultBranchImpl(DEFAULT_MAIN_BRANCH_NAME));
    return dto;
  }

  private ComponentDto prepareBranch(String branchName) {
    ComponentDto projectDto = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branchDto = db.components().insertProjectBranch(projectDto, b -> b.setKey(branchName));
    analysisMetadataHolder.setProject(Project.from(projectDto));
    analysisMetadataHolder.setBranch(new TestBranch(branchName));
    return branchDto;
  }

  private static <T> Consumer<T> defaults() {
    return t -> {
    };
  }

}
