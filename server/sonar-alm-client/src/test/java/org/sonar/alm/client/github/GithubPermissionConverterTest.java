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
package org.sonar.alm.client.github;

import java.util.Arrays;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.sonar.auth.github.GsonRepositoryPermissions;
import org.sonar.db.provisioning.GithubPermissionsMappingDto;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  GithubPermissionConverterTest.ToSonarqubeRolesForDefaultRepositoryPermissionTest.class,
  GithubPermissionConverterTest.ToSonarqubeRolesWithFallbackOnRepositoryPermissionsTest.class
})
public class GithubPermissionConverterTest {

  private static final Set<GithubPermissionsMappingDto> ALL_PERMISSIONS_MAPPING_FROM_DB = Set.of(
    new GithubPermissionsMappingDto("uuid1", "read", "roleRead"),
    new GithubPermissionsMappingDto("uuid2", "triage", "roleTriage"),
    new GithubPermissionsMappingDto("uuid3", "write", "roleWrite"),
    new GithubPermissionsMappingDto("uuid4", "maintain", "roleMaintain"),
    new GithubPermissionsMappingDto("uuid5", "admin", "roleAdmin")
  ) ;

  private static final GsonRepositoryPermissions NO_PERMS = new GsonRepositoryPermissions(false, false, false, false, false);
  private static final GsonRepositoryPermissions READ_PERMS = new GsonRepositoryPermissions(false, false, false, false, true);
  private static final GsonRepositoryPermissions TRIAGE_PERMS = new GsonRepositoryPermissions(false, false, false, true, true);
  private static final GsonRepositoryPermissions WRITE_PERMS = new GsonRepositoryPermissions(false, false, true, true, true);
  private static final GsonRepositoryPermissions MAINTAIN_PERMS = new GsonRepositoryPermissions(false, true, true, true, true);
  private static final GsonRepositoryPermissions ADMIN_PERMS = new GsonRepositoryPermissions(true, true, true, true, true);

  @RunWith(Parameterized.class)
  public static class ToSonarqubeRolesWithFallbackOnRepositoryPermissionsTest {
    private final GithubPermissionConverter githubPermissionConverter = new GithubPermissionConverter();
    private final String role;
    private final GsonRepositoryPermissions repositoryPermissions;
    private final Set<String> expectedSqPermissions;

    @Parameterized.Parameters(name = "GH role:{0}, GH perms:{1}, Expected SQ perms:{2}")
    public static Iterable<Object[]> testData() {
      return Arrays.asList(new Object[][] {
        {"none", NO_PERMS, Set.of()},
        {"read", NO_PERMS, Set.of("roleRead")},
        {"read", READ_PERMS, Set.of("roleRead")},
        {"pull", NO_PERMS, Set.of("roleRead")},
        {"triage", NO_PERMS, Set.of("roleTriage")},
        {"write", NO_PERMS, Set.of("roleWrite")},
        {"push", NO_PERMS, Set.of("roleWrite")},
        {"maintain", NO_PERMS, Set.of("roleMaintain")},
        {"admin", NO_PERMS, Set.of("roleAdmin")},
        {"custom_role_extending_read", READ_PERMS, Set.of("roleRead")},
        {"custom_role_extending_triage", TRIAGE_PERMS, Set.of("roleTriage")},
        {"custom_role_extending_write", WRITE_PERMS, Set.of("roleWrite")},
        {"custom_role_extending_maintain", MAINTAIN_PERMS, Set.of("roleMaintain")},
        {"custom_role_extending_admin", ADMIN_PERMS, Set.of("roleAdmin")},
      });
    }

    public ToSonarqubeRolesWithFallbackOnRepositoryPermissionsTest(String role, GsonRepositoryPermissions repositoryPermissions, Set<String> expectedSqPermissions) {
      this.role = role;
      this.repositoryPermissions = repositoryPermissions;
      this.expectedSqPermissions = expectedSqPermissions;
    }

    @Test
    public void toGithubRepositoryPermissions_convertsCorrectly() {
      Set<String> actualPermissions = githubPermissionConverter.toSonarqubeRolesWithFallbackOnRepositoryPermissions(ALL_PERMISSIONS_MAPPING_FROM_DB, role, repositoryPermissions);
      assertThat(actualPermissions).isEqualTo(expectedSqPermissions);
    }
  }

  @RunWith(Parameterized.class)
  public static class ToSonarqubeRolesForDefaultRepositoryPermissionTest {
    private final GithubPermissionConverter githubPermissionConverter = new GithubPermissionConverter();
    private final String role;
    private final Set<String> expectedSqPermissions;

    @Parameterized.Parameters(name = "GH role:{0}, GH perms:{1}, Expected SQ perms:{2}")
    public static Iterable<Object[]> testData() {
      return Arrays.asList(new Object[][] {
        {"none", Set.of()},
        {"read", Set.of("roleRead")},
        {"triage", Set.of("roleTriage")},
        {"write", Set.of("roleWrite")},
        {"maintain", Set.of("roleMaintain")},
        {"admin", Set.of("roleAdmin")}
      });
    }

    public ToSonarqubeRolesForDefaultRepositoryPermissionTest(String role, Set<String> expectedSqPermissions) {
      this.role = role;
      this.expectedSqPermissions = expectedSqPermissions;
    }

    @Test
    public void toGithubRepositoryPermissions_convertsCorrectly() {
      Set<String> actualPermissions = githubPermissionConverter.toSonarqubeRolesForDefaultRepositoryPermission(ALL_PERMISSIONS_MAPPING_FROM_DB, role);
      assertThat(actualPermissions).isEqualTo(expectedSqPermissions);
    }
  }
}
