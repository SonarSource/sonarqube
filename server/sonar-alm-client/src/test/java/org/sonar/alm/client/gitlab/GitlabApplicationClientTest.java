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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nullable;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.event.Level;
import org.sonar.alm.client.ConstantTimeoutConfiguration;
import org.sonar.alm.client.TimeoutConfiguration;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.auth.gitlab.GsonGroup;
import org.sonar.auth.gitlab.GsonMemberRole;
import org.sonar.auth.gitlab.GsonProjectMember;
import org.sonar.auth.gitlab.GsonUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GitlabApplicationClientTest {

  @Rule
  public LogTester logTester = new LogTester();

  private final GitlabPaginatedHttpClient gitlabPaginatedHttpClient = mock();

  private final MockWebServer server = new MockWebServer();
  private GitlabApplicationClient underTest;
  private String gitlabUrl;

  @Before
  public void prepare() throws IOException {
    server.start();
    String urlWithEndingSlash = server.url("").toString();
    gitlabUrl = urlWithEndingSlash.substring(0, urlWithEndingSlash.length() - 1);

    TimeoutConfiguration timeoutConfiguration = new ConstantTimeoutConfiguration(10_000);
    underTest = new GitlabApplicationClient(gitlabPaginatedHttpClient, timeoutConfiguration);
  }

  @After
  public void stopServer() throws IOException {
    server.shutdown();
  }

  @Test
  public void should_throw_IllegalArgumentException_when_token_is_revoked() {
    MockResponse response = new MockResponse()
      .setResponseCode(401)
      .setBody("{\"error\":\"invalid_token\",\"error_description\":\"Token was revoked. You have to re-authorize from the user.\"}");
    server.enqueue(response);

    assertThatThrownBy(() -> underTest.searchProjects(gitlabUrl, "pat", "example", 1, 2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Your GitLab token was revoked");
  }

  @Test
  public void should_throw_IllegalArgumentException_when_token_insufficient_scope() {
    MockResponse response = new MockResponse()
      .setResponseCode(403)
      .setBody("{\"error\":\"insufficient_scope\"," +
        "\"error_description\":\"The request requires higher privileges than provided by the access token.\"," +
        "\"scope\":\"api read_api\"}");
    server.enqueue(response);

    assertThatThrownBy(() -> underTest.searchProjects(gitlabUrl, "pat", "example", 1, 2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Your GitLab token has insufficient scope");
  }

  @Test
  public void should_throw_IllegalArgumentException_when_invalide_json_in_401_response() {
    MockResponse response = new MockResponse()
      .setResponseCode(401)
      .setBody("error in pat");
    server.enqueue(response);

    assertThatThrownBy(() -> underTest.searchProjects(gitlabUrl, "pat", "example", 1, 2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid personal access token");
  }

  @Test
  public void should_throw_IllegalArgumentException_when_redirected() {
    MockResponse response = new MockResponse()
      .setResponseCode(308);
    server.enqueue(response);

    assertThatThrownBy(() -> underTest.searchProjects(gitlabUrl, "pat", "example", 1, 2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Request was redirected, please provide the correct URL");
  }

  @Test
  public void get_project() {
    MockResponse response = new MockResponse()
      .setResponseCode(200)
      .setBody("""
        {
            "id": 12345,
            "name": "SonarQube example 1",
            "name_with_namespace": "SonarSource / SonarQube / SonarQube example 1",
            "path": "sonarqube-example-1",
            "path_with_namespace": "sonarsource/sonarqube/sonarqube-example-1",
            "visibility": "visibilityFromGitLab",
            "web_url": "https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-1"
          }
        """);
    server.enqueue(response);

    assertThat(underTest.getProject(gitlabUrl, "pat", 12345L))
      .extracting(Project::getId, Project::getName, Project::getVisibility)
      .containsExactly(12345L, "SonarQube example 1", "visibilityFromGitLab");
  }

  @Test
  public void get_project_fail_if_non_json_payload() {
    MockResponse response = new MockResponse()
      .setResponseCode(200)
      .setBody("non json payload");
    server.enqueue(response);

    assertThatThrownBy(() -> underTest.getProject(gitlabUrl, "pat", 12345L))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Could not parse GitLab answer to retrieve a project. Got a non-json payload as result.");
  }

  @Test
  public void get_branches() {
    MockResponse response = new MockResponse()
      .setResponseCode(200)
      .setBody("""
        [{
            "name": "main",
            "default": true
        },{
            "name": "other",
            "default": false
        }]""");
    server.enqueue(response);

    assertThat(underTest.getBranches(gitlabUrl, "pat", 12345L))
      .extracting(GitLabBranch::getName, GitLabBranch::isDefault)
      .containsExactly(
        tuple("main", true),
        tuple("other", false));
  }

  @Test
  public void get_branches_fail_if_non_json_payload() {
    MockResponse response = new MockResponse()
      .setResponseCode(200)
      .setBody("non json payload");
    server.enqueue(response);

    String instanceUrl = gitlabUrl;
    assertThatThrownBy(() -> underTest.getBranches(instanceUrl, "pat", 12345L))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Could not parse GitLab answer to retrieve project branches. Got a non-json payload as result.");
  }

  @Test
  public void get_branches_fail_if_exception() throws IOException {
    server.shutdown();

    String instanceUrl = gitlabUrl;
    assertThatThrownBy(() -> underTest.getBranches(instanceUrl, "pat", 12345L))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failed to connect to");
  }

  @Test
  public void search_projects() throws InterruptedException {
    MockResponse projects = new MockResponse()
      .setResponseCode(200)
      .setBody("""
        [
          {
            "id": 1,
            "name": "SonarQube example 1",
            "name_with_namespace": "SonarSource / SonarQube / SonarQube example 1",
            "path": "sonarqube-example-1",
            "path_with_namespace": "sonarsource/sonarqube/sonarqube-example-1",
            "web_url": "https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-1"
          },
          {
            "id": 2,
            "name": "SonarQube example 2",
            "name_with_namespace": "SonarSource / SonarQube / SonarQube example 2",
            "path": "sonarqube-example-2",
            "path_with_namespace": "sonarsource/sonarqube/sonarqube-example-2",
            "web_url": "https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-2"
          },
          {
            "id": 3,
            "name": "SonarQube example 3",
            "name_with_namespace": "SonarSource / SonarQube / SonarQube example 3",
            "path": "sonarqube-example-3",
            "path_with_namespace": "sonarsource/sonarqube/sonarqube-example-3",
            "web_url": "https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-3"
          }
        ]""");
    projects.addHeader("X-Page", 1);
    projects.addHeader("X-Per-Page", 10);
    projects.addHeader("X-Total", 3);
    server.enqueue(projects);

    ProjectList projectList = underTest.searchProjects(gitlabUrl, "pat", "example", 1, 10);

    assertThat(projectList.getPageNumber()).isOne();
    assertThat(projectList.getPageSize()).isEqualTo(10);
    assertThat(projectList.getTotal()).isEqualTo(3);

    assertThat(projectList.getProjects()).hasSize(3);
    assertThat(projectList.getProjects()).extracting(
      Project::getId, Project::getName, Project::getNameWithNamespace, Project::getPath, Project::getPathWithNamespace, Project::getWebUrl).containsExactly(
        tuple(1L, "SonarQube example 1", "SonarSource / SonarQube / SonarQube example 1", "sonarqube-example-1", "sonarsource/sonarqube/sonarqube-example-1",
          "https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-1"),
        tuple(2L, "SonarQube example 2", "SonarSource / SonarQube / SonarQube example 2", "sonarqube-example-2", "sonarsource/sonarqube/sonarqube-example-2",
          "https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-2"),
        tuple(3L, "SonarQube example 3", "SonarSource / SonarQube / SonarQube example 3", "sonarqube-example-3", "sonarsource/sonarqube/sonarqube-example-3",
          "https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-3"));

    RecordedRequest projectGitlabRequest = server.takeRequest(10, TimeUnit.SECONDS);
    String gitlabUrlCall = projectGitlabRequest.getRequestUrl().toString();
    assertThat(gitlabUrlCall).isEqualTo(server.url("") + "projects?archived=false&simple=true&membership=true&order_by=name&sort=asc&search=example&page=1&per_page=10");
    assertThat(projectGitlabRequest.getMethod()).isEqualTo("GET");
  }

  @Test
  public void search_projects_dont_fail_if_no_x_total() throws InterruptedException {
    MockResponse projects = new MockResponse()
      .setResponseCode(200)
      .setBody("""
        [
          {
            "id": 1,
            "name": "SonarQube example 1",
            "name_with_namespace": "SonarSource / SonarQube / SonarQube example 1",
            "path": "sonarqube-example-1",
            "path_with_namespace": "sonarsource/sonarqube/sonarqube-example-1",
            "web_url": "https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-1"
          }\
        ]""");
    projects.addHeader("X-Page", 1);
    projects.addHeader("X-Per-Page", 10);
    server.enqueue(projects);

    ProjectList projectList = underTest.searchProjects(gitlabUrl, "pat", "example", 1, 10);

    assertThat(projectList.getPageNumber()).isOne();
    assertThat(projectList.getPageSize()).isEqualTo(10);
    assertThat(projectList.getTotal()).isNull();

    assertThat(projectList.getProjects()).hasSize(1);
    assertThat(projectList.getProjects()).extracting(
      Project::getId, Project::getName, Project::getNameWithNamespace, Project::getPath, Project::getPathWithNamespace, Project::getWebUrl).containsExactly(
        tuple(1L, "SonarQube example 1", "SonarSource / SonarQube / SonarQube example 1", "sonarqube-example-1", "sonarsource/sonarqube/sonarqube-example-1",
          "https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-1"));

    RecordedRequest projectGitlabRequest = server.takeRequest(10, TimeUnit.SECONDS);
    String gitlabUrlCall = projectGitlabRequest.getRequestUrl().toString();
    assertThat(gitlabUrlCall).isEqualTo(server.url("") + "projects?archived=false&simple=true&membership=true&order_by=name&sort=asc&search=example&page=1&per_page=10");
    assertThat(projectGitlabRequest.getMethod()).isEqualTo("GET");
  }

  @Test
  public void search_projects_with_case_insensitive_pagination_headers() throws InterruptedException {
    MockResponse projects1 = new MockResponse()
      .setResponseCode(200)
      .setBody("""
        [
          {
            "id": 1,
            "name": "SonarQube example 1",
            "name_with_namespace": "SonarSource / SonarQube / SonarQube example 1",
            "path": "sonarqube-example-1",
            "path_with_namespace": "sonarsource/sonarqube/sonarqube-example-1",
            "web_url": "https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-1"
          }\
        ]""");
    projects1.addHeader("x-page", 1);
    projects1.addHeader("x-Per-page", 1);
    projects1.addHeader("X-Total", 2);
    server.enqueue(projects1);

    ProjectList projectList = underTest.searchProjects(gitlabUrl, "pat", "example", 1, 10);

    assertThat(projectList.getPageNumber()).isOne();
    assertThat(projectList.getPageSize()).isOne();
    assertThat(projectList.getTotal()).isEqualTo(2);

    assertThat(projectList.getProjects()).hasSize(1);
    assertThat(projectList.getProjects()).extracting(
      Project::getId, Project::getName, Project::getNameWithNamespace, Project::getPath, Project::getPathWithNamespace, Project::getWebUrl).containsExactly(
        tuple(1L, "SonarQube example 1", "SonarSource / SonarQube / SonarQube example 1", "sonarqube-example-1", "sonarsource/sonarqube/sonarqube-example-1",
          "https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-1"));

    RecordedRequest projectGitlabRequest = server.takeRequest(10, TimeUnit.SECONDS);
    String gitlabUrlCall = projectGitlabRequest.getRequestUrl().toString();
    assertThat(gitlabUrlCall).isEqualTo(server.url("") + "projects?archived=false&simple=true&membership=true&order_by=name&sort=asc&search=example&page=1&per_page=10");
    assertThat(projectGitlabRequest.getMethod()).isEqualTo("GET");
  }

  @Test
  public void search_projects_projectName_param_should_be_encoded() throws InterruptedException {
    MockResponse projects = new MockResponse()
      .setResponseCode(200)
      .setBody("[]");
    projects.addHeader("X-Page", 1);
    projects.addHeader("X-Per-Page", 10);
    projects.addHeader("X-Total", 0);
    server.enqueue(projects);

    ProjectList projectList = underTest.searchProjects(gitlabUrl, "pat", "&page=<script>alert('nasty')</script>", 1, 10);

    RecordedRequest projectGitlabRequest = server.takeRequest(10, TimeUnit.SECONDS);
    String gitlabUrlCall = projectGitlabRequest.getRequestUrl().toString();
    assertThat(projectList.getProjects()).isEmpty();
    assertThat(gitlabUrlCall).isEqualTo(
      server.url("")
        + "projects?archived=false&simple=true&membership=true&order_by=name&sort=asc&search=%26page%3D%3Cscript%3Ealert%28%27nasty%27%29%3C%2Fscript%3E&page=1&per_page=10");
    assertThat(projectGitlabRequest.getMethod()).isEqualTo("GET");
  }

  @Test
  public void search_projects_projectName_param_null_should_pass_empty_string() throws InterruptedException {
    MockResponse projects = new MockResponse()
      .setResponseCode(200)
      .setBody("[]");
    projects.addHeader("X-Page", 1);
    projects.addHeader("X-Per-Page", 10);
    projects.addHeader("X-Total", 0);
    server.enqueue(projects);

    ProjectList projectList = underTest.searchProjects(gitlabUrl, "pat", null, 1, 10);

    RecordedRequest projectGitlabRequest = server.takeRequest(10, TimeUnit.SECONDS);
    String gitlabUrlCall = projectGitlabRequest.getRequestUrl().toString();
    assertThat(projectList.getProjects()).isEmpty();
    assertThat(gitlabUrlCall).isEqualTo(
      server.url("") + "projects?archived=false&simple=true&membership=true&order_by=name&sort=asc&search=&page=1&per_page=10");
    assertThat(projectGitlabRequest.getMethod()).isEqualTo("GET");
  }

  @Test
  public void get_project_details() throws InterruptedException {
    MockResponse projectResponse = new MockResponse()
      .setResponseCode(200)
      .setBody("""
        {\
          "id": 1234,\
          "name": "SonarQube example 2",\
          "name_with_namespace": "SonarSource / SonarQube / SonarQube example 2",\
          "path": "sonarqube-example-2",\
          "path_with_namespace": "sonarsource/sonarqube/sonarqube-example-2",\
          "web_url": "https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-2"\
        }""");

    server.enqueue(projectResponse);

    Project project = underTest.getProject(gitlabUrl, "pat", 1234L);

    RecordedRequest projectGitlabRequest = server.takeRequest(10, TimeUnit.SECONDS);
    String gitlabUrlCall = projectGitlabRequest.getRequestUrl().toString();

    assertThat(project).isNotNull();

    assertThat(gitlabUrlCall).isEqualTo(
      server.url("") + "projects/1234");
    assertThat(projectGitlabRequest.getMethod()).isEqualTo("GET");
  }

  @Test
  public void get_reporter_level_access_project() throws InterruptedException {
    MockResponse projectResponse = new MockResponse()
      .setResponseCode(200)
      .setBody("[{"
        + "  \"id\": 1234,"
        + "  \"name\": \"SonarQube example 2\","
        + "  \"name_with_namespace\": \"SonarSource / SonarQube / SonarQube example 2\","
        + "  \"path\": \"sonarqube-example-2\","
        + "  \"path_with_namespace\": \"sonarsource/sonarqube/sonarqube-example-2\","
        + "  \"web_url\": \"https://example.gitlab.com/sonarsource/sonarqube/sonarqube-example-2\""
        + "}]");

    server.enqueue(projectResponse);

    Optional<Project> project = underTest.getReporterLevelAccessProject(gitlabUrl, "pat", 1234L);

    RecordedRequest projectGitlabRequest = server.takeRequest(10, TimeUnit.SECONDS);
    String gitlabUrlCall = projectGitlabRequest.getRequestUrl().toString();

    assertThat(project).isNotNull();

    assertThat(gitlabUrlCall).isEqualTo(
      server.url("") + "projects?min_access_level=20&id_after=1233&id_before=1235");
    assertThat(projectGitlabRequest.getMethod()).isEqualTo("GET");
  }

  @Test
  public void search_projects_fail_if_could_not_parse_pagination_number() {
    MockResponse projects = new MockResponse()
      .setResponseCode(200)
      .setBody("[ ]");
    projects.addHeader("X-Page", "bad-page-number");
    projects.addHeader("X-Per-Page", "bad-per-page-number");
    projects.addHeader("X-Total", "bad-total-number");
    server.enqueue(projects);

    assertThatThrownBy(() -> underTest.searchProjects(gitlabUrl, "pat", "example", 1, 10))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Could not parse pagination number");
  }

  @Test
  public void search_projects_fail_if_pagination_data_not_returned() {
    MockResponse projects = new MockResponse()
      .setResponseCode(200)
      .setBody("[ ]");
    server.enqueue(projects);

    assertThatThrownBy(() -> underTest.searchProjects(gitlabUrl, "pat", "example", 1, 10))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Pagination data from GitLab response is missing");
  }

  @Test
  public void throws_ISE_when_get_projects_not_http_200() {
    MockResponse projects = new MockResponse()
      .setResponseCode(500)
      .setBody("test");
    server.enqueue(projects);

    assertThatThrownBy(() -> underTest.searchProjects(gitlabUrl, "pat", "example", 1, 2))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Could not get projects from GitLab instance");
  }

  @Test
  public void fail_check_read_permission_with_unexpected_io_exception_with_detailed_log() throws IOException {
    server.shutdown();

    assertThatThrownBy(() -> underTest.checkReadPermission(gitlabUrl, "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Could not validate GitLab read permission. Got an unexpected answer.");
    assertThat(logTester.logs(Level.INFO).get(0))
      .contains("Gitlab API call to [" + server.url("/projects") + "] " +
        "failed with error message : [Failed to connect to ")
      .contains(server.getHostName());
  }

  @Test
  public void fail_check_token_with_unexpected_io_exception_with_detailed_log() throws IOException {
    server.shutdown();

    assertThatThrownBy(() -> underTest.checkToken(gitlabUrl, "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Could not validate GitLab token. Got an unexpected answer.");
    assertThat(logTester.logs(Level.INFO).get(0))
      .contains("Gitlab API call to [" + server.url("user") + "] " +
        "failed with error message : [Failed to connect to ")
      .contains(server.getHostName());
  }

  @Test
  public void fail_check_write_permission_with_unexpected_io_exception_with_detailed_log() throws IOException {
    server.shutdown();

    assertThatThrownBy(() -> underTest.checkWritePermission(gitlabUrl, "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Could not validate GitLab write permission. Got an unexpected answer.");
    assertThat(logTester.logs(Level.INFO).get(0))
      .contains("Gitlab API call to [" + server.url("/markdown") + "] " +
        "failed with error message : [Failed to connect to ")
      .contains(server.getHostName());
  }

  @Test
  public void fail_get_project_with_unexpected_io_exception_with_detailed_log() throws IOException {
    server.shutdown();

    assertThatThrownBy(() -> underTest.getProject(gitlabUrl, "token", 0L))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failed to connect to");
    assertThat(logTester.logs(Level.INFO).get(0))
      .contains("Gitlab API call to [" + server.url("/projects/0") + "] " +
        "failed with error message : [Failed to connect to ")
      .contains(server.getHostName());
  }

  @Test
  public void fail_get_branches_with_unexpected_io_exception_with_detailed_log() throws IOException {
    server.shutdown();

    assertThatThrownBy(() -> underTest.getBranches(gitlabUrl, "token", 0L))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failed to connect to ")
      .hasMessageContaining(server.getHostName());
    assertThat(logTester.logs(Level.INFO).get(0))
      .contains("Gitlab API call to [" + server.url("/projects/0/repository/branches") + "] " +
        "failed with error message : [Failed to connect to ")
      .contains(server.getHostName());
  }

  @Test
  public void fail_search_projects_with_unexpected_io_exception_with_detailed_log() throws IOException {
    server.shutdown();

    assertThatThrownBy(() -> underTest.searchProjects(gitlabUrl, "token", null, 1, 1))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failed to connect to");
    assertThat(logTester.logs(Level.INFO).get(0))
      .contains(
        "Gitlab API call to [" + server.url("/projects?archived=false&simple=true&membership=true&order_by=name&sort=asc&search=&page=1&per_page=1")
          + "] " +
          "failed with error message : [Failed to connect to ")
      .contains( server.getHostName());
  }

  @Test
  public void getGroups_whenCallIsInError_rethrows() throws IOException {
    String token = "token-toto";
    GitlabToken gitlabToken = new GitlabToken(token);
    when(gitlabPaginatedHttpClient.get(eq(gitlabUrl), eq(gitlabToken), eq("/groups"), any())).thenThrow(new IllegalStateException("exception"));

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.getGroups(gitlabUrl, token))
      .withMessage("exception");
  }

  @Test
  public void getGroups_whenCallIsSuccessful_deserializesAndReturnsCorrectlyGroups() throws IOException {
    ArgumentCaptor<Function<String, List<GsonGroup>>> deserializerCaptor = ArgumentCaptor.forClass(Function.class);

    String token = "token-toto";
    GitlabToken gitlabToken = new GitlabToken(token);
    List<GsonGroup> expectedGroups = expectedGroups();
    when(gitlabPaginatedHttpClient.get(eq(gitlabUrl), eq(gitlabToken), eq("/groups"), deserializerCaptor.capture())).thenReturn(expectedGroups);

    Set<GsonGroup> groups = underTest.getGroups(gitlabUrl, token);
    assertThat(groups).containsExactlyInAnyOrderElementsOf(expectedGroups);

    String responseContent = getResponseContent("groups-full-response.json");

    List<GsonGroup> deserializedGroups = deserializerCaptor.getValue().apply(responseContent);
    assertThat(deserializedGroups).usingRecursiveComparison().isEqualTo(expectedGroups);
  }

  private static List<GsonGroup> expectedGroups() {
    GsonGroup gsonGroup = createGsonGroup("56232243", "sonarsource/cfamily", "this is a long description");
    GsonGroup gsonGroup2 = createGsonGroup("78902256", "sonarsource/sonarqube/mmf-3052-ant1", "");
    return List.of(gsonGroup, gsonGroup2);
  }

  private static GsonGroup createGsonGroup(String number, String fullPath, String description) {
    GsonGroup gsonGroup = mock(GsonGroup.class);
    when(gsonGroup.getId()).thenReturn(number);
    when(gsonGroup.getFullPath()).thenReturn(fullPath);
    return gsonGroup;
  }

  @Test
  public void getDirectGroupMembers_whenCallIsInError_rethrows() {
    String token = "token-toto";
    GitlabToken gitlabToken = new GitlabToken(token);
    when(gitlabPaginatedHttpClient.get(eq(gitlabUrl), eq(gitlabToken), eq("/groups/42/members"), any())).thenThrow(new IllegalStateException("exception"));

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.getDirectGroupMembers(gitlabUrl, token, "42"))
      .withMessage("exception");
  }

  @Test
  public void getDirectGroupMembers_whenCallIsSuccessful_deserializesAndReturnsCorrectlyGroupMembers() throws IOException {
    ArgumentCaptor<Function<String, List<GsonUser>>> deserializerCaptor = ArgumentCaptor.forClass(Function.class);

    String token = "token-toto";
    GitlabToken gitlabToken = new GitlabToken(token);
    List<GsonUser> expectedGroupMembers = expectedGroupMembers();
    when(gitlabPaginatedHttpClient.get(eq(gitlabUrl), eq(gitlabToken), eq("/groups/42/members"), deserializerCaptor.capture())).thenReturn(expectedGroupMembers);

    Set<GsonUser> actualGroupMembers = underTest.getDirectGroupMembers(gitlabUrl, token, "42");
    assertThat(actualGroupMembers).containsExactlyInAnyOrderElementsOf(expectedGroupMembers);

    String responseContent = getResponseContent("group-members-full-response.json");

    List<GsonUser> deserializedUsers = deserializerCaptor.getValue().apply(responseContent);
    assertThat(deserializedUsers).usingRecursiveComparison().isEqualTo(expectedGroupMembers);
  }

  @Test
  public void getDirectGroupMembersWithInheritedMembers_whenCallIsInError_rethrows() {
    String token = "token-toto";
    GitlabToken gitlabToken = new GitlabToken(token);
    when(gitlabPaginatedHttpClient.get(eq(gitlabUrl), eq(gitlabToken), eq("/groups/42/members/all"), any())).thenThrow(new IllegalStateException("exception"));

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.getAllGroupMembers(gitlabUrl, token, "42"))
      .withMessage("exception");
  }

  @Test
  public void getAllGroupMembers_whenCallIsSuccessful_deserializesAndReturnsCorrectlyGroupMembers() throws IOException {
    ArgumentCaptor<Function<String, List<GsonUser>>> deserializerCaptor = ArgumentCaptor.forClass(Function.class);

    String token = "token-toto";
    GitlabToken gitlabToken = new GitlabToken(token);
    List<GsonUser> expectedGroupMembers = expectedGroupMembers();
    when(gitlabPaginatedHttpClient.get(eq(gitlabUrl), eq(gitlabToken), eq("/groups/42/members/all"), deserializerCaptor.capture())).thenReturn(expectedGroupMembers);

    Set<GsonUser> actualGroupMembers = underTest.getAllGroupMembers(gitlabUrl, token, "42");
    assertThat(actualGroupMembers).containsExactlyInAnyOrderElementsOf(expectedGroupMembers);

    String responseContent = getResponseContent("group-members-full-response.json");

    List<GsonUser> deserializedUsers = deserializerCaptor.getValue().apply(responseContent);
    assertThat(deserializedUsers).usingRecursiveComparison().isEqualTo(expectedGroupMembers);
  }

  private static List<GsonUser> expectedGroupMembers() {
    GsonUser user1 = createGsonUser(12818153, "aurelien-poscia-sonarsource", "Aurelien", 50);
    GsonUser user2 = createGsonUser(10941672, "antoine.vigneau", "Antoine Vigneau", 30);
    GsonUser user3 = createGsonUser(13569073, "wojciech.wajerowicz.sonarsource", "Wojciech Wajerowicz", 30);
    return List.of(user1, user2, user3);
  }

  private static GsonUser createGsonUser(int id, String username, String name, int accessLevel) {
    GsonUser gsonUser = mock();
    when(gsonUser.getId()).thenReturn((long) id);
    when(gsonUser.getUsername()).thenReturn(username);
    when(gsonUser.getName()).thenReturn(name);
    when(gsonUser.getAccessLevel()).thenReturn(accessLevel);
    return gsonUser;
  }

  @Test
  public void getAllProjectMembers_whenCallIsSuccesfull_deserializesAndReturnsCorrectlyProjectsMembers() throws IOException {
    ArgumentCaptor<Function<String, List<GsonProjectMember>>> deserializerCaptor = ArgumentCaptor.forClass(Function.class);

    String token = "token-toto";
    GitlabToken gitlabToken = new GitlabToken(token);
    List<GsonProjectMember> expectedProjectMembers = expectedProjectMembers();
    when(gitlabPaginatedHttpClient.get(eq(gitlabUrl), eq(gitlabToken), eq("/projects/42/members/all"), deserializerCaptor.capture())).thenReturn(expectedProjectMembers);

    Set<GsonProjectMember> actualProjectMembers = underTest.getAllProjectMembers(gitlabUrl, token, 42);
    assertThat(actualProjectMembers).containsExactlyInAnyOrderElementsOf(expectedProjectMembers);

    String responseContent = getResponseContent("project-members-full-response.json");

    List<GsonProjectMember> deserializedProjectMembers = deserializerCaptor.getValue().apply(responseContent);
    assertThat(deserializedProjectMembers).isEqualTo(expectedProjectMembers);

  }

  @Test
  public void getAllProjectMembers_whenCallIsInError_rethrows() {
    String token = "token-toto";
    GitlabToken gitlabToken = new GitlabToken(token);
    when(gitlabPaginatedHttpClient.get(eq(gitlabUrl), eq(gitlabToken), eq("/projects/42/members/all"), any())).thenThrow(new IllegalStateException("exception"));

    assertThatIllegalStateException()
      .isThrownBy(() -> underTest.getAllProjectMembers(gitlabUrl, token, 42))
      .withMessage("exception");
  }

  private static List<GsonProjectMember> expectedProjectMembers() {
    GsonProjectMember user1 = createGsonProjectMember(12818153, 5, null);
    GsonProjectMember user2 = createGsonProjectMember(22330087, 50, null);
    GsonProjectMember user3 = createGsonProjectMember(20824381, 40, new GsonMemberRole("custom-role"));
    return List.of(user1, user2, user3);
  }

  private static GsonProjectMember createGsonProjectMember(int id, int accessLevel, @Nullable GsonMemberRole gsonMemberRole) {
    return new GsonProjectMember(id, accessLevel, gsonMemberRole);
  }

  private static String getResponseContent(String path) throws IOException {
    return IOUtils.toString(GitlabApplicationClientTest.class.getResourceAsStream(path), StandardCharsets.UTF_8);
  }

}
