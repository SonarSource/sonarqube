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

import com.google.common.collect.Lists;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.common.graphql.GraphQlClient;
import org.sonar.server.common.graphql.GraphQlQueryParameters;
import org.sonar.server.common.graphql.GsonGraphQlAnswer;

import static com.google.common.base.Preconditions.checkState;

public class GitLabGraphQlClient {

  private static final Logger LOG = LoggerFactory.getLogger(GitLabGraphQlClient.class);
  private static final int GRAPHQL_MAX_ARGS_SIZE = 100;

  private static final String GRAPHQL_GROUPS_QUERY = """
    query($search: String, $cursor: String) {
      currentUser {
        groups(search: $search, first: 100, after: $cursor) {
          nodes {
            fullPath
          }
          pageInfo {
            hasNextPage
            endCursor
          }
        }
      }
    }""";

  private static final Type GROUPS_ANSWER_TYPE = TypeToken.getParameterized(GsonGraphQlAnswer.class, GroupsData.class).getType();

  private static final String GRAPHQL_PROJECTS_QUERY = """
    query($ids: [ID!], $cursor: String) {
      projects(ids: $ids, first: 100, after: $cursor) {
        nodes {
          id
          name
          visibility
        }
        pageInfo {
          hasNextPage
          endCursor
        }
      }
    }""";

  private static final Type PROJECTS_ANSWER_TYPE = TypeToken.getParameterized(GsonGraphQlAnswer.class, ProjectsData.class).getType();

  private final GitLabSettings gitLabSettings;
  private final GraphQlClient graphQlClient;

  public GitLabGraphQlClient(GitLabSettings gitLabSettings, GraphQlClient graphQlClient) {
    this.gitLabSettings = gitLabSettings;
    this.graphQlClient = graphQlClient;
  }

  List<GsonGroup> getGroups(String accessToken, @Nullable String searchTerm) {
    LOG.debug("Getting GitLab groups {}", searchTerm);
    Map<String, String> variables = new HashMap<>();
    variables.put("search", searchTerm);

    String gitlabUrl = gitLabSettings.url();
    checkState(gitlabUrl != null, "GitLab URL is not configured");
    var queryWithPagination = new GraphQlQueryParameters.QueryWithPagination<>(
      gitlabUrl + "/api/graphql",
      accessToken,
      GRAPHQL_GROUPS_QUERY,
      variables,
      GitLabGraphQlClient::toGsonGroups,
      GitLabGraphQlClient::extractCursor,
      GitLabGraphQlClient::hasNextPage,
      GROUPS_ANSWER_TYPE);

    return graphQlClient.executeQuery(queryWithPagination);
  }

  public List<ProjectsData.ProjectNode> getProjectsDetails(String accessToken, List<String> ids) {
    String gitlabUrl = gitLabSettings.url();
    checkState(gitlabUrl != null, "GitLab URL is not configured");
    return Lists.partition(ids, GRAPHQL_MAX_ARGS_SIZE).stream()
      .flatMap(chunk -> {
        Map<String, List<String>> variables = new HashMap<>();
        variables.put("ids", chunk);
        var queryWithPagination = new GraphQlQueryParameters.QueryWithPagination<>(
          gitlabUrl + "/api/graphql",
          accessToken,
          GRAPHQL_PROJECTS_QUERY,
          variables,
          GitLabGraphQlClient::toProjectNodes,
          GitLabGraphQlClient::extractProjectsCursor,
          GitLabGraphQlClient::hasNextProjectsPage,
          PROJECTS_ANSWER_TYPE);
        return graphQlClient.executeQuery(queryWithPagination).stream();
      })
      .toList();
  }

  private static List<ProjectsData.ProjectNode> toProjectNodes(GsonGraphQlAnswer<ProjectsData> answer) {
    return answer.getNonNullData().projects().nodes();
  }

  private static String extractProjectsCursor(GsonGraphQlAnswer<ProjectsData> answer) {
    return answer.getNonNullData().projects().pageInfo().endCursor();
  }

  private static boolean hasNextProjectsPage(GsonGraphQlAnswer<ProjectsData> answer) {
    return answer.getNonNullData().projects().pageInfo().hasNextPage();
  }

  private static List<GsonGroup> toGsonGroups(GsonGraphQlAnswer<GroupsData> answer) {
    return answer.getNonNullData().currentUser().groups().nodes().stream()
      .map(node -> {
        GsonGroup group = new GsonGroup();
        group.setFullPath(node.fullPath());
        return group;
      })
      .toList();
  }

  private static String extractCursor(GsonGraphQlAnswer<GroupsData> answer) {
    return answer.getNonNullData().currentUser().groups().pageInfo().endCursor();
  }

  private static boolean hasNextPage(GsonGraphQlAnswer<GroupsData> answer) {
    return answer.getNonNullData().currentUser().groups().pageInfo().hasNextPage();
  }

  public record ProjectsData(ProjectConnection projects) {
    record ProjectConnection(List<ProjectNode> nodes, PageInfo pageInfo) {
    }

    public record ProjectNode(String id, String name, String visibility) {
      public long numericId() {
        checkState(id != null, "id must be non-null");
        return Long.parseLong(id.substring(id.lastIndexOf('/') + 1));
      }
    }
  }

  record GroupsData(CurrentUser currentUser) {
    record CurrentUser(GroupConnection groups) {
      record GroupConnection(List<GroupNode> nodes, PageInfo pageInfo) {
        record GroupNode(String fullPath) {
        }

      }
    }
  }

  record PageInfo(boolean hasNextPage, String endCursor) {
  }
}
