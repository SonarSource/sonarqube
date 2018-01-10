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

import com.google.common.collect.Multimap;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.db.user.GroupMembershipQuery.OUT;
import static org.sonar.db.user.GroupMembershipQuery.builder;
import static org.sonar.db.user.UserTesting.newDisabledUser;
import static org.sonar.db.user.UserTesting.newUserDto;

public class GroupMembershipDaoTest {

  @Rule
  public DbTester db = DbTester.create();

  private OrganizationDto organizationDto;
  private UserDto user1;
  private UserDto user2;
  private UserDto user3;
  private GroupDto group1;
  private GroupDto group2;
  private GroupDto group3;

  private GroupMembershipDao underTest = db.getDbClient().groupMembershipDao();

  @Before
  public void setUp() throws Exception {
    organizationDto = db.organizations().insert();
    user1 = db.users().insertUser(newUserDto("admin login", "Admin name", "admin@email.com"));
    user2 = db.users().insertUser(newUserDto("not.admin", "Not Admin", "Not Admin"));
    user3 = db.users().insertUser(newDisabledUser("inactive"));
    group1 = db.users().insertGroup(organizationDto, "sonar-administrators");
    group2 = db.users().insertGroup(organizationDto, "sonar-users");
    group3 = db.users().insertGroup(organizationDto, "sonar-reviewers");
    db.organizations().addMember(organizationDto, user1);
    db.organizations().addMember(organizationDto, user2);
  }

  @Test
  public void count_groups() {
    db.users().insertMember(group1, user1);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group3, user1);
    db.users().insertMember(group2, user2);

    // user1 is member of 3 groups
    assertThat(underTest.countGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(IN).build(), user1.getId())).isEqualTo(3);
    assertThat(underTest.countGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(OUT).build(), user1.getId())).isZero();
    // user2 is member of 1 group on 3
    assertThat(underTest.countGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(IN).build(), user2.getId())).isEqualTo(1);
    assertThat(underTest.countGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(OUT).build(), user2.getId())).isEqualTo(2);
    // user3 is member of 0 group
    assertThat(underTest.countGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(IN).build(), user3.getId())).isZero();
    assertThat(underTest.countGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(OUT).build(), user3.getId())).isEqualTo(3);
    // unknown user is member of 0 group
    assertThat(underTest.countGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(IN).build(), 999)).isZero();
    assertThat(underTest.countGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(OUT).build(), 999)).isEqualTo(3);
  }

  @Test
  public void count_groups_only_from_given_organization() {
    OrganizationDto otherOrganization = db.organizations().insert();
    GroupDto otherGroup = db.users().insertGroup(otherOrganization, "sonar-administrators-other_orga");
    db.users().insertMember(group1, user1);
    db.users().insertMember(otherGroup, user1);

    assertThat(underTest.countGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(IN).build(), user1.getId())).isEqualTo(1);
  }

  @Test
  public void select_groups() {
    db.users().insertMember(group1, user1);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group3, user1);
    db.users().insertMember(group2, user2);

    // user1 is member of 3 groups
    assertThat(underTest.selectGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(IN).build(), user1.getId(), 0, 10)).hasSize(3);
    assertThat(underTest.selectGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(OUT).build(), user1.getId(), 0, 10)).isEmpty();
    // user2 is member of 1 group on 3
    assertThat(underTest.selectGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(IN).build(), user2.getId(), 0, 10)).hasSize(1);
    assertThat(underTest.selectGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(OUT).build(), user2.getId(), 0, 10)).hasSize(2);
    // user3 is member of 0 group
    assertThat(underTest.selectGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(IN).build(), user3.getId(), 0, 10)).isEmpty();
    assertThat(underTest.selectGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(OUT).build(), user3.getId(), 0, 10)).hasSize(3);
    // unknown user is member of 0 group
    assertThat(underTest.selectGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(IN).build(), 999, 0, 10)).isEmpty();
    assertThat(underTest.selectGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(OUT).build(), 999, 0, 10)).hasSize(3);
  }

  @Test
  public void select_groups_only_from_given_organization() {
    OrganizationDto otherOrganization = db.organizations().insert();
    GroupDto otherGroup = db.users().insertGroup(otherOrganization, "sonar-administrators-other_orga");
    db.users().insertMember(group1, user1);
    db.users().insertMember(otherGroup, user1);

    assertThat(underTest.selectGroups(db.getSession(), builder().organizationUuid(organizationDto.getUuid()).membership(IN).build(), user1.getId(), 0, 10)).hasSize(1);
  }

  @Test
  public void count_users_by_group() {
    GroupDto emptyGroup = db.users().insertGroup(organizationDto, "sonar-nobody");
    db.users().insertMember(group1, user1);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group3, user1);
    db.users().insertMember(group2, user2);

    assertThat(underTest.countUsersByGroups(db.getSession(), asList(group1.getId(), group2.getId(), group3.getId(), emptyGroup.getId())))
      .containsOnly(entry(group1.getName(), 1), entry(group2.getName(), 2), entry(group3.getName(), 1), entry(emptyGroup.getName(), 0));
    assertThat(underTest.countUsersByGroups(db.getSession(), asList(group1.getId(), emptyGroup.getId())))
      .containsOnly(entry(group1.getName(), 1), entry(emptyGroup.getName(), 0));
  }

  @Test
  public void count_groups_by_logins() {
    db.users().insertMember(group1, user1);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group3, user1);
    db.users().insertMember(group2, user2);

    assertThat(underTest.selectGroupsByLogins(db.getSession(), emptyList()).keys()).isEmpty();
    Multimap<String, String> groupsByLogin = underTest.selectGroupsByLogins(db.getSession(), asList(user1.getLogin(), user2.getLogin(), user3.getLogin()));
    assertThat(groupsByLogin.get(user1.getLogin())).containsOnly(group1.getName(), group2.getName(), group3.getName());
    assertThat(groupsByLogin.get(user2.getLogin())).containsOnly(group2.getName());
    assertThat(groupsByLogin.get(user3.getLogin())).isEmpty();
  }

  @Test
  public void count_members() {
    GroupDto emptyGroup = db.users().insertGroup(organizationDto, "sonar-nobody");
    db.users().insertMember(group1, user1);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group3, user1);
    db.users().insertMember(group2, user2);

    // 100 has 1 member and 1 non member
    assertThat(underTest.countMembers(db.getSession(), newQuery().groupId(group1.getId()).membership(UserMembershipQuery.IN).build())).isEqualTo(1);
    assertThat(underTest.countMembers(db.getSession(), newQuery().groupId(group1.getId()).membership(UserMembershipQuery.OUT).build())).isEqualTo(1);
    // 101 has 2 members
    assertThat(underTest.countMembers(db.getSession(), newQuery().groupId(group2.getId()).membership(UserMembershipQuery.IN).build())).isEqualTo(2);
    assertThat(underTest.countMembers(db.getSession(), newQuery().groupId(group2.getId()).membership(UserMembershipQuery.OUT).build())).isZero();
    // 102 has 1 member and 1 non member
    assertThat(underTest.countMembers(db.getSession(), newQuery().groupId(group3.getId()).membership(UserMembershipQuery.IN).build())).isEqualTo(1);
    assertThat(underTest.countMembers(db.getSession(), newQuery().groupId(group3.getId()).membership(UserMembershipQuery.OUT).build())).isEqualTo(1);
    // 103 has no member
    assertThat(underTest.countMembers(db.getSession(), newQuery().groupId(emptyGroup.getId()).membership(UserMembershipQuery.IN).build())).isZero();
    assertThat(underTest.countMembers(db.getSession(), newQuery().groupId(emptyGroup.getId()).membership(UserMembershipQuery.OUT).build())).isEqualTo(2);
  }

  @Test
  public void select_group_members_by_query() {
    GroupDto emptyGroup = db.users().insertGroup(organizationDto, "sonar-nobody");
    db.users().insertMember(group1, user1);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group3, user1);
    db.users().insertMember(group2, user2);

    // 100 has 1 member
    assertThat(underTest.selectMembers(db.getSession(), newQuery().groupId(group1.getId()).membership(UserMembershipQuery.IN).build(), 0, 10)).hasSize(1);
    // 101 has 2 members
    assertThat(underTest.selectMembers(db.getSession(), newQuery().groupId(group2.getId()).membership(UserMembershipQuery.IN).build(), 0, 10)).hasSize(2);
    // 102 has 1 member
    assertThat(underTest.selectMembers(db.getSession(), newQuery().groupId(group3.getId()).membership(UserMembershipQuery.IN).build(), 0, 10)).hasSize(1);
    // 103 has no member
    assertThat(underTest.selectMembers(db.getSession(), newQuery().groupId(emptyGroup.getId()).membership(UserMembershipQuery.IN).build(), 0, 10)).isEmpty();
  }

  @Test
  public void select_users_not_affected_to_a_group_by_query() {
    GroupDto emptyGroup = db.users().insertGroup(organizationDto, "sonar-nobody");
    db.users().insertMember(group1, user1);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group3, user1);
    db.users().insertMember(group2, user2);

    // 100 has 1 member
    assertThat(underTest.selectMembers(db.getSession(), newQuery().groupId(group1.getId()).membership(UserMembershipQuery.OUT).build(), 0, 10)).hasSize(1);
    // 101 has 2 members
    assertThat(underTest.selectMembers(db.getSession(), newQuery().groupId(group2.getId()).membership(UserMembershipQuery.OUT).build(), 0, 10)).isEmpty();
    // 102 has 1 member
    assertThat(underTest.selectMembers(db.getSession(), newQuery().groupId(group3.getId()).membership(UserMembershipQuery.OUT).build(), 0, 10)).hasSize(1);
    // 103 has no member
    assertThat(underTest.selectMembers(db.getSession(), newQuery().groupId(emptyGroup.getId()).membership(UserMembershipQuery.OUT).build(), 0, 10)).hasSize(2);
  }

  @Test
  public void search_by_user_name_or_login() {
    db.users().insertMember(group1, user1);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group3, user1);
    db.users().insertMember(group2, user2);

    List<UserMembershipDto> result = underTest.selectMembers(db.getSession(), newQuery().groupId(group1.getId()).memberSearch("admin").build(), 0, 10);
    assertThat(result).hasSize(2);

    assertThat(result.get(0).getName()).isEqualTo("Admin name");
    assertThat(result.get(1).getName()).isEqualTo("Not Admin");

    result = underTest.selectMembers(db.getSession(), newQuery().groupId(group1.getId()).memberSearch("not").build(), 0, 10);
    assertThat(result).hasSize(1);
  }

  @Test
  public void search_by_login_name_or_email() {
    db.users().insertMember(group1, user1);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group3, user1);
    db.users().insertMember(group2, user2);

    // search is case insensitive only on name
    List<UserMembershipDto> result = underTest.selectMembers(db.getSession(), newQuery().groupId(group1.getId()).memberSearch("NaMe").build(), 0, 10);
    assertThat(result).hasSize(1);

    result = underTest.selectMembers(db.getSession(), newQuery().groupId(group1.getId()).memberSearch("login").build(), 0, 10);
    assertThat(result).hasSize(1);

    result = underTest.selectMembers(db.getSession(), newQuery().groupId(group1.getId()).memberSearch("email").build(), 0, 10);
    assertThat(result).hasSize(1);
  }

  @Test
  public void should_be_sorted_by_user_name() {
    db.users().insertMember(group1, user1);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group3, user1);
    db.users().insertMember(group2, user2);

    List<UserMembershipDto> result = underTest.selectMembers(db.getSession(), newQuery().groupId(group1.getId()).build(), 0, 10);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Admin name");
    assertThat(result.get(1).getName()).isEqualTo("Not Admin");
  }

  @Test
  public void members_should_be_paginated() {
    db.users().insertMember(group1, user1);
    db.users().insertMember(group2, user1);
    db.users().insertMember(group3, user1);
    db.users().insertMember(group2, user2);

    List<UserMembershipDto> result = underTest.selectMembers(db.getSession(), newQuery().groupId(group1.getId()).build(), 0, 2);
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("Admin name");
    assertThat(result.get(1).getName()).isEqualTo("Not Admin");

    result = underTest.selectMembers(db.getSession(), newQuery().groupId(100).build(), 1, 2);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Not Admin");

    result = underTest.selectMembers(db.getSession(), newQuery().groupId(100).build(), 2, 1);
    assertThat(result).isEmpty();
  }

  private UserMembershipQuery.Builder newQuery() {
    return UserMembershipQuery.builder().organizationUuid(organizationDto.getUuid());
  }
}
