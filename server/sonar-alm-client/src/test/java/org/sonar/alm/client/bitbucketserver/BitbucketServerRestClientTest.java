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

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.alm.client.ConstantTimeoutConfiguration;
import org.sonar.api.utils.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class BitbucketServerRestClientTest {
  private final MockWebServer server = new MockWebServer();
  private static final String REPOS_BODY = "{\n" +
    "  \"isLastPage\": true,\n" +
    "  \"values\": [\n" +
    "    {\n" +
    "      \"slug\": \"banana\",\n" +
    "      \"id\": 2,\n" +
    "      \"name\": \"banana\",\n" +
    "      \"project\": {\n" +
    "        \"key\": \"HOY\",\n" +
    "        \"id\": 2,\n" +
    "        \"name\": \"hoy\"\n" +
    "      }\n" +
    "    },\n" +
    "    {\n" +
    "      \"slug\": \"potato\",\n" +
    "      \"id\": 1,\n" +
    "      \"name\": \"potato\",\n" +
    "      \"project\": {\n" +
    "        \"key\": \"HEY\",\n" +
    "        \"id\": 1,\n" +
    "        \"name\": \"hey\"\n" +
    "      }\n" +
    "    }\n" +
    "  ]\n" +
    "}";

  @Rule
  public LogTester logTester = new LogTester();

  private BitbucketServerRestClient underTest;

  @Before
  public void prepare() throws IOException {
    server.start();

    underTest = new BitbucketServerRestClient(new ConstantTimeoutConfiguration(500));
  }

  @After
  public void stopServer() throws IOException {
    server.shutdown();
  }

  @Test
  public void get_repos() {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody("{\n" +
        "  \"isLastPage\": true,\n" +
        "  \"values\": [\n" +
        "    {\n" +
        "      \"slug\": \"banana\",\n" +
        "      \"id\": 2,\n" +
        "      \"name\": \"banana\",\n" +
        "      \"project\": {\n" +
        "        \"key\": \"HOY\",\n" +
        "        \"id\": 2,\n" +
        "        \"name\": \"hoy\"\n" +
        "      }\n" +
        "    },\n" +
        "    {\n" +
        "      \"slug\": \"potato\",\n" +
        "      \"id\": 1,\n" +
        "      \"name\": \"potato\",\n" +
        "      \"project\": {\n" +
        "        \"key\": \"HEY\",\n" +
        "        \"id\": 1,\n" +
        "        \"name\": \"hey\"\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}"));

    RepositoryList gsonBBSRepoList = underTest.getRepos(server.url("/").toString(), "token", "", "");
    assertThat(gsonBBSRepoList.isLastPage()).isTrue();
    assertThat(gsonBBSRepoList.getValues()).hasSize(2);
    assertThat(gsonBBSRepoList.getValues()).extracting(Repository::getId, Repository::getName, Repository::getSlug,
      g -> g.getProject().getId(), g -> g.getProject().getKey(), g -> g.getProject().getName())
      .containsExactlyInAnyOrder(
        tuple(2L, "banana", "banana", 2L, "HOY", "hoy"),
        tuple(1L, "potato", "potato", 1L, "HEY", "hey"));
  }

  @Test
  public void get_recent_repos() {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody("{\n" +
        "  \"isLastPage\": true,\n" +
        "  \"values\": [\n" +
        "    {\n" +
        "      \"slug\": \"banana\",\n" +
        "      \"id\": 2,\n" +
        "      \"name\": \"banana\",\n" +
        "      \"project\": {\n" +
        "        \"key\": \"HOY\",\n" +
        "        \"id\": 2,\n" +
        "        \"name\": \"hoy\"\n" +
        "      }\n" +
        "    },\n" +
        "    {\n" +
        "      \"slug\": \"potato\",\n" +
        "      \"id\": 1,\n" +
        "      \"name\": \"potato\",\n" +
        "      \"project\": {\n" +
        "        \"key\": \"HEY\",\n" +
        "        \"id\": 1,\n" +
        "        \"name\": \"hey\"\n" +
        "      }\n" +
        "    }\n" +
        "  ]\n" +
        "}"));

    RepositoryList gsonBBSRepoList = underTest.getRecentRepo(server.url("/").toString(), "token");
    assertThat(gsonBBSRepoList.isLastPage()).isTrue();
    assertThat(gsonBBSRepoList.getValues()).hasSize(2);
    assertThat(gsonBBSRepoList.getValues()).extracting(Repository::getId, Repository::getName, Repository::getSlug,
      g -> g.getProject().getId(), g -> g.getProject().getKey(), g -> g.getProject().getName())
      .containsExactlyInAnyOrder(
        tuple(2L, "banana", "banana", 2L, "HOY", "hoy"),
        tuple(1L, "potato", "potato", 1L, "HEY", "hey"));
  }

  @Test
  public void get_repo() {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody(
        "    {" +
          "      \"slug\": \"banana-slug\"," +
          "      \"id\": 2,\n" +
          "      \"name\": \"banana\"," +
          "      \"project\": {\n" +
          "        \"key\": \"HOY\"," +
          "        \"id\": 3,\n" +
          "        \"name\": \"hoy\"" +
          "      }" +
          "    }"));

    Repository repository = underTest.getRepo(server.url("/").toString(), "token", "", "");
    assertThat(repository.getId()).isEqualTo(2L);
    assertThat(repository.getName()).isEqualTo("banana");
    assertThat(repository.getSlug()).isEqualTo("banana-slug");
    assertThat(repository.getProject())
      .extracting(Project::getId, Project::getKey, Project::getName)
      .contains(3L, "HOY", "hoy");
  }

  @Test
  public void get_projects() {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody("{\n" +
        "  \"isLastPage\": true,\n" +
        "  \"values\": [\n" +
        "    {\n" +
        "      \"key\": \"HEY\",\n" +
        "      \"id\": 1,\n" +
        "      \"name\": \"hey\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"key\": \"HOY\",\n" +
        "      \"id\": 2,\n" +
        "      \"name\": \"hoy\"\n" +
        "    }\n" +
        "  ]\n" +
        "}"));

    final ProjectList gsonBBSProjectList = underTest.getProjects(server.url("/").toString(), "token");
    assertThat(gsonBBSProjectList.getValues()).hasSize(2);
    assertThat(gsonBBSProjectList.getValues()).extracting(Project::getId, Project::getKey, Project::getName)
      .containsExactlyInAnyOrder(
        tuple(1L, "HEY", "hey"),
        tuple(2L, "HOY", "hoy"));
  }

  @Test
  public void getBranches_given0Branches_returnEmptyList() {
    String bodyWith0Branches = "{\n" +
      "  \"size\": 0,\n" +
      "  \"limit\": 25,\n" +
      "  \"isLastPage\": true,\n" +
      "  \"values\": [],\n" +
      "  \"start\": 0\n" +
      "}";
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody(bodyWith0Branches));

    BranchesList branches = underTest.getBranches(server.url("/").toString(), "token", "projectSlug", "repoSlug");

    assertThat(branches.getBranches()).isEmpty();
  }

  @Test
  public void getBranches_given1Branch_returnListWithOneBranch() {
    String bodyWith1Branch = "{\n" +
      "  \"size\": 1,\n" +
      "  \"limit\": 25,\n" +
      "  \"isLastPage\": true,\n" +
      "  \"values\": [{\n" +
      "    \"id\": \"refs/heads/demo\",\n" +
      "    \"displayId\": \"demo\",\n" +
      "    \"type\": \"BRANCH\",\n" +
      "    \"latestCommit\": \"3e30a6701af6f29f976e9a6609a6076b32a69ac3\",\n" +
      "    \"latestChangeset\": \"3e30a6701af6f29f976e9a6609a6076b32a69ac3\",\n" +
      "    \"isDefault\": false\n" +
      "  }],\n" +
      "  \"start\": 0\n" +
      "}";
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody(bodyWith1Branch));

    BranchesList branches = underTest.getBranches(server.url("/").toString(), "token", "projectSlug", "repoSlug");
    assertThat(branches.getBranches()).hasSize(1);

    Branch branch = branches.getBranches().get(0);
    assertThat(branch.getName()).isEqualTo("demo");
    assertThat(branch.isDefault()).isFalse();

  }

  @Test
  public void getBranches_given2Branches_returnListWithTwoBranches() {
    String bodyWith2Branches = "{\n" +
      "  \"size\": 2,\n" +
      "  \"limit\": 25,\n" +
      "  \"isLastPage\": true,\n" +
      "  \"values\": [{\n" +
      "    \"id\": \"refs/heads/demo\",\n" +
      "    \"displayId\": \"demo\",\n" +
      "    \"type\": \"BRANCH\",\n" +
      "    \"latestCommit\": \"3e30a6701af6f29f976e9a6609a6076b32a69ac3\",\n" +
      "    \"latestChangeset\": \"3e30a6701af6f29f976e9a6609a6076b32a69ac3\",\n" +
      "    \"isDefault\": false\n" +
      "  }, {\n" +
      "    \"id\": \"refs/heads/master\",\n" +
      "    \"displayId\": \"master\",\n" +
      "    \"type\": \"BRANCH\",\n" +
      "    \"latestCommit\": \"66633864d27c531ff43892f6dfea6d91632682fa\",\n" +
      "    \"latestChangeset\": \"66633864d27c531ff43892f6dfea6d91632682fa\",\n" +
      "    \"isDefault\": true\n" +
      "  }],\n" +
      "  \"start\": 0\n" +
      "}";
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody(bodyWith2Branches));

    BranchesList branches = underTest.getBranches(server.url("/").toString(), "token", "projectSlug", "repoSlug");

    assertThat(branches.getBranches()).hasSize(2);
  }

  @Test
  public void invalid_url() {
    assertThatThrownBy(() -> BitbucketServerRestClient.buildUrl("file://wrong-url", ""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("url must start with http:// or https://");
  }

  @Test
  public void malformed_json() {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody(
        "I'm malformed JSON"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.getRepo(serverUrl, "token", "", ""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server, got an unexpected response");
  }

  @Test
  public void error_handling() {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setResponseCode(400)
      .setBody("{\n" +
        "  \"errors\": [\n" +
        "    {\n" +
        "      \"context\": null,\n" +
        "      \"message\": \"Bad message\",\n" +
        "      \"exceptionName\": \"com.atlassian.bitbucket.auth.BadException\"\n" +
        "    }\n" +
        "  ]\n" +
        "}"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.getRepo(serverUrl, "token", "", ""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server");
  }

  @Test
  public void unauthorized_error() {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setResponseCode(401)
      .setBody("{\n" +
        "  \"errors\": [\n" +
        "    {\n" +
        "      \"context\": null,\n" +
        "      \"message\": \"Bad message\",\n" +
        "      \"exceptionName\": \"com.atlassian.bitbucket.auth.BadException\"\n" +
        "    }\n" +
        "  ]\n" +
        "}"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.getRepo(serverUrl, "token", "", ""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid personal access token");
  }

  @Test
  public void fail_validate_on_io_exception() throws IOException {
    server.shutdown();

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateUrl(serverUrl))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server");

    assertThat(String.join(", ", logTester.logs())).contains("Unable to contact Bitbucket server: Failed to connect");
  }

  @Test
  public void fail_validate_url_on_non_json_result_log_correctly_the_response() {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setResponseCode(500)
      .setBody("not json"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateReadPermission(serverUrl, "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server");

    assertThat(String.join(", ", logTester.logs())).contains("Unable to contact Bitbucket server: 500 not json");
  }

  @Test
  public void fail_validate_url_on_text_result_log_the_returned_payload() {
    server.enqueue(new MockResponse()
      .setResponseCode(500)
      .setBody("this is a text payload"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateReadPermission(serverUrl, "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server");

    assertThat(String.join(", ", logTester.logs())).contains("Unable to contact Bitbucket server: 500 this is a text payload");
  }

  @Test
  public void validate_url_success() {
    server.enqueue(new MockResponse().setResponseCode(200)
      .setBody(REPOS_BODY));

    underTest.validateUrl(server.url("/").toString());
  }

  @Test
  public void validate_url_fail_when_not_starting_with_protocol() {
    assertThatThrownBy(() -> underTest.validateUrl("any_url_not_starting_with_http.com"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("url must start with http:// or https://");
  }

  @Test
  public void validate_token_success() {
    server.enqueue(new MockResponse().setResponseCode(200)
      .setBody("{\n" +
        "   \"size\":10,\n" +
        "   \"limit\":25,\n" +
        "   \"isLastPage\":true,\n" +
        "   \"values\":[\n" +
        "      {\n" +
        "         \"name\":\"jean.michel\",\n" +
        "         \"emailAddress\":\"jean.michel@sonarsource.com\",\n" +
        "         \"id\":2,\n" +
        "         \"displayName\":\"Jean Michel\",\n" +
        "         \"active\":true,\n" +
        "         \"slug\":\"jean.michel\",\n" +
        "         \"type\":\"NORMAL\",\n" +
        "         \"links\":{\n" +
        "            \"self\":[\n" +
        "               {\n" +
        "                  \"href\":\"https://bitbucket-testing.valiantys.sonarsource.com/users/jean.michel\"\n" +
        "               }\n" +
        "            ]\n" +
        "         }\n" +
        "      },\n" +
        "      {\n" +
        "         \"name\":\"prince.de.lu\",\n" +
        "         \"emailAddress\":\"prince.de.lu@sonarsource.com\",\n" +
        "         \"id\":103,\n" +
        "         \"displayName\":\"Prince de Lu\",\n" +
        "         \"active\":true,\n" +
        "         \"slug\":\"prince.de.lu\",\n" +
        "         \"type\":\"NORMAL\",\n" +
        "         \"links\":{\n" +
        "            \"self\":[\n" +
        "               {\n" +
        "                  \"href\":\"https://bitbucket-testing.valiantys.sonarsource.com/users/prince.de.lu\"\n" +
        "               }\n" +
        "            ]\n" +
        "         }\n" +
        "      },\n" +
        "   ],\n" +
        "   \"start\":0\n" +
        "}"));

    underTest.validateToken(server.url("/").toString(), "token");
  }

  @Test
  public void validate_read_permission_success() {
    server.enqueue(new MockResponse().setResponseCode(200)
      .setBody(REPOS_BODY));

    underTest.validateReadPermission(server.url("/").toString(), "token");
  }

  @Test
  public void fail_validate_url_when_on_http_error() {
    server.enqueue(new MockResponse().setResponseCode(500)
      .setBody("something unexpected"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateUrl(serverUrl))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server");
  }

  @Test
  public void fail_validate_url_when_not_found_is_returned() {
    server.enqueue(new MockResponse().setResponseCode(404)
      .setBody("something unexpected"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateUrl(serverUrl))
      .isInstanceOf(BitbucketServerException.class)
      .hasMessage("something unexpected")
      .extracting(e -> ((BitbucketServerException) e).getHttpStatus()).isEqualTo(404);
  }

  @Test
  public void fail_validate_url_when_body_is_empty() {
    server.enqueue(new MockResponse().setResponseCode(404).setBody(""));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateUrl(serverUrl))
      .isInstanceOf(BitbucketServerException.class)
      .hasMessage("")
      .extracting(e -> ((BitbucketServerException) e).getHttpStatus()).isEqualTo(404);
  }

  @Test
  public void fail_validate_url_when_validate_url_return_non_json_payload() {
    server.enqueue(new MockResponse().setResponseCode(400)
      .setBody("this is not a json payload"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateUrl(serverUrl))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server");
  }

  @Test
  public void fail_validate_url_when_returning_non_json_payload_with_a_200_code() {
    server.enqueue(new MockResponse().setResponseCode(200)
      .setBody("this is not a json payload"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateUrl(serverUrl))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server, got an unexpected response");
  }

  @Test
  public void fail_validate_token_when_server_return_non_json_payload() {
    server.enqueue(new MockResponse().setResponseCode(400)
      .setBody("this is not a json payload"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateToken(serverUrl, "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server");
  }

  @Test
  public void fail_validate_token_when_returning_non_json_payload_with_a_200_code() {
    server.enqueue(new MockResponse().setResponseCode(200)
      .setBody("this is not a json payload"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateToken(serverUrl, "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server, got an unexpected response");
  }

  @Test
  public void fail_validate_token_when_using_an_invalid_token() {
    server.enqueue(new MockResponse().setResponseCode(401)
      .setBody("com.atlassian.bitbucket.AuthorisationException You are not permitted to access this resource"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateToken(serverUrl, "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid personal access token");
  }

  @Test
  public void fail_validate_read_permission_when_server_return_non_json_payload() {
    server.enqueue(new MockResponse().setResponseCode(400)
      .setBody("this is not a json payload"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateReadPermission(serverUrl, "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server");
  }

  @Test
  public void fail_validate_read_permission_when_returning_non_json_payload_with_a_200_code() {
    server.enqueue(new MockResponse().setResponseCode(200)
      .setBody("this is not a json payload"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateReadPermission(serverUrl, "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server, got an unexpected response");
  }

  @Test
  public void fail_validate_read_permission_when_permissions_are_not_granted() {
    server.enqueue(new MockResponse().setResponseCode(401)
      .setBody("com.atlassian.bitbucket.AuthorisationException You are not permitted to access this resource"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateReadPermission(serverUrl, "token"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid personal access token");
  }

}
