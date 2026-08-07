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
import org.sonarsource.history.api.mapper.HistoryModelConverter;
import org.sonarsource.history.api.model.IssueCountDistributionType;
import org.sonarsource.history.api.model.IssueDensityHistoryResponse;
import org.sonarsource.history.api.model.IssueCountStatus;
import org.sonarsource.history.api.model.IssueSeverity;
import org.sonarsource.history.api.model.IssueType;
import org.sonarsource.history.api.rest.IssueDensityHistoryApi;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.server.service.IssueCountHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.sonar.server.v2.WebApiEndpoints.HISTORY_DOMAIN;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/** Serves issue density history requests for authenticated project branches. */
@RestController
@RequestMapping(HISTORY_DOMAIN)
@RequireAuthentication
public class DefaultIssueDensityHistoryController implements IssueDensityHistoryApi {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultIssueDensityHistoryController.class);

  private final UserSession userSession;
  private final DbClient dbClient;
  private final IssueCountHistoryService issueHistoryService;
  private final Clock clock;

  DefaultIssueDensityHistoryController(UserSession userSession, DbClient dbClient,
                                       IssueCountHistoryService issueHistoryService, Clock clock) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.issueHistoryService = issueHistoryService;
    this.clock = clock;
  }

  /** Validates the request, checks access, and returns issue density history. */
  @Override
  public ResponseEntity<IssueDensityHistoryResponse> getIssueDensityHistory(
      String entityId,
      String entityType,
      OffsetDateTime startDate,
      @Nullable OffsetDateTime endDate,
      @Nullable List<String> impacts,
      @Nullable List<IssueType> issueTypes,
      @Nullable List<String> ruleKeys,
      @Nullable List<IssueSeverity> severities,
      @Nullable IssueCountDistributionType sliceBy,
      @Nullable List<IssueCountStatus> statuses) {
    LOG.debug("getIssueDensityHistory invoked: entityId={}, entityType={}, startDate={}, endDate={}, filters=[{}]",
      entityId, entityType, startDate, endDate, Arrays.asList(sliceBy, impacts, issueTypes, ruleKeys, severities, statuses));

    EntityType entityTypeEnum = HistoryControllerUtils.assertValidEntityType(entityType);
    HistoryControllerUtils.HistoryDateRange dateRange = HistoryControllerUtils.assertValidDateRange(clock, startDate, endDate);
    HistoryControllerUtils.assertUserHasPermission(userSession, dbClient, entityId, entityTypeEnum);

    try {
      return ResponseEntity.ok(
        HistoryModelConverter.toApiIssueDensityHistoryResponse(
          issueHistoryService.queryIssueDensityHistory(
            entityId, entityTypeEnum, dateRange.start(), dateRange.end(),
            ruleKeys, HistoryModelConverter.toCoreSeverities(severities),
            HistoryModelConverter.toCoreIssueTypes(issueTypes),
             HistoryModelConverter.toCoreStatuses(statuses), impacts,
             HistoryModelConverter.toCoreIssueCountDistribution(sliceBy))));
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(BAD_REQUEST, e.getMessage(), e);
    }
  }
}
