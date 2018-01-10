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
import com.sonar.orchestrator.build.SonarScanner;
import java.io.IOException;
import java.util.Map;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonarqube.tests.Category4Suite;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.System;
import util.ItUtils;
import util.selenium.Selenese;

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
    System.StatusResponse response = tester.wsClient().system().status();

    String version = response.getVersion();
    if (!startsWithAny(version, new String[]{"6.", "7.", "8."})) {
      fail("Bad version: " + version);
    }
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2727
   */
  @Test
  public void display_warnings_when_using_h2() {
    String dialect = orchestrator.getConfiguration().getString("sonar.jdbc.dialect");
    if (dialect == null || dialect.equals("h2") || dialect.equals("embedded")) {
      Selenese.runSelenese(orchestrator, "/serverSystem/ServerSystemTest/derby-warning.html");
    }
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2840
   */
  @Test
  public void hide_jdbc_settings_to_non_admin() {
    Selenese.runSelenese(orchestrator, "/serverSystem/ServerSystemTest/hide-jdbc-settings.html");
  }

  @Test
  public void http_response_should_be_gzipped() {
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
    Selenese.runSelenese(orchestrator, "/serverSystem/ServerSystemTest/url_ending_by_jsp.html");
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

}
