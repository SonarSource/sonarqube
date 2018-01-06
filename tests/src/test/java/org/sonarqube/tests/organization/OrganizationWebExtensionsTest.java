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
package org.sonarqube.tests.organization;

import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Organizations.Organization;
import org.sonarqube.ws.Users.CreateWsResponse.User;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.WebDriverRunner.url;
import static org.assertj.core.api.Assertions.assertThat;

public class OrganizationWebExtensionsTest {

  @ClassRule
  public static Orchestrator orchestrator = OrganizationSuite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator);

  @Test
  public void organization_page() {
    Organization organization = tester.organizations().generate();
    tester.openBrowser().open("/organizations/" + organization.getKey() + "/projects");

    $("#organization-navigation-more").click();
    $(By.linkText("Organization Page")).shouldBe(Condition.visible).click();

    assertThat(url()).contains("uiextensionsplugin/organization_page");
    $("body").shouldHave(text("uiextensionsplugin/organization_page"));
  }

  @Test
  public void organization_admin_page() {
    Organization organization = tester.organizations().generate();
    User administrator = tester.users().generateAdministrator(organization);
    tester.openBrowser()
      .logIn().submitCredentials(administrator.getLogin())
      .open("/organizations/" + organization.getKey() + "/projects");

    $("#organization-navigation-admin").click();
    $(By.linkText("Organization Admin Page")).shouldBe(Condition.visible).click();

    assertThat(url()).contains("uiextensionsplugin/organization_admin_page");
    $("body").shouldHave(text("uiextensionsplugin/organization_admin_page"));
  }
}
