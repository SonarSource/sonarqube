/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import org.sonar.api.CoreProperties;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.System2;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClientFactories;
import org.springframework.context.annotation.Bean;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.sonar.core.config.ProxyProperties.HTTP_PROXY_PASSWORD;
import static org.sonar.core.config.ProxyProperties.HTTP_PROXY_USER;

public class ScannerWsClientProvider {
  static final int CONNECT_TIMEOUT_MS = 5_000;
  static final String READ_TIMEOUT_SEC_PROPERTY = "sonar.ws.timeout";
  public static final String TOKEN_PROPERTY = "sonar.token";
  private static final String TOKEN_ENV_VARIABLE = "SONAR_TOKEN";
  static final int DEFAULT_READ_TIMEOUT_SEC = 60;

  @Bean("DefaultScannerWsClient")
  public DefaultScannerWsClient provide(ScannerProperties scannerProps, EnvironmentInformation env, GlobalAnalysisMode globalMode,
    System2 system, AnalysisWarnings analysisWarnings) {
    String url = defaultIfBlank(scannerProps.property("sonar.host.url"), "http://localhost:9000");
    HttpConnector.Builder connectorBuilder = HttpConnector.newBuilder();

    String timeoutSec = defaultIfBlank(scannerProps.property(READ_TIMEOUT_SEC_PROPERTY), valueOf(DEFAULT_READ_TIMEOUT_SEC));
    String envVarToken = defaultIfBlank(system.envVariable(TOKEN_ENV_VARIABLE), null);
    String token = defaultIfBlank(scannerProps.property(TOKEN_PROPERTY), envVarToken);
    String login = defaultIfBlank(scannerProps.property(CoreProperties.LOGIN), token);
    connectorBuilder
      .readTimeoutMilliseconds(parseInt(timeoutSec) * 1_000)
      .connectTimeoutMilliseconds(CONNECT_TIMEOUT_MS)
      .userAgent(env.toString())
      .url(url)
      .credentials(login, scannerProps.property(CoreProperties.PASSWORD));

    // OkHttp detect 'http.proxyHost' java property, but credentials should be filled
    final String proxyUser = System.getProperty(HTTP_PROXY_USER, "");
    if (!proxyUser.isEmpty()) {
      connectorBuilder.proxyCredentials(proxyUser, System.getProperty(HTTP_PROXY_PASSWORD));
    }

    return new DefaultScannerWsClient(WsClientFactories.getDefault().newClient(connectorBuilder.build()), login != null,
      globalMode, analysisWarnings);
  }
}
