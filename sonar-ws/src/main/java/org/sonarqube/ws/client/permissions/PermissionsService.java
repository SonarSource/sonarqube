/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonarqube.ws.client.permissions;

import java.util.stream.Collectors;
import javax.annotation.Generated;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.BaseService;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsConnector;
import org.sonarqube.ws.Permissions.CreateTemplateWsResponse;
import org.sonarqube.ws.Permissions.WsGroupsResponse;
import org.sonarqube.ws.Permissions.WsSearchGlobalPermissionsResponse;
import org.sonarqube.ws.Permissions.SearchProjectPermissionsWsResponse;
import org.sonarqube.ws.Permissions.SearchTemplatesWsResponse;
import org.sonarqube.ws.Permissions.WsTemplateGroupsResponse;
import org.sonarqube.ws.Permissions.UpdateTemplateWsResponse;
import org.sonarqube.ws.Permissions.UsersWsResponse;

/**
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions">Further information about this web service online</a>
 */
@Generated("sonar-ws-generator")
public class PermissionsService extends BaseService {

  public PermissionsService(WsConnector wsConnector) {
    super(wsConnector, "api/permissions");
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/add_group">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void addGroup(AddGroupRequest request) {
    call(
      new PostRequest(path("add_group"))
        .setParam("groupId", request.getGroupId())
        .setParam("groupName", request.getGroupName())
        .setParam("organization", request.getOrganization())
        .setParam("permission", request.getPermission())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/add_group_to_template">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void addGroupToTemplate(AddGroupToTemplateRequest request) {
    call(
      new PostRequest(path("add_group_to_template"))
        .setParam("groupId", request.getGroupId())
        .setParam("groupName", request.getGroupName())
        .setParam("organization", request.getOrganization())
        .setParam("permission", request.getPermission())
        .setParam("templateId", request.getTemplateId())
        .setParam("templateName", request.getTemplateName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/add_project_creator_to_template">Further information about this action online (including a response example)</a>
   * @since 6.0
   */
  public void addProjectCreatorToTemplate(AddProjectCreatorToTemplateRequest request) {
    call(
      new PostRequest(path("add_project_creator_to_template"))
        .setParam("organization", request.getOrganization())
        .setParam("permission", request.getPermission())
        .setParam("templateId", request.getTemplateId())
        .setParam("templateName", request.getTemplateName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/add_user">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void addUser(AddUserRequest request) {
    call(
      new PostRequest(path("add_user"))
        .setParam("login", request.getLogin())
        .setParam("organization", request.getOrganization())
        .setParam("permission", request.getPermission())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/add_user_to_template">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void addUserToTemplate(AddUserToTemplateRequest request) {
    call(
      new PostRequest(path("add_user_to_template"))
        .setParam("login", request.getLogin())
        .setParam("organization", request.getOrganization())
        .setParam("permission", request.getPermission())
        .setParam("templateId", request.getTemplateId())
        .setParam("templateName", request.getTemplateName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/apply_template">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void applyTemplate(ApplyTemplateRequest request) {
    call(
      new PostRequest(path("apply_template"))
        .setParam("organization", request.getOrganization())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey())
        .setParam("templateId", request.getTemplateId())
        .setParam("templateName", request.getTemplateName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/bulk_apply_template">Further information about this action online (including a response example)</a>
   * @since 5.5
   */
  public void bulkApplyTemplate(BulkApplyTemplateRequest request) {
    call(
      new PostRequest(path("bulk_apply_template"))
        .setParam("analyzedBefore", request.getAnalyzedBefore())
        .setParam("onProvisionedOnly", request.getOnProvisionedOnly())
        .setParam("organization", request.getOrganization())
        .setParam("projects", request.getProjects() == null ? null : request.getProjects().stream().collect(Collectors.joining(",")))
        .setParam("q", request.getQ())
        .setParam("qualifiers", request.getQualifiers() == null ? null : request.getQualifiers().stream().collect(Collectors.joining(",")))
        .setParam("templateId", request.getTemplateId())
        .setParam("templateName", request.getTemplateName())
        .setParam("visibility", request.getVisibility())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/create_template">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public CreateTemplateWsResponse createTemplate(CreateTemplateRequest request) {
    return call(
      new PostRequest(path("create_template"))
        .setParam("description", request.getDescription())
        .setParam("name", request.getName())
        .setParam("organization", request.getOrganization())
        .setParam("projectKeyPattern", request.getProjectKeyPattern()),
      CreateTemplateWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/delete_template">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void deleteTemplate(DeleteTemplateRequest request) {
    call(
      new PostRequest(path("delete_template"))
        .setParam("organization", request.getOrganization())
        .setParam("templateId", request.getTemplateId())
        .setParam("templateName", request.getTemplateName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/groups">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public WsGroupsResponse groups(GroupsRequest request) {
    return call(
      new GetRequest(path("groups"))
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("permission", request.getPermission())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ()),
      WsGroupsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/remove_group">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void removeGroup(RemoveGroupRequest request) {
    call(
      new PostRequest(path("remove_group"))
        .setParam("groupId", request.getGroupId())
        .setParam("groupName", request.getGroupName())
        .setParam("organization", request.getOrganization())
        .setParam("permission", request.getPermission())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/remove_group_from_template">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void removeGroupFromTemplate(RemoveGroupFromTemplateRequest request) {
    call(
      new PostRequest(path("remove_group_from_template"))
        .setParam("groupId", request.getGroupId())
        .setParam("groupName", request.getGroupName())
        .setParam("organization", request.getOrganization())
        .setParam("permission", request.getPermission())
        .setParam("templateId", request.getTemplateId())
        .setParam("templateName", request.getTemplateName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/remove_project_creator_from_template">Further information about this action online (including a response example)</a>
   * @since 6.0
   */
  public void removeProjectCreatorFromTemplate(RemoveProjectCreatorFromTemplateRequest request) {
    call(
      new PostRequest(path("remove_project_creator_from_template"))
        .setParam("organization", request.getOrganization())
        .setParam("permission", request.getPermission())
        .setParam("templateId", request.getTemplateId())
        .setParam("templateName", request.getTemplateName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/remove_user">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void removeUser(RemoveUserRequest request) {
    call(
      new PostRequest(path("remove_user"))
        .setParam("login", request.getLogin())
        .setParam("organization", request.getOrganization())
        .setParam("permission", request.getPermission())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/remove_user_from_template">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void removeUserFromTemplate(RemoveUserFromTemplateRequest request) {
    call(
      new PostRequest(path("remove_user_from_template"))
        .setParam("login", request.getLogin())
        .setParam("organization", request.getOrganization())
        .setParam("permission", request.getPermission())
        .setParam("templateId", request.getTemplateId())
        .setParam("templateName", request.getTemplateName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/search_global_permissions">Further information about this action online (including a response example)</a>
   * @since 5.2
   * @deprecated since 6.5
   */
  @Deprecated
  public WsSearchGlobalPermissionsResponse searchGlobalPermissions(SearchGlobalPermissionsRequest request) {
    return call(
      new GetRequest(path("search_global_permissions"))
        .setParam("organization", request.getOrganization()),
      WsSearchGlobalPermissionsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/search_project_permissions">Further information about this action online (including a response example)</a>
   * @since 5.2
   * @deprecated since 6.5
   */
  @Deprecated
  public SearchProjectPermissionsWsResponse searchProjectPermissions(SearchProjectPermissionsRequest request) {
    return call(
      new GetRequest(path("search_project_permissions"))
        .setParam("p", request.getP())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("qualifier", request.getQualifier()),
      SearchProjectPermissionsWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/search_templates">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public SearchTemplatesWsResponse searchTemplates(SearchTemplatesRequest request) {
    return call(
      new GetRequest(path("search_templates"))
        .setParam("organization", request.getOrganization())
        .setParam("q", request.getQ()),
      SearchTemplatesWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/set_default_template">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public void setDefaultTemplate(SetDefaultTemplateRequest request) {
    call(
      new PostRequest(path("set_default_template"))
        .setParam("organization", request.getOrganization())
        .setParam("qualifier", request.getQualifier())
        .setParam("templateId", request.getTemplateId())
        .setParam("templateName", request.getTemplateName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/template_groups">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public WsTemplateGroupsResponse templateGroups(TemplateGroupsRequest request) {
    return call(
      new GetRequest(path("template_groups"))
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("permission", request.getPermission())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("templateId", request.getTemplateId())
        .setParam("templateName", request.getTemplateName()),
      WsTemplateGroupsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/template_users">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public String templateUsers(TemplateUsersRequest request) {
    return call(
      new GetRequest(path("template_users"))
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("permission", request.getPermission())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ())
        .setParam("templateId", request.getTemplateId())
        .setParam("templateName", request.getTemplateName())
        .setMediaType(MediaTypes.JSON)
      ).content();
  }

  /**
   *
   * This is part of the internal API.
   * This is a POST request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/update_template">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public UpdateTemplateWsResponse updateTemplate(UpdateTemplateRequest request) {
    return call(
      new PostRequest(path("update_template"))
        .setParam("description", request.getDescription())
        .setParam("id", request.getId())
        .setParam("name", request.getName())
        .setParam("projectKeyPattern", request.getProjectKeyPattern()),
      UpdateTemplateWsResponse.parser());
  }

  /**
   *
   * This is part of the internal API.
   * This is a GET request.
   * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/permissions/users">Further information about this action online (including a response example)</a>
   * @since 5.2
   */
  public UsersWsResponse users(UsersRequest request) {
    return call(
      new GetRequest(path("users"))
        .setParam("organization", request.getOrganization())
        .setParam("p", request.getP())
        .setParam("permission", request.getPermission())
        .setParam("projectId", request.getProjectId())
        .setParam("projectKey", request.getProjectKey())
        .setParam("ps", request.getPs())
        .setParam("q", request.getQ()),
      UsersWsResponse.parser());
  }
}
