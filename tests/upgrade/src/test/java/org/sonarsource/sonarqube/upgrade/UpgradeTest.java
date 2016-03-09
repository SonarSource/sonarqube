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
package org.sonarsource.sonarqube.upgrade;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.OrchestratorBuilder;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.version.Version;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.sonar.wsclient.services.ResourceQuery;

import static org.assertj.core.api.Assertions.assertThat;

public class UpgradeTest {

  public static final String PROJECT_KEY = "org.apache.struts:struts-parent";

  private Orchestrator orchestrator;

  @After
  public void stop() {
    if (orchestrator != null) {
      orchestrator.stop();
      orchestrator = null;
    }
  }

  @Test
  public void test_upgrade_from_4_5_lts() {
    testDatabaseUpgrade(Version.create("4.5.1"));
  }

  @Test
  public void test_upgrade_from_5_2() {
    testDatabaseUpgrade(Version.create("5.2"));
  }

  private void testDatabaseUpgrade(Version fromVersion, BeforeUpgrade... tasks) {
    startServer(fromVersion, false);
    scanProject();
    int files = countFiles(PROJECT_KEY);
    assertThat(files).isGreaterThan(0);

    for (BeforeUpgrade task : tasks) {
      task.execute();
    }

    stopServer();
    // latest version
    startServer(Version.create(Orchestrator.builderEnv().getSonarVersion()), true);
    checkSystemStatus(ServerStatusResponse.Status.DB_MIGRATION_NEEDED);
    upgradeDatabase();
    checkSystemStatus(ServerStatusResponse.Status.UP);

    assertThat(countFiles(PROJECT_KEY)).isEqualTo(files);
    scanProject();
    assertThat(countFiles(PROJECT_KEY)).isEqualTo(files);
    browseWebapp();
  }

  private void checkSystemStatus(ServerStatusResponse.Status serverStatus) {
    ServerStatusResponse serverStatusResponse = new ServerStatusCall(orchestrator).call();

    assertThat(serverStatusResponse.getStatus()).isEqualTo(serverStatus);
  }

  private void browseWebapp() {
    testUrl("/");
    testUrl("/api/issues/search?projectKeys=org.apache.struts%3Astruts-core");
    testUrl("/api/components/tree?baseComponentKey=org.apache.struts%3Astruts-core");
    testUrl("/api/measures/component_tree?baseComponentKey=org.apache.struts%3Astruts-core&metricKeys=ncloc,files,violations");
    testUrl("/api/qualityprofiles/search");
    testUrl("/issues/index");
    testUrl("/dashboard/index/org.apache.struts:struts-parent");
    testUrl("/issues/search");
    testUrl("/component/index?id=org.apache.struts%3Astruts-core%3Asrc%2Fmain%2Fjava%2Forg%2Fapache%2Fstruts%2Fchain%2Fcommands%2Fgeneric%2FWrappingLookupCommand.java");
    testUrl("/profiles");
  }

  private void upgradeDatabase() {
    ServerMigrationResponse serverMigrationResponse = new ServerMigrationCall(orchestrator).callAndWait();

    assertThat(serverMigrationResponse.getStatus()).isEqualTo(ServerMigrationResponse.Status.MIGRATION_SUCCEEDED);
  }

  private void startServer(Version sqVersion, boolean keepDatabase) {
    String jdbcUrl = MssqlConfig.fixUrl(Configuration.createEnv(), sqVersion);
    OrchestratorBuilder builder = Orchestrator.builderEnv()
      .setOrchestratorProperty("sonar.jdbc.url", jdbcUrl)
      .setSonarVersion(sqVersion.toString());
    builder.setOrchestratorProperty("orchestrator.keepDatabase", String.valueOf(keepDatabase));
    if (!keepDatabase) {
      builder.restoreProfileAtStartup(FileLocation.ofClasspath("/sonar-way-5.1.xml"));
    }
    builder.setOrchestratorProperty("javaVersion", "OLDEST_COMPATIBLE").addPlugin("java");
    orchestrator = builder.build();
    orchestrator.start();
  }

  private void stopServer() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  private void scanProject() {
    MavenBuild build = MavenBuild.create(new File("projects/struts-1.3.9-diet/pom.xml"))
      .setCleanSonarGoals()
      .setProperty("sonar.dynamicAnalysis", "false")
      .setProperty("sonar.scm.disabled", "true")
      .setProperty("sonar.cpd.cross_project", "true")
      .setProperty("sonar.profile", "sonar-way-5.1");
    orchestrator.executeBuild(build);
  }

  private int countFiles(String key) {
    return orchestrator.getServer().getWsClient().find(ResourceQuery.createForMetrics(key, "files")).getMeasureIntValue("files");
  }

  private void testUrl(String path) {
    HttpURLConnection connection = null;
    try {
      URL url = new URL(orchestrator.getServer().getUrl() + path);
      connection = (HttpURLConnection) url.openConnection();
      connection.connect();
      assertThat(connection.getResponseCode()).as("Fail to load " + path).isEqualTo(HttpURLConnection.HTTP_OK);

      // Error HTML pages generated by Ruby on Rails
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

  private static interface BeforeUpgrade {
    void execute();
  }
}
