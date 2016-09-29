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
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.core.permission.GlobalPermissions.PROVISIONING;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.user.UserTesting.newUserDto;

public class UserPermissionDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private UserPermissionDao underTest = new UserPermissionDao();
  private UserDto user1 = newUserDto().setLogin("login1").setName("Marius").setActive(true);
  private UserDto user2 = newUserDto().setLogin("login2").setName("Marie").setActive(true);
  private UserDto user3 = newUserDto().setLogin("login3").setName("Bernard").setActive(true);
  private ComponentDto project1 = newProjectDto();
  private ComponentDto project2 = newProjectDto();

  @Before
  public void setUp() throws Exception {
    dbTester.getDbClient().userDao().insert(dbTester.getSession(), user1);
    dbTester.getDbClient().userDao().insert(dbTester.getSession(), user2);
    dbTester.getDbClient().userDao().insert(dbTester.getSession(), user3);
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), project1);
    dbTester.getDbClient().componentDao().insert(dbTester.getSession(), project2);
    dbTester.commit();
  }

  @Test
  public void select_global_permissions() {
    UserPermissionDto global1 = insertGlobalPermission(SYSTEM_ADMIN, user1.getId());
    UserPermissionDto global2 = insertGlobalPermission(SYSTEM_ADMIN, user2.getId());
    UserPermissionDto global3 = insertGlobalPermission(PROVISIONING, user2.getId());
    UserPermissionDto project1Perm = insertProjectPermission(USER, user3.getId(), this.project1.getId());

    // global permissions of users who has at least one global permission, ordered by user name then permission
    PermissionQuery query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, null, global2, global3, global1);

    // default query returns all permissions
    query = PermissionQuery.builder().build();
    expectPermissions(query, null, project1Perm, global2, global3, global1);

    // return empty list if non-null but empty logins
    expectPermissions(query, Collections.emptyList());

    // global permissions of user1
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList(user1.getLogin()), global1);

    // global permissions of user2
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList(user2.getLogin()), global2, global3);

    // global permissions of user1, user2 and another one
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList(user1.getLogin(), user2.getLogin(), "missing"), global2, global3, global1);

    // empty global permissions if login does not exist
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList("missing"));

    // empty global permissions if user does not have any
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList(user3.getLogin()));

    // user3 has no global permissions
    query = PermissionQuery.builder().withAtLeastOnePermission().build();
    expectPermissions(query, asList(user3.getLogin()));

    // global permissions "admin"
    query = PermissionQuery.builder().setPermission(SYSTEM_ADMIN).build();
    expectPermissions(query, null, global2, global1);

    // empty if nobody has the specified global permission
    query = PermissionQuery.builder().setPermission("missing").build();
    expectPermissions(query, null);

    // search by user name (matches 2 users)
    query = PermissionQuery.builder().withAtLeastOnePermission().setSearchQuery("Mari").build();
    expectPermissions(query, null, global2, global3, global1);

    // search by user name (matches 2 users) and global permission
    query = PermissionQuery.builder().setSearchQuery("Mari").setPermission(PROVISIONING).build();
    expectPermissions(query, null, global3);

    // search by user name (no match)
    query = PermissionQuery.builder().setSearchQuery("Unknown").build();
    expectPermissions(query, null);
  }

  @Test
  public void select_project_permissions() {
    insertGlobalPermission(SYSTEM_ADMIN, user1.getId());
    UserPermissionDto perm1 = insertProjectPermission(USER, user1.getId(), project1.getId());
    UserPermissionDto perm2 = insertProjectPermission(ISSUE_ADMIN, user1.getId(), project1.getId());
    UserPermissionDto perm3 = insertProjectPermission(ISSUE_ADMIN, user2.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user3.getId(), project2.getId());

    // project permissions of users who has at least one permission on this project
    PermissionQuery query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, null, perm3, perm2, perm1);

    // project permissions of user1
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList(user1.getLogin()), perm2, perm1);

    // project permissions of user2
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList(user2.getLogin()), perm3);

    // project permissions of user2 and another one
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList(user2.getLogin(), "missing"), perm3);

    // empty project permissions if login does not exist
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList("missing"));

    // empty project permissions if user does not have any
    query = PermissionQuery.builder().withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, asList(user3.getLogin()));

    // empty if nobody has the specified global permission
    query = PermissionQuery.builder().setPermission("missing").setComponentUuid(project1.uuid()).build();
    expectPermissions(query, null);

    // search by user name (matches 2 users), users with at least one permission
    query = PermissionQuery.builder().setSearchQuery("Mari").withAtLeastOnePermission().setComponentUuid(project1.uuid()).build();
    expectPermissions(query, null, perm3, perm2, perm1);

    // search by user name (matches 2 users) and project permission
    query = PermissionQuery.builder().setSearchQuery("Mari").setPermission(ISSUE_ADMIN).setComponentUuid(project1.uuid()).build();
    expectPermissions(query, null, perm3, perm2);

    // search by user name (no match)
    query = PermissionQuery.builder().setSearchQuery("Unknown").setComponentUuid(project1.uuid()).build();
    expectPermissions(query, null);

    // permissions of unknown project
    query = PermissionQuery.builder().setComponentUuid("missing").withAtLeastOnePermission().build();
    expectPermissions(query, null);
  }

  @Test
  public void countUsersByProjectPermission() {
    insertGlobalPermission(SYSTEM_ADMIN, user1.getId());
    insertProjectPermission(USER, user1.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user1.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user2.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user2.getId(), project2.getId());

    // no projects -> return empty list
    assertThat(underTest.countUsersByProjectPermission(dbTester.getSession(), Collections.emptyList())).isEmpty();

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
  public void selectLogins() {
    insertProjectPermission(USER, user1.getId(), project1.getId());
    insertProjectPermission(USER, user2.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user2.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user2.getId(), project2.getId());

    // logins are ordered by user name: user2 ("Marie") then user1 ("Marius")
    PermissionQuery query = PermissionQuery.builder().setComponentUuid(project1.uuid()).withAtLeastOnePermission().build();
    List<String> logins = underTest.selectLogins(dbTester.getSession(), query);
    assertThat(logins).containsExactly(user2.getLogin(), user1.getLogin());

    // on a project without permissions
    query = PermissionQuery.builder().setComponentUuid("missing").withAtLeastOnePermission().build();
    assertThat(underTest.selectLogins(dbTester.getSession(), query)).isEmpty();
  }

  @Test
  public void selectPermissionsByLogin() {
    insertGlobalPermission(SYSTEM_ADMIN, user1.getId());
    insertProjectPermission(USER, user1.getId(), project1.getId());
    insertProjectPermission(USER, user2.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user2.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user2.getId(), project2.getId());

    // user1 has one global permission and user2 has no global permissions
    assertThat(underTest.selectPermissionsByLogin(dbTester.getSession(), user1.getLogin(), null)).hasSize(1);
    assertThat(underTest.selectPermissionsByLogin(dbTester.getSession(), user2.getLogin(), null)).hasSize(0);

    // user1 has one permission on project1, user2 has 2
    assertThat(underTest.selectPermissionsByLogin(dbTester.getSession(), user1.getLogin(), project1.uuid())).hasSize(1);
    assertThat(underTest.selectPermissionsByLogin(dbTester.getSession(), user2.getLogin(), project1.uuid())).hasSize(2);

    // nobody has permissions on a project that does not exist!
    assertThat(underTest.selectPermissionsByLogin(dbTester.getSession(), user1.getLogin(), "missing")).hasSize(0);

    // users who do not exist don't have permissions!
    assertThat(underTest.selectPermissionsByLogin(dbTester.getSession(), "missing", null)).hasSize(0);
  }

  @Test
  public void delete_by_project() {
    insertGlobalPermission(SYSTEM_ADMIN, user1.getId());
    insertProjectPermission(USER, user1.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user2.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user2.getId(), project2.getId());

    underTest.delete(dbTester.getSession(), null, project1.uuid(), null);

    assertThat(dbTester.countSql(dbTester.getSession(), "select count(id) from user_roles where resource_id=" + project1.getId())).isEqualTo(0);
    // remains global permission and project2 permission
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), "user_roles")).isEqualTo(2);
  }

  @Test
  public void delete_by_user() {
    insertGlobalPermission(SYSTEM_ADMIN, user1.getId());
    insertProjectPermission(USER, user1.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user2.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user2.getId(), project2.getId());

    underTest.delete(dbTester.getSession(), user1.getLogin(), null, null);

    assertThat(dbTester.countSql(dbTester.getSession(), "select count(id) from user_roles where user_id=" + user1.getId())).isEqualTo(0);
    // remains user2 permissions
    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), "user_roles")).isEqualTo(2);
  }

  @Test
  public void delete_specific_permission() {
    insertGlobalPermission(SYSTEM_ADMIN, user1.getId());
    insertProjectPermission(USER, user1.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user2.getId(), project1.getId());
    insertProjectPermission(ISSUE_ADMIN, user2.getId(), project2.getId());

    underTest.delete(dbTester.getSession(), user1.getLogin(), project1.uuid(), USER);

    assertThat(dbTester.countRowsOfTable(dbTester.getSession(), "user_roles")).isEqualTo(3);
    assertThat(dbTester.countSql(dbTester.getSession(), "select count(id) from user_roles where user_id=" + user1.getId())).isEqualTo(1);
    assertThat(dbTester.countSql(dbTester.getSession(), "select count(id) from user_roles where role='" + SYSTEM_ADMIN + "' and user_id=" + user1.getId())).isEqualTo(1);
  }

  private void expectCount(List<Long> projectIds, CountPerProjectPermission... expected) {
    List<CountPerProjectPermission> got = underTest.countUsersByProjectPermission(dbTester.getSession(), projectIds);
    assertThat(got).hasSize(expected.length);

    for (CountPerProjectPermission expect : expected) {
      boolean found = got.stream().anyMatch(b -> b.getPermission().equals(expect.getPermission()) &&
        b.getCount() == expect.getCount() &&
        b.getComponentId() == expect.getComponentId());
      assertThat(found).isTrue();
    }
  }

  private void expectPermissions(PermissionQuery query, @Nullable Collection<String> logins, UserPermissionDto... expected) {
    // test method "select()"
    List<ExtendedUserPermissionDto> permissions = underTest.select(dbTester.getSession(), query, logins);
    assertThat(permissions).hasSize(expected.length);
    for (int i = 0; i < expected.length; i++) {
      ExtendedUserPermissionDto got = permissions.get(i);
      UserPermissionDto expect = expected[i];
      assertThat(got.getUserId()).isEqualTo(expect.getUserId());
      assertThat(got.getPermission()).isEqualTo(expect.getPermission());
      assertThat(got.getComponentId()).isEqualTo(expect.getComponentId());
      assertThat(got.getUserLogin()).isNotNull();
      if (got.getComponentId() != null) {
        assertThat(got.getComponentUuid()).isNotNull();
      }
    }

    if (logins == null) {
      // test method "countUsers()", which does not make sense if users are filtered
      long distinctUsers = Arrays.stream(expected).mapToLong(p -> p.getUserId()).distinct().count();
      assertThat((long) underTest.countUsers(dbTester.getSession(), query)).isEqualTo(distinctUsers);
    }
  }

  private UserPermissionDto insertGlobalPermission(String permission, long userId) {
    UserPermissionDto dto = new UserPermissionDto(permission, userId, null);
    underTest.insert(dbTester.getSession(), dto);
    dbTester.commit();
    return dto;
  }

  private UserPermissionDto insertProjectPermission(String permission, long userId, long projectId) {
    UserPermissionDto dto = new UserPermissionDto(permission, userId, projectId);
    underTest.insert(dbTester.getSession(), dto);
    dbTester.commit();
    return dto;
  }
}
