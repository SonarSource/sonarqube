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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.qa.util.pageobjects.ProjectsManagementPage;
import org.sonarqube.ws.Permissions;
import org.sonarqube.ws.client.permissions.AddUserToTemplateRequest;
import org.sonarqube.ws.client.permissions.CreateTemplateRequest;
import org.sonarqube.ws.client.permissions.UsersRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionTemplatePageTest {

  @ClassRule
  public static Orchestrator orchestrator = AuthorizationSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator)
    // all the tests of AuthorizationSuite must disable organizations
    .disableOrganizations();

  @Test
  public void bulk_apply_permission_template() {
    String project = tester.projects().provision().getKey();
    String userLogin = tester.users().generateMemberOfDefaultOrganization().getLogin();
    String adminLogin = tester.users().generateAdministratorOnDefaultOrganization().getLogin();

    tester.wsClient().permissions().createTemplate(new CreateTemplateRequest().setName("foo-template"));
    tester.wsClient().permissions().addUserToTemplate(
      new AddUserToTemplateRequest()
        .setPermission("admin")
        .setTemplateName("foo-template")
        .setLogin(userLogin));

    ProjectsManagementPage page = tester.openBrowser().logIn().submitCredentials(adminLogin).openProjectsManagement();
    page.shouldHaveProject(project);
    page.bulkApplyPermissionTemplate("foo-template");
    Permissions.UsersWsResponse usersResponse = tester.wsClient().permissions().users(new UsersRequest()
      .setProjectKey(project)
      .setPermission("admin")
    );
    assertThat(usersResponse.getUsersCount()).isEqualTo(1);
    assertThat(usersResponse.getUsers(0).getLogin()).isEqualTo(userLogin);
  }

}
