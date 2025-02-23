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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.Pagination;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;

import static org.assertj.core.api.Assertions.assertThat;

class ScaReleasesDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final ScaReleasesDao scaReleasesDao = db.getDbClient().scaReleasesDao();

  @Test
  void insert_shouldPersistScaReleases() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaReleaseDto scaReleaseDto = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "1");

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from sca_releases");
    assertThat(select).hasSize(1);
    Map<String, Object> stringObjectMap = select.get(0);
    assertThat(stringObjectMap).containsExactlyInAnyOrderEntriesOf(
      Map.ofEntries(
        Map.entry("uuid", scaReleaseDto.uuid()),
        Map.entry("component_uuid", scaReleaseDto.componentUuid()),
        Map.entry("package_url", scaReleaseDto.packageUrl()),
        Map.entry("package_manager", scaReleaseDto.packageManager().name()),
        Map.entry("package_name", scaReleaseDto.packageName()),
        Map.entry("version", scaReleaseDto.version()),
        Map.entry("license_expression", scaReleaseDto.licenseExpression()),
        Map.entry("known", scaReleaseDto.known()),
        Map.entry("created_at", scaReleaseDto.createdAt()),
        Map.entry("updated_at", scaReleaseDto.updatedAt())));
  }

  @Test
  void deleteByUuid_shouldDeleteScaReleases() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaReleaseDto scaReleaseDto = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "1");

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from sca_releases");
    assertThat(select).isNotEmpty();

    scaReleasesDao.deleteByUuid(db.getSession(), scaReleaseDto.uuid());

    select = db.select(db.getSession(), "select * from sca_releases");
    assertThat(select).isEmpty();
  }

  @Test
  void selectByUuid_shouldLoadScaRelease() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaReleaseDto scaReleaseDto = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "1");

    var loadedOptional = scaReleasesDao.selectByUuid(db.getSession(), scaReleaseDto.uuid());

    assertThat(loadedOptional).contains(scaReleaseDto);
  }

  @Test
  void selectByUuid_shouldLoadScaReleases() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaReleaseDto scaReleaseDto1 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "1");
    db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "2");
    ScaReleaseDto scaReleaseDto3 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "3");

    // we don't ask for the second one, so this tests we only get what we asked for.
    var loaded = scaReleasesDao.selectByUuids(db.getSession(), Set.of(scaReleaseDto1.uuid(), scaReleaseDto3.uuid()));

    assertThat(loaded).containsExactlyInAnyOrder(scaReleaseDto1, scaReleaseDto3);
  }

  @Test
  void selectByUuid_shouldLoadEmptyScaReleases() {
    ComponentDto componentDto = prepareComponentDto("1");
    db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "1");
    db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "2");
    db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "3");

    var loaded = scaReleasesDao.selectByUuids(db.getSession(), Collections.emptyList());

    assertThat(loaded).isEmpty();
  }

  @Test
  void selectByQuery_shouldReturnScaReleases_whenQueryByBranchUuid() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaReleaseDto scaReleaseDto = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "1");

    ScaReleasesQuery scaReleasesQuery = new ScaReleasesQuery(componentDto.branchUuid(), null, null, null);
    List<ScaReleaseDto> results = scaReleasesDao.selectByQuery(db.getSession(), scaReleasesQuery, Pagination.all());

    assertThat(results).hasSize(1);
    assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(scaReleaseDto);
  }

  @Test
  void selectByQuery_shouldReturnPaginatedScaReleases() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaReleaseDto scaReleaseDto1 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "1");
    ScaReleaseDto scaReleaseDto2 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "2");
    ScaReleaseDto scaReleaseDto3 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "3");
    ScaReleaseDto scaReleaseDto4 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "4");

    ScaReleasesQuery scaReleasesQuery = new ScaReleasesQuery(componentDto.branchUuid(), null, null, null);
    List<ScaReleaseDto> page1Results = scaReleasesDao.selectByQuery(db.getSession(), scaReleasesQuery, Pagination.forPage(1).andSize(2));
    List<ScaReleaseDto> page2Results = scaReleasesDao.selectByQuery(db.getSession(), scaReleasesQuery, Pagination.forPage(2).andSize(2));

    // we order by created_at so it would seem we can assert the order here... except that created_at has finite resolution, so it can be nondeterministic.
    var allResults = new ArrayList<>(page1Results);
    allResults.addAll(page2Results);
    assertThat(allResults).containsExactlyInAnyOrder(scaReleaseDto1, scaReleaseDto2, scaReleaseDto3, scaReleaseDto4);
    assertThat(List.of(page1Results.size(), page2Results.size())).containsExactly(2, 2);
  }

  @Test
  void selectByQuery_shouldPartiallyMatchPackageName_whenQueriedByText() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaReleaseDto scaReleaseDto1 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "1", PackageManager.MAVEN, "foo.bar");
    db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), scaReleaseDto1, "1", true);
    db.getScaDependenciesDbTester().insertScaDependency(componentDto.uuid(), scaReleaseDto1, "2", false);
    var log = LoggerFactory.getLogger("");
    List<Map<String, Object>> temp = db.select(db.getSession(), "select * from sca_releases");
    log.warn("sca_releases: {}", temp.stream().count());
    for (Map<String, Object> map : temp) {
      log.warn(map.toString());
    }
    temp = db.select(db.getSession(), "select * from sca_dependencies");
    log.warn("sca_dependencies: {}", temp.stream().count());
    for (Map<String, Object> map : temp) {
      log.warn(map.toString());
    }

    @SuppressWarnings("unused")
    ScaReleaseDto scaReleaseDto2 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "2", PackageManager.MAVEN, "bar.mee");
    ScaReleaseDto scaReleaseDto3 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "3", PackageManager.MAVEN, "foo.bar.me");
    @SuppressWarnings("unused")
    ScaReleaseDto scaReleaseDto4 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "4", PackageManager.MAVEN, "some.foo.bar");

    ScaReleasesQuery scaReleasesQuery = new ScaReleasesQuery(componentDto.branchUuid(), null, null, "foo.bar");
    List<ScaReleaseDto> results = scaReleasesDao.selectByQuery(db.getSession(), scaReleasesQuery, Pagination.all());

    assertThat(results).hasSize(2);
    assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(scaReleaseDto1);
    assertThat(results.get(1)).usingRecursiveComparison().isEqualTo(scaReleaseDto3);

    ScaReleasesQuery scaReleasesCaseInsensitiveQuery = new ScaReleasesQuery(componentDto.branchUuid(), null, null, "Foo.Bar");
    List<ScaReleaseDto> resultsCaseInsensitive = scaReleasesDao.selectByQuery(db.getSession(), scaReleasesCaseInsensitiveQuery, Pagination.all());

    assertThat(resultsCaseInsensitive).hasSize(2);
    assertThat(resultsCaseInsensitive.get(0)).usingRecursiveComparison().isEqualTo(scaReleaseDto1);
    assertThat(resultsCaseInsensitive.get(1)).usingRecursiveComparison().isEqualTo(scaReleaseDto3);
  }

  @Test
  void selectByQuery_shouldReturnScaReleases_whenQueryByDirect() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaReleaseDto scaReleaseDto1 = db.getScaReleasesDbTester().insertScaReleaseWithDependency(componentDto.uuid(), "1", 2, true, PackageManager.MAVEN, "foo.bar");
    ScaReleaseDto scaReleaseDto2 = db.getScaReleasesDbTester().insertScaReleaseWithDependency(componentDto.uuid(), "2", 3, false, PackageManager.MAVEN, "foo.bar");

    ScaReleasesQuery scaReleasesDirectQuery = new ScaReleasesQuery(componentDto.branchUuid(), true, null, null);
    List<ScaReleaseDto> resultsDirect = scaReleasesDao.selectByQuery(db.getSession(), scaReleasesDirectQuery, Pagination.all());

    assertThat(resultsDirect).hasSize(1);
    assertThat(resultsDirect.get(0)).usingRecursiveComparison().isEqualTo(scaReleaseDto1);

    ScaReleasesQuery scaReleasesNoDirectQuery = new ScaReleasesQuery(componentDto.branchUuid(), false, null, null);
    List<ScaReleaseDto> resultsNoDirect = scaReleasesDao.selectByQuery(db.getSession(), scaReleasesNoDirectQuery, Pagination.all());

    assertThat(resultsNoDirect).hasSize(1);
    assertThat(resultsNoDirect.get(0)).usingRecursiveComparison().isEqualTo(scaReleaseDto2);
  }

  @Test
  void selectByQuery_shouldReturnScaReleases_whenQueryByPackageManager() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaReleaseDto scaReleaseDto1 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "1", PackageManager.MAVEN, "foo.bar");
    ScaReleaseDto scaReleaseDto2 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "2", PackageManager.NPM, "foo.bar");
    ScaReleaseDto scaReleaseDto3 = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "3", PackageManager.CARGO, "foo.bar");

    ScaReleasesQuery scaReleasesMavenQuery = new ScaReleasesQuery(componentDto.branchUuid(), null, List.of(PackageManager.MAVEN.name()), null);
    List<ScaReleaseDto> resultsMaven = scaReleasesDao.selectByQuery(db.getSession(), scaReleasesMavenQuery, Pagination.all());

    assertThat(resultsMaven).hasSize(1);
    assertThat(resultsMaven.get(0)).usingRecursiveComparison().isEqualTo(scaReleaseDto1);

    ScaReleasesQuery scaReleasesNpmAndCargoQuery = new ScaReleasesQuery(componentDto.branchUuid(), null,
      List.of(PackageManager.NPM.name(), PackageManager.CARGO.name()), null);
    List<ScaReleaseDto> resultsNpm = scaReleasesDao.selectByQuery(db.getSession(), scaReleasesNpmAndCargoQuery, Pagination.all());

    assertThat(resultsNpm).hasSize(2);
    assertThat(resultsNpm.get(0)).usingRecursiveComparison().isEqualTo(scaReleaseDto2);
    assertThat(resultsNpm.get(1)).usingRecursiveComparison().isEqualTo(scaReleaseDto3);
  }

  @Test
  void update_shouldUpdateScaRelease() {
    ComponentDto componentDto = prepareComponentDto("1");
    ScaReleaseDto scaReleaseDto = db.getScaReleasesDbTester().insertScaRelease(componentDto.uuid(), "1", PackageManager.MAVEN, "foo.bar");
    ScaReleaseDto updatedScaRelease = scaReleaseDto.toBuilder().setUpdatedAt(scaReleaseDto.updatedAt() + 1).setVersion("newVersion").build();

    scaReleasesDao.update(db.getSession(), updatedScaRelease);

    List<Map<String, Object>> select = db.select(db.getSession(), "select * from sca_releases");
    assertThat(select).hasSize(1);
    Map<String, Object> stringObjectMap = select.get(0);
    assertThat(stringObjectMap).containsExactlyInAnyOrderEntriesOf(
      Map.ofEntries(
        Map.entry("uuid", updatedScaRelease.uuid()),
        Map.entry("component_uuid", updatedScaRelease.componentUuid()),
        Map.entry("package_url", updatedScaRelease.packageUrl()),
        Map.entry("package_manager", updatedScaRelease.packageManager().name()),
        Map.entry("package_name", updatedScaRelease.packageName()),
        Map.entry("version", updatedScaRelease.version()),
        Map.entry("license_expression", updatedScaRelease.licenseExpression()),
        Map.entry("known", updatedScaRelease.known()),
        Map.entry("created_at", updatedScaRelease.createdAt()),
        Map.entry("updated_at", updatedScaRelease.updatedAt())));
  }

  @Test
  void countByQuery_shouldReturnTheTotalOfReleases() {
    ComponentDto componentDto1 = prepareComponentDto("1");
    db.getScaReleasesDbTester().insertScaReleaseWithDependency(componentDto1.uuid(), "1", 1, true, PackageManager.MAVEN, "foo.bar");
    db.getScaReleasesDbTester().insertScaReleaseWithDependency(componentDto1.uuid(), "2", 2, true, PackageManager.MAVEN, "foo.bar.mee");
    db.getScaReleasesDbTester().insertScaReleaseWithDependency(componentDto1.uuid(), "3", 3, true, PackageManager.MAVEN, "bar.foo");

    ScaReleasesQuery scaReleasesQuery = new ScaReleasesQuery(componentDto1.branchUuid(), null, null, "foo");

    assertThat(scaReleasesDao.countByQuery(db.getSession(), scaReleasesQuery)).isEqualTo(2);
    assertThat(scaReleasesDao.countByQuery(db.getSession(), new ScaReleasesQuery(componentDto1.branchUuid(), null, null, null))).isEqualTo(3);
    assertThat(scaReleasesDao.countByQuery(db.getSession(), new ScaReleasesQuery("another_branch_uuid", null, null, null))).isZero();
  }

  private ComponentDto prepareComponentDto(String suffix) {
    ProjectData projectData = db.components().insertPublicProject();
    return db.getScaReleasesDbTester().insertComponent(projectData.mainBranchUuid(), suffix);
  }
}
