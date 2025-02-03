/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.sca;

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

class ScaDependenciesDaoIT {

  private static final String PROJECT_BRANCH_UUID = "branchUuid";

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final ScaDependenciesDao scaDependenciesDao = db.getDbClient().scaDependenciesDao();

  @Test
  void insert_shouldPersistScaDependencies() {
    var scaDependencyDto = insertScaDependency();

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from sca_dependencies");
    assertThat(select).hasSize(1);
    Map<String, Object> stringObjectMap = select.get(0);
    assertThat(stringObjectMap).containsExactlyInAnyOrderEntriesOf(
      Map.ofEntries(
        Map.entry("uuid", scaDependencyDto.uuid()),
        Map.entry("component_uuid", scaDependencyDto.componentUuid()),
        Map.entry("package_url", scaDependencyDto.packageUrl()),
        Map.entry("package_manager", scaDependencyDto.packageManager().name()),
        Map.entry("package_name", scaDependencyDto.packageName()),
        Map.entry("version", scaDependencyDto.version()),
        Map.entry("direct", scaDependencyDto.direct()),
        Map.entry("scope", scaDependencyDto.scope()),
        Map.entry("dependency_file_path", scaDependencyDto.dependencyFilePath()),
        Map.entry("license_expression", scaDependencyDto.licenseExpression()),
        Map.entry("known", scaDependencyDto.known()),
        Map.entry("created_at", scaDependencyDto.createdAt()),
        Map.entry("updated_at", scaDependencyDto.updatedAt())
      )
    );
  }

  @Test
  void deleteByUuid_shouldDeleteScaDependencies() {
    var scaDependencyDto = insertScaDependency();

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from sca_dependencies");
    assertThat(select).isNotEmpty();

    scaDependenciesDao.deleteByUuid(db.getSession(), scaDependencyDto.uuid());

    select = db.select(db.getSession(), "select * from sca_dependencies");
    assertThat(select).isEmpty();
  }

  @Test
  void selectByQuery_shouldReturnScaDependencies_whenQueryByBranchUuid() {
    ProjectData projectData = db.components().insertPublicProject();
    ScaDependencyDto scaDependencyDto = db.getScaDependenciesDbTester().insertScaDependency(projectData.mainBranchUuid());

    ScaDependenciesQuery scaDependenciesQuery = new ScaDependenciesQuery(projectData.mainBranchUuid(), null);
    List<ScaDependencyDto> results = scaDependenciesDao.selectByQuery(db.getSession(), scaDependenciesQuery, Pagination.all());

    assertThat(results).hasSize(1);
    assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(scaDependencyDto);
  }

  @Test
  void selectByQuery_shouldReturnPaginatedScaDependencies() {
    ScaDependencyDto scaDependencyDto1 = insertScaDependency("1");
    ScaDependencyDto scaDependencyDto2 = insertScaDependency("2");
    ScaDependencyDto scaDependencyDto3 = insertScaDependency("3");
    ScaDependencyDto scaDependencyDto4 = insertScaDependency("4");

    ScaDependenciesQuery scaDependenciesQuery = new ScaDependenciesQuery(PROJECT_BRANCH_UUID, null);
    List<ScaDependencyDto> page1Results = scaDependenciesDao.selectByQuery(db.getSession(), scaDependenciesQuery, Pagination.forPage(1).andSize(2));
    List<ScaDependencyDto> page2Results = scaDependenciesDao.selectByQuery(db.getSession(), scaDependenciesQuery, Pagination.forPage(2).andSize(2));

    assertThat(page1Results).hasSize(2);
    assertThat(page1Results.get(0)).usingRecursiveComparison().isEqualTo(scaDependencyDto1);
    assertThat(page1Results.get(1)).usingRecursiveComparison().isEqualTo(scaDependencyDto2);
    assertThat(page2Results).hasSize(2);
    assertThat(page2Results.get(0)).usingRecursiveComparison().isEqualTo(scaDependencyDto3);
    assertThat(page2Results.get(1)).usingRecursiveComparison().isEqualTo(scaDependencyDto4);
  }

  @Test
  void selectByQuery_shouldPartiallyMatchLongName_whenQueriedByText() {
    ScaDependencyDto projectDepSearched = insertScaDependency("sEArched");
    insertScaDependency("notWanted");
    ScaDependencyDto projectDepSearchAsWell = insertScaDependency("sEArchedAsWell");
    insertScaDependency("notwantedeither");

    ScaDependenciesQuery scaDependenciesQuery = new ScaDependenciesQuery(PROJECT_BRANCH_UUID, "long_nameSearCHed");
    List<ScaDependencyDto> results = scaDependenciesDao.selectByQuery(db.getSession(), scaDependenciesQuery, Pagination.all());

    assertThat(results).hasSize(2);
    assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(projectDepSearched);
    assertThat(results.get(1)).usingRecursiveComparison().isEqualTo(projectDepSearchAsWell);
  }

  @Test
  void selectByQuery_shouldExactlyMatchKee_whenQueriedByText() {
    ScaDependencyDto projectDepSearched = insertScaDependency("1", dto -> dto.setKey("keySearched"));
    insertScaDependency("2", dto -> dto.setKey("KEySearCHed"));
    insertScaDependency("3", dto -> dto.setKey("some_keySearched"));

    ScaDependenciesQuery scaDependenciesQuery = new ScaDependenciesQuery(PROJECT_BRANCH_UUID, "keySearched");
    List<ScaDependencyDto> results = scaDependenciesDao.selectByQuery(db.getSession(), scaDependenciesQuery, Pagination.all());

    assertThat(results).hasSize(1);
    assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(projectDepSearched);
  }

  @Test
  void update_shouldUpdateScaDependency() {
    ScaDependencyDto scaDependencyDto = insertScaDependency();
    ScaDependencyDto updatedScaDependency =
      scaDependencyDto.toBuilder().setUpdatedAt(scaDependencyDto.updatedAt() + 1).setVersion("newVersion").build();

    scaDependenciesDao.update(db.getSession(), updatedScaDependency);

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from sca_dependencies");
    assertThat(select).hasSize(1);
    Map<String, Object> stringObjectMap = select.get(0);
    assertThat(stringObjectMap).containsExactlyInAnyOrderEntriesOf(
      Map.ofEntries(
        Map.entry("uuid", updatedScaDependency.uuid()),
        Map.entry("component_uuid", updatedScaDependency.componentUuid()),
        Map.entry("package_url", updatedScaDependency.packageUrl()),
        Map.entry("package_manager", updatedScaDependency.packageManager().name()),
        Map.entry("package_name", updatedScaDependency.packageName()),
        Map.entry("version", updatedScaDependency.version()),
        Map.entry("direct", updatedScaDependency.direct()),
        Map.entry("scope", updatedScaDependency.scope()),
        Map.entry("dependency_file_path", updatedScaDependency.dependencyFilePath()),
        Map.entry("license_expression", updatedScaDependency.licenseExpression()),
        Map.entry("known", updatedScaDependency.known()),
        Map.entry("created_at", updatedScaDependency.createdAt()),
        Map.entry("updated_at", updatedScaDependency.updatedAt())
      )
    );
  }

  @Test
  void countByQuery_shouldReturnTheTotalOfDependencies() {
    insertScaDependency("sEArched");
    insertScaDependency("notWanted");
    insertScaDependency("sEArchedAsWell");
    db.getScaDependenciesDbTester().insertScaDependency("another_branch_uuid", "searched");

    ScaDependenciesQuery scaDependenciesQuery = new ScaDependenciesQuery(PROJECT_BRANCH_UUID, "long_nameSearCHed");

    assertThat(scaDependenciesDao.countByQuery(db.getSession(), scaDependenciesQuery)).isEqualTo(2);
    assertThat(scaDependenciesDao.countByQuery(db.getSession(), new ScaDependenciesQuery(PROJECT_BRANCH_UUID, null))).isEqualTo(3);
    assertThat(scaDependenciesDao.countByQuery(db.getSession(), new ScaDependenciesQuery("another_branch_uuid", null))).isEqualTo(1);
  }

  private ScaDependencyDto insertScaDependency() {
    return db.getScaDependenciesDbTester().insertScaDependency(PROJECT_BRANCH_UUID);
  }
  
  private ScaDependencyDto insertScaDependency(String suffix) {
    return insertScaDependency(suffix, null);
  }

  private ScaDependencyDto insertScaDependency(String suffix, @Nullable Consumer<ComponentDto> dtoPopulator) {
    return db.getScaDependenciesDbTester().insertScaDependency(PROJECT_BRANCH_UUID, suffix, dtoPopulator);
  }
}
