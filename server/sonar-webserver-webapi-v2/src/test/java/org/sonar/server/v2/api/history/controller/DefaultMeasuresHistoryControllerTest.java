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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarsource.history.api.model.MeasureHistoryEntityType;
import org.sonarsource.history.api.model.MeasuresHistoryResponse;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.server.service.MeasuresHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.OK;

public class DefaultMeasuresHistoryControllerTest {

  private static final String ENTITY_ID = "123e4567-e89b-12d3-a456-426614174000";
  private static final String PROJECT_BRANCH_ID = "branch-1";
  private static final String PROJECT_UUID = "123e4567-e89b-12d3-a456-426614174002";
  private static final MeasureHistoryEntityType ENTITY_TYPE = MeasureHistoryEntityType.PORTFOLIO;
  private static final List<String> METRIC_KEYS = List.of("ncloc");
  private static final Instant NOW = Instant.parse("2026-07-08T01:00:00Z");

  private final MeasuresHistoryService measuresHistoryService = mock();
  private final UserSession userSession = mock();
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final BranchDao branchDao = mock();
  private final ProjectDao projectDao = mock();
  private final DefaultMeasuresHistoryController underTest = new DefaultMeasuresHistoryController(
    userSession, dbClient, measuresHistoryService, Clock.fixed(NOW, ZoneOffset.UTC));

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.projectDao()).thenReturn(projectDao);
  }

  @Test
  public void getMeasuresHistory_whenMetricKeysAreEmpty_shouldReject() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");

    assertThatThrownBy(() -> underTest.getMeasuresHistory(ENTITY_TYPE, ENTITY_ID, List.of(), startDate, null))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining("metricKeys must not be empty");

    verifyNoInteractions(measuresHistoryService);
  }

  @Test
  public void getMeasuresHistory_whenServiceRejects_shouldReturnBadRequest() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    stubProjectBranch(project(PROJECT_UUID, ComponentQualifiers.PROJECT));
    when(measuresHistoryService.queryMeasuresHistory(
      PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH, METRIC_KEYS,
      startDate.toInstant(), NOW))
      .thenThrow(new IllegalArgumentException("Unsupported metric"));

    assertThatThrownBy(() -> underTest.getMeasuresHistory(
      MeasureHistoryEntityType.PROJECT_BRANCH, PROJECT_BRANCH_ID, METRIC_KEYS, startDate, null))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining("Unsupported metric");
  }

  @Test
  public void getMeasuresHistory_whenPortfolioIsRequested_shouldReturnNotImplementedWithoutDatabaseOrHistoryInteraction() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");

    assertThatThrownBy(() -> underTest.getMeasuresHistory(ENTITY_TYPE, ENTITY_ID, METRIC_KEYS, startDate, null))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining("Portfolio history is not implemented on SonarQube Server")
      .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(NOT_IMPLEMENTED));

    verifyNoInteractions(dbClient, measuresHistoryService, userSession);
  }

  @Test
  public void getMeasuresHistory_whenStartInstantIsAfterNow_shouldReject() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T23:30:00-02:00");

    assertThatThrownBy(() -> underTest.getMeasuresHistory(
      MeasureHistoryEntityType.PROJECT_BRANCH, PROJECT_BRANCH_ID, METRIC_KEYS, startDate, null))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining("must be less than or equal to the current date");

    verifyNoInteractions(measuresHistoryService);
  }

  @Test
  public void getMeasuresHistory_whenEndInstantIsBeforeStartInstant_shouldReject() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-07T23:59:59Z");

    assertThatThrownBy(() -> underTest.getMeasuresHistory(
      MeasureHistoryEntityType.PROJECT_BRANCH, PROJECT_BRANCH_ID, METRIC_KEYS, startDate, endDate))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining("must be greater than or equal to start date");

    verifyNoInteractions(measuresHistoryService);
  }

  @Test
  public void getMeasuresHistory_whenEndInstantIsAfterNow_shouldClampEndDateAndQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-09T00:00:00Z");
    stubProjectBranch(project(PROJECT_UUID, ComponentQualifiers.PROJECT));
    org.sonarsource.history.model.MeasuresHistoryResponse response = new org.sonarsource.history.model.MeasuresHistoryResponse(
      List.of(new org.sonarsource.history.model.MeasureHistoryItem(
        Instant.parse("2026-07-08T00:00:00Z"),
        List.of(new org.sonarsource.history.model.MeasureHistoryEntry("ncloc", "INT", "42")))));
    when(measuresHistoryService.queryMeasuresHistory(
      PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH, METRIC_KEYS,
      startDate.toInstant(), NOW))
        .thenReturn(response);

    ResponseEntity<MeasuresHistoryResponse> result = underTest.getMeasuresHistory(
      MeasureHistoryEntityType.PROJECT_BRANCH, PROJECT_BRANCH_ID, METRIC_KEYS, startDate, endDate);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    assertThat(result.getBody().getMeasuresHistory()).singleElement()
      .satisfies(item -> assertThat(item.getMeasures()).singleElement()
        .satisfies(measure -> assertThat(measure.getMetric()).isEqualTo("ncloc")));
    verify(measuresHistoryService).queryMeasuresHistory(
      PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH, METRIC_KEYS,
      startDate.toInstant(), NOW);
  }

  @Test
  public void getMeasuresHistory_whenRangeHasSameInstant_shouldQueryHistoryWithSameInstantBounds() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    stubProjectBranch(project(PROJECT_UUID, ComponentQualifiers.PROJECT));
    org.sonarsource.history.model.MeasuresHistoryResponse response = mock();
    when(measuresHistoryService.queryMeasuresHistory(
      PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH, METRIC_KEYS,
      startDate.toInstant(), endDate.toInstant()))
        .thenReturn(response);

    ResponseEntity<MeasuresHistoryResponse> result = underTest.getMeasuresHistory(
      MeasureHistoryEntityType.PROJECT_BRANCH, PROJECT_BRANCH_ID, METRIC_KEYS, startDate, endDate);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    assertThat(result.getBody()).isNotNull();
    verify(measuresHistoryService).queryMeasuresHistory(
      PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH, METRIC_KEYS,
      startDate.toInstant(), endDate.toInstant());
  }

  @Test
  public void getMeasuresHistory_whenRangeExceedsSixMonthsButIsWithinOneYear_shouldQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2025-12-01T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-01T00:00:00Z");
    stubProjectBranch(project(PROJECT_UUID, ComponentQualifiers.PROJECT));
    org.sonarsource.history.model.MeasuresHistoryResponse response = mock();
    when(measuresHistoryService.queryMeasuresHistory(
      PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH, METRIC_KEYS,
      startDate.toInstant(), endDate.toInstant()))
        .thenReturn(response);

    ResponseEntity<MeasuresHistoryResponse> result = underTest.getMeasuresHistory(
      MeasureHistoryEntityType.PROJECT_BRANCH, PROJECT_BRANCH_ID, METRIC_KEYS, startDate, endDate);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    assertThat(result.getBody()).isNotNull();
    verify(measuresHistoryService).queryMeasuresHistory(
      PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH, METRIC_KEYS,
      startDate.toInstant(), endDate.toInstant());
  }

  @Test
  public void getMeasuresHistory_whenProjectBranchIsAuthorized_shouldQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    ProjectDto project = project(PROJECT_UUID, ComponentQualifiers.PROJECT);
    stubProjectBranch(project);
    org.sonarsource.history.model.MeasuresHistoryResponse response = mock();
    when(measuresHistoryService.queryMeasuresHistory(
      PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH, METRIC_KEYS,
      startDate.toInstant(), NOW))
      .thenReturn(response);

    ResponseEntity<MeasuresHistoryResponse> result = underTest.getMeasuresHistory(
      MeasureHistoryEntityType.PROJECT_BRANCH, PROJECT_BRANCH_ID, METRIC_KEYS, startDate, null);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    assertThat(result.getBody()).isNotNull();
    verify(branchDao).selectByUuid(dbSession, PROJECT_BRANCH_ID);
    verify(projectDao).selectByUuid(dbSession, PROJECT_UUID);
    verify(userSession).checkEntityPermission(ProjectPermission.USER, project);
    verify(measuresHistoryService).queryMeasuresHistory(
      PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH, METRIC_KEYS,
      startDate.toInstant(), NOW);
  }

  @Test
  public void getMeasuresHistory_whenProjectBranchIsUnauthorized_shouldNotQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    ProjectDto project = project(PROJECT_UUID, ComponentQualifiers.PROJECT);
    stubProjectBranch(project);
    doThrow(new ForbiddenException("Access forbidden"))
      .when(userSession).checkEntityPermission(ProjectPermission.USER, project);

    assertThatThrownBy(() -> underTest.getMeasuresHistory(
      MeasureHistoryEntityType.PROJECT_BRANCH, PROJECT_BRANCH_ID, METRIC_KEYS, startDate, null))
      .isInstanceOf(ForbiddenException.class);

    verifyNoInteractions(measuresHistoryService);
  }

  @Test
  public void getMeasuresHistory_whenProjectBranchBelongsToApplication_shouldCheckChildProjectsPermission() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    ProjectDto application = project(PROJECT_UUID, ComponentQualifiers.APP);
    stubProjectBranch(application);
    org.sonarsource.history.model.MeasuresHistoryResponse mockResponse = mock();
    when(measuresHistoryService.queryMeasuresHistory(
      PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH, METRIC_KEYS,
      startDate.toInstant(), NOW))
      .thenReturn(mockResponse);

    underTest.getMeasuresHistory(
      MeasureHistoryEntityType.PROJECT_BRANCH, PROJECT_BRANCH_ID, METRIC_KEYS, startDate, null);

    verify(projectDao).selectByUuid(dbSession, PROJECT_UUID);
    verify(userSession).checkEntityPermission(ProjectPermission.USER, application);
    verify(userSession).checkChildProjectsPermission(ProjectPermission.USER, application);
  }

  @Test
  public void getMeasuresHistory_whenProjectBranchIsMissing_shouldReturnNotFound() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.getMeasuresHistory(
      MeasureHistoryEntityType.PROJECT_BRANCH, PROJECT_BRANCH_ID, METRIC_KEYS, startDate, null))
      .isInstanceOf(NotFoundException.class);

    verifyNoInteractions(projectDao, measuresHistoryService);
  }

  @Test
  public void getMeasuresHistory_whenProjectIsMissing_shouldReturnNotFound() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.getMeasuresHistory(
      MeasureHistoryEntityType.PROJECT_BRANCH, PROJECT_BRANCH_ID, METRIC_KEYS, startDate, null))
      .isInstanceOf(NotFoundException.class);

    verifyNoInteractions(measuresHistoryService);
  }

  private void stubProjectBranch(ProjectDto project) {
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(project.getUuid())));
    when(projectDao.selectByUuid(dbSession, project.getUuid())).thenReturn(Optional.of(project));
  }

  private static ProjectDto project(String uuid, String qualifier) {
    return new ProjectDto()
      .setUuid(uuid)
      .setQualifier(qualifier);
  }
}
