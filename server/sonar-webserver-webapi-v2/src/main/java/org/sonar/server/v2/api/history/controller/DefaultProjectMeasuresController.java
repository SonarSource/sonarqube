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
import org.sonar.db.metric.MetricDto;
import org.sonar.server.v2.security.RequireAuthentication;
import org.sonarsource.history.api.ProjectBreakdownRequestValidator;
import org.sonarsource.history.api.mapper.HistoryModelConverter;
import org.sonarsource.history.api.model.ProjectCollectionHistoryEntityType;
import org.sonarsource.history.api.model.ProjectMeasuresResponse;
import org.sonarsource.history.api.rest.ProjectMeasuresApi;
import org.sonarsource.history.server.service.ProjectMeasuresService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.sonar.server.v2.WebApiEndpoints.HISTORY_DOMAIN;

/** Serves project measure breakdown requests. */
@RestController
@RequestMapping(HISTORY_DOMAIN)
@RequireAuthentication
public class DefaultProjectMeasuresController implements ProjectMeasuresApi {

  private final DbClient dbClient;
  private final ProjectCollectionContextLoader contextLoader;
  private final ProjectMeasuresService projectMeasuresService;
  private final Clock clock;

  DefaultProjectMeasuresController(
    DbClient dbClient,
    ProjectCollectionContextLoader contextLoader,
    ProjectMeasuresService projectMeasuresService,
    Clock clock) {
    this.dbClient = dbClient;
    this.contextLoader = contextLoader;
    this.projectMeasuresService = projectMeasuresService;
    this.clock = clock;
  }

  @Override
  public ResponseEntity<ProjectMeasuresResponse> getProjectMeasures(
    String metricKey,
    @Nullable String metricValue,
    @Nullable String nameContains,
    Integer pageIndex,
    Integer pageSize,
    @Nullable String portfolioId,
    @Nullable ProjectCollectionHistoryEntityType entityType,
    @Nullable String entityId,
    @Nullable OffsetDateTime referenceDate,
    List<String> sort,
    Boolean requireValue) {
    ProjectBreakdownRequestValidator.validateReferenceDate(clock, referenceDate);
    ProjectBreakdownRequestValidator.validateSelector(portfolioId, entityType, entityId);
    ProjectCollectionContext context;
    String metricType;
    try (DbSession session = dbClient.openSession(false)) {
      MetricDto metric = dbClient.metricDao().selectByKey(session, metricKey);
      if (metric == null) {
        throw new IllegalArgumentException("Metric with key %s not found".formatted(metricKey));
      }
      metricType = metric.getValueType();
      context = portfolioId != null
        ? contextLoader.load(session, portfolioId)
        : contextLoader.load(session, Objects.requireNonNull(entityType), Objects.requireNonNull(entityId));
    }
    var response = projectMeasuresService.queryProjectMeasures(
      context.branches(), context.visibleBranchIds(), metricKey, metricType, metricValue, nameContains,
      pageIndex, pageSize, ProjectBreakdownRequestValidator.toInstant(referenceDate), sort, requireValue);
    return ResponseEntity.ok(HistoryModelConverter.toApiProjectMeasuresResponse(response));
  }
}
