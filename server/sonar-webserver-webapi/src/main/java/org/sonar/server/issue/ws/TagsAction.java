/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import java.util.List;
import java.util.Optional;
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
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueQuery;
import org.sonarqube.ws.Issues;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Optional.ofNullable;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.server.issue.index.IssueQueryFactory.ISSUE_TYPE_NAMES;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

/**
 * List issue tags matching a given query.
 * @since 5.1
 */
public class TagsAction implements IssuesWsAction {
  private static final String PARAM_PROJECT = "project";

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
      .setChangelog(new Change("7.4", "Result doesn't include rules tags anymore"));
    action.createSearchQuery("misra", "tags");
    action.createPageSize(10, 100);
    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(false)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setSince("7.4");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String projectKey = request.param(PARAM_PROJECT);
      checkIfAnyComponentsNeedIssueSync(dbSession, projectKey);
      Optional<ComponentDto> project = getProject(dbSession, projectKey);
      List<String> tags = searchTags(project.orElse(null), request);
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

  private List<String> searchTags(@Nullable ComponentDto project, Request request) {
    IssueQuery.Builder issueQueryBuilder = IssueQuery.builder()
      .types(ISSUE_TYPE_NAMES);
    ofNullable(project).ifPresent(p -> {
      switch (p.qualifier()) {
        case Qualifiers.PROJECT:
          issueQueryBuilder.projectUuids(ImmutableSet.of(p.uuid()));
          return;
        case Qualifiers.APP:
        case Qualifiers.VIEW:
          issueQueryBuilder.viewUuids(ImmutableSet.of(p.uuid()));
          return;
        default:
          throw new IllegalArgumentException(String.format("Component of type '%s' is not supported", p.qualifier()));
      }
    });
    return issueIndex.searchTags(
      issueQueryBuilder.build(),
      request.param(TEXT_QUERY),
      request.mandatoryParamAsInt(PAGE_SIZE));
  }

}
