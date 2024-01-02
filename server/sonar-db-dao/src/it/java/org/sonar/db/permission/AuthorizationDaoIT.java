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
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.EmailSubscriberDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.ExternalGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_PROFILES;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;

public class AuthorizationDaoIT {

  private static final String PROJECT_UUID = "uuid";
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
  private Set<String> randomPublicEntityUuids;
  private Set<String> randomPrivateEntityUuids;
  private Set<String> randomExistingUserUuids;
  private String randomPermission = "p" + random.nextInt();

  @Before
  public void setUp() {
    user = db.users().insertUser();
    group1 = db.users().insertGroup("group1");
    group2 = db.users().insertGroup("group2");
    randomExistingUserUuids = IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .mapToObj(i -> db.users().insertUser().getUuid())
      .collect(Collectors.toSet());
    randomPublicEntityUuids = IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .mapToObj(i -> db.components().insertPublicProject().getProjectDto().getUuid())
      .collect(Collectors.toSet());
    randomPrivateEntityUuids = IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .mapToObj(i -> db.components().insertPrivateProject().getProjectDto().getUuid())
      .collect(Collectors.toSet());
  }

  /**
   * Union of the permissions granted to:
   * - the user
   * - the groups which user is member
   * - anyone
   */
  @Test
  public void selectGlobalPermissions_for_logged_in_user() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
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
    db.users().insertGlobalPermissionOnUser(user3, ADMINISTER);
    db.users().insertPermissionOnAnyone(ADMINISTER);

    assertThat(underTest.selectUserUuidsWithGlobalPermission(db.getSession(), ADMINISTER.getKey()))
      .containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid(), user3.getUuid());
    assertThat(underTest.selectUserUuidsWithGlobalPermission(db.getSession(), PROVISION_PROJECTS.getKey()))
      .containsExactlyInAnyOrder(user1.getUuid(), user2.getUuid());
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_for_group_AnyOne_if_project_set_is_empty_on_public_project() {
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, Collections.emptySet(), null, UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_for_user_if_project_set_is_empty_on_public_project() {
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, Collections.emptySet(), user.getUuid(), UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_for_group_AnyOne_for_non_existent_projects() {
    Set<String> randomNonProjectsSet = IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .mapToObj(i -> Integer.toString(3_562 + i))
      .collect(Collectors.toSet());

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomNonProjectsSet, null, UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_for_user_for_non_existent_projects() {
    Set<String> randomNonProjectsSet = IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .mapToObj(i -> Integer.toString(9_666 + i))
      .collect(Collectors.toSet());

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomNonProjectsSet, user.getUuid(), UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_any_public_project_for_group_AnyOne_without_any_permission_in_DB_and_permission_USER() {
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPublicEntityUuids, null, UserRole.USER))
      .containsAll(randomPublicEntityUuids);
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_any_public_project_for_user_without_any_permission_in_DB_and_permission_USER() {
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPublicEntityUuids, user.getUuid(), UserRole.USER))
      .containsAll(randomPublicEntityUuids);
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_any_public_project_for_group_AnyOne_without_any_permission_in_DB_and_permission_CODEVIEWER() {
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPublicEntityUuids, null, UserRole.CODEVIEWER))
      .containsAll(randomPublicEntityUuids);
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_any_public_project_for_user_without_any_permission_in_DB_and_permission_CODEVIEWER() {
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPublicEntityUuids, user.getUuid(), UserRole.CODEVIEWER))
      .containsAll(randomPublicEntityUuids);
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_for_other_permission_for_group_AnyOne_on_public_project_without_any_permission_in_DB() {
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPublicEntityUuids, null, randomPermission))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_for_any_permission_for_user_on_public_project_without_any_permission_in_DB() {
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPublicEntityUuids, user.getUuid(), randomPermission))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_public_project_if_user_is_granted_project_permission_directly() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPublicProject().getProjectDto();
    UserDto otherUser = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, randomPermission, project);

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.getUuid()), otherUser.getUuid(), randomPermission))
      .isEmpty();
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(otherProject.getUuid()), user.getUuid(), randomPermission))
      .isEmpty();
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.getUuid()), user.getUuid(), randomPermission))
      .containsOnly(project.getUuid());
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.getUuid()), user.getUuid(), "another perm"))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_public_project_if_user_is_granted_project_permission_by_group() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    ComponentDto otherProject = db.components().insertPublicProject().getMainBranchComponent();
    UserDto otherUser = db.users().insertUser();
    db.users().insertMember(group1, user);
    db.users().insertEntityPermissionOnGroup(group1, randomPermission, project);

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.getUuid()), user.getUuid(), randomPermission))
      .containsOnly(project.getUuid());
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(otherProject.uuid()), user.getUuid(), randomPermission))
      .isEmpty();
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.getUuid()), otherUser.getUuid(), randomPermission))
      .isEmpty();
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.getUuid()), user.getUuid(), "another perm"))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_public_project_if_group_AnyOne_is_granted_project_permission_directly() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPublicProject().getProjectDto();
    db.users().insertEntityPermissionOnAnyone(randomPermission, project);

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.getUuid()), null, randomPermission))
      .containsOnly(project.getUuid());
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.getUuid()), null, "another perm"))
      .isEmpty();
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(otherProject.getUuid()), null, randomPermission))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_for_user_on_private_project_without_any_permission_in_DB_and_permission_USER() {
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPrivateEntityUuids, user.getUuid(), UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_for_group_AnyOne_on_private_project_without_any_permission_in_DB_and_permission_USER() {
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPrivateEntityUuids, null, UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_for_user_on_private_project_without_any_permission_in_DB_and_permission_CODEVIEWER() {
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPrivateEntityUuids, user.getUuid(), UserRole.CODEVIEWER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_for_group_AnyOne_on_private_project_without_any_permission_in_DB_and_permission_CODEVIEWER() {
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPrivateEntityUuids, null, UserRole.CODEVIEWER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_for_user_and_any_permission_on_private_project_without_any_permission_in_DB() {
    PermissionsTestHelper.ALL_PERMISSIONS
      .forEach(perm -> {
        assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPrivateEntityUuids, user.getUuid(), perm))
          .isEmpty();
      });
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPrivateEntityUuids, user.getUuid(), randomPermission))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_for_group_AnyOne_and_any_permission_on_private_project_without_any_permission_in_DB() {
    PermissionsTestHelper.ALL_PERMISSIONS
      .forEach(perm -> {
        assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPrivateEntityUuids, null, perm))
          .isEmpty();
      });
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, randomPrivateEntityUuids, null, randomPermission))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_private_project_if_user_is_granted_project_permission_directly() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPrivateProject().getProjectDto();
    UserDto otherUser = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, randomPermission, project);

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.getUuid()), user.getUuid(), randomPermission))
      .containsOnly(project.getUuid());
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.getUuid()), user.getUuid(), "another perm"))
      .isEmpty();
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(otherProject.getUuid()), user.getUuid(), randomPermission))
      .isEmpty();
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.getUuid()), otherUser.getUuid(), randomPermission))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_private_project_if_user_is_granted_project_permission_by_group() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto otherProject = db.components().insertPrivateProject().getMainBranchComponent();
    UserDto otherUser = db.users().insertUser();
    db.users().insertMember(group1, user);
    db.users().insertProjectPermissionOnGroup(group1, randomPermission, project);

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.uuid()), user.getUuid(), randomPermission))
      .containsOnly(project.uuid());
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.uuid()), user.getUuid(), "another perm"))
      .isEmpty();
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(otherProject.uuid()), user.getUuid(), randomPermission))
      .isEmpty();
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, singleton(project.uuid()), otherUser.getUuid(), randomPermission))
      .isEmpty();
  }

  @Test
  public void user_should_be_authorized() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject().getProjectDto();
    UserDto user = db.users().insertUser("u1");
    GroupDto group = db.users().insertGroup();
    db.users().insertProjectPermissionOnUser(user, UserRole.USER, project2);
    db.users().insertProjectPermissionOnUser(user, UserRole.USER, project3);
    db.users().insertMember(group, user);
    db.users().insertEntityPermissionOnGroup(group, UserRole.USER, project1);

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(project2.getUuid(), project3.getUuid()), user.getUuid(), UserRole.USER))
      .containsOnly(project2.getUuid(), project3.getUuid());

    // user does not have the role "admin"
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(project2.getUuid()), user.getUuid(), UserRole.ADMIN))
      .isEmpty();

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, Collections.emptySet(), user.getUuid(), UserRole.ADMIN))
      .isEmpty();
  }

  @Test
  public void group_should_be_authorized() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject().getProjectDto();
    UserDto user1 = db.users().insertUser("u1");
    GroupDto group = db.users().insertGroup();
    db.users().insertMembers(group, user1);
    db.users().insertProjectPermissionOnUser(user1, UserRole.USER, project1);
    db.users().insertEntityPermissionOnGroup(group, UserRole.USER, project2);
    db.users().insertEntityPermissionOnGroup(group, UserRole.USER, project3);

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(project2.getUuid(), project3.getUuid()), user1.getUuid(), UserRole.USER))
      .containsOnly(project2.getUuid(), project3.getUuid());

    // group does not have the role "admin"
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(project2.getUuid(), project3.getUuid()), user1.getUuid(), UserRole.ADMIN))
      .isEmpty();
  }

  @Test
  public void anonymous_should_be_authorized() {
    ProjectDto project1 = db.components().insertPublicProject().getProjectDto();
    ProjectDto project2 = db.components().insertPublicProject().getProjectDto();
    UserDto user1 = db.users().insertUser("u1");
    GroupDto group = db.users().insertGroup();
    db.users().insertMembers(group, user1);

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(project1.getUuid(), project2.getUuid()), null, UserRole.USER))
      .containsOnly(project1.getUuid(), project2.getUuid());

    // group does not have the role "admin"
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(project1.getUuid()), null, "admin"))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_should_be_able_to_handle_lots_of_projects() {
    List<ProjectDto> projects = IntStream.range(0, 2000).mapToObj(i -> db.components().insertPublicProject().getProjectDto()).toList();

    Collection<String> uuids = projects.stream().map(ProjectDto::getUuid).collect(Collectors.toSet());
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, uuids, null, UserRole.USER))
      .containsOnly(uuids.toArray(new String[0]));
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_returns_empty_if_user_set_is_empty_on_public_project() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, Collections.emptySet(), UserRole.USER, project.getUuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_returns_empty_for_non_existent_users() {
    ProjectDto project = random.nextBoolean() ? db.components().insertPublicProject().getProjectDto() : db.components().insertPrivateProject().getProjectDto();
    Set<String> randomNonExistingUserUuidsSet = IntStream.range(0, 1 + Math.abs(random.nextInt(5)))
      .mapToObj(i -> Uuids.createFast())
      .collect(Collectors.toSet());

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, randomNonExistingUserUuidsSet, UserRole.USER, project.getUuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_returns_any_users_for_public_project_without_any_permission_in_DB_and_permission_USER() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, randomExistingUserUuids, UserRole.USER, project.getUuid()))
      .containsAll(randomExistingUserUuids);
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_returns_any_users_for_public_project_without_any_permission_in_DB_and_permission_CODEVIEWER() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, randomExistingUserUuids, UserRole.CODEVIEWER, project.getUuid()))
      .containsAll(randomExistingUserUuids);
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_returns_empty_for_any_users_on_public_project_without_any_permission_in_DB() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, randomExistingUserUuids, randomPermission, project.getUuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_returns_user_if_granted_project_permission_directly_on_public_project() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPublicProject().getProjectDto();
    UserDto otherUser = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, randomPermission, project);

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), randomPermission, project.getUuid()))
      .containsOnly(user.getUuid());
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), "another perm", project.getUuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(otherUser.getUuid()), randomPermission, project.getUuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), randomPermission, otherProject.getUuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_returns_user_if_granted_project_permission_by_group_on_public_project() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPublicProject().getProjectDto();
    UserDto otherUser = db.users().insertUser();
    db.users().insertMember(group1, user);
    db.users().insertEntityPermissionOnGroup(group1, randomPermission, project);

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), randomPermission, project.getUuid()))
      .containsOnly(user.getUuid());
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), "another perm", project.getUuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), randomPermission, otherProject.getUuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(otherUser.getUuid()), randomPermission, project.getUuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_does_not_return_user_if_granted_project_permission_by_AnyOne_on_public_project() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPublicProject().getProjectDto();
    UserDto otherUser = db.users().insertUser();
    db.users().insertEntityPermissionOnAnyone(randomPermission, project);

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), randomPermission, project.getUuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), "another perm", project.getUuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), randomPermission, otherProject.getUuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(otherUser.getUuid()), randomPermission, project.getUuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_returns_empty_for_any_user_on_private_project_without_any_permission_in_DB_and_permission_USER() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, randomExistingUserUuids, UserRole.USER, project.getUuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_returns_empty_for_any_user_on_private_project_without_any_permission_in_DB_and_permission_CODEVIEWER() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, randomExistingUserUuids, UserRole.CODEVIEWER, project.getUuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_returns_empty_for_any_users_and_any_permission_on_private_project_without_any_permission_in_DB() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    PermissionsTestHelper.ALL_PERMISSIONS
      .forEach(perm -> {
        assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, randomExistingUserUuids, perm, project.getUuid()))
          .isEmpty();
      });
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, randomExistingUserUuids, randomPermission, project.getUuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_returns_user_if_granted_project_permission_directly_on_private_project() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPublicProject().getProjectDto();
    UserDto otherUser = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, randomPermission, project);

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), randomPermission, project.getUuid()))
      .containsOnly(user.getUuid());
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), "another perm", project.getUuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(otherUser.getUuid()), randomPermission, project.getUuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), randomPermission, otherProject.getUuid()))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_returns_user_if_granted_project_permission_by_group_on_private_project() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPublicProject().getProjectDto();
    UserDto otherUser = db.users().insertUser();
    db.users().insertMember(group1, user);
    db.users().insertEntityPermissionOnGroup(group1, randomPermission, project);

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), randomPermission, project.getUuid()))
      .containsOnly(user.getUuid());
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), "another perm", project.getUuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(user.getUuid()), randomPermission, otherProject.getUuid()))
      .isEmpty();
    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession, singleton(otherUser.getUuid()), randomPermission, project.getUuid()))
      .isEmpty();
  }

  @Test
  public void keep_authorized_users_returns_empty_list_for_role_and_project_for_anonymous() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject().getProjectDto();
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
    db.users().insertEntityPermissionOnGroup(group2, UserRole.USER, project3);

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession,
      // Only 100 and 101 has 'user' role on project
      newHashSet(user1.getUuid(), user2.getUuid(), user3.getUuid()), "user", PROJECT_UUID)).isEmpty();
  }

  @Test
  public void keepAuthorizedUsersForRoleAndEntity_should_be_able_to_handle_lots_of_users() {
    List<UserDto> users = IntStream.range(0, 2000).mapToObj(i -> db.users().insertUser()).toList();

    assertThat(underTest.keepAuthorizedUsersForRoleAndEntity(dbSession,
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
  public void selectEntityPermissionsOfAnonymous_returns_permissions_of_anonymous_user_on_specified_public_project() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    db.users().insertEntityPermissionOnAnyone("p1", project);
    db.users().insertProjectPermissionOnUser(db.users().insertUser(), "p2", project);
    ProjectDto otherProject = db.components().insertPublicProject().getProjectDto();
    db.users().insertEntityPermissionOnAnyone("p3", otherProject);

    assertThat(underTest.selectEntityPermissionsOfAnonymous(dbSession, project.getUuid())).containsOnly("p1");
  }

  @Test
  public void selectEntityPermissionsOfAnonymous_returns_empty_set_when_project_does_not_exist() {
    assertThat(underTest.selectEntityPermissionsOfAnonymous(dbSession, "does_not_exist")).isEmpty();
  }

  @Test
  public void selectEntityPermissions_returns_empty_set_when_logged_in_user_and_project_does_not_exist() {
    assertThat(underTest.selectEntityPermissions(dbSession, "does_not_exist", user.getUuid())).isEmpty();
  }

  @Test
  public void selectEntityPermissions_returns_permissions_of_logged_in_user_on_specified_public_project_through_anonymous_permissions() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    db.users().insertEntityPermissionOnAnyone("p1", project);
    db.users().insertEntityPermissionOnAnyone("p2", project);

    assertThat(underTest.selectEntityPermissions(dbSession, project.getUuid(), user.getUuid())).containsOnly("p1", "p2");
  }

  @Test
  public void selectEntityPermissions_returns_permissions_of_logged_in_user_on_specified_project() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.users().insertProjectPermissionOnUser(user, UserRole.CODEVIEWER, project);
    db.users().insertProjectPermissionOnUser(db.users().insertUser(), UserRole.ISSUE_ADMIN, project);

    assertThat(underTest.selectEntityPermissions(dbSession, project.getUuid(), user.getUuid())).containsOnly(UserRole.CODEVIEWER);
  }

  @Test
  public void selectEntityPermissions_returns_permissions_of_logged_in_user_on_specified_project_through_group_membership() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.users().insertEntityPermissionOnGroup(group1, UserRole.CODEVIEWER, project);
    db.users().insertEntityPermissionOnGroup(group2, UserRole.ISSUE_ADMIN, project);
    db.users().insertMember(group1, user);

    assertThat(underTest.selectEntityPermissions(dbSession, project.getUuid(), user.getUuid())).containsOnly(UserRole.CODEVIEWER);
  }

  @Test
  public void selectEntityPermissions_returns_permissions_of_logged_in_user_on_specified_private_project_through_all_possible_configurations() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.users().insertProjectPermissionOnUser(user, UserRole.CODEVIEWER, project);
    db.users().insertEntityPermissionOnGroup(group1, UserRole.USER, project);
    db.users().insertMember(group1, user);

    assertThat(underTest.selectEntityPermissions(dbSession, project.getUuid(), user.getUuid())).containsOnly(UserRole.CODEVIEWER, UserRole.USER);
  }

  @Test
  public void selectEntityPermissions_returns_permissions_of_logged_in_user_on_specified_public_project_through_all_possible_configurations() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    db.users().insertProjectPermissionOnUser(user, "p1", project);
    db.users().insertEntityPermissionOnAnyone("p2", project);
    db.users().insertEntityPermissionOnGroup(group1, "p3", project);
    db.users().insertMember(group1, user);

    assertThat(underTest.selectEntityPermissions(dbSession, project.getUuid(), user.getUuid())).containsOnly("p1", "p2", "p3");
  }

  @Test
  public void selectEntityPermissionsObtainedViaManagedGroup_shouldOnlyReturnGroupLevelPermissions() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    GroupDto groupDto = insertExternalGroup("github");
    db.users().insertEntityPermissionOnGroup(groupDto, "groupLevelPermissionThatShouldBeRetrieved", project);
    db.users().insertProjectPermissionOnUser(user, "userLevelPermissionThatShouldBeIgnored", project);

    Set<UserAndPermissionDto> permissionsViaGroup = underTest.selectEntityPermissionsObtainedViaManagedGroup(dbSession, project.getUuid(), "github");
    assertThat(permissionsViaGroup)
      .containsExactlyInAnyOrder(new UserAndPermissionDto(user.getUuid(), "groupLevelPermissionThatShouldBeRetrieved"));
  }

  @Test
  public void selectEntityPermissionsObtainedViaManagedGroup_shouldOnlyReturnPermissionsFromRightManagedInstanceProvider() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    GroupDto groupDto = insertExternalGroup("github");
    db.users().insertEntityPermissionOnGroup(groupDto, "groupLevelPermissionThatShouldBeRetrieved", project);

    GroupDto groupDtoManagedByGithero = insertExternalGroup("githero");
    db.users().insertEntityPermissionOnGroup(groupDtoManagedByGithero, "groupLevelPermissionThatShouldBeIgnored", project);

    Set<UserAndPermissionDto> permissionsViaGroup = underTest.selectEntityPermissionsObtainedViaManagedGroup(dbSession, project.getUuid(), "github");
    assertThat(permissionsViaGroup)
      .containsExactlyInAnyOrder(new UserAndPermissionDto(user.getUuid(), "groupLevelPermissionThatShouldBeRetrieved"));
  }

  @Test
  public void selectEntityPermissionsObtainedViaManagedGroup_shouldOnlyReturnPermissionsFromExpectedProject() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    GroupDto groupDto = insertExternalGroup("github");
    db.users().insertEntityPermissionOnGroup(groupDto, "groupLevelPermissionThatShouldBeRetrieved", project);

    ProjectDto projectNotInScope = db.components().insertPublicProject().getProjectDto();
    db.users().insertEntityPermissionOnGroup(groupDto, "groupLevelPermissionThatShouldBeIgnored", projectNotInScope);

    Set<UserAndPermissionDto> permissionsViaGroup = underTest.selectEntityPermissionsObtainedViaManagedGroup(dbSession, project.getUuid(), "github");
    assertThat(permissionsViaGroup)
      .containsExactlyInAnyOrder(new UserAndPermissionDto(user.getUuid(), "groupLevelPermissionThatShouldBeRetrieved"));
  }

  @Test
  public void selectEntityPermissionsObtainedViaManagedGroup_whenSeveralRoles_shouldReturnsThemAll() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    GroupDto groupDto = insertExternalGroup("github");
    db.users().insertEntityPermissionOnGroup(groupDto, "groupLevelPermissionThatShouldBeRetrieved", project);
    db.users().insertEntityPermissionOnGroup(groupDto, "groupLevelPermissionThatShouldBeRetrieved2", project);

    Set<UserAndPermissionDto> permissionsViaGroup = underTest.selectEntityPermissionsObtainedViaManagedGroup(dbSession, project.getUuid(), "github");
    assertThat(permissionsViaGroup)
      .containsExactlyInAnyOrder(
        new UserAndPermissionDto(user.getUuid(), "groupLevelPermissionThatShouldBeRetrieved"),
        new UserAndPermissionDto(user.getUuid(), "groupLevelPermissionThatShouldBeRetrieved2")
      );
  }

  @Test
  public void selectEntityPermissionsObtainedViaManagedGroup_whenSeveralUsers_shouldReturnsThemAll() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    GroupDto groupDto = insertExternalGroup("github");
    db.users().insertEntityPermissionOnGroup(groupDto, "groupLevelPermissionThatShouldBeRetrieved", project);

    UserDto user2 = db.users().insertUser();
    db.users().insertMember(groupDto, user2);

    Set<UserAndPermissionDto> permissionsViaGroup = underTest.selectEntityPermissionsObtainedViaManagedGroup(dbSession, project.getUuid(), "github");
    assertThat(permissionsViaGroup)
      .containsExactlyInAnyOrder(
        new UserAndPermissionDto(user.getUuid(), "groupLevelPermissionThatShouldBeRetrieved"),
        new UserAndPermissionDto(user2.getUuid(), "groupLevelPermissionThatShouldBeRetrieved")
      );
  }

  @Test
  public void selectEntityPermissionsObtainedViaManagedGroup_whenMultipleGroupsAndUsers_shouldMergeEqualsObjects() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    GroupDto groupDto = insertExternalGroup("github");
    db.users().insertEntityPermissionOnGroup(groupDto, "groupLevelPermissionThatShouldBeRetrieved", project);

    UserDto user2 = db.users().insertUser();
    db.users().insertMember(groupDto, user2);

    GroupDto groupDto2 = insertExternalGroup("github");
    db.users().insertEntityPermissionOnGroup(groupDto2, "groupLevelPermissionThatShouldBeRetrieved", project);
    db.users().insertMember(groupDto2, user2);

    Set<UserAndPermissionDto> permissionsViaGroup = underTest.selectEntityPermissionsObtainedViaManagedGroup(dbSession, project.getUuid(), "github");
    assertThat(permissionsViaGroup)
      .containsExactlyInAnyOrder(
        new UserAndPermissionDto(user.getUuid(), "groupLevelPermissionThatShouldBeRetrieved"),
        new UserAndPermissionDto(user2.getUuid(), "groupLevelPermissionThatShouldBeRetrieved")
      );
  }

  private GroupDto insertExternalGroup(String managedInstanceProvider) {
    GroupDto groupDto = db.users().insertGroup();
    db.users().insertExternalGroup(new ExternalGroupDto(groupDto.getUuid(), "ext_" + groupDto.getUuid(), managedInstanceProvider));
    db.users().insertMember(groupDto, user);
    return groupDto;
  }

  @Test
  public void keepAuthorizedEntityUuids_filters_projects_authorized_to_logged_in_user_by_direct_permission() {
    ProjectDto privateProject = db.components().insertPrivateProject().getProjectDto();
    ProjectDto publicProject = db.components().insertPublicProject().getProjectDto();
    UserDto user = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(user, UserRole.ADMIN, privateProject);

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(privateProject.getUuid(), publicProject.getUuid()), user.getUuid(), UserRole.ADMIN))
      .containsOnly(privateProject.getUuid());
    // user does not have the permission "issueadmin"
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(privateProject.getUuid(), publicProject.getUuid()), user.getUuid(), UserRole.ISSUE_ADMIN))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_filters_projects_authorized_to_logged_in_user_by_group_permission() {
    ProjectDto privateProject = db.components().insertPrivateProject().getProjectDto();
    ProjectDto publicProject = db.components().insertPublicProject().getProjectDto();
    UserDto user = db.users().insertUser();
    GroupDto group = db.users().insertGroup();
    db.users().insertMember(group, user);
    db.users().insertEntityPermissionOnGroup(group, UserRole.ADMIN, privateProject);

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(privateProject.getUuid(), publicProject.getUuid()), user.getUuid(), UserRole.ADMIN))
      .containsOnly(privateProject.getUuid());
    // user does not have the permission "issueadmin"
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(privateProject.getUuid(), publicProject.getUuid()), user.getUuid(), UserRole.ISSUE_ADMIN))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_list_if_input_is_empty() {
    ProjectDto publicProject = db.components().insertPublicProject().getProjectDto();
    UserDto user = db.users().insertUser();

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, Collections.emptySet(), user.getUuid(), UserRole.USER))
      .isEmpty();

    // projects do not exist
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet("does_not_exist"), user.getUuid(), UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_empty_list_if_input_does_not_reference_existing_projects() {
    ProjectDto publicProject = db.components().insertPublicProject().getProjectDto();
    UserDto user = db.users().insertUser();

    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet("does_not_exist"), user.getUuid(), UserRole.USER))
      .isEmpty();
  }

  @Test
  public void keepAuthorizedEntityUuids_returns_public_projects_if_permission_USER_or_CODEVIEWER() {
    ProjectDto publicProject = db.components().insertPublicProject().getProjectDto();
    UserDto user = db.users().insertUser();

    // logged-in user
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(publicProject.getUuid()), user.getUuid(), UserRole.CODEVIEWER))
      .containsOnly(publicProject.getUuid());
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(publicProject.getUuid()), user.getUuid(), UserRole.USER))
      .containsOnly(publicProject.getUuid());
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(publicProject.getUuid()), user.getUuid(), UserRole.ADMIN))
      .isEmpty();

    // anonymous
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(publicProject.getUuid()), null, UserRole.CODEVIEWER))
      .containsOnly(publicProject.getUuid());
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(publicProject.getUuid()), null, UserRole.USER))
      .containsOnly(publicProject.getUuid());
    assertThat(underTest.keepAuthorizedEntityUuids(dbSession, newHashSet(publicProject.getUuid()), null, UserRole.ADMIN))
      .isEmpty();
  }

  @Test
  public void selectQualityProfileAdministratorLogins_return_users_with_quality_profile_administrator_permission() {
    UserDto user1 = db.users().insertUser(withEmail("user1"));
    db.users().insertGlobalPermissionOnUser(user1, ADMINISTER_QUALITY_PROFILES);

    UserDto user2 = db.users().insertUser(withEmail("user2"));
    db.users().insertGlobalPermissionOnUser(user2, ADMINISTER_QUALITY_PROFILES);

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
    db.users().insertGlobalPermissionOnUser(user1, ADMINISTER);
    db.users().insertUser(withoutEmail("user2"));

    Set<EmailSubscriberDto> subscribers = underTest.selectQualityProfileAdministratorLogins(dbSession);

    assertThat(subscribers).isEmpty();
  }

  @Test
  public void selectQualityProfileAdministratorLogins_does_not_return_quality_profile_administrator_without_email() {
    UserDto user1NoEmail = db.users().insertUser(withoutEmail("user1NoEmail"));
    db.users().insertGlobalPermissionOnUser(user1NoEmail, ADMINISTER_QUALITY_PROFILES);
    UserDto user1WithEmail = db.users().insertUser(withEmail("user1WithEmail"));
    db.users().insertGlobalPermissionOnUser(user1WithEmail, ADMINISTER_QUALITY_PROFILES);
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
    db.users().insertGlobalPermissionOnUser(user4NoEmail, ADMINISTER_QUALITY_PROFILES);
    UserDto user4WithEmail = db.users().insertUser(withEmail("user4WithEmail"));
    db.users().insertGlobalPermissionOnUser(user4WithEmail, ADMINISTER_QUALITY_PROFILES);
    UserDto user5NoEmail = db.users().insertUser(withoutEmail("user5NoEmail"));
    db.users().insertGlobalPermissionOnUser(user5NoEmail, ADMINISTER_QUALITY_PROFILES);
    UserDto user5WithEmail = db.users().insertUser(withEmail("user5WithEmail"));
    db.users().insertGlobalPermissionOnUser(user5WithEmail, ADMINISTER_QUALITY_PROFILES);
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
    db.users().insertGlobalPermissionOnUser(user1, ADMINISTER);

    UserDto user2 = db.users().insertUser(withEmail("user2"));
    db.users().insertGlobalPermissionOnUser(user2, ADMINISTER);

    // user3 is global administer via a group
    GroupDto administratorGroup2 = db.users().insertGroup();
    db.users().insertPermissionOnGroup(administratorGroup2, ADMINISTER);
    UserDto user3 = db.users().insertUser(withEmail("user3"));
    db.users().insertMember(administratorGroup2, user3);
    // user4 has another global permission via a group
    GroupDto administratorGroup3 = db.users().insertGroup();
    db.users().insertPermissionOnGroup(administratorGroup3, ADMINISTER_QUALITY_PROFILES);
    UserDto user4 = db.users().insertUser(withEmail("user4"));
    db.users().insertMember(administratorGroup3, user4);

    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    // user5 is only project level administer
    UserDto user5 = db.users().insertUser(withEmail("user5"));
    // db.users().insertPermissionOnUser(user5, ADMINISTER);
    db.users().insertProjectPermissionOnUser(user5, "admin", project);
    // user6 has other global permission
    UserDto user6 = db.users().insertUser(withEmail("user6"));
    db.users().insertGlobalPermissionOnUser(user6, ADMINISTER_QUALITY_PROFILES);
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
    db.users().insertGlobalPermissionOnUser(user1, ADMINISTER);
    UserDto user1NoEmail = db.users().insertUser(withoutEmail("user1NoEmail"));
    db.users().insertGlobalPermissionOnUser(user1NoEmail, ADMINISTER);
    // user2 and user2NoEmail are global administers
    UserDto user2 = db.users().insertUser(withEmail("user2"));
    db.users().insertGlobalPermissionOnUser(user2, ADMINISTER);
    UserDto user2NoEmail = db.users().insertUser(withoutEmail("user2NoEmail"));
    db.users().insertGlobalPermissionOnUser(user2NoEmail, ADMINISTER);

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
  public void keepAuthorizedLoginsOnEntity_return_correct_users_on_public_project() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    UserDto user1 = db.users().insertUser();

    // admin with "direct" ADMIN role
    UserDto admin1 = db.users().insertUser();
    db.users().insertProjectPermissionOnUser(admin1, UserRole.ADMIN, project);

    // admin2 with ADMIN role through group
    UserDto admin2 = db.users().insertUser();
    GroupDto adminGroup = db.users().insertGroup("ADMIN");
    db.users().insertMember(adminGroup, admin2);
    db.users().insertEntityPermissionOnGroup(adminGroup, UserRole.ADMIN, project);

    assertThat(underTest.keepAuthorizedLoginsOnEntity(dbSession, newHashSet(user1.getLogin()), project.getKey(), UserRole.USER))
      .containsOnly(user1.getLogin());
    assertThat(underTest.keepAuthorizedLoginsOnEntity(dbSession, newHashSet(user1.getLogin(), admin1.getLogin(), admin2.getLogin()), project.getKey(), UserRole.USER))
      .containsOnly(user1.getLogin(), admin1.getLogin(), admin2.getLogin());
    assertThat(underTest.keepAuthorizedLoginsOnEntity(dbSession, newHashSet(user1.getLogin(), admin1.getLogin(), admin2.getLogin()), project.getKey(), UserRole.ADMIN))
      .containsOnly(admin1.getLogin(), admin2.getLogin());
  }

  @Test
  public void keepAuthorizedLoginsOnEntity_return_correct_users_on_private_project() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    GroupDto userGroup = db.users().insertGroup("USERS");
    GroupDto adminGroup = db.users().insertGroup("ADMIN");
    db.users().insertEntityPermissionOnGroup(userGroup, UserRole.USER, project);
    db.users().insertEntityPermissionOnGroup(adminGroup, UserRole.ADMIN, project);

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

    assertThat(underTest.keepAuthorizedLoginsOnEntity(dbSession, newHashSet(userWithNoRole.getLogin()), project.getKey(), UserRole.USER))
      .isEmpty();
    assertThat(underTest.keepAuthorizedLoginsOnEntity(dbSession, newHashSet(user1.getLogin()), project.getKey(), UserRole.USER))
      .containsOnly(user1.getLogin());

    Set<String> allLogins = newHashSet(admin1.getLogin(), admin2.getLogin(), user1.getLogin(), user2.getLogin(), userWithNoRole.getLogin());

    // Admin does not have the USER permission set
    assertThat(underTest.keepAuthorizedLoginsOnEntity(dbSession, allLogins, project.getKey(), UserRole.USER))
      .containsOnly(user1.getLogin(), user2.getLogin());
    assertThat(underTest.keepAuthorizedLoginsOnEntity(dbSession, allLogins, project.getKey(), UserRole.ADMIN))
      .containsOnly(admin1.getLogin(), admin2.getLogin());
  }

  @Test
  public void keepAuthorizedLoginsOnEntity_whenHasBranch_shouldReturnExpectedUsers() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project, c -> c.setBranchType(BranchType.BRANCH));

    GroupDto userGroup = db.users().insertGroup("USERS");
    GroupDto adminGroup = db.users().insertGroup("ADMIN");
    db.users().insertEntityPermissionOnGroup(userGroup, UserRole.USER, project);
    db.users().insertEntityPermissionOnGroup(adminGroup, UserRole.ADMIN, project);

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

    assertThat(underTest.keepAuthorizedLoginsOnEntity(dbSession, newHashSet(userWithNoRole.getLogin()), project.getKey(), UserRole.USER))
      .isEmpty();
    assertThat(underTest.keepAuthorizedLoginsOnEntity(dbSession, newHashSet(user1.getLogin()), project.getKey(), UserRole.USER))
      .containsOnly(user1.getLogin());

    Set<String> allLogins = newHashSet(admin1.getLogin(), admin2.getLogin(), user1.getLogin(), user2.getLogin(), userWithNoRole.getLogin());

    // Admin does not have the USER permission set
    assertThat(underTest.keepAuthorizedLoginsOnEntity(dbSession, allLogins, project.getKey(), UserRole.USER))
      .containsOnly(user1.getLogin(), user2.getLogin());
    assertThat(underTest.keepAuthorizedLoginsOnEntity(dbSession, allLogins, project.getKey(), UserRole.ADMIN))
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
