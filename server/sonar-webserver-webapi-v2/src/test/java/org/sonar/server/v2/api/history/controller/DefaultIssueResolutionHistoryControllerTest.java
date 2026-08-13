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
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.server.user.UserSession;
import org.sonarsource.history.api.model.HistoryEntityType;
import org.sonarsource.history.api.model.IssueResolutionHistoryResponse;
import org.sonarsource.history.api.model.IssueResolutionSliceBy;
import org.sonarsource.history.api.model.IssueResolutionStatistic;
import org.sonarsource.history.api.model.IssueSeverity;
import org.sonarsource.history.api.model.IssueType;
import org.sonarsource.history.model.EntityType;
import org.sonarsource.history.model.IssueCountHistoryFilters;
import org.sonarsource.history.model.IssueResolutionHistoryPoint;
import org.sonarsource.history.server.service.IssueTtrHistoryService;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.OK;

public class DefaultIssueResolutionHistoryControllerTest {

  private static final String ENTITY_ID = "123e4567-e89b-12d3-a456-426614174000";
  private static final Instant NOW = Instant.parse("2026-07-08T01:00:00Z");
  private static final OffsetDateTime START = OffsetDateTime.parse("2020-07-07T12:00:00Z");
  private static final OffsetDateTime END = OffsetDateTime.parse("2020-07-08T12:00:00Z");

  private final IssueTtrHistoryService issueTtrHistoryService = mock();
  private final UserSession userSession = mock();
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final ComponentDao componentDao = mock();
  private final DefaultIssueResolutionHistoryController underTest = new DefaultIssueResolutionHistoryController(
    userSession, dbClient, issueTtrHistoryService, Clock.fixed(NOW, ZoneOffset.UTC));

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.componentDao()).thenReturn(componentDao);
  }

  @Test
  public void getIssueResolutionHistoryUsesAuthorizationAndPreservesDatesOlderThanOneYear() {
    ComponentDto portfolio = new ComponentDto()
      .setUuid(ENTITY_ID)
      .setBranchUuid(ENTITY_ID)
      .setQualifier(ComponentQualifiers.VIEW);
    when(componentDao.selectByUuid(dbSession, ENTITY_ID)).thenReturn(Optional.of(portfolio));
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
    verify(userSession).checkComponentPermission(ProjectPermission.USER, portfolio);
  }

  @Test
  public void getIssueResolutionHistoryRejectsFutureStartBeforeQuerying() {
    OffsetDateTime future = OffsetDateTime.parse("2026-07-08T02:00:00Z");

    assertThatThrownBy(() -> underTest.getIssueResolutionHistory(
      ENTITY_ID,
      HistoryEntityType.PROJECT_BRANCH,
      IssueResolutionStatistic.RESOLVED_ISSUES,
      future,
      null,
      null,
      null,
      null,
      null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("must not be in the future.");

    verifyNoInteractions(issueTtrHistoryService);
  }
}
