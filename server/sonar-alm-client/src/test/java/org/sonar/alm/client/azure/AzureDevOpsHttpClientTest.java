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
package org.sonar.alm.client.azure;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.ConstantTimeoutConfiguration;
import org.sonar.alm.client.TimeoutConfiguration;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class AzureDevOpsHttpClientTest {
  public static final String UNABLE_TO_CONTACT_AZURE = "Unable to contact Azure DevOps server, got an unexpected response";
  @Rule
  public LogTester logTester = new LogTester();

  private static final String NON_JSON_PAYLOAD = "non json payload";
  private final MockWebServer server = new MockWebServer();
  private AzureDevOpsHttpClient underTest;

  @Before
  public void prepare() throws IOException {
    server.start();

    TimeoutConfiguration timeoutConfiguration = new ConstantTimeoutConfiguration(10_000);
    underTest = new AzureDevOpsHttpClient(timeoutConfiguration);
  }

  @After
  public void stopServer() throws IOException {
    server.shutdown();
  }

  @Test
  public void check_pat() throws InterruptedException {
    enqueueResponse(200, " { \"count\": 1,\n" +
      "  \"value\": [\n" +
      "    {\n" +
      "      \"id\": \"3311cd05-3f00-4a5e-b47f-df94a9982b6e\",\n" +
      "      \"name\": \"Project\",\n" +
      "      \"description\": \"Project Description\",\n" +
      "      \"url\": \"https://ado.sonarqube.com/DefaultCollection/_apis/projects/3311cd05-3f00-4a5e-b47f-df94a9982b6e\",\n" +
      "      \"state\": \"wellFormed\",\n" +
      "      \"revision\": 63,\n" +
      "      \"visibility\": \"private\"\n" +
      "    }]}");

    underTest.checkPAT(server.url("").toString(), "token");

    RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
    String azureDevOpsUrlCall = request.getRequestUrl().toString();
    assertThat(azureDevOpsUrlCall).isEqualTo(server.url("") + "_apis/projects?api-version=3.0");
    assertThat(request.getMethod()).isEqualTo("GET");

    assertThat(logTester.logs()).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG))
      .contains("check pat : [" + server.url("").toString() + "_apis/projects?api-version=3.0]");
  }

  @Test
  public void check_invalid_pat() {
    enqueueResponse(401);

    String serverUrl = server.url("").toString();
    assertThatThrownBy(() -> underTest.checkPAT(serverUrl, "invalid-token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid personal access token");
  }

  @Test
  public void check_pat_with_server_error() {
    enqueueResponse(500);

    String serverUrl = server.url("").toString();
    assertThatThrownBy(() -> underTest.checkPAT(serverUrl, "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Azure DevOps server");
  }

  @Test
  public void get_projects() throws InterruptedException {
    enqueueResponse(200, " { \"count\": 2,\n" +
      "  \"value\": [\n" +
      "    {\n" +
      "      \"id\": \"3311cd05-3f00-4a5e-b47f-df94a9982b6e\",\n" +
      "      \"name\": \"Project 1\",\n" +
      "      \"description\": \"Project Description\",\n" +
      "      \"url\": \"https://ado.sonarqube.com/DefaultCollection/_apis/projects/3311cd05-3f00-4a5e-b47f-df94a9982b6e\",\n" +
      "      \"state\": \"wellFormed\",\n" +
      "      \"revision\": 63,\n" +
      "      \"visibility\": \"private\"\n" +
      "    }," +
      "{\n" +
      "      \"id\": \"3be0f34d-c931-4ff8-8d37-18a83663bd3c\",\n" +
      "      \"name\": \"Project 2\",\n" +
      "      \"url\": \"https://ado.sonarqube.com/DefaultCollection/_apis/projects/3be0f34d-c931-4ff8-8d37-18a83663bd3c\",\n" +
      "      \"state\": \"wellFormed\",\n" +
      "      \"revision\": 52,\n" +
      "      \"visibility\": \"private\"\n" +
      "    }]}");

    GsonAzureProjectList projects = underTest.getProjects(server.url("").toString(), "token");

    RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
    String azureDevOpsUrlCall = request.getRequestUrl().toString();
    assertThat(azureDevOpsUrlCall).isEqualTo(server.url("") + "_apis/projects?api-version=3.0");
    assertThat(request.getMethod()).isEqualTo("GET");

    assertThat(logTester.logs(LoggerLevel.DEBUG)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG))
      .contains("get projects : [" + server.url("").toString() + "_apis/projects?api-version=3.0]");
    assertThat(projects.getValues()).hasSize(2);
    assertThat(projects.getValues())
      .extracting(GsonAzureProject::getName, GsonAzureProject::getDescription)
      .containsExactly(tuple("Project 1", "Project Description"), tuple("Project 2", null));
  }

  @Test
  public void get_projects_non_json_payload() {
    enqueueResponse(200, NON_JSON_PAYLOAD);

    assertThatThrownBy(() -> underTest.getProjects(server.url("").toString(), "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(UNABLE_TO_CONTACT_AZURE);
  }

  @Test
  public void get_projects_with_invalid_pat() {
    enqueueResponse(401);

    assertThatThrownBy(() -> underTest.getProjects(server.url("").toString(), "invalid-token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid personal access token");
  }

  @Test
  public void get_projects_with_server_error() {
    enqueueResponse(500);

    assertThatThrownBy(() -> underTest.getProjects(server.url("").toString(), "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Azure DevOps server");
  }

  @Test
  public void get_repos_with_project_name() throws InterruptedException {
    enqueueResponse(200, "{\n" +
      "  \"value\": [\n" +
      "    {\n" +
      "      \"id\": \"741248a4-285e-4a6d-af52-1a49d8070638\",\n" +
      "      \"name\": \"Repository 1\",\n" +
      "      \"url\": \"https://ado.sonarqube.com/repositories/\",\n" +
      "      \"project\": {\n" +
      "        \"id\": \"c88ddb32-ced8-420d-ab34-764133038b34\",\n" +
      "        \"name\": \"projectName\",\n" +
      "        \"url\": \"https://ado.sonarqube.com/DefaultCollection/_apis/projects/c88ddb32-ced8-420d-ab34-764133038b34\",\n" +
      "        \"state\": \"wellFormed\",\n" +
      "        \"revision\": 29,\n" +
      "        \"visibility\": \"private\",\n" +
      "        \"lastUpdateTime\": \"2020-11-11T09:38:03.3Z\"\n" +
      "      },\n" +
      "      \"size\": 0\n" +
      "    }\n" +
      "  ],\n" +
      "  \"count\": 1\n" +
      "}");

    GsonAzureRepoList repos = underTest.getRepos(server.url("").toString(), "token", "projectName");

    RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
    String azureDevOpsUrlCall = request.getRequestUrl().toString();
    assertThat(azureDevOpsUrlCall).isEqualTo(server.url("") + "projectName/_apis/git/repositories?api-version=3.0");
    assertThat(request.getMethod()).isEqualTo("GET");

    assertThat(logTester.logs(LoggerLevel.DEBUG)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG))
      .contains("get repos : [" + server.url("").toString() + "projectName/_apis/git/repositories?api-version=3.0]");
    assertThat(repos.getValues()).hasSize(1);
    assertThat(repos.getValues())
      .extracting(GsonAzureRepo::getName, GsonAzureRepo::getUrl, r -> r.getProject().getName())
      .containsExactly(tuple("Repository 1", "https://ado.sonarqube.com/repositories/", "projectName"));
  }

  @Test
  public void get_repos_without_project_name() throws InterruptedException {
    enqueueResponse(200, "{  \"value\": [],  \"count\": 0 }");

    GsonAzureRepoList repos = underTest.getRepos(server.url("").toString(), "token", null);

    RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
    String azureDevOpsUrlCall = request.getRequestUrl().toString();
    assertThat(azureDevOpsUrlCall).isEqualTo(server.url("") + "_apis/git/repositories?api-version=3.0");
    assertThat(request.getMethod()).isEqualTo("GET");
    assertThat(repos.getValues()).isEmpty();
  }

  @Test
  public void get_repos_non_json_payload() {
    enqueueResponse(200, NON_JSON_PAYLOAD);

    assertThatThrownBy(() -> underTest.getRepos(server.url("").toString(), "token", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(UNABLE_TO_CONTACT_AZURE);
  }

  @Test
  public void get_repo() throws InterruptedException {
    enqueueResponse(200, "{ " +
      "  \"id\": \"Repo-Id-1\",\n" +
      "  \"name\": \"Repo-Name-1\",\n" +
      "  \"url\": \"https://ado.sonarqube.com/DefaultCollection/Repo-Id-1\",\n" +
      "  \"project\": {\n" +
      "    \"id\": \"84ea9d51-0c8a-44ad-be92-b2af7fe2c299\",\n" +
      "    \"name\": \"Project-Name\",\n" +
      "    \"description\": \"Project's description\" \n" +
      "  },\n" +
      "  \"defaultBranch\": \"refs/heads/default-branch\",\n" +
      "  \"size\": 0" +
      "}");

    GsonAzureRepo repo = underTest.getRepo(server.url("").toString(), "token", "Project-Name", "Repo-Name-1");

    RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
    String azureDevOpsUrlCall = request.getRequestUrl().toString();
    assertThat(azureDevOpsUrlCall).isEqualTo(server.url("") + "Project-Name/_apis/git/repositories/Repo-Name-1?api-version=3.0");
    assertThat(request.getMethod()).isEqualTo("GET");

    assertThat(logTester.logs(LoggerLevel.DEBUG)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.DEBUG))
      .contains("get repo : [" + server.url("").toString() + "Project-Name/_apis/git/repositories/Repo-Name-1?api-version=3.0]");
    assertThat(repo.getId()).isEqualTo("Repo-Id-1");
    assertThat(repo.getName()).isEqualTo("Repo-Name-1");
    assertThat(repo.getUrl()).isEqualTo("https://ado.sonarqube.com/DefaultCollection/Repo-Id-1");
    assertThat(repo.getProject().getName()).isEqualTo("Project-Name");
    assertThat(repo.getDefaultBranchName()).isEqualTo("default-branch");
  }

  @Test
  public void get_repo_non_json_payload() {
    enqueueResponse(200, NON_JSON_PAYLOAD);

    assertThatThrownBy(() -> underTest.getRepo(server.url("").toString(), "token", "projectName", "repoName"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(UNABLE_TO_CONTACT_AZURE);
  }

  @Test
  public void get_repo_json_error_payload() {
    enqueueResponse(400,
      "{'message':'TF200016: The following project does not exist: projectName. Verify that the name of the project is correct and that the project exists on the specified Azure DevOps Server.'}");

    assertThatThrownBy(() -> underTest.getRepo(server.url("").toString(), "token", "projectName", "repoName"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(
        "Unable to contact Azure DevOps server : TF200016: The following project does not exist: projectName. Verify that the name of the project is correct and that the project exists on the specified Azure DevOps Server.");
  }

  private void enqueueResponse(int responseCode) {
    enqueueResponse(responseCode, "");
  }

  private void enqueueResponse(int responseCode, @Nullable String body) {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setResponseCode(responseCode)
      .setBody(body));
  }

  @Test
  public void trim_url() {
    assertThat(AzureDevOpsHttpClient.getTrimmedUrl("http://localhost:4564/"))
      .isEqualTo("http://localhost:4564");
  }

  @Test
  public void trim_url_without_ending_slash() {
    assertThat(AzureDevOpsHttpClient.getTrimmedUrl("http://localhost:4564"))
      .isEqualTo("http://localhost:4564");
  }

  @Test
  public void trim_null_url() {
    assertThat(AzureDevOpsHttpClient.getTrimmedUrl(null))
      .isNull();
  }

  @Test
  public void trim_empty_url() {
    assertThat(AzureDevOpsHttpClient.getTrimmedUrl(""))
      .isEmpty();
  }
}
