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
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarsource.history.api.model.IssueCountHistoryResponse;
import org.sonarsource.history.model.EntityType;
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
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.OK;

public class DefaultIssueCountHistoryControllerTest {

  private static final String ENTITY_ID = "123e4567-e89b-12d3-a456-426614174000";
  private static final String ENTITY_TYPE = "PORTFOLIO";
  private static final String PROJECT_BRANCH_ID = "branch-1";
  private static final String PROJECT_UUID = "123e4567-e89b-12d3-a456-426614174002";
  private static final Instant NOW = Instant.parse("2026-07-08T01:00:00Z");

  private final IssueCountHistoryService issueHistoryService = mock();
  private final UserSession userSession = mock();
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final BranchDao branchDao = mock();
  private final ProjectDao projectDao = mock();
  private final DefaultIssueCountHistoryController underTest = new DefaultIssueCountHistoryController(
    userSession, dbClient, issueHistoryService, Clock.fixed(NOW, ZoneOffset.UTC));

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.projectDao()).thenReturn(projectDao);
  }

  @Test
  public void getIssueCountHistory_whenPortfolioIsRequested_shouldReturnNotImplementedWithoutDatabaseOrHistoryInteraction() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
      ENTITY_ID, ENTITY_TYPE, startDate, endDate, null, null, null, null, null, null))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining("Portfolio history is not implemented on SonarQube Server")
      .satisfies(exception -> assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(NOT_IMPLEMENTED));

    verifyNoInteractions(dbClient, issueHistoryService, userSession);
  }

  @Test
  public void getIssueCountHistory_whenEntityIdIsShort_shouldPassThroughUnchanged() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    stubProjectBranch(project(PROJECT_UUID, ComponentQualifiers.PROJECT));
    when(issueHistoryService.queryIssueCountHistory(
      eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(NOW),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenReturn(new org.sonarsource.history.model.IssueCountHistoryResponse(java.util.List.of()));

    ResponseEntity<IssueCountHistoryResponse> result = underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, "PROJECT_BRANCH", startDate, null, null, null, null, null, null, null);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    verify(branchDao).selectByUuid(dbSession, PROJECT_BRANCH_ID);
    verify(issueHistoryService).queryIssueCountHistory(
      eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(NOW),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  public void getIssueCountHistory_whenEntityTypeIsInvalid_shouldReject() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
      ENTITY_ID, "INVALID", startDate, null, null, null, null, null, null, null))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining("entityType must be one of");

    verifyNoInteractions(issueHistoryService);
  }

  @Test
  public void getIssueCountHistory_whenServiceRejects_shouldReturnBadRequest() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    stubProjectBranch(project(PROJECT_UUID, ComponentQualifiers.PROJECT));
    when(issueHistoryService.queryIssueCountHistory(
       eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(NOW),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenThrow(new IllegalArgumentException("Unsupported history filter"));

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, "PROJECT_BRANCH", startDate, null, null, null, null, null, null, null))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining("Unsupported history filter");
  }

  @Test
  public void getIssueCountHistory_whenStartInstantIsAfterNow_shouldReject() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T23:30:00-02:00");

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, "PROJECT_BRANCH", startDate, null, null, null, null, null, null, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("must be less than or equal to the current date");

    verifyNoInteractions(issueHistoryService);
  }

  @Test
  public void getIssueCountHistory_whenEndInstantIsAfterNow_shouldClampEndDateAndQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-09T00:00:00Z");
    stubProjectBranch(project(PROJECT_UUID, ComponentQualifiers.PROJECT));
    org.sonarsource.history.model.IssueCountHistoryResponse response = new org.sonarsource.history.model.IssueCountHistoryResponse(
      java.util.List.of(new org.sonarsource.history.model.IssueCountHistoryPoint(
        Instant.parse("2026-07-08T00:00:00Z"),
        java.util.List.of(new org.sonarsource.history.model.IssueCountDistribution("SAFE", 3)))));
    when(issueHistoryService.queryIssueCountHistory(
       eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(NOW),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenReturn(response);

    ResponseEntity<IssueCountHistoryResponse> result = underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, "PROJECT_BRANCH", startDate, endDate, null, null, null, null, null, null);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    assertThat(result.getBody().getIssueCountHistory()).singleElement()
      .satisfies(item -> assertThat(item.getDistribution()).singleElement()
        .satisfies(distribution -> assertThat(distribution.getKey()).isEqualTo("SAFE")));
    verify(issueHistoryService).queryIssueCountHistory(
       eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(NOW),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  public void getIssueCountHistory_whenEndInstantIsBeforeStartInstant_shouldReject() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-07T23:59:59Z");

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, "PROJECT_BRANCH", startDate, endDate, null, null, null, null, null, null))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining("must be greater than or equal to start date");

    verifyNoInteractions(issueHistoryService);
  }

  @Test
  public void getIssueCountHistory_whenProjectBranchIsAuthorized_shouldQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    ProjectDto project = project(PROJECT_UUID, ComponentQualifiers.PROJECT);
    stubProjectBranch(project);
    org.sonarsource.history.model.IssueCountHistoryResponse response = new org.sonarsource.history.model.IssueCountHistoryResponse(java.util.List.of());
    when(issueHistoryService.queryIssueCountHistory(
       eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(NOW),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenReturn(response);

    ResponseEntity<IssueCountHistoryResponse> result = underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, "PROJECT_BRANCH", startDate, null, null, null, null, null, null, null);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    assertThat(result.getBody()).isNotNull();
    verify(branchDao).selectByUuid(dbSession, PROJECT_BRANCH_ID);
    verify(projectDao).selectByUuid(dbSession, PROJECT_UUID);
    verify(userSession).checkEntityPermission(ProjectPermission.USER, project);
    verify(issueHistoryService).queryIssueCountHistory(
       eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(NOW),
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
      PROJECT_BRANCH_ID, "PROJECT_BRANCH", startDate, null, null, null, null, null, null, null))
      .isInstanceOf(ForbiddenException.class);

    verifyNoInteractions(issueHistoryService);
  }

  @Test
  public void getIssueCountHistory_whenProjectBranchBelongsToApplication_shouldCheckChildProjectsPermission() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    ProjectDto application = project(PROJECT_UUID, ComponentQualifiers.APP);
    stubProjectBranch(application);
    when(issueHistoryService.queryIssueCountHistory(
       eq(PROJECT_BRANCH_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(NOW),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
       .thenReturn(new org.sonarsource.history.model.IssueCountHistoryResponse(java.util.List.of()));

    underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, "PROJECT_BRANCH", startDate, null, null, null, null, null, null, null);

    verify(projectDao).selectByUuid(dbSession, PROJECT_UUID);
    verify(userSession).checkEntityPermission(ProjectPermission.USER, application);
    verify(userSession).checkChildProjectsPermission(ProjectPermission.USER, application);
  }

  @Test
  public void getIssueCountHistory_whenProjectBranchIsMissing_shouldReturnNotFound() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.getIssueCountHistory(
      PROJECT_BRANCH_ID, "PROJECT_BRANCH", startDate, null, null, null, null, null, null, null))
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
      PROJECT_BRANCH_ID, "PROJECT_BRANCH", startDate, null, null, null, null, null, null, null))
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

}
