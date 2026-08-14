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
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.DbClient;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.security.RequireAuthentication;
import org.sonarsource.history.HistoryDateRange;
import org.sonarsource.history.api.HistoryControllerUtils;
import org.sonarsource.history.api.mapper.HistoryModelConverter;
import org.sonarsource.history.api.model.HistoryEntityType;
import org.sonarsource.history.api.model.IssueResolutionHistoryResponse;
import org.sonarsource.history.api.model.IssueResolutionSliceBy;
import org.sonarsource.history.api.model.IssueResolutionStatistic;
import org.sonarsource.history.api.model.IssueSeverity;
import org.sonarsource.history.api.model.IssueType;
import org.sonarsource.history.api.rest.IssueResolutionHistoryApi;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.model.IssueResolutionHistoryQuery;
import org.sonarsource.history.server.service.IssueCountHistoryService;
import org.sonarsource.history.server.service.IssueTtrHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.HISTORY_DOMAIN;

/** Serves issue-resolution history requests for authenticated entities. */
@RestController
@RequestMapping(HISTORY_DOMAIN)
@RequireAuthentication
public class DefaultIssueResolutionHistoryController implements IssueResolutionHistoryApi {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultIssueResolutionHistoryController.class);

  private final UserSession userSession;
  private final DbClient dbClient;
  private final IssueTtrHistoryService issueTtrHistoryService;
  private final Clock clock;

  DefaultIssueResolutionHistoryController(
    UserSession userSession,
    DbClient dbClient,
    IssueTtrHistoryService issueTtrHistoryService,
    Clock clock) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.issueTtrHistoryService = issueTtrHistoryService;
    this.clock = clock;
  }

  @Override
  public ResponseEntity<IssueResolutionHistoryResponse> getIssueResolutionHistory(
    String entityId,
    HistoryEntityType entityType,
    IssueResolutionStatistic statistic,
    OffsetDateTime startDate,
    @Nullable OffsetDateTime endDate,
    @Nullable List<String> impacts,
    @Nullable List<IssueType> issueTypes,
    @Nullable List<IssueSeverity> severities,
    @Nullable IssueResolutionSliceBy sliceBy) {
    LOG.debug("getIssueResolutionHistory invoked: entityId={}, entityType={}, startDate={}, endDate={}, filters=[{}]",
      entityId, entityType, startDate, endDate, Arrays.asList(sliceBy, impacts, issueTypes, severities));

    EntityType entityTypeEnum = HistoryControllerUtils.ensureValidEntityType(entityType);
    HistoryDateRange dateRange = HistoryControllerUtils.ensureValidDateRange(startDate, endDate, clock);
    HistoryAuthUtils.assertUserHasPermission(userSession, dbClient, entityId, entityTypeEnum);

    var filters = IssueCountHistoryService.buildFilters(
      null,
      HistoryModelConverter.toCoreSeverities(severities),
      HistoryModelConverter.toCoreIssueTypes(issueTypes),
      null,
      impacts);
    var query = IssueResolutionHistoryQuery.builder(entityId, entityTypeEnum, dateRange.start())
      .endDate(dateRange.end())
      .sliceBy(HistoryModelConverter.toCoreIssueResolutionSliceBy(sliceBy))
      .filters(filters)
      .build();
    var response = new org.sonarsource.history.model.IssueResolutionHistoryResponse(
      HistoryModelConverter.toCoreIssueResolutionStatistic(statistic),
      issueTtrHistoryService.query(HistoryModelConverter.toCoreIssueResolutionStatistic(statistic), query));
    return ResponseEntity.ok(HistoryModelConverter.toApiIssueResolutionHistoryResponse(response));
  }
}
