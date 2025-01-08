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
package org.sonar.db.permission;

import org.junit.jupiter.api.Test;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentTesting.newPublicProjectDto;

class PermissionQueryTest {

  @Test
  void create_query() {
    ComponentDto project = newPublicProjectDto();
    PermissionQuery query = PermissionQuery.builder()
      .setEntity(project)
      .setPermission("user")
      .setSearchQuery("sonar")
      .build();

    assertThat(query.getEntityUuid()).isEqualTo(project.uuid());
    assertThat(query.getPermission()).isEqualTo("user");
    assertThat(query.getSearchQuery()).isEqualTo("sonar");
  }

  @Test
  void create_query_with_pagination() {
    PermissionQuery query = PermissionQuery.builder()
      .setPageSize(10)
      .setPageIndex(5)
      .build();

    assertThat(query.getPageOffset()).isEqualTo(40);
    assertThat(query.getPageSize()).isEqualTo(10);
  }

  @Test
  void create_query_with_default_pagination() {
    PermissionQuery query = PermissionQuery.builder()
      .build();

    assertThat(query.getPageOffset()).isZero();
    assertThat(query.getPageSize()).isEqualTo(20);
  }

  @Test
  void fail_when_search_query_length_is_less_than_3_characters() {
    assertThatThrownBy(() -> {
      PermissionQuery.builder()
        .setSearchQuery("so")
        .build();
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Search query should contains at least 3 characters");
  }
}
