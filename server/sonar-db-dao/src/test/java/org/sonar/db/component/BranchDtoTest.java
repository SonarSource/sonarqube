/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BranchDtoTest {

  private BranchDto underTest = new BranchDto();

  @Test
  public void isMain_is_true_if_branch_uuid_equals_project_uuid() {
    underTest.setProjectUuid("U1");
    underTest.setUuid("U1");

    assertThat(underTest.isMain()).isTrue();
  }

  @Test
  public void isMain_is_false_if_branch_uuid_does_not_equal_project_uuid() {
    underTest.setProjectUuid("U1");
    underTest.setUuid("U2");

    assertThat(underTest.isMain()).isFalse();
  }
}
