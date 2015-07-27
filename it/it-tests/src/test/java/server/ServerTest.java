/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package server;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.SonarRunner;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.selenium.Selenese;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.wsclient.base.HttpException;
import org.sonar.wsclient.services.PropertyDeleteQuery;
import org.sonar.wsclient.services.PropertyUpdateQuery;
import org.sonar.wsclient.services.Server;
import org.sonar.wsclient.services.ServerQuery;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class ServerTest {

  Orchestrator orchestrator;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2727
   */
  @Test
  public void display_warnings_when_using_h2() {
    OrchestratorBuilder builder = Orchestrator.builderEnv();
    if (builder.getOrchestratorConfiguration().getString("sonar.jdbc.dialect").equals("h2")) {
      orchestrator = builder.build();
      orchestrator.start();

      Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("derby-warnings",
        "/server/ServerTest/derby-warning.html").build();
      orchestrator.executeSelenese(selenese);
    }
  }

  /**
   * See http://jira.codehaus.org/browse/SONAR-2840
   */
  @Test
  public void hide_jdbc_settings_to_non_admin() {
    orchestrator = Orchestrator.createEnv();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("jdbc-settings",
      "/server/ServerTest/hide-jdbc-settings.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void test_settings() {
    URL secretKeyUrl = getClass().getResource("/server/ServerTest/sonar-secret.txt");
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.pluginArtifact("settings-plugin"))
      .addPlugin(ItUtils.pluginArtifact("license-plugin"))
      .setServerProperty("sonar.secretKeyPath", secretKeyUrl.getFile())
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("settings",
      "/server/ServerTest/settings/general-settings.html",

      // SONAR-2869 the annotation @Properties can be used on extensions and not only on plugin entry points
      "/server/ServerTest/settings/hidden-extension-property.html",
      "/server/ServerTest/settings/global-extension-property.html",

      // SONAR-3344 - licenses
      "/server/ServerTest/settings/ignore-corrupted-license.html",
      "/server/ServerTest/settings/display-license.html",
      "/server/ServerTest/settings/display-untyped-license.html",

      // SONAR-2084 - encryption
      "/server/ServerTest/settings/generate-secret-key.html",
      "/server/ServerTest/settings/encrypt-text.html",

      // SONAR-1378 - property types
      "/server/ServerTest/settings/validate-property-type.html",

      // SONAR-3127 - hide passwords
      "/server/ServerTest/settings/hide-passwords.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void property_relocation() {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.pluginArtifact("property-relocation-plugin"))
      .addPlugin(ItUtils.xooPlugin())
      .setServerProperty("sonar.deprecatedKey", "true")
      .build();
    orchestrator.start();

    SonarRunner withDeprecatedKey = SonarRunner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("sonar.deprecatedKey", "true");
    SonarRunner withNewKey = SonarRunner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("sonar.newKey", "true");
    // should not fail
    orchestrator.executeBuilds(withDeprecatedKey, withNewKey);

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("property_relocation",
      "/server/ServerTest/settings/property_relocation.html"
      ).build();
    orchestrator.executeSelenese(selenese);
  }

  /**
   * SONAR-5542
   */
  @Test
  public void force_authentication_should_be_used_on_java_web_services_but_not_on_batch_index_and_file() throws IOException {
    orchestrator = Orchestrator.createEnv();
    orchestrator.start();

    try {
      orchestrator.getServer().getAdminWsClient().update(new PropertyUpdateQuery("sonar.forceAuthentication", "true"));

      // /batch/index should never need authentication
      String batchIndex = orchestrator.getServer().wsClient().get("/batch/index");
      assertThat(batchIndex).isNotEmpty();

      String jar = batchIndex.split("\\|")[0];

      // /batch/file should never need authentication
      HttpClient httpclient = new DefaultHttpClient();
      try {
        HttpGet get = new HttpGet(orchestrator.getServer().getUrl() + "/batch/file?name=" + jar);
        HttpResponse response = httpclient.execute(get);
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
        EntityUtils.consume(response.getEntity());

        // As Sonar runner is still using /batch/key, we have to also verify it
        get = new HttpGet(orchestrator.getServer().getUrl() + "/batch/" + jar);
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
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.pluginArtifact("global-property-change-plugin"))
      .build();
    orchestrator.start();

    orchestrator.getServer().adminWsClient().post("api/properties/create?id=globalPropertyChange.received&value=NEWVALUE");
    assertThat(FileUtils.readFileToString(orchestrator.getServer().getLogs()).contains("Received change: NEWVALUE"));
  }

  /**
   * SONAR-3516
   */
  @Test
  public void check_minimal_sonar_version_at_startup() throws Exception {
    try {
      orchestrator = Orchestrator.builderEnv()
        .addPlugin(FileLocation.of(new File(ServerTest.class.getResource("/server/ServerTest/incompatible-plugin-1.0.jar").toURI())))
        .build();
      orchestrator.start();
      fail();
    } catch (Exception e) {
      assertThat(FileUtils.readFileToString(orchestrator.getServer().getLogs())).contains(
        "Plugin incompatible-plugin [incompatibleplugin] requires at least SonarQube 5.9");
    }
  }

  /**
   * SONAR-3962
   */
  @Test
  public void not_fail_with_url_ending_by_jsp() {
    orchestrator = Orchestrator.builderEnv().addPlugin(ItUtils.xooPlugin()).build();
    orchestrator.start();

    orchestrator.executeBuild(SonarRunner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("sonar.projectKey", "myproject.jsp"));
    // Access dashboard
    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("url_ending_by_jsp",
      "/server/ServerTest/url_ending_by_jsp.html").build();
    orchestrator.executeSelenese(selenese);
  }

  @Test
  public void support_install_dir_with_whitespaces() throws Exception {
    String dirName = "target/has space";
    FileUtils.deleteDirectory(new File(dirName));
    orchestrator = Orchestrator.builderEnv().setOrchestratorProperty("orchestrator.workspaceDir", dirName).build();
    orchestrator.start();

    Server.Status status = orchestrator.getServer().getAdminWsClient().find(new ServerQuery()).getStatus();
    assertThat(status).isEqualTo(Server.Status.UP);
  }

  // SONAR-4404
  @Test
  public void should_get_settings_default_value() {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.pluginArtifact("server-plugin"))
      .build();
    orchestrator.start();

    Selenese selenese = Selenese.builder().setHtmlTestsInClasspath("settings-default-value",
      "/server/ServerTest/settings-default-value.html").build();
    orchestrator.executeSelenese(selenese);
  }

  // SONAR-4748
  @Test
  public void should_create_in_temp_folder() throws Exception {
    orchestrator = Orchestrator.builderEnv()
      .addPlugin(ItUtils.pluginArtifact("server-plugin"))
      .setServerProperty("sonar.createTempFiles", "true")
      .build();
    orchestrator.start();

    File tempDir = new File(orchestrator.getServer().getHome(), "temp/tmp");

    String logs = FileUtils.readFileToString(orchestrator.getServer().getLogs());
    assertThat(logs).contains("Creating temp directory: " + tempDir.getAbsolutePath() + File.separator + "sonar-it");
    assertThat(logs).contains("Creating temp file: " + tempDir.getAbsolutePath() + File.separator + "sonar-it");

    // Verify temp folder is created
    assertThat(new File(tempDir, "sonar-it")).isDirectory().exists();

    orchestrator.stop();

    // Verify temp folder is deleted after shutdown
    assertThat(new File(tempDir, "sonar-it")).doesNotExist();
  }

  /**
   * SONAR-4843
   */
  @Test
  public void restart_forbidden_if_not_dev_mode() throws Exception {
    // server classloader locks Jar files on Windows
    if (!SystemUtils.IS_OS_WINDOWS) {
      orchestrator = Orchestrator.builderEnv().build();
      orchestrator.start();
      try {
        orchestrator.getServer().adminWsClient().systemClient().restart();
        fail();
      } catch (Exception e) {
        assertThat(e.getMessage()).contains("403");
      }
    }
  }

  /**
   * SONAR-4843
   */
  @Test
  public void restart_on_dev_mode() throws Exception {
    // server classloader locks Jar files on Windows
    if (!SystemUtils.IS_OS_WINDOWS) {
      orchestrator = Orchestrator.builderEnv().setServerProperty("sonar.web.dev", "true").build();
      orchestrator.start();

      orchestrator.getServer().adminWsClient().systemClient().restart();
      assertThat(FileUtils.readFileToString(orchestrator.getServer().getLogs()))
        .contains("Restart server")
        .contains("Server restarted");
    }
  }
}
