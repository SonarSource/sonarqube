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
import org.sonarqube.ws.WsPermissions.CreateTemplateWsResponse;
import org.sonarqube.ws.client.WsClient;

import static org.sonarqube.ws.client.WsRequest.newGetRequest;
import static org.sonarqube.ws.client.WsRequest.newPostRequest;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_DESCRIPTION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY_PATTERN;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class PermissionsWsClient {
  private final WsClient wsClient;

  public PermissionsWsClient(WsClient wsClient) {
    this.wsClient = wsClient;
  }

  public WsPermissions.WsGroupsResponse groups(GroupsWsRequest request) {
    return wsClient.execute(newGetRequest(action("groups"))
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam("p", request.getPage())
      .setParam("ps", request.getPageSize())
      .setParam("selected", request.getSelected())
      .setParam("q", request.getQuery()),
      WsPermissions.WsGroupsResponse.parser());
  }

  public void addGroup(AddGroupWsRequest request) {
    wsClient.execute(newPostRequest(action("add_group"))
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam(PARAM_GROUP_ID, request.getGroupId())
      .setParam(PARAM_GROUP_NAME, request.getGroupName()));
  }

  public void addGroupToTemplate(AddGroupToTemplateWsRequest request) {
    wsClient.execute(newPostRequest(action("add_group_to_template"))
      .setParam(PARAM_GROUP_ID, request.getGroupId())
      .setParam(PARAM_GROUP_NAME, request.getGroupName())
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public void addUser(AddUserWsRequest request) {
    wsClient.execute(newPostRequest(action("add_user"))
      .setParam(PARAM_USER_LOGIN, request.getLogin())
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey()));
  }

  public void addUserToTemplate(AddUserToTemplateWsRequest request) {
    wsClient.execute(newPostRequest(action("add_user_to_template"))
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_USER_LOGIN, request.getLogin())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public void applyTemplate(ApplyTemplateWsRequest request) {
    wsClient.execute(newPostRequest(action("apply_template"))
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public CreateTemplateWsResponse createTemplate(CreateTemplateWsRequest request) {
    return wsClient.execute(newPostRequest(
      action("create_template"))
        .setParam(PARAM_NAME, request.getName())
        .setParam(PARAM_DESCRIPTION, request.getDescription())
        .setParam(PARAM_PROJECT_KEY_PATTERN, request.getProjectKeyPattern()),
      CreateTemplateWsResponse.parser());
  }

  public void deleteTemplate(DeleteTemplateWsRequest request) {
    wsClient.execute(newPostRequest(action("delete_template"))
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public void removeGroup(RemoveGroupWsRequest request) {
    wsClient.execute(newPostRequest(action("remove_group"))
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_GROUP_ID, request.getGroupId())
      .setParam(PARAM_GROUP_NAME, request.getGroupName())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey()));
  }

  public void removeGroupFromTemplate(RemoveGroupFromTemplateWsRequest request) {
    wsClient.execute(newPostRequest(action("remove_group_from_template"))
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_GROUP_ID, request.getGroupId())
      .setParam(PARAM_GROUP_NAME, request.getGroupName())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public void removeUser(RemoveUserWsRequest request) {
    wsClient.execute(newPostRequest(action("remove_user"))
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_USER_LOGIN, request.getLogin())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey()));
  }

  public void removeUserFromTemplate(RemoveUserFromTemplateWsRequest request) {
    wsClient.execute(newPostRequest(action("remove_user_from_template"))
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_USER_LOGIN, request.getLogin())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  private static String action(String action) {
    return PermissionsWsParameters.ENDPOINT + "/" + action;
  }
}
