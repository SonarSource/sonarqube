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
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.v2.api.ControllerTester;
import org.sonarsource.history.api.mapper.HistoryModelConverter;
import org.sonarsource.history.api.model.ProjectCollectionHistoryEntityType;
import org.sonarsource.history.api.model.IssueCountStatus;
import org.sonarsource.history.api.model.IssueSeverity;
import org.sonarsource.history.api.model.IssueType;
import org.sonarsource.history.model.Pagination;
import org.sonarsource.history.model.ProjectBranch;
import org.sonarsource.history.model.ProjectIssueCount;
import org.sonarsource.history.model.ProjectIssueCountsResponse;
import org.sonarsource.history.server.service.IssueCountHistoryService;
import org.sonarsource.history.server.service.ProjectIssueCountsService;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultProjectIssueCountsControllerTest {

  private static final String PORTFOLIO_ID = "portfolio-uuid";
  private static final String APPLICATION_BRANCH_ID = "application-branch-uuid";
  private static final String BRANCH_ID = "branch-1";
  private static final Instant NOW = Instant.parse("2026-07-08T01:00:00Z");
  private static final OffsetDateTime VALID_REFERENCE_DATE = OffsetDateTime.parse("2026-07-07T00:00:00Z");
  private static final List<String> SORT = List.of("-issueCount");
  private static final ProjectCollectionContext CONTEXT = new ProjectCollectionContext(
    List.of(new ProjectBranch(BRANCH_ID, "main", "alpha", "Alpha")), Set.of(BRANCH_ID));

  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final ProjectCollectionContextLoader contextLoader = mock();
  private final ProjectIssueCountsService projectIssueCountsService = mock();
  private final DefaultProjectIssueCountsController underTest = new DefaultProjectIssueCountsController(
    dbClient, contextLoader, projectIssueCountsService, Clock.fixed(NOW, ZoneOffset.UTC));
  private final MockMvc mockMvc = ControllerTester.getMockMvc(underTest);

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
  }

  @Test
  public void getProjectIssueCountsDelegatesAndConvertsResponse() {
    List<String> ruleKeys = List.of("java:S100");
    List<IssueSeverity> severities = List.of(IssueSeverity.HIGH);
    List<IssueType> issueTypes = List.of(IssueType.BUG);
    List<IssueCountStatus> statuses = List.of(IssueCountStatus.OPEN);
    List<String> impacts = List.of("SECURITY:HIGH");
    OffsetDateTime referenceDate = OffsetDateTime.parse("2026-07-01T00:00:00Z");
    var filters = IssueCountHistoryService.buildFilters(
      ruleKeys,
      HistoryModelConverter.toCoreSeverities(severities),
      HistoryModelConverter.toCoreIssueTypes(issueTypes),
      HistoryModelConverter.toCoreStatuses(statuses),
      impacts);
    var serviceResponse = new ProjectIssueCountsResponse(
      1,
      List.of(new ProjectIssueCount(BRANCH_ID, "Alpha", "alpha", "main", 7, 3L)),
      new Pagination(1, 50, 1));
    when(contextLoader.load(dbSession, PORTFOLIO_ID)).thenReturn(CONTEXT);
    when(projectIssueCountsService.getProjectIssueCounts(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), filters, "alp", referenceDate.toInstant(),
      1, 50, SORT, true))
        .thenReturn(serviceResponse);

    var response = underTest.getProjectIssueCounts(
      PORTFOLIO_ID, null, null, ruleKeys, severities, issueTypes, statuses, impacts, "alp", referenceDate,
      1, 50, SORT, true);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    assertThat(response.getBody().getHiddenProjectCount()).isOne();
    assertThat(response.getBody().getProjectIssueCounts()).singleElement()
      .satisfies(item -> {
        assertThat(item.getIssueCount()).isEqualTo(7L);
        assertThat(item.getReferenceIssueCount()).isEqualTo(3L);
      });
    verify(contextLoader).load(dbSession, PORTFOLIO_ID);
    verify(projectIssueCountsService).getProjectIssueCounts(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), filters, "alp", referenceDate.toInstant(),
      1, 50, SORT, true);
    InOrder sessionClosedBeforeServiceCall = inOrder(dbSession, projectIssueCountsService);
    sessionClosedBeforeServiceCall.verify(dbSession).close();
    sessionClosedBeforeServiceCall.verify(projectIssueCountsService).getProjectIssueCounts(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), filters, "alp", referenceDate.toInstant(),
      1, 50, SORT, true);
  }

  @Test
  public void getProjectIssueCountsSupportsTypedSelector() {
    var filters = IssueCountHistoryService.buildFilters(null, null, null, null, null);
    var serviceResponse = new ProjectIssueCountsResponse(0, List.of(), new Pagination(1, 50, 0));
    when(contextLoader.load(dbSession, ProjectCollectionHistoryEntityType.APPLICATION, APPLICATION_BRANCH_ID)).thenReturn(CONTEXT);
    when(projectIssueCountsService.getProjectIssueCounts(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), filters, null, VALID_REFERENCE_DATE.toInstant(),
      1, 50, SORT, false))
        .thenReturn(serviceResponse);

    var response = underTest.getProjectIssueCounts(
      null, ProjectCollectionHistoryEntityType.APPLICATION, APPLICATION_BRANCH_ID, null, null, null, null, null, null, VALID_REFERENCE_DATE,
      1, 50, SORT, false);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    assertThat(response.getBody().getProjectIssueCounts()).isEmpty();
    verify(contextLoader).load(dbSession, ProjectCollectionHistoryEntityType.APPLICATION, APPLICATION_BRANCH_ID);
  }

  @Test
  public void getProjectIssueCountsReturnsForbiddenWhenCollectionAccessIsDenied() throws Exception {
    when(contextLoader.load(dbSession, PORTFOLIO_ID))
      .thenThrow(new ForbiddenException("Insufficient privileges"));

    mockMvc.perform(get("/history/project-issue-counts")
        .queryParam("portfolioId", PORTFOLIO_ID))
      .andExpectAll(
        status().isForbidden(),
        content().json("{\"message\":\"Insufficient privileges\"}"));

    verifyNoInteractions(projectIssueCountsService);
    verify(dbSession).close();
  }

  @Test
  public void getProjectIssueCountsReturnsNotFoundWhenCollectionIsMissing() throws Exception {
    when(contextLoader.load(dbSession, PORTFOLIO_ID))
      .thenThrow(new NotFoundException("Portfolio or application branch 'portfolio-uuid' not found"));

    mockMvc.perform(get("/history/project-issue-counts")
        .queryParam("portfolioId", PORTFOLIO_ID))
      .andExpectAll(
        status().isNotFound(),
        content().json("{\"message\":\"Portfolio or application branch 'portfolio-uuid' not found\"}"));

    verifyNoInteractions(projectIssueCountsService);
    verify(dbSession).close();
  }

  @Test
  public void getProjectIssueCountsReturnsBadRequestWhenServiceRejectsRequest() throws Exception {
    when(contextLoader.load(dbSession, PORTFOLIO_ID)).thenReturn(CONTEXT);
    doThrow(new IllegalArgumentException("Unsupported issue-count request"))
      .when(projectIssueCountsService)
      .getProjectIssueCounts(any(), any(), any(), any(), any(), anyInt(), anyInt(), any(), anyBoolean());

    mockMvc.perform(get("/history/project-issue-counts")
        .queryParam("portfolioId", PORTFOLIO_ID))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Unsupported issue-count request\"}"));
  }

  @Test
  public void getProjectIssueCountsAcceptsNullReferenceDateAndForwardsNull() {
    var filters = IssueCountHistoryService.buildFilters(null, null, null, null, null);
    var serviceResponse = new ProjectIssueCountsResponse(0, List.of(), new Pagination(1, 50, 0));
    when(contextLoader.load(dbSession, PORTFOLIO_ID)).thenReturn(CONTEXT);
    when(projectIssueCountsService.getProjectIssueCounts(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), filters, null, null,
      1, 50, SORT, false))
      .thenReturn(serviceResponse);

    var response = underTest.getProjectIssueCounts(
      PORTFOLIO_ID, null, null, null, null, null, null, null, null, null,
      1, 50, SORT, false);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    verify(projectIssueCountsService).getProjectIssueCounts(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), filters, null, null,
      1, 50, SORT, false);
  }

  @Test
  public void getProjectIssueCountsRejectsFutureReferenceDateBeforeLoadingContext() throws Exception {
    OffsetDateTime referenceDate = OffsetDateTime.parse("2026-07-09T00:00:00Z");

    mockMvc.perform(get("/history/project-issue-counts")
        .queryParam("portfolioId", PORTFOLIO_ID)
        .queryParam("referenceDate", referenceDate.toString()))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"referenceDate 2026-07-09T00:00:00Z must be before the current date\"}"));

    verifyNoInteractions(contextLoader, projectIssueCountsService);
    verify(dbClient, never()).openSession(false);
  }

  @Test
  public void getProjectIssueCountsRejectsCurrentMidnightReferenceDateBeforeLoadingContext() throws Exception {
    OffsetDateTime referenceDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");

    mockMvc.perform(get("/history/project-issue-counts")
        .queryParam("portfolioId", PORTFOLIO_ID)
        .queryParam("referenceDate", referenceDate.toString()))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"referenceDate 2026-07-08T00:00:00Z must be before the current date\"}"));

    verifyNoInteractions(contextLoader, projectIssueCountsService);
    verify(dbClient, never()).openSession(false);
  }

  @Test
  public void getProjectIssueCountsAcceptsReferenceDateOlderThanOneYear() {
    OffsetDateTime referenceDate = OffsetDateTime.parse("2020-07-07T23:59:59Z");
    var filters = IssueCountHistoryService.buildFilters(null, null, null, null, null);
    var serviceResponse = new ProjectIssueCountsResponse(0, List.of(), new Pagination(1, 50, 0));
    when(contextLoader.load(dbSession, PORTFOLIO_ID)).thenReturn(CONTEXT);
    when(projectIssueCountsService.getProjectIssueCounts(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), filters, null, referenceDate.toInstant(),
      1, 50, SORT, false))
      .thenReturn(serviceResponse);

    var response = underTest.getProjectIssueCounts(
      PORTFOLIO_ID, null, null, null, null, null, null, null, null, referenceDate,
      1, 50, SORT, false);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    verify(projectIssueCountsService).getProjectIssueCounts(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), filters, null, referenceDate.toInstant(),
      1, 50, SORT, false);
  }

  @Test
  public void getProjectIssueCountsRejectsInvalidSelectorBeforeOpeningSession() throws Exception {
    mockMvc.perform(get("/history/project-issue-counts")
        .queryParam("referenceDate", VALID_REFERENCE_DATE.toString()))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"Either portfolioId or both entityType and entityId must be provided\"}"));

    verify(dbClient, never()).openSession(false);
    verifyNoInteractions(contextLoader, projectIssueCountsService);
  }

  @Test
  public void getProjectIssueCountsReturnsBadRequestForMixedLegacyAndTypedSelectors() throws Exception {
    mockMvc.perform(get("/history/project-issue-counts")
      .queryParam("portfolioId", PORTFOLIO_ID)
      .queryParam("entityType", "PORTFOLIO")
      .queryParam("entityId", PORTFOLIO_ID)
      .queryParam("referenceDate", "2026-07-07T00:00:00Z"))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"portfolioId cannot be combined with entityType or entityId\"}"));
  }

  @Test
  public void getProjectIssueCountsRejectsPageSizeAboveMaximum() throws Exception {
    when(contextLoader.load(dbSession, PORTFOLIO_ID)).thenReturn(CONTEXT);
    when(projectIssueCountsService.getProjectIssueCounts(
      eq(CONTEXT.branches()), eq(CONTEXT.visibleBranchIds()), any(), isNull(), isNull(),
      eq(1), eq(5001), eq(SORT), eq(false)))
      .thenReturn(new ProjectIssueCountsResponse(0, List.of(), new Pagination(1, 5001, 0)));

    mockMvc.perform(get("/history/project-issue-counts")
        .queryParam("portfolioId", PORTFOLIO_ID)
        .queryParam("pageSize", "5001"))
      .andExpectAll(
        status().isBadRequest(),
        content().json("{\"message\":\"pageSize: must be less than or equal to 5000\"}"));

    verify(dbClient, never()).openSession(false);
    verifyNoInteractions(contextLoader, projectIssueCountsService);
  }

  @Test
  public void getProjectIssueCounts_whenSecurityHotspotIssueTypeIsRequested_shouldReturnBadRequest() throws Exception {
    mockMvc.perform(get("/history/project-issue-counts")
      .queryParam("portfolioId", PORTFOLIO_ID)
      .queryParam("issueTypes", "SECURITY_HOTSPOT"))
      .andExpect(status().isBadRequest());
    verifyNoInteractions(dbClient, contextLoader, projectIssueCountsService);
  }
}
