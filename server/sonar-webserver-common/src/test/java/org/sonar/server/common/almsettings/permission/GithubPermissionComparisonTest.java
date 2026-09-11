/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.common.almsettings.permission;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class GithubPermissionComparisonTest {

  private static final Map<String, String> REQUIRED = Map.of("contents", "write", "metadata", "read");

  @ParameterizedTest
  @MethodSource("grantedLevels")
  void findDeficits_comparesAccessLevelsRatherThanRequiringAnExactMatch(String grantedContents, boolean expectedDeficit) {
    Map<String, String> granted = Map.of("contents", grantedContents, "metadata", "read");

    List<PermissionDeficit> deficits = GithubPermissionComparison.findDeficits(REQUIRED, granted);

    assertThat(deficits).hasSize(expectedDeficit ? 1 : 0);
  }

  private static List<Arguments> grantedLevels() {
    return List.of(
      Arguments.of("write", false),
      Arguments.of("admin", false),
      Arguments.of("WRITE", false),
      Arguments.of("read", true),
      Arguments.of("none", true),
      // A level we do not know how to rank is worth nothing: better a deficit an administrator can look at than a
      // check we silently pass without being able to make it.
      Arguments.of("something-new", true));
  }

  @Test
  void findDeficits_whenPermissionIsAbsentEntirely_reportsItWithNoGrantedLevel() {
    List<PermissionDeficit> deficits = GithubPermissionComparison.findDeficits(REQUIRED, Map.of("metadata", "read"));

    assertThat(deficits)
      .extracting(PermissionDeficit::permission, PermissionDeficit::required, PermissionDeficit::granted)
      .containsExactly(tuple("contents", "write", null));
  }

  @Test
  void findDeficits_whenAReadRequirementIsMetByWrite_reportsNothing() {
    assertThat(GithubPermissionComparison.findDeficits(Map.of("metadata", "read"), Map.of("metadata", "write"))).isEmpty();
  }

  @Test
  void findDeficits_ignoresGrantedPermissionsThatAreNotRequired() {
    Map<String, String> granted = Map.of("contents", "write", "metadata", "read", "members", "read", "emails", "read");

    assertThat(GithubPermissionComparison.findDeficits(REQUIRED, granted)).isEmpty();
  }

  @Test
  void findDeficits_whenNothingIsGranted_reportsEveryRequirementSortedByName() {
    List<PermissionDeficit> deficits = GithubPermissionComparison.findDeficits(REQUIRED, Map.of());

    assertThat(deficits).extracting(PermissionDeficit::permission).containsExactly("contents", "metadata");
  }
}
