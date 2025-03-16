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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;

class ScaReleasesDependenciesDaoIT {
  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final ScaReleasesDependenciesDao scaReleasesDependenciesDao = db.getDbClient().scaReleasesDependenciesDao();

  @Test
  void test_whenEmptyDatabaseAndQuery_selectByReleaseUuids() {
    assertThat(scaReleasesDependenciesDao.selectByReleaseUuids(db.getSession(), List.of())).isEmpty();
  }

  @Test
  void test_whenSomeDependencies_selectByReleaseUuids() {
    ComponentDto componentDto = db.components().insertPublicProject().getMainBranchComponent();

    ScaDependencyDto scaDependencyDto1a = db.getScaDependenciesDbTester().insertScaDependencyWithRelease(componentDto.uuid(), "1a", true, PackageManager.MAVEN, "foo.bar1");
    // same release, different dependency
    ScaDependencyDto scaDependencyDto1b = db.getScaDependenciesDbTester().insertScaDependency(scaDependencyDto1a.scaReleaseUuid(), "1b", false);
    ScaDependencyDto scaDependencyDto2 = db.getScaDependenciesDbTester().insertScaDependencyWithRelease(componentDto.uuid(), "2", true, PackageManager.MAVEN, "foo.bar2");
    ScaDependencyDto scaDependencyDto3 = db.getScaDependenciesDbTester().insertScaDependencyWithRelease(componentDto.uuid(), "3", true, PackageManager.MAVEN, "foo.bar3");

    List<ScaReleaseDependenciesDto> results = scaReleasesDependenciesDao.selectByReleaseUuids(db.getSession(),
      List.of(scaDependencyDto1a.scaReleaseUuid(), scaDependencyDto2.scaReleaseUuid()));

    assertThat(results.stream().map(ScaReleaseDependenciesDto::dependencies).flatMap(List::stream).toList())
      .containsExactlyInAnyOrder(scaDependencyDto1a, scaDependencyDto1b, scaDependencyDto2)
      .doesNotContain(scaDependencyDto3);
    var twoDeps = results.stream().filter(rd -> rd.releaseUuid().equals(scaDependencyDto1a.scaReleaseUuid()))
      .findFirst().map(ScaReleaseDependenciesDto::dependencies).orElseThrow();
    assertThat(twoDeps).containsExactlyInAnyOrder(scaDependencyDto1a, scaDependencyDto1b);

    var resultsWithEmptyQuery = scaReleasesDependenciesDao.selectByReleaseUuids(db.getSession(), List.of());
    assertThat(resultsWithEmptyQuery).isEmpty();
  }
}
