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
package org.sonarqube.tests.startup;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.util.NetworkUtils;
import java.io.File;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonarqube.qa.util.LogsTailer;
import org.sonarqube.qa.util.Tester;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.users.SearchRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.pluginArtifact;

public class StartupIndexationTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public TestRule safeguard = new DisableOnDebug(Timeout.seconds(600));

  @Test
  public void elasticsearch_error_at_startup_must_shutdown_node() throws Exception {
    try (SonarQube sonarQube = new SonarQube();
         LogsTailer.Watch failedInitialization = sonarQube.logsTailer.watch("Background initialization failed. Stopping SonarQube");
         LogsTailer.Watch stopWatcher = sonarQube.logsTailer.watch("SonarQube is stopped")) {
      sonarQube.lockAllElasticsearchWrites();
      sonarQube.resume();
      stopWatcher.waitForLog(10, TimeUnit.SECONDS);
      assertThat(stopWatcher.getLog()).isPresent();
      assertThat(failedInitialization.getLog()).isPresent();
    }

    // Restarting is recreating the indexes
    try (SonarQube sonarQube = new SonarQube();
         LogsTailer.Watch sonarQubeIsUpWatcher = sonarQube.logsTailer.watch("SonarQube is up")) {
      sonarQube.resume();
      sonarQubeIsUpWatcher.waitForLog(20, TimeUnit.SECONDS);
      SearchRequest searchRequest = new SearchRequest().setQ("admin");
      Users.SearchWsResponse searchWsResponse = sonarQube.tester.wsClient().users().search(searchRequest);
      assertThat(searchWsResponse.getUsersCount()).isEqualTo(1);
      assertThat(searchWsResponse.getUsers(0).getName()).isEqualTo("Administrator");
    }
  }

  private class SonarQube implements AutoCloseable {
    private final Orchestrator orchestrator;
    private final Tester tester;
    private final File pauseFile;
    private final LogsTailer logsTailer;
    private final int esHttpPort = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());

    SonarQube() throws Exception {
      pauseFile = temp.newFile();
      FileUtils.touch(pauseFile);

      orchestrator = Orchestrator.builderEnv()
        .setServerProperty("sonar.web.pause.path", pauseFile.getAbsolutePath())
        .addPlugin(pluginArtifact("wait-at-platform-level4-plugin"))
        .setStartupLogWatcher(l -> l.contains("PlatformLevel4 initialization phase is paused"))
        .setServerProperty("sonar.search.httpPort", "" + esHttpPort)
        .build();

      tester = new Tester(orchestrator);
      orchestrator.start();
      tester.before();

      logsTailer = LogsTailer.builder()
        .addFile(orchestrator.getServer().getWebLogs())
        .addFile(orchestrator.getServer().getCeLogs())
        .addFile(orchestrator.getServer().getAppLogs())
        .build();
    }

    LogsTailer logs() {
      return logsTailer;
    }

    void resume() throws Exception {
      FileUtils.forceDelete(pauseFile);
    }

    void lockElasticsearchWritesOn(String index) throws Exception {
      tester.elasticsearch().lockWrites(index);
    }

    void lockAllElasticsearchWrites() throws Exception {
      for (String index : Arrays.asList("metadatas", "components", "tests", "projectmeasures", "rules", "issues", "users", "views")) {
        lockElasticsearchWritesOn(index);
      }
    }

    @Override
    public void close() {
      if (tester != null) {
        try {
          tester.after();
        } catch (Exception e) {
          e.printStackTrace(System.err);
        }
      }
      if (orchestrator != null) {
        orchestrator.stop();
      }
      if (logsTailer != null) {
        logsTailer.close();
      }
    }
  }
}
