/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import java.util.concurrent.TimeUnit;
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

@ServerSide
public class AzureDevOpsHttpClient {

  private static final Logger LOG = LoggerFactory.getLogger(AzureDevOpsHttpClient.class);

  public static final String API_VERSION_3 = "api-version=3.0";

  protected static final String GET = "GET";
  protected static final String UNABLE_TO_CONTACT_AZURE_SERVER = "Unable to contact Azure DevOps server";
  protected static final String UNABLE_TO_CONTACT_AZURE_SERVER_MESSAGE_FORMAT = "%s for request [%s]: [%s]";
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
  protected static final String PATH_CONNECTION_DATA = "connectionData";

  // Query parameter names
  protected static final String PARAM_API_VERSION = "api-version";

  // Cross-org identity endpoint used to detect Global PATs ("All accessible organizations").
  // It carries no organization segment, so only a cross-org-scoped PAT can authenticate against
  // it: a 200 response means the PAT is global, 401/403 means it is scoped to a single organization.
  // Azure DevOps Server has no equivalent (no cross-org scope).
  protected static final String VSSPS_GLOBAL_URL = "https://app.vssps.visualstudio.com";
  private static final long GLOBAL_PAT_PROBE_TIMEOUT_MS = 5_000;

  protected final OkHttpClient client;
  private final OkHttpClient globalPatProbeClient;

  public AzureDevOpsHttpClient(TimeoutConfiguration timeoutConfiguration, OkHttpClient okHttpClient) {
    client = okHttpClient.newBuilder()
      .connectTimeout(timeoutConfiguration.getConnectTimeout(), TimeUnit.MILLISECONDS)
      .readTimeout(timeoutConfiguration.getReadTimeout(), TimeUnit.MILLISECONDS)
      .followRedirects(false)
      .followSslRedirects(false)
      .build();
    globalPatProbeClient = client.newBuilder()
      .connectTimeout(GLOBAL_PAT_PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
      .readTimeout(GLOBAL_PAT_PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
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

  /**
   * True when {@code token} is a Global PAT ("All accessible organizations"), probed against
   * {@link #VSSPS_GLOBAL_URL}. Callers should only invoke this for Azure DevOps Services URLs;
   * Azure DevOps Server has no such concept.
   */
  public boolean isGlobalPat(String token) {
    return isGlobalPat(VSSPS_GLOBAL_URL, token);
  }

  protected boolean isGlobalPat(String vsspsGlobalUrl, String token) {
    HttpUrl url = Objects.requireNonNull(HttpUrl.parse(vsspsGlobalUrl), INVALID_SERVER_URL)
      .newBuilder()
      .addPathSegment(PATH_APIS)
      .addPathSegment(PATH_CONNECTION_DATA)
      .addQueryParameter(PARAM_API_VERSION, API_VERSION_3_PREVIEW_VALUE)
      .build();
    Request request = prepareRequestWithToken(token, GET, url, null);
    try (Response response = globalPatProbeClient.newCall(request).execute()) {
      if (response.code() == HttpURLConnection.HTTP_OK) {
        return true;
      }
      if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED || response.code() == HttpURLConnection.HTTP_FORBIDDEN) {
        return false;
      }
      String body = Objects.requireNonNull(response.body(), MISSING_RESPONSE_BODY).string();
      throw new AzureDevopsServerException(response.code(), generateErrorMessage(body));
    } catch (IOException e) {
      throw new IllegalArgumentException(
        String.format(UNABLE_TO_CONTACT_AZURE_SERVER_MESSAGE_FORMAT, UNABLE_TO_CONTACT_AZURE_SERVER, request.url(), e.getMessage()), e);
    }
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
        String.format(UNABLE_TO_CONTACT_AZURE_SERVER_MESSAGE_FORMAT, UNABLE_TO_CONTACT_AZURE_SERVER, request.url(), e.getMessage()),
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
        String.format(UNABLE_TO_CONTACT_AZURE_SERVER_MESSAGE_FORMAT, UNABLE_TO_CONTACT_AZURE_SERVER, request.url(), e.getMessage()),
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
      String body = responseBody.string();
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
