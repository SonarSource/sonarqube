/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.alm.client.bitbucket.bitbucketcloud;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import javax.net.ssl.SSLHandshakeException;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonarqube.ws.client.OkHttpClientBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient.BBC_FAIL_WITH_ERROR;
import static org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient.BBC_FAIL_WITH_RESPONSE;
import static org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient.ERROR_BBC_SERVERS;
import static org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient.JSON_MEDIA_TYPE;
import static org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient.MISSING_PULL_REQUEST_READ_PERMISSION;
import static org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient.OAUTH_CONSUMER_NOT_PRIVATE;
import static org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient.SCOPE;
import static org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient.UNABLE_TO_CONTACT_BBC_SERVERS;
import static org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient.UNAUTHORIZED_CLIENT;

class BitbucketCloudRestClientTest {

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final MockWebServer server = new MockWebServer();
  private BitbucketCloudRestClient underTest;
  private String serverURL;

  @BeforeEach
  void prepare() throws IOException {
    server.start();
    serverURL = server.url("/").toString();
    underTest = new BitbucketCloudRestClient(new OkHttpClientBuilder().build(), serverURL, serverURL);
  }

  private static String encodeCredentials(String password) {
    byte[] bytes = ("username" + ":" + password).getBytes(StandardCharsets.UTF_8);
    return Base64.getEncoder().encodeToString(bytes);
  }

  @AfterEach
  void stopServer() throws IOException {
    server.shutdown();
  }

  @Test
  void get_repos() {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody("""
        {
          "values": [
            {
              "slug": "banana",
              "uuid": "BANANA-UUID",
              "name": "banana",
              "project": {
                "key": "HOY",
                "uuid": "BANANA-PROJECT-UUID",
                "name": "hoy"
              }
            },
            {
              "slug": "potato",
              "uuid": "POTATO-UUID",
              "name": "potato",
              "project": {
                "key": "HEY",
                "uuid": "POTATO-PROJECT-UUID",
                "name": "hey"
              }
            }
          ]
        }"""));

    RepositoryList repositoryList = underTest.searchRepos("user:apppwd", "", null, 1, 100);
    assertThat(repositoryList.getNext()).isNull();
    assertThat(repositoryList.getValues())
      .hasSize(2)
      .extracting(Repository::getUuid, Repository::getName, Repository::getSlug,
        g -> g.getProject().getUuid(), g -> g.getProject().getKey(), g -> g.getProject().getName())
      .containsExactlyInAnyOrder(
        tuple("BANANA-UUID", "banana", "banana", "BANANA-PROJECT-UUID", "HOY", "hoy"),
        tuple("POTATO-UUID", "potato", "potato", "POTATO-PROJECT-UUID", "HEY", "hey"));
  }

  @Test
  void get_repo() {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody("""
            {
              "slug": "banana",
              "uuid": "BANANA-UUID",
              "name": "banana",
              "mainbranch": {
                "type": "branch",
                "name": "develop"
               },\
              "project": {
                "key": "HOY",
                "uuid": "BANANA-PROJECT-UUID",
                "name": "hoy"
              }
            }\
        """));

    Repository repository = underTest.getRepo("user:apppwd", "workspace", "rep");
    assertThat(repository.getUuid()).isEqualTo("BANANA-UUID");
    assertThat(repository.getName()).isEqualTo("banana");
    assertThat(repository.getSlug()).isEqualTo("banana");
    assertThat(repository.getProject())
      .extracting(Project::getUuid, Project::getKey, Project::getName)
      .contains("BANANA-PROJECT-UUID", "HOY", "hoy");
    assertThat(repository.getMainBranch())
      .extracting(MainBranch::getType, MainBranch::getName)
      .contains("branch", "develop");
  }

  @Test
  void bbc_object_serialization_deserialization() {
    Project project = new Project("PROJECT-UUID-ONE", "projectKey", "projectName");
    MainBranch mainBranch = new MainBranch("branch", "develop");
    Repository repository = new Repository("REPO-UUID-ONE", "repo-slug", "repoName", project, mainBranch);
    RepositoryList repos = new RepositoryList(null, List.of(repository), 1, 100);
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody(new Gson().toJson(repos)));

    RepositoryList repositoryList = underTest.searchRepos("user:apppwd", "", null, 1, 100);
    assertThat(repositoryList.getNext()).isNull();
    assertThat(repositoryList.getPage()).isOne();
    assertThat(repositoryList.getPagelen()).isEqualTo(100);
    assertThat(repositoryList.getValues())
      .hasSize(1)
      .extracting(Repository::getUuid, Repository::getName, Repository::getSlug,
        g -> g.getProject().getUuid(), g -> g.getProject().getKey(), g -> g.getProject().getName(),
        g -> g.getMainBranch().getType(), g -> g.getMainBranch().getName())
      .containsExactlyInAnyOrder(
        tuple("REPO-UUID-ONE", "repoName", "repo-slug",
          "PROJECT-UUID-ONE", "projectKey", "projectName",
          "branch", "develop"));
  }

  @Test
  void validate_fails_if_unauthorized() {
    server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage(UNABLE_TO_CONTACT_BBC_SERVERS);
    assertThat(logTester.logs(Level.INFO)).containsExactly(String.format(BBC_FAIL_WITH_RESPONSE, serverURL, "401", "Unauthorized"));
  }

  @Test
  void validate_fails_with_IAE_if_timeout() {
    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"));
  }

  @Test
  void validate_success() throws Exception {
    String tokenResponse = "{\"scopes\": \"webhook pullrequest:write\", \"access_token\": \"token\", \"expires_in\": 7200, "
      + "\"token_type\": \"bearer\", \"state\": \"client_credentials\", \"refresh_token\": \"abc\"}";

    server.enqueue(new MockResponse().setBody(tokenResponse));
    server.enqueue(new MockResponse().setBody("OK"));

    underTest.validate("clientId", "clientSecret", "workspace");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getPath()).isEqualTo("/");
    assertThat(request.getHeader("Authorization")).isNotNull();
    assertThat(request.getBody().readUtf8()).isEqualTo("grant_type=client_credentials");
  }

  @Test
  void validate_fails_if_unsufficient_pull_request_privileges() {
    String tokenResponse = "{\"scopes\": \"\", \"access_token\": \"token\", \"expires_in\": 7200, "
      + "\"token_type\": \"bearer\", \"state\": \"client_credentials\", \"refresh_token\": \"abc\"}";
    server.enqueue(new MockResponse().setBody(tokenResponse));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage(ERROR_BBC_SERVERS + ": " + MISSING_PULL_REQUEST_READ_PERMISSION);
    assertThat(logTester.logs(Level.INFO)).containsExactly(MISSING_PULL_REQUEST_READ_PERMISSION + String.format(SCOPE, ""));
  }

  @Test
  void validate_with_invalid_workspace() {
    String tokenResponse = "{\"scopes\": \"webhook pullrequest:write\", \"access_token\": \"token\", \"expires_in\": 7200, "
      + "\"token_type\": \"bearer\", \"state\": \"client_credentials\", \"refresh_token\": \"abc\"}";
    server.enqueue(new MockResponse().setBody(tokenResponse).setResponseCode(200).setHeader("Content-Type", JSON_MEDIA_TYPE));

    String response = "{\"type\": \"error\", \"error\": {\"message\": \"No workspace with identifier 'workspace'.\"}}";
    server.enqueue(new MockResponse().setBody(response).setResponseCode(404).setHeader("Content-Type", JSON_MEDIA_TYPE));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage("Error returned by Bitbucket Cloud: No workspace with identifier 'workspace'. [HTTP 404]");
    assertThat(logTester.logs(Level.INFO)).containsExactly(String.format(BBC_FAIL_WITH_RESPONSE, serverURL + "2.0/repositories/workspace", "404", response));
  }

  @Test
  void validate_with_private_consumer() {
    String response = "{\"error_description\": \"Cannot use client_credentials with a consumer marked as \\\"public\\\". "
      + "Calls for auto generated consumers should use urn:bitbucket:oauth2:jwt instead.\", \"error\": \"invalid_grant\"}";

    server.enqueue(new MockResponse().setBody(response).setResponseCode(400).setHeader("Content-Type", JSON_MEDIA_TYPE));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage(UNABLE_TO_CONTACT_BBC_SERVERS + ": " + OAUTH_CONSUMER_NOT_PRIVATE);
    assertThat(logTester.logs(Level.INFO)).containsExactly(String.format(BBC_FAIL_WITH_RESPONSE, serverURL, "400", "invalid_grant"));
  }

  @Test
  void validate_with_invalid_credentials() {
    String response = "{\"error_description\": \"Invalid OAuth client credentials\", \"error\": \"unauthorized_client\"}";

    server.enqueue(new MockResponse().setBody(response).setResponseCode(400).setHeader("Content-Type", JSON_MEDIA_TYPE));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage(UNABLE_TO_CONTACT_BBC_SERVERS + ": " + UNAUTHORIZED_CLIENT);
    assertThat(logTester.logs(Level.INFO)).containsExactly(String.format(BBC_FAIL_WITH_RESPONSE, serverURL, "400", "unauthorized_client"));
  }

  @Test
  void validate_with_insufficient_privileges() {
    String tokenResponse = "{\"scopes\": \"webhook pullrequest:write\", \"access_token\": \"token\", \"expires_in\": 7200, "
      + "\"token_type\": \"bearer\", \"state\": \"client_credentials\", \"refresh_token\": \"abc\"}";
    server.enqueue(new MockResponse().setBody(tokenResponse).setResponseCode(200).setHeader("Content-Type", JSON_MEDIA_TYPE));

    String error = "{\"type\": \"error\", \"error\": {\"message\": \"Your credentials lack one or more required privilege scopes.\", \"detail\": "
      + "{\"granted\": [\"email\"], \"required\": [\"account\"]}}}\n";
    server.enqueue(new MockResponse().setBody(error).setResponseCode(400).setHeader("Content-Type", JSON_MEDIA_TYPE));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage("Error returned by Bitbucket Cloud: Your credentials lack one or more required privilege scopes. [HTTP 400]");
    assertThat(logTester.logs(Level.INFO)).containsExactly(String.format(BBC_FAIL_WITH_RESPONSE, serverURL + "2.0/repositories/workspace", "400", error));
  }

  @Test
  void validate_app_password_success() throws Exception {
    String reposResponse = """
      {"pagelen": 10,
      "values": [],
      "page": 1,
      "size": 0
      }""";

    server.enqueue(new MockResponse().setBody(reposResponse));
    server.enqueue(new MockResponse().setBody("OK"));

    underTest.validateAppPassword(encodeCredentials("ATATvalidtoken123"), "workspace");

    RecordedRequest request = server.takeRequest();
    assertThat(request.getPath()).isEqualTo("/2.0/repositories/workspace");
    assertThat(request.getHeader("Authorization")).isNotNull();
  }

  @Test
  void validate_app_password_with_invalid_credentials() {
    String response = "{\"type\": \"error\", \"error\": {\"message\": \"Invalid credentials.\"}}";
    server.enqueue(new MockResponse().setBody(response).setResponseCode(401).setHeader("Content-Type", JSON_MEDIA_TYPE));

    String encodedCredentials = encodeCredentials("ATATinvalidtoken");
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validateAppPassword(encodedCredentials, "workspace"))
      .withMessage("Error returned by Bitbucket Cloud: Invalid credentials. [HTTP 401]");
    assertThat(logTester.logs(Level.INFO)).containsExactly(String.format(BBC_FAIL_WITH_RESPONSE, serverURL + "2.0/repositories/workspace", "401", response));
  }

  @Test
  void validate_app_password_fails_with_old_app_password_prefix() {
    // ATBB prefix - no longer supported
    String encodedCredentials = encodeCredentials("ATBBsome_large_string_12345");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validateAppPassword(encodedCredentials, "workspace"))
      .withMessage("Bitbucket App Passwords are no longer supported. Please update your configuration to use API tokens");
  }

  @Test
  void validate_app_password_fails_with_no_valid_prefix() {
    // No valid prefix
    String encodedCredentials = encodeCredentials("invalid-app-password");

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validateAppPassword(encodedCredentials, "workspace"))
      .withMessage("Bitbucket App Passwords are no longer supported. Please update your configuration to use API tokens");
  }


  @Test
  void nullErrorBodyIsSupported() throws IOException {
    OkHttpClient clientMock = mock(OkHttpClient.class);
    Call callMock = mock(Call.class);

    String url = "http://any.test/";
    String message = "Unknown issue";
    when(callMock.execute()).thenReturn(new Response.Builder()
      .request(new Request.Builder().url(url).build())
      .protocol(Protocol.HTTP_1_1)
      .code(500)
      .message(message)
      .build());
    when(clientMock.newCall(any())).thenReturn(callMock);

    underTest = new BitbucketCloudRestClient(clientMock);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage(UNABLE_TO_CONTACT_BBC_SERVERS);
    assertThat(logTester.logs(Level.INFO)).containsExactly(String.format(BBC_FAIL_WITH_RESPONSE, url, "500", message));
  }

  @Test
  void invalidJsonResponseBodyIsSupported() {
    String body = "not a JSON string";
    server.enqueue(new MockResponse().setResponseCode(500)
      .setHeader("content-type", "application/json; charset=utf-8")
      .setBody(body));

    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage(UNABLE_TO_CONTACT_BBC_SERVERS);
    assertThat(logTester.logs(Level.INFO)).containsExactly(String.format(BBC_FAIL_WITH_RESPONSE, serverURL, "500", body));
  }

  @Test
  void responseBodyWithoutErrorFieldIsSupported() {
    String body = "{\"foo\": \"bar\"}";
    server.enqueue(new MockResponse().setResponseCode(500)
      .setHeader("content-type", "application/json; charset=utf-8")
      .setBody(body));

    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage(UNABLE_TO_CONTACT_BBC_SERVERS);
    assertThat(logTester.logs(Level.INFO)).containsExactly(String.format(BBC_FAIL_WITH_RESPONSE, serverURL, "500", body));
  }

  @Test
  void validate_fails_when_ssl_verification_failed() throws IOException {
    // GIVEN
    OkHttpClient okHttpClient = mock(OkHttpClient.class);
    Call call = mock(Call.class);
    underTest = new BitbucketCloudRestClient(okHttpClient, serverURL, serverURL);
    when(okHttpClient.newCall(any())).thenReturn(call);
    when(call.execute()).thenThrow(new SSLHandshakeException("SSL verification failed"));
    // WHEN
    // THEN
    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage(UNABLE_TO_CONTACT_BBC_SERVERS);
    assertThat(logTester.logs(Level.INFO)).containsExactly(String.format(BBC_FAIL_WITH_ERROR, serverURL, "SSL verification failed"));
  }

  @ParameterizedTest
  @MethodSource("appPasswordDeprecationScenarios")
  void operations_with_errors_and_app_password_show_deprecation_message(String operation, int httpCode, String credential, String errorMessage) {
    String response = String.format("{\"type\": \"error\", \"error\": {\"message\": \"%s\"}}", errorMessage);
    server.enqueue(new MockResponse().setBody(response).setResponseCode(httpCode).setHeader("Content-Type", JSON_MEDIA_TYPE));

    String encodedCredentials = encodeCredentials(credential);
    
    if ("getRepo".equals(operation)) {
      assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> underTest.getRepo(encodedCredentials, "workspace", "repo"))
        .withMessageContaining(String.format("Error returned by Bitbucket Cloud: %s [HTTP %d]", errorMessage, httpCode))
        .withMessageContaining(" - Note: Bitbucket App Passwords are deprecated and may cause authentication failures. Consider updating to API tokens using the SonarQube UI.");
    } else if ("searchRepos".equals(operation)) {
      assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> underTest.searchRepos(encodedCredentials, "workspace", "repo", 1, 100))
        .withMessageContaining(String.format("Error returned by Bitbucket Cloud: %s [HTTP %d]", errorMessage, httpCode))
        .withMessageContaining(" - Note: Bitbucket App Passwords are deprecated and may cause authentication failures. Consider updating to API tokens using the SonarQube UI.");
    }
  }

  @ParameterizedTest
  @MethodSource("validTokenScenarios")
  void getRepo_with_errors_and_valid_token_does_not_show_deprecation_message(int httpCode, String credential, String errorMessage) {
    String response = String.format("{\"type\": \"error\", \"error\": {\"message\": \"%s\"}}", errorMessage);
    server.enqueue(new MockResponse().setBody(response).setResponseCode(httpCode).setHeader("Content-Type", JSON_MEDIA_TYPE));

    String encodedCredentials = encodeCredentials(credential);
    assertThatExceptionOfType(IllegalStateException.class)
      .isThrownBy(() -> underTest.getRepo(encodedCredentials, "workspace", "repo"))
      .withMessage(String.format("Error returned by Bitbucket Cloud: %s [HTTP %d]", errorMessage, httpCode))
      .withMessageNotContaining("App Passwords");
  }

  private static java.util.stream.Stream<Arguments> appPasswordDeprecationScenarios() {
    return java.util.stream.Stream.of(
      Arguments.of("getRepo", 403, "old-app-password", "Access forbidden"),
      Arguments.of("getRepo", 404, "simple-password", "Repository not found"),
      Arguments.of("getRepo", 401, "old-app-password", "Authentication failed"),
      Arguments.of("searchRepos", 403, "legacy-password", "Access forbidden")
    );
  }

  private static java.util.stream.Stream<Arguments> validTokenScenarios() {
    return java.util.stream.Stream.of(
      Arguments.of(403, "ATATvalid_api_token", "Access forbidden"),
      Arguments.of(404, "ATCTvalid_access_token", "Repository not found")
    );
  }
}
