/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.authorisation;

import com.sonar.orchestrator.Orchestrator;
import it.Category1Suite;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.project.NewProject;
import org.sonar.wsclient.project.Project;
import org.sonarqube.ws.client.permission.AddGroupWsRequest;
import org.sonarqube.ws.client.permission.AddUserWsRequest;
import org.sonarqube.ws.client.permission.PermissionsService;
import org.sonarqube.ws.client.permission.RemoveGroupWsRequest;
import org.sonarqube.ws.client.permission.RemoveUserWsRequest;
import util.user.UserRule;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
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

  static PermissionsService permissionsWsClient;

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
  public void should_not_be_able_to_provision_project() {
    runSelenese(orchestrator, "/authorisation/ProvisioningPermissionTest/should-not-be-able-to-provision-project.html");
  }

  /**
   * SONAR-3871
   * SONAR-4709
   */
  @Test
  public void should_be_able_to_provision_project() {
    runSelenese(orchestrator, "/authorisation/ProvisioningPermissionTest/should-be-able-to-provision-project.html");
  }

  /**
   * SONAR-3871
   * SONAR-4709
   */
  @Test
  public void should_be_allowed_on_ws_with_permission() {
    final String newKey = "new-project";
    final String newName = "New Project";

    SonarClient client = orchestrator.getServer().wsClient(USER_WITH_PROVISIONING, PASSWORD);

    Project created = client.projectClient().create(NewProject.create().key(newKey).name(newName));

    assertThat(created).isNotNull();
    assertThat(created.key()).isEqualTo(newKey);
    assertThat(created.name()).isEqualTo(newName);
  }

  /**
   * SONAR-3871
   * SONAR-4709
   */
  @Test
  public void should_not_be_allowed_on_ws_without_permission() {
    SonarClient client = orchestrator.getServer().wsClient(USER_WITHOUT_PROVISIONING, PASSWORD);

    thrown.expect(HttpException.class);
    thrown.expectMessage("401");
    client.projectClient().create(NewProject.create().key("new-project").name("New Project"));
  }

  private static void addUserPermission(String login, String permission) {
    permissionsWsClient.addUser(new AddUserWsRequest().setLogin(login).setPermission(permission));
  }

  private static void removeUserPermission(String login, String permission) {
    permissionsWsClient.removeUser(new RemoveUserWsRequest().setLogin(login).setPermission(permission));
  }
}
