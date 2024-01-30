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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexSyncProgressChecker;
import org.sonar.server.issue.index.IssueQuery;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Issues.AuthorsResponse;

import static java.util.Optional.ofNullable;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class AuthorsAction implements IssuesWsAction {

  private static final EnumSet<RuleType> ALL_RULE_TYPES_EXCEPT_SECURITY_HOTSPOTS = EnumSet.complementOf(EnumSet.of(RuleType.SECURITY_HOTSPOT));
  private static final String PARAM_PROJECT = "project";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final IssueIndex issueIndex;
  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker;

  public AuthorsAction(UserSession userSession, DbClient dbClient, IssueIndex issueIndex, IssueIndexSyncProgressChecker issueIndexSyncProgressChecker) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.issueIndex = issueIndex;
    this.issueIndexSyncProgressChecker = issueIndexSyncProgressChecker;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("authors")
      .setSince("5.1")
      .setDescription("Search SCM accounts which match a given query.<br/>" +
                      "Requires authentication."
                      + "<br/>When issue indexing is in progress returns 503 service unavailable HTTP code.")
      .setResponseExample(Resources.getResource(this.getClass(), "authors-example.json"))
      .setChangelog(new Change("7.4", "The maximum size of 'ps' is set to 100"))
      .setHandler(this);

    action.createSearchQuery("luke", "authors");
    action.createPageSize(10, 100);

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(false)
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setSince("7.4");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkIfComponentNeedIssueSync(dbSession, request.param(PARAM_PROJECT));

      Optional<EntityDto> entity = getEntity(dbSession, request.param(PARAM_PROJECT));
      List<String> authors = getAuthors(dbSession, entity.orElse(null), request);
      AuthorsResponse wsResponse = AuthorsResponse.newBuilder().addAllAuthors(authors).build();
      writeProtobuf(wsResponse, request, response);
    }
  }

  private void checkIfComponentNeedIssueSync(DbSession dbSession, @Nullable String projectKey) {
    if (projectKey != null) {
      issueIndexSyncProgressChecker.checkIfComponentNeedIssueSync(dbSession, projectKey);
    } else {
      issueIndexSyncProgressChecker.checkIfIssueSyncInProgress(dbSession);
    }
  }

  private Optional<EntityDto> getEntity(DbSession dbSession, @Nullable String projectKey) {
    if (projectKey == null) {
      return Optional.empty();
    }
    return Optional.of(dbClient.entityDao().selectByKey(dbSession, projectKey)
      .filter(e -> !e.getQualifier().equals(Qualifiers.SUBVIEW))
      .orElseThrow(() -> new NotFoundException("Entity not found: " + projectKey)));
  }

  private List<String> getAuthors(DbSession session, @Nullable EntityDto entity, Request request) {
    IssueQuery.Builder issueQueryBuilder = IssueQuery.builder();
    ofNullable(entity).ifPresent(p -> {
      switch (p.getQualifier()) {
        case Qualifiers.PROJECT -> issueQueryBuilder.projectUuids(Set.of(p.getUuid()));
        case Qualifiers.VIEW -> issueQueryBuilder.viewUuids(Set.of(p.getUuid()));
        case Qualifiers.APP -> {
          BranchDto appMainBranch = dbClient.branchDao().selectMainBranchByProjectUuid(session, entity.getUuid())
            .orElseThrow(() -> new IllegalStateException("Couldn't find main branch for APP " + entity.getUuid()));
          issueQueryBuilder.viewUuids(Set.of(appMainBranch.getUuid()));
        }
        default -> throw new IllegalArgumentException(String.format("Component of type '%s' is not supported", p.getQualifier()));
      }
    });
    return issueIndex.searchAuthors(
      issueQueryBuilder
        .types(ALL_RULE_TYPES_EXCEPT_SECURITY_HOTSPOTS.stream().map(Enum::name).toList())
        .build(),
      request.param(TEXT_QUERY),
      request.mandatoryParamAsInt(PAGE_SIZE));
  }
}
