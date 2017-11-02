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
package org.sonarqube.tests.projectAdministration;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category1Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.WsProjects.CreateWsResponse.Project;
import org.sonarqube.ws.client.component.SearchProjectsRequest;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class ProjectBulkDeletionPageTest {

  private String adminUser;

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Before
  public void deleteData() {
    orchestrator.resetData();
    adminUser = tester.users().generateAdministrator().getLogin();
  }

  /**
   * SONAR-2614, SONAR-3805
   */
  @Test
  public void test_bulk_deletion_on_selected_projects() throws Exception {
    Project project1 = tester.projects().generate(null, t -> t.setName("Foo"));
    Project project2 = tester.projects().generate(null, t -> t.setName("Bar"));
    Project project3 = tester.projects().generate(null, t -> t.setName("FooQux"));

    // we must have several projects to test the bulk deletion
    executeBuild(project1);
    executeBuild(project2);
    executeBuild(project3);

    tester.openBrowser().logIn().submitCredentials(adminUser).open("/admin/projects_management");
    $("#content").shouldHave(text(project1.getName())).shouldHave(text(project2.getName())).shouldHave(text(project3.getName()));

    $(".search-box-input").val("foo").pressEnter();
    $("#content").shouldNotHave(text(project2.getName())).shouldHave(text(project1.getName())).shouldHave(text(project3.getName()));

    $(".js-delete").click();
    $(".modal button").click();
    $("#content").shouldNotHave(text(project1.getName())).shouldNotHave(text(project3.getName()));

    assertThat(tester.wsClient().components().searchProjects(SearchProjectsRequest.builder().build())
      .getComponentsCount()).isEqualTo(1);
  }

  private void executeBuild(Project project) {
    orchestrator.executeBuild(
      SonarScanner.create(projectDir("shared/xoo-sample"))
        .setProjectKey(project.getKey())
        .setProjectName(project.getName()));
  }

}
