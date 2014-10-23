/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.issue.db;

import org.junit.Test;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class IssueAuthorizationDtoTest {

  @Test
  public void getter_and_setter() throws Exception {
    IssueAuthorizationDto dto = new IssueAuthorizationDto()
      .setProjectUuid("Sample")
      .setPermission("user")
      .setGroups(newArrayList("sonar-users"))
      .setUsers(newArrayList("john"));

    assertThat(dto.getKey()).isEqualTo("Sample");
    assertThat(dto.getProjectUuid()).isEqualTo("Sample");
    assertThat(dto.getPermission()).isEqualTo("user");
    assertThat(dto.getGroups()).containsExactly("sonar-users");
    assertThat(dto.getUsers()).containsExactly("john");
  }

  @Test
  public void add_group() throws Exception {
    IssueAuthorizationDto dto = new IssueAuthorizationDto()
      .setProjectUuid("Sample")
      .setPermission("user")
      .setGroups(newArrayList("sonar-users"))
      .setUsers(newArrayList("john"));

    assertThat(dto.getGroups()).containsExactly("sonar-users");

    dto.addGroup("sonar-admins");

    assertThat(dto.getGroups()).containsExactly("sonar-users", "sonar-admins");
  }

  @Test
  public void add_user() throws Exception {
    IssueAuthorizationDto dto = new IssueAuthorizationDto()
      .setProjectUuid("Sample")
      .setPermission("user")
      .setGroups(newArrayList("sonar-users"))
      .setUsers(newArrayList("john"));

    assertThat(dto.getUsers()).containsExactly("john");

    dto.addUser("doe");

    assertThat(dto.getUsers()).containsExactly("john", "doe");
  }
}
