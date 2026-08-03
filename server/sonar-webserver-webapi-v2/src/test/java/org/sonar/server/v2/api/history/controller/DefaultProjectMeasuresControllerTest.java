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
import org.sonar.db.metric.MetricDao;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.v2.common.RestResponseEntityExceptionHandler;
import org.sonarsource.history.model.Pagination;
import org.sonarsource.history.model.ProjectBranch;
import org.sonarsource.history.model.ProjectMeasuresResponse;
import org.sonarsource.history.model.ProjectMeasuresResponse.ProjectMeasure;
import org.sonarsource.history.model.ProjectMeasuresResponse.ProjectMeasureMetric;
import org.sonarsource.history.server.service.ProjectMeasuresService;
import org.mockito.InOrder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DefaultProjectMeasuresControllerTest {

  private static final String PORTFOLIO_ID = "portfolio-uuid";
  private static final String APPLICATION_BRANCH_ID = "application-branch-uuid";
  private static final String BRANCH_ID = "branch-1";
  private static final String METRIC_KEY = "coverage";
  private static final Instant NOW = Instant.parse("2026-07-08T01:00:00Z");
  private static final List<String> SORT = List.of("-measure.currentValue");
  private static final ProjectCollectionContext CONTEXT = new ProjectCollectionContext(
    List.of(new ProjectBranch(BRANCH_ID, "main", "alpha", "Alpha")), Set.of(BRANCH_ID));

  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final MetricDao metricDao = mock();
  private final ProjectCollectionContextLoader contextLoader = mock();
  private final ProjectMeasuresService projectMeasuresService = mock();
  private final DefaultProjectMeasuresController underTest = new DefaultProjectMeasuresController(
    dbClient, contextLoader, projectMeasuresService, Clock.fixed(NOW, ZoneOffset.UTC));

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.metricDao()).thenReturn(metricDao);
  }

  @Test
  public void getProjectMeasuresDelegatesAndConvertsResponse() {
    OffsetDateTime referenceDate = OffsetDateTime.parse("2026-07-01T00:00:00Z");
    when(metricDao.selectByKey(dbSession, METRIC_KEY)).thenReturn(new MetricDto().setKey(METRIC_KEY).setValueType("PERCENT"));
    when(contextLoader.load(dbSession, PORTFOLIO_ID)).thenReturn(CONTEXT);
    var measure = new ProjectMeasureMetric("80.0", METRIC_KEY, "75.0", "PERCENT");
    var serviceResponse = new ProjectMeasuresResponse(
      1,
      new Pagination(1, 50, 1),
      List.of(new ProjectMeasure(BRANCH_ID, "main", measure, "alpha", "Alpha")));
    when(projectMeasuresService.queryProjectMeasures(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), METRIC_KEY, "PERCENT", null, "alp",
      1, 50, referenceDate.toInstant(), SORT, false))
        .thenReturn(serviceResponse);

    var response = underTest.getProjectMeasures(
      METRIC_KEY, null, "alp", 1, 50, PORTFOLIO_ID, null, null, referenceDate,
      SORT, false);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    assertThat(response.getBody().getHiddenProjectCount()).isOne();
    assertThat(response.getBody().getProjectMeasures()).singleElement()
      .satisfies(item -> assertThat(item.getMeasure().getCurrentValue()).isEqualTo("80.0"));
    verify(contextLoader).load(dbSession, PORTFOLIO_ID);
    verify(projectMeasuresService).queryProjectMeasures(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), METRIC_KEY, "PERCENT", null, "alp",
      1, 50, referenceDate.toInstant(), SORT, false);
    InOrder sessionClosedBeforeServiceCall = inOrder(dbSession, projectMeasuresService);
    sessionClosedBeforeServiceCall.verify(dbSession).close();
    sessionClosedBeforeServiceCall.verify(projectMeasuresService).queryProjectMeasures(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), METRIC_KEY, "PERCENT", null, "alp",
      1, 50, referenceDate.toInstant(), SORT, false);
  }

  @Test
  public void getProjectMeasuresSupportsMissingReferenceDate() {
    when(metricDao.selectByKey(dbSession, METRIC_KEY)).thenReturn(new MetricDto().setKey(METRIC_KEY).setValueType("PERCENT"));
    when(contextLoader.load(dbSession, "APPLICATION", APPLICATION_BRANCH_ID)).thenReturn(CONTEXT);
    var serviceResponse = new ProjectMeasuresResponse(0, new Pagination(1, 50, 0), List.of());
    when(projectMeasuresService.queryProjectMeasures(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), METRIC_KEY, "PERCENT", null, null,
      1, 50, null, SORT, false))
        .thenReturn(serviceResponse);

    var response = underTest.getProjectMeasures(
      METRIC_KEY, null, null, 1, 50, null, "APPLICATION", APPLICATION_BRANCH_ID, null, SORT, false);

    assertThat(response.getStatusCode()).isEqualTo(OK);
    assertThat(response.getBody().getProjectMeasures()).isEmpty();
    verify(contextLoader).load(dbSession, "APPLICATION", APPLICATION_BRANCH_ID);
    verify(projectMeasuresService).queryProjectMeasures(
      CONTEXT.branches(), CONTEXT.visibleBranchIds(), METRIC_KEY, "PERCENT", null, null,
      1, 50, null, SORT, false);
  }

  @Test
  public void getProjectMeasuresRejectsUnknownMetric() {
    when(metricDao.selectByKey(dbSession, METRIC_KEY)).thenReturn(null);

    assertThatThrownBy(() -> underTest.getProjectMeasures(
      METRIC_KEY, null, null, 1, 50, PORTFOLIO_ID, null, null, null, SORT, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Metric with key %s not found", METRIC_KEY);

    verifyNoInteractions(contextLoader, projectMeasuresService);
    verify(dbSession).close();
  }

  @Test
  public void getProjectMeasuresRejectsReferenceDateOnCurrentDayBeforeLoadingContext() {
    OffsetDateTime referenceDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");

    assertThatThrownBy(() -> underTest.getProjectMeasures(
      METRIC_KEY, null, null, 1, 50, PORTFOLIO_ID, null, null, referenceDate,
      SORT, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be before the current date");

    verifyNoInteractions(contextLoader, projectMeasuresService);
    verify(dbClient, never()).openSession(false);
  }

  @Test
  public void getProjectMeasuresRejectsReferenceDateOlderThanOneYearBeforeLoadingContext() {
    OffsetDateTime referenceDate = OffsetDateTime.parse("2025-07-07T23:59:59Z");

    assertThatThrownBy(() -> underTest.getProjectMeasures(
      METRIC_KEY, null, null, 1, 50, PORTFOLIO_ID, null, null, referenceDate,
      SORT, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be more than 1 year in the past");

    verifyNoInteractions(contextLoader, projectMeasuresService);
    verify(dbClient, never()).openSession(false);
  }

  @Test
  public void getProjectMeasuresRejectsInvalidSelectorBeforeOpeningSession() {
    assertThatThrownBy(() -> underTest.getProjectMeasures(
      METRIC_KEY, null, null, 1, 50, null, null, null, null,
      SORT, false))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Either portfolioId or both entityType and entityId must be provided");

    verify(dbClient, never()).openSession(false);
    verifyNoInteractions(contextLoader, projectMeasuresService);
  }

  @Test
  public void getProjectMeasuresReturnsBadRequestForMixedLegacyAndTypedSelectors() throws Exception {
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(underTest)
      .setControllerAdvice(new RestResponseEntityExceptionHandler())
      .build();

    mockMvc.perform(get("/history/project-measures")
      .queryParam("metricKey", METRIC_KEY)
      .queryParam("portfolioId", PORTFOLIO_ID)
      .queryParam("entityType", "PORTFOLIO")
      .queryParam("entityId", PORTFOLIO_ID))
      .andExpect(status().isBadRequest());
  }
}
