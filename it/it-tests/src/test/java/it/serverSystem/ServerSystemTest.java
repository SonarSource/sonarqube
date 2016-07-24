/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package it.serverSystem;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.selenium.Selenese;
import it.Category4Suite;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONValue;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.services.Server;
import org.sonar.wsclient.services.ServerQuery;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;
import util.ItUtils;
import util.selenium.SeleneseTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static util.ItUtils.newAdminWsClient;

public class ServerSystemTest {

  @ClassRule
  public static final Orchestrator orchestrator = Category4Suite.ORCHESTRATOR;

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  @Test
  public void get_sonarqube_version() {
    String version = orchestrator.getServer().getWsClient().find(new ServerQuery()).getVersion();
    if (!StringUtils.startsWithAny(version, new String[] {"5.", "6."})) {
      fail("Bad version: " + version);
    }
  }

  @Test
  public void get_server_status() {
    assertThat(orchestrator.getServer().getWsClient().find(new ServerQuery()).getStatus()).isEqualTo(Server.Status.UP);
  }

  @Test
  public void generate_server_id() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("server_id",
      "/serverSystem/ServerSystemTest/missing_ip.html",
      // SONAR-4102
      "/serverSystem/ServerSystemTest/organisation_must_not_accept_special_chars.html",
      "/serverSystem/ServerSystemTest/valid_id.html").build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void display_system_info() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("server-administration",
      "/serverSystem/ServerSystemTest/system_info.html").build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void download_system_info() throws Exception {
    waitForComputeEngineToBeUp(orchestrator);

    WsResponse response = newAdminWsClient(orchestrator).wsConnector().call(
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
      File logs = orchestrator.getServer().getLogs();
      if (FileUtils.readFileToString(logs).contains("Compute Engine is up")) {
        return;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // ignored
      }
    }
    throw new IllegalStateException("Compute Engine is not up");
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2727
   */
  @Test
  public void display_warnings_when_using_h2() {
    if (orchestrator.getConfiguration().getString("sonar.jdbc.dialect").equals("h2")) {
      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("derby-warnings",
        "/serverSystem/ServerSystemTest/derby-warning.html").build();
      new SeleneseTest(selenese).runOn(orchestrator);
    }
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2840
   */
  @Test
  public void hide_jdbc_settings_to_non_admin() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("jdbc-settings",
      "/serverSystem/ServerSystemTest/hide-jdbc-settings.html").build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  @Test
  public void http_response_should_be_gzipped() throws IOException {
    HttpClient httpclient = new DefaultHttpClient();
    try {
      HttpGet get = new HttpGet(orchestrator.getServer().getUrl());
      HttpResponse response = httpclient.execute(get);

      assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
      assertThat(response.getLastHeader("Content-Encoding")).isNull();
      EntityUtils.consume(response.getEntity());

      get = new HttpGet(orchestrator.getServer().getUrl());
      get.addHeader("Accept-Encoding", "gzip, deflate");
      response = httpclient.execute(get);
      assertThat(response.getLastHeader("Content-Encoding").getValue()).isEqualTo("gzip");
      EntityUtils.consume(response.getEntity());

    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }

  /**
   * SONAR-3962
   */
  // TODO should be moved elsewhere
  @Test
  public void not_fail_with_url_ending_by_jsp() {
    orchestrator.executeBuild(SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("sonar.projectKey", "myproject.jsp"));
    // Access dashboard
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("url_ending_by_jsp",
      "/serverSystem/ServerSystemTest/url_ending_by_jsp.html").build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  /**
   * SONAR-3147
   */
  // TODO should be moved elsewhere
  @Test
  public void test_widgets_web_service() throws IOException {
    HttpClient httpclient = new DefaultHttpClient();
    try {
      HttpGet get = new HttpGet(orchestrator.getServer().getUrl() + "/api/widgets");
      HttpResponse response = httpclient.execute(get);

      assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
      String json = IOUtils.toString(response.getEntity().getContent());
      List widgets = (List) JSONValue.parse(json);
      assertThat(widgets.size()).isGreaterThan(10);

      // quick test of the first widget
      assertThat(((Map) widgets.get(0)).get("title")).isNotNull();

      EntityUtils.consume(response.getEntity());

    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }

  /**
   * SONAR-5197
   */
  // TODO should be moved elsewhere
  @Test
  public void api_ws_shortcut() throws Exception {
    HttpClient httpclient = new DefaultHttpClient();
    try {
      HttpGet get = new HttpGet(orchestrator.getServer().getUrl() + "/api");
      HttpResponse response = httpclient.execute(get);

      assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
      String json = IOUtils.toString(response.getEntity().getContent());
      Map jsonAsMap = (Map) JSONValue.parse(json);
      assertThat(jsonAsMap.get("webServices")).isNotNull();
      EntityUtils.consume(response.getEntity());

    } finally {
      httpclient.getConnectionManager().shutdown();
    }
  }
}
