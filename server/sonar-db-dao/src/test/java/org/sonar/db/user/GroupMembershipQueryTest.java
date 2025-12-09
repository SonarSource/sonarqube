/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupMembershipQueryTest {


  @Test
  void create_query() {
    GroupMembershipQuery underTest = GroupMembershipQuery.builder()
      .groupSearch("sonar-users")
      .membership(GroupMembershipQuery.IN)
      .pageIndex(2)
      .pageSize(10)
      .build();

    assertThat(underTest.groupSearch()).isEqualTo("sonar-users");
    assertThat(underTest.membership()).isEqualTo("IN");
    assertThat(underTest.pageIndex()).isEqualTo(2);
    assertThat(underTest.pageSize()).isEqualTo(10);
  }

  @Test
  void fail_on_invalid_membership() {
    assertThatThrownBy(() -> {
      GroupMembershipQuery.builder()
        .membership("unknwown")
        .build();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Membership is not valid (got unknwown). Availables values are [ANY, IN, OUT]");
  }
}
