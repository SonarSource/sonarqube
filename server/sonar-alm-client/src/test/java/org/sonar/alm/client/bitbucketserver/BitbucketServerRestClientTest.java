/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.IOException;
import java.util.function.Function;
import java.util.stream.IntStream;
import okhttp3.MediaType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import okio.Buffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.alm.client.ConstantTimeoutConfiguration;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@RunWith(DataProviderRunner.class)
public class BitbucketServerRestClientTest {
  private final MockWebServer server = new MockWebServer();
  private static final String REPOS_BODY = """
    {
      "isLastPage": true,
      "values": [
        {
          "slug": "banana",
          "id": 2,
          "name": "banana",
          "project": {
            "key": "HOY",
            "id": 2,
            "name": "hoy"
          }
        },
        {
          "slug": "potato",
          "id": 1,
          "name": "potato",
          "project": {
            "key": "HEY",
            "id": 1,
            "name": "hey"
          }
        }
      ]
    }
    """;
  private static final String STATUS_BODY = "{\"state\": \"RUNNING\"}";

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
      .setBody("""
        {
          "isLastPage": true,
          "values": [
            {
              "slug": "banana",
              "id": 2,
              "name": "banana",
              "project": {
                "key": "HOY",
                "id": 2,
                "name": "hoy"
              }
            },
            {
              "slug": "potato",
              "id": 1,
              "name": "potato",
              "project": {
                "key": "HEY",
                "id": 1,
                "name": "hey"
              }
            }
          ]
        }
        """));

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
      .setBody("""
        {
          "isLastPage": true,
          "values": [
            {
              "slug": "banana",
              "id": 2,
              "name": "banana",
              "project": {
                "key": "HOY",
                "id": 2,
                "name": "hoy"
              }
            },
            {
              "slug": "potato",
              "id": 1,
              "name": "potato",
              "project": {
                "key": "HEY",
                "id": 1,
                "name": "hey"
              }
            }
          ]
        }
        """));

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
      .setBody("""
        {
          "slug": "banana-slug",
          "id": 2,
          "name": "banana",
          "project": {
            "key": "HOY",
            "id": 3,
            "name": "hoy"
          }
        }
        """));

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
      .setBody("""
        {
          "isLastPage": true,
          "values": [
            {
              "key": "HEY",
              "id": 1,
              "name": "hey"
            },
            {
              "key": "HOY",
              "id": 2,
              "name": "hoy"
            }
          ]
        }
        """));

    final ProjectList gsonBBSProjectList = underTest.getProjects(server.url("/").toString(), "token", null, 25);
    assertThat(gsonBBSProjectList.getValues()).hasSize(2);
    assertThat(gsonBBSProjectList.getValues()).extracting(Project::getId, Project::getKey, Project::getName)
      .containsExactlyInAnyOrder(
        tuple(1L, "HEY", "hey"),
        tuple(2L, "HOY", "hoy"));
  }

  @Test
  public void get_projects_failed() {
    server.enqueue(new MockResponse()
      .setBody(new Buffer().write(new byte[4096]))
      .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.getProjects(serverUrl, "token", null, 25))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server");

    assertThat(String.join(", ", logTester.logs())).contains("Unable to contact Bitbucket server");
  }

  @Test
  public void getBranches_given0Branches_returnEmptyList() {
    String bodyWith0Branches = """
      {
        "size": 0,
        "limit": 25,
        "isLastPage": true,
        "values": [],
        "start": 0
      }
      """;
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody(bodyWith0Branches));

    BranchesList branches = underTest.getBranches(server.url("/").toString(), "token", "projectSlug", "repoSlug");

    assertThat(branches.getBranches()).isEmpty();
  }

  @Test
  public void getBranches_given1Branch_returnListWithOneBranch() {
    String bodyWith1Branch = """
      {
        "size": 1,
        "limit": 25,
        "isLastPage": true,
        "values": [{
          "id": "refs/heads/demo",
          "displayId": "demo",
          "type": "BRANCH",
          "latestCommit": "3e30a6701af6f29f976e9a6609a6076b32a69ac3",
          "latestChangeset": "3e30a6701af6f29f976e9a6609a6076b32a69ac3",
          "isDefault": false
        }],
        "start": 0
      }
      """;
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
    String bodyWith2Branches = """
      {
        "size": 2,
        "limit": 25,
        "isLastPage": true,
        "values": [{
          "id": "refs/heads/demo",
          "displayId": "demo",
          "type": "BRANCH",
          "latestCommit": "3e30a6701af6f29f976e9a6609a6076b32a69ac3",
          "latestChangeset": "3e30a6701af6f29f976e9a6609a6076b32a69ac3",
          "isDefault": false
        }, {
          "id": "refs/heads/master",
          "displayId": "master",
          "type": "BRANCH",
          "latestCommit": "66633864d27c531ff43892f6dfea6d91632682fa",
          "latestChangeset": "66633864d27c531ff43892f6dfea6d91632682fa",
          "isDefault": true
        }],
        "start": 0
      }
      """;
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody(bodyWith2Branches));

    BranchesList branches = underTest.getBranches(server.url("/").toString(), "token", "projectSlug", "repoSlug");

    assertThat(branches.getBranches()).hasSize(2);
  }

  @Test
  public void invalid_empty_url() {
    assertThatThrownBy(() -> BitbucketServerRestClient.buildUrl(null, ""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("url must start with http:// or https://");
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
      .setBody("I'm malformed JSON"));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.getRepo(serverUrl, "token", "", ""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unexpected response from Bitbucket server");
    assertThat(String.join(", ", logTester.logs()))
      .contains("Unexpected response from Bitbucket server : [I'm malformed JSON]");
  }

  @Test
  public void fail_json_error_handling() {
    assertThatThrownBy(() -> BitbucketServerRestClient.applyHandler(body -> BitbucketServerRestClient.buildGson().fromJson(body, Object.class), "not json"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server, got an unexpected response");
    assertThat(String.join(", ", logTester.logs()))
      .contains("Unable to contact Bitbucket server. Unexpected body response was : [not json]");
  }

  @Test
  public void validate_handler_call_on_empty_body() {
    server.enqueue(new MockResponse().setResponseCode(200)
      .setBody(""));
    assertThat(underTest.doGet("token", server.url("/"), Function.identity()))
      .isEmpty();
  }

  @Test
  public void error_handling() {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setResponseCode(400)
      .setBody("""
        {
          "errors": [
            {
              "context": null,
              "message": "Bad message",
              "exceptionName": "com.atlassian.bitbucket.auth.BadException"
            }
          ]
        }
        """));

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
      .setBody("""
        {
          "errors": [
            {
              "context": null,
              "message": "Bad message",
              "exceptionName": "com.atlassian.bitbucket.auth.BadException"
            }
          ]
        }
        """));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.getRepo(serverUrl, "token", "", ""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid personal access token");
  }

  @DataProvider
  public static Object[][] expectedErrorMessageFromHttpNoJsonBody() {
    return new Object[][] {
      {200, "content ready", "application/json;charset=UTF-8", "Unexpected response from Bitbucket server"},
      {201, "content ready!", "application/xhtml+xml", "Unexpected response from Bitbucket server"},
      {401, "<p>unauthorized</p>", "application/json;charset=UTF-8", "Invalid personal access token"},
      {401, "<p>unauthorized</p>", "application/json", "Invalid personal access token"},
      {401, "<not-authorized>401</not-authorized>", "application/xhtml+xml", "Invalid personal access token"},
      {403, "<p>forbidden</p>", "application/json;charset=UTF-8", "Unable to contact Bitbucket server"},
      {404, "<p>not found</p>", "application/json;charset=UTF-8", "Error 404. The requested Bitbucket server is unreachable."},
      {406, "<p>not accepted</p>", "application/json;charset=UTF-8", "Unable to contact Bitbucket server"},
      {409, "<p>conflict</p>", "application/json;charset=UTF-8", "Unable to contact Bitbucket server"}
    };
  }

  @Test
  @UseDataProvider("expectedErrorMessageFromHttpNoJsonBody")
  public void fail_response_when_http_no_json_body(int responseCode, String body, String headerContent, String expectedErrorMessage) {
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", headerContent)
      .setResponseCode(responseCode)
      .setBody(body));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.getRepo(serverUrl, "token", "", ""))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(expectedErrorMessage);
  }

  @Test
  public void fail_validate_on_io_exception() throws IOException {
    server.shutdown();

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateUrl(serverUrl))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unable to contact Bitbucket server");

    assertThat(String.join(", ", logTester.logs())).contains("Unable to contact Bitbucket server");
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
      .setBody(STATUS_BODY));

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
      .setBody("""
        {
           "size":10,
           "limit":25,
           "isLastPage":true,
           "values":[
              {
                 "name":"jean.michel",
                 "emailAddress":"jean.michel@sonarsource.com",
                 "id":2,
                 "displayName":"Jean Michel",
                 "active":true,
                 "slug":"jean.michel",
                 "type":"NORMAL",
                 "links":{
                    "self":[
                       {
                          "href":"https://bitbucket-testing.valiantys.sonarsource.com/users/jean.michel"
                       }
                    ]
                 }
              },
              {
                 "name":"prince.de.lu",
                 "emailAddress":"prince.de.lu@sonarsource.com",
                 "id":103,
                 "displayName":"Prince de Lu",
                 "active":true,
                 "slug":"prince.de.lu",
                 "type":"NORMAL",
                 "links":{
                    "self":[
                       {
                          "href":"https://bitbucket-testing.valiantys.sonarsource.com/users/prince.de.lu"
                       }
                    ]
                 }
              },
           ],
           "start":0
        }
        """));

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
      .hasMessage("Error 404. The requested Bitbucket server is unreachable.")
      .extracting(e -> ((BitbucketServerException) e).getHttpStatus()).isEqualTo(404);
  }

  @Test
  public void fail_validate_url_when_body_is_empty() {
    server.enqueue(new MockResponse().setResponseCode(404).setBody(""));

    String serverUrl = server.url("/").toString();
    assertThatThrownBy(() -> underTest.validateUrl(serverUrl))
      .isInstanceOf(BitbucketServerException.class)
      .hasMessage("Error 404. The requested Bitbucket server is unreachable.")
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
      .hasMessage("Unexpected response from Bitbucket server");
    assertThat(String.join(", ", logTester.logs()))
      .contains("Unexpected response from Bitbucket server : [this is not a json payload]");
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
      .hasMessage("Unexpected response from Bitbucket server");
    assertThat(String.join(", ", logTester.logs()))
      .contains("Unexpected response from Bitbucket server : [this is not a json payload]");
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
      .hasMessage("Unexpected response from Bitbucket server");
    assertThat(String.join(", ", logTester.logs()))
      .contains("Unexpected response from Bitbucket server : [this is not a json payload]");
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

  @Test
  public void check_mediaTypes_equality() {
    assertThat(BitbucketServerRestClient.equals(null, null)).isFalse();
    assertThat(BitbucketServerRestClient.equals(MediaType.parse("application/json"), null)).isFalse();
    assertThat(BitbucketServerRestClient.equals(null, MediaType.parse("application/json"))).isFalse();
    assertThat(BitbucketServerRestClient.equals(MediaType.parse("application/ json"), MediaType.parse("text/html; charset=UTF-8"))).isFalse();
    assertThat(BitbucketServerRestClient.equals(MediaType.parse("application/Json"), MediaType.parse("application/JSON"))).isTrue();
  }

  @Test
  @UseDataProvider("validStartAndPageSizeProvider")
  public void get_projects_with_pagination(int start, int pageSize, boolean isLastPage, int totalProjects) {
    String body = generateProjectsResponse(totalProjects, start, pageSize, isLastPage);
    server.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json;charset=UTF-8")
      .setBody(body));

    ProjectList gsonBBSProjectList = underTest.getProjects(server.url("/").toString(), "token", start, pageSize);

    int expectedSize = Math.min(pageSize, totalProjects - start + 1);
    assertThat(gsonBBSProjectList.getValues()).hasSize(expectedSize);

    // Verify that the correct items are returned
    IntStream.rangeClosed(start, start + expectedSize - 1).forEach(i -> {
      assertThat(gsonBBSProjectList.getValues())
        .extracting(Project::getId, Project::getKey, Project::getName)
        .contains(tuple((long) i, "KEY_" + i, "Project_" + i));
    });

    assertThat(gsonBBSProjectList.isLastPage()).isEqualTo(isLastPage);
    assertThat(gsonBBSProjectList.getNextPageStart()).isEqualTo(isLastPage ? start : start + expectedSize);
  }

  private String generateProjectsResponse(int totalProjects, int start, int pageSize, boolean isLastPage) {
    int end = Math.min(totalProjects, start + pageSize - 1);

    StringBuilder values = new StringBuilder();
    for (int i = start; i <= end; i++) {
      values.append("""
        {
          "key": "KEY_%d",
          "id": %d,
          "name": "Project_%d"
        }
        """.formatted(i, i, i));
      if (i < end) {
        values.append(",");
      }
    }

    return """
      {
        "isLastPage": %s,
        "nextPageStart": %s,
        "values": [%s]
      }
      """.formatted(isLastPage, isLastPage ? start : end + 1, values.toString());
  }

  @DataProvider
  public static Object[][] validStartAndPageSizeProvider() {
    return new Object[][] {
      {1, 25, false, 50}, // First 25 items, not last
      {26, 25, true, 50}, // Remaining items
      {1, 10, false, 15}, // 10 items, more remaining
      {11, 10, true, 15}, // Last 5 items
      {21, 10, false, 100}, // Middle range of items
    };
  }

}
