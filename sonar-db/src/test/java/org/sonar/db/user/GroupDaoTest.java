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
package org.sonar.db.user;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.organization.OrganizationDto;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.user.GroupTesting.newGroupDto;

public class GroupDaoTest {

  private static final long NOW = 1_500_000L;
  private static final OrganizationDto AN_ORGANIZATION = new OrganizationDto()
    .setKey("an-org")
    .setName("An Org")
    .setUuid("abcde");
  private static final long DATE_1 = 8_776_543L;
  private static final long DATE_2 = 4_776_898L;

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);

  private final DbSession dbSession = db.getSession();
  private GroupDao underTest = new GroupDao(system2);

  // not static as group id is changed in each test
  private final GroupDto aGroup = new GroupDto()
    .setName("the-name")
    .setDescription("the description")
    .setOrganizationUuid(AN_ORGANIZATION.getUuid());

  @Before
  public void setUp() {
    when(system2.now()).thenReturn(NOW);
    db.getDbClient().organizationDao().insert(dbSession, AN_ORGANIZATION);
  }

  @Test
  public void selectByName() {
    db.getDbClient().groupDao().insert(dbSession, aGroup);

    GroupDto group = underTest.selectByName(dbSession, AN_ORGANIZATION.getUuid(), aGroup.getName()).get();

    assertThat(group.getId()).isNotNull();
    assertThat(group.getOrganizationUuid()).isEqualTo(aGroup.getOrganizationUuid());
    assertThat(group.getName()).isEqualTo(aGroup.getName());
    assertThat(group.getDescription()).isEqualTo(aGroup.getDescription());
    assertThat(group.getCreatedAt()).isEqualTo(new Date(NOW));
    assertThat(group.getUpdatedAt()).isEqualTo(new Date(NOW));
  }

  @Test
  public void selectByName_returns_absent() {
    Optional<GroupDto> group = underTest.selectByName(dbSession, AN_ORGANIZATION.getUuid(), "missing");

    assertThat(group).isNotPresent();
  }

  @Test
  public void selectByUserLogin() {
    db.prepareDbUnit(getClass(), "find_by_user_login.xml");

    assertThat(underTest.selectByUserLogin(dbSession, "john")).hasSize(2);
    assertThat(underTest.selectByUserLogin(dbSession, "max")).isEmpty();
  }

  @Test
  public void selectByNames() {
    underTest.insert(dbSession, newGroupDto().setName("group1"));
    underTest.insert(dbSession, newGroupDto().setName("group2"));
    underTest.insert(dbSession, newGroupDto().setName("group3"));
    dbSession.commit();

    assertThat(underTest.selectByNames(dbSession, asList("group1", "group2", "group3"))).hasSize(3);
    assertThat(underTest.selectByNames(dbSession, singletonList("group1"))).hasSize(1);
    assertThat(underTest.selectByNames(dbSession, asList("group1", "unknown"))).hasSize(1);
    assertThat(underTest.selectByNames(dbSession, Collections.emptyList())).isEmpty();
  }

  @Test
  public void update() {
    db.getDbClient().groupDao().insert(dbSession, aGroup);
    GroupDto dto = new GroupDto()
      .setId(aGroup.getId())
      .setName("new-name")
      .setDescription("New description")
      .setOrganizationUuid("another-org")
      .setCreatedAt(new Date(NOW + 1_000L));

    underTest.update(dbSession, dto);

    GroupDto reloaded = underTest.selectById(dbSession, aGroup.getId());

    // verify mutable fields
    assertThat(reloaded.getName()).isEqualTo("new-name");
    assertThat(reloaded.getDescription()).isEqualTo("New description");

    // immutable fields --> to be ignored
    assertThat(reloaded.getOrganizationUuid()).isEqualTo(aGroup.getOrganizationUuid());
    assertThat(reloaded.getCreatedAt()).isEqualTo(aGroup.getCreatedAt());
  }

  @Test
  public void selectByQuery() {
    db.prepareDbUnit(getClass(), "select_by_query.xml");

    /*
     * Ordering and paging are not fully tested, case insensitive sort is broken on MySQL
     */

    // Null query
    assertThat(underTest.selectByQuery(dbSession, "org1", null, 0, 10))
      .hasSize(5)
      .extracting("name").containsOnly("customers-group1", "customers-group2", "customers-group3", "SONAR-ADMINS", "sonar-users");

    // Empty query
    assertThat(underTest.selectByQuery(dbSession, "org1", "", 0, 10))
      .hasSize(5)
      .extracting("name").containsOnly("customers-group1", "customers-group2", "customers-group3", "SONAR-ADMINS", "sonar-users");

    // Filter on name
    assertThat(underTest.selectByQuery(dbSession, "org1", "sonar", 0, 10))
      .hasSize(2)
      .extracting("name").containsOnly("SONAR-ADMINS", "sonar-users");

    // Pagination
    assertThat(underTest.selectByQuery(dbSession, "org1", null, 0, 3))
      .hasSize(3);
    assertThat(underTest.selectByQuery(dbSession, "org1", null, 3, 3))
      .hasSize(2);
    assertThat(underTest.selectByQuery(dbSession, "org1", null, 6, 3)).isEmpty();
    assertThat(underTest.selectByQuery(dbSession, "org1", null, 0, 5))
      .hasSize(5);
    assertThat(underTest.selectByQuery(dbSession, "org1", null, 5, 5)).isEmpty();
  }

  @Test
  public void select_by_query_with_special_characters() {
    String groupNameWithSpecialCharacters = "group%_%/name";
    underTest.insert(dbSession, newGroupDto().setName(groupNameWithSpecialCharacters).setOrganizationUuid("org1"));
    db.commit();

    List<GroupDto> result = underTest.selectByQuery(dbSession, "org1", "roup%_%/nam", 0, 10);
    int resultCount = underTest.countByQuery(dbSession, "org1", "roup%_%/nam");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo(groupNameWithSpecialCharacters);
    assertThat(resultCount).isEqualTo(1);
  }

  @Test
  public void countByQuery() {
    db.prepareDbUnit(getClass(), "select_by_query.xml");

    // Null query
    assertThat(underTest.countByQuery(dbSession, "org1", null)).isEqualTo(5);

    // Empty query
    assertThat(underTest.countByQuery(dbSession, "org1", "")).isEqualTo(5);

    // Filter on name
    assertThat(underTest.countByQuery(dbSession, "org1", "sonar")).isEqualTo(2);
  }

  @Test
  public void deleteById() {
    db.getDbClient().groupDao().insert(dbSession, aGroup);

    underTest.deleteById(dbSession, aGroup.getId());

    assertThat(db.countRowsOfTable(dbSession, "groups")).isEqualTo(0);
  }

  @Test
  public void updateRootFlagOfUsersInGroupFromPermissions_sets_root_flag_to_false_if_users_have_no_permission_at_all() {
    UserDto[] usersInGroup1 = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser()
    };
    UserDto[] usersInGroup2 = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser()
    };
    UserDto[] usersNotInGroup = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser()
    };
    GroupDto group1 = db.users().insertGroup();
    stream(usersInGroup1).forEach(user -> db.users().insertMember(group1, user));
    GroupDto group2 = db.users().insertGroup();
    stream(usersInGroup2).forEach(user -> db.users().insertMember(group2, user));

    call_updateRootFlagFromPermissions(group1, DATE_1);
    stream(usersInGroup1).forEach(user -> db.rootFlag().verify(user, false, DATE_1));
    stream(usersInGroup2).forEach(db.rootFlag()::verifyUnchanged);
    stream(usersNotInGroup).forEach(db.rootFlag()::verifyUnchanged);

    call_updateRootFlagFromPermissions(group2, DATE_2);
    stream(usersInGroup2).forEach(user -> db.rootFlag().verify(user, false, DATE_2));
    stream(usersInGroup1).forEach(user -> db.rootFlag().verify(user, false, DATE_1));
    stream(usersNotInGroup).forEach(db.rootFlag()::verifyUnchanged);
  }

  @Test
  public void updateRootFlagOfUsersInGroupFromPermissions_sets_root_flag_to_true_if_users_has_admin_user_permission_on_default_organization() {
    UserDto[] usersWithAdminInGroup1 = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser(),
    };
    UserDto[] usersWithoutAdminInGroup1 = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser(),
    };
    UserDto[] usersWithAdminInGroup2 = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser()
    };
    UserDto[] usersWithoutAdminInGroup2 = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser()
    };
    UserDto[] usersNotInGroup = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser()
    };
    GroupDto group1 = db.users().insertGroup();
    stream(usersWithAdminInGroup1).forEach(user -> db.users().insertMember(group1, user));
    stream(usersWithoutAdminInGroup1).forEach(user -> db.users().insertMember(group1, user));
    stream(usersWithAdminInGroup1).forEach(user -> db.users().insertPermissionOnUser(db.getDefaultOrganization(), user, SYSTEM_ADMIN));
    GroupDto group2 = db.users().insertGroup();
    stream(usersWithAdminInGroup2).forEach(user -> db.users().insertMember(group2, user));
    stream(usersWithoutAdminInGroup2).forEach(user -> db.users().insertMember(group2, user));
    stream(usersWithAdminInGroup2).forEach(user -> db.users().insertPermissionOnUser(db.getDefaultOrganization(), user, SYSTEM_ADMIN));

    call_updateRootFlagFromPermissions(group1, DATE_1);
    stream(usersWithAdminInGroup1).forEach(user -> db.rootFlag().verify(user, true, DATE_1));
    stream(usersWithoutAdminInGroup1).forEach(user -> db.rootFlag().verify(user, false, DATE_1));
    stream(usersWithAdminInGroup2).forEach(db.rootFlag()::verifyUnchanged);
    stream(usersWithoutAdminInGroup2).forEach(db.rootFlag()::verifyUnchanged);
    stream(usersNotInGroup).forEach(db.rootFlag()::verifyUnchanged);

    call_updateRootFlagFromPermissions(group2, DATE_2);
    stream(usersWithAdminInGroup1).forEach(user -> db.rootFlag().verify(user, true, DATE_1));
    stream(usersWithoutAdminInGroup1).forEach(user -> db.rootFlag().verify(user, false, DATE_1));
    stream(usersWithAdminInGroup2).forEach(user -> db.rootFlag().verify(user, true, DATE_2));
    stream(usersWithoutAdminInGroup2).forEach(user -> db.rootFlag().verify(user, false, DATE_2));
    stream(usersNotInGroup).forEach(db.rootFlag()::verifyUnchanged);
  }

  @Test
  public void updateRootFlagOfUsersInGroupFromPermissions_ignores_permissions_on_anyone_on_default_organization() {
    UserDto[] usersWithAdminInGroup1 = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser(),
    };
    UserDto[] usersWithoutAdminInGroup1 = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser(),
    };
    UserDto[] usersWithAdminInGroup2 = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser()
    };
    UserDto[] usersWithoutAdminInGroup2 = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser()
    };
    UserDto[] usersNotInGroup = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser()
    };
    GroupDto group1 = db.users().insertGroup();
    stream(usersWithAdminInGroup1).forEach(user -> db.users().insertMember(group1, user));
    stream(usersWithoutAdminInGroup1).forEach(user -> db.users().insertMember(group1, user));
    GroupDto group2 = db.users().insertGroup();
    stream(usersWithAdminInGroup2).forEach(user -> db.users().insertMember(group2, user));
    stream(usersWithoutAdminInGroup2).forEach(user -> db.users().insertMember(group2, user));
    db.users().insertPermissionOnAnyone(db.getDefaultOrganization(), SYSTEM_ADMIN);

    call_updateRootFlagFromPermissions(group1, DATE_1);
    stream(usersWithAdminInGroup1).forEach(user -> db.rootFlag().verify(user, false, DATE_1));
    stream(usersWithoutAdminInGroup1).forEach(user -> db.rootFlag().verify(user, false, DATE_1));
    stream(usersWithAdminInGroup2).forEach(db.rootFlag()::verifyUnchanged);
    stream(usersWithoutAdminInGroup2).forEach(db.rootFlag()::verifyUnchanged);
    stream(usersNotInGroup).forEach(db.rootFlag()::verifyUnchanged);

    call_updateRootFlagFromPermissions(group2, DATE_2);
    stream(usersWithAdminInGroup1).forEach(user -> db.rootFlag().verify(user, false, DATE_1));
    stream(usersWithoutAdminInGroup1).forEach(user -> db.rootFlag().verify(user, false, DATE_1));
    stream(usersWithAdminInGroup2).forEach(user -> db.rootFlag().verify(user, false, DATE_2));
    stream(usersWithoutAdminInGroup2).forEach(user -> db.rootFlag().verify(user, false, DATE_2));
    stream(usersNotInGroup).forEach(db.rootFlag()::verifyUnchanged);
  }

  @Test
  public void updateRootFlagOfUsersInGroupFromPermissions_ignores_permissions_on_anyone_on_other_organization() {
    UserDto[] usersInGroup = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser(),
    };
    UserDto[] usersInOtherGroup = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser()
    };
    UserDto[] usersNotInGroup = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser()
    };
    GroupDto group = db.users().insertGroup();
    stream(usersInGroup).forEach(user -> db.users().insertMember(group, user));
    OrganizationDto otherOrganization = db.organizations().insert();
    GroupDto otherGroup = db.users().insertGroup(otherOrganization);
    stream(usersInOtherGroup).forEach(user -> db.users().insertMember(otherGroup, user));
    db.users().insertPermissionOnAnyone(otherOrganization, SYSTEM_ADMIN);

    call_updateRootFlagFromPermissions(group, DATE_1);
    stream(usersInGroup).forEach(user -> db.rootFlag().verify(user, false, DATE_1));
    stream(usersInOtherGroup).forEach(db.rootFlag()::verifyUnchanged);
    stream(usersNotInGroup).forEach(db.rootFlag()::verifyUnchanged);

    call_updateRootFlagFromPermissions(otherGroup, DATE_2);
    stream(usersInGroup).forEach(user -> db.rootFlag().verify(user, false, DATE_1));
    stream(usersInOtherGroup).forEach(user -> db.rootFlag().verify(user, false, DATE_2));
    stream(usersNotInGroup).forEach(db.rootFlag()::verifyUnchanged);
  }

  @Test
  public void updateRootFlagOfUsersInGroupFromPermissions_set_root_flag_to_false_on_users_of_group_of_non_default_organization() {
    UserDto[] nonAdminUsers = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser()
    };
    UserDto[] adminPerUserPermissionUsers = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser() // incorrectly not root
    };
    UserDto[] adminPerGroupPermissionUsers = {
      db.users().makeRoot(db.users().insertUser()),
      db.users().insertUser() // incorrectly not root
    };
    OrganizationDto otherOrganization = db.organizations().insert();
    GroupDto nonAdminGroup = db.users().insertGroup(otherOrganization);
    db.users().insertMembers(nonAdminGroup, nonAdminUsers);
    db.users().insertMembers(nonAdminGroup, adminPerUserPermissionUsers);
    stream(adminPerUserPermissionUsers).forEach(user -> db.users().insertPermissionOnUser(otherOrganization, user, SYSTEM_ADMIN));
    GroupDto adminGroup = db.users().insertAdminGroup(otherOrganization);
    db.users().insertMembers(adminGroup, adminPerGroupPermissionUsers);

    call_updateRootFlagFromPermissions(nonAdminGroup, DATE_2);
    stream(nonAdminUsers).forEach(user -> db.rootFlag().verify(user, false, DATE_2));
    stream(adminPerUserPermissionUsers).forEach(user -> db.rootFlag().verify(user, false, DATE_2));
    stream(adminPerGroupPermissionUsers).forEach(db.rootFlag()::verifyUnchanged);

    call_updateRootFlagFromPermissions(adminGroup, DATE_1);
    stream(nonAdminUsers).forEach(user -> db.rootFlag().verify(user, false, DATE_2));
    stream(adminPerUserPermissionUsers).forEach(user -> db.rootFlag().verify(user, false, DATE_2));
    stream(adminPerGroupPermissionUsers).forEach(user -> db.rootFlag().verify(user, false, DATE_1));
  }

  @Test
  public void deleteByOrganization_does_not_fail_when_table_is_empty() {
    underTest.deleteByOrganization(dbSession, "some uuid");
    dbSession.commit();
  }

  @Test
  public void deleteByOrganization_does_not_fail_when_organization_has_no_group() {
    OrganizationDto organization = db.organizations().insert();

    underTest.deleteByOrganization(dbSession, organization.getUuid());
    dbSession.commit();
  }

  @Test
  public void deleteByOrganization_deletes_all_groups_in_specified_organization() {
    OrganizationDto organization1 = db.organizations().insert();
    OrganizationDto organization2 = db.organizations().insert();
    OrganizationDto organization3 = db.organizations().insert();

    db.users().insertGroup(organization1);
    db.users().insertGroup(organization2);
    db.users().insertGroup(organization3);
    db.users().insertGroup(organization3);
    db.users().insertGroup(organization2);

    underTest.deleteByOrganization(dbSession, organization2.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable(organization1.getUuid(), organization3.getUuid());

    underTest.deleteByOrganization(dbSession, organization1.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable(organization3.getUuid());

    underTest.deleteByOrganization(dbSession, organization3.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable();
  }

  private void verifyOrganizationUuidsInTable(String... organizationUuids) {
    assertThat(db.select("select distinct organization_uuid as \"organizationUuid\" from groups"))
      .extracting(row -> (String) row.get("organizationUuid"))
      .containsOnly(organizationUuids);
  }

  private void call_updateRootFlagFromPermissions(GroupDto groupDto, long now) {
    when(system2.now()).thenReturn(now);
    underTest.updateRootFlagOfUsersInGroupFromPermissions(db.getSession(), groupDto.getId(), db.getDefaultOrganization().getUuid());
    db.commit();
  }
}
