/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.user;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.server.exceptions.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Use BbUnit tests because there's no IT on this feature for the moment
 */

public class GroupMembershipServiceTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  GroupMembershipService service = new GroupMembershipService(new GroupMembershipFinder(dbTester.getDbClient().userDao(), dbTester.getDbClient().groupMembershipDao()));

  @Test
  public void find_all_member_groups() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    GroupMembershipFinder.Membership queryResult = service.find(ImmutableMap.<String, Object>of(
      "user", "user1",
      "selected", "all"));
    List<GroupMembership> result = queryResult.groups();
    assertThat(result).hasSize(3);
    check(result.get(0), "sonar-administrators", false);
    check(result.get(1), "sonar-reviewers", false);
    check(result.get(2), "sonar-users", true);
  }

  @Test
  public void find_all_member_groups_when_no_selected_parameter() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    GroupMembershipFinder.Membership queryResult = service.find(ImmutableMap.<String, Object>of(
      "user", "user1"));
    List<GroupMembership> result = queryResult.groups();
    assertThat(result).hasSize(3);
    check(result.get(0), "sonar-administrators", false);
    check(result.get(1), "sonar-reviewers", false);
    check(result.get(2), "sonar-users", true);
  }

  @Test
  public void find_member_groups() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    GroupMembershipFinder.Membership queryResult = service.find(ImmutableMap.<String, Object>of(
      "user", "user1",
      "selected", "selected"));
    List<GroupMembership> result = queryResult.groups();
    assertThat(result).hasSize(1);
    check(result.get(0), "sonar-users", true);
  }

  @Test
  public void find_not_member_groups() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    GroupMembershipFinder.Membership queryResult = service.find(ImmutableMap.<String, Object>of(
      "user", "user1",
      "selected", "deselected"));
    List<GroupMembership> result = queryResult.groups();
    assertThat(result).hasSize(2);
    check(result.get(0), "sonar-administrators", false);
    check(result.get(1), "sonar-reviewers", false);
  }

  @Test
  public void find_with_paging_with_more_results() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    GroupMembershipFinder.Membership queryResult = service.find(ImmutableMap.<String, Object>of(
      "user", "user1",
      "selected", "all",
      "page", 1,
      "pageSize", 2
    ));
    List<GroupMembership> result = queryResult.groups();
    assertThat(result).hasSize(2);
    assertThat(queryResult.hasMoreResults()).isTrue();
  }

  @Test
  public void find_with_paging_with_no_more_results() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    GroupMembershipFinder.Membership queryResult = service.find(ImmutableMap.<String, Object>of(
      "user", "user1",
      "selected", "all",
      "page", 3,
      "pageSize", 1
    ));
    List<GroupMembership> result = queryResult.groups();
    assertThat(result).hasSize(1);
    assertThat(queryResult.hasMoreResults()).isFalse();
  }

  @Test
  public void fail_if_user_not_found() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    try {
      service.find(ImmutableMap.<String, Object>of(
        "user", "user_not_existing",
        "selected", "all"));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("User 'user_not_existing' does not exists.");
    }
  }

  @Test
  public void find_matched_groups_name() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    GroupMembershipFinder.Membership queryResult = service.find(ImmutableMap.<String, Object>of(
      "user", "user1",
      "selected", "all",
      "query", "user"));
    List<GroupMembership> result = queryResult.groups();
    assertThat(result).hasSize(1);
    check(result.get(0), "sonar-users", true);

    queryResult = service.find(ImmutableMap.<String, Object>of(
      "user", "user1",
      "selected", "all",
      "query", "sonar"));
    result = queryResult.groups();
    assertThat(result).hasSize(3);
  }

  private void check(GroupMembership groupMembership, String expectedName, boolean isMember) {
    assertThat(groupMembership.name()).isEqualTo(expectedName);
    assertThat(groupMembership.isMember()).isEqualTo(isMember);
  }
}
