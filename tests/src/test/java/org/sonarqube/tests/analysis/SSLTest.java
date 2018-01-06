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
import org.sonarqube.tests.Category3Suite;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import util.ItUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class SSLTest {

  private static final String CLIENT_KEYSTORE = "/analysis/SSLTest/clientkeystore.jks";
  private static final String CLIENT_KEYSTORE_PWD = "clientp12pwd";

  private static final String CLIENT_TRUSTSTORE = "/analysis/SSLTest/clienttruststore.jks";
  private static final String CLIENT_TRUSTSTORE_PWD = "clienttruststorepwd";

  @ClassRule
  public static Orchestrator orchestrator = Category3Suite.ORCHESTRATOR;

  private static Server server;
  private static int httpsPort;

  @Before
  public void deleteData() {
    orchestrator.resetData();
  }

  public static void startSSLTransparentReverseProxy(boolean requireClientAuth) throws Exception {
    int httpPort = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());
    httpsPort = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());

    // Setup Threadpool
    QueuedThreadPool threadPool = new QueuedThreadPool();
    threadPool.setMaxThreads(500);

    server = new Server(threadPool);

    // HTTP Configuration
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setSecureScheme("https");
    httpConfig.setSecurePort(httpsPort);
    httpConfig.setSendServerVersion(true);
    httpConfig.setSendDateHeader(false);

    // Handler Structure
    HandlerCollection handlers = new HandlerCollection();
    handlers.setHandlers(new Handler[] {proxyHandler(), new DefaultHandler()});
    server.setHandler(handlers);

    ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    http.setPort(httpPort);
    server.addConnector(http);

    Path serverKeyStore = Paths.get(SSLTest.class.getResource("/analysis/SSLTest/serverkeystore.jks").toURI()).toAbsolutePath();
    String keyStorePassword = "serverkeystorepwd";
    String serverKeyPassword = "serverp12pwd";
    Path serverTrustStore = Paths.get(SSLTest.class.getResource("/analysis/SSLTest/servertruststore.jks").toURI()).toAbsolutePath();
    String trustStorePassword = "servertruststorepwd";

    // SSL Context Factory
    SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setKeyStorePath(serverKeyStore.toString());
    sslContextFactory.setKeyStorePassword(keyStorePassword);
    sslContextFactory.setKeyManagerPassword(serverKeyPassword);
    sslContextFactory.setTrustStorePath(serverTrustStore.toString());
    sslContextFactory.setTrustStorePassword(trustStorePassword);
    sslContextFactory.setNeedClientAuth(requireClientAuth);
    sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA",
      "SSL_DHE_RSA_WITH_DES_CBC_SHA", "SSL_DHE_DSS_WITH_DES_CBC_SHA",
      "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
      "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
      "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
      "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");

    // SSL HTTP Configuration
    HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);

    // SSL Connector
    ServerConnector sslConnector = new ServerConnector(server,
      new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
      new HttpConnectionFactory(httpsConfig));
    sslConnector.setPort(httpsPort);
    server.addConnector(sslConnector);

    server.start();
  }

  private static ServletContextHandler proxyHandler() {
    ServletContextHandler contextHandler = new ServletContextHandler();
    contextHandler.setServletHandler(newServletHandler());
    return contextHandler;
  }

  private static ServletHandler newServletHandler() {
    ServletHandler handler = new ServletHandler();
    ServletHolder holder = handler.addServletWithMapping(ProxyServlet.Transparent.class, "/*");
    holder.setInitParameter("proxyTo", orchestrator.getServer().getUrl());
    return handler;
  }

  @After
  public void stopProxy() throws Exception {
    if (server != null && server.isStarted()) {
      server.stop();
    }
  }

  @Test
  public void simple_analysis_with_server_and_client_certificate() throws Exception {
    startSSLTransparentReverseProxy(true);

    SonarScanner sonarScanner = SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("sonar.host.url", "https://localhost:" + httpsPort);

    BuildResult buildResult = orchestrator.executeBuildQuietly(sonarScanner);
    assertThat(buildResult.getLastStatus()).isNotEqualTo(0);
    assertThat(buildResult.getLogs()).contains("javax.net.ssl.SSLHandshakeException");

    Path clientTruststore = Paths.get(SSLTest.class.getResource(CLIENT_TRUSTSTORE).toURI()).toAbsolutePath();
    String truststorePassword = CLIENT_TRUSTSTORE_PWD;
    Path clientKeystore = Paths.get(SSLTest.class.getResource(CLIENT_KEYSTORE).toURI()).toAbsolutePath();
    String keystorePassword = CLIENT_KEYSTORE_PWD;

    buildResult = orchestrator.executeBuild(sonarScanner
      .setEnvironmentVariable("SONAR_SCANNER_OPTS",
        "-Djavax.net.ssl.trustStore=" + clientTruststore.toString() +
          " -Djavax.net.ssl.trustStorePassword=" + truststorePassword +
          " -Djavax.net.ssl.keyStore=" + clientKeystore.toString() +
          " -Djavax.net.ssl.keyStorePassword=" + keystorePassword));
  }

  @Test
  public void simple_analysis_with_server_certificate() throws Exception {
    startSSLTransparentReverseProxy(false);

    SonarScanner sonarScanner = SonarScanner.create(ItUtils.projectDir("shared/xoo-sample"))
      .setProperty("sonar.host.url", "https://localhost:" + httpsPort);

    BuildResult buildResult = orchestrator.executeBuildQuietly(sonarScanner);
    assertThat(buildResult.getLastStatus()).isNotEqualTo(0);
    assertThat(buildResult.getLogs()).contains("javax.net.ssl.SSLHandshakeException");

    Path clientTruststore = Paths.get(SSLTest.class.getResource(CLIENT_TRUSTSTORE).toURI()).toAbsolutePath();
    String truststorePassword = CLIENT_TRUSTSTORE_PWD;

    buildResult = orchestrator.executeBuild(sonarScanner
      .setEnvironmentVariable("SONAR_SCANNER_OPTS",
        "-Djavax.net.ssl.trustStore=" + clientTruststore.toString() +
          " -Djavax.net.ssl.trustStorePassword=" + truststorePassword));
  }
}
