/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.sonar.server.v2.api.history.controller;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.server.user.ServerUserSession;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.ControllerTester;
import org.sonar.server.v2.security.RequireAuthentication;
import org.sonarsource.history.api.model.HistoryEntityType;
import org.sonarsource.history.api.model.IssueResolutionStatistic;
import org.sonarsource.history.api.model.ProjectIssueResolutionStatistic;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.model.IssueCountHistoryFilters;
import org.sonarsource.history.model.IssueCountHistoryResponse;
import org.sonarsource.history.model.IssueDensityHistoryResponse;
import org.sonarsource.history.model.IssueResolutionHistoryPoint;
import org.sonarsource.history.model.MeasuresHistoryResponse;
import org.sonarsource.history.model.Pagination;
import org.sonarsource.history.model.ProjectIssueCountsResponse;
import org.sonarsource.history.model.ProjectIssueResolutionResponse;
import org.sonarsource.history.model.ProjectMeasuresResponse;
import org.sonarsource.history.server.service.IssueCountHistoryService;
import org.sonarsource.history.server.service.IssueTtrHistoryService;
import org.sonarsource.history.server.service.MeasuresHistoryService;
import org.sonarsource.history.server.service.ProjectIssueCountsService;
import org.sonarsource.history.server.service.ProjectIssueResolutionService;
import org.sonarsource.history.server.service.ProjectMeasuresService;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HistoryAuthorizationIT {

  private static final Instant NOW = Instant.parse("2026-07-08T01:00:00Z");
  private static final OffsetDateTime START_DATE = OffsetDateTime.parse("2026-07-07T00:00:00Z");
  private static final Instant END_DATE = Instant.parse("2026-07-08T00:00:00Z");
  private static final String METRIC_KEY = "coverage";
  private static final List<String> ISSUE_COUNTS_SORT = List.of("-issueCount");
  private static final List<String> MEASURES_SORT = List.of("-measure.currentValue");

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private ComponentDto publicPortfolio;
  private ComponentDto privatePortfolio;
  private ProjectData publicProject;
  private UserSession anonymousUserSession;
  private Clock clock;

  @BeforeEach
  void setUp() {
    publicPortfolio = db.components().insertPublicPortfolio();
    privatePortfolio = db.components().insertPrivatePortfolio();
    publicProject = db.components().insertPublicProject();
    db.components().addPortfolioProject(publicPortfolio, publicProject.getProjectDto());
    db.components().insertComponent(newProjectCopy(publicProject.getMainBranchComponent(), publicPortfolio));
    db.measures().insertMetric(metric -> metric.setKey(METRIC_KEY).setValueType("INT"));
    anonymousUserSession = new ServerUserSession(db.getDbClient(), null, false);
    clock = Clock.fixed(NOW, ZoneOffset.UTC);
  }

  @Test
  void allSingleEntityHistoryEndpoints_allowAnonymousAccessToPublicPortfolio() {
    IssueCountHistoryService issueCountHistoryService = mock();
    when(issueCountHistoryService.queryIssueCountHistory(
      eq(publicPortfolio.uuid()), eq(EntityType.PORTFOLIO), eq(START_DATE.toInstant()), eq(END_DATE),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenReturn(new IssueCountHistoryResponse(List.of()));

    IssueCountHistoryService issueDensityHistoryService = mock();
    when(issueDensityHistoryService.queryIssueDensityHistory(
      eq(publicPortfolio.uuid()), eq(EntityType.PORTFOLIO), eq(START_DATE.toInstant()), eq(END_DATE),
      isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
      .thenReturn(new IssueDensityHistoryResponse(List.of()));

    IssueTtrHistoryService issueResolutionHistoryService = mock();
    when(issueResolutionHistoryService.query(
      eq(org.sonarsource.history.model.IssueResolutionStatistic.MTTR), any()))
      .thenReturn(List.of(new IssueResolutionHistoryPoint(END_DATE, List.of())));

    MeasuresHistoryService measuresHistoryService = mock();
    when(measuresHistoryService.queryMeasuresHistory(
      publicPortfolio.uuid(), EntityType.PORTFOLIO, List.of(METRIC_KEY), START_DATE.toInstant(), END_DATE))
      .thenReturn(new MeasuresHistoryResponse(List.of()));

    assertThat(new DefaultIssueCountHistoryController(
      anonymousUserSession, db.getDbClient(), issueCountHistoryService, clock)
      .getIssueCountHistory(publicPortfolio.uuid(), HistoryEntityType.PORTFOLIO, START_DATE, null,
        null, null, null, null, null, null)
      .getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(new DefaultIssueDensityHistoryController(
      anonymousUserSession, db.getDbClient(), issueDensityHistoryService, clock)
      .getIssueDensityHistory(publicPortfolio.uuid(), HistoryEntityType.PORTFOLIO, START_DATE, null,
        null, null, null, null, null, null)
      .getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(new DefaultIssueResolutionHistoryController(
      anonymousUserSession, db.getDbClient(), issueResolutionHistoryService, clock)
      .getIssueResolutionHistory(publicPortfolio.uuid(), HistoryEntityType.PORTFOLIO,
        IssueResolutionStatistic.MTTR, START_DATE, null, null, null, null, null)
      .getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(new DefaultMeasuresHistoryController(
      anonymousUserSession, db.getDbClient(), measuresHistoryService, clock)
      .getMeasuresHistory(HistoryEntityType.PORTFOLIO, publicPortfolio.uuid(), List.of(METRIC_KEY), START_DATE, null)
      .getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void allCollectionHistoryEndpoints_allowAnonymousAccessToPublicPortfolio() {
    ProjectCollectionContextLoader contextLoader = new ProjectCollectionContextLoader(anonymousUserSession, db.getDbClient());

    ProjectIssueCountsService issueCountsService = mock();
    when(issueCountsService.getProjectIssueCounts(
      anyList(), anySet(), any(IssueCountHistoryFilters.class), isNull(), any(Instant.class),
      anyInt(), anyInt(), eq(ISSUE_COUNTS_SORT), eq(false)))
      .thenReturn(new ProjectIssueCountsResponse(
        0, List.of(), new Pagination(1, 50, 0)));

    ProjectMeasuresService measuresService = mock();
    when(measuresService.queryProjectMeasures(
      anyList(), anySet(), eq(METRIC_KEY), eq("INT"), isNull(), isNull(), anyInt(), anyInt(), any(Instant.class),
      eq(MEASURES_SORT), eq(false)))
      .thenReturn(new ProjectMeasuresResponse(
        0, new Pagination(1, 50, 0), List.of()));

    ProjectIssueResolutionService issueResolutionService = mock();
    when(issueResolutionService.getProjectIssueResolution(
      anyList(), anySet(), any(), nullable(IssueCountHistoryFilters.class), nullable(String.class), any(Instant.class),
      nullable(Instant.class), anyInt(), anyInt()))
      .thenReturn(new ProjectIssueResolutionResponse(
        org.sonarsource.history.model.IssueResolutionStatistic.MTTR, List.of(), new Pagination(1, 50, 0)));

    assertThat(new DefaultProjectIssueCountsController(
      db.getDbClient(), contextLoader, issueCountsService, clock)
      .getProjectIssueCounts(publicPortfolio.uuid(), null, null, null, null, null, null, null, null, START_DATE,
        1, 50, ISSUE_COUNTS_SORT, false)
      .getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(new DefaultProjectMeasuresController(
      db.getDbClient(), contextLoader, measuresService, clock)
      .getProjectMeasures(METRIC_KEY, null, null, 1, 50, publicPortfolio.uuid(), null, null,
        START_DATE, MEASURES_SORT, false)
      .getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(new DefaultProjectIssueResolutionController(
      db.getDbClient(), contextLoader, issueResolutionService, clock)
      .getProjectIssueResolution(ProjectIssueResolutionStatistic.MTTR, publicPortfolio.uuid(), null, null,
        null, null, null, null, START_DATE, 1, 50)
      .getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void allHistoryEndpoints_securePrivatePortfolioFromAnonymousAccess() throws Exception {
    IssueCountHistoryService issueCountHistoryService = mock();
    IssueCountHistoryService issueDensityHistoryService = mock();
    IssueTtrHistoryService issueResolutionHistoryService = mock();
    MeasuresHistoryService measuresHistoryService = mock();
    ProjectCollectionContextLoader contextLoader = new ProjectCollectionContextLoader(anonymousUserSession, db.getDbClient());

    assertForbidden(
      new DefaultIssueCountHistoryController(anonymousUserSession, db.getDbClient(), issueCountHistoryService, clock),
      get("/history/issue-count-history")
        .queryParam("entityId", privatePortfolio.uuid())
        .queryParam("entityType", "PORTFOLIO")
        .queryParam("startDate", START_DATE.toString()));
    assertForbidden(
      new DefaultIssueDensityHistoryController(anonymousUserSession, db.getDbClient(), issueDensityHistoryService, clock),
      get("/history/issue-density-history")
        .queryParam("entityId", privatePortfolio.uuid())
        .queryParam("entityType", "PORTFOLIO")
        .queryParam("startDate", START_DATE.toString()));
    assertForbidden(
      new DefaultIssueResolutionHistoryController(anonymousUserSession, db.getDbClient(), issueResolutionHistoryService, clock),
      get("/history/issue-resolution-history")
        .queryParam("entityId", privatePortfolio.uuid())
        .queryParam("entityType", "PORTFOLIO")
        .queryParam("statistic", "MTTR")
        .queryParam("startDate", START_DATE.toString()));
    assertForbidden(
      new DefaultMeasuresHistoryController(anonymousUserSession, db.getDbClient(), measuresHistoryService, clock),
      get("/history/measures-history")
        .queryParam("entityType", "PORTFOLIO")
        .queryParam("entityId", privatePortfolio.uuid())
        .queryParam("metricKeys", METRIC_KEY)
        .queryParam("startDate", START_DATE.toString()));

    assertForbidden(
      new DefaultProjectIssueCountsController(db.getDbClient(), contextLoader, mock(), clock),
      get("/history/project-issue-counts")
        .queryParam("portfolioId", privatePortfolio.uuid()));
    assertForbidden(
      new DefaultProjectMeasuresController(db.getDbClient(), contextLoader, mock(), clock),
      get("/history/project-measures")
        .queryParam("metricKey", METRIC_KEY)
        .queryParam("portfolioId", privatePortfolio.uuid()));
    assertForbidden(
      new DefaultProjectIssueResolutionController(db.getDbClient(), contextLoader, mock(), clock),
      get("/history/project-issue-resolution")
        .queryParam("statistic", "MTTR")
        .queryParam("portfolioId", privatePortfolio.uuid()));
  }

  @Test
  void historyControllers_doNotRequireAuthenticationBeforeCheckingEntityAccess() {
    assertThat(DefaultIssueCountHistoryController.class.isAnnotationPresent(RequireAuthentication.class)).isFalse();
    assertThat(DefaultIssueDensityHistoryController.class.isAnnotationPresent(RequireAuthentication.class)).isFalse();
    assertThat(DefaultIssueResolutionHistoryController.class.isAnnotationPresent(RequireAuthentication.class)).isFalse();
    assertThat(DefaultMeasuresHistoryController.class.isAnnotationPresent(RequireAuthentication.class)).isFalse();
    assertThat(DefaultProjectIssueCountsController.class.isAnnotationPresent(RequireAuthentication.class)).isFalse();
    assertThat(DefaultProjectIssueResolutionController.class.isAnnotationPresent(RequireAuthentication.class)).isFalse();
    assertThat(DefaultProjectMeasuresController.class.isAnnotationPresent(RequireAuthentication.class)).isFalse();
  }

  private static void assertForbidden(Object controller, MockHttpServletRequestBuilder request) throws Exception {
    ControllerTester.getMockMvc(controller).perform(request)
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));
  }
}
