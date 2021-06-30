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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.sonar.alm.client.TimeoutConfiguration;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarqube.ws.client.OkHttpClientBuilder;

import static com.google.common.base.Preconditions.checkArgument;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public class GithubApplicationHttpClientImpl implements GithubApplicationHttpClient {

  private static final Logger LOG = Loggers.get(GithubApplicationHttpClientImpl.class);
  private static final Pattern NEXT_LINK_PATTERN = Pattern.compile(".*<(.*)>; rel=\"next\"");
  private static final String GITHUB_API_VERSION_JSON = "application/vnd.github.v3+json";
  private static final String ANTIOPE_PREVIEW_JSON = "application/vnd.github.antiope-preview+json";
  private static final String MACHINE_MAN_PREVIEW_JSON = "application/vnd.github.machine-man-preview+json";

  private final OkHttpClient client;

  public GithubApplicationHttpClientImpl(TimeoutConfiguration timeoutConfiguration) {
    client = new OkHttpClientBuilder()
        .setConnectTimeoutMs(timeoutConfiguration.getConnectTimeout())
        .setReadTimeoutMs(timeoutConfiguration.getReadTimeout())
        .build();
  }

  @Override
  public GetResponse get(String appUrl, AccessToken token, String endPoint) throws IOException {
    validateEndPoint(endPoint);

    try (okhttp3.Response response = client.newCall(newGetRequest(appUrl, token, endPoint)).execute()) {
      int responseCode = response.code();
      if (responseCode != HTTP_OK) {
        LOG.warn("GET response did not have expected HTTP code (was {}): {}", responseCode, attemptReadContent(response));
        return new GetResponseImpl(responseCode, null, null);
      }
      return new GetResponseImpl(responseCode, readContent(response.body()).orElse(null), readNextEndPoint(response));
    }
  }

  private static void validateEndPoint(String endPoint) {
    checkArgument(endPoint.startsWith("/") || endPoint.startsWith("http"), "endpoint must start with '/' or 'http'");
  }

  private static Request newGetRequest(String appUrl, AccessToken token, String endPoint) {
    return newRequestBuilder(appUrl, token, endPoint).get().build();
  }

  @Override
  public Response post(String appUrl, AccessToken token, String endPoint) throws IOException {
    return doPost(appUrl, token, endPoint, new FormBody.Builder().build());
  }

  @Override
  public Response post(String appUrl, AccessToken token, String endPoint, String json) throws IOException {
    RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
    return doPost(appUrl, token, endPoint, body);
  }

  @Override
  public Response patch(String appUrl, AccessToken token, String endPoint, String json) throws IOException {
    RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
    return doPatch(appUrl, token, endPoint, body);
  }

  @Override
  public Response delete(String appUrl, AccessToken token, String endPoint) throws IOException {
    validateEndPoint(endPoint);

    try (okhttp3.Response response = client.newCall(newDeleteRequest(appUrl, token, endPoint)).execute()) {
      int responseCode = response.code();
      if (responseCode != HTTP_NO_CONTENT) {
        String content = attemptReadContent(response);
        LOG.warn("DELETE response did not have expected HTTP code (was {}): {}", responseCode, content);
        return new ResponseImpl(responseCode, content);
      }
      return new ResponseImpl(responseCode, null);
    }
  }

  private static Request newDeleteRequest(String appUrl, AccessToken token, String endPoint) {
    return newRequestBuilder(appUrl, token, endPoint).delete().build();
  }

  private Response doPost(String appUrl, @Nullable AccessToken token, String endPoint, RequestBody body) throws IOException {
    validateEndPoint(endPoint);

    try (okhttp3.Response response = client.newCall(newPostRequest(appUrl, token, endPoint, body)).execute()) {
      int responseCode = response.code();
      if (responseCode == HTTP_OK || responseCode == HTTP_CREATED) {
        return new ResponseImpl(responseCode, readContent(response.body()).orElse(null));
      } else if (responseCode == HTTP_NO_CONTENT) {
        return new ResponseImpl(responseCode, null);
      }
      String content = attemptReadContent(response);
      LOG.warn("POST response did not have expected HTTP code (was {}): {}", responseCode, content);
      return new ResponseImpl(responseCode, content);
    }
  }

  private Response doPatch(String appUrl, AccessToken token, String endPoint, RequestBody body) throws IOException {
    validateEndPoint(endPoint);

    try (okhttp3.Response response = client.newCall(newPatchRequest(token, appUrl, endPoint, body)).execute()) {
      int responseCode = response.code();
      if (responseCode == HTTP_OK) {
        return new ResponseImpl(responseCode, readContent(response.body()).orElse(null));
      } else if (responseCode == HTTP_NO_CONTENT) {
        return new ResponseImpl(responseCode, null);
      }
      String content = attemptReadContent(response);
      LOG.warn("PATCH response did not have expected HTTP code (was {}): {}", responseCode, content);
      return new ResponseImpl(responseCode, content);
    }
  }

  private static Request newPostRequest(String appUrl, @Nullable AccessToken token, String endPoint, RequestBody body) {
    return newRequestBuilder(appUrl, token, endPoint).post(body).build();
  }

  private static Request newPatchRequest(AccessToken token, String appUrl, String endPoint, RequestBody body) {
    return newRequestBuilder(appUrl, token, endPoint).patch(body).build();
  }

  private static Request.Builder newRequestBuilder(String appUrl, @Nullable AccessToken token, String endPoint) {
    Request.Builder url = new Request.Builder().url(toAbsoluteEndPoint(appUrl, endPoint));
    if (token != null) {
      url.addHeader("Authorization", token.getAuthorizationHeaderPrefix() + " " + token);
      url.addHeader("Accept", String.format("%s, %s, %s", ANTIOPE_PREVIEW_JSON, MACHINE_MAN_PREVIEW_JSON, GITHUB_API_VERSION_JSON));
    }
    return url;
  }

  private static String toAbsoluteEndPoint(String host, String endPoint) {
    if (endPoint.startsWith("http")) {
      return endPoint;
    }
    try {
      return new URL(host + endPoint).toExternalForm();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(String.format("%s is not a valid url", host + endPoint));
    }
  }

  private static String attemptReadContent(okhttp3.Response response) {
    try {
      return readContent(response.body()).orElse(null);
    } catch (IOException e) {
      return null;
    }
  }

  private static Optional<String> readContent(@Nullable ResponseBody body) throws IOException {
    if (body == null) {
      return empty();
    }
    try {
      return of(body.string());
    } finally {
      body.close();
    }
  }

  @CheckForNull
  private static String readNextEndPoint(okhttp3.Response response) {
    String links = response.headers().get("link");
    if (links == null || links.isEmpty() || !links.contains("rel=\"next\"")) {
      return null;
    }

    Matcher nextLinkMatcher = NEXT_LINK_PATTERN.matcher(links);
    if (!nextLinkMatcher.find()) {
      return null;
    }

    return nextLinkMatcher.group(1);
  }

  private static class ResponseImpl implements Response {
    private final int code;
    private final String content;

    private ResponseImpl(int code, @Nullable String content) {
      this.code = code;
      this.content = content;
    }

    @Override
    public int getCode() {
      return code;
    }

    @Override
    public Optional<String> getContent() {
      return ofNullable(content);
    }
  }

  private static final class GetResponseImpl extends ResponseImpl implements GetResponse {
    private final String nextEndPoint;

    private GetResponseImpl(int code, @Nullable String content, @Nullable String nextEndPoint) {
      super(code, content);
      this.nextEndPoint = nextEndPoint;
    }

    @Override
    public Optional<String> getNextEndPoint() {
      return ofNullable(nextEndPoint);
    }
  }
}
