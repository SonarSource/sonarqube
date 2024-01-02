/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.db.permission;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.EmailSubscriberDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.permission.GlobalPermissions.QUALITY_PROFILE_ADMIN;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class AuthorizationDaoTest {

  private static final String PROJECT_UUID = "uuid";
  private static final int MISSING_ID = -1;
  private static final String MISSING_UUID = "unknown";
  private static final String A_PERMISSION = "a-permission";
  private static final String DOES_NOT_EXIST = "does-not-exist";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private final Random random = new Random();
  private DbSession dbSession = db.getSession();
  private AuthorizationDao underTest = new AuthorizationDao();
  private UserDto user;
  private GroupDto group1;
  private GroupDto group2;
  private Set<String> randomPublicProjectUuids;
  private Set<String> randomPrivateProjectUuids;
  private Set<String> randomExistingUserUuids;
  private String randomPermission = "p" + random.nextInt();

  @Before
  public void setUp() {
    user = db.users().insertUser();
    group1 = db.users().insertGroup("group1");
    group2 = db.users().insertGroup("group2");
    randomExistingUserUuids = IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .mapToObj(i -> db.users().insertUser().getUuid())
      .collect(MoreCollectors.toSet());
    randomPublicProjectUuids = IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .mapToObj(i -> db.components().insertPublicProject().uuid())
      .collect(MoreCollectors.toSet());
    randomPrivateProjectUuids = IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .mapToObj(i -> db.components().insertPrivateProject().uuid())
      .collect(MoreCollectors.toSet());
  }

  /**
   * Union of the permissions granted to:
   * - the user
   * - the groups which user is member
   * - anyone
   */
  @Test
  public void selectGlobalPermissions_for_logged_in_user() {
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertMember(group1, user);
    db.users().insertPermissionOnUser(user, "perm1");
    db.users().insertProjectPermissionOnUser(user, "perm42", project);
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertPermissionOnAnyone("perm3");

    // ignored permissions, user is not member of this group
    db.users().insertPermissionOnGroup(group2, "ignored");

    Set<String> permissions = underTest.selectGlobalPermissions(dbSession, user.getUuid());

    assertThat(permissions).containsOnly("perm1", "perm2", "perm3");
  }

  @Test
  public void countUsersWithGlobalPermissionExcludingGroup() {
    // users with global permission "perm1" :
    // - "u1" and "u2" through group "g1"
    // - "u1" and "u3" through group "g2"
    // - "u4"

    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();
    UserDto user4 = db.users().insertUser();
    UserDto user5 = db.users().insertUser();

    GroupDto group1 = db.users().insertGroup("g1");
    db.users().insertPermissionOnGroup(group1, "perm1");
    db.users().insertPermissionOnGroup(group1, "perm2");
    db.users().insertMember(group1, user1);
    db.users().insertMember(group1, user2);

    GroupDto group2 = db.users().insertGroup("g2");
    db.users().insertPermissionOnGroup(group2, "perm1");
    db.users().insertPermissionOnGroup(group2, "perm2");
    db.users().insertMember(group2, user1);
    db.users().insertMember(group2, user3);

    // group3 has the permission "perm1" but has no users
    GroupDto group3 = db.users().insertGroup("g3");
    db.users().insertPermissionOnGroup(group3, "perm1");

    db.users().insertPermissionOnUser(user4, "perm1");
    db.users().insertPermissionOnUser(user4, "perm2");
    db.users().insertPermissionOnAnyone("perm1");

    // excluding group "g1" -> remain u1, u3 and u4
    assertThat(underTest.countUsersWithGlobalPermissionExcludingGroup(db.getSession(),
      "perm1", group1.getUuid())).isEqualTo(3);

    // excluding group "g2" -> remain u1, u2 and u4
    assertThat(underTest.countUsersWithGlobalPermissionExcludingGroup(db.getSession(),
      "perm1", group2.getUuid())).isEqualTo(3);

    // excluding group "g3" -> remain u1, u2, u3 and u4
    assertThat(underTest.countUsersWithGlobalPermissionExcludingGroup(db.getSession(),
      "perm1", group3.getUuid())).isEqualTo(4);

    // nobody has the permission
    assertThat(underTest.countUsersWithGlobalPermissionExcludingGroup(db.getSession(),
      "missingPermission", group1.getUuid())).isZero();
  }

  @Test
  public void countUsersWithGlobalPermissionExcludingUser() {
    // group g1 has the permission p1 and has members user1 and user2
    // user3 has the permission
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();

    GroupDto group1 = db.users().insertGroup("g1");
    db.users().insertPermissionOnGroup(group1, "p1");
    db.users().insertPermissionOnGroup(group1, "p2");
    db.users().insertMember(group1, user1);
    db.users().insertMember(group1, user2);
    db.users().insertPermissionOnUser(user3, "p1");
    db.users().insertPermissionOnAnyone("p1");

    // excluding user1 -> remain user2 and user3
    assertThat(underTest.countUsersWithGlobalPermissionExcludingUser(db.getSession(),
      "p1", user1.getUuid())).isEqualTo(2);

    // excluding user3 -> remain the members of group g1
    assertThat(underTest.countUsersWithGlobalPermissionExcludingUser(db.getSession(),
      "p1", user3.getUuid())).isEqualTo(2);

    // excluding unknown user
    assertThat(underTest.countUsersWithGlobalPermissionExcludingUser(db.getSession(),
      "p1", "-1")).isEqualTo(3);

    // nobody has the permission
    assertThat(underTest.countUsersWithGlobalPermissionExcludingUser(db.getSession(),
      "missingPermission", user1.getUuid())).isZero();
  }

  @Test
  public void selectUserUuidsWithGlobalPermission() {
    // group g1 has the permission p1 and has members user1 and user2
    // user3 has the permission
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserDto user3 = db.users().insertUser();

    GroupDto group1 = db.users().insertGroup();
    db.users().insertPermissionOnGroup(group1, ADMINISTER);
    db.users().insertPermissionOnGroup(group1, PROVISION_PROJECTS);
    db.users().insertMember(group1, user1);
    db.users().insertMember(group1, user2);
    db.users().insertPermissionOnUser(user3, ADMINISTER);
    db.users().insertPermissionOnAnyone(ADMINISTER);

    assertThat(underTest.selectUserUuidsWithGlobalPermission(db.getSession(), ADMINISTER.getKey()))
      .containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid(), user3.getUuid());
    assertThat(underTest.selectUserUuidsWithGlobalPermission(db.getSession(), PROVISION_PROJECTS.getKey()))
      .containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid());
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_for_group_AnyOne_if_project_set_is_empty_on_public_project() {
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, Collections.emptySet(), null, UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_for_user_if_project_set_is_empty_on_public_project() {
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, Collections.emptySet(), user.getUuid(), UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_for_group_AnyOne_for_non_existent_projects() {
    Set<String> randomNonProjectsSet = IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .mapToObj(i -> Integer.toString(3_562 + i))
      .collect(MoreCollectors.toSet());

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomNonProjectsSet, null, UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_for_user_for_non_existent_projects() {
    Set<String> randomNonProjectsSet = IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .mapToObj(i -> Integer.toString(9_666 + i))
      .collect(MoreCollectors.toSet());

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomNonProjectsSet, user.getUuid(), UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_any_public_project_for_group_AnyOne_without_any_permission_in_DB_and_permission_USER() {
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPublicProjectUuids, null, UserRole.USER))
      .containsAll(randomPublicProjectUuids);
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_any_public_project_for_user_without_any_permission_in_DB_and_permission_USER() {
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPublicProjectUuids, user.getUuid(), UserRole.USER))
      .containsAll(randomPublicProjectUuids);
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_any_public_project_for_group_AnyOne_without_any_permission_in_DB_and_permission_CODEVIEWER() {
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPublicProjectUuids, null, UserRole.CODEVIEWER))
      .containsAll(randomPublicProjectUuids);
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_any_public_project_for_user_without_any_permission_in_DB_and_permission_CODEVIEWER() {
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPublicProjectUuids, user.getUuid(), UserRole.CODEVIEWER))
      .containsAll(randomPublicProjectUuids);
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_for_other_permission_for_group_AnyOne_on_public_project_without_any_permission_in_DB() {
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPublicProjectUuids, null, randomPermission))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_for_any_permission_for_user_on_public_project_without_any_permission_in_DB() {
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPublicProjectUuids, user.getUuid(), randomPermission))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_public_project_if_user_is_granted_project_permission_directly() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto otherProject = db.components().insertPublicProject();
    UserDto otherUser = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, randomPermission, project);

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), otherUser.getUuid(), randomPermission))
      .isEmpty();
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(otherProject.uuid()), user.getUuid(), randomPermission))
      .isEmpty();
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), user.getUuid(), randomPermission))
      .containsOnly(project.uuid());
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), user.getUuid(), "another perm"))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_public_project_if_user_is_granted_project_permission_by_group() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto otherProject = db.components().insertPublicProject();
    UserDto otherUser = db.users().insertUser();
    db.users().insertMember(group1, user);
    db.users().insertProjectPermissionOnGroup(group1, randomPermission, project);

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), user.getUuid(), randomPermission))
      .containsOnly(project.uuid());
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(otherProject.uuid()), user.getUuid(), randomPermission))
      .isEmpty();
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), otherUser.getUuid(), randomPermission))
      .isEmpty();
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), user.getUuid(), "another perm"))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_public_project_if_group_AnyOne_is_granted_project_permission_directly() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto otherProject = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnAnyone(randomPermission, project);

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), null, randomPermission))
      .containsOnly(project.uuid());
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), null, "another perm"))
      .isEmpty();
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(otherProject.uuid()), null, randomPermission))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_for_user_on_private_project_without_any_permission_in_DB_and_permission_USER() {
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPrivateProjectUuids, user.getUuid(), UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_for_group_AnyOne_on_private_project_without_any_permission_in_DB_and_permission_USER() {
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPrivateProjectUuids, null, UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_for_user_on_private_project_without_any_permission_in_DB_and_permission_CODEVIEWER() {
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPrivateProjectUuids, user.getUuid(), UserRole.CODEVIEWER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_for_group_AnyOne_on_private_project_without_any_permission_in_DB_and_permission_CODEVIEWER() {
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPrivateProjectUuids, null, UserRole.CODEVIEWER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_for_user_and_any_permission_on_private_project_without_any_permission_in_DB() {
    PermissionsTestHelper.ALL_PERMISSIONS
      .forEach(perm -> {
        assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPrivateProjectUuids, user.getUuid(), perm))
          .isEmpty();
      });
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPrivateProjectUuids, user.getUuid(), randomPermission))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_for_group_AnyOne_and_any_permission_on_private_project_without_any_permission_in_DB() {
    PermissionsTestHelper.ALL_PERMISSIONS
      .forEach(perm -> {
        assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPrivateProjectUuids, null, perm))
          .isEmpty();
      });
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, randomPrivateProjectUuids, null, randomPermission))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_private_project_if_user_is_granted_project_permission_directly() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto otherProject = db.components().insertPrivateProject();
    UserDto otherUser = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, randomPermission, project);

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), user.getUuid(), randomPermission))
      .containsOnly(project.uuid());
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), user.getUuid(), "another perm"))
      .isEmpty();
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(otherProject.uuid()), user.getUuid(), randomPermission))
      .isEmpty();
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), otherUser.getUuid(), randomPermission))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_private_project_if_user_is_granted_project_permission_by_group() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto otherProject = db.components().insertPrivateProject();
    UserDto otherUser = db.users().insertUser();
    db.users().insertMember(group1, user);
    db.users().insertProjectPermissionOnGroup(group1, randomPermission, project);

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), user.getUuid(), randomPermission))
      .containsOnly(project.uuid());
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), user.getUuid(), "another perm"))
      .isEmpty();
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(otherProject.uuid()), user.getUuid(), randomPermission))
      .isEmpty();
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, singleton(project.uuid()), otherUser.getUuid(), randomPermission))
      .isEmpty();
  }

  @Test
  public void user_should_be_authorized() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    UserDto user = db.users().insertUser("u1");
    GroupDto group = db.users().insertGroup();
    db.users().insertProjectPermissionOnUser(user, UserRole.USER, project2);
    db.users().insertProjectPermissionOnUser(user, UserRole.USER, project3);
    db.users().insertMember(group, user);
    db.users().insertProjectPermissionOnGroup(group, UserRole.USER, project1);

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(project2.uuid(), project3.uuid()), user.getUuid(), UserRole.USER))
      .containsOnly(project2.uuid(), project3.uuid());

    // user does not have the role "admin"
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(project2.uuid()), user.getUuid(), UserRole.ADMIN))
      .isEmpty();

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, Collections.emptySet(), user.getUuid(), UserRole.ADMIN))
      .isEmpty();
  }

  @Test
  public void group_should_be_authorized() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    UserDto user1 = db.users().insertUser("u1");
    GroupDto group = db.users().insertGroup();
    db.users().insertMembers(group, user1);
    db.users().insertProjectPermissionOnUser(user1, UserRole.USER, project1);
    db.users().insertProjectPermissionOnGroup(group, UserRole.USER, project2);
    db.users().insertProjectPermissionOnGroup(group, UserRole.USER, project3);

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(project2.uuid(), project3.uuid()), user1.getUuid(), UserRole.USER))
      .containsOnly(project2.uuid(), project3.uuid());

    // group does not have the role "admin"
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(project2.uuid(), project3.uuid()), user1.getUuid(), UserRole.ADMIN))
      .isEmpty();
  }

  @Test
  public void anonymous_should_be_authorized() {
    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto project2 = db.components().insertPublicProject();
    UserDto user1 = db.users().insertUser("u1");
    GroupDto group = db.users().insertGroup();
    db.users().insertMembers(group, user1);

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(project1.uuid(), project2.uuid()), null, UserRole.USER))
      .containsOnly(project1.uuid(), project2.uuid());

    // group does not have the role "admin"
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(project1.uuid()), null, "admin"))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_should_be_able_to_handle_lots_of_projects() {
    List<ComponentDto> projects = IntStream.range(0, 2000).mapToObj(i -> db.components().insertPublicProject()).toList();

    Collection<String> uuids = projects.stream().map(ComponentDto::uuid).collect(Collectors.toSet());
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, uuids, null, UserRole.USER))
      .containsOnly(uuids.toArray(new String[0]));
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_returns_empty_if_user_set_is_empty_on_public_project() {
    ComponentDto project = db.components().insertPublicProject();

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, Collections.emptySet(), UserRole.USER, project.uuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_returns_empty_for_non_existent_users() {
    ComponentDto project = random.nextBoolean() ? db.components().insertPublicProject() : db.components().insertPrivateProject();
    Set<String> randomNonExistingUserUuidsSet = IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .mapToObj(i -> Uuids.createFast())
      .collect(MoreCollectors.toSet());

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, randomNonExistingUserUuidsSet, UserRole.USER, project.uuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_returns_any_users_for_public_project_without_any_permission_in_DB_and_permission_USER() {
    ComponentDto project = db.components().insertPublicProject();

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, randomExistingUserUuids, UserRole.USER, project.uuid()))
      .containsAll(randomExistingUserUuids);
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_returns_any_users_for_public_project_without_any_permission_in_DB_and_permission_CODEVIEWER() {
    ComponentDto project = db.components().insertPublicProject();

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, randomExistingUserUuids, UserRole.CODEVIEWER, project.uuid()))
      .containsAll(randomExistingUserUuids);
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_returns_empty_for_any_users_on_public_project_without_any_permission_in_DB() {
    ComponentDto project = db.components().insertPublicProject();

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, randomExistingUserUuids, randomPermission, project.uuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_returns_user_if_granted_project_permission_directly_on_public_project() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto otherProject = db.components().insertPublicProject();
    UserDto otherUser = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, randomPermission, project);

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), randomPermission, project.uuid()))
      .containsOnly(user.getUuid());
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), "another perm", project.uuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(otherUser.getUuid()), randomPermission, project.uuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), randomPermission, otherProject.uuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_returns_user_if_granted_project_permission_by_group_on_public_project() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto otherProject = db.components().insertPublicProject();
    UserDto otherUser = db.users().insertUser();
    db.users().insertMember(group1, user);
    db.users().insertProjectPermissionOnGroup(group1, randomPermission, project);

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), randomPermission, project.uuid()))
      .containsOnly(user.getUuid());
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), "another perm", project.uuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), randomPermission, otherProject.uuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(otherUser.getUuid()), randomPermission, project.uuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_does_not_return_user_if_granted_project_permission_by_AnyOne_on_public_project() {
    ComponentDto project = db.components().insertPublicProject();
    ComponentDto otherProject = db.components().insertPublicProject();
    UserDto otherUser = db.users().insertUser();
    db.users().insertProjectPermissionOnAnyone(randomPermission, project);

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), randomPermission, project.uuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), "another perm", project.uuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), randomPermission, otherProject.uuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(otherUser.getUuid()), randomPermission, project.uuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_returns_empty_for_any_user_on_private_project_without_any_permission_in_DB_and_permission_USER() {
    ComponentDto project = db.components().insertPrivateProject();

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, randomExistingUserUuids, UserRole.USER, project.uuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_returns_empty_for_any_user_on_private_project_without_any_permission_in_DB_and_permission_CODEVIEWER() {
    ComponentDto project = db.components().insertPrivateProject();

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, randomExistingUserUuids, UserRole.CODEVIEWER, project.uuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_returns_empty_for_any_users_and_any_permission_on_private_project_without_any_permission_in_DB() {
    ComponentDto project = db.components().insertPrivateProject();

    PermissionsTestHelper.ALL_PERMISSIONS
      .forEach(perm -> {
        assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, randomExistingUserUuids, perm, project.uuid()))
          .isEmpty();
      });
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, randomExistingUserUuids, randomPermission, project.uuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_returns_user_if_granted_project_permission_directly_on_private_project() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto otherProject = db.components().insertPublicProject();
    UserDto otherUser = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, randomPermission, project);

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), randomPermission, project.uuid()))
      .containsOnly(user.getUuid());
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), "another perm", project.uuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(otherUser.getUuid()), randomPermission, project.uuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), randomPermission, otherProject.uuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_returns_user_if_granted_project_permission_by_group_on_private_project() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto otherProject = db.components().insertPublicProject();
    UserDto otherUser = db.users().insertUser();
    db.users().insertMember(group1, user);
    db.users().insertProjectPermissionOnGroup(group1, randomPermission, project);

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), randomPermission, project.uuid()))
      .containsOnly(user.getUuid());
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), "another perm", project.uuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(user.getUuid()), randomPermission, otherProject.uuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession, singleton(otherUser.getUuid()), randomPermission, project.uuid()))
      .isEmpty();
  }

  @Test
  public void keep_authorized_users_returns_empty_list_for_role_and_project_for_anonymous() {
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    UserDto user1 = db.users().insertUser("u1");
    UserDto user2 = db.users().insertUser("u2");
    UserDto user3 = db.users().insertUser("u3");
    GroupDto group1 = db.users().insertGroup();
    GroupDto group2 = db.users().insertGroup();
    db.users().insertMembers(group1, user1, user2);
    db.users().insertMembers(group2, user3);
    db.users().insertProjectPermissionOnUser(user1, UserRole.USER, project1);
    db.users().insertProjectPermissionOnUser(user2, UserRole.USER, project1);
    db.users().insertProjectPermissionOnUser(user3, UserRole.USER, project1);
    db.users().insertProjectPermissionOnGroup(group2, UserRole.USER, project3);

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession,
      // Only 100 and 101 has 'user' role on project
      newHashSet(user1.getUuid(), user2.getUuid(), user3.getUuid()), "user", PROJECT_UUID)).isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndProject_should_be_able_to_handle_lots_of_users() {
    List<UserDto> users = IntStream.range(0, 2000).mapToObj(i -> db.users().insertUser()).toList();

    assertThat(underTest.keepAuthorizedUsersForRoleAndProject(dbSession,
      users.stream().map(UserDto::getUuid).collect(Collectors.toSet()), "user", PROJECT_UUID)).isEmpty();
  }

  @Test
  public void countUsersWithGlobalPermissionExcludingGroupMember() {
    // u1 has the direct permission, u2 and u3 have the permission through their group
    UserDto u1 = db.users().insertUser();
    db.users().insertPermissionOnUser(u1, A_PERMISSION);
    db.users().insertPermissionOnGroup(group1, A_PERMISSION);
    db.users().insertPermissionOnGroup(group1, "another-permission");
    UserDto u2 = db.users().insertUser();
    db.users().insertMember(group1, u2);
    UserDto u3 = db.users().insertUser();
    db.users().insertMember(group1, u3);

    // excluding u2 membership --> remain u1 and u3
    int count = underTest.countUsersWithGlobalPermissionExcludingGroupMember(dbSession, A_PERMISSION, group1.getUuid(), u2.getUuid());
    assertThat(count).isEqualTo(2);

    // excluding unknown memberships
    count = underTest.countUsersWithGlobalPermissionExcludingGroupMember(dbSession, A_PERMISSION, group1.getUuid(), MISSING_UUID);
    assertThat(count).isEqualTo(3);
    count = underTest.countUsersWithGlobalPermissionExcludingGroupMember(dbSession, A_PERMISSION, MISSING_UUID, u2.getUuid());
    assertThat(count).isEqualTo(3);

    // another permission
    count = underTest.countUsersWithGlobalPermissionExcludingGroupMember(dbSession, DOES_NOT_EXIST, group1.getUuid(), u2.getUuid());
    assertThat(count).isZero();
  }

  @Test
  public void countUsersWithGlobalPermissionExcludingUserPermission() {
    // u1 and u2 have the direct permission, u3 has the permission through his group
    UserDto u1 = db.users().insertUser();
    db.users().insertPermissionOnUser(u1, A_PERMISSION);
    UserDto u2 = db.users().insertUser();
    db.users().insertPermissionOnUser(u2, A_PERMISSION);
    db.users().insertPermissionOnGroup(group1, A_PERMISSION);
    UserDto u3 = db.users().insertUser();
    db.users().insertMember(group1, u3);

    // excluding u2 permission --> remain u1 and u3
    int count = underTest.countUsersWithGlobalPermissionExcludingUserPermission(dbSession, A_PERMISSION, u2.getUuid());
    assertThat(count).isEqualTo(2);

    // excluding unknown user
    count = underTest.countUsersWithGlobalPermissionExcludingUserPermission(dbSession, A_PERMISSION, MISSING_UUID);
    assertThat(count).isEqualTo(3);

    // another permission
    count = underTest.countUsersWithGlobalPermissionExcludingUserPermission(dbSession, DOES_NOT_EXIST, u2.getUuid());
    assertThat(count).isZero();
  }

  @Test
  public void selectProjectPermissionsOfAnonymous_returns_permissions_of_anonymous_user_on_specified_public_project() {
    ComponentDto project = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnAnyone("p1", project);
    db.users().insertProjectPermissionOnUser(db.users().insertUser(), "p2", project);
    ComponentDto otherProject = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnAnyone("p3", otherProject);

    assertThat(underTest.selectProjectPermissionsOfAnonymous(dbSession, project.uuid())).containsOnly("p1");
  }

  @Test
  public void selectProjectPermissionsOfAnonymous_returns_empty_set_when_project_does_not_exist() {
    assertThat(underTest.selectProjectPermissionsOfAnonymous(dbSession, "does_not_exist")).isEmpty();
  }

  @Test
  public void selectProjectPermissions_returns_empty_set_when_logged_in_user_and_project_does_not_exist() {
    assertThat(underTest.selectProjectPermissions(dbSession, "does_not_exist", user.getUuid())).isEmpty();
  }

  @Test
  public void selectProjectPermissions_returns_permissions_of_logged_in_user_on_specified_public_project_through_anonymous_permissions() {
    ComponentDto project = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnAnyone("p1", project);
    db.users().insertProjectPermissionOnAnyone("p2", project);

    assertThat(underTest.selectProjectPermissions(dbSession, project.uuid(), user.getUuid())).containsOnly("p1", "p2");
  }

  @Test
  public void selectProjectPermissions_returns_permissions_of_logged_in_user_on_specified_project() {
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, UserRole.CODEVIEWER, project);
    db.users().insertProjectPermissionOnUser(db.users().insertUser(), UserRole.ISSUE_ADMIN, project);

    assertThat(underTest.selectProjectPermissions(dbSession, project.uuid(), user.getUuid())).containsOnly(UserRole.CODEVIEWER);
  }

  @Test
  public void selectProjectPermissions_returns_permissions_of_logged_in_user_on_specified_project_through_group_membership() {
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnGroup(group1, UserRole.CODEVIEWER, project);
    db.users().insertProjectPermissionOnGroup(group2, UserRole.ISSUE_ADMIN, project);
    db.users().insertMember(group1, user);

    assertThat(underTest.selectProjectPermissions(dbSession, project.uuid(), user.getUuid())).containsOnly(UserRole.CODEVIEWER);
  }

  @Test
  public void selectProjectPermissions_returns_permissions_of_logged_in_user_on_specified_private_project_through_all_possible_configurations() {
    ComponentDto project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, UserRole.CODEVIEWER, project);
    db.users().insertProjectPermissionOnGroup(group1, UserRole.USER, project);
    db.users().insertMember(group1, user);

    assertThat(underTest.selectProjectPermissions(dbSession, project.uuid(), user.getUuid())).containsOnly(UserRole.CODEVIEWER, UserRole.USER);
  }

  @Test
  public void selectProjectPermissions_returns_permissions_of_logged_in_user_on_specified_public_project_through_all_possible_configurations() {
    ComponentDto project = db.components().insertPublicProject();
    db.users().insertProjectPermissionOnUser(user, "p1", project);
    db.users().insertProjectPermissionOnAnyone("p2", project);
    db.users().insertProjectPermissionOnGroup(group1, "p3", project);
    db.users().insertMember(group1, user);

    assertThat(underTest.selectProjectPermissions(dbSession, project.uuid(), user.getUuid())).containsOnly("p1", "p2", "p3");
  }

  @Test
  public void keepAuthorizedProjectUuids_filters_projects_authorized_to_logged_in_user_by_direct_permission() {
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto publicProject = db.components().insertPublicProject();
    UserDto user = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, privateProject);

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(privateProject.uuid(), publicProject.uuid()), user.getUuid(), UserRole.ADMIN))
      .containsOnly(privateProject.uuid());
    // user does not have the permission "issueadmin"
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(privateProject.uuid(), publicProject.uuid()), user.getUuid(), UserRole.ISSUE_ADMIN))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_filters_projects_authorized_to_logged_in_user_by_group_permission() {
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto publicProject = db.components().insertPublicProject();
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);
    db.users().insertProjectPermissionOnGroup(group, UserRole.ADMIN, privateProject);

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(privateProject.uuid(), publicProject.uuid()), user.getUuid(), UserRole.ADMIN))
      .containsOnly(privateProject.uuid());
    // user does not have the permission "issueadmin"
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(privateProject.uuid(), publicProject.uuid()), user.getUuid(), UserRole.ISSUE_ADMIN))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_list_if_input_is_empty() {
    ComponentDto publicProject = db.components().insertPublicProject();
    UserDto user = db.users().insertUser();

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, Collections.emptySet(), user.getUuid(), UserRole.USER))
      .isEmpty();

    // projects do not exist
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet("does_not_exist"), user.getUuid(), UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_empty_list_if_input_does_not_reference_existing_projects() {
    ComponentDto publicProject = db.components().insertPublicProject();
    UserDto user = db.users().insertUser();

    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet("does_not_exist"), user.getUuid(), UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedProjectUuids_returns_public_projects_if_permission_USER_or_CODEVIEWER() {
    ComponentDto publicProject = db.components().insertPublicProject();
    UserDto user = db.users().insertUser();

    // logged-in user
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(publicProject.uuid()), user.getUuid(), UserRole.CODEVIEWER))
      .containsOnly(publicProject.uuid());
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(publicProject.uuid()), user.getUuid(), UserRole.USER))
      .containsOnly(publicProject.uuid());
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(publicProject.uuid()), user.getUuid(), UserRole.ADMIN))
      .isEmpty();

    // anonymous
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(publicProject.uuid()), null, UserRole.CODEVIEWER))
      .containsOnly(publicProject.uuid());
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(publicProject.uuid()), null, UserRole.USER))
      .containsOnly(publicProject.uuid());
    assertThat(underTest.keepAuthorizedProjectUuids(dbSession, newHashSet(publicProject.uuid()), null, UserRole.ADMIN))
      .isEmpty();
  }

  @Test
  public void selectQualityProfileAdministratorLogins_return_users_with_quality_profile_administrator_permission() {
    UserDto user1 = db.users().insertUser(withEmail("user1"));
    db.users().insertPermissionOnUser(user1, ADMINISTER_QUALITY_PROFILES);

    UserDto user2 = db.users().insertUser(withEmail("user2"));
    db.users().insertPermissionOnUser(user2, ADMINISTER_QUALITY_PROFILES);

    Set<EmailSubscriberDto> subscribers = underTest.selectQualityProfileAdministratorLogins(dbSession);

    assertThat(subscribers).containsOnly(globalEmailSubscriberOf(user1), globalEmailSubscriberOf(user2));
  }

  @Test
  public void selectQualityProfileAdministratorLogins_return_users_within_quality_profile_administrator_group() {
    GroupDto qualityProfileAdministratorGroup1 = db.users().insertGroup();
    db.users().insertPermissionOnGroup(qualityProfileAdministratorGroup1, ADMINISTER_QUALITY_PROFILES);
    UserDto user1 = db.users().insertUser(withEmail("user1"));
    db.users().insertMember(qualityProfileAdministratorGroup1, user1);
    GroupDto qualityProfileAdministratorGroup2 = db.users().insertGroup();
    db.users().insertPermissionOnGroup(qualityProfileAdministratorGroup2, ADMINISTER_QUALITY_PROFILES);
    UserDto user2 = db.users().insertUser(withEmail("user2"));
    db.users().insertMember(qualityProfileAdministratorGroup2, user2);

    Set<EmailSubscriberDto> subscribers = underTest.selectQualityProfileAdministratorLogins(dbSession);

    assertThat(subscribers).containsOnly(globalEmailSubscriberOf(user1), globalEmailSubscriberOf(user2));
  }

  @Test
  public void selectQualityProfileAdministratorLogins_does_not_return_non_quality_profile_administrator() {
    UserDto user1 = db.users().insertUser(withEmail("user1"));
    db.users().insertPermissionOnUser(user1, ADMINISTER);
    db.users().insertUser(withoutEmail("user2"));

    Set<EmailSubscriberDto> subscribers = underTest.selectQualityProfileAdministratorLogins(dbSession);

    assertThat(subscribers).isEmpty();
  }

  @Test
  public void selectQualityProfileAdministratorLogins_does_not_return_quality_profile_administrator_without_email() {
    UserDto user1NoEmail = db.users().insertUser(withoutEmail("user1NoEmail"));
    db.users().insertPermissionOnUser(user1NoEmail, ADMINISTER_QUALITY_PROFILES);
    UserDto user1WithEmail = db.users().insertUser(withEmail("user1WithEmail"));
    db.users().insertPermissionOnUser(user1WithEmail, ADMINISTER_QUALITY_PROFILES);
    GroupDto qualityProfileAdministratorGroup1 = db.users().insertGroup();
    db.users().insertPermissionOnGroup(qualityProfileAdministratorGroup1, ADMINISTER_QUALITY_PROFILES);
    UserDto user2NoEmail = db.users().insertUser(withoutEmail("user2NoEmail"));
    db.users().insertMember(qualityProfileAdministratorGroup1, user2NoEmail);
    UserDto user2WithEmail = db.users().insertUser(withEmail("user2WithEmail"));
    db.users().insertMember(qualityProfileAdministratorGroup1, user2WithEmail);

    GroupDto qualityProfileAdministratorGroup2 = db.users().insertGroup();
    db.users().insertPermissionOnGroup(qualityProfileAdministratorGroup2, ADMINISTER_QUALITY_PROFILES);
    UserDto user3NoEmail = db.users().insertUser(withoutEmail("user3NoEmail"));
    db.users().insertMember(qualityProfileAdministratorGroup2, user3NoEmail);
    UserDto user3WithEmail = db.users().insertUser(withEmail("user3WithEmail"));
    db.users().insertMember(qualityProfileAdministratorGroup2, user3WithEmail);
    UserDto user4NoEmail = db.users().insertUser(withoutEmail("user4NoEmail"));
    db.users().insertPermissionOnUser(user4NoEmail, ADMINISTER_QUALITY_PROFILES);
    UserDto user4WithEmail = db.users().insertUser(withEmail("user4WithEmail"));
    db.users().insertPermissionOnUser(user4WithEmail, ADMINISTER_QUALITY_PROFILES);
    UserDto user5NoEmail = db.users().insertUser(withoutEmail("user5NoEmail"));
    db.users().insertPermissionOnUser(user5NoEmail, ADMINISTER_QUALITY_PROFILES);
    UserDto user5WithEmail = db.users().insertUser(withEmail("user5WithEmail"));
    db.users().insertPermissionOnUser(user5WithEmail, ADMINISTER_QUALITY_PROFILES);
    db.users().insertUser(withoutEmail("user6NoEmail"));
    db.users().insertUser(withEmail("user6WithEmail"));

    Set<EmailSubscriberDto> subscribers = underTest.selectQualityProfileAdministratorLogins(dbSession);

    assertThat(subscribers)
      .containsOnly(
        globalEmailSubscriberOf(user1WithEmail),
        globalEmailSubscriberOf(user2WithEmail),
        globalEmailSubscriberOf(user3WithEmail),
        globalEmailSubscriberOf(user4WithEmail),
        globalEmailSubscriberOf(user5WithEmail));
  }

  @Test
  public void selectGlobalAdministerEmailSubscribers_returns_only_global_administers() {
    UserDto user1 = db.users().insertUser(withEmail("user1"));
    db.users().insertPermissionOnUser(user1, ADMINISTER);

    UserDto user2 = db.users().insertUser(withEmail("user2"));
    db.users().insertPermissionOnUser(user2, ADMINISTER);

    // user3 is global administer via a group
    GroupDto administratorGroup2 = db.users().insertGroup();
    db.users().insertPermissionOnGroup(administratorGroup2, ADMINISTER);
    UserDto user3 = db.users().insertUser(withEmail("user3"));
    db.users().insertMember(administratorGroup2, user3);
    // user4 has another global permission via a group
    GroupDto administratorGroup3 = db.users().insertGroup();
    db.users().insertPermissionOnGroup(administratorGroup3, QUALITY_PROFILE_ADMIN);
    UserDto user4 = db.users().insertUser(withEmail("user4"));
    db.users().insertMember(administratorGroup3, user4);

    ComponentDto project = db.components().insertPrivateProject();

    // user5 is only project level administer
    UserDto user5 = db.users().insertUser(withEmail("user5"));
    // db.users().insertPermissionOnUser(user5, ADMINISTER);
    db.users().insertProjectPermissionOnUser(user5, "admin", project);
    // user6 has other global permission
    UserDto user6 = db.users().insertUser(withEmail("user6"));
    db.users().insertPermissionOnUser(user6, ADMINISTER_QUALITY_PROFILES);
    // user7 has no permission
    db.users().insertUser(withEmail("user7"));

    Set<EmailSubscriberDto> subscribers = underTest.selectGlobalAdministerEmailSubscribers(dbSession);

    assertThat(subscribers).containsOnly(
      globalEmailSubscriberOf(user1),
      globalEmailSubscriberOf(user2),
      globalEmailSubscriberOf(user3));
  }

  @Test
  public void selectGlobalAdministerEmailSubscribers_ignores_global_administers_without_email() {
    // user1 and user1NoEmail are global administers
    UserDto user1 = db.users().insertUser(withEmail("user1"));
    db.users().insertPermissionOnUser(user1, ADMINISTER);
    UserDto user1NoEmail = db.users().insertUser(withoutEmail("user1NoEmail"));
    db.users().insertPermissionOnUser(user1NoEmail, ADMINISTER);
    // user2 and user2NoEmail are global administers
    UserDto user2 = db.users().insertUser(withEmail("user2"));
    db.users().insertPermissionOnUser(user2, ADMINISTER);
    UserDto user2NoEmail = db.users().insertUser(withoutEmail("user2NoEmail"));
    db.users().insertPermissionOnUser(user2NoEmail, ADMINISTER);

    // user3 and user3NoEmail are global administer via a group
    GroupDto administratorGroup2 = db.users().insertGroup();
    db.users().insertPermissionOnGroup(administratorGroup2, ADMINISTER);
    UserDto user3 = db.users().insertUser(withEmail("user3"));
    db.users().insertMember(administratorGroup2, user3);
    UserDto user3NoEmail = db.users().insertUser(withoutEmail("user3NoEmail"));
    db.users().insertMember(administratorGroup2, user3NoEmail);

    Set<EmailSubscriberDto> subscribers = underTest.selectGlobalAdministerEmailSubscribers(dbSession);

    assertThat(subscribers).containsOnly(
      globalEmailSubscriberOf(user1),
      globalEmailSubscriberOf(user2),
      globalEmailSubscriberOf(user3));
  }

  @Test
  public void keepAuthorizedLoginsOnProject_return_correct_users_on_public_project() {
    ComponentDto project = db.components().insertPublicProject();

    UserDto user1 = db.users().insertUser();

    // admin with "direct" ADMIN role
    UserDto admin1 = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(admin1, UserRole.ADMIN, project);

    // admin2 with ADMIN role through group
    UserDto admin2 = db.users().insertUser();
    GroupDto adminGroup = db.users().insertGroup("ADMIN");
    db.users().insertMember(adminGroup, admin2);
    db.users().insertProjectPermissionOnGroup(adminGroup, UserRole.ADMIN, project);

    assertThat(underTest.keepAuthorizedLoginsOnProject(dbSession, newHashSet(user1.getLogin()), project.getKey(), UserRole.USER))
      .containsOnly(user1.getLogin());
    assertThat(underTest.keepAuthorizedLoginsOnProject(dbSession, newHashSet(user1.getLogin(), admin1.getLogin(), admin2.getLogin()), project.getKey(), UserRole.USER))
      .containsOnly(user1.getLogin(), admin1.getLogin(), admin2.getLogin());
    assertThat(underTest.keepAuthorizedLoginsOnProject(dbSession, newHashSet(user1.getLogin(), admin1.getLogin(), admin2.getLogin()), project.getKey(), UserRole.ADMIN))
      .containsOnly(admin1.getLogin(), admin2.getLogin());
  }

  @Test
  public void keepAuthorizedLoginsOnProject_return_correct_users_on_private_project() {
    ComponentDto project = db.components().insertPrivateProject();

    GroupDto userGroup = db.users().insertGroup("USERS");
    GroupDto adminGroup = db.users().insertGroup("ADMIN");
    db.users().insertProjectPermissionOnGroup(userGroup, UserRole.USER, project);
    db.users().insertProjectPermissionOnGroup(adminGroup, UserRole.ADMIN, project);

    // admin with "direct" ADMIN role
    UserDto admin1 = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(admin1, UserRole.ADMIN, project);

    // admin2 with ADMIN role through group
    UserDto admin2 = db.users().insertUser();
    db.users().insertMember(adminGroup, admin2);

    // user1 with "direct" USER role
    UserDto user1 = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user1, UserRole.USER, project);

    // user2 with USER role through group
    UserDto user2 = db.users().insertUser();
    db.users().insertMember(userGroup, user2);

    // user without role
    UserDto userWithNoRole = db.users().insertUser();

    assertThat(underTest.keepAuthorizedLoginsOnProject(dbSession, newHashSet(userWithNoRole.getLogin()), project.getKey(), UserRole.USER))
      .isEmpty();
    assertThat(underTest.keepAuthorizedLoginsOnProject(dbSession, newHashSet(user1.getLogin()), project.getKey(), UserRole.USER))
      .containsOnly(user1.getLogin());

    Set<String> allLogins = newHashSet(admin1.getLogin(), admin2.getLogin(), user1.getLogin(), user2.getLogin(), userWithNoRole.getLogin());

    // Admin does not have the USER permission set
    assertThat(underTest.keepAuthorizedLoginsOnProject(dbSession, allLogins, project.getKey(), UserRole.USER))
      .containsOnly(user1.getLogin(), user2.getLogin());
    assertThat(underTest.keepAuthorizedLoginsOnProject(dbSession, allLogins, project.getKey(), UserRole.ADMIN))
      .containsOnly(admin1.getLogin(), admin2.getLogin());
  }

  @Test
  public void keepAuthorizedLoginsOnProject_return_correct_users_on_branch() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project, c -> c.setBranchType(BranchType.BRANCH));

    GroupDto userGroup = db.users().insertGroup("USERS");
    GroupDto adminGroup = db.users().insertGroup("ADMIN");
    db.users().insertProjectPermissionOnGroup(userGroup, UserRole.USER, project);
    db.users().insertProjectPermissionOnGroup(adminGroup, UserRole.ADMIN, project);

    // admin with "direct" ADMIN role
    UserDto admin1 = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(admin1, UserRole.ADMIN, project);

    // admin2 with ADMIN role through group
    UserDto admin2 = db.users().insertUser();
    db.users().insertMember(adminGroup, admin2);

    // user1 with "direct" USER role
    UserDto user1 = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user1, UserRole.USER, project);

    // user2 with USER role through group
    UserDto user2 = db.users().insertUser();
    db.users().insertMember(userGroup, user2);

    // user without role
    UserDto userWithNoRole = db.users().insertUser();

    assertThat(underTest.keepAuthorizedLoginsOnProject(dbSession, newHashSet(userWithNoRole.getLogin()), branch.getKey(), UserRole.USER))
      .isEmpty();
    assertThat(underTest.keepAuthorizedLoginsOnProject(dbSession, newHashSet(user1.getLogin()), branch.getKey(), UserRole.USER))
      .containsOnly(user1.getLogin());

    Set<String> allLogins = newHashSet(admin1.getLogin(), admin2.getLogin(), user1.getLogin(), user2.getLogin(), userWithNoRole.getLogin());

    // Admin does not have the USER permission set
    assertThat(underTest.keepAuthorizedLoginsOnProject(dbSession, allLogins, branch.getKey(), UserRole.USER))
      .containsOnly(user1.getLogin(), user2.getLogin());
    assertThat(underTest.keepAuthorizedLoginsOnProject(dbSession, allLogins, branch.getKey(), UserRole.ADMIN))
      .containsOnly(admin1.getLogin(), admin2.getLogin());
  }

  private static EmailSubscriberDto globalEmailSubscriberOf(UserDto userDto) {
    return EmailSubscriberDto.create(userDto.getLogin(), true, emailOf(userDto));
  }

  private static Consumer<UserDto> withEmail(String login) {
    return t -> t.setLogin(login).setEmail(emailOf(t));
  }

  private static String emailOf(UserDto t) {
    return t.getLogin() + "@foo";
  }

  private static Consumer<UserDto> withoutEmail(String login) {
    return t -> t.setLogin(login).setEmail(null);
  }
}
