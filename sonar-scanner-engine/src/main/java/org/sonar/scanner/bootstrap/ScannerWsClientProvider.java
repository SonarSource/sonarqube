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
package org.sonar.scanner.bootstrap;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ScannerSide;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClientFactories;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;

@ScannerSide
public class ScannerWsClientProvider extends ProviderAdapter {

  static final int CONNECT_TIMEOUT_MS = 5_000;
  static final String READ_TIMEOUT_SEC_PROPERTY = "sonar.ws.timeout";
  static final int DEFAULT_READ_TIMEOUT_SEC = 60;

  private ScannerWsClient wsClient;

  public synchronized ScannerWsClient provide(final GlobalProperties settings, final EnvironmentInformation env, GlobalAnalysisMode globalMode) {
    if (wsClient == null) {
      String url = defaultIfBlank(settings.property("sonar.host.url"), CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE);
      HttpConnector.Builder connectorBuilder = HttpConnector.newBuilder();

      String timeoutSec = defaultIfBlank(settings.property(READ_TIMEOUT_SEC_PROPERTY), valueOf(DEFAULT_READ_TIMEOUT_SEC));
      String login = defaultIfBlank(settings.property(CoreProperties.LOGIN), null);
      connectorBuilder
        .readTimeoutMilliseconds(parseInt(timeoutSec) * 1_000)
        .connectTimeoutMilliseconds(CONNECT_TIMEOUT_MS)
        .userAgent(env.toString())
        .url(url)
        .credentials(login, settings.property(CoreProperties.PASSWORD));

      // OkHttp detect 'http.proxyHost' java property, but credentials should be filled
      final String proxyUser = System.getProperty("http.proxyUser", "");
      if (!proxyUser.isEmpty()) {
        connectorBuilder.proxyCredentials(proxyUser, System.getProperty("http.proxyPassword"));
      }

      wsClient = new ScannerWsClient(WsClientFactories.getDefault().newClient(connectorBuilder.build()), login != null, globalMode);
    }
    return wsClient;
  }
}
