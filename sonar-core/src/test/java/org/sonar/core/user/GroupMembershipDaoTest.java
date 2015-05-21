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

package org.sonar.core.user;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

@Category(DbTests.class)
public class GroupMembershipDaoTest {

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  private GroupMembershipDao dao;

  @Before
  public void setUp() {
    dbTester.truncateTables();
    dao = new GroupMembershipDao(dbTester.myBatis());
  }

  @Test
  public void select_all_groups_by_query() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    GroupMembershipQuery query = GroupMembershipQuery.builder().login("arthur").build();
    List<GroupMembershipDto> result = dao.selectGroups(query, 200L);
    assertThat(result).hasSize(3);
  }

  @Test
  public void select_user_group() {
    dbTester.prepareDbUnit(getClass(), "select_user_group.xml");

    GroupMembershipQuery query = GroupMembershipQuery.builder().login("arthur").build();
    List<GroupMembershipDto> result = dao.selectGroups(query, 201L);
    assertThat(result).hasSize(1);

    GroupMembershipDto dto = result.get(0);
    assertThat(dto.getId()).isEqualTo(101L);
    assertThat(dto.getName()).isEqualTo("sonar-users");
    assertThat(dto.getDescription()).isEqualTo("Any new users created will automatically join this group");
    assertThat(dto.getUserId()).isEqualTo(201L);
  }

  @Test
  public void select_user_groups_by_query() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    // 200 is member of 3 groups
    assertThat(dao.selectGroups(GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.IN).build(), 200L)).hasSize(3);
    // 201 is member of 1 group on 3
    assertThat(dao.selectGroups(GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.IN).build(), 201L)).hasSize(1);
    // 999 is member of 0 group
    assertThat(dao.selectGroups(GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.IN).build(), 999L)).isEmpty();
  }

  @Test
  public void select_groups_not_affected_to_a_user_by_query() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    // 200 is member of 3 groups
    assertThat(dao.selectGroups(GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.OUT).build(), 200L)).isEmpty();
    // 201 is member of 1 group on 3
    assertThat(dao.selectGroups(GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.OUT).build(), 201L)).hasSize(2);
    // 999 is member of 0 group
    assertThat(dao.selectGroups(GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.OUT).build(), 2999L)).hasSize(3);
  }

  @Test
  public void search_by_group_name() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<GroupMembershipDto> result = dao.selectGroups(GroupMembershipQuery.builder().login("arthur").groupSearch("user").build(), 200L);
    assertThat(result).hasSize(1);

    assertThat(result.get(0).getName()).isEqualTo("sonar-users");

    result = dao.selectGroups(GroupMembershipQuery.builder().login("arthur").groupSearch("sonar").build(), 200L);
    assertThat(result).hasSize(3);
  }

  @Test
  public void search_by_group_name_with_capitalization() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<GroupMembershipDto> result = dao.selectGroups(GroupMembershipQuery.builder().login("arthur").groupSearch("UsER").build(), 200L);
    assertThat(result).hasSize(1);

    assertThat(result.get(0).getName()).isEqualTo("sonar-users");

    result = dao.selectGroups(GroupMembershipQuery.builder().login("arthur").groupSearch("sonar").build(), 200L);
    assertThat(result).hasSize(3);
  }

  @Test
  public void should_be_sorted_by_group_name() {
    dbTester.prepareDbUnit(getClass(), "should_be_sorted_by_group_name.xml");

    List<GroupMembershipDto> result = dao.selectGroups(GroupMembershipQuery.builder().login("arthur").build(), 200L);
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getName()).isEqualTo("sonar-administrators");
    assertThat(result.get(1).getName()).isEqualTo("sonar-reviewers");
    assertThat(result.get(2).getName()).isEqualTo("sonar-users");
  }

  @Test
  public void should_be_paginated() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    List<GroupMembershipDto> result = dao.selectGroups(GroupMembershipQuery.builder().login("arthur").build(), 200L, 0, 2);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("sonar-administrators");
    assertThat(result.get(1).getName()).isEqualTo("sonar-reviewers");

    result = dao.selectGroups(GroupMembershipQuery.builder().login("arthur").build(), 200L, 1, 2);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("sonar-reviewers");
    assertThat(result.get(1).getName()).isEqualTo("sonar-users");

    result = dao.selectGroups(GroupMembershipQuery.builder().login("arthur").build(), 200L, 2, 1);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("sonar-users");
  }

  @Test
  public void count_groups() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    DbSession session = dbTester.myBatis().openSession(false);

    try {
      // 200 is member of 3 groups
      assertThat(dao.countGroups(session, GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.IN).build(), 200L)).isEqualTo(3);
      assertThat(dao.countGroups(session, GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.OUT).build(), 200L)).isZero();
      // 201 is member of 1 group on 3
      assertThat(dao.countGroups(session, GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.IN).build(), 201L)).isEqualTo(1);
      assertThat(dao.countGroups(session, GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.OUT).build(), 201L)).isEqualTo(2);
      // 999 is member of 0 group
      assertThat(dao.countGroups(session, GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.IN).build(), 999L)).isZero();
      assertThat(dao.countGroups(session, GroupMembershipQuery.builder().login("arthur").membership(GroupMembershipQuery.OUT).build(), 2999L)).isEqualTo(3);
    } finally {
      session.close();
    }
  }

  @Test
  public void count_groups_by_login() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");
    DbSession session = dbTester.myBatis().openSession(false);

    try {
      assertThat(dao.countGroupsByLogins(session, Arrays.<String>asList())).isEmpty();
      assertThat(dao.countGroupsByLogins(session, Arrays.asList("two-hundred")))
        .containsExactly(entry("two-hundred", 3));
      assertThat(dao.countGroupsByLogins(session, Arrays.asList("two-hundred", "two-hundred-one")))
        .containsOnly(entry("two-hundred", 3), entry("two-hundred-one", 1));
      assertThat(dao.countGroupsByLogins(session, Arrays.asList("two-hundred", "two-hundred-one", "two-hundred-two")))
        .containsOnly(entry("two-hundred", 3), entry("two-hundred-one", 1), entry("two-hundred-two", 0));
      assertThat(dao.countGroupsByLogins(session, Arrays.asList("two-hundred-two")))
        .containsOnly(entry("two-hundred-two", 0));
    } finally {
      session.close();
    }
  }

}
