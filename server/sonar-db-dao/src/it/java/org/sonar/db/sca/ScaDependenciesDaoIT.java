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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;

import static org.assertj.core.api.Assertions.assertThat;

class ScaDependenciesDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final ScaDependenciesDao scaDependenciesDao = db.getDbClient().scaDependenciesDao();

  @Test
  void insert_shouldPersistScaDependencies() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaDependencyDto scaDependencyDto = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "1", true, PackageManager.MAVEN, "foo.bar");

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
    ComponentDto componentDto = prepareComponentDto("1");
    ScaDependencyDto scaDependencyDto = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "1", true, PackageManager.MAVEN, "foo.bar");

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from sca_dependencies");
    assertThat(select).isNotEmpty();

    scaDependenciesDao.deleteByUuid(db.getSession(), scaDependencyDto.uuid());

    select = db.select(db.getSession(), "select * from sca_dependencies");
    assertThat(select).isEmpty();
  }

  @Test
  void selectByUuid_shouldLoadScaDependency() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaDependencyDto scaDependencyDto = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "1", true, PackageManager.MAVEN, "foo.bar");

    var loadedOptional = scaDependenciesDao.selectByUuid(db.getSession(), scaDependencyDto.uuid());

    assertThat(loadedOptional).contains(scaDependencyDto);
  }

  @Test
  void selectByQuery_shouldReturnScaDependencies_whenQueryByBranchUuid() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaDependencyDto scaDependencyDto = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "1", true, PackageManager.MAVEN, "foo.bar");

    ScaDependenciesQuery scaDependenciesQuery = new ScaDependenciesQuery(componentDto.branchUuid(), null, null, null);
    List<ScaDependencyDto> results = scaDependenciesDao.selectByQuery(db.getSession(), scaDependenciesQuery, Pagination.all());

    assertThat(results).hasSize(1);
    assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(scaDependencyDto);
  }

  @Test
  void selectByQuery_shouldReturnPaginatedScaDependencies() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaDependencyDto scaDependencyDto1 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "1", true, PackageManager.MAVEN, "foo.bar");
    ScaDependencyDto scaDependencyDto2 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "2", true, PackageManager.MAVEN, "foo.bar");
    ScaDependencyDto scaDependencyDto3 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "3", true, PackageManager.MAVEN, "foo.bar");
    ScaDependencyDto scaDependencyDto4 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "4", true, PackageManager.MAVEN, "foo.bar");

    ScaDependenciesQuery scaDependenciesQuery = new ScaDependenciesQuery(componentDto.branchUuid(), null, null, null);
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
  void selectByQuery_shouldPartiallyMatchPackageName_whenQueriedByText() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaDependencyDto scaDependencyDto1 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "1", true, PackageManager.MAVEN, "foo.bar");
    @SuppressWarnings("unused")
    ScaDependencyDto scaDependencyDto2 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "2", true, PackageManager.MAVEN, "bar.mee");
    ScaDependencyDto scaDependencyDto3 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "3", true, PackageManager.MAVEN, "foo.bar.me");
    @SuppressWarnings("unused")
    ScaDependencyDto scaDependencyDto4 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "4", true, PackageManager.MAVEN, "some.foo.bar");

    ScaDependenciesQuery scaDependenciesQuery = new ScaDependenciesQuery(componentDto.branchUuid(), null, null, "foo.bar");
    List<ScaDependencyDto> results = scaDependenciesDao.selectByQuery(db.getSession(), scaDependenciesQuery, Pagination.all());

    assertThat(results).hasSize(2);
    assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(scaDependencyDto1);
    assertThat(results.get(1)).usingRecursiveComparison().isEqualTo(scaDependencyDto3);

    ScaDependenciesQuery scaDependenciesCaseInsensitiveQuery = new ScaDependenciesQuery(componentDto.branchUuid(), null, null, "Foo.Bar");
    List<ScaDependencyDto> resultsCaseInsensitive = scaDependenciesDao.selectByQuery(db.getSession(), scaDependenciesCaseInsensitiveQuery, Pagination.all());

    assertThat(resultsCaseInsensitive).hasSize(2);
    assertThat(resultsCaseInsensitive.get(0)).usingRecursiveComparison().isEqualTo(scaDependencyDto1);
    assertThat(resultsCaseInsensitive.get(1)).usingRecursiveComparison().isEqualTo(scaDependencyDto3);
  }

  @Test
  void selectByQuery_shouldReturnScaDependencies_whenQueryByDirect() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaDependencyDto scaDependencyDto1 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "1", true, PackageManager.MAVEN, "foo.bar");
    ScaDependencyDto scaDependencyDto2 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "2", false, PackageManager.MAVEN, "foo.bar");

    ScaDependenciesQuery scaDependenciesDirectQuery = new ScaDependenciesQuery(componentDto.branchUuid(), true, null, null);
    List<ScaDependencyDto> resultsDirect = scaDependenciesDao.selectByQuery(db.getSession(), scaDependenciesDirectQuery, Pagination.all());

    assertThat(resultsDirect).hasSize(1);
    assertThat(resultsDirect.get(0)).usingRecursiveComparison().isEqualTo(scaDependencyDto1);

    ScaDependenciesQuery scaDependenciesNoDirectQuery = new ScaDependenciesQuery(componentDto.branchUuid(), false, null, null);
    List<ScaDependencyDto> resultsNoDirect = scaDependenciesDao.selectByQuery(db.getSession(), scaDependenciesNoDirectQuery, Pagination.all());

    assertThat(resultsNoDirect).hasSize(1);
    assertThat(resultsNoDirect.get(0)).usingRecursiveComparison().isEqualTo(scaDependencyDto2);
  }

  @Test
  void selectByQuery_shouldReturnScaDependencies_whenQueryByPackageManager() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaDependencyDto scaDependencyDto1 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "1", true, PackageManager.MAVEN, "foo.bar");
    ScaDependencyDto scaDependencyDto2 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "2", true, PackageManager.NPM, "foo.bar");
    ScaDependencyDto scaDependencyDto3 = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "3", true, PackageManager.CARGO, "foo.bar");

    ScaDependenciesQuery scaDependenciesMavenQuery = new ScaDependenciesQuery(componentDto.branchUuid(), null, List.of(PackageManager.MAVEN.name()), null);
    List<ScaDependencyDto> resultsMaven = scaDependenciesDao.selectByQuery(db.getSession(), scaDependenciesMavenQuery, Pagination.all());

    assertThat(resultsMaven).hasSize(1);
    assertThat(resultsMaven.get(0)).usingRecursiveComparison().isEqualTo(scaDependencyDto1);

    ScaDependenciesQuery scaDependenciesNpmAndCargoQuery = new ScaDependenciesQuery(componentDto.branchUuid(), null,
      List.of(PackageManager.NPM.name(), PackageManager.CARGO.name()), null);
    List<ScaDependencyDto> resultsNpm = scaDependenciesDao.selectByQuery(db.getSession(), scaDependenciesNpmAndCargoQuery, Pagination.all());

    assertThat(resultsNpm).hasSize(2);
    assertThat(resultsNpm.get(0)).usingRecursiveComparison().isEqualTo(scaDependencyDto2);
    assertThat(resultsNpm.get(1)).usingRecursiveComparison().isEqualTo(scaDependencyDto3);
  }

  @Test
  void update_shouldUpdateScaDependency() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaDependencyDto scaDependencyDto = db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), "1", true, PackageManager.MAVEN, "foo.bar");
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
    ComponentDto componentDto1 = prepareComponentDto("1");
    db.getScaDependenciesDbTester().insertScaDependency(componentDto1.uuid(), "1", true, PackageManager.MAVEN, "foo.bar");
    db.getScaDependenciesDbTester().insertScaDependency(componentDto1.uuid(), "2", true, PackageManager.MAVEN, "foo.bar.mee");
    db.getScaDependenciesDbTester().insertScaDependency(componentDto1.uuid(), "3", true, PackageManager.MAVEN, "bar.foo");

    ScaDependenciesQuery scaDependenciesQuery = new ScaDependenciesQuery(componentDto1.branchUuid(), null, null, "foo");

    assertThat(scaDependenciesDao.countByQuery(db.getSession(), scaDependenciesQuery)).isEqualTo(2);
    assertThat(scaDependenciesDao.countByQuery(db.getSession(), new ScaDependenciesQuery(componentDto1.branchUuid(), null, null, null))).isEqualTo(3);
    assertThat(scaDependenciesDao.countByQuery(db.getSession(), new ScaDependenciesQuery("another_branch_uuid", null, null, null))).isZero();
  }

  private ComponentDto prepareComponentDto(String suffix) {
    ProjectData projectData = db.components().insertPublicProject();
    return db.getScaDependenciesDbTester().insertComponent(projectData.mainBranchUuid(), suffix);
  }
}
