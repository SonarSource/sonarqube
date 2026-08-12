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
package org.sonar.server.v2.api.history.controller;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.v2.security.RequireAuthentication;
import org.sonarsource.history.api.ProjectBreakdownRequestValidator;
import org.sonarsource.history.api.mapper.HistoryModelConverter;
import org.sonarsource.history.api.model.ProjectCollectionHistoryEntityType;
import org.sonarsource.history.api.model.IssueCountStatus;
import org.sonarsource.history.api.model.IssueSeverity;
import org.sonarsource.history.api.model.IssueType;
import org.sonarsource.history.api.model.ProjectIssueCountsResponse;
import org.sonarsource.history.api.rest.ProjectIssueCountsApi;
import org.sonarsource.history.server.service.IssueCountHistoryService;
import org.sonarsource.history.server.service.ProjectIssueCountsService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.HISTORY_DOMAIN;

/** Serves project issue-count breakdown requests. */
@RestController
@RequestMapping(HISTORY_DOMAIN)
@RequireAuthentication
public class DefaultProjectIssueCountsController implements ProjectIssueCountsApi {

  private final DbClient dbClient;
  private final ProjectCollectionContextLoader contextLoader;
  private final ProjectIssueCountsService projectIssueCountsService;
  private final Clock clock;

  DefaultProjectIssueCountsController(
    DbClient dbClient,
    ProjectCollectionContextLoader contextLoader,
    ProjectIssueCountsService projectIssueCountsService,
    Clock clock) {
    this.dbClient = dbClient;
    this.contextLoader = contextLoader;
    this.projectIssueCountsService = projectIssueCountsService;
    this.clock = clock;
  }

  @Override
  public ResponseEntity<ProjectIssueCountsResponse> getProjectIssueCounts(
    @Nullable String portfolioId,
    @Nullable ProjectCollectionHistoryEntityType entityType,
    @Nullable String entityId,
    @Nullable List<String> ruleKeys,
    @Nullable List<IssueSeverity> severities,
    @Nullable List<IssueType> issueTypes,
    @Nullable List<IssueCountStatus> statuses,
    @Nullable List<String> impacts,
    @Nullable String nameContains,
    @Nullable OffsetDateTime referenceDate,
    Integer pageIndex,
    Integer pageSize,
    List<String> sort,
    Boolean requireIssues) {
    ProjectBreakdownRequestValidator.validateReferenceDate(clock, referenceDate);
    ProjectBreakdownRequestValidator.validateSelector(portfolioId, entityType, entityId);
    var filters = IssueCountHistoryService.buildFilters(
      ruleKeys,
      HistoryModelConverter.toCoreSeverities(severities),
      HistoryModelConverter.toCoreIssueTypes(issueTypes),
      HistoryModelConverter.toCoreStatuses(statuses),
      impacts);
    ProjectCollectionContext context;
    try (DbSession session = dbClient.openSession(false)) {
      context = portfolioId != null
        ? contextLoader.load(session, portfolioId)
        : contextLoader.load(session, Objects.requireNonNull(entityType), Objects.requireNonNull(entityId));
    }
    var response = projectIssueCountsService.getProjectIssueCounts(
      context.branches(), context.visibleBranchIds(), filters, nameContains,
      ProjectBreakdownRequestValidator.toInstant(referenceDate), pageIndex, pageSize, sort, requireIssues);
    return ResponseEntity.ok(HistoryModelConverter.toApiProjectIssueCountsResponse(response));
  }
}
