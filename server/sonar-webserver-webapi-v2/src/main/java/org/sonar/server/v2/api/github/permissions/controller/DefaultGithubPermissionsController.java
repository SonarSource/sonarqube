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
package org.sonar.server.v2.api.github.permissions.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import org.sonar.server.common.github.permissions.GithubPermissionsMapping;
import org.sonar.server.common.github.permissions.GithubPermissionsMappingService;
import org.sonar.server.common.github.permissions.PermissionMappingChange;
import org.sonar.server.common.permission.Operation;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.github.permissions.model.RestGithubPermissionsMapping;
import org.sonar.server.v2.api.github.permissions.request.GithubPermissionMappingUpdateRequest;
import org.sonar.server.v2.api.github.permissions.request.PermissionMappingUpdate;
import org.sonar.server.v2.api.github.permissions.response.GithubPermissionsMappingRestResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.api.web.UserRole.SCAN;
import static org.sonar.api.web.UserRole.SECURITYHOTSPOT_ADMIN;
import static org.sonar.api.web.UserRole.USER;

public class DefaultGithubPermissionsController implements GithubPermissionsController {

  private final UserSession userSession;
  private final GithubPermissionsMappingService githubPermissionsMappingService;

  public DefaultGithubPermissionsController(UserSession userSession, GithubPermissionsMappingService githubPermissionsMappingService) {
    this.userSession = userSession;
    this.githubPermissionsMappingService = githubPermissionsMappingService;
  }

  @Override
  public GithubPermissionsMappingRestResponse fetchAll() {
    userSession.checkIsSystemAdministrator();
    List<GithubPermissionsMapping> permissionsMapping = githubPermissionsMappingService.getPermissionsMapping();
    return new GithubPermissionsMappingRestResponse(toRestResources(permissionsMapping));
  }

  @Override
  public RestGithubPermissionsMapping updateMapping(@PathVariable("githubRole") String githubRole, @Valid @RequestBody GithubPermissionMappingUpdateRequest request) {
    userSession.checkIsSystemAdministrator();
    PermissionMappingUpdate update = request.permissions();
    Set<PermissionMappingChange> changes = new HashSet<>();

    update.getUser().map(shouldAddPermission -> toPermissionMappingChange(githubRole, USER, shouldAddPermission)).applyIfDefined(changes::add);
    update.getCodeViewer().map(shouldAddPermission -> toPermissionMappingChange(githubRole, CODEVIEWER, shouldAddPermission)).applyIfDefined(changes::add);
    update.getIssueAdmin().map(shouldAddPermission -> toPermissionMappingChange(githubRole, ISSUE_ADMIN, shouldAddPermission)).applyIfDefined(changes::add);
    update.getSecurityHotspotAdmin().map(shouldAddPermission -> toPermissionMappingChange(githubRole, SECURITYHOTSPOT_ADMIN, shouldAddPermission)).applyIfDefined(changes::add);
    update.getAdmin().map(shouldAddPermission -> toPermissionMappingChange(githubRole, ADMIN, shouldAddPermission)).applyIfDefined(changes::add);
    update.getScan().map(shouldAddPermission -> toPermissionMappingChange(githubRole, SCAN, shouldAddPermission)).applyIfDefined(changes::add);

    githubPermissionsMappingService.updatePermissionsMappings(changes);

    return toRestGithubPermissionMapping(githubPermissionsMappingService.getPermissionsMappingForGithubRole(githubRole));

  }

  private static PermissionMappingChange toPermissionMappingChange(String githubRole, String sonarqubePermission, boolean shouldAddPermission) {
    return new PermissionMappingChange(githubRole, sonarqubePermission, shouldAddPermission ? Operation.ADD : Operation.REMOVE);
  }

  private static List<RestGithubPermissionsMapping> toRestResources(List<GithubPermissionsMapping> permissionsMapping) {
    return permissionsMapping.stream()
      .map(DefaultGithubPermissionsController::toRestGithubPermissionMapping)
      .toList();
  }

  private static RestGithubPermissionsMapping toRestGithubPermissionMapping(GithubPermissionsMapping githubPermissionsMapping) {
    return new RestGithubPermissionsMapping(
      githubPermissionsMapping.roleName(),
      githubPermissionsMapping.roleName(),
      githubPermissionsMapping.isBaseRole(),
      githubPermissionsMapping.permissions()
    );
  }

}
