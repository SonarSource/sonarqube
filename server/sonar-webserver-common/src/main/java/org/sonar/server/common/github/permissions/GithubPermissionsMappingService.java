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

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.provisioning.GithubPermissionsMappingDao;
import org.sonar.db.provisioning.GithubPermissionsMappingDto;
import org.sonar.server.exceptions.NotFoundException;

import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.sonar.api.utils.Preconditions.checkArgument;
import static org.sonar.server.common.permission.Operation.ADD;
import static org.sonar.server.common.permission.Operation.REMOVE;

public class GithubPermissionsMappingService {
  public static final String READ_GITHUB_ROLE = "read";
  public static final String TRIAGE_GITHUB_ROLE = "triage";
  public static final String WRITE_GITHUB_ROLE = "write";
  public static final String MAINTAIN_GITHUB_ROLE = "maintain";
  public static final String ADMIN_GITHUB_ROLE = "admin";

  private static final Set<String> GITHUB_BASE_ROLES = Set.of(
    READ_GITHUB_ROLE,
    TRIAGE_GITHUB_ROLE,
    WRITE_GITHUB_ROLE,
    MAINTAIN_GITHUB_ROLE,
    ADMIN_GITHUB_ROLE);

  private static final Map<String, Consumer<SonarqubePermissions.Builder>> permissionAsStringToSonarqubePermission = Map.of(
    UserRole.USER, builder -> builder.user(true),
    UserRole.CODEVIEWER, builder -> builder.codeViewer(true),
    UserRole.ISSUE_ADMIN, builder -> builder.issueAdmin(true),
    UserRole.SECURITYHOTSPOT_ADMIN, builder -> builder.securityHotspotAdmin(true),
    UserRole.ADMIN, builder -> builder.admin(true),
    UserRole.SCAN, builder -> builder.scan(true));

  private final DbClient dbClient;
  private final GithubPermissionsMappingDao githubPermissionsMappingDao;
  private final UuidFactory uuidFactory;

  public GithubPermissionsMappingService(DbClient dbClient, GithubPermissionsMappingDao githubPermissionsMappingDao, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.githubPermissionsMappingDao = githubPermissionsMappingDao;
    this.uuidFactory = uuidFactory;
  }

  public GithubPermissionsMapping getPermissionsMappingForGithubRole(String githubRole) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<GithubPermissionsMappingDto> permissionsMappingForGithubRole = getPermissionsMappingForGithubRole(dbSession, githubRole);
      return toGithubPermissionsMapping(permissionsMappingForGithubRole, githubRole);
    }
  }

  public List<GithubPermissionsMapping> getPermissionsMapping() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return toGithubPermissionsMappings(githubPermissionsMappingDao.findAll(dbSession));
    }
  }

  private static List<GithubPermissionsMapping> toGithubPermissionsMappings(Set<GithubPermissionsMappingDto> githubPermissionsMappingDtos) {
    Map<String, Set<GithubPermissionsMappingDto>> githubRoleToGithubPermissionsMappingDto = githubPermissionsMappingDtos.stream()
      .collect(groupingBy(GithubPermissionsMappingDto::githubRole, toSet()));

    Set<String> allRoles = Sets.union(GITHUB_BASE_ROLES, githubRoleToGithubPermissionsMappingDto.keySet());
    return allRoles.stream()
      .map(githubRole -> toGithubPermissionsMapping(githubRoleToGithubPermissionsMappingDto.getOrDefault(githubRole, Set.of()), githubRole))
      .toList();
  }

  private static GithubPermissionsMapping toGithubPermissionsMapping(Set<GithubPermissionsMappingDto> githubPermissionsMappingDtos, String githubRole) {
    boolean isBaseRole = GITHUB_BASE_ROLES.contains(githubRole);
    SonarqubePermissions sonarqubePermissions = getSonarqubePermissions(githubPermissionsMappingDtos);
    return new GithubPermissionsMapping(githubRole, isBaseRole, sonarqubePermissions);
  }

  public void updatePermissionsMappings(Set<PermissionMappingChange> permissionChanges) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, List<PermissionMappingChange>> githubRolesToChanges = permissionChanges.stream()
        .collect(groupingBy(PermissionMappingChange::githubRole));
      githubRolesToChanges.forEach((githubRole, changes) -> updatePermissionsMappings(dbSession, githubRole, changes));
      dbSession.commit();
    }
  }

  public void deletePermissionMappings(String githubRole) {
    checkArgument(!GITHUB_BASE_ROLES.contains(githubRole), "Deleting permission mapping for GitHub base role '" + githubRole + "' is not allowed.");
    try (DbSession dbSession = dbClient.openSession(false)) {
      Set<GithubPermissionsMappingDto> existingPermissions = githubPermissionsMappingDao.findAllForGithubRole(dbSession, githubRole);
      if (existingPermissions.isEmpty()) {
        throw new NotFoundException("Role '" + githubRole + "' not found.");
      }
      githubPermissionsMappingDao.deleteAllPermissionsForRole(dbSession, githubRole);
      dbSession.commit();
    }
  }

  private void updatePermissionsMappings(DbSession dbSession, String githubRole, List<PermissionMappingChange> permissionChanges) {
    Set<String> currentPermissionsForRole = getSqPermissionsForGithubRole(dbSession, githubRole);
    removePermissions(dbSession, permissionChanges, currentPermissionsForRole);
    addPermissions(dbSession, permissionChanges, currentPermissionsForRole);
  }

  private Set<String> getSqPermissionsForGithubRole(DbSession dbSession, String githubRole) {
    return getPermissionsMappingForGithubRole(dbSession, githubRole).stream()
      .map(GithubPermissionsMappingDto::sonarqubePermission)
      .collect(toSet());
  }

  private Set<GithubPermissionsMappingDto> getPermissionsMappingForGithubRole(DbSession dbSession, String githubRole) {
    return githubPermissionsMappingDao.findAllForGithubRole(dbSession, githubRole);
  }

  private void removePermissions(DbSession dbSession, List<PermissionMappingChange> permissionChanges, Set<String> currentPermissionsForRole) {
    permissionChanges.stream()
      .filter(permissionMappingChange -> REMOVE.equals(permissionMappingChange.operation()))
      .filter(permissionMappingChange -> currentPermissionsForRole.contains(permissionMappingChange.sonarqubePermission()))
      .forEach(mapping -> githubPermissionsMappingDao.delete(dbSession, mapping.githubRole(), mapping.sonarqubePermission()));
  }

  private void addPermissions(DbSession dbSession, List<PermissionMappingChange> permissionChanges, Set<String> currentPermissionsForRole) {
    permissionChanges.stream()
      .filter(permissionMappingChange -> ADD.equals(permissionMappingChange.operation()))
      .filter(permissionMappingChange -> !currentPermissionsForRole.contains(permissionMappingChange.sonarqubePermission()))
      .forEach(
        mapping -> githubPermissionsMappingDao.insert(dbSession, new GithubPermissionsMappingDto(uuidFactory.create(), mapping.githubRole(), mapping.sonarqubePermission())));
  }

  private static SonarqubePermissions getSonarqubePermissions(Set<GithubPermissionsMappingDto> githubPermissionsMappingDtos) {
    SonarqubePermissions.Builder builder = SonarqubePermissions.Builder.builder();
    githubPermissionsMappingDtos.stream()
      .map(GithubPermissionsMappingDto::sonarqubePermission)
      .map(permissionAsStringToSonarqubePermission::get)
      .forEach(builderConsumer -> builderConsumer.accept(builder));
    return builder.build();
  }

  public GithubPermissionsMapping createPermissionMapping(GithubPermissionsMapping request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      validateCreatePermissionMappingRequest(request, dbSession);
      toGithubPermissionsMappingDtos(request).forEach(dto -> githubPermissionsMappingDao.insert(dbSession, dto));
      dbSession.commit();
      return getPermissionsMappingForGithubRole(request.githubRole());
    }
  }

  private void validateCreatePermissionMappingRequest(GithubPermissionsMapping request, DbSession dbSession) {
    if (!getPermissionsMappingForGithubRole(dbSession, request.githubRole()).isEmpty()) {
      throw new IllegalArgumentException(format("Role %s already exists, it can't be created again.", request.githubRole()));
    }
    if (conflictBaseRole(request.githubRole())) {
      throw new IllegalArgumentException(format("Role %s can conflicts with a GitHub base role, please chose another name.", request.githubRole()));
    }
    if (noPermissionsGranted(request.permissions())) {
      throw new IllegalArgumentException(format("Role %s has no permission set, please set at least one permission.", request.githubRole()));
    }
  }

  private static boolean conflictBaseRole(String githubRole) {
    return GITHUB_BASE_ROLES.stream()
      .anyMatch(baseRole -> githubRole.toLowerCase(Locale.ROOT).equals(baseRole));
  }

  private static boolean noPermissionsGranted(SonarqubePermissions permissions) {
    return !permissions.user() &&
      !permissions.codeViewer() &&
      !permissions.issueAdmin() &&
      !permissions.securityHotspotAdmin() &&
      !permissions.admin() &&
      !permissions.scan();
  }

  private Set<GithubPermissionsMappingDto> toGithubPermissionsMappingDtos(GithubPermissionsMapping request) {
    SonarqubePermissions permissions = request.permissions();
    Set<GithubPermissionsMappingDto> githubPermissionsMappingDtos = new HashSet<>();

    toGithubPermissionsMappingDto(permissions.user(), UserRole.USER, request.githubRole()).ifPresent(githubPermissionsMappingDtos::add);
    toGithubPermissionsMappingDto(permissions.codeViewer(), UserRole.CODEVIEWER, request.githubRole()).ifPresent(githubPermissionsMappingDtos::add);
    toGithubPermissionsMappingDto(permissions.issueAdmin(), UserRole.ISSUE_ADMIN, request.githubRole()).ifPresent(githubPermissionsMappingDtos::add);
    toGithubPermissionsMappingDto(permissions.securityHotspotAdmin(), UserRole.SECURITYHOTSPOT_ADMIN, request.githubRole()).ifPresent(githubPermissionsMappingDtos::add);
    toGithubPermissionsMappingDto(permissions.admin(), UserRole.ADMIN, request.githubRole()).ifPresent(githubPermissionsMappingDtos::add);
    toGithubPermissionsMappingDto(permissions.scan(), UserRole.SCAN, request.githubRole()).ifPresent(githubPermissionsMappingDtos::add);

    return githubPermissionsMappingDtos;
  }

  private Optional<GithubPermissionsMappingDto> toGithubPermissionsMappingDto(boolean granted, String permission, String githubRole) {
    if (granted) {
      return Optional.of(new GithubPermissionsMappingDto(uuidFactory.create(), githubRole, permission));
    }
    return Optional.empty();
  }

}
