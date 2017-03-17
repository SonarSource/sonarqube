/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonarqube.ws.client.user;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupsRequestTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  GroupsRequest.Builder underTest = GroupsRequest.builder();

  @Test
  public void create_request() {
    GroupsRequest result = underTest
      .setLogin("john")
      .setOrganization("orga-uuid")
      .setSelected("all")
      .setQuery("sonar-users")
      .setPage(10)
      .setPageSize(50)
      .build();

    assertThat(result.getLogin()).isEqualTo("john");
    assertThat(result.getOrganization()).isEqualTo("orga-uuid");
    assertThat(result.getSelected()).isEqualTo("all");
    assertThat(result.getQuery()).isEqualTo("sonar-users");
    assertThat(result.getPage()).isEqualTo(10);
    assertThat(result.getPageSize()).isEqualTo(50);
  }

  @Test
  public void create_request_wih_minimal_fields() {
    GroupsRequest result = underTest
      .setLogin("john")
      .build();

    assertThat(result.getLogin()).isEqualTo("john");
    assertThat(result.getOrganization()).isNull();
    assertThat(result.getSelected()).isNull();
    assertThat(result.getQuery()).isNull();
    assertThat(result.getPage()).isNull();
    assertThat(result.getPageSize()).isNull();
  }

  @Test
  public void fail_when_empty_login() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Login is mandatory and must not be empty");

    underTest.setLogin("").build();
  }

  @Test
  public void fail_when_null_login() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Login is mandatory and must not be empty");

    underTest.build();
  }

}
