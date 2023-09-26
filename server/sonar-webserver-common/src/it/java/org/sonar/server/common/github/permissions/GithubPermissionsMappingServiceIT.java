/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.common.github.permissions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.AuditPersister;
import org.sonar.db.provisioning.GithubPermissionsMappingDao;
import org.sonar.db.provisioning.GithubPermissionsMappingDto;
import org.sonar.server.common.permission.Operation;
import org.sonar.server.exceptions.NotFoundException;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.server.common.github.permissions.GithubPermissionsMappingService.ADMIN_GITHUB_ROLE;
import static org.sonar.server.common.github.permissions.GithubPermissionsMappingService.MAINTAIN_GITHUB_ROLE;
import static org.sonar.server.common.github.permissions.GithubPermissionsMappingService.READ_GITHUB_ROLE;
import static org.sonar.server.common.github.permissions.GithubPermissionsMappingService.TRIAGE_GITHUB_ROLE;
import static org.sonar.server.common.github.permissions.GithubPermissionsMappingService.WRITE_GITHUB_ROLE;

public class GithubPermissionsMappingServiceIT {

  private static final String CUSTOM_ROLE_NAME = "customRole1";

  private static final SonarqubePermissions NO_SQ_PERMISSIONS = new SonarqubePermissions(false, false, false, false, false, false);

  @Rule
  public DbTester db = DbTester.create();
  private final DbSession dbSession = db.getSession();

  private final AuditPersister auditPersister = mock();
  private final GithubPermissionsMappingDao githubPermissionsMappingDao = new GithubPermissionsMappingDao(auditPersister);

  private final UuidFactory uuidFactory = new SequenceUuidFactory();

  private final GithubPermissionsMappingService underTest = new GithubPermissionsMappingService(db.getDbClient(), githubPermissionsMappingDao, uuidFactory);

  @Test
  public void getPermissionsMapping_whenMappingNotDefined_returnMappingEntirelyFalse() {
    List<GithubPermissionsMapping> actualPermissionsMapping = underTest.getPermissionsMapping();

    List<GithubPermissionsMapping> expectedPermissionsMapping = List.of(
      new GithubPermissionsMapping(READ_GITHUB_ROLE, true, NO_SQ_PERMISSIONS),
      new GithubPermissionsMapping(TRIAGE_GITHUB_ROLE, true, NO_SQ_PERMISSIONS),
      new GithubPermissionsMapping(WRITE_GITHUB_ROLE, true, NO_SQ_PERMISSIONS),
      new GithubPermissionsMapping(MAINTAIN_GITHUB_ROLE, true, NO_SQ_PERMISSIONS),
      new GithubPermissionsMapping(ADMIN_GITHUB_ROLE, true, NO_SQ_PERMISSIONS));

    assertThat(actualPermissionsMapping).containsAll(expectedPermissionsMapping);
  }

  @Test
  public void getPermissionsMapping_whenMappingDefined_returnMapping() {
    Map<String, Set<String>> githubRolesToSqPermissions = Map.of(
      CUSTOM_ROLE_NAME, Set.of("user"),
      READ_GITHUB_ROLE, Set.of("user", "codeviewer"),
      WRITE_GITHUB_ROLE, Set.of("user", "codeviewer", "issueadmin", "securityhotspotadmin", "admin", "scan"));
    persistGithubPermissionsMapping(githubRolesToSqPermissions);

    List<GithubPermissionsMapping> actualPermissionsMapping = underTest.getPermissionsMapping();

    List<GithubPermissionsMapping> expectedPermissionsMapping = List.of(
      new GithubPermissionsMapping(CUSTOM_ROLE_NAME, false, new SonarqubePermissions(true, false, false, false, false, false)),
      new GithubPermissionsMapping(READ_GITHUB_ROLE, true, new SonarqubePermissions(true, true, false, false, false, false)),
      new GithubPermissionsMapping(TRIAGE_GITHUB_ROLE, true, NO_SQ_PERMISSIONS),
      new GithubPermissionsMapping(WRITE_GITHUB_ROLE, true, new SonarqubePermissions(true, true, true, true, true, true)),
      new GithubPermissionsMapping(MAINTAIN_GITHUB_ROLE, true, NO_SQ_PERMISSIONS),
      new GithubPermissionsMapping(ADMIN_GITHUB_ROLE, true, NO_SQ_PERMISSIONS));

    assertThat(actualPermissionsMapping).containsAll(expectedPermissionsMapping);
  }

  private void persistGithubPermissionsMapping(Map<String, Set<String>> githubRolesToSonarqubePermissions) {
    for (Map.Entry<String, Set<String>> githubRoleToSonarqubePermissions : githubRolesToSonarqubePermissions.entrySet()) {
      String githubRole = githubRoleToSonarqubePermissions.getKey();
      githubRoleToSonarqubePermissions.getValue()
        .forEach(permission -> githubPermissionsMappingDao.insert(
          dbSession,
          new GithubPermissionsMappingDto("uuid_" + githubRole + "_" + permission, githubRole, permission)));
    }
    dbSession.commit();
  }

  @Test
  public void updatePermissionsMappings_onBaseRole_shouldAddAndRemovePermissions() {
    Map<String, Set<String>> githubRolesToSqPermissions = Map.of(READ_GITHUB_ROLE, Set.of("user", "codeviewer"));
    persistGithubPermissionsMapping(githubRolesToSqPermissions);

    PermissionMappingChange permToAdd1 = new PermissionMappingChange(READ_GITHUB_ROLE, "issueadmin", Operation.ADD);
    PermissionMappingChange permToAdd2 = new PermissionMappingChange(READ_GITHUB_ROLE, "scan", Operation.ADD);
    PermissionMappingChange permToRemove1 = new PermissionMappingChange(READ_GITHUB_ROLE, "user", Operation.REMOVE);
    PermissionMappingChange permToRemove2 = new PermissionMappingChange(READ_GITHUB_ROLE, "codeviewer", Operation.REMOVE);

    underTest.updatePermissionsMappings(Set.of(permToAdd1, permToAdd2, permToRemove1, permToRemove2));

    GithubPermissionsMapping updatedPermissionsMapping = underTest.getPermissionsMappingForGithubRole(READ_GITHUB_ROLE);

    SonarqubePermissions expectedSqPermissions = new SonarqubePermissions(false, false, true, false, false, true);
    GithubPermissionsMapping expectedPermissionsMapping = new GithubPermissionsMapping(READ_GITHUB_ROLE, true, expectedSqPermissions);
    assertThat(updatedPermissionsMapping).isEqualTo(expectedPermissionsMapping);
  }

  @Test
  public void updatePermissionsMappings_onCustomRole_shouldAddAndRemovePermissions() {
    Map<String, Set<String>> githubRolesToSqPermissions = Map.of(CUSTOM_ROLE_NAME, Set.of("user", "codeviewer"));
    persistGithubPermissionsMapping(githubRolesToSqPermissions);

    PermissionMappingChange permToAdd1 = new PermissionMappingChange(CUSTOM_ROLE_NAME, "issueadmin", Operation.ADD);
    PermissionMappingChange permToRemove1 = new PermissionMappingChange(CUSTOM_ROLE_NAME, "user", Operation.REMOVE);

    underTest.updatePermissionsMappings(Set.of(permToAdd1, permToRemove1));

    GithubPermissionsMapping updatedPermissionsMapping = underTest.getPermissionsMappingForGithubRole(CUSTOM_ROLE_NAME);

    SonarqubePermissions expectedSqPermissions = new SonarqubePermissions(false, true, true, false, false, false);
    GithubPermissionsMapping expectedPermissionsMapping = new GithubPermissionsMapping(CUSTOM_ROLE_NAME, false, expectedSqPermissions);
    assertThat(updatedPermissionsMapping).isEqualTo(expectedPermissionsMapping);
  }

  @Test
  public void updatePermissionsMappings_whenRemovingNonExistingPermission_isNoOp() {
    PermissionMappingChange permToRemove1 = new PermissionMappingChange(READ_GITHUB_ROLE, "user", Operation.REMOVE);

    underTest.updatePermissionsMappings(Set.of(permToRemove1));

    GithubPermissionsMapping updatedPermissionsMapping = underTest.getPermissionsMappingForGithubRole(READ_GITHUB_ROLE);

    GithubPermissionsMapping expectedPermissionsMapping = new GithubPermissionsMapping(READ_GITHUB_ROLE, true, NO_SQ_PERMISSIONS);
    assertThat(updatedPermissionsMapping).isEqualTo(expectedPermissionsMapping);
  }

  @Test
  public void updatePermissionsMappings_whenAddingAlreadyExistingPermission_isNoOp() {
    Map<String, Set<String>> githubRolesToSqPermissions = Map.of(READ_GITHUB_ROLE, Set.of("user", "codeviewer"));
    persistGithubPermissionsMapping(githubRolesToSqPermissions);
    PermissionMappingChange permToAdd1 = new PermissionMappingChange(READ_GITHUB_ROLE, "user", Operation.ADD);

    underTest.updatePermissionsMappings(Set.of(permToAdd1));

    GithubPermissionsMapping updatedPermissionsMapping = underTest.getPermissionsMappingForGithubRole(READ_GITHUB_ROLE);

    SonarqubePermissions expectedSqPermissions = new SonarqubePermissions(true, true, false, false, false, false);
    GithubPermissionsMapping expectedPermissionsMapping = new GithubPermissionsMapping(READ_GITHUB_ROLE, true, expectedSqPermissions);
    assertThat(updatedPermissionsMapping).isEqualTo(expectedPermissionsMapping);
  }

  @Test
  public void updatePermissionsMappings_handlesUpdatesForDifferentRoles() {
    PermissionMappingChange permToAdd1 = new PermissionMappingChange(READ_GITHUB_ROLE, "user", Operation.ADD);
    PermissionMappingChange permToAdd2 = new PermissionMappingChange(WRITE_GITHUB_ROLE, "user", Operation.ADD);

    underTest.updatePermissionsMappings(Set.of(permToAdd1, permToAdd2));

    SonarqubePermissions userOnlySqPermission = new SonarqubePermissions(true, false, false, false, false, false);

    GithubPermissionsMapping updatedPermissionsMapping = underTest.getPermissionsMappingForGithubRole(READ_GITHUB_ROLE);
    assertThat(updatedPermissionsMapping).isEqualTo(new GithubPermissionsMapping(READ_GITHUB_ROLE, true, userOnlySqPermission));

    updatedPermissionsMapping = underTest.getPermissionsMappingForGithubRole(WRITE_GITHUB_ROLE);
    assertThat(updatedPermissionsMapping).isEqualTo(new GithubPermissionsMapping(WRITE_GITHUB_ROLE, true, userOnlySqPermission));
  }

  @Test
  public void getPermissionsMappingForGithubRole_onBaseRole_shouldReturnMappingOnlyForRole() {
    Map<String, Set<String>> githubRolesToSqPermissions = Map.of(
      READ_GITHUB_ROLE, Set.of("user", "codeviewer"),
      WRITE_GITHUB_ROLE, Set.of("user", "codeviewer", "issueadmin", "securityhotspotadmin", "admin", "scan"));
    persistGithubPermissionsMapping(githubRolesToSqPermissions);

    GithubPermissionsMapping actualPermissionsMapping = underTest.getPermissionsMappingForGithubRole(READ_GITHUB_ROLE);

    SonarqubePermissions expectedSqPermissions = new SonarqubePermissions(true, true, false, false, false, false);
    GithubPermissionsMapping expectedPermissionsMapping = new GithubPermissionsMapping(READ_GITHUB_ROLE, true, expectedSqPermissions);

    assertThat(actualPermissionsMapping).isEqualTo(expectedPermissionsMapping);
  }

  @Test
  public void getPermissionsMappingForGithubRole_onCustomRole_shouldReturnMappingOnlyForRole() {
    Map<String, Set<String>> githubRolesToSqPermissions = Map.of(
      CUSTOM_ROLE_NAME, Set.of("admin"),
      WRITE_GITHUB_ROLE, Set.of("user", "codeviewer", "issueadmin", "securityhotspotadmin", "admin", "scan"));
    persistGithubPermissionsMapping(githubRolesToSqPermissions);

    GithubPermissionsMapping actualPermissionsMapping = underTest.getPermissionsMappingForGithubRole(CUSTOM_ROLE_NAME);

    SonarqubePermissions expectedSqPermissions = new SonarqubePermissions(false, false, false, false, true, false);
    GithubPermissionsMapping expectedPermissionsMapping = new GithubPermissionsMapping(CUSTOM_ROLE_NAME, false, expectedSqPermissions);

    assertThat(actualPermissionsMapping).isEqualTo(expectedPermissionsMapping);
  }

  @Test
  public void deletePermissionMappings_whenTryingToDeleteForBaseRole_shouldThrow() {
    assertThatThrownBy(() -> underTest.deletePermissionMappings(READ_GITHUB_ROLE))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Deleting permission mapping for GitHub base role '" + READ_GITHUB_ROLE + "' is not allowed.");
  }

  @Test
  public void deletePermissionMappings_whenNoMappingsExistForGithubRole_shouldThrow() {
    assertThatThrownBy(() -> underTest.deletePermissionMappings(CUSTOM_ROLE_NAME))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Role '" + CUSTOM_ROLE_NAME + "' not found.");
  }

  @Test
  public void deletePermissionMappings_whenTryingToDeleteForCustomRole_shouldDeleteMapping() {
    Map<String, Set<String>> githubRolesToSqPermissions = Map.of(
      READ_GITHUB_ROLE, Set.of("user", "codeviewer"),
      WRITE_GITHUB_ROLE, Set.of("user", "codeviewer", "issueadmin", "securityhotspotadmin", "admin", "scan"),
      CUSTOM_ROLE_NAME, Set.of("user", "codeviewer", "scan"),
      "customRole2", Set.of("user", "codeviewer"));

    persistGithubPermissionsMapping(githubRolesToSqPermissions);
    underTest.deletePermissionMappings("customRole2");

    List<GithubPermissionsMapping> allPermissionMappings = underTest.getPermissionsMapping();

    assertThat(allPermissionMappings)
      .containsExactlyInAnyOrder(
        new GithubPermissionsMapping(READ_GITHUB_ROLE, true, new SonarqubePermissions(true, true, false, false, false, false)),
        new GithubPermissionsMapping(WRITE_GITHUB_ROLE, true, new SonarqubePermissions(true, true, true, true, true, true)),
        new GithubPermissionsMapping(TRIAGE_GITHUB_ROLE, true, NO_SQ_PERMISSIONS),
        new GithubPermissionsMapping(MAINTAIN_GITHUB_ROLE, true, NO_SQ_PERMISSIONS),
        new GithubPermissionsMapping(ADMIN_GITHUB_ROLE, true, NO_SQ_PERMISSIONS),
        new GithubPermissionsMapping(CUSTOM_ROLE_NAME, false, new SonarqubePermissions(true, true, false, false, false, true)));
  }

  @Test
  public void createPermissionMapping_whenRoleExists_shouldThrow() {
    Map<String, Set<String>> githubRolesToSqPermissions = Map.of(CUSTOM_ROLE_NAME, Set.of("user", "codeviewer"));
    persistGithubPermissionsMapping(githubRolesToSqPermissions);

    GithubPermissionsMapping request = new GithubPermissionsMapping(CUSTOM_ROLE_NAME, false, new SonarqubePermissions(true, true, true, true, true, true));
    assertThatThrownBy(() -> underTest.createPermissionMapping(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Role %s already exists, it can't be created again.", CUSTOM_ROLE_NAME));
  }

  @Test
  public void createPermissionMapping_whenRoleNameConflictsWithBaseRole_shouldThrow() {
    assertBaseRoleConflict("read");
    assertBaseRoleConflict("Read");
    assertBaseRoleConflict("READ");
  }

  private void assertBaseRoleConflict(String role) {
    GithubPermissionsMapping request = new GithubPermissionsMapping(role, false, new SonarqubePermissions(true, true, true, true, true, true));
    assertThatThrownBy(() -> underTest.createPermissionMapping(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Role %s can conflicts with a GitHub base role, please chose another name.", role));
  }

  @Test
  public void createPermissionMapping_whenNoPermissions_shouldThrow() {
    GithubPermissionsMapping request = new GithubPermissionsMapping(CUSTOM_ROLE_NAME, false, new SonarqubePermissions(false, false, false, false, false, false));
    assertThatThrownBy(() -> underTest.createPermissionMapping(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(format("Role %s has no permission set, please set at least one permission.", CUSTOM_ROLE_NAME));
  }

  @Test
  public void createPermissionMapping_whenValidRequests_shouldCreateMapping() {
    GithubPermissionsMapping role1 = new GithubPermissionsMapping("role1", false, new SonarqubePermissions(false, false, false, false, false, true));
    GithubPermissionsMapping role2 = new GithubPermissionsMapping("role2", false, new SonarqubePermissions(false, false, false, false, true, true));
    GithubPermissionsMapping role3 = new GithubPermissionsMapping("role3", false, new SonarqubePermissions(false, false, false, true, true, true));
    GithubPermissionsMapping role4 = new GithubPermissionsMapping("role4", false, new SonarqubePermissions(false, false, true, true, true, true));
    GithubPermissionsMapping role5 = new GithubPermissionsMapping("role5", false, new SonarqubePermissions(false, true, true, true, true, true));
    GithubPermissionsMapping role6 = new GithubPermissionsMapping("role6", false, new SonarqubePermissions(true, true, true, true, true, true));

    underTest.createPermissionMapping(role1);
    underTest.createPermissionMapping(role2);
    underTest.createPermissionMapping(role3);
    underTest.createPermissionMapping(role4);
    underTest.createPermissionMapping(role5);
    underTest.createPermissionMapping(role6);

    assertThat(underTest.getPermissionsMapping())
      .contains(role1, role2, role3, role4, role5, role6);
  }

}
