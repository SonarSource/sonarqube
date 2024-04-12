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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.utils.System2;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScannerWsClientProviderTest {

  public static final GlobalAnalysisMode GLOBAL_ANALYSIS_MODE = new GlobalAnalysisMode(new ScannerProperties(Collections.emptyMap()));
  public static final AnalysisWarnings ANALYSIS_WARNINGS = warning -> {
  };
  private final Map<String, String> scannerProps = new HashMap<>();

  private final ScannerWsClientProvider underTest = new ScannerWsClientProvider();
  private final EnvironmentInformation env = new EnvironmentInformation("Maven Plugin", "2.3");
  public static final String PROXY_AUTH_ENABLED = "proxy-auth";

  @RegisterExtension
  static WireMockExtension sonarqubeMock = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  @RegisterExtension
  static WireMockExtension proxyMock = WireMockExtension.newInstance()
    .options(wireMockConfig().dynamicPort())
    .build();

  private final System2 system2 = mock(System2.class);
  private final Properties systemProps = new Properties();

  @BeforeEach
  void configureMocks(TestInfo info) {
    when(system2.properties()).thenReturn(systemProps);

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
  void provide_client_with_default_settings() {
    DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS);

    assertThat(client).isNotNull();
    assertThat(client.baseUrl()).isEqualTo("http://localhost:9000/");
    HttpConnector httpConnector = (HttpConnector) client.wsConnector();
    assertThat(httpConnector.baseUrl()).isEqualTo("http://localhost:9000/");
    assertThat(httpConnector.okHttpClient().proxy()).isNull();
    assertThat(httpConnector.okHttpClient().connectTimeoutMillis()).isEqualTo(5_000);
    assertThat(httpConnector.okHttpClient().readTimeoutMillis()).isEqualTo(60_000);

    // Proxy is not accessed
    assertThat(proxyMock.findAllUnmatchedRequests()).isEmpty();
  }

  @Test
  void provide_client_with_custom_settings() {
    scannerProps.put("sonar.host.url", "https://here/sonarqube");
    scannerProps.put("sonar.token", "testToken");
    scannerProps.put("sonar.ws.timeout", "42");

    DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS);

    assertThat(client).isNotNull();
    HttpConnector httpConnector = (HttpConnector) client.wsConnector();
    assertThat(httpConnector.baseUrl()).isEqualTo("https://here/sonarqube/");
    assertThat(httpConnector.okHttpClient().proxy()).isNull();
  }

  @Test
  void it_should_honor_scanner_proxy_settings() {
    scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());
    scannerProps.put("sonar.scanner.proxyHost", "localhost");
    scannerProps.put("sonar.scanner.proxyPort", "" + proxyMock.getPort());

    DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS);

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
  void it_should_throw_if_invalid_proxy_port() {
    scannerProps.put("sonar.host.url", sonarqubeMock.baseUrl());
    scannerProps.put("sonar.scanner.proxyHost", "localhost");
    scannerProps.put("sonar.scanner.proxyPort", "not_a_number");
    var scannerPropertiesBean = new ScannerProperties(scannerProps);

    assertThrows(IllegalArgumentException.class, () -> underTest.provide(scannerPropertiesBean, env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS));
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

    DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS);

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

    DefaultScannerWsClient client = underTest.provide(new ScannerProperties(scannerProps), env, GLOBAL_ANALYSIS_MODE, system2, ANALYSIS_WARNINGS);

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
