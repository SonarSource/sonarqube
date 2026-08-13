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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.v2.security.RequireAuthentication;
import org.sonarsource.history.api.ProjectBreakdownRequestValidator;
import org.sonarsource.history.api.mapper.HistoryModelConverter;
import org.sonarsource.history.api.model.ProjectCollectionHistoryEntityType;
import org.sonarsource.history.api.model.IssueSeverity;
import org.sonarsource.history.api.model.IssueType;
import org.sonarsource.history.api.model.ProjectIssueResolutionResponse;
import org.sonarsource.history.api.model.ProjectIssueResolutionStatistic;
import org.sonarsource.history.api.rest.ProjectIssueResolutionApi;
import org.sonarsource.history.server.service.IssueCountHistoryService;
import org.sonarsource.history.server.service.ProjectIssueResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.HISTORY_DOMAIN;

/** Serves project issue-resolution breakdown requests. */
@RestController
@RequestMapping(HISTORY_DOMAIN)
@RequireAuthentication
public class DefaultProjectIssueResolutionController implements ProjectIssueResolutionApi {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultProjectIssueResolutionController.class);

  private final DbClient dbClient;
  private final ProjectCollectionContextLoader contextLoader;
  private final ProjectIssueResolutionService projectIssueResolutionService;
  private final Clock clock;

  DefaultProjectIssueResolutionController(
    DbClient dbClient,
    ProjectCollectionContextLoader contextLoader,
    ProjectIssueResolutionService projectIssueResolutionService,
    Clock clock) {
    this.dbClient = dbClient;
    this.contextLoader = contextLoader;
    this.projectIssueResolutionService = projectIssueResolutionService;
    this.clock = clock;
  }

  @Override
  public ResponseEntity<ProjectIssueResolutionResponse> getProjectIssueResolution(
    ProjectIssueResolutionStatistic statistic,
    @Nullable String portfolioId,
    @Nullable ProjectCollectionHistoryEntityType entityType,
    @Nullable String entityId,
    @Nullable List<IssueSeverity> severities,
    @Nullable List<IssueType> issueTypes,
    @Nullable List<String> impacts,
    @Nullable String nameContains,
    @Nullable OffsetDateTime trendSince,
    Integer pageIndex,
    Integer pageSize) {
    ProjectBreakdownRequestValidator.validateReferenceDate(clock, trendSince);
    ProjectBreakdownRequestValidator.validateSelector(portfolioId, entityType, entityId);
    var filters = IssueCountHistoryService.buildFilters(
      null,
      HistoryModelConverter.toCoreSeverities(severities),
      HistoryModelConverter.toCoreIssueTypes(issueTypes),
      null,
      impacts);
    LOG.debug("getProjectIssueResolution invoked: portfolioId={}, entityType={}, entityId={}, statistic={}, trendSince={}, filters={}",
      portfolioId, entityType, entityId, statistic, trendSince, filters);
    ProjectCollectionContext context;
    try (DbSession session = dbClient.openSession(false)) {
      context = portfolioId != null
        ? contextLoader.load(session, portfolioId)
        : contextLoader.load(session, Objects.requireNonNull(entityType), Objects.requireNonNull(entityId));
    }

    var response = projectIssueResolutionService.getProjectIssueResolution(
      context.branches(),
      context.visibleBranchIds(),
      HistoryModelConverter.toCoreProjectIssueResolutionStatistic(statistic),
      filters,
      nameContains,
      clock.instant(),
      ProjectBreakdownRequestValidator.toInstant(trendSince),
      pageIndex,
      pageSize);
    return ResponseEntity.ok(HistoryModelConverter.toApiProjectIssueResolutionResponse(response));
  }
}
