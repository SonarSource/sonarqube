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
import org.sonar.db.portfolio.PortfolioDao;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.user.UserSession;
import org.sonarsource.history.api.model.HistoryEntityType;
import org.sonarsource.history.api.model.IssueResolutionHistoryResponse;
import org.sonarsource.history.api.model.IssueResolutionSliceBy;
import org.sonarsource.history.api.model.IssueResolutionStatistic;
import org.sonarsource.history.api.model.IssueSeverity;
import org.sonarsource.history.api.model.IssueType;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.model.IssueCountHistoryFilters;
import org.sonarsource.history.model.IssueResolutionHistoryQuery;
import org.sonarsource.history.model.IssueResolutionHistoryPoint;
import org.sonarsource.history.server.service.IssueTtrHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultIssueResolutionHistoryControllerTest {

  private static final String ENTITY_ID = "123e4567-e89b-12d3-a456-426614174000";
  private static final String PROJECT_BRANCH_ID = "branch-1";
  private static final String PROJECT_UUID = "123e4567-e89b-12d3-a456-426614174002";
  private static final String APPLICATION_BRANCH_ID = "application-branch-uuid";
  private static final String APPLICATION_UUID = "application-uuid";
  private static final Instant NOW = Instant.parse("2026-07-08T01:00:00Z");
  private static final OffsetDateTime START = OffsetDateTime.parse("2020-07-07T12:00:00Z");
  private static final OffsetDateTime END = OffsetDateTime.parse("2020-07-08T12:00:00Z");

  private final IssueTtrHistoryService issueTtrHistoryService = mock();
  private final UserSession userSession = mock();
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final BranchDao branchDao = mock();
  private final PortfolioDao portfolioDao = mock();
  private final ProjectDao projectDao = mock();
  private final DefaultIssueResolutionHistoryController underTest = new DefaultIssueResolutionHistoryController(
    userSession, dbClient, issueTtrHistoryService, Clock.fixed(NOW, ZoneOffset.UTC));
  private final MockMvc mockMvc = ControllerTester.getMockMvc(underTest);

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.portfolioDao()).thenReturn(portfolioDao);
    when(dbClient.projectDao()).thenReturn(projectDao);
  }

  @Test
  public void getIssueResolutionHistoryUsesAuthorizationAndPreservesDatesOlderThanOneYear() {
    PortfolioDto portfolio = new PortfolioDto()
      .setUuid(ENTITY_ID)
      .setRootUuid(ENTITY_ID);
    when(portfolioDao.selectByUuid(dbSession, ENTITY_ID)).thenReturn(Optional.of(portfolio));
    when(issueTtrHistoryService.query(
      eq(org.sonarsource.history.model.IssueResolutionStatistic.MTTR),
      argThat(query -> query.entityId().equals(ENTITY_ID)
        && query.entityType() == EntityType.PORTFOLIO
        && query.startDate().equals(START.toInstant())
        && query.endDate().equals(END.toInstant())
        && query.sliceBy().equals("SEVERITY")
        && query.filters().equals(new IssueCountHistoryFilters(null, List.of(4), List.of(3), null, null)))))
      .thenReturn(List.of(new IssueResolutionHistoryPoint(END.toInstant(), List.of())));

    ResponseEntity<IssueResolutionHistoryResponse> response = underTest.getIssueResolutionHistory(
      ENTITY_ID,
      HistoryEntityType.PORTFOLIO,
      IssueResolutionStatistic.MTTR,
      START,
      END,
      null,
      List.of(IssueType.VULNERABILITY),
      List.of(IssueSeverity.HIGH),
      IssueResolutionSliceBy.SEVERITY);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    assertThat(response.getBody().getStatistic()).isEqualTo(IssueResolutionStatistic.MTTR);
    assertThat(response.getBody().getIssueResolutionHistory()).hasSize(1);
    verify(userSession).checkEntityPermission(ProjectPermission.USER, portfolio);
  }

  @Test
  public void getIssueResolutionHistory_whenStartDateIsInFuture_shouldReturnBadRequest() throws Exception {
    OffsetDateTime future = OffsetDateTime.parse("2026-07-08T02:00:00Z");

    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", ENTITY_ID)
        .queryParam("entityType", "PROJECT_BRANCH")
        .queryParam("statistic", "RESOLVED_ISSUES")
        .queryParam("startDate", future.toString()))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("{\"message\":\"Start date [2026-07-08T02:00Z] must not be in the future.\"}"));

    verifyNoInteractions(issueTtrHistoryService);
  }

  @Test
  public void getIssueResolutionHistory_whenStatisticIsInvalid_shouldReturnBadRequest() throws Exception {
    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", ENTITY_ID)
        .queryParam("entityType", "PROJECT_BRANCH")
        .queryParam("statistic", "INVALID")
        .queryParam("startDate", START.toString()))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Invalid parameter type.\"}"));

    verifyNoInteractions(issueTtrHistoryService);
  }

  @Test
  public void getIssueResolutionHistory_whenEndInstantIsBeforeStartInstant_shouldReturnBadRequest() throws Exception {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-07T23:59:59Z");

    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", PROJECT_BRANCH_ID)
        .queryParam("entityType", "PROJECT_BRANCH")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", startDate.toString())
        .queryParam("endDate", endDate.toString()))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"End date [2026-07-07T23:59:59Z] must be greater than or equal to start date [2026-07-08T00:00Z].\"}"));

    verifyNoInteractions(issueTtrHistoryService);
  }

  @Test
  public void getIssueResolutionHistory_whenServiceRejects_shouldReturnBadRequestToClient() throws Exception {
    PortfolioDto portfolio = portfolio();
    when(portfolioDao.selectByUuid(dbSession, ENTITY_ID)).thenReturn(Optional.of(portfolio));
    when(issueTtrHistoryService.query(
      eq(org.sonarsource.history.model.IssueResolutionStatistic.MTTR), any(IssueResolutionHistoryQuery.class)))
      .thenThrow(new IllegalArgumentException("Unsupported resolution filter"));
    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", ENTITY_ID)
        .queryParam("entityType", "PORTFOLIO")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", START.toString()))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("{\"message\":\"Unsupported resolution filter\"}"));
  }

  @Test
  public void getIssueResolutionHistory_whenSecurityHotspotIssueTypeIsRequested_shouldReturnBadRequest() throws Exception {
    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", ENTITY_ID)
        .queryParam("entityType", "PORTFOLIO")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", START.toString())
        .queryParam("issueTypes", "SECURITY_HOTSPOT"))
      .andExpect(status().isBadRequest());
    verifyNoInteractions(dbClient, issueTtrHistoryService);
  }

  @Test
  public void getIssueResolutionHistory_whenProjectBranchIsAuthorized_shouldQueryHistory() {
    ProjectDto project = project(PROJECT_UUID, ComponentQualifiers.PROJECT);
    stubProjectBranch(project);
    when(issueTtrHistoryService.query(
      eq(org.sonarsource.history.model.IssueResolutionStatistic.MTTR), any(IssueResolutionHistoryQuery.class)))
      .thenReturn(List.of());

    ResponseEntity<IssueResolutionHistoryResponse> response = underTest.getIssueResolutionHistory(
      PROJECT_BRANCH_ID,
      HistoryEntityType.PROJECT_BRANCH,
      IssueResolutionStatistic.MTTR,
      START,
      END,
      null,
      null,
      null,
      null);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    assertThat(response.getBody()).isNotNull();
    verify(branchDao).selectByUuid(dbSession, PROJECT_BRANCH_ID);
    verify(projectDao).selectByUuid(dbSession, PROJECT_UUID);
    verify(userSession).checkEntityPermission(ProjectPermission.USER, project);
    verify(issueTtrHistoryService).query(
      eq(org.sonarsource.history.model.IssueResolutionStatistic.MTTR), any(IssueResolutionHistoryQuery.class));
  }

  @Test
  public void getIssueResolutionHistory_whenApplicationIsAuthorized_shouldQueryHistory() {
    ProjectDto application = project(APPLICATION_UUID, ComponentQualifiers.APP);
    stubApplicationBranch(application);
    when(issueTtrHistoryService.query(
      eq(org.sonarsource.history.model.IssueResolutionStatistic.MTTR), any(IssueResolutionHistoryQuery.class)))
      .thenReturn(List.of());

    ResponseEntity<IssueResolutionHistoryResponse> response = underTest.getIssueResolutionHistory(
      APPLICATION_BRANCH_ID,
      HistoryEntityType.APPLICATION,
      IssueResolutionStatistic.MTTR,
      START,
      END,
      null,
      null,
      null,
      null);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    assertThat(response.getBody()).isNotNull();
    verify(userSession).checkEntityPermission(ProjectPermission.USER, application);
    verify(userSession).checkChildProjectsPermission(ProjectPermission.USER, application);
    verify(issueTtrHistoryService).query(
      eq(org.sonarsource.history.model.IssueResolutionStatistic.MTTR), any(IssueResolutionHistoryQuery.class));
  }

  @Test
  public void getIssueResolutionHistory_whenProjectBranchBelongsToApplicationAndChildProjectsAreUnauthorized_shouldReturnForbidden() throws Exception {
    ProjectDto application = project(APPLICATION_UUID, ComponentQualifiers.APP);
    stubProjectBranch(application);
    doThrow(new ForbiddenException("Insufficient privileges"))
      .when(userSession).checkChildProjectsPermission(ProjectPermission.USER, application);

    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", PROJECT_BRANCH_ID)
        .queryParam("entityType", "PROJECT_BRANCH")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", START.toString()))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));

    verifyNoInteractions(issueTtrHistoryService);
  }

  @Test
  public void getIssueResolutionHistory_whenProjectIsUnauthorized_shouldReturnForbidden() throws Exception {
    ProjectDto project = project(PROJECT_UUID, ComponentQualifiers.PROJECT);
    stubProjectBranch(project);
    doThrow(new ForbiddenException("Insufficient privileges"))
      .when(userSession).checkEntityPermission(ProjectPermission.USER, project);

    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", PROJECT_BRANCH_ID)
        .queryParam("entityType", "PROJECT_BRANCH")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", START.toString()))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));

    verifyNoInteractions(issueTtrHistoryService);
  }

  @Test
  public void getIssueResolutionHistory_whenApplicationIsUnauthorized_shouldReturnForbidden() throws Exception {
    ProjectDto application = project(APPLICATION_UUID, ComponentQualifiers.APP);
    stubApplicationBranch(application);
    doThrow(new ForbiddenException("Insufficient privileges"))
      .when(userSession).checkEntityPermission(ProjectPermission.USER, application);

    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", APPLICATION_BRANCH_ID)
        .queryParam("entityType", "APPLICATION")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", START.toString()))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));

    verifyNoInteractions(issueTtrHistoryService);
  }

  @Test
  public void getIssueResolutionHistory_whenApplicationChildProjectsAreUnauthorized_shouldReturnForbidden() throws Exception {
    ProjectDto application = project(APPLICATION_UUID, ComponentQualifiers.APP);
    stubApplicationBranch(application);
    doThrow(new ForbiddenException("Insufficient privileges"))
      .when(userSession).checkChildProjectsPermission(ProjectPermission.USER, application);

    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", APPLICATION_BRANCH_ID)
        .queryParam("entityType", "APPLICATION")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", START.toString()))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));

    verifyNoInteractions(issueTtrHistoryService);
  }

  @Test
  public void getIssueResolutionHistory_whenPortfolioIsUnauthorized_shouldReturnForbidden() throws Exception {
    PortfolioDto portfolio = portfolio();
    when(portfolioDao.selectByUuid(dbSession, ENTITY_ID)).thenReturn(Optional.of(portfolio));
    doThrow(new ForbiddenException("Insufficient privileges"))
      .when(userSession).checkEntityPermission(ProjectPermission.USER, portfolio);

    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", ENTITY_ID)
        .queryParam("entityType", "PORTFOLIO")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", START.toString()))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));

    verifyNoInteractions(issueTtrHistoryService);
  }

  @Test
  public void getIssueResolutionHistory_whenPortfolioIsMissing_shouldReturnNotFound() throws Exception {
    when(portfolioDao.selectByUuid(dbSession, ENTITY_ID)).thenReturn(Optional.empty());

    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", ENTITY_ID)
        .queryParam("entityType", "PORTFOLIO")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", START.toString()))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Portfolio with uuid '123e4567-e89b-12d3-a456-426614174000' not found\"}"));

    verifyNoInteractions(issueTtrHistoryService);
  }

  @Test
  public void getIssueResolutionHistory_whenProjectBranchIsMissing_shouldReturnNotFound() throws Exception {
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID)).thenReturn(Optional.empty());

    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", PROJECT_BRANCH_ID)
        .queryParam("entityType", "PROJECT_BRANCH")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", START.toString()))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Project branch with uuid 'branch-1' not found\"}"));

    verifyNoInteractions(issueTtrHistoryService);
  }

  @Test
  public void getIssueResolutionHistory_whenProjectIsMissing_shouldReturnNotFound() throws Exception {
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.empty());

    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", PROJECT_BRANCH_ID)
        .queryParam("entityType", "PROJECT_BRANCH")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", START.toString()))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Project with uuid '123e4567-e89b-12d3-a456-426614174002' not found\"}"));

    verifyNoInteractions(issueTtrHistoryService);
  }

  @Test
  public void getIssueResolutionHistory_whenApplicationBranchIsMissing_shouldReturnNotFound() throws Exception {
    when(branchDao.selectByUuid(dbSession, APPLICATION_BRANCH_ID)).thenReturn(Optional.empty());

    mockMvc.perform(get("/history/issue-resolution-history")
        .queryParam("entityId", APPLICATION_BRANCH_ID)
        .queryParam("entityType", "APPLICATION")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", START.toString()))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Portfolio or application branch 'application-branch-uuid' not found\"}"));

    verifyNoInteractions(issueTtrHistoryService);
  }

  private static PortfolioDto portfolio() {
    return new PortfolioDto()
      .setUuid(ENTITY_ID)
      .setRootUuid(ENTITY_ID);
  }

  private void stubProjectBranch(ProjectDto project) {
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(project.getUuid())));
    when(projectDao.selectByUuid(dbSession, project.getUuid())).thenReturn(Optional.of(project));
  }

  private void stubApplicationBranch(ProjectDto application) {
    when(branchDao.selectByUuid(dbSession, APPLICATION_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(APPLICATION_BRANCH_ID).setProjectUuid(application.getUuid())));
    when(projectDao.selectByUuid(dbSession, application.getUuid())).thenReturn(Optional.of(application));
  }

  private static ProjectDto project(String uuid, String qualifier) {
    return new ProjectDto()
      .setUuid(uuid)
      .setQualifier(qualifier);
  }
}
