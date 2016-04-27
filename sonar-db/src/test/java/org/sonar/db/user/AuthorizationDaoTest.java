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

import java.util.Collection;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;


public class AuthorizationDaoTest {

  private static final int USER = 100;
  private static final Long PROJECT_ID = 300L;
  private static final Long PROJECT_ID_WITHOUT_SNAPSHOT = 400L;
  private static final String PROJECT = "pj-w-snapshot";
  private static final String PROJECT_WIHOUT_SNAPSHOT = "pj-wo-snapshot";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  AuthorizationDao authorization = dbTester.getDbClient().authorizationDao();

  @Test
  public void user_should_be_authorized() {
    // but user is not in an authorized group
    dbTester.prepareDbUnit(getClass(), "user_should_be_authorized.xml");

    Collection<Long> componentIds = authorization.keepAuthorizedProjectIds(dbTester.getSession(),
      newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT);

    // user does not have the role "admin"
    componentIds = authorization.keepAuthorizedProjectIds(dbTester.getSession(),
      newHashSet(PROJECT_ID),
      USER, "admin");
    assertThat(componentIds).isEmpty();

    assertThat(authorization.keepAuthorizedProjectIds(dbTester.getSession(),
      Collections.<Long>emptySet(),
      USER, "admin")).isEmpty();
  }

  @Test
  public void keep_authorized_project_ids_for_user() {
    dbTester.prepareDbUnit(getClass(), "keep_authorized_project_ids_for_user.xml");

    assertThat(authorization.keepAuthorizedProjectIds(dbTester.getSession(), newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT), USER, "user")).containsOnly(PROJECT_ID);

    // user does not have the role "admin"
    assertThat(authorization.keepAuthorizedProjectIds(dbTester.getSession(), newHashSet(PROJECT_ID), USER, "admin")).isEmpty();

    // Empty list
    assertThat(authorization.keepAuthorizedProjectIds(dbTester.getSession(), Collections.<Long>emptySet(), USER, "admin")).isEmpty();
  }

  @Test
  public void keep_authorized_project_ids_for_group() {
    dbTester.prepareDbUnit(getClass(), "keep_authorized_project_ids_for_group.xml");

    assertThat(authorization.keepAuthorizedProjectIds(dbTester.getSession(), newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT), USER, "user")).containsOnly(PROJECT_ID);

    // user does not have the role "admin"
    assertThat(authorization.keepAuthorizedProjectIds(dbTester.getSession(), newHashSet(PROJECT_ID), USER, "admin")).isEmpty();

    // Empty list
    assertThat(authorization.keepAuthorizedProjectIds(dbTester.getSession(), Collections.<Long>emptySet(), USER, "admin")).isEmpty();
  }

  @Test
  public void keep_authorized_project_ids_for_anonymous() {
    dbTester.prepareDbUnit(getClass(), "keep_authorized_project_ids_for_anonymous.xml");

    assertThat(authorization.keepAuthorizedProjectIds(dbTester.getSession(), newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT), null, "user")).containsOnly(PROJECT_ID);

    // user does not have the role "admin"
    assertThat(authorization.keepAuthorizedProjectIds(dbTester.getSession(), newHashSet(PROJECT_ID), null, "admin")).isEmpty();

    // Empty list
    assertThat(authorization.keepAuthorizedProjectIds(dbTester.getSession(), Collections.<Long>emptySet(), null, "admin")).isEmpty();
  }

  @Test
  public void is_authorized_component_key_for_user() {
    dbTester.prepareDbUnit(getClass(), "keep_authorized_project_ids_for_user.xml");

    assertThat(authorization.isAuthorizedComponentKey(PROJECT, USER, "user")).isTrue();
    assertThat(authorization.isAuthorizedComponentKey(PROJECT_WIHOUT_SNAPSHOT, USER, "user")).isFalse();

    // user does not have the role "admin"
    assertThat(authorization.isAuthorizedComponentKey(PROJECT, USER, "admin")).isFalse();
  }

  @Test
  public void is_authorized_component_key_for_group() {
    dbTester.prepareDbUnit(getClass(), "keep_authorized_project_ids_for_group.xml");

    assertThat(authorization.isAuthorizedComponentKey(PROJECT, USER, "user")).isTrue();
    assertThat(authorization.isAuthorizedComponentKey(PROJECT_WIHOUT_SNAPSHOT, USER, "user")).isFalse();

    // user does not have the role "admin"
    assertThat(authorization.isAuthorizedComponentKey(PROJECT, USER, "admin")).isFalse();
  }

  @Test
  public void is_authorized_component_key_for_anonymous() {
    dbTester.prepareDbUnit(getClass(), "keep_authorized_project_ids_for_anonymous.xml");

    assertThat(authorization.isAuthorizedComponentKey(PROJECT, null, "user")).isTrue();
    assertThat(authorization.isAuthorizedComponentKey(PROJECT_WIHOUT_SNAPSHOT, null, "user")).isFalse();
    assertThat(authorization.isAuthorizedComponentKey(PROJECT, null, "admin")).isFalse();
  }

  @Test
  public void group_should_be_authorized() {
    // user is in an authorized group
    dbTester.prepareDbUnit(getClass(), "group_should_be_authorized.xml");

    Collection<Long> componentIds = authorization.keepAuthorizedProjectIds(dbTester.getSession(),
      newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedProjectIds(dbTester.getSession(),
      newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT),
      USER, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void anonymous_should_be_authorized() {
    dbTester.prepareDbUnit(getClass(), "anonymous_should_be_authorized.xml");

    Collection<Long> componentIds = authorization.keepAuthorizedProjectIds(dbTester.getSession(),
      newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT),
      null, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT);

    // group does not have the role "admin"
    componentIds = authorization.keepAuthorizedProjectIds(dbTester.getSession(),
      newHashSet(PROJECT_ID),
      null, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void should_return_root_project_keys_for_user() {
    dbTester.prepareDbUnit(getClass(), "should_return_root_project_keys_for_user.xml");

    Collection<String> rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(USER, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // user does not have the role "admin"
    rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(USER, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_keys_for_group() {
    // but user is not in an authorized group
    dbTester.prepareDbUnit(getClass(), "should_return_root_project_keys_for_group.xml");

    Collection<String> rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(USER, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // user does not have the role "admin"
    rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(USER, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_keys_for_anonymous() {
    dbTester.prepareDbUnit(getClass(), "should_return_root_project_keys_for_anonymous.xml");

    Collection<String> rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(null, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // group does not have the role "admin"
    rootProjectIds = authorization.selectAuthorizedRootProjectsKeys(null, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_uuids_for_user() {
    dbTester.prepareDbUnit(getClass(), "should_return_root_project_keys_for_user.xml");

    Collection<String> rootProjectUuids = authorization.selectAuthorizedRootProjectsUuids(USER, "user");

    assertThat(rootProjectUuids).containsOnly("ABCD");

    // user does not have the role "admin"
    rootProjectUuids = authorization.selectAuthorizedRootProjectsKeys(USER, "admin");
    assertThat(rootProjectUuids).isEmpty();
  }

  @Test
  public void should_return_root_project_uuids_for_group() {
    // but user is not in an authorized group
    dbTester.prepareDbUnit(getClass(), "should_return_root_project_keys_for_group.xml");

    Collection<String> rootProjectUuids = authorization.selectAuthorizedRootProjectsUuids(USER, "user");

    assertThat(rootProjectUuids).containsOnly("ABCD");

    // user does not have the role "admin"
    rootProjectUuids = authorization.selectAuthorizedRootProjectsKeys(USER, "admin");
    assertThat(rootProjectUuids).isEmpty();
  }

  @Test
  public void should_return_root_project_uuids_for_anonymous() {
    dbTester.prepareDbUnit(getClass(), "should_return_root_project_keys_for_anonymous.xml");

    Collection<String> rootProjectUuids = authorization.selectAuthorizedRootProjectsUuids(null, "user");

    assertThat(rootProjectUuids).containsOnly("ABCD");

    // group does not have the role "admin"
    rootProjectUuids = authorization.selectAuthorizedRootProjectsKeys(null, "admin");
    assertThat(rootProjectUuids).isEmpty();
  }

  @Test
  public void should_return_user_global_permissions() {
    dbTester.prepareDbUnit(getClass(), "should_return_user_global_permissions.xml");

    assertThat(authorization.selectGlobalPermissions("john")).containsOnly("user", "admin");
    assertThat(authorization.selectGlobalPermissions("arthur")).containsOnly("user");
    assertThat(authorization.selectGlobalPermissions("none")).isEmpty();
  }

  @Test
  public void should_return_group_global_permissions() {
    dbTester.prepareDbUnit(getClass(), "should_return_group_global_permissions.xml");

    assertThat(authorization.selectGlobalPermissions("john")).containsOnly("user", "admin");
    assertThat(authorization.selectGlobalPermissions("arthur")).containsOnly("user");
    assertThat(authorization.selectGlobalPermissions("none")).isEmpty();
  }

  @Test
  public void should_return_global_permissions_for_anonymous() {
    dbTester.prepareDbUnit(getClass(), "should_return_global_permissions_for_anonymous.xml");

    assertThat(authorization.selectGlobalPermissions(null)).containsOnly("user", "admin");
  }

  @Test
  public void should_return_global_permissions_for_group_anyone() {
    dbTester.prepareDbUnit(getClass(), "should_return_global_permissions_for_group_anyone.xml");

    assertThat(authorization.selectGlobalPermissions("anyone_user")).containsOnly("user", "profileadmin");
  }

  @Test
  public void keep_authorized_users_for_role_and_project_for_user() {
    dbTester.prepareDbUnit(getClass(), "keep_authorized_users_for_role_and_project_for_user.xml");

    assertThat(authorization.keepAuthorizedUsersForRoleAndProject(dbTester.getSession(),
      // Only 100 and 101 has 'user' role on project
      newHashSet(100L, 101L, 102L), "user", PROJECT_ID)).containsOnly(100L, 101L);

    assertThat(authorization.keepAuthorizedUsersForRoleAndProject(dbTester.getSession(),
      // Only 100 and 101 has 'user' role on project
      newHashSet(100L), "user", PROJECT_ID)).containsOnly(100L);

    // user does not have the role "admin"
    assertThat(authorization.keepAuthorizedUsersForRoleAndProject(dbTester.getSession(), newHashSet(100L), "admin", PROJECT_ID)).isEmpty();

    // Empty list
    assertThat(authorization.keepAuthorizedUsersForRoleAndProject(dbTester.getSession(), Collections.<Long>emptySet(), "user", PROJECT_ID)).isEmpty();
  }

  @Test
  public void keep_authorized_users_for_role_and_project_for_group() {
    dbTester.prepareDbUnit(getClass(), "keep_authorized_users_for_role_and_project_for_group.xml");

    assertThat(authorization.keepAuthorizedUsersForRoleAndProject(dbTester.getSession(),
      // Only 100 and 101 has 'user' role on project
      newHashSet(100L, 101L, 102L), "user", PROJECT_ID)).containsOnly(100L, 101L);

    assertThat(authorization.keepAuthorizedUsersForRoleAndProject(dbTester.getSession(),
      newHashSet(100L), "user", PROJECT_ID)).containsOnly(100L);

    // user does not have the role "admin"
    assertThat(authorization.keepAuthorizedUsersForRoleAndProject(dbTester.getSession(), newHashSet(100L), "admin", PROJECT_ID)).isEmpty();

    // Empty list
    assertThat(authorization.keepAuthorizedUsersForRoleAndProject(dbTester.getSession(), Collections.<Long>emptySet(), "user", PROJECT_ID)).isEmpty();
  }

  @Test
  public void keep_authorized_users_returns_empty_list_for_role_and_project_for_anonymous() {
    dbTester.prepareDbUnit(getClass(), "keep_authorized_users_for_role_and_project_for_anonymous.xml");

    assertThat(authorization.keepAuthorizedUsersForRoleAndProject(dbTester.getSession(),
      // Only 100 and 101 has 'user' role on project
      newHashSet(100L, 101L, 102L), "user", PROJECT_ID)).isEmpty();
  }

}
