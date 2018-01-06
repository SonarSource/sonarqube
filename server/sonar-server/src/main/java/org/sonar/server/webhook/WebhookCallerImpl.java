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
package org.sonar.server.webhook;

import java.io.IOException;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.utils.System2;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static okhttp3.internal.http.StatusLine.HTTP_PERM_REDIRECT;
import static okhttp3.internal.http.StatusLine.HTTP_TEMP_REDIRECT;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

@ComputeEngineSide
public class WebhookCallerImpl implements WebhookCaller {

  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  private static final String PROJECT_KEY_HEADER = "X-SonarQube-Project";

  private final System2 system;
  private final OkHttpClient okHttpClient;

  public WebhookCallerImpl(System2 system, OkHttpClient okHttpClient) {
    this.system = system;
    this.okHttpClient = newClientWithoutRedirect(okHttpClient);
  }

  @Override
  public WebhookDelivery call(Webhook webhook, WebhookPayload payload) {
    WebhookDelivery.Builder builder = new WebhookDelivery.Builder();
    long startedAt = system.now();
    builder
      .setAt(startedAt)
      .setPayload(payload)
      .setWebhook(webhook);

    try {
      Request request = buildHttpRequest(webhook, payload);
      try (Response response = execute(request)) {
        builder.setHttpStatus(response.code());
        builder.setDurationInMs((int) (system.now() - startedAt));
      }
    } catch (Exception e) {
      builder.setError(e);
    }
    return builder.build();
  }

  private static Request buildHttpRequest(Webhook webhook, WebhookPayload payload) {
    HttpUrl url = HttpUrl.parse(webhook.getUrl());
    if (url == null) {
      throw new IllegalArgumentException("Webhook URL is not valid: " + webhook.getUrl());
    }
    Request.Builder request = new Request.Builder();
    request.url(url);
    request.header(PROJECT_KEY_HEADER, payload.getProjectKey());
    if (isNotEmpty(url.username())) {
      request.header("Authorization", Credentials.basic(url.username(), url.password(), UTF_8));
    }

    RequestBody body = RequestBody.create(JSON, payload.getJson());
    request.post(body);
    return request.build();
  }

  private Response execute(Request request) throws IOException {
    Response response = okHttpClient.newCall(request).execute();
    switch (response.code()) {
      case HTTP_MOVED_PERM:
      case HTTP_MOVED_TEMP:
      case HTTP_TEMP_REDIRECT:
      case HTTP_PERM_REDIRECT:
        // OkHttpClient does not follow the redirect with the same HTTP method. A POST is
        // redirected to a GET. Because of that the redirect must be manually
        // implemented.
        // See:
        // https://github.com/square/okhttp/blob/07309c1c7d9e296014268ebd155ebf7ef8679f6c/okhttp/src/main/java/okhttp3/internal/http/RetryAndFollowUpInterceptor.java#L316
        // https://github.com/square/okhttp/issues/936#issuecomment-266430151
        return followPostRedirect(response);
      default:
        return response;
    }
  }

  /**
   * Inspired by https://github.com/square/okhttp/blob/parent-3.6.0/okhttp/src/main/java/okhttp3/internal/http/RetryAndFollowUpInterceptor.java#L286
   */
  private Response followPostRedirect(Response response) throws IOException {
    String location = response.header("Location");
    if (location == null) {
      throw new IllegalStateException(format("Missing HTTP header 'Location' in redirect of %s", response.request().url()));
    }
    HttpUrl url = response.request().url().resolve(location);

    // Don't follow redirects to unsupported protocols.
    if (url == null) {
      throw new IllegalStateException(format("Unsupported protocol in redirect of %s to %s", response.request().url(), location));
    }

    Request.Builder redirectRequest = response.request().newBuilder();
    redirectRequest.post(response.request().body());
    response.body().close();
    return okHttpClient.newCall(redirectRequest.url(url).build()).execute();
  }

  private static OkHttpClient newClientWithoutRedirect(OkHttpClient client) {
    return client.newBuilder()
      .followRedirects(false)
      .followSslRedirects(false)
      .build();
  }
}
