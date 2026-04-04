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

public class GitLabGraphQlClient {

  private static final Logger LOG = LoggerFactory.getLogger(GitLabGraphQlClient.class);

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

    var queryWithPagination = new GraphQlQueryParameters.QueryWithPagination<>(
      gitLabSettings.url() + "/api/graphql",
      accessToken,
      GRAPHQL_GROUPS_QUERY,
      variables,
      GitLabGraphQlClient::toGsonGroups,
      GitLabGraphQlClient::extractCursor,
      GitLabGraphQlClient::hasNextPage,
      GROUPS_ANSWER_TYPE);

    return graphQlClient.executeQuery(queryWithPagination);
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

  record GroupsData(CurrentUser currentUser) {
    record CurrentUser(GroupConnection groups) {
      record GroupConnection(List<GroupNode> nodes, PageInfo pageInfo) {
        record GroupNode(String fullPath) {
        }

        record PageInfo(boolean hasNextPage, String endCursor) {
        }
      }
    }
  }
}
