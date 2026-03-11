/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.users;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.db.DbTester;
import org.sonar.db.component.ProjectData;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.user.UserDto;
import org.sonarsource.organizations.server.DefaultOrganizationProvider;
import org.sonarsource.users.api.EffectiveRolesBatchQuery;
import org.sonarsource.users.api.EffectiveRolesQuery;
import org.sonarsource.users.api.model.EffectiveRole;
import org.sonarsource.users.api.model.EffectiveRoleBatch;
import org.sonarsource.users.api.model.Principal;
import org.sonarsource.users.api.model.PrincipalType;
import org.sonarsource.users.api.model.ResourceType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EffectiveRolesServiceImplTest {

  public static final String MEMBER_ROLE = "member";

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private EffectiveRolesServiceImpl underTest;

  @BeforeEach
  void setUp() {
    underTest = new EffectiveRolesServiceImpl(db.getDbClient());
  }

  @Test
  void getEffectiveRoles_withOrganizationResourceType_shouldReturnGlobalPermissions() {
    UserDto user = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.ADMINISTER);
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.PROVISION_PROJECTS);

    EffectiveRolesQuery query = new EffectiveRolesQuery(
      new Principal(PrincipalType.USER, user.getUuid()),
      null,
      ResourceType.ORGANIZATION
    );

    List<EffectiveRole> roles = underTest.getEffectiveRoles(query);

    assertThat(roles).containsExactlyInAnyOrder(new EffectiveRole("admin"), new EffectiveRole("provisioning"), new EffectiveRole("member"));
  }

  @Test
  void getEffectiveRoles_withProjectResourceType_shouldReturnEntityPermissions() {
    UserDto user = db.users().insertUser();
    ProjectData project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.USER, project.getProjectDto());
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.CODEVIEWER, project.getProjectDto());

    EffectiveRolesQuery query = new EffectiveRolesQuery(
      new Principal(PrincipalType.USER, user.getUuid()),
      project.projectUuid(),
      ResourceType.PROJECT
    );

    List<EffectiveRole> roles = underTest.getEffectiveRoles(query);

    assertThat(roles).containsExactlyInAnyOrder(new EffectiveRole("user"), new EffectiveRole("codeviewer"));
  }

  @ParameterizedTest
  @MethodSource("getEffectiveRolesExceptionCases")
  void getEffectiveRoles_shouldThrowException(PrincipalType principalType, String principalId, String resourceId,
    ResourceType resourceType, String expectedMessage) {
    String actualPrincipalId = principalId;
    if (principalType == PrincipalType.USER && principalId == null) {
      UserDto user = db.users().insertUser();
      actualPrincipalId = user.getUuid();
    }

    EffectiveRolesQuery query = new EffectiveRolesQuery(
      new Principal(principalType, actualPrincipalId),
      resourceId,
      resourceType
    );

    assertThatThrownBy(() -> underTest.getEffectiveRoles(query))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(expectedMessage);
  }

  private static Stream<Arguments> getEffectiveRolesExceptionCases() {
    return Stream.of(
      Arguments.of(PrincipalType.USER, null, null, ResourceType.PROJECT,
        "Resource IDs are required for PROJECT resource type"),
      Arguments.of(PrincipalType.USER, null, null, ResourceType.SQC,
        "Resource type SQC is not yet supported"),
      Arguments.of(PrincipalType.USER, null, null, ResourceType.UNKNOWN,
        "Resource type UNKNOWN is not supported"),
      Arguments.of(PrincipalType.ORGANIZATION_TOKEN, "token-id", null, ResourceType.ORGANIZATION,
        "Only USER principal type is supported")
    );
  }

  @Test
  void getEffectiveRolesBatch_withMultiplePrincipals_shouldReturnAllRoles() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(user1, GlobalPermission.ADMINISTER);
    db.users().insertGlobalPermissionOnUser(user2, GlobalPermission.PROVISION_PROJECTS);

    EffectiveRolesBatchQuery query = new EffectiveRolesBatchQuery(
      List.of(user1.getUuid(), user2.getUuid()),
      PrincipalType.USER,
      List.of(),
      ResourceType.ORGANIZATION,
      null
    );

    List<EffectiveRoleBatch> batches = underTest.getEffectiveRolesBatch(query);

    assertThat(batches).hasSize(2)
      .anySatisfy(batch -> {
        assertThat(batch.principalId()).isEqualTo(user1.getUuid());
        assertThat(batch.effectiveRoles()).containsExactlyInAnyOrder(new EffectiveRole("admin"), new EffectiveRole("member"));
      })
      .anySatisfy(batch -> {
        assertThat(batch.principalId()).isEqualTo(user2.getUuid());
        assertThat(batch.effectiveRoles()).containsExactlyInAnyOrder(new EffectiveRole("provisioning"), new EffectiveRole("member"));
      });
  }

  @Test
  void getEffectiveRolesBatch_withRoleFilter_shouldReturnFilteredRoles() {
    UserDto user = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.ADMINISTER);
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.PROVISION_PROJECTS);

    EffectiveRolesBatchQuery query = new EffectiveRolesBatchQuery(
      List.of(user.getUuid()),
      PrincipalType.USER,
      List.of(),
      ResourceType.ORGANIZATION,
      "admin"
    );

    List<EffectiveRoleBatch> batches = underTest.getEffectiveRolesBatch(query);

    assertThat(batches).containsExactly(
      new EffectiveRoleBatch(user.getUuid(), PrincipalType.USER, DefaultOrganizationProvider.ID.toString(), ResourceType.ORGANIZATION, List.of(new EffectiveRole("admin")))
    );
  }

  @Test
  void getEffectiveRolesBatch_withInvalidPrincipalType_shouldThrowException() {
    EffectiveRolesBatchQuery query = new EffectiveRolesBatchQuery(
      List.of("token-id"),
      PrincipalType.ORGANIZATION_TOKEN,
      List.of(),
      ResourceType.ORGANIZATION,
      null
    );

    assertThatThrownBy(() -> underTest.getEffectiveRolesBatch(query))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Only USER principal type is supported");
  }

  @Test
  void getEffectiveRolesBatch_withMultipleResourceIds_shouldReturnRolesForEachResource() {
    UserDto user = db.users().insertUser();
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.USER, project1.getProjectDto());
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, project2.getProjectDto());

    EffectiveRolesBatchQuery query = new EffectiveRolesBatchQuery(
      List.of(user.getUuid()),
      PrincipalType.USER,
      List.of(project1.projectUuid(), project2.projectUuid()),
      ResourceType.PROJECT,
      null
    );

    List<EffectiveRoleBatch> batches = underTest.getEffectiveRolesBatch(query);

    assertThat(batches).containsExactlyInAnyOrder(
      new EffectiveRoleBatch(user.getUuid(), PrincipalType.USER, project1.projectUuid(), ResourceType.PROJECT, List.of(new EffectiveRole("user"))),
      new EffectiveRoleBatch(user.getUuid(), PrincipalType.USER, project2.projectUuid(), ResourceType.PROJECT, List.of(new EffectiveRole("admin")))
    );
  }

  @Test
  void getEffectiveRolesBatch_withLargeBatch_shouldReturnAllRoles() {
    // Create 100 users with different permissions to test batch query efficiency
    List<String> userUuids = IntStream.range(0, 100)
      .mapToObj(i -> {
        UserDto user = db.users().insertUser();
        // Alternate between ADMIN and PROVISIONING permissions
        if (i % 2 == 0) {
          db.users().insertGlobalPermissionOnUser(user, GlobalPermission.ADMINISTER);
        } else {
          db.users().insertGlobalPermissionOnUser(user, GlobalPermission.PROVISION_PROJECTS);
        }
        return user.getUuid();
      }).toList();

    EffectiveRolesBatchQuery query = new EffectiveRolesBatchQuery(
      userUuids, PrincipalType.USER, List.of(), ResourceType.ORGANIZATION, null
    );

    List<EffectiveRoleBatch> batches = underTest.getEffectiveRolesBatch(query);

    assertThat(batches).hasSize(100);
    assertThat(batches.stream().flatMap(batch -> batch.effectiveRoles().stream()).filter(role -> role.role().equals("admin")).count()).isEqualTo(50);
    assertThat(batches.stream().flatMap(batch -> batch.effectiveRoles().stream()).filter(role -> role.role().equals("provisioning")).count()).isEqualTo(50);
  }

  @Test
  void getEffectiveRolesBatch_withMultiplePrincipalsAndResources_shouldReturnAllCombinations() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    ProjectData project1 = db.components().insertPrivateProject();
    ProjectData project2 = db.components().insertPrivateProject();

    db.users().insertProjectPermissionOnUser(user1, ProjectPermission.USER, project1.getProjectDto());
    db.users().insertProjectPermissionOnUser(user1, ProjectPermission.ADMIN, project2.getProjectDto());
    db.users().insertProjectPermissionOnUser(user2, ProjectPermission.CODEVIEWER, project1.getProjectDto());
    db.users().insertProjectPermissionOnUser(user2, ProjectPermission.ISSUE_ADMIN, project2.getProjectDto());

    EffectiveRolesBatchQuery query = new EffectiveRolesBatchQuery(
      List.of(user1.getUuid(), user2.getUuid()),
      PrincipalType.USER,
      List.of(project1.projectUuid(), project2.projectUuid()),
      ResourceType.PROJECT,
      null
    );

    List<EffectiveRoleBatch> batches = underTest.getEffectiveRolesBatch(query);

    assertThat(batches).containsExactlyInAnyOrder(
      new EffectiveRoleBatch(user1.getUuid(), PrincipalType.USER, project1.projectUuid(), ResourceType.PROJECT, List.of(new EffectiveRole("user"))),
      new EffectiveRoleBatch(user1.getUuid(), PrincipalType.USER, project2.projectUuid(), ResourceType.PROJECT, List.of(new EffectiveRole("admin"))),
      new EffectiveRoleBatch(user2.getUuid(), PrincipalType.USER, project1.projectUuid(), ResourceType.PROJECT, List.of(new EffectiveRole("codeviewer"))),
      new EffectiveRoleBatch(user2.getUuid(), PrincipalType.USER, project2.projectUuid(), ResourceType.PROJECT, List.of(new EffectiveRole("issueadmin")))
    );
  }

  @Test
  void isOrganizationAdmin_whenUserIsAdmin_shouldReturnTrue() {
    UserDto user = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.ADMINISTER);

    assertThat(underTest.isOrganizationAdmin(user.getUuid(), DefaultOrganizationProvider.ID.toString())).isTrue();
  }

  @Test
  void isOrganizationAdmin_whenUserIsNotAdmin_shouldReturnFalse() {
    UserDto user = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.PROVISION_PROJECTS);

    assertThat(underTest.isOrganizationAdmin(user.getUuid(), DefaultOrganizationProvider.ID.toString())).isFalse();
  }

  @Test
  void isProjectAdmin_whenUserIsAdmin_shouldReturnTrue() {
    UserDto user = db.users().insertUser();
    ProjectData project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.ADMIN, project.getProjectDto());

    assertThat(underTest.isProjectAdmin(user.getUuid(), project.projectUuid())).isTrue();
  }

  @Test
  void isProjectAdmin_whenUserIsNotAdmin_shouldReturnFalse() {
    UserDto user = db.users().insertUser();
    ProjectData project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.USER, project.getProjectDto());

    assertThat(underTest.isProjectAdmin(user.getUuid(), project.projectUuid())).isFalse();
  }

  @Test
  void hasOrganizationRole_whenUserHasRole_shouldReturnTrue() {
    UserDto user = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.ADMINISTER);

    assertThat(underTest.hasOrganizationRole(user.getUuid(), DefaultOrganizationProvider.ID.toString(), "admin")).isTrue();
  }

  @Test
  void hasOrganizationRole_whenUserDoesNotHaveRole_shouldReturnFalse() {
    UserDto user = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.PROVISION_PROJECTS);

    assertThat(underTest.hasOrganizationRole(user.getUuid(), DefaultOrganizationProvider.ID.toString(), "admin")).isFalse();
  }

  @Test
  void hasProjectRole_whenUserHasRole_shouldReturnTrue() {
    UserDto user = db.users().insertUser();
    ProjectData project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.USER, project.getProjectDto());

    assertThat(underTest.hasProjectRole(user.getUuid(), project.projectUuid(), "user")).isTrue();
  }

  @Test
  void hasProjectRole_whenUserDoesNotHaveRole_shouldReturnFalse() {
    UserDto user = db.users().insertUser();
    ProjectData project = db.components().insertPrivateProject();
    db.users().insertProjectPermissionOnUser(user, ProjectPermission.CODEVIEWER, project.getProjectDto());

    assertThat(underTest.hasProjectRole(user.getUuid(), project.projectUuid(), "user")).isFalse();
  }

  @Test
  void getOrganizationRolesBatch_whenUserExistsWithNoOtherPermissions_shouldReturnMemberRole() {
    var user = db.users().insertUser();
    var query = new EffectiveRolesBatchQuery(
      List.of(user.getUuid()),
      PrincipalType.USER,
      List.of(),
      ResourceType.ORGANIZATION,
      null
    );

    var batches = underTest.getEffectiveRolesBatch(query);

    assertThat(batches).containsExactly(
      new EffectiveRoleBatch(user.getUuid(), PrincipalType.USER, DefaultOrganizationProvider.ID.toString(), ResourceType.ORGANIZATION, List.of(new EffectiveRole(MEMBER_ROLE)))
    );
  }

  @Test
  void getOrganizationRolesBatch_whenUserExistsWithOtherPermissions_shouldIncludeMemberRole() {
    var user = db.users().insertUser();
    db.users().insertGlobalPermissionOnUser(user, GlobalPermission.ADMINISTER);
    var query = new EffectiveRolesBatchQuery(
      List.of(user.getUuid()),
      PrincipalType.USER,
      List.of(),
      ResourceType.ORGANIZATION,
      null
    );

    var batches = underTest.getEffectiveRolesBatch(query);

    assertThat(batches).hasSize(1);
    assertThat(batches.getFirst().effectiveRoles()).containsExactlyInAnyOrder(new EffectiveRole("admin"), new EffectiveRole(MEMBER_ROLE));
  }

  @Test
  void getOrganizationRolesBatch_whenUserDoesNotExist_shouldNotReturnMemberRole() {
    var query = new EffectiveRolesBatchQuery(
      List.of("non-existent-uuid"),
      PrincipalType.USER,
      List.of(),
      ResourceType.ORGANIZATION,
      null
    );

    var batches = underTest.getEffectiveRolesBatch(query);

    assertThat(batches).isEmpty();
  }

  @Test
  void getOrganizationRolesBatch_withMixedExistingAndNonExistingUsers_shouldOnlyAddMemberForExistingUsers() {
    var existingUser = db.users().insertUser();
    var query = new EffectiveRolesBatchQuery(
      List.of(existingUser.getUuid(), "non-existent-uuid"),
      PrincipalType.USER,
      List.of(),
      ResourceType.ORGANIZATION,
      null
    );

    var batches = underTest.getEffectiveRolesBatch(query);

    assertThat(batches).containsExactly(
      new EffectiveRoleBatch(existingUser.getUuid(), PrincipalType.USER, DefaultOrganizationProvider.ID.toString(), ResourceType.ORGANIZATION, List.of(new EffectiveRole(MEMBER_ROLE)))
    );
  }
}
