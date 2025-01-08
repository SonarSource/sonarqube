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
package org.sonar.db.measure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LargestBranchNclocDtoTest {

  private final LargestBranchNclocDto underTest = new LargestBranchNclocDto();

  @Test
  void test_getter_and_setter() {
    setUnderTest();

    assertThat(underTest.getProjectUuid()).isEqualTo("projectUuid");
    assertThat(underTest.getProjectName()).isEqualTo("projectName");
    assertThat(underTest.getProjectKey()).isEqualTo("projectKey");
    assertThat(underTest.getLoc()).isEqualTo(123L);
    assertThat(underTest.getBranchName()).isEqualTo("branchName");
    assertThat(underTest.getBranchType()).isEqualTo("branchType");
  }

  private void setUnderTest() {
    underTest
      .setProjectUuid("projectUuid")
      .setProjectName("projectName")
      .setProjectKey("projectKey")
      .setLoc(123L)
      .setBranchName("branchName")
      .setBranchType("branchType");
  }

}
