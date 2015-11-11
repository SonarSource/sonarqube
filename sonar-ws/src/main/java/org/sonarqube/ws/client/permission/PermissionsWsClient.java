/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonarqube.ws.client.permission;

import org.sonarqube.ws.WsPermissions;
import org.sonarqube.ws.client.WsClient;

import static org.sonarqube.ws.client.WsRequest.newGetRequest;
import static org.sonarqube.ws.client.WsRequest.newPostRequest;

public class PermissionsWsClient {
  private final WsClient wsClient;

  public PermissionsWsClient(WsClient wsClient) {
    this.wsClient = wsClient;
  }

  public WsPermissions.WsGroupsResponse groups(GroupsWsRequest request) {
    return wsClient.execute(newGetRequest("api/permissions/groups")
      .setParam("permission", request.getPermission())
      .setParam("projectId", request.getProjectId())
      .setParam("projectKey", request.getProjectKey())
      .setParam("p", request.getPage())
      .setParam("ps", request.getPageSize())
      .setParam("selected", request.getSelected())
      .setParam("q", request.getQuery()),
      WsPermissions.WsGroupsResponse.parser());
  }

  public void addGroup(AddGroupWsRequest request) {
    wsClient.execute(newPostRequest("api/permissions/add_group")
      .setParam("permission", request.getPermission())
      .setParam("projectId", request.getProjectId())
      .setParam("projectKey", request.getProjectKey())
      .setParam("groupId", request.getGroupId())
      .setParam("groupName", request.getGroupName()));
  }
}
