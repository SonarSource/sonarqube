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
import java.util.function.Consumer;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.provisioning.GithubPermissionsMappingDao;
import org.sonar.db.provisioning.GithubPermissionsMappingDto;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

public class GithubPermissionsMappingService {
  public static final String READ_GITHUB_ROLE = "read";
  public static final String TRIAGE_GITHUB_ROLE = "triage";
  public static final String WRITE_GITHUB_ROLE = "write";
  public static final String MAINTAIN_GITHUB_ROLE = "maintain";
  public static final String ADMIN_GITHUB_ROLE = "admin";

  private static final Set<String> GITHUB_BASE_ROLE = Set.of(
    READ_GITHUB_ROLE,
    TRIAGE_GITHUB_ROLE,
    WRITE_GITHUB_ROLE,
    MAINTAIN_GITHUB_ROLE,
    ADMIN_GITHUB_ROLE
  );

  private static final Map<String, Consumer<SonarqubePermissions.Builder>> permissionAsStringToSonarqubePermission = Map.of(
    UserRole.USER, builder -> builder.user(true),
    UserRole.CODEVIEWER, builder -> builder.codeViewer(true),
    UserRole.ISSUE_ADMIN, builder -> builder.issueAdmin(true),
    UserRole.SECURITYHOTSPOT_ADMIN, builder -> builder.securityHotspotAdmin(true),
    UserRole.ADMIN, builder -> builder.admin(true),
    UserRole.SCAN, builder -> builder.scan(true)
  );

  private final DbClient dbClient;
  private final GithubPermissionsMappingDao githubPermissionsMappingDao;

  public GithubPermissionsMappingService(DbClient dbClient, GithubPermissionsMappingDao githubPermissionsMappingDao) {
    this.dbClient = dbClient;
    this.githubPermissionsMappingDao = githubPermissionsMappingDao;
  }

  public List<GithubPermissionsMapping> getPermissionsMapping() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      return toGithubPermissionsMappings(githubPermissionsMappingDao.findAll(dbSession));
    }
  }

  private static List<GithubPermissionsMapping> toGithubPermissionsMappings(Set<GithubPermissionsMappingDto> githubPermissionsMappingDtos) {
    Map<String, Set<GithubPermissionsMappingDto>> githubRoleToGithubPermissionsMappingDto = githubPermissionsMappingDtos.stream()
      .collect(groupingBy(GithubPermissionsMappingDto::githubRole, toSet()));
    return GITHUB_BASE_ROLE.stream()
      .map(githubRole -> toGithubPermissionsMapping(githubRoleToGithubPermissionsMappingDto.get(githubRole), githubRole))
      .toList();
  }

  private static GithubPermissionsMapping toGithubPermissionsMapping(Set<GithubPermissionsMappingDto> githubPermissionsMappingDtos, String githubRole) {
    return new GithubPermissionsMapping(githubRole, getSonarqubePermissions(githubPermissionsMappingDtos));
  }

  private static SonarqubePermissions getSonarqubePermissions(Set<GithubPermissionsMappingDto> githubPermissionsMappingDtos) {
    SonarqubePermissions.Builder builder = SonarqubePermissions.Builder.builder();
    if (githubPermissionsMappingDtos != null) {
      githubPermissionsMappingDtos.stream()
        .map(GithubPermissionsMappingDto::sonarqubePermission)
        .map(permissionAsStringToSonarqubePermission::get)
        .forEach(builderConsumer -> builderConsumer.accept(builder));
    }
    return builder.build();
  }

}
