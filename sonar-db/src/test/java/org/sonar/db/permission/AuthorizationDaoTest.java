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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthorizationDaoTest {

  private static final int USER = 100;
  private static final Long PROJECT_ID = 300L;
  private static final Long PROJECT_ID_WITHOUT_SNAPSHOT = 400L;
  private static final String PROJECT = "pj-w-snapshot";
  private static final String PROJECT_WIHOUT_SNAPSHOT = "pj-wo-snapshot";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbSession dbSession = db.getSession();
  private AuthorizationDao underTest = new AuthorizationDao(db.myBatis());
  private OrganizationDto org;
  private UserDto user;
  private GroupDto group1;
  private GroupDto group2;

  @Before
  public void setUp() throws Exception {
    org = db.organizations().insert();
    user = db.users().insertUser();
    group1 = db.users().insertGroup(org, "group1");
    group2 = db.users().insertGroup(org, "group2");
  }

  /**
   * Union of the permissions granted to:
   * - the user
   * - the groups which user is member
   * - anyone
   */
  @Test
  public void selectOrganizationPermissions_for_logged_in_user() {
    db.users().insertMember(group1, user);
    db.users().insertPermissionOnUser(org, user, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertPermissionOnAnyone(org, "perm3");

    // ignored permissions, user is not member of this group
    db.users().insertPermissionOnGroup(group2, "ignored");

    Set<String> permissions = underTest.selectOrganizationPermissions(dbSession, org.getUuid(), user.getId());

    assertThat(permissions).containsOnly("perm1", "perm2", "perm3");
  }

  /**
   * Anonymous user only benefits from the permissions granted to
   * "Anyone"
   */
  @Test
  public void selectOrganizationPermissions_for_anonymous_user() {
    db.users().insertPermissionOnAnyone(org, "perm1");

    // ignored permissions
    db.users().insertPermissionOnUser(org, user, "ignored");
    db.users().insertPermissionOnGroup(group1, "ignored");

    Set<String> permissions = underTest.selectOrganizationPermissionsOfAnonymous(dbSession, org.getUuid());

    assertThat(permissions).containsOnly("perm1");
  }

  /**
   * Union of the permissions granted to:
   * - the user
   * - the groups which user is member
   * - anyone
   */
  @Test
  public void selectRootComponentPermissions_for_logged_in_user() {
    db.users().insertMember(group1, user);
    ComponentDto project1 = db.components().insertProject();
    db.users().insertProjectPermissionOnAnyone("perm1", project1);
    db.users().insertProjectPermissionOnGroup(group1, "perm2", project1);
    db.users().insertProjectPermissionOnUser(user, "perm3", project1);

    // ignored permissions
    db.users().insertPermissionOnAnyone(org, "ignored");
    db.users().insertPermissionOnGroup(group2, "ignored");
    ComponentDto project2 = db.components().insertProject();

    Set<String> permissions = underTest.selectRootComponentPermissions(dbSession, project1.getId(), user.getId());
    assertThat(permissions).containsOnly("perm1", "perm2", "perm3");

    // non granted project
    permissions = underTest.selectRootComponentPermissions(dbSession, project2.getId(), user.getId());
    assertThat(permissions).isEmpty();
  }

  /**
   * Anonymous user only benefits from the permissions granted to
   * "Anyone"
   */
  @Test
  public void selectRootComponentPermissions_for_anonymous_user() {
    ComponentDto project1 = db.components().insertProject();
    db.users().insertProjectPermissionOnAnyone("perm1", project1);

    // ignored permissions
    db.users().insertPermissionOnAnyone(org, "ignored");
    db.users().insertPermissionOnUser(org, user, "ignored");
    db.users().insertPermissionOnGroup(group1, "ignored");
    ComponentDto project2 = db.components().insertProject();
    db.users().insertProjectPermissionOnGroup(group1, "ignored", project2);

    Set<String> permissions = underTest.selectRootComponentPermissionsOfAnonymous(dbSession, project1.getId());
    assertThat(permissions).containsOnly("perm1");

    // non granted project
    permissions = underTest.selectRootComponentPermissionsOfAnonymous(dbSession, project2.getId());
    assertThat(permissions).isEmpty();
  }

  @Test
  public void user_should_be_authorized() {
    // but user is not in an authorized group
    db.prepareDbUnit(getClass(), "user_should_be_authorized.xml");

    Collection<Long> componentIds = underTest.keepAuthorizedProjectIds(dbSession,
      newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT);

    // user does not have the role "admin"
    componentIds = underTest.keepAuthorizedProjectIds(dbSession,
      newHashSet(PROJECT_ID),
      USER, "admin");
    assertThat(componentIds).isEmpty();

    assertThat(underTest.keepAuthorizedProjectIds(dbSession,
      Collections.emptySet(),
      USER, "admin")).isEmpty();
  }

  @Test
  public void countRemainingUserIdsWithGlobalPermissionIfExcludeGroup() {
    // users with global permission "perm1" :
    // - "u1" and "u2" through group "g1"
    // - "u1" and "u3" through group "g2"
    // - "u4"

    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    UserDto user4 = db.users().insertUser();
    UserDto user5 = db.users().insertUser();

    OrganizationDto org = db.organizations().insert();
    GroupDto group1 = db.users().insertGroup(org, "g1");
    db.users().insertPermissionOnGroup(group1, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertMember(group1, user1);
    db.users().insertMember(group1, user2);

    GroupDto group2 = db.users().insertGroup(org, "g2");
    db.users().insertPermissionOnGroup(group2, "perm1");
    db.users().insertPermissionOnGroup(group2, "perm2");
    db.users().insertMember(group2, user1);
    db.users().insertMember(group2, user3);

    // group3 has the permission "perm1" but has no users
    GroupDto group3 = db.users().insertGroup(org, "g2");
    db.users().insertPermissionOnGroup(group3, "perm1");

    db.users().insertPermissionOnUser(user4, "perm1");
    db.users().insertPermissionOnUser(user4, "perm2");

    db.users().insertPermissionOnAnyone(org, "perm1");

    // excluding group "g1" -> remain u1, u3 and u4
    assertThat(underTest.countRemainingUserIdsWithGlobalPermissionIfExcludeGroup(db.getSession(),
      org.getUuid(), "perm1", group1.getId())).isEqualTo(3);

    // excluding group "g2" -> remain u1, u2 and u4
    assertThat(underTest.countRemainingUserIdsWithGlobalPermissionIfExcludeGroup(db.getSession(),
      org.getUuid(), "perm1", group2.getId())).isEqualTo(3);

    // excluding group "g3" -> remain u1, u2, u3 and u4
    assertThat(underTest.countRemainingUserIdsWithGlobalPermissionIfExcludeGroup(db.getSession(),
      org.getUuid(), "perm1", group3.getId())).isEqualTo(4);

    // nobody has the permission
    assertThat(underTest.countRemainingUserIdsWithGlobalPermissionIfExcludeGroup(db.getSession(),
      org.getUuid(), "missingPermission", group1.getId())).isEqualTo(0);

  }

  @Test
  public void keep_authorized_project_ids_for_user() {
    db.prepareDbUnit(getClass(), "keep_authorized_project_ids_for_user.xml");

    assertThat(underTest.keepAuthorizedProjectIds(dbSession, newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT), USER, "user")).containsOnly(PROJECT_ID);

    // user does not have the role "admin"
    assertThat(underTest.keepAuthorizedProjectIds(dbSession, newHashSet(PROJECT_ID), USER, "admin")).isEmpty();

    // Empty list
    assertThat(underTest.keepAuthorizedProjectIds(dbSession, Collections.emptySet(), USER, "admin")).isEmpty();
  }

  @Test
  public void keep_authorized_project_ids_for_group() {
    db.prepareDbUnit(getClass(), "keep_authorized_project_ids_for_group.xml");

    assertThat(underTest.keepAuthorizedProjectIds(dbSession, newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT), USER, "user")).containsOnly(PROJECT_ID);

    // user does not have the role "admin"
    assertThat(underTest.keepAuthorizedProjectIds(dbSession, newHashSet(PROJECT_ID), USER, "admin")).isEmpty();

    // Empty list
    assertThat(underTest.keepAuthorizedProjectIds(dbSession, Collections.emptySet(), USER, "admin")).isEmpty();
  }

  @Test
  public void keep_authorized_project_ids_for_anonymous() {
    db.prepareDbUnit(getClass(), "keep_authorized_project_ids_for_anonymous.xml");

    assertThat(underTest.keepAuthorizedProjectIds(dbSession, newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT), null, "user")).containsOnly(PROJECT_ID);

    // user does not have the role "admin"
    assertThat(underTest.keepAuthorizedProjectIds(dbSession, newHashSet(PROJECT_ID), null, "admin")).isEmpty();

    // Empty list
    assertThat(underTest.keepAuthorizedProjectIds(dbSession, Collections.emptySet(), null, "admin")).isEmpty();
  }

  @Test
  public void group_should_be_authorized() {
    // user is in an authorized group
    db.prepareDbUnit(getClass(), "group_should_be_authorized.xml");

    Collection<Long> componentIds = underTest.keepAuthorizedProjectIds(dbSession,
      newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT),
      USER, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT);

    // group does not have the role "admin"
    componentIds = underTest.keepAuthorizedProjectIds(dbSession,
      newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT),
      USER, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void anonymous_should_be_authorized() {
    db.prepareDbUnit(getClass(), "anonymous_should_be_authorized.xml");

    Collection<Long> componentIds = underTest.keepAuthorizedProjectIds(dbSession,
      newHashSet(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT),
      null, "user");

    assertThat(componentIds).containsOnly(PROJECT_ID, PROJECT_ID_WITHOUT_SNAPSHOT);

    // group does not have the role "admin"
    componentIds = underTest.keepAuthorizedProjectIds(dbSession,
      newHashSet(PROJECT_ID),
      null, "admin");
    assertThat(componentIds).isEmpty();
  }

  @Test
  public void should_return_root_project_keys_for_user() {
    db.prepareDbUnit(getClass(), "should_return_root_project_keys_for_user.xml");

    Collection<String> rootProjectIds = underTest.selectAuthorizedRootProjectsKeys(dbSession, USER, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // user does not have the role "admin"
    rootProjectIds = underTest.selectAuthorizedRootProjectsKeys(dbSession, USER, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_keys_for_group() {
    // but user is not in an authorized group
    db.prepareDbUnit(getClass(), "should_return_root_project_keys_for_group.xml");

    Collection<String> rootProjectIds = underTest.selectAuthorizedRootProjectsKeys(dbSession, USER, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // user does not have the role "admin"
    rootProjectIds = underTest.selectAuthorizedRootProjectsKeys(dbSession, USER, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_keys_for_anonymous() {
    db.prepareDbUnit(getClass(), "should_return_root_project_keys_for_anonymous.xml");

    Collection<String> rootProjectIds = underTest.selectAuthorizedRootProjectsKeys(dbSession, null, "user");

    assertThat(rootProjectIds).containsOnly(PROJECT);

    // group does not have the role "admin"
    rootProjectIds = underTest.selectAuthorizedRootProjectsKeys(dbSession, null, "admin");
    assertThat(rootProjectIds).isEmpty();
  }

  @Test
  public void should_return_root_project_uuids_for_user() {
    db.prepareDbUnit(getClass(), "should_return_root_project_keys_for_user.xml");

    Collection<String> rootProjectUuids = underTest.selectAuthorizedRootProjectsUuids(dbSession, USER, "user");

    assertThat(rootProjectUuids).containsOnly("ABCD");

    // user does not have the role "admin"
    rootProjectUuids = underTest.selectAuthorizedRootProjectsKeys(dbSession, USER, "admin");
    assertThat(rootProjectUuids).isEmpty();
  }

  @Test
  public void should_return_root_project_uuids_for_group() {
    // but user is not in an authorized group
    db.prepareDbUnit(getClass(), "should_return_root_project_keys_for_group.xml");

    Collection<String> rootProjectUuids = underTest.selectAuthorizedRootProjectsUuids(dbSession, USER, "user");

    assertThat(rootProjectUuids).containsOnly("ABCD");

    // user does not have the role "admin"
    rootProjectUuids = underTest.selectAuthorizedRootProjectsKeys(dbSession, USER, "admin");
    assertThat(rootProjectUuids).isEmpty();
  }

  @Test
  public void should_return_root_project_uuids_for_anonymous() {
    db.prepareDbUnit(getClass(), "should_return_root_project_keys_for_anonymous.xml");

    Collection<String> rootProjectUuids = underTest.selectAuthorizedRootProjectsUuids(dbSession, null, "user");

    assertThat(rootProjectUuids).containsOnly("ABCD");

    // group does not have the role "admin"
    rootProjectUuids = underTest.selectAuthorizedRootProjectsKeys(dbSession, null, "admin");
    assertThat(rootProjectUuids).isEmpty();
  }

  @Test
  public void should_return_user_global_permissions() {
    db.prepareDbUnit(getClass(), "should_return_user_global_permissions.xml");

    assertThat(underTest.selectGlobalPermissions("john")).containsOnly("user", "admin");
    assertThat(underTest.selectGlobalPermissions("arthur")).containsOnly("user");
    assertThat(underTest.selectGlobalPermissions("none")).isEmpty();
  }

  @Test
  public void should_return_group_global_permissions() {
    db.prepareDbUnit(getClass(), "should_return_group_global_permissions.xml");

    assertThat(underTest.selectGlobalPermissions("john")).containsOnly("user", "admin");
    assertThat(underTest.selectGlobalPermissions("arthur")).containsOnly("user");
    assertThat(underTest.selectGlobalPermissions("none")).isEmpty();
  }

  @Test
  public void should_return_global_permissions_for_anonymous() {
    db.prepareDbUnit(getClass(), "should_return_global_permissions_for_anonymous.xml");

    assertThat(underTest.selectGlobalPermissions(null)).containsOnly("user", "admin");
  }

  @Test
  public void should_return_global_permissions_for_group_anyone() {
    db.prepareDbUnit(getClass(), "should_return_global_permissions_for_group_anyone.xml");

    assertThat(underTest.selectGlobalPermissions("anyone_user")).containsOnly("user", "profileadmin");
  }

  @Test
  public void is_authorized_component_key_for_user() {
    db.prepareDbUnit(getClass(), "keep_authorized_project_ids_for_user.xml");

    assertThat(underTest.isAuthorizedComponentKey(PROJECT, USER, "user")).isTrue();
    assertThat(underTest.isAuthorizedComponentKey(PROJECT_WIHOUT_SNAPSHOT, USER, "user")).isFalse();

    // user does not have the role "admin"
    assertThat(underTest.isAuthorizedComponentKey(PROJECT, USER, "admin")).isFalse();
  }

  @Test
  public void is_authorized_component_key_for_group() {
    db.prepareDbUnit(getClass(), "keep_authorized_project_ids_for_group.xml");

    assertThat(underTest.isAuthorizedComponentKey(PROJECT, USER, "user")).isTrue();
    assertThat(underTest.isAuthorizedComponentKey(PROJECT_WIHOUT_SNAPSHOT, USER, "user")).isFalse();

    // user does not have the role "admin"
    assertThat(underTest.isAuthorizedComponentKey(PROJECT, USER, "admin")).isFalse();
  }

  @Test
  public void is_authorized_component_key_for_anonymous() {
    db.prepareDbUnit(getClass(), "keep_authorized_project_ids_for_anonymous.xml");

    assertThat(underTest.isAuthorizedComponentKey(PROJECT, null, "user")).isTrue();
    assertThat(underTest.isAuthorizedComponentKey(PROJECT_WIHOUT_SNAPSHOT, null, "user")).isFalse();
    assertThat(underTest.isAuthorizedComponentKey(PROJECT, null, "admin")).isFalse();
  }

  @Test
  public void keep_authorized_users_for_role_and_project_for_user() {
    db.prepareDbUnit(getClass(), "keep_authorized_users_for_role_and_project_for_user.xml");

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession,
      // Only 100 and 101 has 'user' role on project
      newHashSet(100L, 101L, 102L), "user", PROJECT_ID)).containsOnly(100L, 101L);

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession,
      // Only 100 and 101 has 'user' role on project
      newHashSet(100L), "user", PROJECT_ID)).containsOnly(100L);

    // user does not have the role "admin"
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, newHashSet(100L), "admin", PROJECT_ID)).isEmpty();

    // Empty list
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, Collections.emptySet(), "user", PROJECT_ID)).isEmpty();
  }

  @Test
  public void keep_authorized_users_for_role_and_project_for_group() {
    db.prepareDbUnit(getClass(), "keep_authorized_users_for_role_and_project_for_group.xml");

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession,
      // Only 100 and 101 has 'user' role on project
      newHashSet(100L, 101L, 102L), "user", PROJECT_ID)).containsOnly(100L, 101L);

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession,
      newHashSet(100L), "user", PROJECT_ID)).containsOnly(100L);

    // user does not have the role "admin"
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, newHashSet(100L), "admin", PROJECT_ID)).isEmpty();

    // Empty list
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, Collections.emptySet(), "user", PROJECT_ID)).isEmpty();
  }

  @Test
  public void keep_authorized_users_returns_empty_list_for_role_and_project_for_anonymous() {
    db.prepareDbUnit(getClass(), "keep_authorized_users_for_role_and_project_for_anonymous.xml");

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession,
      // Only 100 and 101 has 'user' role on project
      newHashSet(100L, 101L, 102L), "user", PROJECT_ID)).isEmpty();
  }
}
