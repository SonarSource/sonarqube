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
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueQuery;
import org.sonarqube.ws.Issues;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;
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
  private static final String PARAM_ORGANIZATION = "organization";
  private static final String PARAM_PROJECT = "project";
  private static final String PARAM_BRANCH = "branch";
  private static final String PARAM_ALL = "all";

  private final IssueIndex issueIndex;
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public TagsAction(IssueIndex issueIndex, IssueIndexSyncProgressChecker issueIndexSyncProgressChecker, DbClient dbClient, ComponentFinder componentFinder) {
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
    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setRequired(false)
      .setInternal(true)
      .setExampleValue("my-org")
      .setSince("6.4");
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
      String organizationKey = request.param(PARAM_ORGANIZATION);
      String branchKey = request.param(PARAM_BRANCH);
      boolean all = request.mandatoryParamAsBoolean(PARAM_ALL);
      checkIfAnyComponentsNeedIssueSync(dbSession, projectKey);

      OrganizationDto organization = getOrganization(dbSession, organizationKey);
      Optional<EntityDto> entity = getEntity(dbSession, projectKey);
      entity.ifPresent(e -> checkArgument(e.getOrganizationUuid().equals(organization.getUuid()), "Project '%s' is not part of the organization '%s'", projectKey, organization.getKey()));

      Optional<BranchDto> branch = branchKey == null ? Optional.empty() : entity.flatMap(p -> dbClient.branchDao().selectByBranchKey(dbSession, p.getUuid(), branchKey));
      List<String> tags = searchTags(organization, entity.orElse(null), branch.orElse(null), request, all, dbSession);

      Issues.TagsResponse.Builder tagsResponseBuilder = Issues.TagsResponse.newBuilder();
      tags.forEach(tagsResponseBuilder::addTags);
      writeProtobuf(tagsResponseBuilder.build(), request, response);
    }
  }

  private OrganizationDto getOrganization(DbSession dbSession, @Nullable String organizationKey) {
    return checkFoundWithOptional(dbClient.organizationDao().selectByKey(dbSession, organizationKey), "No organization with key '%s'", organizationKey);
  }

  private Optional<EntityDto> getEntity(DbSession dbSession, @Nullable String entityKey) {
    if (entityKey == null) {
      return Optional.empty();
    }
    return Optional.of(componentFinder.getEntityByKey(dbSession, entityKey))
      .filter(e -> !e.getQualifier().equals(ComponentQualifiers.SUBVIEW));
  }

  private void checkIfAnyComponentsNeedIssueSync(DbSession session, @Nullable String projectKey) {
    if (projectKey != null) {
      issueIndexSyncProgressChecker.checkIfComponentNeedIssueSync(session, projectKey);
    } else {
      issueIndexSyncProgressChecker.checkIfIssueSyncInProgress(session);
    }
  }

  private List<String> searchTags(OrganizationDto organization, @Nullable EntityDto entity, @Nullable BranchDto branch, Request request, boolean all, DbSession dbSession) {
    IssueQuery.Builder issueQueryBuilder = IssueQuery.builder()
      .organizationUuid(organization.getUuid())
      .types(ISSUE_TYPE_NAMES);
    if (entity != null) {
      switch (entity.getQualifier()) {
        case ComponentQualifiers.PROJECT -> issueQueryBuilder.projectUuids(Set.of(entity.getUuid()));
        case ComponentQualifiers.VIEW, ComponentQualifiers.APP -> issueQueryBuilder.viewUuids(Set.of(entity.getUuid()));
        default -> throw new IllegalArgumentException(String.format("Entity of type '%s' is not supported", entity.getQualifier()));
      }

      if (branch != null && !branch.isMain()) {
        issueQueryBuilder.branchUuid(branch.getUuid());
        issueQueryBuilder.mainBranch(false);
      } else if (ComponentQualifiers.APP.equals(entity.getQualifier())) {
        dbClient.branchDao().selectMainBranchByProjectUuid(dbSession, entity.getUuid())
          .ifPresent(b -> issueQueryBuilder.branchUuid(b.getUuid()));
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
