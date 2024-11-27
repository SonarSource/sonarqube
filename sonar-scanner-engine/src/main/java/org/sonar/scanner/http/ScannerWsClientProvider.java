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
package org.sonar.scanner.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import javax.annotation.Nullable;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.exception.GenericKeyStoreException;
import nl.altindag.ssl.util.KeyStoreUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.System2;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.bootstrap.ScannerProperties;
import org.sonar.scanner.bootstrap.SonarUserHome;
import org.sonar.scanner.http.ssl.CertificateStore;
import org.sonar.scanner.http.ssl.SslConfig;
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

  private static final Logger LOG = LoggerFactory.getLogger(ScannerWsClientProvider.class);

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
  public static final String SKIP_SYSTEM_TRUST_MATERIAL = "sonar.scanner.skipSystemTruststore";

  @Bean("DefaultScannerWsClient")
  public DefaultScannerWsClient provide(ScannerProperties scannerProps, EnvironmentInformation env, GlobalAnalysisMode globalMode,
    System2 system, AnalysisWarnings analysisWarnings, SonarUserHome sonarUserHome) {
    String url = defaultIfBlank(scannerProps.property("sonar.host.url"), "http://localhost:9000");
    HttpConnector.Builder connectorBuilder = HttpConnector.newBuilder().acceptGzip(true);

    String oldSocketTimeout = defaultIfBlank(scannerProps.property(READ_TIMEOUT_SEC_PROPERTY), valueOf(DEFAULT_READ_TIMEOUT_SEC));
    String socketTimeout = defaultIfBlank(scannerProps.property(SONAR_SCANNER_SOCKET_TIMEOUT), oldSocketTimeout);
    String connectTimeout = defaultIfBlank(scannerProps.property(SONAR_SCANNER_CONNECT_TIMEOUT), valueOf(DEFAULT_CONNECT_TIMEOUT));
    String responseTimeout = defaultIfBlank(scannerProps.property(SONAR_SCANNER_RESPONSE_TIMEOUT), valueOf(DEFAULT_RESPONSE_TIMEOUT));
    String envVarToken = defaultIfBlank(system.envVariable(TOKEN_ENV_VARIABLE), null);
    String token = defaultIfBlank(scannerProps.property(TOKEN_PROPERTY), envVarToken);
    String login = defaultIfBlank(scannerProps.property(CoreProperties.LOGIN), token);
    boolean skipSystemTrustMaterial = Boolean.parseBoolean(defaultIfBlank(scannerProps.property(SKIP_SYSTEM_TRUST_MATERIAL), "false"));
    var sslContext = configureSsl(parseSslConfig(scannerProps, sonarUserHome), system, skipSystemTrustMaterial);
    connectorBuilder
      .readTimeoutMilliseconds(parseDurationProperty(socketTimeout, SONAR_SCANNER_SOCKET_TIMEOUT))
      .connectTimeoutMilliseconds(parseDurationProperty(connectTimeout, SONAR_SCANNER_CONNECT_TIMEOUT))
      .responseTimeoutMilliseconds(parseDurationProperty(responseTimeout, SONAR_SCANNER_RESPONSE_TIMEOUT))
      .userAgent(env.toString())
      .url(url)
      .credentials(login, scannerProps.property(CoreProperties.PASSWORD))
      .setSSLSocketFactory(sslContext.getSslSocketFactory())
      .setTrustManager(sslContext.getTrustManager().orElseThrow());

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

  private static SslConfig parseSslConfig(ScannerProperties scannerProperties, SonarUserHome sonarUserHome) {
    var keyStorePath = defaultIfBlank(scannerProperties.property("sonar.scanner.keystorePath"), sonarUserHome.getPath().resolve("ssl/keystore.p12").toString());
    var keyStorePassword = scannerProperties.property("sonar.scanner.keystorePassword");
    var keyStore = new CertificateStore(Path.of(keyStorePath), keyStorePassword);
    var trustStorePath = defaultIfBlank(scannerProperties.property("sonar.scanner.truststorePath"), sonarUserHome.getPath().resolve("ssl/truststore.p12").toString());
    var trustStorePassword = scannerProperties.property("sonar.scanner.truststorePassword");
    var trustStore = new CertificateStore(Path.of(trustStorePath), trustStorePassword);
    return new SslConfig(keyStore, trustStore);
  }

  private static SSLFactory configureSsl(SslConfig sslConfig, System2 system2, boolean skipSystemTrustMaterial) {
    var sslFactoryBuilder = SSLFactory.builder()
      .withDefaultTrustMaterial();
    if (!skipSystemTrustMaterial) {
      LOG.debug("Loading OS trusted SSL certificates...");
      LOG.debug("This operation might be slow or even get stuck. You can skip it by passing the scanner property '{}=true'", SKIP_SYSTEM_TRUST_MATERIAL);
      sslFactoryBuilder.withSystemTrustMaterial();
    }
    if (system2.properties().containsKey("javax.net.ssl.keyStore")) {
      sslFactoryBuilder.withSystemPropertyDerivedIdentityMaterial();
    }
    var keyStoreConfig = sslConfig.getKeyStore();
    if (keyStoreConfig != null && Files.exists(keyStoreConfig.getPath())) {
      keyStoreConfig.getKeyStorePassword()
        .ifPresentOrElse(
          password -> sslFactoryBuilder.withIdentityMaterial(keyStoreConfig.getPath(), password.toCharArray(), keyStoreConfig.getKeyStoreType()),
          () -> loadIdentityMaterialWithDefaultPassword(sslFactoryBuilder, keyStoreConfig.getPath()));
    }
    var trustStoreConfig = sslConfig.getTrustStore();
    if (trustStoreConfig != null && Files.exists(trustStoreConfig.getPath())) {
      KeyStore trustStore;
      try {
        trustStore = loadTrustStoreWithBouncyCastle(
          trustStoreConfig.getPath(),
          trustStoreConfig.getKeyStorePassword().orElse(null),
          trustStoreConfig.getKeyStoreType());
        LOG.debug("Loaded truststore from '{}' containing {} certificates", trustStoreConfig.getPath(), trustStore.size());
      } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e) {
        throw new GenericKeyStoreException("Unable to read truststore from '" + trustStoreConfig.getPath() + "'", e);
      }
      sslFactoryBuilder.withTrustMaterial(trustStore);
    }
    return sslFactoryBuilder.build();
  }

  private static void loadIdentityMaterialWithDefaultPassword(SSLFactory.Builder sslFactoryBuilder, Path path) {
    try {
      var keystore = KeyStoreUtils.loadKeyStore(path, CertificateStore.DEFAULT_PASSWORD.toCharArray(), CertificateStore.DEFAULT_STORE_TYPE);
      sslFactoryBuilder.withIdentityMaterial(keystore, CertificateStore.DEFAULT_PASSWORD.toCharArray());
    } catch (GenericKeyStoreException e) {
      var keystore = KeyStoreUtils.loadKeyStore(path, CertificateStore.OLD_DEFAULT_PASSWORD.toCharArray(), CertificateStore.DEFAULT_STORE_TYPE);
      LOG.warn("Using deprecated default password for keystore '{}'.", path);
      sslFactoryBuilder.withIdentityMaterial(keystore, CertificateStore.OLD_DEFAULT_PASSWORD.toCharArray());
    }
  }

  static KeyStore loadTrustStoreWithBouncyCastle(Path keystorePath, @Nullable String keystorePassword, String keystoreType) throws IOException,
    KeyStoreException, CertificateException, NoSuchAlgorithmException {
    KeyStore keystore = KeyStore.getInstance(keystoreType, new BouncyCastleProvider());
    if (keystorePassword != null) {
      loadKeyStoreWithPassword(keystorePath, keystore, keystorePassword);
    } else {
      try {
        loadKeyStoreWithPassword(keystorePath, keystore, CertificateStore.DEFAULT_PASSWORD);
      } catch (Exception e) {
        loadKeyStoreWithPassword(keystorePath, keystore, CertificateStore.OLD_DEFAULT_PASSWORD);
        LOG.warn("Using deprecated default password for truststore '{}'.", keystorePath);
      }
    }
    return keystore;
  }

  private static void loadKeyStoreWithPassword(Path keystorePath, KeyStore keystore, String oldDefaultPassword) throws IOException, NoSuchAlgorithmException, CertificateException {
    try (InputStream keystoreInputStream = Files.newInputStream(keystorePath, StandardOpenOption.READ)) {
      keystore.load(keystoreInputStream, oldDefaultPassword.toCharArray());
    }
  }

}
