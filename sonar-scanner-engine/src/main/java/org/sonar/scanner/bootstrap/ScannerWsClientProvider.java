/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import org.sonar.api.CoreProperties;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.System2;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClientFactories;
import org.springframework.context.annotation.Bean;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sonar.core.config.ProxyProperties.HTTP_PROXY_PASSWORD;
import static org.sonar.core.config.ProxyProperties.HTTP_PROXY_USER;

public class ScannerWsClientProvider {
  static final int DEFAULT_CONNECT_TIMEOUT = 5;
  static final int DEFAULT_RESPONSE_TIMEOUT = 0;
  static final String READ_TIMEOUT_SEC_PROPERTY = "sonar.ws.timeout";
  public static final String TOKEN_PROPERTY = "sonar.token";
  private static final String TOKEN_ENV_VARIABLE = "SONAR_TOKEN";
  static final int DEFAULT_READ_TIMEOUT_SEC = 60;
  public static final String SONAR_SCANNER_PROXY_PORT = "sonar.scanner.proxyPort";
  public static final String SONAR_SCANNER_CONNECT_TIMEOUT = "sonar.scanner.connectTimeout";
  public static final String SONAR_SCANNER_SOCKET_TIMEOUT = "sonar.scanner.socketTimeout";
  public static final String SONAR_SCANNER_RESPONSE_TIMEOUT = "sonar.scanner.responseTimeout";

  @Bean("DefaultScannerWsClient")
  public DefaultScannerWsClient provide(ScannerProperties scannerProps, EnvironmentInformation env, GlobalAnalysisMode globalMode,
    System2 system, AnalysisWarnings analysisWarnings) {
    String url = defaultIfBlank(scannerProps.property("sonar.host.url"), "http://localhost:9000");
    HttpConnector.Builder connectorBuilder = HttpConnector.newBuilder().acceptGzip(true);

    String oldSocketTimeout = defaultIfBlank(scannerProps.property(READ_TIMEOUT_SEC_PROPERTY), valueOf(DEFAULT_READ_TIMEOUT_SEC));
    String socketTimeout = defaultIfBlank(scannerProps.property(SONAR_SCANNER_SOCKET_TIMEOUT), oldSocketTimeout);
    String connectTimeout = defaultIfBlank(scannerProps.property(SONAR_SCANNER_CONNECT_TIMEOUT), valueOf(DEFAULT_CONNECT_TIMEOUT));
    String responseTimeout = defaultIfBlank(scannerProps.property(SONAR_SCANNER_RESPONSE_TIMEOUT), valueOf(DEFAULT_RESPONSE_TIMEOUT));
    String envVarToken = defaultIfBlank(system.envVariable(TOKEN_ENV_VARIABLE), null);
    String token = defaultIfBlank(scannerProps.property(TOKEN_PROPERTY), envVarToken);
    String login = defaultIfBlank(scannerProps.property(CoreProperties.LOGIN), token);
    connectorBuilder
      .readTimeoutMilliseconds(parseDurationProperty(socketTimeout, SONAR_SCANNER_SOCKET_TIMEOUT))
      .connectTimeoutMilliseconds(parseDurationProperty(connectTimeout, SONAR_SCANNER_CONNECT_TIMEOUT))
      .responseTimeoutMilliseconds(parseDurationProperty(responseTimeout, SONAR_SCANNER_RESPONSE_TIMEOUT))
      .userAgent(env.toString())
      .url(url)
      .credentials(login, scannerProps.property(CoreProperties.PASSWORD));

    // OkHttp detects 'http.proxyHost' java property already, so just focus on sonar properties
    String proxyHost = defaultIfBlank(scannerProps.property("sonar.scanner.proxyHost"), null);
    if (proxyHost != null) {
      String proxyPortStr = defaultIfBlank(scannerProps.property(SONAR_SCANNER_PROXY_PORT), url.startsWith("https") ? "443" : "80");
      var proxyPort = parseIntProperty(proxyPortStr, SONAR_SCANNER_PROXY_PORT);
      connectorBuilder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
    }

    var scannerProxyUser = scannerProps.property("sonar.scanner.proxyUser");
    String proxyUser = scannerProxyUser != null ? scannerProxyUser : system.properties().getProperty(HTTP_PROXY_USER, "");
    if (isNotBlank(proxyUser)) {
      var scannerProxyPwd = scannerProps.property("sonar.scanner.proxyPassword");
      String proxyPassword = scannerProxyPwd != null ? scannerProxyPwd : system.properties().getProperty(HTTP_PROXY_PASSWORD, "");
      connectorBuilder.proxyCredentials(proxyUser, proxyPassword);
    }

    return new DefaultScannerWsClient(WsClientFactories.getDefault().newClient(connectorBuilder.build()), login != null, globalMode, analysisWarnings);
  }

  private static int parseIntProperty(String propValue, String propKey) {
    try {
      return parseInt(propValue);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(propKey + " is not a valid integer: " + propValue, e);
    }
  }

  /**
   * For testing, we can accept timeouts that are smaller than a second, expressed using ISO-8601 format for durations.
   * If we can't parse as ISO-8601, then fallback to the official format that is simply the number of seconds
   */
  private static int parseDurationProperty(String propValue, String propKey) {
    try {
      return (int) Duration.parse(propValue).toMillis();
    } catch (DateTimeParseException e) {
      return parseIntProperty(propValue, propKey) * 1_000;
    }
  }
}
