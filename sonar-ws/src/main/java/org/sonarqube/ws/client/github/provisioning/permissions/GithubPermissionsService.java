/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarqube.ws.client.github.provisioning.permissions;

import com.google.gson.Gson;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.client.WsResponse;

public class GithubPermissionsService extends BaseService {

  public GithubPermissionsService(WsConnector wsConnector) {
    super(wsConnector, "api/v2");
  }

  public void addPermissionMapping(AddGithubPermissionMappingRequest addGithubPermissionMappingRequest) {
    callEndpoint(addGithubPermissionMappingRequest).close();
  }

  private WsResponse callEndpoint(AddGithubPermissionMappingRequest addGithubPermissionMappingRequest) {
    return call(
      new PostRequest(path("dop-translation/github-permission-mappings"))
        .setBody(new Gson().toJson(addGithubPermissionMappingRequest))
        .setMediaType(MediaTypes.JSON));
  }
}
