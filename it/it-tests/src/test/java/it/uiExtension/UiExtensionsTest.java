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
package it.uiExtension;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import it.Category4Suite;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import pageobjects.Navigation;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;
import static util.selenium.Selenese.runSelenese;

public class UiExtensionsTest {

  @ClassRule
  public static Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Rule
  public Navigation nav = Navigation.get(orchestrator);

  @BeforeClass
  public static void setUp() throws Exception {
    orchestrator.resetData();
    orchestrator.executeBuild(SonarScanner.create(projectDir("shared/xoo-sample")));
  }

  @Test
  public void test_static_files() {
    runSelenese(orchestrator, "/uiExtension/UiExtensionsTest/static-files.html");
  }

  @Test
  public void global_page() {
    nav.open("/about");

    // on about page
    $("#global-navigation-more").click();
    $(By.linkText("Global Page")).click();

    assertThat(url()).contains("/uiextensionsplugin/global_page");
    $("body").shouldHave(text("uiextensionsplugin/global_page"));
  }

  @Test
  public void global_admin_page() {
    nav.logIn().asAdmin().open("/about");

    $(".navbar-admin-link").click();
    $("#settings-navigation-configuration").click();
    $(By.linkText("Global Admin Page")).click();

    assertThat(url()).contains("uiextensionsplugin/global_admin_page");
    $("body").shouldHave(text("uiextensionsplugin/global_admin_page"));
  }

  @Test
  public void project_page() {
    nav.open("/dashboard?id=sample");

    $("#component-navigation-more").click();
    $(By.linkText("Project Page")).click();

    assertThat(url()).contains("uiextensionsplugin/project_page");
    $("body").shouldHave(text("uiextensionsplugin/project_page"));
  }

  @Test
  public void project_admin_page() {
    nav.logIn().asAdmin().open("/dashboard?id=sample");

    $("#component-navigation-admin").click();
    $(By.linkText("Project Admin Page")).click();

    assertThat(url()).contains("uiextensionsplugin/project_admin_page");
    $("body").shouldHave(text("uiextensionsplugin/project_admin_page"));
  }
}
