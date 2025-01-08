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
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
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

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

@ServerSide
public class AzureDevOpsHttpClient {

  private static final Logger LOG = LoggerFactory.getLogger(AzureDevOpsHttpClient.class);

  public static final String API_VERSION_3 = "api-version=3.0";

  protected static final String GET = "GET";
  protected static final String UNABLE_TO_CONTACT_AZURE_SERVER = "Unable to contact Azure DevOps server";

  protected final OkHttpClient client;

  public AzureDevOpsHttpClient(TimeoutConfiguration timeoutConfiguration) {
    client = new OkHttpClientBuilder()
      .setConnectTimeoutMs(timeoutConfiguration.getConnectTimeout())
      .setReadTimeoutMs(timeoutConfiguration.getReadTimeout())
      .setFollowRedirects(false)
      .build();
  }

  public void checkPAT(String serverUrl, String token) {
    String url = String.format("%s/_apis/projects?%s", getTrimmedUrl(serverUrl), API_VERSION_3);
    doGet(token, url);
  }

  public GsonAzureProjectList getProjects(String serverUrl, String token) {
    String url = String.format("%s/_apis/projects?%s", getTrimmedUrl(serverUrl), API_VERSION_3);
    return doGet(token, url, r -> buildGson().fromJson(r.body().charStream(), GsonAzureProjectList.class));
  }

  public GsonAzureProject getProject(String serverUrl, String token, String projectName) {
    String url = String.format("%s/_apis/projects/%s?%s", getTrimmedUrl(serverUrl), projectName, API_VERSION_3);
    return doGet(token, url, r -> buildGson().fromJson(r.body().charStream(), GsonAzureProject.class));

  }

  public GsonAzureRepoList getRepos(String serverUrl, String token, @Nullable String projectName) {
    String url = Stream.of(getTrimmedUrl(serverUrl), projectName, "_apis/git/repositories?" + API_VERSION_3)
      .filter(StringUtils::isNotBlank)
      .collect(joining("/"));
    return doGet(token, url, r -> buildGson().fromJson(r.body().charStream(), GsonAzureRepoList.class));
  }

  public GsonAzureRepo getRepo(String serverUrl, String token, String projectName, String repositoryName) {
    String url = Stream.of(getTrimmedUrl(serverUrl), projectName, "_apis/git/repositories", repositoryName + "?" + API_VERSION_3)
      .filter(StringUtils::isNotBlank)
      .collect(joining("/"));
    return doGet(token, url, r -> buildGson().fromJson(r.body().charStream(), GsonAzureRepo.class));
  }

  private void doGet(String token, String url) {
    Request request = prepareRequestWithToken(token, GET, url, null);
    doCall(request);
  }

  protected void doCall(Request request) {
    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response);
    } catch (IOException e) {
      LOG.error(String.format(UNABLE_TO_CONTACT_AZURE_SERVER + " for request [%s]: [%s]", request.url(), e.getMessage()));
      throw new IllegalArgumentException(UNABLE_TO_CONTACT_AZURE_SERVER, e);
    }
  }

  protected <G> G doGet(String token, String url, Function<Response, G> handler) {
    Request request = prepareRequestWithToken(token, GET, url, null);
    return doCall(request, handler);
  }

  protected <G> G doCall(Request request, Function<Response, G> handler) {
    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response);
      return handler.apply(response);
    } catch (JsonSyntaxException e) {
      LOG.error(String.format("Response from Azure for request [%s] could not be parsed: [%s]",
        request.url(),
        e.getMessage()));
      throw new IllegalArgumentException(UNABLE_TO_CONTACT_AZURE_SERVER + ", got an unexpected response", e);
    } catch (IOException e) {
      LOG.error(String.format(UNABLE_TO_CONTACT_AZURE_SERVER + " for request [%s]: [%s]", request.url(), e.getMessage()));
      throw new IllegalArgumentException(UNABLE_TO_CONTACT_AZURE_SERVER, e);
    }
  }

  protected static Request prepareRequestWithToken(String token, String method, String url, @Nullable RequestBody body) {
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
      String errorMessage = generateErrorMessage(body, UNABLE_TO_CONTACT_AZURE_SERVER);
      LOG.error("Azure API call to [{}] failed with {} http code. Azure response content : [{}]", response.request().url(), response.code(), body);
      throw new AzureDevopsServerException(response.code(), errorMessage);
    }
  }

  protected static String generateErrorMessage(String body, String defaultMessage) {
    GsonAzureError gsonAzureError = null;
    try {
      gsonAzureError = buildGson().fromJson(body, GsonAzureError.class);
    } catch (JsonSyntaxException e) {
      // not a json payload, ignore the error
    }
    if (gsonAzureError != null && !Strings.isNullOrEmpty(gsonAzureError.message())) {
      return defaultMessage + " : " + gsonAzureError.message();
    } else {
      return defaultMessage;
    }
  }

  protected static String getTrimmedUrl(String rawUrl) {
    if (isBlank(rawUrl)) {
      return rawUrl;
    }
    if (rawUrl.endsWith("/")) {
      return substringBeforeLast(rawUrl, "/");
    }
    return rawUrl;
  }

  protected static String encodeToken(String token) {
    return String.format("BASIC %s", Base64.encodeBase64String(token.getBytes(StandardCharsets.UTF_8)));
  }

  protected static Gson buildGson() {
    return new GsonBuilder()
      .create();
  }
}
