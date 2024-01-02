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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.auth.github.GsonRepositoryPermissions;
import org.sonar.db.provisioning.GithubPermissionsMappingDto;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.sonar.server.permission.PermissionServiceImpl.ALL_PROJECT_PERMISSIONS;

public class GithubPermissionConverter {
  private static final Logger LOG = LoggerFactory.getLogger(GithubPermissionConverter.class);
  private static final String PULL_GROUP_PERMISSION = "pull";
  private static final String TRIAGE_GROUP_PERMISSION = "triage";
  private static final String PUSH_GROUP_PERMISSION = "push";
  private static final String MAINTAIN_GROUP_PERMISSION = "maintain";
  private static final String ADMIN_GROUP_PERMISSION = "admin";
  private static final String READ_GITHUB_ROLE = "read";
  private static final String TRIAGE_GITHUB_ROLE = "triage";
  private static final String WRITE_GITHUB_ROLE = "write";
  private static final String MAINTAIN_GITHUB_ROLE = "maintain";
  private static final String ADMIN_GITHUB_ROLE = "admin";

  private static final Map<String, String> GITHUB_GROUP_PERMISSION_TO_ROLE_NAME = Map.of(
    PULL_GROUP_PERMISSION, READ_GITHUB_ROLE,
    TRIAGE_GROUP_PERMISSION, TRIAGE_GITHUB_ROLE,
    PUSH_GROUP_PERMISSION, WRITE_GITHUB_ROLE,
    MAINTAIN_GROUP_PERMISSION, MAINTAIN_GITHUB_ROLE,
    ADMIN_GROUP_PERMISSION, ADMIN_GITHUB_ROLE);

  private static final Map<GsonRepositoryPermissions, String> GITHUB_PERMISSION_TO_GITHUB_BASE_ROLE = Map.of(
    new GsonRepositoryPermissions(false, false, false, false, false), "none",
    new GsonRepositoryPermissions(false, false, false, false, true), READ_GITHUB_ROLE,
    new GsonRepositoryPermissions(false, false, false, true, true), TRIAGE_GITHUB_ROLE,
    new GsonRepositoryPermissions(false, false, true, true, true), WRITE_GITHUB_ROLE,
    new GsonRepositoryPermissions(false, true, true, true, true), MAINTAIN_GITHUB_ROLE,
    new GsonRepositoryPermissions(true, true, true, true, true), ADMIN_GITHUB_ROLE
  );

  public Map<String, Boolean> toSonarqubeRolesToHasPermissions(Set<String> sonarqubeRoles) {
    return ALL_PROJECT_PERMISSIONS.stream()
      .collect(toMap(identity(), sonarqubeRoles::contains));
  }

  public Set<String> toSonarqubeRolesWithFallbackOnRepositoryPermissions(Set<GithubPermissionsMappingDto> allPermissionsMappings,
    String githubRoleOrPermission, GsonRepositoryPermissions repositoryPermissions) {
    String roleName = toRoleName(githubRoleOrPermission);
    return toSonarqubeRoles(allPermissionsMappings, roleName, repositoryPermissions);
  }

  private static String toRoleName(String permission) {
    return GITHUB_GROUP_PERMISSION_TO_ROLE_NAME.getOrDefault(permission, permission);
  }

  public Set<String> toSonarqubeRolesForDefaultRepositoryPermission(Set<GithubPermissionsMappingDto> allPermissionsMappings, String roleName) {
    return toSonarqubeRoles(allPermissionsMappings, roleName, null);
  }

  private static Set<String> toSonarqubeRoles(Set<GithubPermissionsMappingDto> allPermissionsMappings, String githubRoleName,
    @Nullable GsonRepositoryPermissions repositoryPermissions) {
    Map<String, List<GithubPermissionsMappingDto>> permissionMappings = allPermissionsMappings.stream()
      .collect(Collectors.groupingBy(GithubPermissionsMappingDto::githubRole));

    Set<String> sonarqubePermissions = Optional.ofNullable(permissionMappings.get(githubRoleName))
      .orElse(GithubPermissionConverter.computeBaseRoleAndGetSqPermissions(permissionMappings, repositoryPermissions))
      .stream()
      .map(GithubPermissionsMappingDto::sonarqubePermission)
      .collect(Collectors.toSet());

    if (sonarqubePermissions.isEmpty()) {
      LOG.warn("No permission found matching role:{}, and permissions {}", githubRoleName, repositoryPermissions);
    }
    return sonarqubePermissions;
  }

  private static List<GithubPermissionsMappingDto> computeBaseRoleAndGetSqPermissions(Map<String, List<GithubPermissionsMappingDto>> permissionMappings,
    @Nullable GsonRepositoryPermissions repositoryPermissions) {
    return Optional.ofNullable(repositoryPermissions)
      .map(GITHUB_PERMISSION_TO_GITHUB_BASE_ROLE::get)
      .map(permissionMappings::get)
      .orElse(List.of());
  }

}
