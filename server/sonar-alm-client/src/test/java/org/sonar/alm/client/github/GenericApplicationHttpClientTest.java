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
package org.sonar.alm.client.github;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.event.Level;
import org.sonar.alm.client.ApplicationHttpClient.GetResponse;
import org.sonar.alm.client.ApplicationHttpClient.Response;
import org.sonar.alm.client.ConstantTimeoutConfiguration;
import org.sonar.alm.client.DevopsPlatformHeaders;
import org.sonar.alm.client.GenericApplicationHttpClient;
import org.sonar.alm.client.TimeoutConfiguration;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.auth.github.security.AccessToken;
import org.sonar.auth.github.security.UserAccessToken;

import static java.lang.String.format;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.sonar.alm.client.ApplicationHttpClient.RateLimit;

@RunWith(DataProviderRunner.class)
public class GenericApplicationHttpClientTest {
  private static final String GH_API_VERSION_HEADER = "X-GitHub-Api-Version";
  private static final String GH_API_VERSION = "2022-11-28";

  @Rule
  public MockWebServer server = new MockWebServer();

  @ClassRule
  public static LogTester logTester = new LogTester().setLevel(Level.WARN);

  private GenericApplicationHttpClient underTest;

  private final AccessToken accessToken = new UserAccessToken(secure().nextAlphabetic(10));
  private final String randomEndPoint = "/" + secure().nextAlphabetic(10);
  private final String randomBody = secure().nextAlphabetic(40);
  private String appUrl;

  @Before
  public void setUp() {
    this.appUrl = format("http://%s:%s", server.getHostName(), server.getPort());
    this.underTest = new TestApplicationHttpClient(new GithubHeaders(), new ConstantTimeoutConfiguration(500));
    logTester.clear();
  }

  private static class TestApplicationHttpClient extends GenericApplicationHttpClient {
    public TestApplicationHttpClient(DevopsPlatformHeaders devopsPlatformHeaders, TimeoutConfiguration timeoutConfiguration) {
      super(devopsPlatformHeaders, timeoutConfiguration);
    }
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
  public void getSilent_no_log_if_code_is_not_200() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(403));

    GetResponse response = underTest.getSilent(appUrl, accessToken, randomEndPoint);

    assertThat(logTester.logs()).isEmpty();
    assertThat(response.getContent()).isEmpty();

  }

  @Test
  public void get_log_if_code_is_not_200() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(403));

    GetResponse response = underTest.get(appUrl, accessToken, randomEndPoint);

    assertThat(logTester.logs(Level.WARN)).isNotEmpty();
    assertThat(response.getContent()).isEmpty();

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
    assertThat(recordedRequest.getHeader(GH_API_VERSION_HEADER)).isEqualTo(GH_API_VERSION);
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

    assertThat(response.getContent()).contains(randomBody);
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

  @Test
  public void get_returns_endPoint_when_link_header_is_from_gitlab() throws IOException {
    String linkHeader = "<https://gitlab.com/api/v4/groups?all_available=false&order_by=name&owned=false&page=2&per_page=2&sort=asc&statistics=false&with_custom_attributes=false>; rel=\"next\", <https://gitlab.com/api/v4/groups?all_available=false&order_by=name&owned=false&page=1&per_page=2&sort=asc&statistics=false&with_custom_attributes=false>; rel=\"first\", <https://gitlab.com/api/v4/groups?all_available=false&order_by=name&owned=false&page=8&per_page=2&sort=asc&statistics=false&with_custom_attributes=false>; rel=\"last\"";
    server.enqueue(new MockResponse().setBody(randomBody)
      .setHeader("link", linkHeader));

    GetResponse response = underTest.get(appUrl, accessToken, randomEndPoint);

    assertThat(response.getNextEndPoint()).contains("https://gitlab.com/api/v4/groups?all_available=false"
      + "&order_by=name&owned=false&page=2&per_page=2&sort=asc&statistics=false&with_custom_attributes=false");
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
    assertThat(recordedRequest.getHeader(GH_API_VERSION_HEADER)).isEqualTo(GH_API_VERSION);
  }

  @Test
  @DataProvider({"200", "201", "202"})
  public void post_returns_body_as_response_if_success(int code) throws IOException {
    server.enqueue(new MockResponse().setResponseCode(code).setBody(randomBody));

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

  @Test
  public void get_whenRateLimitHeadersArePresent_returnsRateLimit() throws Exception {
    testRateLimitHeader(() -> underTest.get(appUrl, accessToken, randomEndPoint), false);
  }

  @Test
  public void get_whenRateLimitHeadersArePresentAndUppercased_returnsRateLimit() throws Exception {
    testRateLimitHeader(() -> underTest.get(appUrl, accessToken, randomEndPoint), true);
  }

  private void testRateLimitHeader(Callable<Response> request, boolean uppercasedHeaders) throws Exception {
    server.enqueue(new MockResponse().setBody(randomBody)
      .setHeader(uppercasedHeaders ? "x-ratelimit-remaining" : "x-ratelimit-REMAINING", "1")
      .setHeader(uppercasedHeaders ? "x-ratelimit-limit" : "X-RATELIMIT-LIMIT", "10")
      .setHeader(uppercasedHeaders ? "x-ratelimit-reset" : "X-ratelimit-reset", "1000"));

    Response response = request.call();

    assertThat(response.getRateLimit())
      .isEqualTo(new RateLimit(1, 10, 1000L));
  }

  @Test
  public void get_whenRateLimitHeadersAreMissing_returnsNull() throws Exception {

    testMissingRateLimitHeader(() -> underTest.get(appUrl, accessToken, randomEndPoint));

  }

  private void testMissingRateLimitHeader(Callable<Response> request) throws Exception {
    server.enqueue(new MockResponse().setBody(randomBody));

    Response response = request.call();
    assertThat(response.getRateLimit())
      .isNull();
  }

  @Test
  public void delete_whenRateLimitHeadersArePresent_returnsRateLimit() throws Exception {
    testRateLimitHeader(() -> underTest.delete(appUrl, accessToken, randomEndPoint), false);

  }

  @Test
  public void delete_whenRateLimitHeadersAreMissing_returnsNull() throws Exception {
    testMissingRateLimitHeader(() -> underTest.delete(appUrl, accessToken, randomEndPoint));

  }

  @Test
  public void patch_whenRateLimitHeadersArePresent_returnsRateLimit() throws Exception {
    testRateLimitHeader(() -> underTest.patch(appUrl, accessToken, randomEndPoint, "body"), false);
  }

  @Test
  public void patch_whenRateLimitHeadersAreMissing_returnsNull() throws Exception {
    testMissingRateLimitHeader(() -> underTest.patch(appUrl, accessToken, randomEndPoint, "body"));
  }

  @Test
  public void post_whenRateLimitHeadersArePresent_returnsRateLimit() throws Exception {
    testRateLimitHeader(() -> underTest.post(appUrl, accessToken, randomEndPoint), false);
  }

  @Test
  public void post_whenRateLimitHeadersAreMissing_returnsNull() throws Exception {
    testMissingRateLimitHeader(() -> underTest.post(appUrl, accessToken, randomEndPoint));
  }
}
