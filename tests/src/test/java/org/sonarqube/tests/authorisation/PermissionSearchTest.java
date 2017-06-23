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
package org.sonarqube.tests.authorisation;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category1Suite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.WsPermissions;
import org.sonarqube.ws.WsPermissions.Permission;
import org.sonarqube.ws.WsPermissions.SearchTemplatesWsResponse;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.permission.AddGroupToTemplateWsRequest;
import org.sonarqube.ws.client.permission.AddGroupWsRequest;
import org.sonarqube.ws.client.permission.AddProjectCreatorToTemplateWsRequest;
import org.sonarqube.ws.client.permission.AddUserToTemplateWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.CreateTemplateWsRequest;
import org.sonarqube.ws.client.permission.GroupsWsRequest;
import org.sonarqube.ws.client.permission.PermissionsService;
import org.sonarqube.ws.client.permission.RemoveGroupFromTemplateWsRequest;
import org.sonarqube.ws.client.permission.RemoveProjectCreatorFromTemplateWsRequest;
import org.sonarqube.ws.client.permission.RemoveUserFromTemplateWsRequest;
import org.sonarqube.ws.client.permission.SearchTemplatesWsRequest;
import org.sonarqube.ws.client.permission.UsersWsRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class PermissionSearchTest {
  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;
  private static WsClient adminWsClient;
  private static PermissionsService permissionsWsClient;

  private static final String PROJECT_KEY = "sample";
  private static final String LOGIN = "george.orwell";
  private static final String GROUP_NAME = "1984";

  @BeforeClass
  public static void analyzeProject() {
    orchestrator.resetData();

    ItUtils.restoreProfile(orchestrator, PermissionSearchTest.class.getResource("/authorisation/one-issue-per-line-profile.xml"));

    orchestrator.getServer().provisionProject(PROJECT_KEY, "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample"));
    orchestrator.executeBuild(sampleProject);

    adminWsClient = newAdminWsClient(orchestrator);
    permissionsWsClient = adminWsClient.permissions();

    createUser(LOGIN, "George Orwell");
    createGroup(GROUP_NAME);
  }

  @AfterClass
  public static void delete_data() {
    deactivateUser(LOGIN);
    deleteGroup(GROUP_NAME);
  }

  @Test
  public void permission_web_services() {
    permissionsWsClient.addUser(
      new AddUserWsRequest()
        .setPermission("admin")
        .setLogin(LOGIN));
    permissionsWsClient.addGroup(
      new AddGroupWsRequest()
        .setPermission("admin")
        .setGroupName(GROUP_NAME));

    WsPermissions.WsSearchGlobalPermissionsResponse searchGlobalPermissionsWsResponse = permissionsWsClient.searchGlobalPermissions();
    assertThat(searchGlobalPermissionsWsResponse.getPermissionsList().get(0).getKey()).isEqualTo("admin");
    assertThat(searchGlobalPermissionsWsResponse.getPermissionsList().get(0).getUsersCount()).isEqualTo(1);
    // by default, a group has the global admin permission
    assertThat(searchGlobalPermissionsWsResponse.getPermissionsList().get(0).getGroupsCount()).isEqualTo(2);

    WsPermissions.UsersWsResponse users = permissionsWsClient
      .users(new UsersWsRequest().setPermission("admin"));
    assertThat(users.getUsersList()).extracting("login").contains(LOGIN);

    WsPermissions.WsGroupsResponse groupsResponse = permissionsWsClient
      .groups(new GroupsWsRequest()
        .setPermission("admin"));
    assertThat(groupsResponse.getGroupsList()).extracting("name").contains(GROUP_NAME);
  }

  @Test
  public void template_permission_web_services() {
    WsPermissions.CreateTemplateWsResponse createTemplateWsResponse = permissionsWsClient.createTemplate(
      new CreateTemplateWsRequest()
        .setName("my-new-template")
        .setDescription("template-used-in-tests"));
    assertThat(createTemplateWsResponse.getPermissionTemplate().getName()).isEqualTo("my-new-template");

    permissionsWsClient.addUserToTemplate(
      new AddUserToTemplateWsRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .setLogin(LOGIN));

    permissionsWsClient.addGroupToTemplate(
      new AddGroupToTemplateWsRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .setGroupName(GROUP_NAME));

    permissionsWsClient.addProjectCreatorToTemplate(
      AddProjectCreatorToTemplateWsRequest.builder()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .build());

    SearchTemplatesWsResponse searchTemplatesWsResponse = permissionsWsClient.searchTemplates(
      new SearchTemplatesWsRequest()
        .setQuery("my-new-template"));
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getName()).isEqualTo("my-new-template");
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getPermissions(0).getKey()).isEqualTo("admin");
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getPermissions(0).getUsersCount()).isEqualTo(1);
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getPermissions(0).getGroupsCount()).isEqualTo(1);
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getPermissions(0).getWithProjectCreator()).isTrue();

    permissionsWsClient.removeGroupFromTemplate(
      new RemoveGroupFromTemplateWsRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .setGroupName(GROUP_NAME));

    permissionsWsClient.removeUserFromTemplate(
      new RemoveUserFromTemplateWsRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .setLogin(LOGIN));

    permissionsWsClient.removeProjectCreatorFromTemplate(
      RemoveProjectCreatorFromTemplateWsRequest.builder()
        .setPermission("admin")
        .setTemplateName("my-new-template")
      .build()
    );

    SearchTemplatesWsResponse clearedSearchTemplatesWsResponse = permissionsWsClient.searchTemplates(
      new SearchTemplatesWsRequest()
        .setQuery("my-new-template"));
    assertThat(clearedSearchTemplatesWsResponse.getPermissionTemplates(0).getPermissionsList())
      .extracting(Permission::getUsersCount, Permission::getGroupsCount, Permission::getWithProjectCreator)
      .hasSize(5)
      .containsOnly(tuple(0, 0, false));
  }

  private static void createUser(String login, String name) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/users/create")
        .setParam("login", login)
        .setParam("name", name)
        .setParam("password", "123456"));
  }

  private static void deactivateUser(String login) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/users/deactivate")
        .setParam("login", login));
  }

  private static void createGroup(String groupName) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/user_groups/create")
        .setParam("name", groupName));
  }

  private static void deleteGroup(String groupName) {
    adminWsClient.wsConnector().call(
      new PostRequest("api/user_groups/delete")
        .setParam("name", groupName));
  }
}
