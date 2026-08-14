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
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.user.UserSession;
import org.sonarsource.history.api.model.HistoryEntityType;
import org.sonarsource.history.api.model.IssueCountDistributionType;
import org.sonarsource.history.api.model.IssueDensityHistoryResponse;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.model.IssueCountDistribution;
import org.sonarsource.history.model.IssueDensityDistribution;
import org.sonarsource.history.model.IssueDensityHistoryPoint;
import org.sonarsource.history.server.service.IssueCountHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultIssueDensityHistoryControllerTest {

  private static final String ENTITY_ID = "123e4567-e89b-12d3-a456-426614174000";
  private static final Instant NOW = Instant.parse("2026-07-08T01:00:00Z");

  private final IssueCountHistoryService issueHistoryService = mock();
  private final UserSession userSession = mock();
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final BranchDao branchDao = mock();
  private final ComponentDao componentDao = mock();
  private final ProjectDao projectDao = mock();
  private final DefaultIssueDensityHistoryController underTest = new DefaultIssueDensityHistoryController(
    userSession, dbClient, issueHistoryService, Clock.fixed(NOW, ZoneOffset.UTC));
  private final MockMvc mockMvc = ControllerTester.getMockMvc(underTest);

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.projectDao()).thenReturn(projectDao);
  }

  @Test
  public void getIssueDensityHistory_whenStartDateIsInFuture_shouldReturnBadRequest() throws Exception {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T23:30:00-02:00");

    mockMvc.perform(get("/history/issue-density-history")
        .queryParam("entityId", ENTITY_ID)
        .queryParam("entityType", "PROJECT_BRANCH")
        .queryParam("startDate", startDate.toString()))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("{\"message\":\"Start date [2026-07-07T23:30-02:00] must not be in the future.\"}"));
    verifyNoInteractions(issueHistoryService);
  }

  @Test
  public void getIssueDensityHistory_whenServiceRejects_shouldReturnBadRequestToClient() throws Exception {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    ComponentDto portfolio = portfolio();
    when(componentDao.selectByUuid(dbSession, ENTITY_ID)).thenReturn(Optional.of(portfolio));
    when(issueHistoryService.queryIssueDensityHistory(
      ENTITY_ID, EntityType.PORTFOLIO, startDate.toInstant(), NOW.minusSeconds(3600),
      null, null, null, null, null, null))
      .thenThrow(new IllegalArgumentException("Unsupported density filter"));
    mockMvc.perform(get("/history/issue-density-history")
        .queryParam("entityId", ENTITY_ID)
        .queryParam("entityType", "PORTFOLIO")
        .queryParam("startDate", startDate.toString()))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("{\"message\":\"Unsupported density filter\"}"));
  }

  @Test
  public void getIssueDensityHistory_whenEndDateIsInFuture_shouldClampToNow() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-09T00:00:00Z");
    ProjectDto project = new ProjectDto().setUuid("project-uuid").setQualifier(ComponentQualifiers.PROJECT);
    when(branchDao.selectByUuid(dbSession, ENTITY_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(ENTITY_ID).setProjectUuid(project.getUuid())));
    when(projectDao.selectByUuid(dbSession, project.getUuid())).thenReturn(Optional.of(project));
    when(issueHistoryService.queryIssueDensityHistory(
      eq(ENTITY_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(NOW),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenReturn(new org.sonarsource.history.model.IssueDensityHistoryResponse(List.of()));

    ResponseEntity<IssueDensityHistoryResponse> result = underTest.getIssueDensityHistory(
      ENTITY_ID, HistoryEntityType.PROJECT_BRANCH, startDate, endDate, null, null, null, null, null, null);
    assertThat(result.getStatusCode()).isEqualTo(OK);
    verify(issueHistoryService).queryIssueDensityHistory(
      eq(ENTITY_ID), eq(EntityType.PROJECT_BRANCH), eq(startDate.toInstant()), eq(NOW),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
  }

  @Test
  public void getIssueDensityHistory_whenEndInstantIsBeforeStartInstant_shouldReturnBadRequest() throws Exception {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-07T23:59:59Z");

    mockMvc.perform(get("/history/issue-density-history")
        .queryParam("entityId", ENTITY_ID)
        .queryParam("entityType", "PROJECT_BRANCH")
        .queryParam("startDate", startDate.toString())
        .queryParam("endDate", endDate.toString()))
      .andExpect(status().isBadRequest())
      .andExpect(content().json("{\"message\":\"End date [2026-07-07T23:59:59Z] must be greater than or equal to start date [2026-07-08T00:00Z].\"}"));

    verifyNoInteractions(issueHistoryService);
  }

  @Test
  public void getIssueDensityHistory_whenPortfolioIsAuthorized_shouldQueryIssueCountService() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    ComponentDto portfolio = new ComponentDto()
      .setUuid(ENTITY_ID)
      .setBranchUuid(ENTITY_ID)
      .setQualifier(ComponentQualifiers.VIEW);
    when(componentDao.selectByUuid(dbSession, ENTITY_ID)).thenReturn(Optional.of(portfolio));
    when(issueHistoryService.queryIssueDensityHistory(
      ENTITY_ID, EntityType.PORTFOLIO, startDate.toInstant(), endDate.toInstant(),
      null, null, null, null, null, IssueCountDistribution.STATUS))
       .thenReturn(new org.sonarsource.history.model.IssueDensityHistoryResponse(List.of(
        new IssueDensityHistoryPoint(
          startDate.toInstant(),
          List.of(new IssueDensityDistribution("all", null))))));

    ResponseEntity<IssueDensityHistoryResponse> result = underTest.getIssueDensityHistory(
      ENTITY_ID, HistoryEntityType.PORTFOLIO, startDate, endDate, null, null, null, null, IssueCountDistributionType.STATUS, null);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    assertThat(result.getBody().getIssueDensityHistory()).singleElement()
      .satisfies(item -> assertThat(item.getDistribution()).singleElement()
        .satisfies(distribution -> assertThat(distribution.getValue()).isNull()));
    verify(userSession).checkComponentPermission(ProjectPermission.USER, portfolio);
    verify(issueHistoryService).queryIssueDensityHistory(
      ENTITY_ID, EntityType.PORTFOLIO, startDate.toInstant(), endDate.toInstant(),
      null, null, null, null, null, IssueCountDistribution.STATUS);
  }

  @Test
  public void getIssueDensityHistory_whenApplicationBranchIsAuthorized_shouldQueryHistory() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    ProjectDto application = new ProjectDto().setUuid("application-uuid").setQualifier(ComponentQualifiers.APP);
    when(branchDao.selectByUuid(dbSession, ENTITY_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(ENTITY_ID).setProjectUuid(application.getUuid())));
    when(projectDao.selectByUuid(dbSession, application.getUuid())).thenReturn(Optional.of(application));
    when(issueHistoryService.queryIssueDensityHistory(
      ENTITY_ID, EntityType.APPLICATION, startDate.toInstant(), endDate.toInstant(),
      null, null, null, null, null, IssueCountDistribution.STATUS))
      .thenReturn(new org.sonarsource.history.model.IssueDensityHistoryResponse(List.of()));

    ResponseEntity<IssueDensityHistoryResponse> result = underTest.getIssueDensityHistory(
      ENTITY_ID, HistoryEntityType.APPLICATION, startDate, endDate, null, null, null, null,
      IssueCountDistributionType.STATUS, null);

    assertThat(result.getStatusCode()).isEqualTo(OK);
    verify(userSession).checkEntityPermission(ProjectPermission.USER, application);
    verify(userSession).checkChildProjectsPermission(ProjectPermission.USER, application);
    verify(issueHistoryService).queryIssueDensityHistory(
      ENTITY_ID, EntityType.APPLICATION, startDate.toInstant(), endDate.toInstant(),
      null, null, null, null, null, IssueCountDistribution.STATUS);
  }

  private static ComponentDto portfolio() {
    return new ComponentDto()
      .setUuid(ENTITY_ID)
      .setBranchUuid(ENTITY_ID)
      .setQualifier(ComponentQualifiers.VIEW);
  }

}
