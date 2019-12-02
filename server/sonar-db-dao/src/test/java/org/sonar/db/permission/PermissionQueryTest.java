/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.component.ComponentTesting.newPublicProjectDto;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;

public class PermissionQueryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_query() {
    OrganizationDto organization = newOrganizationDto();
    ComponentDto project= newPublicProjectDto(organization);
    PermissionQuery query = PermissionQuery.builder()
      .setComponent(project)
      .setOrganizationUuid("ORGANIZATION_UUID")
      .setPermission("user")
      .setSearchQuery("sonar")
      .build();

    assertThat(query.getComponentUuid()).isEqualTo(project.uuid());
    assertThat(query.getComponentId()).isEqualTo(project.getId());
    assertThat(query.getOrganizationUuid()).isEqualTo("ORGANIZATION_UUID");
    assertThat(query.getPermission()).isEqualTo("user");
    assertThat(query.getSearchQuery()).isEqualTo("sonar");
  }

  @Test
  public void create_query_with_pagination() {
    PermissionQuery query = PermissionQuery.builder()
      .setOrganizationUuid("ORGANIZATION_UUID")
      .setPageSize(10)
      .setPageIndex(5)
      .build();

    assertThat(query.getPageOffset()).isEqualTo(40);
    assertThat(query.getPageSize()).isEqualTo(10);
  }

  @Test
  public void create_query_with_default_pagination() {
    PermissionQuery query = PermissionQuery.builder()
      .setOrganizationUuid("ORGANIZATION_UUID")
      .build();

    assertThat(query.getPageOffset()).isEqualTo(0);
    assertThat(query.getPageSize()).isEqualTo(20);
  }

  @Test
  public void fail_when_no_organization() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Organization UUID cannot be null");

    PermissionQuery.builder().setOrganizationUuid(null).build();
  }

  @Test
  public void fail_when_search_query_length_is_less_than_3_characters() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Search query should contains at least 3 characters");

    PermissionQuery.builder()
      .setOrganizationUuid("ORGANIZATION_UUID")
      .setSearchQuery("so")
      .build();
  }
}
