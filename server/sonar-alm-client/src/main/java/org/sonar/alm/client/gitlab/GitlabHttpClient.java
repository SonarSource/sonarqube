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
package org.sonar.alm.client.gitlab;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.logging.log4j.util.Strings;
import org.sonar.alm.client.TimeoutConfiguration;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.OkHttpClientBuilder;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.nio.charset.StandardCharsets.UTF_8;

@ServerSide
public class GitlabHttpClient {

  private static final Logger LOG = Loggers.get(GitlabHttpClient.class);
  protected static final String PRIVATE_TOKEN = "Private-Token";
  protected final OkHttpClient client;

  public GitlabHttpClient(TimeoutConfiguration timeoutConfiguration) {
    client = new OkHttpClientBuilder()
      .setConnectTimeoutMs(timeoutConfiguration.getConnectTimeout())
      .setReadTimeoutMs(timeoutConfiguration.getReadTimeout())
      .setFollowRedirects(false)
      .build();
  }

  public void checkReadPermission(@Nullable String gitlabUrl, @Nullable String personalAccessToken) {
    checkProjectAccess(gitlabUrl, personalAccessToken, "Could not validate GitLab read permission. Got an unexpected answer.");
  }

  public void checkUrl(@Nullable String gitlabUrl) {
    checkProjectAccess(gitlabUrl, null, "Could not validate GitLab url. Got an unexpected answer.");
  }

  private void checkProjectAccess(@Nullable String gitlabUrl, @Nullable String personalAccessToken, String errorMessage) {
    String url = String.format("%s/projects", gitlabUrl);

    LOG.debug(String.format("get projects : [%s]", url));
    Request.Builder builder = new Request.Builder()
      .url(url)
      .get();

    if (personalAccessToken != null) {
      builder.addHeader(PRIVATE_TOKEN, personalAccessToken);
    }

    Request request = builder.build();

    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response, errorMessage);
      Project.parseJsonArray(response.body().string());
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException("Could not parse GitLab answer to verify read permission. Got a non-json payload as result.");
    } catch (IOException e) {
      logException(url, e);
      throw new IllegalArgumentException(errorMessage);
    }
  }

  private static void logException(String url, IOException e) {
    LOG.info(String.format("Gitlab API call to [%s] failed with error message : [%s]", url, e.getMessage()), e);
  }

  public void checkToken(String gitlabUrl, String personalAccessToken) {
    String url = String.format("%s/user", gitlabUrl);

    LOG.debug(String.format("get current user : [%s]", url));
    Request.Builder builder = new Request.Builder()
      .addHeader(PRIVATE_TOKEN, personalAccessToken)
      .url(url)
      .get();

    Request request = builder.build();

    String errorMessage = "Could not validate GitLab token. Got an unexpected answer.";
    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response, errorMessage);
      GsonId.parseOne(response.body().string());
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException("Could not parse GitLab answer to verify token. Got a non-json payload as result.");
    } catch (IOException e) {
      logException(url, e);
      throw new IllegalArgumentException(errorMessage);
    }
  }

  public void checkWritePermission(String gitlabUrl, String personalAccessToken) {
    String url = String.format("%s/markdown", gitlabUrl);

    LOG.debug(String.format("verify write permission by formating some markdown : [%s]", url));
    Request.Builder builder = new Request.Builder()
      .url(url)
      .addHeader(PRIVATE_TOKEN, personalAccessToken)
      .addHeader("Content-Type", MediaTypes.JSON)
      .post(RequestBody.create("{\"text\":\"validating write permission\"}".getBytes(UTF_8)));

    Request request = builder.build();

    String errorMessage = "Could not validate GitLab write permission. Got an unexpected answer.";
    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response, errorMessage);
      GsonMarkdown.parseOne(response.body().string());
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException("Could not parse GitLab answer to verify write permission. Got a non-json payload as result.");
    } catch (IOException e) {
      logException(url, e);
      throw new IllegalArgumentException(errorMessage);
    }

  }

  private static String urlEncode(String value) {
    try {
      return URLEncoder.encode(value, UTF_8.toString());
    } catch (UnsupportedEncodingException ex) {
      throw new IllegalStateException(ex.getCause());
    }
  }

  protected static void checkResponseIsSuccessful(Response response) throws IOException {
    checkResponseIsSuccessful(response, "GitLab Merge Request did not happen, please check your configuration");
  }

  protected static void checkResponseIsSuccessful(Response response, String errorMessage) throws IOException {
    if (!response.isSuccessful()) {
      String body = response.body().string();
      LOG.error(String.format("Gitlab API call to [%s] failed with %s http code. gitlab response content : [%s]", response.request().url().toString(), response.code(), body));
      if (isTokenRevoked(response, body)) {
        throw new GitlabServerException(response.code(), "Your GitLab token was revoked");
      } else if (isTokenExpired(response, body)) {
        throw new GitlabServerException(response.code(), "Your GitLab token is expired");
      } else if (isInsufficientScope(response, body)) {
        throw new GitlabServerException(response.code(), "Your GitLab token has insufficient scope");
      } else if (response.code() == HTTP_UNAUTHORIZED) {
        throw new GitlabServerException(response.code(), "Invalid personal access token");
      } else if (response.isRedirect()) {
        throw new GitlabServerException(response.code(), "Request was redirected, please provide the correct URL");
      } else {
        throw new GitlabServerException(response.code(), errorMessage);
      }
    }
  }

  private static boolean isTokenRevoked(Response response, String body) {
    if (response.code() == HTTP_UNAUTHORIZED) {
      try {
        Optional<GsonError> gitlabError = GsonError.parseOne(body);
        return gitlabError.map(GsonError::getErrorDescription).map(description -> description.contains("Token was revoked")).orElse(false);
      } catch (JsonParseException e) {
        // nothing to do
      }
    }
    return false;
  }

  private static boolean isTokenExpired(Response response, String body) {
    if (response.code() == HTTP_UNAUTHORIZED) {
      try {
        Optional<GsonError> gitlabError = GsonError.parseOne(body);
        return gitlabError.map(GsonError::getErrorDescription).map(description -> description.contains("Token is expired")).orElse(false);
      } catch (JsonParseException e) {
        // nothing to do
      }
    }
    return false;
  }

  private static boolean isInsufficientScope(Response response, String body) {
    if (response.code() == HTTP_FORBIDDEN) {
      try {
        Optional<GsonError> gitlabError = GsonError.parseOne(body);
        return gitlabError.map(GsonError::getError).map("insufficient_scope"::equals).orElse(false);
      } catch (JsonParseException e) {
        // nothing to do
      }
    }
    return false;
  }

  public Project getProject(String gitlabUrl, String pat, Long gitlabProjectId) {
    String url = String.format("%s/projects/%s", gitlabUrl, gitlabProjectId);
    LOG.debug(String.format("get project : [%s]", url));
    Request request = new Request.Builder()
      .addHeader(PRIVATE_TOKEN, pat)
      .get()
      .url(url)
      .build();

    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response);
      String body = response.body().string();
      LOG.trace(String.format("loading project payload result : [%s]", body));
      return new GsonBuilder().create().fromJson(body, Project.class);
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException("Could not parse GitLab answer to retrieve a project. Got a non-json payload as result.");
    } catch (IOException e) {
      logException(url, e);
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  //
  // This method is used to check if a user has REPORTER level access to the project, which is a requirement for PR decoration.
  // As of June 9, 2021 there is no better way to do this check and still support GitLab 11.7.
  //
  public Optional<Project> getReporterLevelAccessProject(String gitlabUrl, String pat, Long gitlabProjectId) {
    String url = String.format("%s/projects?min_access_level=20&id_after=%s&id_before=%s", gitlabUrl, gitlabProjectId - 1,
      gitlabProjectId + 1);
    LOG.debug(String.format("get project : [%s]", url));
    Request request = new Request.Builder()
      .addHeader(PRIVATE_TOKEN, pat)
      .get()
      .url(url)
      .build();

    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response);
      String body = response.body().string();
      LOG.trace(String.format("loading project payload result : [%s]", body));

      List<Project> projects = Project.parseJsonArray(body);
      if (projects.isEmpty()) {
        return Optional.empty();
      } else {
        return Optional.of(projects.get(0));
      }
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException("Could not parse GitLab answer to retrieve a project. Got a non-json payload as result.");
    } catch (IOException e) {
      logException(url, e);
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public List<GitLabBranch> getBranches(String gitlabUrl, String pat, Long gitlabProjectId) {
    String url = String.format("%s/projects/%s/repository/branches", gitlabUrl, gitlabProjectId);
    LOG.debug(String.format("get branches : [%s]", url));
    Request request = new Request.Builder()
      .addHeader(PRIVATE_TOKEN, pat)
      .get()
      .url(url)
      .build();

    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response);
      String body = response.body().string();
      LOG.trace(String.format("loading branches payload result : [%s]", body));
      return Arrays.asList(new GsonBuilder().create().fromJson(body, GitLabBranch[].class));
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException("Could not parse GitLab answer to retrieve project branches. Got a non-json payload as result.");
    } catch (IOException e) {
      logException(url, e);
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  public ProjectList searchProjects(String gitlabUrl, String personalAccessToken, @Nullable String projectName,
    @Nullable Integer pageNumber, @Nullable Integer pageSize) {
    String url = String.format("%s/projects?archived=false&simple=true&membership=true&order_by=name&sort=asc&search=%s%s%s",
      gitlabUrl,
      projectName == null ? "" : urlEncode(projectName),
      pageNumber == null ? "" : String.format("&page=%d", pageNumber),
      pageSize == null ? "" : String.format("&per_page=%d", pageSize)
    );

    LOG.debug(String.format("get projects : [%s]", url));
    Request request = new Request.Builder()
      .addHeader(PRIVATE_TOKEN, personalAccessToken)
      .url(url)
      .get()
      .build();

    try (Response response = client.newCall(request).execute()) {
      Headers headers = response.headers();
      checkResponseIsSuccessful(response, "Could not get projects from GitLab instance");
      List<Project> projectList = Project.parseJsonArray(response.body().string());
      int returnedPageNumber = parseAndGetIntegerHeader(headers.get("X-Page"));
      int returnedPageSize = parseAndGetIntegerHeader(headers.get("X-Per-Page"));
      String xtotal = headers.get("X-Total");
      Integer totalProjects = Strings.isEmpty(xtotal) ? null : parseAndGetIntegerHeader(xtotal);
      return new ProjectList(projectList, returnedPageNumber, returnedPageSize, totalProjects);
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException("Could not parse GitLab answer to search projects. Got a non-json payload as result.");
    } catch (IOException e) {
      logException(url, e);
      throw new IllegalStateException(e.getMessage(), e);
    }
  }

  private static int parseAndGetIntegerHeader(@Nullable String header) {
    if (header == null) {
      throw new IllegalArgumentException("Pagination data from GitLab response is missing");
    } else {
      try {
        return Integer.parseInt(header);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Could not parse pagination number", e);
      }
    }
  }

}
