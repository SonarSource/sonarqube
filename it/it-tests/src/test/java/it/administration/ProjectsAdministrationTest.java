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
package it.administration;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category1Suite;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.client.permission.RemoveGroupWsRequest;
import org.sonarqube.ws.client.project.UpdateVisibilityRequest;
import pageobjects.Navigation;
import util.ItUtils;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static org.sonarqube.ws.client.project.UpdateVisibilityRequest.Visibility.PRIVATE;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.projectDir;

public class ProjectsAdministrationTest {

  @ClassRule
  public static Orchestrator orchestrator = Category1Suite.ORCHESTRATOR;

  @Rule
  public Navigation nav = Navigation.get(orchestrator);

  @Before
  public void deleteAnalysisData() throws SQLException {
    orchestrator.resetData();
  }

  @Test
  public void return_all_projects_even_when_no_permission() throws Exception {
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")).setProperties("sonar.projectKey", "sample1"));
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")).setProperties("sonar.projectKey", "sample2"));
    ItUtils.newAdminWsClient(orchestrator).projects().updateVisibility(new UpdateVisibilityRequest("sample2", PRIVATE));
    // Remove 'Admin' permission for admin group on project 2 -> No one can access or admin this project, expect System Admin
    newAdminWsClient(orchestrator).permissions().removeGroup(new RemoveGroupWsRequest().setProjectKey("sample2").setGroupName("sonar-administrators").setPermission("admin"));

    nav.logIn().asAdmin().open("/projects_admin");
    $(".data.zebra")
      .shouldHave(text("sample1"))
      .shouldHave(text("sample2"));
  }

}
