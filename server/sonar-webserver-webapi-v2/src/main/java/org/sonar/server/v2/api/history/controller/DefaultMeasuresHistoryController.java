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
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.user.UserSession;
import org.sonarsource.history.HistoryDateRange;
import org.sonarsource.history.api.HistoryControllerUtils;
import org.sonarsource.history.api.mapper.HistoryModelConverter;
import org.sonarsource.history.api.model.HistoryEntityType;
import org.sonarsource.history.api.model.MeasuresHistoryResponse;
import org.sonarsource.history.api.rest.MeasuresHistoryApi;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.server.service.MeasuresHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.util.stream.Collectors.toSet;
import static org.sonar.server.v2.WebApiEndpoints.HISTORY_DOMAIN;

/** Serves measure history requests for project branches. */
@RestController
@RequestMapping(HISTORY_DOMAIN)
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
    HistoryEntityType entityType,
    String entityId,
    List<String> metricKeys,
    OffsetDateTime startDate,
    @Nullable OffsetDateTime endDate) {
    if (metricKeys.isEmpty()) {
      throw new IllegalArgumentException("metricKeys must not be empty");
    }

    EntityType entityTypeEnum = HistoryControllerUtils.ensureValidEntityType(entityType);
    HistoryDateRange dateRange = HistoryControllerUtils.ensureValidDateRange(startDate, endDate, clock);
    HistoryAuthUtils.assertUserHasPermission(userSession, dbClient, entityId, entityTypeEnum);

    validateMetricKeys(metricKeys);
    return ResponseEntity.ok(HistoryModelConverter.toApiMeasuresHistoryResponse(measuresHistoryService.queryMeasuresHistory(
      entityId, entityTypeEnum, metricKeys, dateRange.start(), dateRange.end())));
  }

  private void validateMetricKeys(List<String> metricKeys) {
    try (DbSession session = dbClient.openSession(false)) {
      Set<String> knownMetricKeys = dbClient.metricDao().selectByKeys(session, metricKeys).stream()
        .map(MetricDto::getKey)
        .collect(toSet());
      for (String metricKey : metricKeys) {
        if (!knownMetricKeys.contains(metricKey)) {
          throw new IllegalArgumentException("Invalid metric key: '%s'".formatted(metricKey));
        }
      }
    }
  }

}
