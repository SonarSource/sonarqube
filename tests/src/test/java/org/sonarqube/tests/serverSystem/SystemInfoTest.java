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
package org.sonarqube.tests.serverSystem;

import com.sonar.orchestrator.Orchestrator;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.qa.util.pageobjects.SystemInfoPage;
import org.sonarqube.tests.Category4Suite;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class SystemInfoTest {

  private static final String ADMIN_USER_LOGIN = "admin-user";

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Test
  public void test_system_info_page() {
    tester.users().generateAdministrator(u -> u.setLogin(ADMIN_USER_LOGIN).setPassword(ADMIN_USER_LOGIN));
    SystemInfoPage page = tester.openBrowser().logIn().submitCredentials(ADMIN_USER_LOGIN).openSystemInfo();
    page.shouldHaveCards("System", "Web", "Compute Engine", "Search");

    page.getCardItem("System")
      .shouldHaveHealth()
      .shouldHaveMainSection()
      .shouldHaveSection("Database")
      .shouldNotHaveSection("Settings")
      .shouldNotHaveSection("Plugins")
      .shouldNotHaveSection("Statistics")
      .shouldHaveField("Official Distribution")
      .shouldHaveField("Version")
      .shouldHaveField("High Availability")
      .shouldNotHaveField("Health")
      .shouldNotHaveField("Health Causes");

    page.getCardItem("Web")
      .shouldHaveSection("Web JVM Properties")
      .shouldHaveSection("Web Logging")
      .shouldHaveSection("Web JVM State");

    page.getCardItem("Compute Engine")
      .shouldHaveSection("Compute Engine Database Connection")
      .shouldHaveSection("Compute Engine JVM State")
      .shouldHaveSection("Compute Engine Logging")
      .shouldHaveSection("Compute Engine Tasks");

    page.getCardItem("Search Engine")
      .shouldHaveSection("Search State")
      .shouldHaveSection("Search Indexes");
  }

  @Test
  public void test_system_info_web_service() throws Exception {
    waitForComputeEngineToBeUp(orchestrator);

    WsResponse response = tester.wsClient().wsConnector().call(
      new GetRequest("api/system/info"));

    assertThat(response.code()).isEqualTo(200);
    Map<String, Object> json = ItUtils.jsonToMap(response.content());

    // SONAR-7436 monitor ES and CE
    assertThat((Map)json.get("Compute Engine Database Connection")).isNotEmpty();
    assertThat((Map)json.get("Compute Engine JVM State")).isNotEmpty();
    assertThat((Map)json.get("Compute Engine Tasks")).isNotEmpty();
    Map<String,Object> esJson = (Map) json.get("Search State");
    assertThat(esJson.get("State")).isEqualTo("GREEN");

    // SONAR-7271 get settings
    Map<String,Object> settingsJson = (Map) json.get("Settings");
    assertThat(settingsJson.get("sonar.jdbc.url")).isNotNull();
    assertThat(settingsJson.get("sonar.path.data")).isNotNull();
  }

  private static void waitForComputeEngineToBeUp(Orchestrator orchestrator) throws IOException {
    for (int i = 0; i < 10_000; i++) {
      File logs = orchestrator.getServer().getCeLogs();
      if (FileUtils.readFileToString(logs).contains("Compute Engine is operational")) {
        return;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // ignored
      }
    }
    throw new IllegalStateException("Compute Engine is not operational");
  }
}
