/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.io.IOException;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonarqube.ws.client.OkHttpClientBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient.JSON_MEDIA_TYPE;

public class BitbucketCloudRestClientTest {
  private final MockWebServer server = new MockWebServer();
  private BitbucketCloudRestClient underTest;

  @Before
  public void prepare() throws IOException {
    server.start();

    underTest = new BitbucketCloudRestClient(new OkHttpClientBuilder().build(), server.url("/").toString(), server.url("/").toString());
  }

  @After
  public void stopServer() throws IOException {
    server.shutdown();
  }

  @Test
  public void failIfUnauthorized() {
    server.enqueue(new MockResponse().setResponseCode(401).setBody("Unauthorized"));

    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage("Unable to contact Bitbucket Cloud servers");
  }

  @Test
  public void validate_fails_with_IAE_if_timeout() {
    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"));
  }

  @Test
  public void validate_success() throws Exception {
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
  public void validate_with_invalid_workspace() {
    String tokenResponse = "{\"scopes\": \"webhook pullrequest:write\", \"access_token\": \"token\", \"expires_in\": 7200, "
      + "\"token_type\": \"bearer\", \"state\": \"client_credentials\", \"refresh_token\": \"abc\"}";
    server.enqueue(new MockResponse().setBody(tokenResponse).setResponseCode(200).setHeader("Content-Type", JSON_MEDIA_TYPE));
    String response = "{\"type\": \"error\", \"error\": {\"message\": \"No workspace with identifier 'workspace'.\"}}";

    server.enqueue(new MockResponse().setBody(response).setResponseCode(404).setHeader("Content-Type", JSON_MEDIA_TYPE));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage("Error returned by Bitbucket Cloud: No workspace with identifier 'workspace'.");
  }

  @Test
  public void validate_with_private_consumer() {
    String response = "{\"error_description\": \"Cannot use client_credentials with a consumer marked as \\\"public\\\". "
      + "Calls for auto generated consumers should use urn:bitbucket:oauth2:jwt instead.\", \"error\": \"invalid_grant\"}";

    server.enqueue(new MockResponse().setBody(response).setResponseCode(400).setHeader("Content-Type", JSON_MEDIA_TYPE));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage("Unable to contact Bitbucket Cloud servers: Configure the OAuth consumer in the Bitbucket workspace to be a private consumer");
  }

  @Test
  public void validate_with_invalid_credentials() {
    String response = "{\"error_description\": \"Invalid OAuth client credentials\", \"error\": \"unauthorized_client\"}";

    server.enqueue(new MockResponse().setBody(response).setResponseCode(400).setHeader("Content-Type", JSON_MEDIA_TYPE));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage("Unable to contact Bitbucket Cloud servers: Check your credentials");
  }

  @Test
  public void validate_with_insufficient_privileges() {
    String tokenResponse = "{\"scopes\": \"webhook pullrequest:write\", \"access_token\": \"token\", \"expires_in\": 7200, "
      + "\"token_type\": \"bearer\", \"state\": \"client_credentials\", \"refresh_token\": \"abc\"}";
    server.enqueue(new MockResponse().setBody(tokenResponse).setResponseCode(200).setHeader("Content-Type", JSON_MEDIA_TYPE));

    String error = "{\"type\": \"error\", \"error\": {\"message\": \"Your credentials lack one or more required privilege scopes.\", \"detail\": "
      + "{\"granted\": [\"email\"], \"required\": [\"account\"]}}}\n";
    server.enqueue(new MockResponse().setBody(error).setResponseCode(400).setHeader("Content-Type", JSON_MEDIA_TYPE));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage("Error returned by Bitbucket Cloud: Your credentials lack one or more required privilege scopes.");
  }

  @Test
  public void nullErrorBodyIsSupported() throws IOException {
    OkHttpClient clientMock = mock(OkHttpClient.class);
    Call callMock = mock(Call.class);

    when(callMock.execute()).thenReturn(new Response.Builder()
      .request(new Request.Builder().url("http://any.test").build())
      .protocol(Protocol.HTTP_1_1)
      .code(500)
      .message("")
      .build());
    when(clientMock.newCall(any())).thenReturn(callMock);

    underTest = new BitbucketCloudRestClient(clientMock);

    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage("Unable to contact Bitbucket Cloud servers");
  }

  @Test
  public void invalidJsonResponseBodyIsSupported() {
    server.enqueue(new MockResponse().setResponseCode(500)
      .setHeader("content-type", "application/json; charset=utf-8")
      .setBody("not a JSON string"));

    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage("Unable to contact Bitbucket Cloud servers");
  }

  @Test
  public void responseBodyWithoutErrorFieldIsSupported() {
    server.enqueue(new MockResponse().setResponseCode(500)
      .setHeader("content-type", "application/json; charset=utf-8")
      .setBody("{\"foo\": \"bar\"}"));

    assertThatIllegalArgumentException()
      .isThrownBy(() -> underTest.validate("clientId", "clientSecret", "workspace"))
      .withMessage("Unable to contact Bitbucket Cloud servers");
  }
}
