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
package org.sonar.alm.client.bitbucketserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.sonar.alm.client.TimeoutConfiguration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarqube.ws.client.OkHttpClientBuilder;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.util.Locale.ENGLISH;
import static org.sonar.api.internal.apachecommons.lang.StringUtils.removeEnd;

@ServerSide
public class BitbucketServerRestClient {

  private static final Logger LOG = Loggers.get(BitbucketServerRestClient.class);
  private static final String GET = "GET";
  protected static final String UNABLE_TO_CONTACT_BITBUCKET_SERVER = "Unable to contact Bitbucket server";

  protected final OkHttpClient client;

  public BitbucketServerRestClient(TimeoutConfiguration timeoutConfiguration) {
    OkHttpClientBuilder okHttpClientBuilder = new OkHttpClientBuilder();
    client = okHttpClientBuilder
      .setConnectTimeoutMs(timeoutConfiguration.getConnectTimeout())
      .setReadTimeoutMs(timeoutConfiguration.getReadTimeout())
      .build();
  }

  public void validateUrl(String serverUrl) {
    HttpUrl url = buildUrl(serverUrl, "/rest/api/1.0/repos");
    doGet("", url, r -> buildGson().fromJson(r.body().charStream(), RepositoryList.class));
  }

  public void validateToken(String serverUrl, String token) {
    HttpUrl url = buildUrl(serverUrl, "/rest/api/1.0/users");
    doGet(token, url, r -> buildGson().fromJson(r.body().charStream(), UserList.class));
  }

  public void validateReadPermission(String serverUrl, String personalAccessToken) {
    HttpUrl url = buildUrl(serverUrl, "/rest/api/1.0/repos");
    doGet(personalAccessToken, url, r -> buildGson().fromJson(r.body().charStream(), RepositoryList.class));
  }

  public RepositoryList getRepos(String serverUrl, String token, @Nullable String project, @Nullable String repo) {
    String projectOrEmpty = Optional.ofNullable(project).orElse("");
    String repoOrEmpty = Optional.ofNullable(repo).orElse("");
    HttpUrl url = buildUrl(serverUrl, format("/rest/api/1.0/repos?projectname=%s&name=%s", projectOrEmpty, repoOrEmpty));
    return doGet(token, url, r -> buildGson().fromJson(r.body().charStream(), RepositoryList.class));
  }

  public Repository getRepo(String serverUrl, String token, String project, String repoSlug) {
    HttpUrl url = buildUrl(serverUrl, format("/rest/api/1.0/projects/%s/repos/%s", project, repoSlug));
    return doGet(token, url, r -> buildGson().fromJson(r.body().charStream(), Repository.class));
  }

  public RepositoryList getRecentRepo(String serverUrl, String token) {
    HttpUrl url = buildUrl(serverUrl, "/rest/api/1.0/profile/recent/repos");
    return doGet(token, url, r -> buildGson().fromJson(r.body().charStream(), RepositoryList.class));
  }

  public ProjectList getProjects(String serverUrl, String token) {
    HttpUrl url = buildUrl(serverUrl, "/rest/api/1.0/projects");
    return doGet(token, url, r -> buildGson().fromJson(r.body().charStream(), ProjectList.class));
  }

  public BranchesList getBranches(String serverUrl, String token, String projectSlug, String repositorySlug) {
    HttpUrl url = buildUrl(serverUrl, format("/rest/api/1.0/projects/%s/repos/%s/branches", projectSlug, repositorySlug));
    return doGet(token, url, r -> buildGson().fromJson(r.body().charStream(), BranchesList.class));
  }

  protected static HttpUrl buildUrl(@Nullable String serverUrl, String relativeUrl) {
    if (serverUrl == null || !(serverUrl.toLowerCase(ENGLISH).startsWith("http://") || serverUrl.toLowerCase(ENGLISH).startsWith("https://"))) {
      throw new IllegalArgumentException("url must start with http:// or https://");
    }
    return HttpUrl.parse(removeEnd(serverUrl, "/") + relativeUrl);
  }

  protected <G> G doGet(String token, HttpUrl url, Function<Response, G> handler) {
    Request request = prepareRequestWithBearerToken(token, GET, url, null);
    return doCall(request, handler);
  }

  protected static Request prepareRequestWithBearerToken(@Nullable String token, String method, HttpUrl url, @Nullable RequestBody body) {
    Request.Builder builder = new Request.Builder()
      .method(method, body)
      .url(url)
      .addHeader("x-atlassian-token", "no-check");

    if (!isNullOrEmpty(token)) {
      builder.addHeader("Authorization", "Bearer " + token);
    }

    return builder.build();
  }

  protected <G> G doCall(Request request, Function<Response, G> handler) {
    try (Response response = client.newCall(request).execute()) {
      handleError(response);
      return handler.apply(response);
    } catch (JsonSyntaxException e) {
      LOG.info(UNABLE_TO_CONTACT_BITBUCKET_SERVER + ": " + e.getMessage(), e);
      throw new IllegalArgumentException(UNABLE_TO_CONTACT_BITBUCKET_SERVER + ", got an unexpected response", e);
    } catch (IOException e) {
      LOG.info(UNABLE_TO_CONTACT_BITBUCKET_SERVER + ": " + e.getMessage(), e);
      throw new IllegalArgumentException(UNABLE_TO_CONTACT_BITBUCKET_SERVER, e);
    }
  }

  protected static void handleError(Response response) throws IOException {
    if (!response.isSuccessful()) {
      String errorMessage = getErrorMessage(response.body());
      LOG.debug(UNABLE_TO_CONTACT_BITBUCKET_SERVER + ": {} {}", response.code(), errorMessage);
      if (response.code() == HTTP_UNAUTHORIZED) {
        throw new BitbucketServerException(HTTP_UNAUTHORIZED, "Invalid personal access token");
      } else if (response.code() == HTTP_NOT_FOUND) {
        throw new BitbucketServerException(HTTP_NOT_FOUND, errorMessage);
      }
      throw new IllegalArgumentException(UNABLE_TO_CONTACT_BITBUCKET_SERVER);
    }
  }

  protected static boolean equals(@Nullable MediaType first, @Nullable MediaType second) {
    String s1 = first == null ? null : first.toString().toLowerCase(ENGLISH).replace(" ", "");
    String s2 = second == null ? null : second.toString().toLowerCase(ENGLISH).replace(" ", "");
    return s1 != null && s2 != null && s1.equals(s2);
  }

  protected static String getErrorMessage(ResponseBody body) throws IOException {
    String bodyString = body.string();
    if (equals(MediaType.parse("application/json;charset=utf-8"), body.contentType()) && !isNullOrEmpty(bodyString)) {
      try {
        return Stream.of(buildGson().fromJson(bodyString, Errors.class).errorData)
          .map(e -> e.exceptionName + " " + e.message)
          .collect(Collectors.joining("\n"));
      } catch (JsonParseException e) {
        return bodyString;
      }
    }
    return bodyString;
  }

  protected static Gson buildGson() {
    return new GsonBuilder()
      .create();
  }

  protected static class Errors {

    @SerializedName("errors")
    public Error[] errorData;

    public Errors() {
      // http://stackoverflow.com/a/18645370/229031
    }

    public static class Error {
      @SerializedName("message")
      public String message;

      @SerializedName("exceptionName")
      public String exceptionName;

      public Error() {
        // http://stackoverflow.com/a/18645370/229031
      }
    }

  }

}
