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

import com.codeborne.selenide.Condition;
import com.sonar.orchestrator.Orchestrator;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import pageobjects.Navigation;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.WebDriverRunner.url;
import static java.util.Collections.emptyMap;
import static java.util.Locale.ENGLISH;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.pluginArtifact;
import static util.ItUtils.xooPlugin;

public class OrganizationUiExtensionsTest {

  @ClassRule
  public static final Orchestrator orchestrator = Orchestrator.builderEnv()
    .addPlugin(xooPlugin())
    .addPlugin(pluginArtifact("ui-extensions-plugin"))
    .build();

  @Rule
  public Navigation nav = Navigation.get(orchestrator);

  private static WsClient adminClient;

  @BeforeClass
  public static void setUp() throws Exception {
    orchestrator.resetData();
    adminClient = newAdminWsClient(orchestrator);
    orchestrator.getServer().post("api/organizations/enable_support", emptyMap());
  }

  @Test
  public void organization_page() {
    String orgKey = createOrganization();
    nav.open("/organizations/" + orgKey + "/projects");

    $("#organization-navigation-more").click();
    $(By.linkText("Organization Page")).shouldBe(Condition.visible).click();

    assertThat(url()).contains("uiextensionsplugin/organization_page");
    $("body").shouldHave(text("uiextensionsplugin/organization_page"));
  }

  @Test
  public void organization_admin_page() {
    String orgKey = createOrganization();
    nav.logIn().asAdmin().open("/organizations/" + orgKey + "/projects");

    $("#context-navigation a.navbar-admin-link").click();
    $(By.linkText("Organization Admin Page")).shouldBe(Condition.visible).click();

    assertThat(url()).contains("uiextensionsplugin/organization_admin_page");
    $("body").shouldHave(text("uiextensionsplugin/organization_admin_page"));
  }

  private static String createOrganization() {
    String keyAndName = randomAlphabetic(32).toLowerCase(ENGLISH);
    adminClient.organizations().create(new CreateWsRequest.Builder().setKey(keyAndName).setName(keyAndName).build()).getOrganization();
    return keyAndName;
  }
}
