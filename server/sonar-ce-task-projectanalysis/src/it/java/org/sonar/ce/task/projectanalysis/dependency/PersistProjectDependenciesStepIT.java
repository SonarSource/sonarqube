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
package org.sonar.ce.task.projectanalysis.dependency;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.dependency.ProjectDependencyDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class PersistProjectDependenciesStepIT {

  public static final int NOW = 1_000_000;
  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  @RegisterExtension
  private final MutableProjectDependenciesHolderRule projectDependenciesHolder = new MutableProjectDependenciesHolderRule();
  @RegisterExtension
  private final TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  private final DbSession dbSession = db.getSession();
  private final DbClient dbClient = db.getDbClient();

  private PersistProjectDependenciesStep underTest;
  private ComponentDto branch;

  @BeforeEach
  void setUp() {
    ProjectData projectData = db.components().insertPublicProject();
    branch = db.components().insertProjectBranch(projectData.getMainBranchComponent());
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(branch.uuid()).build());
    underTest = new PersistProjectDependenciesStep(dbClient, projectDependenciesHolder, treeRootHolder, Clock.fixed(Instant.ofEpochMilli(NOW), ZoneId.of("UTC")));
  }

  @Test
  void getDescription_shouldReturnStepDescription() {
    assertThat(underTest.getDescription()).isEqualTo("Persist project dependencies");
  }

  @Test
  void execute_shouldInsertNewDependencies() {
    ProjectDependency minimalDep = buildDependency("mini").build();
    ProjectDependency depWithVersion = buildDependency("version").setVersion("1.0").build();
    ProjectDependency depWithFullName = buildDependency("fullname").setFullName("fullname").build();
    ProjectDependency depWithDescription = buildDependency("desc").setDescription("Some description").build();
    ProjectDependency depWithPackageManager = buildDependency("packagemanager").setPackageManager("mvn").build();
    projectDependenciesHolder.setDependencies(List.of(minimalDep, depWithVersion, depWithFullName, depWithDescription, depWithPackageManager));

    underTest.execute(new TestComputationStepContext());

    assertDependencyPersistedInDatabase(minimalDep, NOW, NOW);
    assertDependencyPersistedInDatabase(depWithVersion, NOW, NOW);
    assertDependencyPersistedInDatabase(depWithFullName, NOW, NOW);
    assertDependencyPersistedInDatabase(depWithDescription, NOW, NOW);
    assertDependencyPersistedInDatabase(depWithPackageManager, NOW, NOW);
  }

  private void assertDependencyPersistedInDatabase(ProjectDependency dep, long createdAt, long updatedAt) {
    ProjectDependencyDto depDto = dbClient.projectDependenciesDao().selectByUuid(dbSession, dep.getUuid())
      .orElseGet(() -> fail(String.format("Dependency with uuid %s not found", dep.getUuid())));
    assertThat(depDto.uuid()).isEqualTo(dep.getUuid());
    assertThat(depDto.version()).isEqualTo(dep.getVersion());
    assertThat(depDto.packageManager()).isEqualTo(dep.getPackageManager());
    assertThat(depDto.includePaths()).isNull();
    assertThat(depDto.createdAt()).isEqualTo(createdAt);
    assertThat(depDto.updatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void execute_shouldUpdateExistingDependencies() {
    db.components().insertComponent(ComponentTesting.newDependencyDto(branch, "dep-uuid-1"));
    dbClient.projectDependenciesDao().insert(dbSession, new ProjectDependencyDto("dep-uuid-1", "1.0-OLD", null, "mvn-old", NOW - 10L, NOW - 10L));
    db.components().insertComponent(ComponentTesting.newDependencyDto(branch, "dep-uuid-2"));
    dbClient.projectDependenciesDao().insert(dbSession, new ProjectDependencyDto("dep-uuid-2", "2.0-OLD", null, "npm-old", NOW - 10L, NOW - 10L));
    db.commit();
    ProjectDependency dep1 = buildDependency("1").build();
    ProjectDependency dep2 = buildDependency("2").setVersion("2.0").setPackageManager("npm").build();
    projectDependenciesHolder.setDependencies(List.of(dep1, dep2));

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable(dbSession, "project_dependencies")).isEqualTo(2);
    assertDependencyPersistedInDatabase(dep1, NOW - 10L, NOW);
    assertDependencyPersistedInDatabase(dep2, NOW - 10L, NOW);
  }

  @Test
  void execute_shoudDeleteNonExistingDependenciesAndDontTouchUnchanged() {
    db.components().insertComponent(ComponentTesting.newDependencyDto(branch, "dep-uuid-1"));
    dbClient.projectDependenciesDao().insert(dbSession, new ProjectDependencyDto("dep-uuid-1", null, null, null, NOW - 10L, NOW - 10L));
    db.components().insertComponent(ComponentTesting.newDependencyDto(branch, "dep-uuid-2"));
    dbClient.projectDependenciesDao().insert(dbSession, new ProjectDependencyDto("dep-uuid-2", "2.0-OLD", null, "npm-old", NOW - 10L, NOW - 10L));
    db.commit();
    ProjectDependency dep1 = buildDependency("1").build();
    projectDependenciesHolder.setDependencies(List.of(dep1));

    underTest.execute(new TestComputationStepContext());

    assertThat(db.countRowsOfTable(dbSession, "project_dependencies")).isEqualTo(1);
    assertDependencyPersistedInDatabase(dep1, NOW - 10L, NOW - 10L);
  }

  private static ProjectDependencyImpl.Builder buildDependency(String suffix) {
    return ProjectDependencyImpl.builder()
      .setUuid("dep-uuid-" + suffix)
      .setName("name-" + suffix)
      .setKey("key-" + suffix);
  }
}
