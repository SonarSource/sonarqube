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
package org.sonarqube.tests.settings;

import com.sonar.orchestrator.Orchestrator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.ws.Settings.ValuesWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.setting.ValuesRequest;
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.pageobjects.licenses.LicenseItem;
import org.sonarqube.pageobjects.licenses.LicensesPage;
import util.user.UserRule;

import static com.codeborne.selenide.Condition.text;
import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.newAdminWsClient;
import static util.ItUtils.pluginArtifact;

public class LicensesPageTest {
  private static Orchestrator orchestrator;
  private static WsClient wsClient;

  @Rule
  public UserRule userRule = UserRule.from(orchestrator);

  private String adminUser;

  @BeforeClass
  public static void start() {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(pluginArtifact("license-plugin"))
      .build();
    orchestrator.start();

    wsClient = newAdminWsClient(orchestrator);
  }

  @AfterClass
  public static void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Before
  public void before() {
    adminUser = userRule.createAdminUser();
  }

  @Test
  public void display_licenses() {
    LicensesPage page = Navigation.create(orchestrator).logIn().submitCredentials(adminUser).openLicenses();

    page.getLicenses().shouldHaveSize(2);
    page.getLicensesAsItems().get(0).getName().shouldHave(text("Typed property"));
    page.getLicensesAsItems().get(1).getName().shouldHave(text("Property without license type"));
  }

  @Test
  public void change_licenses() {
    String EXAMPLE_LICENSE = "TmFtZTogRGV2ZWxvcHBlcnMKUGx1Z2luOiBhdXRvY29udHJvbApFeHBpcmVzOiAyMDEyLTA0LTAxCktleTogNjI5N2MxMzEwYzg2NDZiZTE5MDU1MWE4ZmZmYzk1OTBmYzEyYTIyMgo=";

    LicensesPage page = Navigation.create(orchestrator).logIn().submitCredentials(adminUser).openLicenses();
    LicenseItem licenseItem = page.getLicenseByKey("typed.license.secured");
    licenseItem.setLicense(EXAMPLE_LICENSE);

    ValuesWsResponse response = wsClient.settings()
      .values(ValuesRequest.builder().setKeys("typed.license.secured").build());
    assertThat(response.getSettings(0).getValue()).isEqualTo(EXAMPLE_LICENSE);
  }
}
