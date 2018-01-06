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
package org.sonarqube.tests.analysis;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import com.sonar.orchestrator.util.NetworkUtils;
import java.net.InetAddress;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.MovedContextHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.sonarqube.tests.Category3Suite;
import org.sonarqube.qa.util.Tester;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class RedirectTest {

  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;
  public static Tester tester = new Tester(orchestrator).disableOrganizations();

  @ClassRule
  public static RuleChain chain = RuleChain
    .outerRule(orchestrator)
    .around(tester);

  private static Server server;
  private static int redirectPort;

  @BeforeClass
  public static void beforeClass() throws Exception {
    // enforce scanners to be authenticated
    tester.settings().setGlobalSetting("sonar.forceAuthentication", "true");

    orchestrator.resetData();
    redirectPort = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());

    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setMaxThreads(500);

    server = new Server(threadPool);
    // HTTP Configuration
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setSendServerVersion(true);
    httpConfig.setSendDateHeader(false);

    // Moved handler
    MovedContextHandler movedContextHandler = new MovedContextHandler();
    movedContextHandler.setPermanent(true);
    movedContextHandler.setNewContextURL(orchestrator.getServer().getUrl());
    server.setHandler(movedContextHandler);

    // http connector
    ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    http.setPort(redirectPort);
    server.addConnector(http);
    server.start();
  }

  @AfterClass
  public static void after() throws Exception {
    server.stop();
  }

  @Test
  public void testFollowRedirectWithAuthentication() {
    orchestrator.getServer();
    SonarScanner sonarScanner = SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("sonar.host.url", "http://localhost:" + redirectPort)
      .setProperties(
        "sonar.login", com.sonar.orchestrator.container.Server.ADMIN_LOGIN,
        "sonar.password", com.sonar.orchestrator.container.Server.ADMIN_PASSWORD);
    BuildResult buildResult = orchestrator.executeBuild(sonarScanner);

    // logs show original URL
    assertThat(buildResult.getLogs()).contains("ANALYSIS SUCCESSFUL, you can browse " + "http://localhost:" + redirectPort);

  }
}
