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
package org.sonar.server.issue.ws;

import com.google.common.io.Resources;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueQuery;
import org.sonarqube.ws.Issues;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.server.issue.index.IssueQueryFactory.ISSUE_TYPE_NAMES;
import static org.sonar.server.ws.KeyExamples.KEY_BRANCH_EXAMPLE_001;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

/**
 * List issue tags matching a given query.
 *
 * @since 5.1
 */
public class TagsAction implements IssuesWsAction {
  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_ALL = "all";

  private final IssueIndex issueIndex;
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public TagsAction(IssueIndex issueIndex,
                    IssueIndexSyncProgressChecker issueIndexSyncProgressChecker, DbClient dbClient,
                    ComponentFinder componentFinder) {
    this.issueIndex = issueIndex;
    this.issueIndexSyncProgressChecker = issueIndexSyncProgressChecker;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("tags")
      .setHandler(this)
      .setSince("5.1")
      .setDescription("List tags matching a given query")
      .setResponseExample(Resources.getResource(getClass(), "tags-example.json"))
      .setChangelog(new Change("7.4", "Result doesn't include rules tags anymore"),
        new Change("9.4", "Max page size increased to 500"));
    action.createSearchQuery("misra", "tags");
    action.createPageSize(10, 500);
    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(false)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setSince("7.4");
    action.createParam(PARAM_BRANCH)
      .setDescription("Branch key")
      .setRequired(false)
      .setExampleValue(KEY_BRANCH_EXAMPLE_001)
      .setSince("9.2");
    action.createParam(PARAM_ALL)
      .setDescription("Indicator to search for all tags or only for tags in the main branch of a project")
      .setRequired(false)
      .setDefaultValue(Boolean.FALSE)
      .setPossibleValues(Boolean.TRUE, Boolean.FALSE)
      .setSince("9.2");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String projectKey = request.param(PARAM_PROJECT);
      String branchKey = request.param(PARAM_BRANCH);
      boolean all = request.mandatoryParamAsBoolean(PARAM_ALL);
      checkIfAnyComponentsNeedIssueSync(dbSession, projectKey);

      Optional<ComponentDto> project = getProject(dbSession, projectKey);
      Optional<BranchDto> branch = project.flatMap(p -> dbClient.branchDao().selectByBranchKey(dbSession, p.uuid(), branchKey));
      List<String> tags = searchTags(project.orElse(null), branch.orElse(null), request, all);

      Issues.TagsResponse.Builder tagsResponseBuilder = Issues.TagsResponse.newBuilder();
      tags.forEach(tagsResponseBuilder::addTags);
      writeProtobuf(tagsResponseBuilder.build(), request, response);
    }
  }

  private Optional<ComponentDto> getProject(DbSession dbSession, @Nullable String projectKey) {
    if (projectKey == null) {
      return Optional.empty();
    }
    ComponentDto project = componentFinder.getByKey(dbSession, projectKey);
    checkArgument(project.scope().equals(Scopes.PROJECT), "Component '%s' must be a project", projectKey);
    return Optional.of(project);
  }

  private void checkIfAnyComponentsNeedIssueSync(DbSession session, @Nullable String projectKey) {
    if (projectKey != null) {
      issueIndexSyncProgressChecker.checkIfComponentNeedIssueSync(session, projectKey);
    } else {
      issueIndexSyncProgressChecker.checkIfIssueSyncInProgress(session);
    }
  }

  private List<String> searchTags(@Nullable ComponentDto project, @Nullable BranchDto branch, Request request, boolean all) {
    IssueQuery.Builder issueQueryBuilder = IssueQuery.builder()
      .types(ISSUE_TYPE_NAMES);
    if (project != null) {
      switch (project.qualifier()) {
        case Qualifiers.PROJECT:
          issueQueryBuilder.projectUuids(Set.of(project.uuid()));
          break;
        case Qualifiers.APP, Qualifiers.VIEW:
          issueQueryBuilder.viewUuids(Set.of(project.uuid()));
          break;
        default:
          throw new IllegalArgumentException(String.format("Component of type '%s' is not supported", project.qualifier()));
      }

      if (branch != null && !project.uuid().equals(branch.getUuid())) {
        issueQueryBuilder.branchUuid(branch.getUuid());
        issueQueryBuilder.mainBranch(false);
      }
    }
    if (all) {
      issueQueryBuilder.mainBranch(null);
    }

    return issueIndex.searchTags(
      issueQueryBuilder.build(),
      request.param(TEXT_QUERY),
      request.mandatoryParamAsInt(PAGE_SIZE));
  }

}
