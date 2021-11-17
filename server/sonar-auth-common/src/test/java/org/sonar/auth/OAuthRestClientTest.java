/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.auth;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.oauth.OAuth20Service;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.auth.OAuthRestClient.executePaginatedRequest;
import static org.sonar.auth.OAuthRestClient.executeRequest;

public class OAuthRestClientTest {
  @Rule
  public MockWebServer mockWebServer = new MockWebServer();

  private OAuth2AccessToken auth2AccessToken = mock(OAuth2AccessToken.class);

  private String serverUrl;

  private OAuth20Service oAuth20Service = new ServiceBuilder("API_KEY")
    .apiSecret("API_SECRET")
    .callback("CALLBACK")
    .build(new TestAPI());

  @Before
  public void setUp() {
    this.serverUrl = format("http://%s:%d", mockWebServer.getHostName(), mockWebServer.getPort());
  }

  @Test
  public void execute_request() throws IOException {
    String body = randomAlphanumeric(10);
    mockWebServer.enqueue(new MockResponse().setBody(body));

    Response response = executeRequest(serverUrl + "/test", oAuth20Service, auth2AccessToken);

    assertThat(response.getBody()).isEqualTo(body);
  }

  @Test
  public void fail_to_execute_request() throws IOException {
    mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("Error!"));

    assertThatThrownBy(() -> executeRequest(serverUrl + "/test", oAuth20Service, auth2AccessToken))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(format("Fail to execute request '%s/test'. HTTP code: 404, response: Error!", serverUrl));
  }

  @Test
  public void execute_paginated_request() {
    mockWebServer.enqueue(new MockResponse()
      .setHeader("Link", "<" + serverUrl + "/test?per_page=100&page=2>; rel=\"next\", <" + serverUrl + "/test?per_page=100&page=2>; rel=\"last\"")
      .setBody("A"));
    mockWebServer.enqueue(new MockResponse()
      .setHeader("Link", "<" + serverUrl + "/test?per_page=100&page=1>; rel=\"prev\", <" + serverUrl + "/test?per_page=100&page=1>; rel=\"first\"")
      .setBody("B"));

    List<String> response = executePaginatedRequest(serverUrl + "/test", oAuth20Service, auth2AccessToken, Arrays::asList);

    assertThat(response).contains("A", "B");
  }

  @Test
  public void execute_paginated_request_with_query_parameter() throws InterruptedException {
    mockWebServer.enqueue(new MockResponse()
      .setHeader("Link", "<" + serverUrl + "/test?param=value&per_page=100&page=2>; rel=\"next\", <" + serverUrl + "/test?param=value&per_page=100&page=2>; rel=\"last\"")
      .setBody("A"));
    mockWebServer.enqueue(new MockResponse()
      .setHeader("Link", "<" + serverUrl + "/test?param=value&per_page=100&page=1>; rel=\"prev\", <" + serverUrl + "/test?param=value&per_page=100&page=1>; rel=\"first\"")
      .setBody("B"));

    List<String> response = executePaginatedRequest(serverUrl + "/test?param=value", oAuth20Service, auth2AccessToken, Arrays::asList);

    assertThat(response).contains("A", "B");

    assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/test?param=value&per_page=100");
    assertThat(mockWebServer.takeRequest().getPath()).isEqualTo("/test?param=value&per_page=100&page=2");
  }

  @Test
  public void execute_paginated_request_case_insensitive_headers() {
    mockWebServer.enqueue(new MockResponse()
      .setHeader("link", "<" + serverUrl + "/test?per_page=100&page=2>; rel=\"next\", <" + serverUrl + "/test?per_page=100&page=2>; rel=\"last\"")
      .setBody("A"));
    mockWebServer.enqueue(new MockResponse()
      .setHeader("link", "<" + serverUrl + "/test?per_page=100&page=1>; rel=\"prev\", <" + serverUrl + "/test?per_page=100&page=1>; rel=\"first\"")
      .setBody("B"));

    List<String> response = executePaginatedRequest(serverUrl + "/test", oAuth20Service, auth2AccessToken, Arrays::asList);

    assertThat(response).contains("A", "B");
  }

  @Test
  public void fail_to_executed_paginated_request() {
    mockWebServer.enqueue(new MockResponse()
      .setHeader("Link", "<" + serverUrl + "/test?per_page=100&page=2>; rel=\"next\", <" + serverUrl + "/test?per_page=100&page=2>; rel=\"last\"")
      .setBody("A"));
    mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("Error!"));

    assertThatThrownBy(() -> executePaginatedRequest(serverUrl + "/test", oAuth20Service, auth2AccessToken, Arrays::asList))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(format("Fail to execute request '%s/test?per_page=100&page=2'. HTTP code: 404, response: Error!", serverUrl));
  }

  private class TestAPI extends DefaultApi20 {

    @Override
    public String getAccessTokenEndpoint() {
      return serverUrl + "/login/oauth/access_token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
      return serverUrl + "/login/oauth/authorize";
    }

  }
}
