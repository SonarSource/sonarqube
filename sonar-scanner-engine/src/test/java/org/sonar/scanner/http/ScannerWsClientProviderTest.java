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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import nl.altindag.ssl.exception.GenericKeyStoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junitpioneer.jupiter.RestoreSystemProperties;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.System2;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonar.scanner.bootstrap.GlobalAnalysisMode;
import org.sonar.scanner.bootstrap.ScannerProperties;
import org.sonar.scanner.bootstrap.SonarUserHome;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.HttpConnector;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScannerWsClientProviderTest {

  private static final GlobalAnalysisMode GLOBAL_ANALYSIS_MODE = new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap()));
  private static final AnalysisWarnings ANALYSIS_WARNINGS = warning -> {
  };
  private SonarUserHome sonarUserHome = mock(SonarUserHome.class);
  private final Map<String, String> scannerProps = new HashMap<>();

  private final ScannerWsClientProvider underTest = new ScannerWsClientProvider();
  private final EnvironmentInformation env = new EnvironmentInformation("Maven Plugin", "2.3");

  private final System2 system2 = mock(System2.class);
  private final Properties systemProps = new Properties();

  @BeforeEach
  void configureMocks(@TempDir Path sonarUserHomeDir) {
    when(system2.properties()).thenReturn(systemProps);
    sonarUserHome = new SonarUserHome(sonarUserHomeDir);
  }

  @Test
  void provide_client_with_default_settings() {
    DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome);

    assertThat(client).isNotNull();
    assertThat(client.baseUrl()).isEqualTo("http://localhost:9000/");
    HttpConnector httpConnector = (HttpConnector) client.wsConnector();
    assertThat(httpConnector.baseUrl()).isEqualTo("http://localhost:9000/");
    assertThat(httpConnector.okHttpClient().proxy()).isNull();
    assertThat(httpConnector.okHttpClient().connectTimeoutMillis()).isEqualTo(5_000);
    assertThat(httpConnector.okHttpClient().readTimeoutMillis()).isEqualTo(60_000);
  }

  @Test
  void provide_client_with_custom_settings() {
    scannerProps.put("sonar.host.url", "https://here/sonarqube");
    scannerProps.put("sonar.token", "testToken");
    scannerProps.put("sonar.ws.timeout", "42");

    DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome);

    assertThat(client).isNotNull();
    HttpConnector httpConnector = (HttpConnector) client.wsConnector();
    assertThat(httpConnector.baseUrl()).isEqualTo("https://here/sonarqube/");
    assertThat(httpConnector.okHttpClient().proxy()).isNull();
  }

  @ParameterizedTest
  @CsvSource({
    "keystore_changeit.p12, wrong,        false",
    "keystore_changeit.p12, changeit,     true",
    "keystore_changeit.p12,,              true",
    "keystore_sonar.p12,    wrong,        false",
    "keystore_sonar.p12,    sonar,        true",
    "keystore_sonar.p12,,                 true",
    "keystore_anotherpwd.p12, wrong,      false",
    "keystore_anotherpwd.p12, anotherpwd, true",
    "keystore_anotherpwd.p12,,            false"})
  void it_should_fail_if_invalid_truststore_password(String keystore, @Nullable String password, boolean shouldSucceed) {
    scannerProps.put("sonar.scanner.truststorePath", toPath(requireNonNull(ScannerWsClientProviderTest.class.getResource("/ssl/" + keystore))).toString());
    if (password != null) {
      scannerProps.put("sonar.scanner.truststorePassword", password);
    }

    var scannerPropsObj = new ScannerProperties(scannerProps);
    if (shouldSucceed) {
      assertThatNoException().isThrownBy(() -> underTest.provide(scannerPropsObj, env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome));
    } else {
      assertThatThrownBy(() -> underTest.provide(scannerPropsObj, env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome))
        .isInstanceOf(GenericKeyStoreException.class)
        .hasMessageContaining("Unable to read truststore from")
        .hasStackTraceContaining("wrong password or corrupted file");
    }
  }

  @ParameterizedTest
  @CsvSource({
    "keystore_changeit.p12, wrong,        false",
    "keystore_changeit.p12, changeit,     true",
    "keystore_changeit.p12,,              true",
    "keystore_sonar.p12,    wrong,        false",
    "keystore_sonar.p12,    sonar,        true",
    "keystore_sonar.p12,,                 true",
    "keystore_anotherpwd.p12, wrong,      false",
    "keystore_anotherpwd.p12, anotherpwd, true",
    "keystore_anotherpwd.p12,,            false"})
  void it_should_fail_if_invalid_keystore_password(String keystore, @Nullable String password, boolean shouldSucceed) {
    scannerProps.put("sonar.scanner.keystorePath", toPath(requireNonNull(ScannerWsClientProviderTest.class.getResource("/ssl/" + keystore))).toString());
    if (password != null) {
      scannerProps.put("sonar.scanner.keystorePassword", password);
    }

    var scannerPropsObj = new ScannerProperties(scannerProps);
    if (shouldSucceed) {
      assertThatNoException().isThrownBy(() -> underTest.provide(scannerPropsObj, env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome));
    } else {
      assertThatThrownBy(() -> underTest.provide(scannerPropsObj, env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome))
        .isInstanceOf(GenericKeyStoreException.class)
        .hasMessageContaining("keystore password was incorrect");
    }
  }

  @Nested
  class WithMockHttpSonarQube {

    @RegisterExtension
    static WireMockExtension sonarqubeMock = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicPort())
      .build();

    @Test
    void it_should_timeout_on_long_response() {
      scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());
      scannerProps.put("sonar.scanner.responseTimeout", "PT0.2S");

      DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome);

      sonarqubeMock.stubFor(get("/api/plugins/installed")
        .willReturn(aResponse().withStatus(200)
          .withFixedDelay(2000)
          .withBody("Success")));

      HttpConnector httpConnector = (HttpConnector) client.wsConnector();

      var getRequest = new GetRequest("api/plugins/installed");
      var thrown = assertThrows(IllegalStateException.class, () -> httpConnector.call(getRequest));

      assertThat(thrown).hasStackTraceContaining("timeout");
    }

    @Test
    void it_should_timeout_on_slow_response() {
      scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());
      scannerProps.put("sonar.scanner.socketTimeout", "PT0.2S");

      DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome);

      sonarqubeMock.stubFor(get("/api/plugins/installed")
        .willReturn(aResponse().withStatus(200)
          .withChunkedDribbleDelay(2, 2000)
          .withBody("Success")));

      HttpConnector httpConnector = (HttpConnector) client.wsConnector();

      var getRequest = new GetRequest("api/plugins/installed");
      var thrown = assertThrows(IllegalStateException.class, () -> httpConnector.call(getRequest));

      assertThat(thrown).hasStackTraceContaining("timeout");
    }

    @Test
    void it_should_throw_if_invalid_proxy_port() {
      scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());
      scannerProps.put("sonar.scanner.proxyHost", "localhost");
      scannerProps.put("sonar.scanner.proxyPort", "not_a_number");
      var scannerPropertiesBean = new ScannerProperties(scannerProps);

      assertThrows(IllegalArgumentException.class, () -> underTest.provide(scannerPropertiesBean, env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome));
    }

    @Nested
    class WithProxy {

      private static final String PROXY_AUTH_ENABLED = "proxy-auth";

      @RegisterExtension
      static WireMockExtension proxyMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

      @BeforeEach
      void configureMocks(TestInfo info) {
        if (info.getTags().contains(PROXY_AUTH_ENABLED)) {
          proxyMock.stubFor(get(urlMatching("/api/plugins/.*"))
            .inScenario("Proxy Auth")
            .whenScenarioStateIs(STARTED)
            .willReturn(aResponse()
              .withStatus(407)
              .withHeader("Proxy-Authenticate", "Basic realm=\"Access to the proxy\""))
            .willSetStateTo("Challenge returned"));
          proxyMock.stubFor(get(urlMatching("/api/plugins/.*"))
            .inScenario("Proxy Auth")
            .whenScenarioStateIs("Challenge returned")
            .willReturn(aResponse().proxiedFrom(sonarqubeMock.baseUrl())));
        } else {
          proxyMock.stubFor(get(urlMatching("/api/plugins/.*")).willReturn(aResponse().proxiedFrom(sonarqubeMock.baseUrl())));
        }
      }

      @Test
      void it_should_honor_scanner_proxy_settings() {
        scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());
        scannerProps.put("sonar.scanner.proxyHost", "localhost");
        scannerProps.put("sonar.scanner.proxyPort", "" + proxyMock.getPort());

        DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome);

        sonarqubeMock.stubFor(get("/api/plugins/installed")
          .willReturn(aResponse().withStatus(200).withBody("Success")));

        HttpConnector httpConnector = (HttpConnector) client.wsConnector();
        try (var r = httpConnector.call(new GetRequest("api/plugins/installed"))) {
          assertThat(r.code()).isEqualTo(200);
          assertThat(r.content()).isEqualTo("Success");
        }

        proxyMock.verify(getRequestedFor(urlEqualTo("/api/plugins/installed")));
      }

      @Test
      @Tag(PROXY_AUTH_ENABLED)
      void it_should_honor_scanner_proxy_settings_with_auth() {
        var proxyLogin = "proxyLogin";
        var proxyPassword = "proxyPassword";
        scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());
        scannerProps.put("sonar.scanner.proxyHost", "localhost");
        scannerProps.put("sonar.scanner.proxyPort", "" + proxyMock.getPort());
        scannerProps.put("sonar.scanner.proxyUser", proxyLogin);
        scannerProps.put("sonar.scanner.proxyPassword", proxyPassword);

        DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome);

        sonarqubeMock.stubFor(get("/api/plugins/installed")
          .willReturn(aResponse().withStatus(200).withBody("Success")));

        HttpConnector httpConnector = (HttpConnector) client.wsConnector();
        try (var r = httpConnector.call(new GetRequest("api/plugins/installed"))) {
          assertThat(r.code()).isEqualTo(200);
          assertThat(r.content()).isEqualTo("Success");
        }

        proxyMock.verify(getRequestedFor(urlEqualTo("/api/plugins/installed"))
          .withHeader("Proxy-Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((proxyLogin + ":" + proxyPassword).getBytes(StandardCharsets.UTF_8)))));

      }

      @Test
      @Tag(PROXY_AUTH_ENABLED)
      void it_should_honor_old_jvm_proxy_auth_properties() {
        var proxyLogin = "proxyLogin";
        var proxyPassword = "proxyPassword";
        scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());
        scannerProps.put("sonar.scanner.proxyHost", "localhost");
        scannerProps.put("sonar.scanner.proxyPort", "" + proxyMock.getPort());
        systemProps.put("http.proxyUser", proxyLogin);
        systemProps.put("http.proxyPassword", proxyPassword);

        DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome);

        sonarqubeMock.stubFor(get("/api/plugins/installed")
          .willReturn(aResponse().withStatus(200).withBody("Success")));

        HttpConnector httpConnector = (HttpConnector) client.wsConnector();
        try (var r = httpConnector.call(new GetRequest("api/plugins/installed"))) {
          assertThat(r.code()).isEqualTo(200);
          assertThat(r.content()).isEqualTo("Success");
        }

        proxyMock.verify(getRequestedFor(urlEqualTo("/api/plugins/installed"))
          .withHeader("Proxy-Authorization", equalTo("Basic " + Base64.getEncoder().encodeToString((proxyLogin + ":" + proxyPassword).getBytes(StandardCharsets.UTF_8)))));

      }
    }

  }

  @Nested
  class WithMockHttpsSonarQube {

    public static final String KEYSTORE_PWD = "pwdServerP12";

    @RegisterExtension
    static WireMockExtension sonarqubeMock = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicHttpsPort().httpDisabled(true)
        .keystoreType("pkcs12")
        .keystorePath(toPath(requireNonNull(ScannerWsClientProviderTest.class.getResource("/ssl/server.p12"))).toString())
        .keystorePassword(KEYSTORE_PWD)
        .keyManagerPassword(KEYSTORE_PWD))
      .build();

    @BeforeEach
    void mockResponse() {
      sonarqubeMock.stubFor(get("/api/plugins/installed")
        .willReturn(aResponse().withStatus(200).withBody("Success")));
    }

    @Test
    void it_should_not_trust_server_self_signed_certificate_by_default() {
      scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());

      DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome);

      HttpConnector httpConnector = (HttpConnector) client.wsConnector();
      var getRequest = new GetRequest("api/plugins/installed");
      var thrown = assertThrows(IllegalStateException.class, () -> httpConnector.call(getRequest));

      assertThat(thrown).hasStackTraceContaining("CertificateException");
    }

    @Test
    void it_should_trust_server_self_signed_certificate_when_certificate_is_in_truststore() {
      scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());
      scannerProps.put("sonar.scanner.truststorePath", toPath(requireNonNull(ScannerWsClientProviderTest.class.getResource("/ssl/client-truststore.p12"))).toString());
      scannerProps.put("sonar.scanner.truststorePassword", "pwdClientWithServerCA");

      DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome);

      HttpConnector httpConnector = (HttpConnector) client.wsConnector();
      try (var r = httpConnector.call(new GetRequest("api/plugins/installed"))) {
        assertThat(r.code()).isEqualTo(200);
        assertThat(r.content()).isEqualTo("Success");
      }
    }
  }

  @Nested
  class WithMockHttpsSonarQubeAndClientCertificates {

    public static final String KEYSTORE_PWD = "pwdServerP12";

    @RegisterExtension
    static WireMockExtension sonarqubeMock = WireMockExtension.newInstance()
      .options(wireMockConfig().dynamicHttpsPort().httpDisabled(true)
        .keystoreType("pkcs12")
        .keystorePath(toPath(requireNonNull(ScannerWsClientProviderTest.class.getResource("/ssl/server.p12"))).toString())
        .keystorePassword(KEYSTORE_PWD)
        .keyManagerPassword(KEYSTORE_PWD)
        .needClientAuth(true)
        .trustStoreType("pkcs12")
        .trustStorePath(toPath(requireNonNull(ScannerWsClientProviderTest.class.getResource("/ssl/server-with-client-ca.p12"))).toString())
        .trustStorePassword("pwdServerWithClientCA"))
      .build();

    @BeforeEach
    void mockResponse() {
      sonarqubeMock.stubFor(get("/api/plugins/installed")
        .willReturn(aResponse().withStatus(200).withBody("Success")));
    }

    @Test
    void it_should_fail_if_client_certificate_not_provided() {
      scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());
      scannerProps.put("sonar.scanner.truststorePath", toPath(requireNonNull(ScannerWsClientProviderTest.class.getResource("/ssl/client-truststore.p12"))).toString());
      scannerProps.put("sonar.scanner.truststorePassword", "pwdClientWithServerCA");

      DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome);

      HttpConnector httpConnector = (HttpConnector) client.wsConnector();
      var getRequest = new GetRequest("api/plugins/installed");
      var thrown = assertThrows(IllegalStateException.class, () -> httpConnector.call(getRequest));

      assertThat(thrown).satisfiesAnyOf(
        e -> assertThat(e).hasStackTraceContaining("SSLHandshakeException"),
        // Exception is flaky because of https://bugs.openjdk.org/browse/JDK-8172163
        e -> assertThat(e).hasStackTraceContaining("Broken pipe"));
    }

    @Test
    void it_should_authenticate_using_certificate_in_keystore() {
      scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());

      scannerProps.put("sonar.scanner.truststorePath", toPath(requireNonNull(ScannerWsClientProviderTest.class.getResource("/ssl/client-truststore.p12"))).toString());
      scannerProps.put("sonar.scanner.truststorePassword", "pwdClientWithServerCA");
      scannerProps.put("sonar.scanner.keystorePath", toPath(requireNonNull(ScannerWsClientProviderTest.class.getResource("/ssl/client.p12"))).toString());
      scannerProps.put("sonar.scanner.keystorePassword", "pwdClientCertP12");

      DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome);

      HttpConnector httpConnector = (HttpConnector) client.wsConnector();
      try (var r = httpConnector.call(new GetRequest("api/plugins/installed"))) {
        assertThat(r.code()).isEqualTo(200);
        assertThat(r.content()).isEqualTo("Success");
      }
    }

    @RestoreSystemProperties
    @Test
    void it_should_support_jvm_system_properties() {
      scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());
      System.setProperty("javax.net.ssl.trustStore", toPath(requireNonNull(ScannerWsClientProviderTest.class.getResource("/ssl/client-truststore.p12"))).toString());
      System.setProperty("javax.net.ssl.trustStorePassword", "pwdClientWithServerCA");
      System.setProperty("javax.net.ssl.keyStore", toPath(requireNonNull(ScannerWsClientProviderTest.class.getResource("/ssl/client.p12"))).toString());
      systemProps.setProperty("javax.net.ssl.keyStore", "any value is fine here");
      System.setProperty("javax.net.ssl.keyStorePassword", "pwdClientCertP12");

      DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS, sonarUserHome);

      HttpConnector httpConnector = (HttpConnector) client.wsConnector();
      try (var r = httpConnector.call(new GetRequest("api/plugins/installed"))) {
        assertThat(r.code()).isEqualTo(200);
        assertThat(r.content()).isEqualTo("Success");
      }
    }
  }

  private static Path toPath(URL url) {
    try {
      return Paths.get(url.toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }
}
