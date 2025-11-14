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
package org.sonarqube.ws.client.gitlab.provisioning.permissions;

import com.google.gson.Gson;
import java.util.Locale;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.PatchRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.github.provisioning.permissions.SonarqubePermissions;

public class GitlabPermissionService extends BaseService {

  public GitlabPermissionService(WsConnector wsConnector) {
    super(wsConnector, "api/v2");
  }

  public void addPermissionMapping(AddOrModifyGitlabPermissionMappingRequest addLabPermissionMappingRequest) {
    callEndpointToAddPermissionMapping(addLabPermissionMappingRequest).close();
  }

  private WsResponse callEndpointToAddPermissionMapping(AddOrModifyGitlabPermissionMappingRequest addOrModifyGitlabPermissionMappingRequest) {
    return call(
      new PostRequest(path("dop-translation/gitlab-permission-mappings"))
        .setBody(new Gson().toJson(addOrModifyGitlabPermissionMappingRequest))
        .setMediaType(MediaTypes.JSON));
  }

  public void modifyPermissionMapping(String role, SonarqubePermissions permissions) {
    call(
      new PatchRequest(path("dop-translation/gitlab-permission-mappings/" + role.toLowerCase(Locale.getDefault())))
        .setBody(new Gson().toJson(new AddOrModifyGitlabPermissionMappingRequest(role, permissions)))
        .setContentType("application/merge-patch+json")
        .setMediaType(MediaTypes.JSON)).close();
  }
}
