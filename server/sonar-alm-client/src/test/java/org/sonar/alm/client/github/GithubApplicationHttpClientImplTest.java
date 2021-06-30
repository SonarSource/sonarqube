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
package org.sonar.alm.client.github;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.net.SocketTimeoutException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.alm.client.ConstantTimeoutConfiguration;
import org.sonar.alm.client.github.GithubApplicationHttpClient.GetResponse;
import org.sonar.alm.client.github.GithubApplicationHttpClient.Response;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.alm.client.github.security.UserAccessToken;

import static java.lang.String.format;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;

@RunWith(DataProviderRunner.class)
public class GithubApplicationHttpClientImplTest {
  private static final String BETA_API_HEADER = "application/vnd.github.antiope-preview+json, " +
    "application/vnd.github.machine-man-preview+json, " +
    "application/vnd.github.v3+json";
  @Rule
  public MockWebServer server = new MockWebServer();

  private GithubApplicationHttpClientImpl underTest;

  private final AccessToken accessToken = new UserAccessToken(randomAlphabetic(10));
  private final String randomEndPoint = "/" + randomAlphabetic(10);
  private final String randomBody = randomAlphabetic(40);
  private String appUrl;

  @Before
  public void setUp() {
    this.appUrl = format("http://%s:%s", server.getHostName(), server.getPort());
    this.underTest = new GithubApplicationHttpClientImpl(new ConstantTimeoutConfiguration(500));
  }

  @Test
  public void get_fails_if_endpoint_does_not_start_with_slash() throws IOException {
    assertThatThrownBy(() -> underTest.get(appUrl, accessToken, "api/foo/bar"))
      .hasMessage("endpoint must start with '/' or 'http'")
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void get_fails_if_endpoint_does_not_start_with_http() throws IOException {
    assertThatThrownBy(() -> underTest.get(appUrl, accessToken, "ttp://api/foo/bar"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("endpoint must start with '/' or 'http'");
  }

  @Test
  public void get_fails_if_github_endpoint_is_invalid() throws IOException {
    assertThatThrownBy(() -> underTest.get("invalidUrl", accessToken, "/endpoint"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("invalidUrl/endpoint is not a valid url");
  }

  @Test
  public void get_adds_authentication_header_with_Bearer_type_and_Accept_header() throws IOException, InterruptedException {
    server.enqueue(new MockResponse());

    GetResponse response = underTest.get(appUrl, accessToken, randomEndPoint);

    assertThat(response).isNotNull();
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod()).isEqualTo("GET");
    assertThat(recordedRequest.getPath()).isEqualTo(randomEndPoint);
    assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("token " + accessToken.getValue());
    assertThat(recordedRequest.getHeader("Accept")).isEqualTo(BETA_API_HEADER);
  }

  @Test
  public void get_returns_body_as_response_if_code_is_200() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(200).setBody(randomBody));

    GetResponse response = underTest.get(appUrl, accessToken, randomEndPoint);

    assertThat(response.getContent()).contains(randomBody);
  }

  @Test
  public void get_timeout() {
    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

    try {
      underTest.get(appUrl, accessToken, randomEndPoint);
      fail("Expected timeout");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(SocketTimeoutException.class);
    }
  }

  @Test
  @UseDataProvider("someHttpCodesWithContentBut200")
  public void get_empty_response_if_code_is_not_200(int code) throws IOException {
    server.enqueue(new MockResponse().setResponseCode(code).setBody(randomBody));

    GetResponse response = underTest.get(appUrl, accessToken, randomEndPoint);

    assertThat(response.getContent()).isEmpty();
  }

  @Test
  public void get_returns_empty_endPoint_when_no_link_header() throws IOException {
    server.enqueue(new MockResponse().setBody(randomBody));

    GetResponse response = underTest.get(appUrl, accessToken, randomEndPoint);

    assertThat(response.getNextEndPoint()).isEmpty();
  }

  @Test
  public void get_returns_empty_endPoint_when_link_header_does_not_have_next_rel() throws IOException {
    server.enqueue(new MockResponse().setBody(randomBody)
      .setHeader("link", "<https://api.github.com/installation/repositories?per_page=5&page=4>; rel=\"prev\", " +
        "<https://api.github.com/installation/repositories?per_page=5&page=1>; rel=\"first\""));

    GetResponse response = underTest.get(appUrl, accessToken, randomEndPoint);

    assertThat(response.getNextEndPoint()).isEmpty();
  }

  @Test
  @UseDataProvider("linkHeadersWithNextRel")
  public void get_returns_endPoint_when_link_header_has_next_rel(String linkHeader) throws IOException {
    server.enqueue(new MockResponse().setBody(randomBody)
      .setHeader("link", linkHeader));

    GetResponse response = underTest.get(appUrl, accessToken, randomEndPoint);

    assertThat(response.getNextEndPoint()).contains("https://api.github.com/installation/repositories?per_page=5&page=2");
  }

  @Test
  public void get_returns_endPoint_when_link_header_has_next_rel_different_case() throws IOException {
    String linkHeader = "<https://api.github.com/installation/repositories?per_page=5&page=2>; rel=\"next\"";
    server.enqueue(new MockResponse().setBody(randomBody)
      .setHeader("Link", linkHeader));

    GetResponse response = underTest.get(appUrl, accessToken, randomEndPoint);

    assertThat(response.getNextEndPoint()).contains("https://api.github.com/installation/repositories?per_page=5&page=2");
  }

  @DataProvider
  public static Object[][] linkHeadersWithNextRel() {
    String expected = "https://api.github.com/installation/repositories?per_page=5&page=2";
    return new Object[][] {
      {"<" + expected + ">; rel=\"next\""},
      {"<" + expected + ">; rel=\"next\", " +
        "<https://api.github.com/installation/repositories?per_page=5&page=1>; rel=\"first\""},
      {"<https://api.github.com/installation/repositories?per_page=5&page=1>; rel=\"first\", " +
        "<" + expected + ">; rel=\"next\""},
      {"<https://api.github.com/installation/repositories?per_page=5&page=1>; rel=\"first\", " +
        "<" + expected + ">; rel=\"next\", " +
        "<https://api.github.com/installation/repositories?per_page=5&page=5>; rel=\"last\""},
    };
  }

  @DataProvider
  public static Object[][] someHttpCodesWithContentBut200() {
    return new Object[][] {
      {201},
      {202},
      {203},
      {404},
      {500}
    };
  }

  @Test
  public void post_fails_if_endpoint_does_not_start_with_slash() throws IOException {
    assertThatThrownBy(() -> underTest.post(appUrl, accessToken, "api/foo/bar"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("endpoint must start with '/' or 'http'");
  }

  @Test
  public void post_fails_if_endpoint_does_not_start_with_http() throws IOException {
    assertThatThrownBy(() -> underTest.post(appUrl, accessToken, "ttp://api/foo/bar"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("endpoint must start with '/' or 'http'");
  }

  @Test
  public void post_fails_if_github_endpoint_is_invalid() throws IOException {
    assertThatThrownBy(() -> underTest.post("invalidUrl", accessToken, "/endpoint"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("invalidUrl/endpoint is not a valid url");
  }

  @Test
  public void post_adds_authentication_header_with_Bearer_type_and_Accept_header() throws IOException, InterruptedException {
    server.enqueue(new MockResponse());

    Response response = underTest.post(appUrl, accessToken, randomEndPoint);

    assertThat(response).isNotNull();
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getMethod()).isEqualTo("POST");
    assertThat(recordedRequest.getPath()).isEqualTo(randomEndPoint);
    assertThat(recordedRequest.getHeader("Authorization")).isEqualTo("token " + accessToken.getValue());
    assertThat(recordedRequest.getHeader("Accept")).isEqualTo(BETA_API_HEADER);
  }

  @Test
  public void post_returns_body_as_response_if_code_is_200() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(200).setBody(randomBody));

    Response response = underTest.post(appUrl, accessToken, randomEndPoint);

    assertThat(response.getContent()).contains(randomBody);
  }

  @Test
  public void post_returns_body_as_response_if_code_is_201() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(201).setBody(randomBody));

    Response response = underTest.post(appUrl, accessToken, randomEndPoint);

    assertThat(response.getContent()).contains(randomBody);
  }

  @Test
  public void post_returns_empty_response_if_code_is_204() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(204));

    Response response = underTest.post(appUrl, accessToken, randomEndPoint);

    assertThat(response.getContent()).isEmpty();
  }

  @Test
  @UseDataProvider("httpCodesBut200_201And204")
  public void post_has_json_error_in_body_if_code_is_neither_200_201_nor_204(int code) throws IOException {
    server.enqueue(new MockResponse().setResponseCode(code).setBody(randomBody));

    Response response = underTest.post(appUrl, accessToken, randomEndPoint);

    assertThat(response.getContent()).contains(randomBody);
  }

  @DataProvider
  public static Object[][] httpCodesBut200_201And204() {
    return new Object[][] {
      {202},
      {203},
      {400},
      {401},
      {403},
      {404},
      {500}
    };
  }

  @Test
  public void post_with_json_body_adds_json_to_body_request() throws IOException, InterruptedException {
    server.enqueue(new MockResponse());
    String jsonBody = "{\"foo\": \"bar\"}";
    Response response = underTest.post(appUrl, accessToken, randomEndPoint, jsonBody);

    assertThat(response).isNotNull();
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(jsonBody);
  }

  @Test
  public void patch_with_json_body_adds_json_to_body_request() throws IOException, InterruptedException {
    server.enqueue(new MockResponse());
    String jsonBody = "{\"foo\": \"bar\"}";

    Response response = underTest.patch(appUrl, accessToken, randomEndPoint, jsonBody);

    assertThat(response).isNotNull();
    RecordedRequest recordedRequest = server.takeRequest();
    assertThat(recordedRequest.getBody().readUtf8()).isEqualTo(jsonBody);
  }

  @Test
  public void patch_returns_body_as_response_if_code_is_200() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(200).setBody(randomBody));

    Response response = underTest.patch(appUrl, accessToken, randomEndPoint, "{}");

    assertThat(response.getContent()).contains(randomBody);
  }

  @Test
  public void patch_returns_empty_response_if_code_is_204() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(204));

    Response response = underTest.patch(appUrl, accessToken, randomEndPoint, "{}");

    assertThat(response.getContent()).isEmpty();
  }

  @Test
  public void delete_returns_empty_response_if_code_is_204() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(204));

    Response response = underTest.delete(appUrl, accessToken, randomEndPoint);

    assertThat(response.getContent()).isEmpty();
  }

  @DataProvider
  public static Object[][] httpCodesBut204() {
    return new Object[][] {
      {200},
      {201},
      {202},
      {203},
      {400},
      {401},
      {403},
      {404},
      {500}
    };
  }

  @Test
  @UseDataProvider("httpCodesBut204")
  public void delete_returns_response_if_code_is_not_204(int code) throws IOException {
    server.enqueue(new MockResponse().setResponseCode(code).setBody(randomBody));

    Response response = underTest.delete(appUrl, accessToken, randomEndPoint);

    assertThat(response.getContent()).hasValue(randomBody);
  }

  @DataProvider
  public static Object[][] httpCodesBut200And204() {
    return new Object[][] {
      {201},
      {202},
      {203},
      {400},
      {401},
      {403},
      {404},
      {500}
    };
  }

  @Test
  @UseDataProvider("httpCodesBut200And204")
  public void patch_has_json_error_in_body_if_code_is_neither_200_nor_204(int code) throws IOException {
    server.enqueue(new MockResponse().setResponseCode(code).setBody(randomBody));

    Response response = underTest.patch(appUrl, accessToken, randomEndPoint, "{}");

    assertThat(response.getContent()).contains(randomBody);
  }

}
