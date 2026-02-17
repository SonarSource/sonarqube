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
import java.util.Set;
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
import org.sonarsource.users.api.model.Principal;
import org.sonarsource.users.api.model.PrincipalType;
import org.sonarsource.users.api.model.ResourceType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EffectiveRolesServiceImplTest {

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

    Set<String> roles = underTest.getEffectiveRoles(query);

    assertThat(roles).containsExactlyInAnyOrder("admin", "provisioning");
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

    Set<String> roles = underTest.getEffectiveRoles(query);

    assertThat(roles).containsExactlyInAnyOrder("user", "codeviewer");
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

    Set<EffectiveRole> roles = underTest.getEffectiveRolesBatch(query);

    assertThat(roles)
      .hasSize(2)
      .contains(
        new EffectiveRole(user1.getUuid(), PrincipalType.USER, DefaultOrganizationProvider.ID.toString(), ResourceType.ORGANIZATION, "admin"),
        new EffectiveRole(user2.getUuid(), PrincipalType.USER, DefaultOrganizationProvider.ID.toString(), ResourceType.ORGANIZATION, "provisioning")
      );
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

    Set<EffectiveRole> roles = underTest.getEffectiveRolesBatch(query);

    assertThat(roles)
      .hasSize(1)
      .contains(
        new EffectiveRole(user.getUuid(), PrincipalType.USER, DefaultOrganizationProvider.ID.toString(), ResourceType.ORGANIZATION, "admin")
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

    Set<EffectiveRole> roles = underTest.getEffectiveRolesBatch(query);

    assertThat(roles)
      .hasSize(2)
      .contains(
        new EffectiveRole(user.getUuid(), PrincipalType.USER, project1.projectUuid(), ResourceType.PROJECT, "user"),
        new EffectiveRole(user.getUuid(), PrincipalType.USER, project2.projectUuid(), ResourceType.PROJECT, "admin")
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

    Set<EffectiveRole> roles = underTest.getEffectiveRolesBatch(query);

    assertThat(roles).hasSize(100);
    assertThat(roles.stream().filter(role -> role.role().equals("admin")).count()).isEqualTo(50);
    assertThat(roles.stream().filter(role -> role.role().equals("provisioning")).count()).isEqualTo(50);
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

    Set<EffectiveRole> roles = underTest.getEffectiveRolesBatch(query);

    assertThat(roles)
      .hasSize(4)
      .contains(
        new EffectiveRole(user1.getUuid(), PrincipalType.USER, project1.projectUuid(), ResourceType.PROJECT, "user"),
        new EffectiveRole(user1.getUuid(), PrincipalType.USER, project2.projectUuid(), ResourceType.PROJECT, "admin"),
        new EffectiveRole(user2.getUuid(), PrincipalType.USER, project1.projectUuid(), ResourceType.PROJECT, "codeviewer"),
        new EffectiveRole(user2.getUuid(), PrincipalType.USER, project2.projectUuid(), ResourceType.PROJECT, "issueadmin")
      );
  }
}
