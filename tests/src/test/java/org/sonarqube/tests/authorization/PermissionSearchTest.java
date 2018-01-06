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
package org.sonarqube.tests.authorization;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Permissions;
import org.sonarqube.ws.Permissions.Permission;
import org.sonarqube.ws.Permissions.SearchTemplatesWsResponse;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.permissions.*;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static util.ItUtils.projectDir;

public class PermissionSearchTest {

  private static final String PROJECT_KEY = "sample";
  private static final String LOGIN = "george.orwell";
  private static final String GROUP_NAME = "1984";
  
  @ClassRule
  public static Orchestrator orchestrator = AuthorizationSuite.ORCHESTRATOR;

  private static Tester tester = new Tester(orchestrator)
    // all the tests of AuthorizationSuite must disable organizations
    .disableOrganizations();

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator).around(tester);

  @BeforeClass
  public static void analyzeProject() {
    ItUtils.restoreProfile(orchestrator, PermissionSearchTest.class.getResource("/authorisation/one-issue-per-line-profile.xml"));

    orchestrator.getServer().provisionProject(PROJECT_KEY, "Sample");
    orchestrator.getServer().associateProjectToQualityProfile("sample", "xoo", "one-issue-per-line");
    SonarScanner sampleProject = SonarScanner.create(projectDir("shared/xoo-sample"));
    orchestrator.executeBuild(sampleProject);
    createUser(LOGIN, "George Orwell");
    createGroup(GROUP_NAME);
  }

  @Test
  public void permission_web_services() {
    tester.wsClient().permissions().addUser(
      new AddUserRequest()
        .setPermission("admin")
        .setLogin(LOGIN));
    tester.wsClient().permissions().addGroup(
      new AddGroupRequest()
        .setPermission("admin")
        .setGroupName(GROUP_NAME));

    Permissions.WsSearchGlobalPermissionsResponse searchGlobalPermissionsWsResponse = tester.wsClient().permissions().searchGlobalPermissions(new SearchGlobalPermissionsRequest());
    assertThat(searchGlobalPermissionsWsResponse.getPermissionsList().get(0).getKey()).isEqualTo("admin");
    assertThat(searchGlobalPermissionsWsResponse.getPermissionsList().get(0).getUsersCount()).isEqualTo(1);
    // by default, a group has the global admin permission
    assertThat(searchGlobalPermissionsWsResponse.getPermissionsList().get(0).getGroupsCount()).isEqualTo(2);

    Permissions.UsersWsResponse users = tester.wsClient().permissions()
      .users(new UsersRequest().setPermission("admin"));
    assertThat(users.getUsersList()).extracting("login").contains(LOGIN);

    Permissions.WsGroupsResponse groupsResponse = tester.wsClient().permissions()
      .groups(new GroupsRequest()
        .setPermission("admin"));
    assertThat(groupsResponse.getGroupsList()).extracting("name").contains(GROUP_NAME);
  }

  @Test
  public void template_permission_web_services() {
    Permissions.CreateTemplateWsResponse createTemplateWsResponse = tester.wsClient().permissions().createTemplate(
      new CreateTemplateRequest()
        .setName("my-new-template")
        .setDescription("template-used-in-tests"));
    assertThat(createTemplateWsResponse.getPermissionTemplate().getName()).isEqualTo("my-new-template");

    tester.wsClient().permissions().addUserToTemplate(
      new AddUserToTemplateRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .setLogin(LOGIN));

    tester.wsClient().permissions().addGroupToTemplate(
      new AddGroupToTemplateRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .setGroupName(GROUP_NAME));

    tester.wsClient().permissions().addProjectCreatorToTemplate(
      new AddProjectCreatorToTemplateRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template"));

    SearchTemplatesWsResponse searchTemplatesWsResponse = tester.wsClient().permissions().searchTemplates(
      new SearchTemplatesRequest()
        .setQ("my-new-template"));
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getName()).isEqualTo("my-new-template");
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getPermissions(0).getKey()).isEqualTo("admin");
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getPermissions(0).getUsersCount()).isEqualTo(1);
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getPermissions(0).getGroupsCount()).isEqualTo(1);
    assertThat(searchTemplatesWsResponse.getPermissionTemplates(0).getPermissions(0).getWithProjectCreator()).isTrue();

    tester.wsClient().permissions().removeGroupFromTemplate(
      new RemoveGroupFromTemplateRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .setGroupName(GROUP_NAME));

    tester.wsClient().permissions().removeUserFromTemplate(
      new RemoveUserFromTemplateRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
        .setLogin(LOGIN));

    tester.wsClient().permissions().removeProjectCreatorFromTemplate(
      new RemoveProjectCreatorFromTemplateRequest()
        .setPermission("admin")
        .setTemplateName("my-new-template")
    );

    SearchTemplatesWsResponse clearedSearchTemplatesWsResponse = tester.wsClient().permissions().searchTemplates(
      new SearchTemplatesRequest()
        .setQ("my-new-template"));
    assertThat(clearedSearchTemplatesWsResponse.getPermissionTemplates(0).getPermissionsList())
      .extracting(Permission::getUsersCount, Permission::getGroupsCount, Permission::getWithProjectCreator)
      .hasSize(5)
      .containsOnly(tuple(0, 0, false));
  }

  private static void createUser(String login, String name) {
    tester.wsClient().wsConnector().call(
      new PostRequest("api/users/create")
        .setParam("login", login)
        .setParam("name", name)
        .setParam("password", "123456"));
  }

  private static void createGroup(String groupName) {
    tester.wsClient().wsConnector().call(
      new PostRequest("api/user_groups/create")
        .setParam("name", groupName));
  }
}
