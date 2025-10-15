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
package org.sonar.alm.client.azure;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.TimeoutConfiguration;
import org.sonar.api.server.ServerSide;
import org.sonarqube.ws.client.OkHttpClientBuilder;

@ServerSide
public class AzureDevOpsHttpClient {

  private static final Logger LOG = LoggerFactory.getLogger(AzureDevOpsHttpClient.class);

  public static final String API_VERSION_3 = "api-version=3.0";

  protected static final String GET = "GET";
  protected static final String UNABLE_TO_CONTACT_AZURE_SERVER = "Unable to contact Azure DevOps server";
  protected static final String INVALID_SERVER_URL = "Invalid Azure DevOps server URL";
  protected static final String MISSING_RESPONSE_BODY = "Response body is null";

  // API version values
  protected static final String API_VERSION_3_VALUE = "3.0";
  protected static final String API_VERSION_3_PREVIEW_VALUE = "3.0-preview";

  // Path segments
  protected static final String PATH_APIS = "_apis";
  protected static final String PATH_GIT = "git";
  protected static final String PATH_REPOSITORIES = "repositories";
  protected static final String PATH_PROJECTS = "projects";

  // Query parameter names
  protected static final String PARAM_API_VERSION = "api-version";

  protected final OkHttpClient client;

  public AzureDevOpsHttpClient(TimeoutConfiguration timeoutConfiguration) {
    client = new OkHttpClientBuilder()
      .setConnectTimeoutMs(timeoutConfiguration.getConnectTimeout())
      .setReadTimeoutMs(timeoutConfiguration.getReadTimeout())
      .setFollowRedirects(false)
      .build();
  }

  public void checkPAT(String serverUrl, String token) {
    HttpUrl url = Objects.requireNonNull(HttpUrl.parse(serverUrl), INVALID_SERVER_URL)
      .newBuilder()
      .addPathSegment(PATH_APIS)
      .addPathSegment(PATH_PROJECTS)
      .addQueryParameter(PARAM_API_VERSION, API_VERSION_3_VALUE)
      .build();
    doGet(token, url);
  }

  public GsonAzureProjectList getProjects(String serverUrl, String token) {
    HttpUrl url = Objects.requireNonNull(HttpUrl.parse(serverUrl), INVALID_SERVER_URL)
      .newBuilder()
      .addPathSegment(PATH_APIS)
      .addPathSegment(PATH_PROJECTS)
      .addQueryParameter(PARAM_API_VERSION, API_VERSION_3_VALUE)
      .build();
    return doGet(token, url, r -> buildGson().fromJson(Objects.requireNonNull(r.body(), MISSING_RESPONSE_BODY).charStream(), GsonAzureProjectList.class));
  }

  public GsonAzureProject getProject(String serverUrl, String token, String projectName) {
    HttpUrl url = Objects.requireNonNull(HttpUrl.parse(serverUrl), INVALID_SERVER_URL)
      .newBuilder()
      .addPathSegment(PATH_APIS)
      .addPathSegment(PATH_PROJECTS)
      .addPathSegment(projectName)
      .addQueryParameter(PARAM_API_VERSION, API_VERSION_3_VALUE)
      .build();
    return doGet(token, url, r -> buildGson().fromJson(Objects.requireNonNull(r.body(), MISSING_RESPONSE_BODY).charStream(), GsonAzureProject.class));
  }

  public GsonAzureRepoList getRepos(String serverUrl, String token, @Nullable String projectName) {
    HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(serverUrl), INVALID_SERVER_URL).newBuilder();
    
    if (StringUtils.isNotBlank(projectName)) {
      urlBuilder.addPathSegment(projectName);
    }
    
    HttpUrl url = urlBuilder
      .addPathSegment(PATH_APIS)
      .addPathSegment(PATH_GIT)
      .addPathSegment(PATH_REPOSITORIES)
      .addQueryParameter(PARAM_API_VERSION, API_VERSION_3_VALUE)
      .build();
    
    return doGet(token, url, r -> buildGson().fromJson(Objects.requireNonNull(r.body(), MISSING_RESPONSE_BODY).charStream(), GsonAzureRepoList.class));
  }

  public GsonAzureRepo getRepo(String serverUrl, String token, String projectName, String repositoryName) {
    HttpUrl url = Objects.requireNonNull(HttpUrl.parse(serverUrl), INVALID_SERVER_URL)
      .newBuilder()
      .addPathSegment(projectName)
      .addPathSegment(PATH_APIS)
      .addPathSegment(PATH_GIT)
      .addPathSegment(PATH_REPOSITORIES)
      .addPathSegment(repositoryName)
      .addQueryParameter(PARAM_API_VERSION, API_VERSION_3_VALUE)
      .build();
    
    return doGet(token, url, r -> buildGson().fromJson(Objects.requireNonNull(r.body(), MISSING_RESPONSE_BODY).charStream(), GsonAzureRepo.class));
  }

  private void doGet(String token, HttpUrl url) {
    Request request = prepareRequestWithToken(token, GET, url, null);
    doCall(request);
  }

  protected void doCall(Request request) {
    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response);
    } catch (IOException e) {
      throw new IllegalArgumentException(
        String.format("%s for request [%s]: [%s]", UNABLE_TO_CONTACT_AZURE_SERVER, request.url(), e.getMessage()),
        e);
    }
  }

  protected <G> G doGet(String token, HttpUrl url, Function<Response, G> handler) {
    Request request = prepareRequestWithToken(token, GET, url, null);
    return doCall(request, handler);
  }

  protected <G> G doCall(Request request, Function<Response, G> handler) {
    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response);
      return handler.apply(response);
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException(
        String.format("Response from Azure for request [%s] could not be parsed: [%s]", request.url(), e.getMessage()),
        e);
    } catch (IOException e) {
      throw new IllegalArgumentException(
        String.format("%s for request [%s]: [%s]", UNABLE_TO_CONTACT_AZURE_SERVER, request.url(), e.getMessage()),
        e);
    }
  }

  protected static Request prepareRequestWithToken(String token, String method, HttpUrl url, @Nullable RequestBody body) {
    return new Request.Builder()
      .method(method, body)
      .url(url)
      .addHeader("Authorization", encodeToken("accessToken:" + token))
      .build();
  }

  protected static void checkResponseIsSuccessful(Response response) throws IOException {
    if (!response.isSuccessful()) {
      if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
        LOG.error("{} for request [{}]: Invalid personal access token", UNABLE_TO_CONTACT_AZURE_SERVER, response.request().url());
        throw new AzureDevopsServerException(response.code(), "Invalid personal access token");
      }

      if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
        LOG.error("{} for request [{}]: URL Not Found", UNABLE_TO_CONTACT_AZURE_SERVER, response.request().url());
        throw new AzureDevopsServerException(response.code(), "Invalid Azure URL");
      }

      ResponseBody responseBody = response.body();
      String body = responseBody == null ? "" : responseBody.string();
      String errorMessage = generateErrorMessage(body);
      LOG.error("Azure API call to [{}] failed with {} http code. Azure response content : [{}]", response.request().url(), response.code(), body);
      throw new AzureDevopsServerException(response.code(), errorMessage);
    }
  }

  protected static String generateErrorMessage(String body) {
    GsonAzureError gsonAzureError = null;
    try {
      gsonAzureError = buildGson().fromJson(body, GsonAzureError.class);
    } catch (JsonSyntaxException e) {
      // not a json payload, ignore the error
    }
    if (gsonAzureError != null && !Strings.isNullOrEmpty(gsonAzureError.message())) {
      return AzureDevOpsHttpClient.UNABLE_TO_CONTACT_AZURE_SERVER + " : " + gsonAzureError.message();
    } else {
      return AzureDevOpsHttpClient.UNABLE_TO_CONTACT_AZURE_SERVER;
    }
  }


  protected static String encodeToken(String token) {
    return String.format("BASIC %s", Base64.encodeBase64String(token.getBytes(StandardCharsets.UTF_8)));
  }

  protected static Gson buildGson() {
    return new GsonBuilder()
      .create();
  }
}
