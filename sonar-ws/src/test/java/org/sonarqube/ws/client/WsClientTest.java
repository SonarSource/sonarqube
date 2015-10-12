/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonarqube.ws.client;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonarqube.ws.WsComponents;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;

public class WsClientTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  MockHttpServer server;

  WsClient underTest;

  @Before
  public void setUp() throws Exception {
    server = new MockHttpServer();
    server.start();

    underTest = WsClient.create("http://localhost:" + server.getPort());
  }

  @After
  public void stopServer() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void return_protobuf_response() throws Exception {
    server.doReturnBody(
      WsComponents.WsSearchResponse
        .newBuilder()
        .addComponents(WsComponents.WsSearchResponse.Component.getDefaultInstance())
        .build()
        .toByteArray());
    server.doReturnStatus(HTTP_OK);
    server.doReturnContentType(MediaType.PROTOBUF.toString());

    WsComponents.WsSearchResponse response = underTest.execute(
      new WsRequest("api/components/search").setMediaType(WsRequest.MediaType.PROTOBUF),
      WsComponents.WsSearchResponse.PARSER);

    assertThat(response.getComponentsCount()).isEqualTo(1);
    assertThat(server.requestHeaders().get(HttpHeaders.ACCEPT)).isEqualTo(MediaType.PROTOBUF.toString());
  }

  @Test
  public void return_json_response() throws Exception {
    String expectedResponse = "{\"key\":value}";
    server.doReturnBody(expectedResponse);
    server.doReturnStatus(HTTP_OK);
    server.doReturnContentType(MediaType.JSON_UTF_8.toString());

    String response = underTest.execute(new WsRequest("api/components/search"));

    assertThat(response).isEqualTo(expectedResponse);
    assertThat(server.requestHeaders().get(HttpHeaders.ACCEPT)).isEqualTo(MediaType.JSON_UTF_8.toString());
  }

  @Test
  public void url_should_not_be_null() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Server URL must be set");

    underTest.builder().build();
  }

  @Test
  public void url_should_not_be_empty() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Server URL must be set");

    underTest.create("");
  }

  @Test
  public void test_default_configuration() throws Exception {
    underTest = WsClient.create("http://localhost:9000");
    assertThat(underTest.requestFactory.getBaseUrl()).isEqualTo("http://localhost:9000");
    assertThat(underTest.requestFactory.getLogin()).isNull();
    assertThat(underTest.requestFactory.getPassword()).isNull();
    assertThat(underTest.requestFactory.getConnectTimeoutInMilliseconds()).isEqualTo(WsClient.DEFAULT_CONNECT_TIMEOUT_MILLISECONDS);
    assertThat(underTest.requestFactory.getReadTimeoutInMilliseconds()).isEqualTo(WsClient.DEFAULT_READ_TIMEOUT_MILLISECONDS);
    assertThat(underTest.requestFactory.getProxyHost()).isNull();
    assertThat(underTest.requestFactory.getProxyPort()).isEqualTo(0);
    assertThat(underTest.requestFactory.getProxyLogin()).isNull();
    assertThat(underTest.requestFactory.getProxyPassword()).isNull();
  }

  @Test
  public void test_custom_configuration() throws Exception {
    underTest = WsClient.builder().url("http://localhost:9000")
      .login("eric")
      .password("pass")
      .connectTimeoutMilliseconds(12345)
      .readTimeoutMilliseconds(6789)
      .proxy("localhost", 2052)
      .proxyLogin("proxyLogin")
      .proxyPassword("proxyPass")
      .build();
    assertThat(underTest.requestFactory.getBaseUrl()).isEqualTo("http://localhost:9000");
    assertThat(underTest.requestFactory.getLogin()).isEqualTo("eric");
    assertThat(underTest.requestFactory.getPassword()).isEqualTo("pass");
    assertThat(underTest.requestFactory.getConnectTimeoutInMilliseconds()).isEqualTo(12345);
    assertThat(underTest.requestFactory.getReadTimeoutInMilliseconds()).isEqualTo(6789);
    assertThat(underTest.requestFactory.getProxyHost()).isEqualTo("localhost");
    assertThat(underTest.requestFactory.getProxyPort()).isEqualTo(2052);
    assertThat(underTest.requestFactory.getProxyLogin()).isEqualTo("proxyLogin");
    assertThat(underTest.requestFactory.getProxyPassword()).isEqualTo("proxyPass");
  }
}
