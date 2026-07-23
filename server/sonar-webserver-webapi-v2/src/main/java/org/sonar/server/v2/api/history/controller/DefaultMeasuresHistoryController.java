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
import org.sonar.db.DbClient;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.security.RequireAuthentication;
import org.sonarsource.history.HistoryDateRangeException;
import org.sonarsource.history.api.mapper.HistoryModelConverter;
import org.sonarsource.history.api.model.MeasureHistoryEntityType;
import org.sonarsource.history.api.model.MeasuresHistoryResponse;
import org.sonarsource.history.api.rest.MeasuresHistoryApi;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.server.service.MeasuresHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.sonar.server.v2.WebApiEndpoints.HISTORY_DOMAIN;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;

/** Serves measure history requests for authenticated project branches. */
@RestController
@RequestMapping(HISTORY_DOMAIN)
@RequireAuthentication
public class DefaultMeasuresHistoryController implements MeasuresHistoryApi {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final MeasuresHistoryService measuresHistoryService;
  private final Clock clock;

  DefaultMeasuresHistoryController(UserSession userSession, DbClient dbClient, MeasuresHistoryService measuresHistoryService, Clock clock) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.measuresHistoryService = measuresHistoryService;
    this.clock = clock;
  }

  /** Validates the request, checks access, and returns measure history. */
  @Override
  public ResponseEntity<MeasuresHistoryResponse> getMeasuresHistory(
    MeasureHistoryEntityType entityType,
    String entityId,
    List<String> metricKeys,
    OffsetDateTime startDate,
    @Nullable OffsetDateTime endDate) {
    if (metricKeys.isEmpty()) {
      throw new ResponseStatusException(BAD_REQUEST, "metricKeys must not be empty");
    }

    EntityType entityTypeEnum;
    try {
      entityTypeEnum = EntityType.valueOf(entityType.getValue());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(BAD_REQUEST, "entityType must be one of: " + Arrays.toString(EntityType.values()), e);
    }

    if (EntityType.PORTFOLIO.equals(entityTypeEnum)) {
      throw new ResponseStatusException(NOT_IMPLEMENTED, "Portfolio history is not implemented on SonarQube Server");
    }

    HistoryControllerUtils.HistoryDateRange dateRange;
    try {
      dateRange = HistoryControllerUtils.normalize(clock, startDate, endDate);
    } catch (HistoryDateRangeException e) {
      throw new ResponseStatusException(BAD_REQUEST, e.getMessage(), e);
    }

    HistoryControllerUtils.checkPermission(userSession, dbClient, entityId, entityTypeEnum);

    try {
      return ResponseEntity.ok(HistoryModelConverter.toApiMeasuresHistoryResponse(measuresHistoryService.queryMeasuresHistory(
        entityId, entityTypeEnum, metricKeys, dateRange.start(), dateRange.end())));
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(BAD_REQUEST, e.getMessage(), e);
    }
  }

}
