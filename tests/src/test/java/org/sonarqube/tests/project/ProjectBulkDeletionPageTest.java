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
package org.sonarqube.tests.project;

import com.sonar.orchestrator.Orchestrator;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.client.components.SearchProjectsRequest;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;

public class ProjectBulkDeletionPageTest {

  @ClassRule
  public static Orchestrator orchestrator = ProjectSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  private String sysAdminLogin;

  @Before
  public void setUp() {
    sysAdminLogin = tester.users().generateAdministratorOnDefaultOrganization().getLogin();
  }

  /**
   * SONAR-2614, SONAR-3805
   */
  @Test
  public void bulk_deletion_on_selected_projects() {
    Project project1 = tester.projects().provision(p -> p.setName("Foo"));
    Project project2 = tester.projects().provision(p -> p.setName("Bar"));
    Project project3 = tester.projects().provision(p -> p.setName("FooQux"));

    tester.openBrowser().logIn().submitCredentials(sysAdminLogin).open("/organizations/default-organization/projects_management");
    $("#projects-management-page").shouldHave(text(project1.getName())).shouldHave(text(project2.getName())).shouldHave(text(project3.getName()));

    $("#projects-management-page .search-box-input").val("foo").pressEnter();
    $("#projects-management-page").shouldNotHave(text(project2.getName())).shouldHave(text(project1.getName())).shouldHave(text(project3.getName()));

    $("#projects-management-page .js-delete").click();
    $(".modal").shouldBe(visible);
    $(".modal button").click();
    $("#projects-management-page").shouldNotHave(text(project1.getName())).shouldNotHave(text(project3.getName()));

    assertThat(tester.wsClient().components().searchProjects(new SearchProjectsRequest())
      .getComponentsCount()).isEqualTo(1);
  }
}
