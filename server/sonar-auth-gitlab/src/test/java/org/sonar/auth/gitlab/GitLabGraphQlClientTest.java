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
package org.sonar.auth.gitlab;

import java.util.List;
import java.util.stream.IntStream;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.common.graphql.GraphQlClient;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_ENABLED;
import static org.sonar.auth.gitlab.GitLabSettings.GITLAB_AUTH_URL;

public class GitLabGraphQlClientTest {

  @Rule
  public MockWebServer gitlab = new MockWebServer();

  private final MapSettings mapSettings = new MapSettings();
  private GitLabGraphQlClient underTest;

  @Before
  public void setUp() {
    String gitLabUrl = format("http://%s:%d", gitlab.getHostName(), gitlab.getPort());
    mapSettings
      .setProperty(GITLAB_AUTH_ENABLED, "true")
      .setProperty(GITLAB_AUTH_URL, gitLabUrl);
    GitLabSettings gitLabSettings = new GitLabSettings(mapSettings.asConfig());
    underTest = new GitLabGraphQlClient(gitLabSettings, new GraphQlClient(new OkHttpClient()));
  }

  @Test
  public void getProjectsDetails_shouldReturnProjectDetails() {
    enqueueProjectsResponse(
      projectNode("gid://gitlab/Project/1", "project-1", "private"),
      projectNode("gid://gitlab/Project/2", "project-2", "public"));

    List<GitLabGraphQlClient.ProjectsData.ProjectNode> result =
      underTest.getProjectsDetails("token", List.of("gid://gitlab/Project/1", "gid://gitlab/Project/2"));

    assertThat(result).hasSize(2);
    assertThat(result).extracting(GitLabGraphQlClient.ProjectsData.ProjectNode::name)
      .containsExactly("project-1", "project-2");
    assertThat(result).extracting(GitLabGraphQlClient.ProjectsData.ProjectNode::visibility)
      .containsExactly("private", "public");
    assertThat(result).extracting(GitLabGraphQlClient.ProjectsData.ProjectNode::numericId)
      .containsExactly(1L, 2L);
  }

  @Test
  public void getProjectsDetails_withPagination_shouldFetchAllPages() {
    enqueueProjectsResponseWithNextPage("cursor1",
      projectNode("gid://gitlab/Project/1", "project-1", "private"));
    enqueueProjectsResponse(
      projectNode("gid://gitlab/Project/2", "project-2", "public"));

    List<GitLabGraphQlClient.ProjectsData.ProjectNode> result =
      underTest.getProjectsDetails("token", List.of("gid://gitlab/Project/1", "gid://gitlab/Project/2"));

    assertThat(result).hasSize(2);
    assertThat(result).extracting(GitLabGraphQlClient.ProjectsData.ProjectNode::name)
      .containsExactly("project-1", "project-2");
  }

  @Test
  public void getProjectsDetails_withMoreThan100Ids_shouldSplitIntoBatches() throws InterruptedException {
    List<String> ids = IntStream.rangeClosed(1, 101)
      .mapToObj(i -> "gid://gitlab/Project/" + i)
      .toList();

    // First batch: 100 projects
    String[] firstBatchNodes = IntStream.rangeClosed(1, 100)
      .mapToObj(i -> projectNode("gid://gitlab/Project/" + i, "project-" + i, "private"))
      .toArray(String[]::new);
    enqueueProjectsResponse(firstBatchNodes);

    // Second batch: 1 project
    enqueueProjectsResponse(projectNode("gid://gitlab/Project/101", "project-101", "public"));

    List<GitLabGraphQlClient.ProjectsData.ProjectNode> result = underTest.getProjectsDetails("token", ids);

    assertThat(result).hasSize(101);
    assertThat(result).extracting(GitLabGraphQlClient.ProjectsData.ProjectNode::numericId)
      .containsExactlyElementsOf(IntStream.rangeClosed(1, 101).mapToObj(i -> (long) i).toList());

    assertThat(gitlab.getRequestCount()).isEqualTo(2);

    String firstBody = gitlab.takeRequest().getBody().readUtf8();
    assertThat(firstBody)
      .contains("gid://gitlab/Project/1")
      .contains("gid://gitlab/Project/100")
      .doesNotContain("gid://gitlab/Project/101");

    String secondBody = gitlab.takeRequest().getBody().readUtf8();
    assertThat(secondBody).contains("gid://gitlab/Project/101");
  }

  @Test
  public void getProjectsDetails_withExactly100Ids_shouldMakeSingleRequest() {
    List<String> ids = IntStream.rangeClosed(1, 100)
      .mapToObj(i -> "gid://gitlab/Project/" + i)
      .toList();

    String[] nodes = IntStream.rangeClosed(1, 100)
      .mapToObj(i -> projectNode("gid://gitlab/Project/" + i, "project-" + i, "private"))
      .toArray(String[]::new);
    enqueueProjectsResponse(nodes);

    List<GitLabGraphQlClient.ProjectsData.ProjectNode> result = underTest.getProjectsDetails("token", ids);

    assertThat(result).hasSize(100);
    assertThat(gitlab.getRequestCount()).isEqualTo(1);
  }

  @Test
  public void getProjectsDetails_withEmptyList_shouldReturnEmpty() {
    List<GitLabGraphQlClient.ProjectsData.ProjectNode> result = underTest.getProjectsDetails("token", List.of());

    assertThat(result).isEmpty();
    assertThat(gitlab.getRequestCount()).isZero();
  }

  private static String projectNode(String id, String name, String visibility) {
    return """
      {"id": "%s", "name": "%s", "visibility": "%s"}""".formatted(id, name, visibility);
  }

  private void enqueueProjectsResponse(String... nodes) {
    String nodesJson = String.join(",", nodes);
    gitlab.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json")
      .setBody("""
        {
          "data": {
            "projects": {
              "nodes": [%s],
              "pageInfo": {
                "hasNextPage": false,
                "endCursor": null
              }
            }
          }
        }
        """.formatted(nodesJson)));
  }

  private void enqueueProjectsResponseWithNextPage(String endCursor, String... nodes) {
    String nodesJson = String.join(",", nodes);
    gitlab.enqueue(new MockResponse()
      .setHeader("Content-Type", "application/json")
      .setBody("""
        {
          "data": {
            "projects": {
              "nodes": [%s],
              "pageInfo": {
                "hasNextPage": true,
                "endCursor": "%s"
              }
            }
          }
        }
        """.formatted(nodesJson, endCursor)));
  }
}
