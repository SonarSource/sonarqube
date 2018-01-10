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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.client.permissions.AddGroupRequest;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.sonarqube.ws.client.permissions.RemoveGroupRequest;
import org.sonarqube.ws.client.permissions.RemoveUserRequest;
import org.sonarqube.ws.client.projects.CreateRequest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static util.selenium.Selenese.runSelenese;

public class ProvisioningPermissionTest {

  private static final String PASSWORD = "password";
  private static final String ADMIN_WITH_PROVISIONING = "admin-with-provisioning";
  private static final String ADMIN_WITHOUT_PROVISIONING = "admin-without-provisioning";
  private static final String USER_WITH_PROVISIONING = "user-with-provisioning";
  private static final String USER_WITHOUT_PROVISIONING = "user-without-provisioning";

  @ClassRule
  public static Orchestrator orchestrator = AuthorizationSuite.ORCHESTRATOR;

  private static Tester tester = new Tester(orchestrator)
    // all the tests of AuthorizationSuite must disable organizations
    .disableOrganizations();

  @ClassRule
  public static RuleChain ruleChain = RuleChain.outerRule(orchestrator).around(tester);

  @BeforeClass
  public static void init() {
    // remove default permission "provisioning" from anyone();
    tester.wsClient().permissions().removeGroup(new RemoveGroupRequest().setGroupName("anyone").setPermission("provisioning"));

    tester.users().generate(u -> u.setLogin(ADMIN_WITH_PROVISIONING).setPassword(PASSWORD));
    addUserPermission(ADMIN_WITH_PROVISIONING, "admin");
    addUserPermission(ADMIN_WITH_PROVISIONING, "provisioning");

    tester.users().generate(u -> u.setLogin(ADMIN_WITHOUT_PROVISIONING).setPassword(PASSWORD));
    addUserPermission(ADMIN_WITHOUT_PROVISIONING, "admin");
    removeUserPermission(ADMIN_WITHOUT_PROVISIONING, "provisioning");

    tester.users().generate(u -> u.setLogin(USER_WITH_PROVISIONING).setPassword(PASSWORD));
    addUserPermission(USER_WITH_PROVISIONING, "provisioning");

    tester.users().generate(u -> u.setLogin(USER_WITHOUT_PROVISIONING).setPassword(PASSWORD));
    removeUserPermission(USER_WITHOUT_PROVISIONING, "provisioning");
  }

  @AfterClass
  public static void restoreData() {
    tester.wsClient().permissions().addGroup(new AddGroupRequest().setGroupName("anyone").setPermission("provisioning"));
  }

  /**
   * SONAR-3871
   * SONAR-4709
   */
  @Test
  public void organization_administrator_cannot_provision_project_if_he_does_not_have_provisioning_permission() {
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

    Project created = tester.as(USER_WITH_PROVISIONING, PASSWORD).wsClient().projects()
      .create(new CreateRequest().setProject(newKey).setName(newName))
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
  public void user_cannot_provision_project_through_ws_if_he_does_not_have_provisioning_permission() {
    ItUtils.expectForbiddenError(() -> {
      tester.as(USER_WITHOUT_PROVISIONING, PASSWORD).wsClient().projects()
        .create(new CreateRequest().setProject("new-project").setName("New Project"))
        .getProject();
    });
  }

  private static void addUserPermission(String login, String permission) {
    tester.wsClient().permissions().addUser(new AddUserRequest().setLogin(login).setPermission(permission));
  }

  private static void removeUserPermission(String login, String permission) {
    tester.wsClient().permissions().removeUser(new RemoveUserRequest().setLogin(login).setPermission(permission));
  }
}
