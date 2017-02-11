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
package org.sonar.db.user;

import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class GroupMembershipDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private GroupMembershipDao underTest = dbTester.getDbClient().groupMembershipDao();

  @Test
  public void count_groups() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    // 200 is member of 3 groups
    assertThat(underTest.countGroups(dbTester.getSession(), GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.IN).build(), 200)).isEqualTo(3);
    assertThat(underTest.countGroups(dbTester.getSession(), GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.OUT).build(), 200)).isZero();
    // 201 is member of 1 group on 3
    assertThat(underTest.countGroups(dbTester.getSession(), GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.IN).build(), 201)).isEqualTo(1);
    assertThat(underTest.countGroups(dbTester.getSession(), GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.OUT).build(), 201)).isEqualTo(2);
    // 999 is member of 0 group
    assertThat(underTest.countGroups(dbTester.getSession(), GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.IN).build(), 999)).isZero();
    assertThat(underTest.countGroups(dbTester.getSession(), GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.OUT).build(), 2999)).isEqualTo(3);
  }

  @Test
  public void count_users_by_group() {
    dbTester.prepareDbUnit(getClass(), "shared_plus_empty_group.xml");

    assertThat(underTest.countUsersByGroups(dbTester.getSession(), Arrays.asList(100, 101, 102, 103))).containsOnly(
      entry("sonar-users", 2), entry("sonar-reviewers", 1), entry("sonar-administrators", 1), entry("sonar-nobody", 0));
    assertThat(underTest.countUsersByGroups(dbTester.getSession(), Arrays.asList(100, 103))).containsOnly(
      entry("sonar-administrators", 1), entry("sonar-nobody", 0));
  }

  @Test
  public void count_groups_by_login() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectGroupsByLogins(dbTester.getSession(), Arrays.<String>asList()).keys()).isEmpty();
    Multimap<String, String> groupsByLogin = underTest.selectGroupsByLogins(dbTester.getSession(), Arrays.asList("two-hundred", "two-hundred-one", "two-hundred-two"));
    assertThat(groupsByLogin.get("two-hundred")).containsExactly("sonar-administrators", "sonar-reviewers", "sonar-users");
    assertThat(groupsByLogin.get("two-hundred-one")).containsExactly("sonar-users");
    assertThat(groupsByLogin.get("two-hundred-two")).isEmpty();
  }

  @Test
  public void count_members() {
    dbTester.prepareDbUnit(getClass(), "shared_plus_empty_group.xml");

    // 100 has 1 member and 1 non member
    assertThat(underTest.countMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).membership(UserMembershipQuery.IN).build())).isEqualTo(1);
    assertThat(underTest.countMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).membership(UserMembershipQuery.OUT).build())).isEqualTo(1);
    // 101 has 2 members
    assertThat(underTest.countMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(101).membership(UserMembershipQuery.IN).build())).isEqualTo(2);
    assertThat(underTest.countMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(101).membership(UserMembershipQuery.OUT).build())).isZero();
    // 102 has 1 member and 1 non member
    assertThat(underTest.countMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(102).membership(UserMembershipQuery.IN).build())).isEqualTo(1);
    assertThat(underTest.countMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(102).membership(UserMembershipQuery.OUT).build())).isEqualTo(1);
    // 103 has no member
    assertThat(underTest.countMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(103).membership(UserMembershipQuery.IN).build())).isZero();
    assertThat(underTest.countMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(103).membership(UserMembershipQuery.OUT).build())).isEqualTo(2);
  }

  @Test
  public void select_group_members_by_query() {
    dbTester.prepareDbUnit(getClass(), "shared_plus_empty_group.xml");

    // 100 has 1 member
    assertThat(underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).membership(UserMembershipQuery.IN).build(), 0, 10)).hasSize(1);
    // 101 has 2 members
    assertThat(underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(101).membership(UserMembershipQuery.IN).build(), 0, 10)).hasSize(2);
    // 102 has 1 member
    assertThat(underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(102).membership(UserMembershipQuery.IN).build(), 0, 10)).hasSize(1);
    // 103 has no member
    assertThat(underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(103).membership(UserMembershipQuery.IN).build(), 0, 10)).isEmpty();
  }

  @Test
  public void select_users_not_affected_to_a_group_by_query() {
    dbTester.prepareDbUnit(getClass(), "shared_plus_empty_group.xml");

    // 100 has 1 member
    assertThat(underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).membership(UserMembershipQuery.OUT).build(), 0, 10)).hasSize(1);
    // 101 has 2 members
    assertThat(underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(101).membership(UserMembershipQuery.OUT).build(), 0, 10)).isEmpty();
    // 102 has 1 member
    assertThat(underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(102).membership(UserMembershipQuery.OUT).build(), 0, 10)).hasSize(1);
    // 103 has no member
    assertThat(underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(103).membership(UserMembershipQuery.OUT).build(), 0, 10)).hasSize(2);
  }

  @Test
  public void search_by_user_name_or_login() {
    dbTester.prepareDbUnit(getClass(), "shared_plus_empty_group.xml");

    List<UserMembershipDto> result = underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).memberSearch("admin").build(), 0, 10);
    assertThat(result).hasSize(2);

    assertThat(result.get(0).getName()).isEqualTo("Admin name");
    assertThat(result.get(1).getName()).isEqualTo("Not Admin");

    result = underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).memberSearch("not").build(), 0, 10);
    assertThat(result).hasSize(1);
  }

  @Test
  public void search_by_login_name_or_email() {
    dbTester.prepareDbUnit(getClass(), "shared_plus_empty_group.xml");

    // search is case insensitive only on name
    List<UserMembershipDto> result = underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).memberSearch("NaMe").build(), 0, 10);
    assertThat(result).hasSize(1);

    result = underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).memberSearch("login").build(), 0, 10);
    assertThat(result).hasSize(1);

    result = underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).memberSearch("email").build(), 0, 10);
    assertThat(result).hasSize(1);
  }

  @Test
  public void should_be_sorted_by_user_name() {
    dbTester.prepareDbUnit(getClass(), "shared_plus_empty_group.xml");

    List<UserMembershipDto> result = underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).build(), 0, 10);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Admin name");
    assertThat(result.get(1).getName()).isEqualTo("Not Admin");
  }

  @Test
  public void members_should_be_paginated() {
    dbTester.prepareDbUnit(getClass(), "shared_plus_empty_group.xml");

    List<UserMembershipDto> result = underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).build(), 0, 2);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Admin name");
    assertThat(result.get(1).getName()).isEqualTo("Not Admin");

    result = underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).build(), 1, 2);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Not Admin");

    result = underTest.selectMembers(dbTester.getSession(), UserMembershipQuery.builder().groupId(100).build(), 2, 1);
    assertThat(result).isEmpty();
  }
}
