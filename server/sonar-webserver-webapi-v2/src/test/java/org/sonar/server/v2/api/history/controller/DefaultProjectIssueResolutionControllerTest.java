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
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.v2.api.ControllerTester;
import org.sonarsource.history.api.model.IssueSeverity;
import org.sonarsource.history.api.model.IssueType;
import org.sonarsource.history.api.model.ProjectCollectionHistoryEntityType;
import org.sonarsource.history.api.model.ProjectIssueResolutionResponse;
import org.sonarsource.history.api.model.ProjectIssueResolutionStatistic;
import org.sonarsource.history.model.IssueCountHistoryFilters;
import org.sonarsource.history.model.ProjectBranch;
import org.sonarsource.history.server.service.ProjectIssueResolutionService;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultProjectIssueResolutionControllerTest {

  private static final String PORTFOLIO_ID = "portfolio-uuid";
  private static final Instant NOW = Instant.parse("2026-07-08T01:00:00Z");
  private static final Instant TREND_SINCE = Instant.parse("2026-06-20T00:00:00Z");
  private static final ProjectCollectionContext CONTEXT = new ProjectCollectionContext(
    List.of(new ProjectBranch("branch-1", "main", "project-key", "Project")), Set.of("branch-1"));

  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final ProjectCollectionContextLoader contextLoader = mock();
  private final ProjectIssueResolutionService projectIssueResolutionService = mock();
  private final DefaultProjectIssueResolutionController underTest = new DefaultProjectIssueResolutionController(
    dbClient, contextLoader, projectIssueResolutionService, Clock.fixed(NOW, ZoneOffset.UTC));
  private final MockMvc mockMvc = ControllerTester.getMockMvc(underTest);

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(contextLoader.load(dbSession, PORTFOLIO_ID)).thenReturn(CONTEXT);
  }

  @Test
  public void getProjectIssueResolutionLoadsPortfolioContextAndPassesFiltersAndTrend() {
    when(projectIssueResolutionService.getProjectIssueResolution(
      eq(CONTEXT.branches()),
      eq(CONTEXT.visibleBranchIds()),
      eq(org.sonarsource.history.model.IssueResolutionStatistic.RESOLVED_ISSUES),
      argThat(filters -> filters.equals(new IssueCountHistoryFilters(null, List.of(4), List.of(3), null, List.of(
        new org.sonarsource.history.model.IssueCountHistoryImpactFilter("MAINTAINABILITY", (short) 4))))),
      eq("payments"),
      eq(NOW),
      eq(TREND_SINCE),
      eq(2),
      eq(50)))
      .thenReturn(new org.sonarsource.history.model.ProjectIssueResolutionResponse(
        org.sonarsource.history.model.IssueResolutionStatistic.RESOLVED_ISSUES,
        List.of(new org.sonarsource.history.model.ProjectIssueResolution(
          "branch-1", "Project", "project-key", "main", 42L, 40.0)),
        new org.sonarsource.history.model.Pagination(2, 50, 1)));

    ResponseEntity<ProjectIssueResolutionResponse> response = underTest.getProjectIssueResolution(
      ProjectIssueResolutionStatistic.RESOLVED_ISSUES,
      PORTFOLIO_ID,
      null,
      null,
      List.of(IssueSeverity.HIGH),
      List.of(IssueType.VULNERABILITY),
      List.of("MAINTAINABILITY:HIGH"),
      "payments",
      OffsetDateTime.parse("2026-06-20T00:00:00Z"),
      2,
      50);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    assertThat(response.getBody().getProjectIssueResolution()).singleElement()
      .satisfies(item -> assertThat(item.getTrendPercentage()).isEqualTo(40.0));
    verify(contextLoader).load(dbSession, PORTFOLIO_ID);
  }

  @Test
  public void getProjectIssueResolutionLoadsTypedContext() {
    when(contextLoader.load(dbSession, ProjectCollectionHistoryEntityType.APPLICATION, "application-branch-uuid"))
      .thenReturn(CONTEXT);
    when(projectIssueResolutionService.getProjectIssueResolution(
      eq(CONTEXT.branches()),
      eq(CONTEXT.visibleBranchIds()),
      eq(org.sonarsource.history.model.IssueResolutionStatistic.MTTR),
      argThat(filters -> filters.equals(new IssueCountHistoryFilters(null, null, null, null, null))),
      eq(null),
      eq(NOW),
      eq(null),
      eq(1),
      eq(50)))
      .thenReturn(new org.sonarsource.history.model.ProjectIssueResolutionResponse(
        org.sonarsource.history.model.IssueResolutionStatistic.MTTR,
        List.of(),
        new org.sonarsource.history.model.Pagination(1, 50, 0)));

    underTest.getProjectIssueResolution(
      ProjectIssueResolutionStatistic.MTTR,
      null,
      ProjectCollectionHistoryEntityType.APPLICATION,
      "application-branch-uuid",
      null,
      null,
      null,
      null,
      null,
      1,
      50);

    verify(contextLoader).load(dbSession, ProjectCollectionHistoryEntityType.APPLICATION, "application-branch-uuid");
  }

  @Test
  public void getProjectIssueResolutionRejectsInvalidSelectorBeforeLoadingContext() throws Exception {
    mockMvc.perform(get("/history/project-issue-resolution")
        .queryParam("statistic", "MTTR")
        .queryParam("portfolioId", PORTFOLIO_ID)
        .queryParam("entityType", "PORTFOLIO")
        .queryParam("entityId", PORTFOLIO_ID))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"portfolioId cannot be combined with entityType or entityId\"}"));

    verifyNoInteractions(contextLoader, projectIssueResolutionService);
  }

  @Test
  public void getProjectIssueResolutionRejectsCurrentDayTrendBeforeLoadingContext() throws Exception {
    OffsetDateTime currentDay = OffsetDateTime.parse("2026-07-08T00:00:00Z");

    mockMvc.perform(get("/history/project-issue-resolution")
        .queryParam("statistic", "MTTR")
        .queryParam("portfolioId", PORTFOLIO_ID)
        .queryParam("trendSince", currentDay.toString()))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"referenceDate 2026-07-08T00:00:00Z must be before the current date\"}"));

    verifyNoInteractions(contextLoader, projectIssueResolutionService);
  }
}
