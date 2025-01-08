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
package org.sonar.alm.client.gitlab;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.TimeoutConfiguration;
import org.sonar.api.server.ServerSide;
import org.sonar.auth.gitlab.GsonGroup;
import org.sonar.auth.gitlab.GsonProjectMember;
import org.sonar.auth.gitlab.GsonUser;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.client.OkHttpClientBuilder;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.nio.charset.StandardCharsets.UTF_8;

@ServerSide
public class GitlabApplicationClient {
  private static final Logger LOG = LoggerFactory.getLogger(GitlabApplicationClient.class);
  private static final Gson GSON = new Gson();
  private static final TypeToken<List<GsonGroup>> GITLAB_GROUP = new TypeToken<>() {
  };
  private static final TypeToken<List<GsonUser>> GITLAB_USER = new TypeToken<>() {
  };
  private static final TypeToken<List<GsonProjectMember>> GITLAB_PROJECT_MEMBER = new TypeToken<>() {
  };

  protected static final String PRIVATE_TOKEN = "Private-Token";
  private static final String GITLAB_GROUPS_MEMBERS_ENDPOINT = "/groups/%s/members";
  protected final OkHttpClient client;

  private final GitlabPaginatedHttpClient gitlabPaginatedHttpClient;

  public GitlabApplicationClient(GitlabPaginatedHttpClient gitlabPaginatedHttpClient, TimeoutConfiguration timeoutConfiguration) {
    this.gitlabPaginatedHttpClient = gitlabPaginatedHttpClient;
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
    String url = format("%s/projects", gitlabUrl);

    LOG.debug("get projects : [{}]", url);
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
    String errorMessage = format("Gitlab API call to [%s] failed with error message : [%s]", url, e.getMessage());
    LOG.info(errorMessage, e);
  }

  public void checkToken(String gitlabUrl, String personalAccessToken) {
    String url = format("%s/user", gitlabUrl);

    LOG.debug("get current user : [{}]", url);
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
    String url = format("%s/markdown", gitlabUrl);

    LOG.debug("verify write permission by formating some markdown : [{}]", url);
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
      LOG.error("Gitlab API call to [{}] failed with {} http code. gitlab response content : [{}]", response.request().url(), response.code(), body);
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
    String url = format("%s/projects/%s", gitlabUrl, gitlabProjectId);
    LOG.debug("get project : [{}]", url);
    Request request = new Request.Builder()
      .addHeader(PRIVATE_TOKEN, pat)
      .get()
      .url(url)
      .build();

    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response);
      String body = response.body().string();
      LOG.trace("loading project payload result : [{}]", body);
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
    String url = format("%s/projects?min_access_level=20&id_after=%s&id_before=%s", gitlabUrl, gitlabProjectId - 1,
      gitlabProjectId + 1);
    LOG.debug("get project : [{}]", url);
    Request request = new Request.Builder()
      .addHeader(PRIVATE_TOKEN, pat)
      .get()
      .url(url)
      .build();

    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response);
      String body = response.body().string();
      LOG.trace("loading project payload result : [{}]", body);

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
    String url = format("%s/projects/%s/repository/branches", gitlabUrl, gitlabProjectId);
    LOG.debug("get branches : [{}]", url);
    Request request = new Request.Builder()
      .addHeader(PRIVATE_TOKEN, pat)
      .get()
      .url(url)
      .build();

    try (Response response = client.newCall(request).execute()) {
      checkResponseIsSuccessful(response);
      String body = response.body().string();
      LOG.trace("loading branches payload result : [{}]", body);
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
    String url = format("%s/projects?archived=false&simple=true&membership=true&order_by=name&sort=asc&search=%s%s%s",
      gitlabUrl,
      projectName == null ? "" : urlEncode(projectName),
      pageNumber == null ? "" : format("&page=%d", pageNumber),
      pageSize == null ? "" : format("&per_page=%d", pageSize)
    );

    LOG.debug("get projects : [{}]", url);
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

  public Set<GsonGroup> getGroups(String gitlabUrl, String token) {
    return Set.copyOf(executePaginatedQuery(gitlabUrl, token, "/groups", resp -> GSON.fromJson(resp, GITLAB_GROUP)));
  }

  public Set<GsonUser> getDirectGroupMembers(String gitlabUrl, String token, String groupId) {
    return getMembers(gitlabUrl, token, format(GITLAB_GROUPS_MEMBERS_ENDPOINT, groupId));
  }

  public Set<GsonUser> getAllGroupMembers(String gitlabUrl, String token, String groupId) {
    return getMembers(gitlabUrl, token, format(GITLAB_GROUPS_MEMBERS_ENDPOINT + "/all", groupId));
  }

  private Set<GsonUser> getMembers(String gitlabUrl, String token, String endpoint) {
    return Set.copyOf(executePaginatedQuery(gitlabUrl, token, endpoint, resp -> GSON.fromJson(resp, GITLAB_USER)));
  }

  public Set<GsonProjectMember> getAllProjectMembers(String gitlabUrl, String token, long projectId) {
    String url = format("/projects/%s/members/all", projectId);
    return Set.copyOf(executePaginatedQuery(gitlabUrl, token, url, resp -> GSON.fromJson(resp, GITLAB_PROJECT_MEMBER)));
  }

  private <E> List<E> executePaginatedQuery(String appUrl, String token, String query, Function<String, List<E>> responseDeserializer) {
    GitlabToken gitlabToken = new GitlabToken(token);
    return gitlabPaginatedHttpClient.get(appUrl, gitlabToken, query, responseDeserializer);
  }
}
