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
package org.sonar.ce.common.sca;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.db.sca.PackageManager;
import org.sonar.db.sca.ScaDependencyDto;
import org.sonar.db.sca.ScaReleaseDto;

import static org.assertj.core.api.Assertions.assertThat;

class ScaHolderImplTest {
  @Test
  void test_setAndGetDependencies() {
    ScaHolderImpl scaHolderImpl = new ScaHolderImpl();
    var dep = newScaDependencyDto();
    Collection<ScaDependencyDto> dependencies = List.of(dep);
    scaHolderImpl.setDependencies(dependencies);
    List<ScaDependencyDto> result = scaHolderImpl.getDependencies();
    assertThat(result).containsExactly(dep);
  }

  @Test
  void test_setAndGetReleases() {
    ScaHolderImpl scaHolderImpl = new ScaHolderImpl();
    var release = newScaReleaseDto();
    Collection<ScaReleaseDto> releases = List.of(release);
    scaHolderImpl.setReleases(releases);
    List<ScaReleaseDto> result = scaHolderImpl.getReleases();
    assertThat(result).containsExactly(release);
  }

  @Test
  void test_dependencyAnalysisPresent() {
    ScaHolderImpl scaHolderImpl = new ScaHolderImpl();
    assertThat(scaHolderImpl.dependencyAnalysisPresent()).isFalse();
    var dep = newScaDependencyDto();
    var release = newScaReleaseDto();
    Collection<ScaDependencyDto> dependencies = List.of(dep);
    Collection<ScaReleaseDto> releases = List.of(release);
    scaHolderImpl.setDependencies(dependencies);
    assertThat(scaHolderImpl.dependencyAnalysisPresent()).isFalse();
    scaHolderImpl.setReleases(releases);
    assertThat(scaHolderImpl.dependencyAnalysisPresent()).isTrue();
  }

  private static ScaDependencyDto newScaDependencyDto() {
    return new ScaDependencyDto("scaDependencyUuid",
      "scaReleaseUuid",
        true,
        "compile",
        "some/path",
        "another/path",
        List.of(List.of("pkg:npm/foo@1.0.0")),
        1L,
        2L);
  }

  private static ScaReleaseDto newScaReleaseDto() {
    return new ScaReleaseDto("scaReleaseUuid",
      "componentUuid",
      "packageUrl",
      PackageManager.MAVEN,
      "foo:bar",
      "1.0.0",
      "MIT",
      true,
      1L,
      2L);
  }
}
