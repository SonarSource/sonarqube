/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.wsclient.qualitygate.internal;

import java.net.HttpURLConnection;

import org.sonar.wsclient.qualitygate.QualityGates;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.wsclient.MockHttpServerInterceptor;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.qualitygate.QualityGate;
import org.sonar.wsclient.qualitygate.QualityGateClient;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;

public class DefaultQualityGateClientTest {

  @Rule
  public MockHttpServerInterceptor httpServer = new MockHttpServerInterceptor();

  @Test
  public void should_create_qualitygate() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubResponseBody("{\"id\":666,\"name\":\"Ninth\"}");

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);
    QualityGate result = client.create("Ninth");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/create");
    assertThat(httpServer.requestParams()).includes(
        entry("name", "Ninth")
        );
    assertThat(result).isNotNull();
  }

  @Test
  public void should_list_qualitygates() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubResponseBody(
        "{\"qualitygates\":[{\"id\":666,\"name\":\"Ninth\"},{\"id\":42,\"name\":\"Golden\"},{\"id\":43,\"name\":\"Star\"}]}");

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);
    QualityGates qGates = client.list();

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/list");
    assertThat(httpServer.requestParams()).isEmpty();
    assertThat(qGates.qualityGates()).hasSize(3);
    assertThat(qGates.defaultGate()).isNull();
  }

  @Test
  public void should_rename_qualitygate() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubResponseBody("{\"id\":666,\"name\":\"Ninth\"}");

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);
    QualityGate result = client.rename(666L, "Hell");

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/rename");
    assertThat(httpServer.requestParams()).includes(
        entry("id", "666"),
        entry("name", "Hell")
        );
    assertThat(result).isNotNull();
  }

  @Test
  public void should_destroy_qualitygate() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubStatusCode(HttpURLConnection.HTTP_NO_CONTENT);

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);
    client.destroy(666L);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/destroy");
    assertThat(httpServer.requestParams()).includes(
        entry("id", "666")
        );
  }

  @Test
  public void should_set_default_qualitygate() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubStatusCode(HttpURLConnection.HTTP_NO_CONTENT);

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);
    client.setDefault(666L);

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/set_as_default");
    assertThat(httpServer.requestParams()).includes(
        entry("id", "666")
        );
  }

  @Test
  public void should_unset_default_qualitygate() {
    HttpRequestFactory requestFactory = new HttpRequestFactory(httpServer.url());

    httpServer.stubStatusCode(HttpURLConnection.HTTP_NO_CONTENT);

    QualityGateClient client = new DefaultQualityGateClient(requestFactory);
    client.unsetDefault();

    assertThat(httpServer.requestedPath()).isEqualTo("/api/qualitygates/unset_default");
    assertThat(httpServer.requestParams()).isEmpty();
  }
}
