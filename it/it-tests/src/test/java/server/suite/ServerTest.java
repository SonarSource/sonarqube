/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package server.suite;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.selenium.Selenese;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.services.PropertyDeleteQuery;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import selenium.SeleneseTest;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerTest {

  @ClassRule
  public static final Orchestrator orchestrator = ServerTestSuite.ORCHESTRATOR;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void cleanDatabase() {
    orchestrator.resetData();
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2727
   */
  @Test
  public void display_warnings_when_using_h2() {
    if (orchestrator.getConfiguration().getString("sonar.jdbc.dialect").equals("h2")) {
      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("derby-warnings",
        "/server/ServerTest/derby-warning.html").build();
      new SeleneseTest(selenese).runOn(orchestrator);
    }
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2840
   */
  @Test
  public void hide_jdbc_settings_to_non_admin() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("jdbc-settings",
      "/server/ServerTest/hide-jdbc-settings.html").build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  /**
   * SONAR-5542
   */
  @Test
  public void force_authentication_should_be_used_on_java_web_services_but_not_on_batch_index_and_file() throws IOException {
    try {
      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.forceAuthentication", "true"));

      // /batch/index should never need authentication
      String batchIndex = orchestrator.getServer().wsClient().get("/scanner/index");
      assertThat(batchIndex).isNotEmpty();

      String jar = batchIndex.split("\\|")[0];

      // /batch/file should never need authentication
      HttpClient httpclient = new DefaultHttpClient();
      try {
        HttpGet get = new HttpGet(orchestrator.getServer().getUrl() + "/scanner/file?name=" + jar);
        HttpResponse response = httpclient.execute(get);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
        EntityUtils.consume(response.getEntity());

        // As Sonar runner is still using /batch/key, we have to also verify it
        get = new HttpGet(orchestrator.getServer().getUrl() + "/scanner/" + jar);
        response = httpclient.execute(get);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
        EntityUtils.consume(response.getEntity());

      } finally {
        httpclient.getConnectionManager().shutdown();
      }

      // but other java web services should need authentication
      try {
        orchestrator.getServer().wsClient().get("/api");
      } catch (HttpException e) {
        assertThat(e.getMessage()).contains("401");
      }

    } finally {
      orchestrator.getServer().getAdminWsClient().delete(new PropertyDeleteQuery("sonar.forceAuthentication"));
    }
  }

  /**
   * SONAR-3320
   */
  @Test
  public void global_property_change_extension_point() throws IOException {
    orchestrator.getServer().adminWsClient().post("api/properties/create?id=globalPropertyChange.received&value=NEWVALUE");
    assertThat(FileUtils.readFileToString(orchestrator.getServer().getLogs()).contains("Received change: NEWVALUE"));
  }

  /**
   * SONAR-3962
   */
  @Test
  public void not_fail_with_url_ending_by_jsp() {
    orchestrator.executeBuild(SonarRunner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("sonar.projectKey", "myproject.jsp"));
    // Access dashboard
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("url_ending_by_jsp",
      "/server/ServerTest/url_ending_by_jsp.html").build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

  // SONAR-4404
  @Test
  public void should_get_settings_default_value() {
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("settings-default-value",
      "/server/ServerTest/settings-default-value.html").build();
    new SeleneseTest(selenese).runOn(orchestrator);
  }

}
