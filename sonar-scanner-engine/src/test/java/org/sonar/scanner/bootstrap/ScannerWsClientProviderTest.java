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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonarqube.ws.client.HttpConnector;

import static org.assertj.core.api.Assertions.assertThat;

public class ScannerWsClientProviderTest {

  private ScannerWsClientProvider underTest = new ScannerWsClientProvider();
  private EnvironmentInformation env = new EnvironmentInformation("Maven Plugin", "2.3");

  @Test
  public void provide_client_with_default_settings() {
    GlobalProperties settings = new GlobalProperties(new HashMap<>());

    ScannerWsClient client = underTest.provide(settings, env, new GlobalAnalysisMode(new GlobalProperties(Collections.emptyMap())));

    assertThat(client).isNotNull();
    assertThat(client.baseUrl()).isEqualTo("http://localhost:9000/");
    HttpConnector httpConnector = (HttpConnector) client.wsConnector();
    assertThat(httpConnector.baseUrl()).isEqualTo("http://localhost:9000/");
    assertThat(httpConnector.okHttpClient().proxy()).isNull();
    assertThat(httpConnector.okHttpClient().connectTimeoutMillis()).isEqualTo(5_000);
    assertThat(httpConnector.okHttpClient().readTimeoutMillis()).isEqualTo(60_000);
  }

  @Test
  public void provide_client_with_custom_settings() {
    Map<String, String> props = new HashMap<>();
    props.put("sonar.host.url", "https://here/sonarqube");
    props.put("sonar.login", "theLogin");
    props.put("sonar.password", "thePassword");
    props.put("sonar.ws.timeout", "42");
    GlobalProperties settings = new GlobalProperties(props);

    ScannerWsClient client = underTest.provide(settings, env, new GlobalAnalysisMode(new GlobalProperties(Collections.emptyMap())));

    assertThat(client).isNotNull();
    HttpConnector httpConnector = (HttpConnector) client.wsConnector();
    assertThat(httpConnector.baseUrl()).isEqualTo("https://here/sonarqube/");
    assertThat(httpConnector.okHttpClient().proxy()).isNull();
  }

  @Test
  public void build_singleton() {
    GlobalProperties settings = new GlobalProperties(new HashMap<>());
    ScannerWsClient first = underTest.provide(settings, env, new GlobalAnalysisMode(new GlobalProperties(Collections.emptyMap())));
    ScannerWsClient second = underTest.provide(settings, env, new GlobalAnalysisMode(new GlobalProperties(Collections.emptyMap())));
    assertThat(first).isSameAs(second);
  }
}
