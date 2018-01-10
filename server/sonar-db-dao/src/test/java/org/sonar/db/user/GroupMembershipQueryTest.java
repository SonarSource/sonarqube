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
package org.sonar.db.user;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class GroupMembershipQueryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void create_query() {
    GroupMembershipQuery underTest = GroupMembershipQuery.builder()
      .groupSearch("sonar-users")
      .membership(GroupMembershipQuery.IN)
      .pageIndex(2)
      .pageSize(10)
      .organizationUuid("organization_uuid")
      .build();

    assertThat(underTest.groupSearch()).isEqualTo("sonar-users");
    assertThat(underTest.membership()).isEqualTo("IN");
    assertThat(underTest.pageIndex()).isEqualTo(2);
    assertThat(underTest.pageSize()).isEqualTo(10);
    assertThat(underTest.organizationUuid()).isEqualTo("organization_uuid");
  }

  @Test
  public void fail_on_null_organization() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Organization uuid cant be null");

    GroupMembershipQuery.builder()
      .organizationUuid(null)
      .build();
  }

  @Test
  public void fail_on_invalid_membership() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Membership is not valid (got unknwown). Availables values are [ANY, IN, OUT]");

    GroupMembershipQuery.builder()
      .organizationUuid("organization_uuid")
      .membership("unknwown")
      .build();
  }

}
