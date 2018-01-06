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
package org.sonarqube.tests.ui;

import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.tests.Category4Suite;
import org.sonarqube.ws.Projects;
import org.sonarqube.ws.Users.CreateWsResponse.User;
import org.sonarqube.ws.client.projects.CreateRequest;
import util.ItUtils;
import util.selenium.Selenese;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;

public class UiExtensionsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void test_static_files() {
    Selenese.runSelenese(orchestrator, "/ui/UiExtensionsTest/static-files.html");
  }

  @Test
  public void test_global_page() {
    tester.openBrowser().open("/about");

    // on about page
    $("#global-navigation-more").click();
    $(By.linkText("Global Page")).click();

    assertThat(url()).contains("/uiextensionsplugin/global_page");
    $("body").shouldHave(text("uiextensionsplugin/global_page"));
  }

  @Test
  public void test_global_administration_page() {
    User administrator = tester.users().generateAdministrator();
    tester.openBrowser()
      .logIn().submitCredentials(administrator.getLogin())
      .open("/settings");

    $("#settings-navigation-configuration").click();
    $(By.linkText("Global Admin Page")).click();

    assertThat(url()).contains("uiextensionsplugin/global_admin_page");
    $("body").shouldHave(text("uiextensionsplugin/global_admin_page"));
  }

  @Test
  public void test_project_page() {
    Projects.CreateWsResponse.Project project = createSampleProject();

    tester.openBrowser().open("/dashboard?id=" + project.getKey());

    $("#component-navigation-more").click();
    $(By.linkText("Project Page")).click();

    assertThat(url()).contains("uiextensionsplugin/project_page");
    $("body").shouldHave(text("uiextensionsplugin/project_page"));
  }

  @Test
  public void test_project_administration_page() {
    Projects.CreateWsResponse.Project project = createSampleProject();
    User administrator = tester.users().generateAdministrator();

    tester.openBrowser()
      .logIn().submitCredentials(administrator.getLogin())
      .open("/dashboard?id=" + project.getKey());

    $("#component-navigation-admin").click();
    $(By.linkText("Project Admin Page")).click();

    assertThat(url()).contains("uiextensionsplugin/project_admin_page");
    $("body").shouldHave(text("uiextensionsplugin/project_admin_page"));
  }

  private Projects.CreateWsResponse.Project createSampleProject() {
    String projectKey = ItUtils.newProjectKey();
    return tester.wsClient().projects().create(new CreateRequest()
      .setProject(projectKey)
      .setName("Name of " + projectKey)).getProject();
  }
}
