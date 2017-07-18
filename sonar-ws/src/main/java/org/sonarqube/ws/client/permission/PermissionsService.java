/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.permission;

import org.sonarqube.ws.WsPermissions;
import org.sonarqube.ws.WsPermissions.CreateTemplateWsResponse;
import org.sonarqube.ws.WsPermissions.SearchProjectPermissionsWsResponse;
import org.sonarqube.ws.WsPermissions.SearchTemplatesWsResponse;
import org.sonarqube.ws.WsPermissions.UpdateTemplateWsResponse;
import org.sonarqube.ws.WsPermissions.UsersWsResponse;
import org.sonarqube.ws.WsPermissions.WsSearchGlobalPermissionsResponse;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;

import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_DESCRIPTION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY_PATTERN;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_QUALIFIER;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class PermissionsService extends BaseService {

  public PermissionsService(WsConnector wsConnector) {
    super(wsConnector, PermissionsWsParameters.CONTROLLER);
  }

  public WsPermissions.WsGroupsResponse groups(GroupsWsRequest request) {
    GetRequest get = new GetRequest(path("groups"))
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam("p", request.getPage())
      .setParam("ps", request.getPageSize())
      .setParam("q", request.getQuery());
    return call(get, WsPermissions.WsGroupsResponse.parser());
  }

  public void addGroup(AddGroupWsRequest request) {
    call(new PostRequest(path("add_group"))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam(PARAM_GROUP_ID, request.getGroupId())
      .setParam(PARAM_GROUP_NAME, request.getGroupName()));
  }

  public void addGroupToTemplate(AddGroupToTemplateWsRequest request) {
    call(new PostRequest(path("add_group_to_template"))
      .setParam(PARAM_GROUP_ID, request.getGroupId())
      .setParam(PARAM_GROUP_NAME, request.getGroupName())
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public void addUser(AddUserWsRequest request) {
    call(new PostRequest(path("add_user"))
      .setParam(PARAM_USER_LOGIN, request.getLogin())
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam(PARAM_ORGANIZATION, request.getOrganization()));
  }

  public void addUserToTemplate(AddUserToTemplateWsRequest request) {
    call(new PostRequest(path("add_user_to_template"))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_USER_LOGIN, request.getLogin())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public void addProjectCreatorToTemplate(AddProjectCreatorToTemplateWsRequest request) {
    call(new PostRequest(path("add_project_creator_to_template"))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public void applyTemplate(ApplyTemplateWsRequest request) {
    call(new PostRequest(path("apply_template"))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public void bulkApplyTemplate(BulkApplyTemplateWsRequest request) {
    call(new PostRequest(path("bulk_apply_template"))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName())
      .setParam("q", request.getQuery())
      .setParam(PARAM_QUALIFIER, request.getQualifier()));
  }

  public CreateTemplateWsResponse createTemplate(CreateTemplateWsRequest request) {
    PostRequest post = new PostRequest(path("create_template"))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_NAME, request.getName())
      .setParam(PARAM_DESCRIPTION, request.getDescription())
      .setParam(PARAM_PROJECT_KEY_PATTERN, request.getProjectKeyPattern());
    return call(post, CreateTemplateWsResponse.parser());
  }

  public void deleteTemplate(DeleteTemplateWsRequest request) {
    call(new PostRequest(path("delete_template"))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public void removeGroup(RemoveGroupWsRequest request) {
    call(new PostRequest(path("remove_group"))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_GROUP_ID, request.getGroupId())
      .setParam(PARAM_GROUP_NAME, request.getGroupName())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey()));
  }

  public void removeGroupFromTemplate(RemoveGroupFromTemplateWsRequest request) {
    call(new PostRequest(path("remove_group_from_template"))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_GROUP_ID, request.getGroupId())
      .setParam(PARAM_GROUP_NAME, request.getGroupName())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public void removeProjectCreatorFromTemplate(RemoveProjectCreatorFromTemplateWsRequest request) {
    call(
      new PostRequest(path("remove_project_creator_from_template"))
        .setParam(PARAM_ORGANIZATION, request.getOrganization())
        .setParam(PARAM_PERMISSION, request.getPermission())
        .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
        .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public void removeUser(RemoveUserWsRequest request) {
    call(new PostRequest(path("remove_user"))
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_USER_LOGIN, request.getLogin())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey()));
  }

  public void removeUserFromTemplate(RemoveUserFromTemplateWsRequest request) {
    call(new PostRequest(path("remove_user_from_template"))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_USER_LOGIN, request.getLogin())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public WsSearchGlobalPermissionsResponse searchGlobalPermissions() {
    GetRequest get = new GetRequest(path("search_global_permissions"));
    return call(get, WsSearchGlobalPermissionsResponse.parser());
  }

  public SearchProjectPermissionsWsResponse searchProjectPermissions(SearchProjectPermissionsWsRequest request) {
    GetRequest get = new GetRequest(path("search_project_permissions"))
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam(PARAM_QUALIFIER, request.getQualifier())
      .setParam("p", request.getPage())
      .setParam("ps", request.getPageSize())
      .setParam("q", request.getQuery());
    return call(get, SearchProjectPermissionsWsResponse.parser());
  }

  public SearchTemplatesWsResponse searchTemplates(SearchTemplatesWsRequest request) {
    GetRequest get = new GetRequest(path("search_templates"))
      .setParam("q", request.getQuery());
    return call(get, SearchTemplatesWsResponse.parser());
  }

  public void setDefaultTemplate(SetDefaultTemplateWsRequest request) {
    call(new PostRequest(path("set_default_template"))
      .setParam(PARAM_QUALIFIER, request.getQualifier())
      .setParam(PARAM_TEMPLATE_ID, request.getTemplateId())
      .setParam(PARAM_TEMPLATE_NAME, request.getTemplateName()));
  }

  public UpdateTemplateWsResponse updateTemplate(UpdateTemplateWsRequest request) {
    return call(new PostRequest(path("update_template"))
      .setParam(PARAM_DESCRIPTION, request.getDescription())
      .setParam(PARAM_ID, request.getId())
      .setParam(PARAM_NAME, request.getName())
      .setParam(PARAM_PROJECT_KEY_PATTERN, request.getProjectKeyPattern()), UpdateTemplateWsResponse.parser());
  }

  public UsersWsResponse users(UsersWsRequest request) {
    return call(new GetRequest(path("users"))
      .setParam(PARAM_ORGANIZATION, request.getOrganization())
      .setParam(PARAM_PERMISSION, request.getPermission())
      .setParam(PARAM_PROJECT_ID, request.getProjectId())
      .setParam(PARAM_PROJECT_KEY, request.getProjectKey())
      .setParam("p", request.getPage())
      .setParam("ps", request.getPageSize())
      .setParam("q", request.getQuery()), UsersWsResponse.parser());
  }
}
