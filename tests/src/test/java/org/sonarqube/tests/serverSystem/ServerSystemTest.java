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
package org.sonarqube.tests.serverSystem;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import okhttp3.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.pageobjects.Navigation;
import org.sonarqube.pageobjects.ServerIdPage;
import org.sonarqube.tests.Category4Suite;
import org.sonarqube.tests.Tester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.ServerId.ShowWsResponse;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import util.ItUtils;

import static org.apache.commons.lang.StringUtils.startsWithAny;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.call;

public class ServerSystemTest {

  private static final String ADMIN_USER_LOGIN = "admin-user";

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Rule
  public Tester tester = new Tester(orchestrator).disableOrganizations();

  @Before
  public void initAdminUser() {
    tester.users().generateAdministrator(u -> u.setLogin(ADMIN_USER_LOGIN).setPassword(ADMIN_USER_LOGIN));
  }

  @Test
  public void get_sonarqube_version() {
    Map<String, Object> json = callStatus();

    String version = (String) json.get("version");
    if (!startsWithAny(version, new String[] {"6."})) {
      fail("Bad version: " + version);
    }
  }

  @Test
  public void get_server_status() {
    Map<String, Object> json = callStatus();
    assertThat(json.get("status")).isEqualTo("UP");
  }

  @Test
  public void generate_server_id() throws IOException {
    Navigation nav = tester.openBrowser().openHome().logIn().submitCredentials(ADMIN_USER_LOGIN);
    String validIpAddress = getValidIpAddress();

    nav.openServerId()
      .setOrganization("Name with invalid chars like $")
      .setIpAddress(validIpAddress)
      .submitForm()
      .assertError();

    nav.openServerId()
      .setOrganization("DEMO")
      .setIpAddress("invalid_address")
      .submitForm()
      .assertError();

    ServerIdPage page = nav.openServerId()
      .setOrganization("DEMO")
      .setIpAddress(validIpAddress)
      .submitForm();

    String serverId = page.serverIdInput().val();
    assertThat(serverId).isNotEmpty();
  }

  private Map<String, Object> callStatus() {
    WsResponse statusResponse = tester.wsClient().wsConnector().call(new GetRequest("api/system/status"));
    return ItUtils.jsonToMap(statusResponse.content());
  }

  @Test
  public void display_system_info() {
    tester.runHtmlTests("/serverSystem/ServerSystemTest/system_info.html");
  }

  @Test
  public void download_system_info() throws Exception {
    waitForComputeEngineToBeUp(orchestrator);

    WsResponse response = tester.wsClient().wsConnector().call(
      new GetRequest("api/system/info"));

    assertThat(response.code()).isEqualTo(200);

    assertThat(response.content()).contains(
      // SONAR-7436 monitor ES and CE
      "\"Compute Engine Database Connection\":", "\"Compute Engine State\":", "\"Compute Engine Tasks\":",
      "\"Elasticsearch\":", "\"State\":\"GREEN\"",

      // SONAR-7271 get settings
      "\"Settings\":", "\"sonar.jdbc.url\":", "\"sonar.path.data\":");
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

  /**
   * See http://jira.codehaus.org/browse/SONAR-2727
   */
  @Test
  public void display_warnings_when_using_h2() {
    String dialect = orchestrator.getConfiguration().getString("sonar.jdbc.dialect");
    if (dialect == null || dialect.equals("h2") || dialect.equals("embedded")) {
      tester.runHtmlTests("/serverSystem/ServerSystemTest/derby-warning.html");
    }
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2840
   */
  @Test
  public void hide_jdbc_settings_to_non_admin() {
    tester.runHtmlTests( "/serverSystem/ServerSystemTest/hide-jdbc-settings.html");
  }

  @Test
  public void http_response_should_be_gzipped() throws IOException {
    String url = orchestrator.getServer().getUrl() + "/api/rules/search";
    Response metricsResponse = call(url);
    assertThat(metricsResponse.isSuccessful()).as("Response code is %s", metricsResponse.code()).isTrue();
    assertThat(metricsResponse.header("Content-Encoding")).isNull();

    Response homeResponse = call(url, "Accept-Encoding", "gzip, deflate");
    assertThat(homeResponse.isSuccessful()).as("Response code is %s", metricsResponse.code()).isTrue();
    assertThat(homeResponse.header("Content-Encoding")).isEqualToIgnoringCase("gzip");
  }

  /**
   * SONAR-3962
   */
  @Test
  public void not_fail_with_url_ending_by_jsp() {
    orchestrator.executeBuild(SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("sonar.projectKey", "myproject.jsp"));
    // Access dashboard
    tester.runHtmlTests("/serverSystem/ServerSystemTest/url_ending_by_jsp.html");
  }

  /**
   * SONAR-5197
   */
  // TODO should be moved elsewhere
  @Test
  public void api_ws_shortcut() throws Exception {
    Response response = call(orchestrator.getServer().getUrl() + "/api");
    assertThat(response.isSuccessful()).as("Response code is %s", response.code()).isTrue();
    String json = IOUtils.toString(response.body().byteStream());
    Map jsonAsMap = (Map) JSONValue.parse(json);
    assertThat(jsonAsMap.get("webServices")).isNotNull();
  }

  private String getValidIpAddress() throws IOException {
    ShowWsResponse response = ShowWsResponse.parseFrom(tester.wsClient().wsConnector().call(
      new GetRequest("api/server_id/show").setMediaType(MediaTypes.PROTOBUF)).contentStream());
    assertThat(response.getValidIpAddressesCount()).isGreaterThan(0);
    return response.getValidIpAddresses(0);
  }

}
