/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.tests.settings;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.http.HttpClient;
import com.sonar.orchestrator.http.HttpResponse;
import java.net.InetAddress;
import okhttp3.HttpUrl;
import org.junit.Test;
import org.sonar.NetworkUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class ElasticsearchSettingsTest {

  @Test
  public void set_http_port_through_sonar_properties() throws Exception {
    int port = NetworkUtils.getNextAvailablePort(InetAddress.getLoopbackAddress());
    Orchestrator orchestrator = Orchestrator
      .builderEnv()
      .setServerProperty("sonar.search.httpPort", "" + port)
      .setServerProperty("sonar.search.host", InetAddress.getLoopbackAddress().getHostAddress())
      .build();

    orchestrator.start();

    try {
      HttpClient httpClient = new HttpClient.Builder().build();
      HttpUrl url = new HttpUrl.Builder()
        .scheme("http")
        .host(InetAddress.getLoopbackAddress().getHostAddress())
        .port(port)
        .addEncodedPathSegments("_cluster/state")
        .build();
      HttpResponse response = httpClient.newCall(url).execute();
      assertThat(response.isSuccessful()).isTrue();
    } finally {
      orchestrator.stop();
    }
  }
}
