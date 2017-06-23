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
import org.sonarqube.tests.Category1Suite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.WsProjects.CreateWsResponse.Project;
import org.sonarqube.ws.client.HttpException;
import org.sonarqube.ws.client.permission.AddGroupWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.PermissionsService;
import org.sonarqube.ws.client.permission.RemoveGroupWsRequest;
import org.sonarqube.ws.client.permission.RemoveUserWsRequest;
import org.sonarqube.ws.client.project.CreateRequest;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.newUserWsClient;
import static util.selenium.Selenese.runSelenese;

public class ProvisioningPermissionTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @ClassRule
  public static UserRule userRule = UserRule.from(orchestrator);

  private static final String PASSWORD = "password";

  private static final String ADMIN_WITH_PROVISIONING = "admin-with-provisioning";
  private static final String ADMIN_WITHOUT_PROVISIONING = "admin-without-provisioning";
  private static final String USER_WITH_PROVISIONING = "user-with-provisioning";
  private static final String USER_WITHOUT_PROVISIONING = "user-without-provisioning";

  private static PermissionsService permissionsWsClient;

  @BeforeClass
  public static void init() {
    permissionsWsClient = newAdminWsClient(orchestrator).permissions();

    // remove default permission "provisioning" from anyone();
    permissionsWsClient.removeGroup(new RemoveGroupWsRequest().setGroupName("anyone").setPermission("provisioning"));

    userRule.createUser(ADMIN_WITH_PROVISIONING, PASSWORD);
    addUserPermission(ADMIN_WITH_PROVISIONING, "admin");
    addUserPermission(ADMIN_WITH_PROVISIONING, "provisioning");

    userRule.createUser(ADMIN_WITHOUT_PROVISIONING, PASSWORD);
    addUserPermission(ADMIN_WITHOUT_PROVISIONING, "admin");
    removeUserPermission(ADMIN_WITHOUT_PROVISIONING, "provisioning");

    userRule.createUser(USER_WITH_PROVISIONING, PASSWORD);
    addUserPermission(USER_WITH_PROVISIONING, "provisioning");

    userRule.createUser(USER_WITHOUT_PROVISIONING, PASSWORD);
    removeUserPermission(USER_WITHOUT_PROVISIONING, "provisioning");
  }

  @AfterClass
  public static void restoreData() throws Exception {
    userRule.resetUsers();
    permissionsWsClient.addGroup(new AddGroupWsRequest().setGroupName("anyone").setPermission("provisioning"));
  }

  /**
   * SONAR-3871
   * SONAR-4709
   */
  @Test
  public void organization_administrator_cannot_provision_project_if_he_doesnt_have_provisioning_permission() {
    runSelenese(orchestrator, "/authorisation/ProvisioningPermissionTest/should-not-be-able-to-provision-project.html");
  }

  /**
   * SONAR-3871
   * SONAR-4709
   */
  @Test
  public void organization_administrator_can_provision_project_if_he_has_provisioning_permission() {
    runSelenese(orchestrator, "/authorisation/ProvisioningPermissionTest/should-be-able-to-provision-project.html");
  }

  /**
   * SONAR-3871
   * SONAR-4709
   */
  @Test
  public void user_can_provision_project_through_ws_if_he_has_provisioning_permission() {
    final String newKey = "new-project";
    final String newName = "New Project";

    Project created = newUserWsClient(orchestrator, USER_WITH_PROVISIONING, PASSWORD).projects()
      .create(CreateRequest.builder().setKey(newKey).setName(newName).build())
      .getProject();

    assertThat(created).isNotNull();
    assertThat(created.getKey()).isEqualTo(newKey);
    assertThat(created.getName()).isEqualTo(newName);
  }

  /**
   * SONAR-3871
   * SONAR-4709
   */
  @Test
  public void user_cannot_provision_project_through_ws_if_he_doesnt_have_provisioning_permission() {
    thrown.expect(HttpException.class);
    thrown.expectMessage("403");

    newUserWsClient(orchestrator, USER_WITHOUT_PROVISIONING, PASSWORD).projects()
      .create(CreateRequest.builder().setKey("new-project").setName("New Project").build())
      .getProject();
  }

  private static void addUserPermission(String login, String permission) {
    permissionsWsClient.addUser(new AddUserWsRequest().setLogin(login).setPermission(permission));
  }

  private static void removeUserPermission(String login, String permission) {
    permissionsWsClient.removeUser(new RemoveUserWsRequest().setLogin(login).setPermission(permission));
  }
}
