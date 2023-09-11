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

import java.util.List;
import org.sonar.server.common.github.permissions.GithubPermissionsMapping;
import org.sonar.server.common.github.permissions.GithubPermissionsMappingService;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.github.permissions.model.RestGithubPermissionsMapping;
import org.sonar.server.v2.api.github.permissions.response.GithubPermissionsMappingRestResponse;

public class DefaultGithubPermissionsController implements GithubPermissionsController{

  private UserSession userSession;
  private GithubPermissionsMappingService githubPermissionsMappingService;

  public DefaultGithubPermissionsController(UserSession userSession, GithubPermissionsMappingService githubPermissionsMappingService) {
    this.userSession = userSession;
    this.githubPermissionsMappingService = githubPermissionsMappingService;
  }

  @Override
  public GithubPermissionsMappingRestResponse fetchAll() {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    List<GithubPermissionsMapping> permissionsMapping = githubPermissionsMappingService.getPermissionsMapping();
    return new GithubPermissionsMappingRestResponse(toRestResources(permissionsMapping));
  }

  private static List<RestGithubPermissionsMapping> toRestResources(List<GithubPermissionsMapping> permissionsMapping) {
    return permissionsMapping.stream()
      .map(e -> new RestGithubPermissionsMapping(e.roleName(), e.roleName(), e.permissions()))
      .toList();
  }

}
