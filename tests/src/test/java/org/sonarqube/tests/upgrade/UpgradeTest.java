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
package org.sonarqube.tests.upgrade;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonarqube.ws.WsMeasures.Measure;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.WsResponse;
import org.sonarqube.ws.client.measure.ComponentWsRequest;

import static com.codeborne.selenide.Condition.hasText;
import static com.codeborne.selenide.Selenide.$;
import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;

public class UpgradeTest {

  private static final String PROJECT_KEY = "org.apache.struts:struts-parent";
  private static final String LATEST_JAVA_RELEASE = "LATEST_RELEASE";
  private static final Version VERSION_5_2 = Version.create("5.2");
  private static final Version VERSION_5_6_1 = Version.create("5.6.1");
  private static final Version VERSION_CURRENT = Version.create("DEV");

  private Orchestrator orchestrator;

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  @Test
  public void test_upgrade_from_5_6_1() {
    testDatabaseUpgrade(VERSION_5_6_1);
  }

  @Test
  public void test_upgrade_from_5_2_via_5_6_1() {
    testDatabaseUpgrade(VERSION_5_2, VERSION_5_6_1);
  }

  private void testDatabaseUpgrade(Version fromVersion, Version... intermediaryVersions) {
    startOldVersionServer(fromVersion, false);
    scanProject();
    int files = countFiles(PROJECT_KEY);
    assertThat(files).isGreaterThan(0);
    stopServer();

    Arrays.stream(intermediaryVersions).forEach((sqVersion) -> {
      startOldVersionServer(sqVersion, true);
      upgrade(sqVersion);
      verifyAnalysis(files);
      stopServer();
    });

    startDevServer();
    upgrade(VERSION_CURRENT);
    verifyAnalysis(files);
    stopServer();
  }

  private void verifyAnalysis(int expectedNumberOfFiles) {
    assertThat(countFiles(PROJECT_KEY)).isEqualTo(expectedNumberOfFiles);
    scanProject();
    assertThat(countFiles(PROJECT_KEY)).isEqualTo(expectedNumberOfFiles);
    browseWebapp();
  }

  private void upgrade(Version sqVersion) {
    checkSystemStatus(sqVersion, ServerStatusResponse.Status.DB_MIGRATION_NEEDED);
    if (sqVersion.equals(VERSION_CURRENT)) {
      checkUrlsBeforeUpgrade();
    }
    ServerMigrationResponse serverMigrationResponse = new ServerMigrationCall(orchestrator).callAndWait();
    assertThat(serverMigrationResponse.getStatus())
      .describedAs("Migration status of version " + sqVersion + " should be MIGRATION_SUCCEEDED")
      .isEqualTo(ServerMigrationResponse.Status.MIGRATION_SUCCEEDED);
    checkSystemStatus(sqVersion, ServerStatusResponse.Status.UP);
    checkUrlsAfterUpgrade();
  }

  private void checkSystemStatus(Version sqVersion, ServerStatusResponse.Status serverStatus) {
    ServerStatusResponse serverStatusResponse = new ServerStatusCall(orchestrator).callAndWait();

    assertThat(serverStatusResponse.getStatus())
      .describedAs("Server status of version " + sqVersion + " should be " + serverStatus)
      .isEqualTo(serverStatus);
  }

  private void checkUrlsBeforeUpgrade() {
    // These urls should be available when system requires a migration
    checkUrlIsReturningOk("/api/system/status");
    checkUrlIsReturningOk("/api/system/db_migration_status");
    checkUrlIsReturningOk("/api/webservices/list");

    // These urls should not be available when system requires a migration
    checkUrlIsReturningNotFound("/api/issues/search?projectKeys=org.apache.struts%3Astruts-core");
    checkUrlIsReturningNotFound("/api/components/tree?baseComponentKey=org.apache.struts%3Astruts-core");
    checkUrlIsReturningNotFound("/api/measures/component_tree?baseComponentKey=org.apache.struts%3Astruts-core&metricKeys=ncloc,files,violations");
    checkUrlIsReturningNotFound("/api/qualityprofiles/search");

    // These page should all redirect to maintenance page
    checkUrlIsRedirectedToMaintenancePage("/");
    checkUrlIsRedirectedToMaintenancePage("/issues/index");
    checkUrlIsRedirectedToMaintenancePage("/dashboard/index/org.apache.struts:struts-parent");
    checkUrlIsRedirectedToMaintenancePage("/issues");
    checkUrlIsRedirectedToMaintenancePage(
      "/component/index?id=org.apache.struts%3Astruts-core%3Asrc%2Fmain%2Fjava%2Forg%2Fapache%2Fstruts%2Fchain%2Fcommands%2Fgeneric%2FWrappingLookupCommand.java");
    checkUrlIsRedirectedToMaintenancePage("/profiles");
  }

  private void checkUrlsAfterUpgrade() {
    checkUrlIsReturningOk("/api/system/status");
    checkUrlIsReturningOk("/api/system/db_migration_status");
    checkUrlIsReturningOk("/api/webservices/list");
    checkUrlIsReturningOk("/api/l10n/index");

    checkUrlIsReturningOk("/api/issues/search?projectKeys=org.apache.struts%3Astruts-core");
    checkUrlIsReturningOk("/api/components/tree?baseComponentKey=org.apache.struts%3Astruts-core");
    checkUrlIsReturningOk("/api/measures/component_tree?baseComponentKey=org.apache.struts%3Astruts-core&metricKeys=ncloc,files,violations");
    checkUrlIsReturningOk("/api/qualityprofiles/search");
  }

  private void browseWebapp() {
    testUrl("/");
    testUrl("/api/issues/search?projectKeys=org.apache.struts%3Astruts-core");
    testUrl("/api/components/tree?baseComponentKey=org.apache.struts%3Astruts-core");
    testUrl("/api/measures/component_tree?baseComponentKey=org.apache.struts%3Astruts-core&metricKeys=ncloc,files,violations");
    testUrl("/api/qualityprofiles/search");
    testUrl("/issues/index");
    testUrl("/dashboard/index/org.apache.struts:struts-parent");
    testUrl("/issues");
    testUrl("/component/index?id=org.apache.struts%3Astruts-core%3Asrc%2Fmain%2Fjava%2Forg%2Fapache%2Fstruts%2Fchain%2Fcommands%2Fgeneric%2FWrappingLookupCommand.java");
    testUrl("/profiles");
  }

  private void startOldVersionServer(Version sqVersion, boolean keepDatabase) {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .setSonarVersion(sqVersion.toString())
      .setOrchestratorProperty("orchestrator.keepDatabase", String.valueOf(keepDatabase))
      .setOrchestratorProperty("javaVersion", "3.14")
      .addPlugin("java")
      .setStartupLogWatcher(log -> log.contains("Process[web] is up"));
    orchestrator = builder.build();
    orchestrator.start();
    initSelenide(orchestrator);
  }

  private void startDevServer() {
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .setZipFile(FileLocation.byWildcardMavenFilename(new File("../sonar-application/target"), "sonar*.zip").getFile())
      .setOrchestratorProperty("orchestrator.keepDatabase", "true")
      .setOrchestratorProperty("javaVersion", LATEST_JAVA_RELEASE)
      .addPlugin("java")
      .setStartupLogWatcher(log -> log.contains("Database must be upgraded"));
    orchestrator = builder.build();
    orchestrator.start();
    initSelenide(orchestrator);
  }

  private void stopServer() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  private void scanProject() {
    MavenBuild build = MavenBuild.create(new File("projects/struts-1.3.9-diet/pom.xml"))
      .setCleanSonarGoals()
      // exclude pom.xml, otherwise it will be published in SQ 6.3+ and not in previous versions, resulting in a different number of components
      .setProperty("sonar.exclusions", "**/pom.xml")
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("sonar.cpd.cross_project", "true");
    orchestrator.executeBuild(build);
  }

  private int countFiles(String key) {
    if (orchestrator.getConfiguration().getSonarVersion().isGreaterThanOrEquals("5.4")) {
      Measure measure = newWsClient(orchestrator).measures().component(new ComponentWsRequest().setComponentKey(key).setMetricKeys(Collections.singletonList("files")))
        .getComponent().getMeasures(0);
      return parseInt(measure.getValue());
    }
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "files")).getMeasureIntValue("files");
  }

  private void testUrl(String path) {
    HttpURLConnection connection = null;
    try {
      URL url = new URL(orchestrator.getServer().getUrl() + path);
      connection = (HttpURLConnection) url.openConnection();
      connection.connect();
      assertThat(connection.getResponseCode()).as("Fail to load " + path).isEqualTo(HttpURLConnection.HTTP_OK);

      String content = IOUtils.toString(connection.getInputStream());
      assertThat(content).as("Fail to load " + path).doesNotContain("something went wrong");
      assertThat(content).as("Fail to load " + path).doesNotContain("The page you were looking for doesn't exist");
      assertThat(content).as("Fail to load " + path).doesNotContain("Unauthorized access");

    } catch (IOException e) {
      throw new IllegalStateException("Error with " + path, e);

    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private void checkUrlIsReturningOk(String url) {
    newWsClient(orchestrator).wsConnector().call(new GetRequest(url)).failIfNotSuccessful();
  }

  private void checkUrlIsReturningNotFound(String url) {
    WsResponse response = newWsClient(orchestrator).wsConnector().call(new GetRequest(url));
    assertThat(response.code()).isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
  }

  private void checkUrlIsRedirectedToMaintenancePage(String url) {
    shouldBeRedirectToMaintenance(url);
  }

  private static WsClient newWsClient(Orchestrator orchestrator) {
    Server server = orchestrator.getServer();
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(server.getUrl())
      .build());
  }

  private static void initSelenide(Orchestrator orchestrator) {
    String browser = orchestrator.getConfiguration().getString("orchestrator.browser", "firefox");
    SelenideConfig.INSTANCE
      .setBrowser(browser)
      .setBaseUrl(orchestrator.getServer().getUrl());
    WebDriverRunner.getWebDriver().manage().deleteAllCookies();
  }

  private void shouldBeRedirectToMaintenance(String relativeUrl) {
    Selenide.open(relativeUrl);
    $("#content").should(hasText("SonarQube is under maintenance"));
  }
}
