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
package org.sonar.db.permission;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UserPermissionDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private UserPermissionDao underTest = new UserPermissionDao();
  private UserDto user1 = newUserDto().setLogin("login1").setName("Marius").setActive(true);
  private UserDto user2 = newUserDto().setLogin("login2").setName("Marie").setActive(true);
  private UserDto user3 = newUserDto().setLogin("login3").setName("Bernard").setActive(true);
  private OrganizationDto organizationDto;
  private ComponentDto project1;
  private ComponentDto project2;
  private DbSession dbSession = dbTester.getSession();

  @Before
  public void setUp() throws Exception {
    DbClient dbClient = dbTester.getDbClient();
    dbClient.userDao().insert(dbSession, user1);
    dbClient.userDao().insert(dbSession, user2);
    dbClient.userDao().insert(dbSession, user3);
    organizationDto = dbTester.organizations().insert();
    project1 = dbTester.components().insertProject(organizationDto);
    project2 = dbTester.components().insertProject(organizationDto);
    dbTester.commit();
  }

  @Test
  public void select_global_permissions() {
    OrganizationDto org2 = dbTester.organizations().insert();
    UserPermissionDto global1 = addGlobalPermission(organizationDto, SYSTEM_ADMIN, user1);
    UserPermissionDto global2 = addGlobalPermission(organizationDto, SYSTEM_ADMIN, user2);
    UserPermissionDto global3 = addGlobalPermission(organizationDto, PROVISIONING, user2);
    UserPermissionDto project1Perm = addProjectPermission(organizationDto, USER, user3, project1);
    // permissions on another organization, to be excluded
    UserPermissionDto org2Global1 = addGlobalPermission(org2, SYSTEM_ADMIN, user1);
    UserPermissionDto org2Global2 = addGlobalPermission(org2, PROVISIONING, user2);

    // global permissions of users who has at least one global permission, ordered by user name then permission
    PermissionQuery query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(organizationDto, query, null, global2, global3, global1);

    // default query returns all users, whatever their permissions nor organizations
    // (that's a non-sense, but still this is required for api/permissions/groups
    // when filtering users by name)
    query = PermissionQuery.builder().build();
    expectPermissions(organizationDto, query, null, project1Perm, global2, global3, org2Global2, global1, org2Global1);

    // return empty list if non-null but empty logins
    expectPermissions(organizationDto, query, Collections.emptyList());

    // global permissions of user1
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(organizationDto, query, asList(user1.getLogin()), global1);

    // global permissions of user2
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(organizationDto, query, asList(user2.getLogin()), global2, global3);

    // global permissions of user1, user2 and another one
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(organizationDto, query, asList(user1.getLogin(), user2.getLogin(), "missing"), global2, global3, global1);

    // empty global permissions if login does not exist
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(organizationDto, query, asList("missing"));

    // empty global permissions if user does not have any
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(organizationDto, query, asList(user3.getLogin()));

    // user3 has no global permissions
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(organizationDto, query, asList(user3.getLogin()));

    // global permissions "admin"
    query = PermissionQuery.builder().setPermission(SYSTEM_ADMIN).build();
    expectPermissions(organizationDto, query, null, global2, global1);

    // empty if nobody has the specified global permission
    query = PermissionQuery.builder().setPermission("missing").build();
    expectPermissions(organizationDto, query, null);

    // search by user name (matches 2 users)
    query = PermissionQuery.builder().withAtLeastOnePermission().setSearchQuery("Mari").build();
    expectPermissions(organizationDto, query, null, global2, global3, global1);

    // search by user name (matches 2 users) and global permission
    query = PermissionQuery.builder().setSearchQuery("Mari").setPermission(PROVISIONING).build();
    expectPermissions(organizationDto, query, null, global3);

    // search by user name (no match)
    query = PermissionQuery.builder().setSearchQuery("Unknown").build();
    expectPermissions(organizationDto, query, null);
  }

  @Test
  public void select_project_permissions() {
    addGlobalPermission(organizationDto, SYSTEM_ADMIN, user1);
    UserPermissionDto perm1 = addProjectPermission(organizationDto, USER, user1, project1);
    UserPermissionDto perm2 = addProjectPermission(organizationDto, ISSUE_ADMIN, user1, project1);
    UserPermissionDto perm3 = addProjectPermission(organizationDto, ISSUE_ADMIN, user2, project1);
    addProjectPermission(organizationDto, ISSUE_ADMIN, user3, project2);

    // project permissions of users who has at least one permission on this project
    PermissionQuery query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(organizationDto, query, null, perm3, perm2, perm1);

    // project permissions of user1
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(organizationDto, query, asList(user1.getLogin()), perm2, perm1);

    // project permissions of user2
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(organizationDto, query, asList(user2.getLogin()), perm3);

    // project permissions of user2 and another one
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(organizationDto, query, asList(user2.getLogin(), "missing"), perm3);

    // empty project permissions if login does not exist
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(organizationDto, query, asList("missing"));

    // empty project permissions if user does not have any
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(organizationDto, query, asList(user3.getLogin()));

    // empty if nobody has the specified global permission
    query = PermissionQuery.builder().setPermission("missing").setComponentUuid(project1.uuid()).build();
    expectPermissions(organizationDto, query, null);

    // search by user name (matches 2 users), users with at least one permission
    query = PermissionQuery.builder().setSearchQuery("Mari").withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(organizationDto, query, null, perm3, perm2, perm1);

    // search by user name (matches 2 users) and project permission
    query = PermissionQuery.builder().setSearchQuery("Mari").setPermission(ISSUE_ADMIN).setComponentUuid(project1.uuid()).build();
    expectPermissions(organizationDto, query, null, perm3, perm2);

    // search by user name (no match)
    query = PermissionQuery.builder().setSearchQuery("Unknown").setComponentUuid(project1.uuid()).build();
    expectPermissions(organizationDto, query, null);

    // permissions of unknown project
    query = PermissionQuery.builder().setComponentUuid("missing").withAtLeastOnePermission().build();
    expectPermissions(organizationDto, query, null);
  }

  @Test
  public void countUsersByProjectPermission() {
    addGlobalPermission(organizationDto, SYSTEM_ADMIN, user1);
    addProjectPermission(organizationDto, USER, user1, project1);
    addProjectPermission(organizationDto, ISSUE_ADMIN, user1, project1);
    addProjectPermission(organizationDto, ISSUE_ADMIN, user2, project1);
    addProjectPermission(organizationDto, ISSUE_ADMIN, user2, project2);

    // no projects -> return empty list
    assertThat(underTest.countUsersByProjectPermission(dbSession, Collections.emptyList())).isEmpty();

    // one project
    expectCount(asList(project1.getId()),
      new CountPerProjectPermission(project1.getId(), USER, 1),
      new CountPerProjectPermission(project1.getId(), ISSUE_ADMIN, 2));

    // multiple projects
    expectCount(asList(project1.getId(), project2.getId(), -1L),
      new CountPerProjectPermission(project1.getId(), USER, 1),
      new CountPerProjectPermission(project1.getId(), ISSUE_ADMIN, 2),
      new CountPerProjectPermission(project2.getId(), ISSUE_ADMIN, 1));
  }

  @Test
  public void selectUserIds() {
    OrganizationDto org1 = dbTester.organizations().insert();
    OrganizationDto org2 = dbTester.organizations().insert();
    ComponentDto project1 = dbTester.components().insertProject(org1);
    ComponentDto project2 = dbTester.components().insertProject(org2);

    addProjectPermission(org1, USER, user1, project1);
    addProjectPermission(org1, USER, user2, project1);
    addProjectPermission(org2, USER, user1, project2);
    addProjectPermission(org1, ISSUE_ADMIN, user2, project1);
    addProjectPermission(org2, ISSUE_ADMIN, user2, project2);

    // logins are ordered by user name: user2 ("Marie") then user1 ("Marius")
    PermissionQuery query = PermissionQuery.builder().setComponentUuid(project1.uuid()).withAtLeastOnePermission().build();
    assertThat(underTest.selectUserIds(dbSession, org1.getUuid(), query)).containsExactly(user2.getId(), user1.getId());
    assertThat(underTest.selectUserIds(dbSession, "otherOrg", query)).isEmpty();

    // on a project without permissions
    query = PermissionQuery.builder().setComponentUuid("missing").withAtLeastOnePermission().build();
    assertThat(underTest.selectUserIds(dbSession, org1.getUuid(), query)).isEmpty();

    // search all users whose name matches "mar", whatever the permissions
    query = PermissionQuery.builder().setSearchQuery("mar").build();
    assertThat(underTest.selectUserIds(dbSession, org1.getUuid(), query)).containsExactly(user2.getId(), user1.getId());

    // search all users whose name matches "mariu", whatever the permissions
    query = PermissionQuery.builder().setSearchQuery("mariu").build();
    assertThat(underTest.selectUserIds(dbSession, org1.getUuid(), query)).containsExactly(user1.getId());

    // search all users whose name matches "mariu", whatever the permissions
    query = PermissionQuery.builder().setSearchQuery("mariu").setComponentUuid(project1.uuid()).build();
    assertThat(underTest.selectUserIds(dbSession, org1.getUuid(), query)).containsExactly(user1.getId());

    // search all users whose name matches "mariu", whatever the organization
    query = PermissionQuery.builder().setSearchQuery("mariu").build();
    assertThat(underTest.selectUserIds(dbSession, "missingOrg", query)).containsExactly(user1.getId());
  }

  @Test
  public void deleteGlobalPermission() {
    addGlobalPermission(organizationDto, "perm1", user1);
    addGlobalPermission(organizationDto, "perm2", user1);
    addProjectPermission(organizationDto, "perm1", user1, project1);
    addProjectPermission(organizationDto, "perm3", user2, project1);
    addProjectPermission(organizationDto, "perm4", user2, project2);

    // user2 does not have global permissions -> do nothing
    underTest.deleteGlobalPermission(dbSession, user2.getId(), "perm1", dbTester.getDefaultOrganization().getUuid());
    assertThat(dbTester.countRowsOfTable(dbSession, "user_roles")).isEqualTo(5);

    // global permission is not granted -> do nothing
    underTest.deleteGlobalPermission(dbSession, user1.getId(), "notGranted", dbTester.getDefaultOrganization().getUuid());
    assertThat(dbTester.countRowsOfTable(dbSession, "user_roles")).isEqualTo(5);

    // permission is on project -> do nothing
    underTest.deleteGlobalPermission(dbSession, user1.getId(), "perm3", dbTester.getDefaultOrganization().getUuid());
    assertThat(dbTester.countRowsOfTable(dbSession, "user_roles")).isEqualTo(5);

    // global permission on another organization-> do nothing
    underTest.deleteGlobalPermission(dbSession, user1.getId(), "notGranted", "anotherOrg");
    assertThat(dbTester.countRowsOfTable(dbSession, "user_roles")).isEqualTo(5);

    // global permission exists -> delete it, but not the project permission with the same name !
    underTest.deleteGlobalPermission(dbSession, user1.getId(), "perm1", organizationDto.getUuid());
    assertThat(dbTester.countSql(dbSession, "select count(id) from user_roles where role='perm1' and resource_id is null")).isEqualTo(0);
    assertThat(dbTester.countRowsOfTable(dbSession, "user_roles")).isEqualTo(4);
  }

  @Test
  public void deleteProjectPermission() {
    addGlobalPermission(organizationDto, "perm", user1);
    addProjectPermission(organizationDto, "perm", user1, project1);
    addProjectPermission(organizationDto, "perm", user1, project2);
    addProjectPermission(organizationDto, "perm", user2, project1);

    // no such provision -> ignore
    underTest.deleteProjectPermission(dbSession, user1.getId(), "anotherPerm", project1.getId());
    assertThat(dbTester.countRowsOfTable(dbSession, "user_roles")).isEqualTo(4);

    underTest.deleteProjectPermission(dbSession, user1.getId(), "perm", project1.getId());
    assertThatProjectPermissionDoesNotExist(user1, "perm", project1);
    assertThat(dbTester.countRowsOfTable(dbSession, "user_roles")).isEqualTo(3);
  }

  @Test
  public void deleteProjectPermissions() {
    addGlobalPermission(organizationDto, "perm", user1);
    addProjectPermission(organizationDto, "perm", user1, project1);
    addProjectPermission(organizationDto, "perm", user2, project1);
    addProjectPermission(organizationDto, "perm", user1, project2);

    underTest.deleteProjectPermissions(dbSession, project1.getId());
    assertThat(dbTester.countRowsOfTable(dbSession, "user_roles")).isEqualTo(2);
    assertThatProjectHasNoPermissions(project1);
  }

  @Test
  public void projectHasPermissions() {
    addGlobalPermission(organizationDto, SYSTEM_ADMIN, user1);
    addProjectPermission(organizationDto, USER, user1, project1);

    assertThat(underTest.hasRootComponentPermissions(dbSession, project1.getId())).isTrue();
    assertThat(underTest.hasRootComponentPermissions(dbSession, project2.getId())).isFalse();
  }

  @Test
  public void selectGlobalPermissionsOfUser() {
    OrganizationDto org = dbTester.organizations().insert();
    addGlobalPermission(dbTester.getDefaultOrganization(), "perm1", user1);
    addGlobalPermission(org, "perm2", user2);
    addGlobalPermission(org, "perm3", user1);
    addProjectPermission(organizationDto, "perm4", user1, project1);
    addProjectPermission(organizationDto, "perm5", user1, project1);

    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getId(), org.getUuid())).containsOnly("perm3");
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getId(), dbTester.getDefaultOrganization().getUuid())).containsOnly("perm1");
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user1.getId(), "otherOrg")).isEmpty();
    assertThat(underTest.selectGlobalPermissionsOfUser(dbSession, user3.getId(), org.getUuid())).isEmpty();
  }

  @Test
  public void selectProjectPermissionsOfUser() {
    OrganizationDto org = dbTester.organizations().insert();
    ComponentDto project3 = dbTester.components().insertProject(org);
    addGlobalPermission(organizationDto, "perm1", user1);
    addProjectPermission(organizationDto, "perm2", user1, project1);
    addProjectPermission(organizationDto, "perm3", user1, project1);
    addProjectPermission(organizationDto, "perm4", user1, project2);
    addProjectPermission(organizationDto, "perm5", user2, project1);

    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getId(), project1.getId())).containsOnly("perm2", "perm3");
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getId(), project2.getId())).containsOnly("perm4");
    assertThat(underTest.selectProjectPermissionsOfUser(dbSession, user1.getId(), project3.getId())).isEmpty();
  }

  @Test
  public void deleteByOrganization_does_not_fail_if_table_is_empty() {
    underTest.deleteByOrganization(dbSession, "some uuid");
    dbSession.commit();
  }

  @Test
  public void deleteByOrganization_does_not_fail_if_organization_has_no_user_permission() {
    OrganizationDto organization = dbTester.organizations().insert();

    underTest.deleteByOrganization(dbSession, organization.getUuid());
    dbSession.commit();
  }

  @Test
  public void deleteByOrganization_deletes_all_user_permission_of_specified_organization() {
    OrganizationDto organization1 = dbTester.organizations().insert();
    OrganizationDto organization2 = dbTester.organizations().insert();
    OrganizationDto organization3 = dbTester.organizations().insert();
    UserDto user1 = dbTester.users().insertUser();
    UserDto user2 = dbTester.users().insertUser();
    UserDto user3 = dbTester.users().insertUser();
    dbTester.users().insertPermissionOnUser(organization1, user1, "foo");
    dbTester.users().insertPermissionOnUser(organization1, user2, "foo");
    dbTester.users().insertPermissionOnUser(organization1, user2, "bar");
    dbTester.users().insertPermissionOnUser(organization2, user2, "foo");
    dbTester.users().insertPermissionOnUser(organization2, user3, "foo");
    dbTester.users().insertPermissionOnUser(organization2, user3, "bar");
    dbTester.users().insertPermissionOnUser(organization3, user3, "foo");
    dbTester.users().insertPermissionOnUser(organization3, user1, "foo");
    dbTester.users().insertPermissionOnUser(organization3, user1, "bar");

    underTest.deleteByOrganization(dbSession, organization3.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable(organization1.getUuid(), organization2.getUuid());

    underTest.deleteByOrganization(dbSession, organization2.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable(organization1.getUuid());

    underTest.deleteByOrganization(dbSession, organization1.getUuid());
    dbSession.commit();
    verifyOrganizationUuidsInTable();
  }

  private void verifyOrganizationUuidsInTable(String... organizationUuids) {
    assertThat(dbTester.select("select organization_uuid as \"organizationUuid\" from user_roles"))
        .extracting((row) -> (String) row.get("organizationUuid"))
        .containsOnly(organizationUuids);
  }

  private void expectCount(List<Long> projectIds, CountPerProjectPermission... expected) {
    List<CountPerProjectPermission> got = underTest.countUsersByProjectPermission(dbSession, projectIds);
    assertThat(got).hasSize(expected.length);

    for (CountPerProjectPermission expect : expected) {
      boolean found = got.stream().anyMatch(b -> b.getPermission().equals(expect.getPermission()) &&
        b.getCount() == expect.getCount() &&
        b.getComponentId() == expect.getComponentId());
      assertThat(found).isTrue();
    }
  }

  private void expectPermissions(OrganizationDto org, PermissionQuery query, @Nullable Collection<String> logins, UserPermissionDto... expected) {
    // test method "select()"
    List<UserPermissionDto> permissions = underTest.select(dbSession, org.getUuid(), query, logins);
    assertThat(permissions).hasSize(expected.length);
    for (int i = 0; i < expected.length; i++) {
      UserPermissionDto got = permissions.get(i);
      UserPermissionDto expect = expected[i];
      assertThat(got.getUserId()).isEqualTo(expect.getUserId());
      assertThat(got.getPermission()).isEqualTo(expect.getPermission());
      assertThat(got.getComponentId()).isEqualTo(expect.getComponentId());
    }

    if (logins == null) {
      // test method "countUsers()", which does not make sense if users are filtered
      long distinctUsers = Arrays.stream(expected).mapToLong(p -> p.getUserId()).distinct().count();
      assertThat((long) underTest.countUsers(dbSession, org.getUuid(), query)).isEqualTo(distinctUsers);
    }
  }

  private UserPermissionDto addGlobalPermission(OrganizationDto org, String permission, UserDto user) {
    UserPermissionDto dto = new UserPermissionDto(org.getUuid(), permission, user.getId(), null);
    underTest.insert(dbSession, dto);
    dbTester.commit();
    return dto;
  }

  private UserPermissionDto addProjectPermission(OrganizationDto org, String permission, UserDto user, ComponentDto project) {
    UserPermissionDto dto = new UserPermissionDto(org.getUuid(), permission, user.getId(), project.getId());
    underTest.insert(dbSession, dto);
    dbTester.commit();
    return dto;
  }

  private void assertThatProjectPermissionDoesNotExist(UserDto user, String permission, ComponentDto project) {
    assertThat(dbTester.countSql(dbSession, "select count(id) from user_roles where role='" + permission + "' and user_id=" + user.getId() + " and resource_id=" + project.getId()))
      .isEqualTo(0);
  }

  private void assertThatProjectHasNoPermissions(ComponentDto project) {
    assertThat(dbTester.countSql(dbSession, "select count(id) from user_roles where resource_id=" + project.getId())).isEqualTo(0);
  }
}
