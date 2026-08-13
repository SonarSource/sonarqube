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
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarsource.history.api.model.HistoryEntityType;
import org.sonarsource.history.api.model.IssueCountDistributionType;
import org.sonarsource.history.api.model.IssueCountHistoryResponse;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.model.IssueCountDistribution;
import org.sonarsource.history.server.service.IssueCountHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

public class DefaultIssueCountHistoryControllerTest {

  private static final String ENTITY_ID = "123e4567-e89b-12d3-a456-426614174000";
  private static final HistoryEntityType ENTITY_TYPE = HistoryEntityType.PORTFOLIO;
  private static final String PROJECT_BRANCH_ID = "branch-1";
  private static final String PROJECT_UUID = "123e4567-e89b-12d3-a456-426614174002";
  private static final Instant NOW = Instant.parse("2026-07-08T01:00:00Z");
  private static final Instant UTC_MIDNIGHT = Instant.parse("2026-07-08T00:00:00Z");

  private final IssueCountHistoryService issueHistoryService = mock();
  private final UserSession userSession = mock();
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final BranchDao branchDao = mock();
  private final ComponentDao componentDao = mock();
  private final ProjectDao projectDao = mock();
  private final DefaultIssueCountHistoryController underTest = new DefaultIssueCountHistoryController(
    userSession, dbClient, issueHistoryService, Clock.fixed(NOW, ZoneOffset.UTC));

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.projectDao()).thenReturn(projectDao);
  }

  @Test
  public void getIssueCountHistory_whenPortfolioIsAuthorized_shouldQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    ComponentDto portfolio = portfolio();
    when(componentDao.selectByUuid(dbSession, ENTITY_ID)).thenReturn(Optional.of(portfolio));
    when(issueHistoryService.queryIssueCountHistory(
      ENTITY_ID, EntityType.PORTFOLIO, startDate.toInstant(), endDate.toInstant(),
      null, null, null, null, null, IssueCountDistribution.STATUS))
      .thenReturn(new org.sonarsource.history.model.IssueCountHistoryResponse(java.util.List.of()));

    ResponseEntity<IssueCountHistoryResponse> result = underTest.getIssueCountHistory(
      ENTITY_ID, ENTITY_TYPE, startDate, endDate, null, null, null, null, IssueCountDistributionType.STATUS, null);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    verify(componentDao).selectByUuid(dbSession, ENTITY_ID);
    verify(userSession).checkComponentPermission(ProjectPermission.USER, portfolio);
    verify(issueHistoryService).queryIssueCountHistory(
      ENTITY_ID, EntityType.PORTFOLIO, startDate.toInstant(), endDate.toInstant(),
      null, null, null, null, null, IssueCountDistribution.STATUS);
  }

  @Test
  public void getIssueCountHistory_whenPortfolioIsMissing_shouldReturnNotFound() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    when(componentDao.selectByUuid(dbSession, ENTITY_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
      ENTITY_ID, ENTITY_TYPE, startDate, null, null, null, null, null, null, null))
      .isInstanceOf(NotFoundException.class);

    verifyNoInteractions(issueHistoryService);
  }

  @Test
  public void getIssueCountHistory_whenPortfolioIsUnauthorized_shouldNotQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    ComponentDto portfolio = portfolio();
    when(componentDao.selectByUuid(dbSession, ENTITY_ID)).thenReturn(Optional.of(portfolio));
    doThrow(new ForbiddenException("Access forbidden"))
      .when(userSession).checkComponentPermission(ProjectPermission.USER, portfolio);

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
      ENTITY_ID, ENTITY_TYPE, startDate, null, null, null, null, null, null, null))
      .isInstanceOf(ForbiddenException.class);

    verifyNoInteractions(issueHistoryService);
  }

  @Test
  public void getIssueCountHistory_whenEntityIdIsShort_shouldPassThroughUnchanged() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    stubProjectBranch(project(PROJECT_UUID, ComponentQualifiers.PROJECT));
    when(issueHistoryService.queryIssueCountHistory(
      eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(UTC_MIDNIGHT),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenReturn(new org.sonarsource.history.model.IssueCountHistoryResponse(java.util.List.of()));

    ResponseEntity<IssueCountHistoryResponse> result = underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, HistoryEntityType.PROJECT_BRANCH, startDate, null, null, null, null, null, null, null);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    verify(branchDao).selectByUuid(dbSession, PROJECT_BRANCH_ID);
    verify(issueHistoryService).queryIssueCountHistory(
      eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(UTC_MIDNIGHT),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  public void getIssueCountHistory_whenServiceRejects_shouldReturnBadRequest() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    stubProjectBranch(project(PROJECT_UUID, ComponentQualifiers.PROJECT));
    when(issueHistoryService.queryIssueCountHistory(
       eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(UTC_MIDNIGHT),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenThrow(new IllegalArgumentException("Unsupported history filter"));

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, HistoryEntityType.PROJECT_BRANCH, startDate, null, null, null, null, null, null, null))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining("Unsupported history filter");
  }

  @Test
  public void getIssueCountHistory_whenStartDateIsInFuture_shouldReject() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T23:30:00-02:00");

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, HistoryEntityType.PROJECT_BRANCH, startDate, null, null, null, null, null, null, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Start date [2026-07-07T23:30-02:00] must not be in the future.");

    verifyNoInteractions(issueHistoryService);
  }

  @Test
  public void getIssueCountHistory_whenEndDateIsInFuture_shouldClampToNow() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-09T00:00:00Z");
    stubProjectBranch(project(PROJECT_UUID, ComponentQualifiers.PROJECT));
    when(issueHistoryService.queryIssueCountHistory(
      eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(NOW),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenReturn(new org.sonarsource.history.model.IssueCountHistoryResponse(java.util.List.of()));

    ResponseEntity<IssueCountHistoryResponse> result = underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, HistoryEntityType.PROJECT_BRANCH, startDate, endDate, null, null, null, null, null, null);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    verify(issueHistoryService).queryIssueCountHistory(
      eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(NOW),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  public void getIssueCountHistory_whenEndInstantIsBeforeStartInstant_shouldReject() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-07T23:59:59Z");

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
       PROJECT_BRANCH_ID, HistoryEntityType.PROJECT_BRANCH, startDate, endDate, null, null, null, null, null, null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("End date [2026-07-07T23:59:59Z] must be greater than or equal to start date [2026-07-08T00:00Z].");

    verifyNoInteractions(issueHistoryService);
  }

  @Test
  public void getIssueCountHistory_whenStartDateIsOlderThanOneYear_shouldQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2020-12-01T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-01T00:00:00Z");
    stubProjectBranch(project(PROJECT_UUID, ComponentQualifiers.PROJECT));
    when(issueHistoryService.queryIssueCountHistory(
      eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(endDate.toInstant()),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenReturn(new org.sonarsource.history.model.IssueCountHistoryResponse(java.util.List.of()));

    ResponseEntity<IssueCountHistoryResponse> result = underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, HistoryEntityType.PROJECT_BRANCH, startDate, endDate, null, null, null, null, null, null);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    verify(issueHistoryService).queryIssueCountHistory(
      eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(endDate.toInstant()),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  public void getIssueCountHistory_whenProjectBranchIsAuthorized_shouldQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    ProjectDto project = project(PROJECT_UUID, ComponentQualifiers.PROJECT);
    stubProjectBranch(project);
    org.sonarsource.history.model.IssueCountHistoryResponse response = new org.sonarsource.history.model.IssueCountHistoryResponse(java.util.List.of());
    when(issueHistoryService.queryIssueCountHistory(
       eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(UTC_MIDNIGHT),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenReturn(response);

    ResponseEntity<IssueCountHistoryResponse> result = underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, HistoryEntityType.PROJECT_BRANCH, startDate, null, null, null, null, null, null, null);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    assertThat(result.getBody()).isNotNull();
    verify(branchDao).selectByUuid(dbSession, PROJECT_BRANCH_ID);
    verify(projectDao).selectByUuid(dbSession, PROJECT_UUID);
    verify(userSession).checkEntityPermission(ProjectPermission.USER, project);
    verify(issueHistoryService).queryIssueCountHistory(
       eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(UTC_MIDNIGHT),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  public void getIssueCountHistory_whenApplicationBranchIsAuthorized_shouldQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    ProjectDto application = project(PROJECT_UUID, ComponentQualifiers.APP);
    stubProjectBranch(application);
    when(issueHistoryService.queryIssueCountHistory(
      eq(PROJECT_BRANCH_ID), eq(EntityType.APPLICATION), eq(startDate.toInstant()), eq(UTC_MIDNIGHT),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenReturn(new org.sonarsource.history.model.IssueCountHistoryResponse(java.util.List.of()));

    ResponseEntity<IssueCountHistoryResponse> result = underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, HistoryEntityType.APPLICATION, startDate, null, null, null, null, null, null, null);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    verify(userSession).checkEntityPermission(ProjectPermission.USER, application);
    verify(userSession).checkChildProjectsPermission(ProjectPermission.USER, application);
    verify(issueHistoryService).queryIssueCountHistory(
      eq(PROJECT_BRANCH_ID), eq(EntityType.APPLICATION), eq(startDate.toInstant()), eq(UTC_MIDNIGHT),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  public void getIssueCountHistory_whenProjectBranchIsUnauthorized_shouldNotQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    ProjectDto project = project(PROJECT_UUID, ComponentQualifiers.PROJECT);
    stubProjectBranch(project);
    doThrow(new ForbiddenException("Access forbidden"))
      .when(userSession).checkEntityPermission(ProjectPermission.USER, project);

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
       PROJECT_BRANCH_ID, HistoryEntityType.PROJECT_BRANCH, startDate, null, null, null, null, null, null, null))
      .isInstanceOf(ForbiddenException.class);

    verifyNoInteractions(issueHistoryService);
  }

  @Test
  public void getIssueCountHistory_whenProjectBranchBelongsToApplication_shouldCheckChildProjectsPermission() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    ProjectDto application = project(PROJECT_UUID, ComponentQualifiers.APP);
    stubProjectBranch(application);
    when(issueHistoryService.queryIssueCountHistory(
       eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(UTC_MIDNIGHT),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
       .thenReturn(new org.sonarsource.history.model.IssueCountHistoryResponse(java.util.List.of()));

    underTest.getIssueCountHistory(
       PROJECT_BRANCH_ID, HistoryEntityType.PROJECT_BRANCH, startDate, null, null, null, null, null, null, null);

    verify(projectDao).selectByUuid(dbSession, PROJECT_UUID);
    verify(userSession).checkEntityPermission(ProjectPermission.USER, application);
    verify(userSession).checkChildProjectsPermission(ProjectPermission.USER, application);
  }

  @Test
  public void getIssueCountHistory_whenProjectBranchIsMissing_shouldReturnNotFound() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
       PROJECT_BRANCH_ID, HistoryEntityType.PROJECT_BRANCH, startDate, null, null, null, null, null, null, null))
      .isInstanceOf(NotFoundException.class);

    verifyNoInteractions(projectDao, issueHistoryService);
  }

  @Test
  public void getIssueCountHistory_whenProjectIsMissing_shouldReturnNotFound() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
       PROJECT_BRANCH_ID, HistoryEntityType.PROJECT_BRANCH, startDate, null, null, null, null, null, null, null))
      .isInstanceOf(NotFoundException.class);

    verifyNoInteractions(issueHistoryService);
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

  private static ComponentDto portfolio() {
    return new ComponentDto()
      .setUuid(ENTITY_ID)
      .setBranchUuid(ENTITY_ID)
      .setQualifier(ComponentQualifiers.VIEW);
  }

}
