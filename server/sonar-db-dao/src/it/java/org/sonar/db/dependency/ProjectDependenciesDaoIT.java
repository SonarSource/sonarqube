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
package org.sonar.db.dependency;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectDependenciesDaoIT {

  private static final String PROJECT_BRANCH_UUID = "branchUuid";

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final ProjectDependenciesDao projectDependenciesDao = db.getDbClient().projectDependenciesDao();

  @Test
  void insert_shouldPersistProjectDependencies() {
    var projectDependencyDto = new ProjectDependencyDto("projectUuid", "version", "includePaths", "packageManager", 1L, 2L);

    projectDependenciesDao.insert(db.getSession(), projectDependencyDto);

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from project_dependencies");
    assertThat(select).hasSize(1);
    Map<String, Object> stringObjectMap = select.get(0);
    assertThat(stringObjectMap).containsExactlyInAnyOrderEntriesOf(
      Map.of(
        "uuid", projectDependencyDto.uuid(),
        "version", projectDependencyDto.version(),
        "include_paths", projectDependencyDto.includePaths(),
        "package_manager", projectDependencyDto.packageManager(),
        "created_at", projectDependencyDto.createdAt(),
        "updated_at", projectDependencyDto.updatedAt())
    );
  }

  @Test
  void deleteByUuid_shoudDeleteProjectDependencies() {
    var projectDependencyDto = new ProjectDependencyDto("projectUuid", "version", "includePaths", "packageManager", 1L, 2L);
    projectDependenciesDao.insert(db.getSession(), projectDependencyDto);

    projectDependenciesDao.deleteByUuid(db.getSession(), projectDependencyDto.uuid());

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from project_dependencies");
    assertThat(select).isEmpty();
  }

  @Test
  void selectByQuery_shouldReturnProjectDependencies_whenQueryByBranchUuid() {
    ProjectData projectData = db.components().insertPublicProject();
    var projectDependencyDto = new ProjectDependencyDto(projectData.getMainBranchComponent().uuid(), "version", "includePaths", "packageManager", 1L, 2L);
    projectDependenciesDao.insert(db.getSession(), projectDependencyDto);

    ProjectDependenciesQuery projectDependenciesQuery = new ProjectDependenciesQuery(projectData.mainBranchUuid(), null);
    List<ProjectDependencyDto> results = projectDependenciesDao.selectByQuery(db.getSession(), projectDependenciesQuery, Pagination.all());

    assertThat(results).hasSize(1);
    assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(projectDependencyDto);
  }

  @Test
  void selectByQuery_shouldReturnPaginatedProjectDependencies() {
    ProjectDependencyDto projectDependencyDto1 = insertProjectDependency("1");
    ProjectDependencyDto projectDependencyDto2 = insertProjectDependency("2");
    ProjectDependencyDto projectDependencyDto3 = insertProjectDependency("3");
    ProjectDependencyDto projectDependencyDto4 = insertProjectDependency("4");

    ProjectDependenciesQuery projectDependenciesQuery = new ProjectDependenciesQuery(PROJECT_BRANCH_UUID, null);
    List<ProjectDependencyDto> page1Results = projectDependenciesDao.selectByQuery(db.getSession(), projectDependenciesQuery, Pagination.forPage(1).andSize(2));
    List<ProjectDependencyDto> page2Results = projectDependenciesDao.selectByQuery(db.getSession(), projectDependenciesQuery, Pagination.forPage(2).andSize(2));

    assertThat(page1Results).hasSize(2);
    assertThat(page1Results.get(0)).usingRecursiveComparison().isEqualTo(projectDependencyDto1);
    assertThat(page1Results.get(1)).usingRecursiveComparison().isEqualTo(projectDependencyDto2);
    assertThat(page2Results).hasSize(2);
    assertThat(page2Results.get(0)).usingRecursiveComparison().isEqualTo(projectDependencyDto3);
    assertThat(page2Results.get(1)).usingRecursiveComparison().isEqualTo(projectDependencyDto4);
  }

  @Test
  void selectByQuery_shouldPartiallyMatchLongName_whenQueriedByText() {
    ProjectDependencyDto projectDepSearched = insertProjectDependency("sEArched");
    insertProjectDependency("notWanted");
    ProjectDependencyDto projectDepSearchAsWell = insertProjectDependency("sEArchedAsWell");
    insertProjectDependency("notwantedeither");

    ProjectDependenciesQuery projectDependenciesQuery = new ProjectDependenciesQuery(PROJECT_BRANCH_UUID, "long_nameSearCHed");
    List<ProjectDependencyDto> results = projectDependenciesDao.selectByQuery(db.getSession(), projectDependenciesQuery, Pagination.all());

    assertThat(results).hasSize(2);
    assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(projectDepSearched);
    assertThat(results.get(1)).usingRecursiveComparison().isEqualTo(projectDepSearchAsWell);
  }

  @Test
  void selectByQuery_shouldExactlyMatchKee_whenQueriedByText() {
    ProjectDependencyDto projectDepSearched = insertProjectDependency("1", dto -> dto.setKey("keySearched"));
    insertProjectDependency("2", dto -> dto.setKey("KEySearCHed"));
    insertProjectDependency("3", dto -> dto.setKey("some_keySearched"));

    ProjectDependenciesQuery projectDependenciesQuery = new ProjectDependenciesQuery(PROJECT_BRANCH_UUID, "keySearched");
    List<ProjectDependencyDto> results = projectDependenciesDao.selectByQuery(db.getSession(), projectDependenciesQuery, Pagination.all());

    assertThat(results).hasSize(1);
    assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(projectDepSearched);
  }

  @Test
  void update_shouldUpdateProjectDependency() {
    ProjectDependencyDto projectDependencyDto = insertProjectDependency();
    ProjectDependencyDto updatedProjectDependency =
      new ProjectDependencyDto(projectDependencyDto.uuid(), "updatedVersion", "updatedIncludePaths", "updatedPackageManager", 2L, 3L);

    projectDependenciesDao.update(db.getSession(), updatedProjectDependency);

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from project_dependencies");
    assertThat(select).hasSize(1);
    Map<String, Object> stringObjectMap = select.get(0);
    assertThat(stringObjectMap).containsExactlyInAnyOrderEntriesOf(
      Map.of(
        "uuid", updatedProjectDependency.uuid(),
        "version", updatedProjectDependency.version(),
        "include_paths", updatedProjectDependency.includePaths(),
        "package_manager", updatedProjectDependency.packageManager(),
        "created_at", projectDependencyDto.createdAt(),
        "updated_at", updatedProjectDependency.updatedAt())
    );
  }

  @Test
  void countByQuery_shouldReturnTheTotalOfDependencies() {
    insertProjectDependency("sEArched");
    insertProjectDependency("notWanted");
    insertProjectDependency("sEArchedAsWell");
    db.projectDependencies().insertProjectDependency("another_branch_uuid", "searched");

    ProjectDependenciesQuery projectDependenciesQuery = new ProjectDependenciesQuery(PROJECT_BRANCH_UUID, "long_nameSearCHed");

    assertThat(projectDependenciesDao.countByQuery(db.getSession(), projectDependenciesQuery)).isEqualTo(2);
    assertThat(projectDependenciesDao.countByQuery(db.getSession(), new ProjectDependenciesQuery(PROJECT_BRANCH_UUID, null))).isEqualTo(3);
    assertThat(projectDependenciesDao.countByQuery(db.getSession(), new ProjectDependenciesQuery("another_branch_uuid", null))).isEqualTo(1);
  }

  private ProjectDependencyDto insertProjectDependency() {
    return db.projectDependencies().insertProjectDependency(PROJECT_BRANCH_UUID);
  }
  
  private ProjectDependencyDto insertProjectDependency(String suffix) {
    return insertProjectDependency(suffix, null);
  }

  private ProjectDependencyDto insertProjectDependency(String suffix, @Nullable Consumer<ComponentDto> dtoPopulator) {
    return db.projectDependencies().insertProjectDependency(PROJECT_BRANCH_UUID, suffix, dtoPopulator);
  }
}
