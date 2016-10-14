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

import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.organization.OrganizationTesting.newOrganizationDto;

public class AuthorizationDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbSession dbSession = db.getSession();
  private AuthorizationDao underTest = new AuthorizationDao();
  private OrganizationDto org;
  private UserDto user;
  private GroupDto group1;
  private GroupDto group2;

  @Before
  public void setUp() throws Exception {
    org = OrganizationTesting.insert(db, newOrganizationDto());
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

}
