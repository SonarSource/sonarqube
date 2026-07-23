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
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarsource.history.model.EntityType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class HistoryControllerUtilsTest {

  private static final String PROJECT_BRANCH_ID = "branch-1";
  private static final String PROJECT_UUID = "project-1";
  private static final Instant NOW = Instant.parse("2026-07-08T01:00:00Z");

  private final UserSession userSession = mock();
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final BranchDao branchDao = mock();
  private final ProjectDao projectDao = mock();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.projectDao()).thenReturn(projectDao);
  }

  @Test
  public void checkPermission_whenEntityIsNotProjectBranch_shouldNotCheckPermissions() {
    HistoryControllerUtils.checkPermission(userSession, dbClient, "portfolio-1", EntityType.PORTFOLIO);

    verifyNoInteractions(branchDao, projectDao, userSession);
    verify(dbSession).close();
  }

  @Test
  public void checkPermission_whenProjectBranchBelongsToApplication_shouldCheckEntityAndChildPermissions() {
    ProjectDto application = project(PROJECT_UUID, ComponentQualifiers.APP);
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(application));

    HistoryControllerUtils.checkPermission(userSession, dbClient, PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH);

    verify(userSession).checkEntityPermission(ProjectPermission.USER, application);
    verify(userSession).checkChildProjectsPermission(ProjectPermission.USER, application);
    verify(dbSession).close();
  }

  @Test
  public void checkPermission_whenProjectBranchIsMissing_shouldReturnNotFound() {
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> HistoryControllerUtils.checkPermission(userSession, dbClient, PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project branch with uuid 'branch-1' not found");

    verify(dbSession).close();
  }

  @Test
  public void checkPermission_whenProjectIsMissing_shouldReturnNotFound() {
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> HistoryControllerUtils.checkPermission(userSession, dbClient, PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project with uuid 'project-1' not found");

    verify(dbSession).close();
  }

  @Test
  public void normalize_whenEndDateIsNull_shouldUseCurrentInstant() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T00:00:00Z");

    HistoryControllerUtils.HistoryDateRange result = HistoryControllerUtils.normalize(clock, startDate, null);

    assertThat(result.start()).isEqualTo(startDate.toInstant());
    assertThat(result.end()).isEqualTo(NOW);
  }

  @Test
  public void normalize_whenEndDateIsInTheFuture_shouldClampToCurrentInstant() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-08T00:00:00Z");
    OffsetDateTime endDate = OffsetDateTime.parse("2026-07-09T00:00:00Z");

    HistoryControllerUtils.HistoryDateRange result = HistoryControllerUtils.normalize(clock, startDate, endDate);

    assertThat(result.start()).isEqualTo(startDate.toInstant());
    assertThat(result.end()).isEqualTo(NOW);
  }

  @Test
  public void normalize_whenStartDateIsInTheFuture_shouldReject() {
    OffsetDateTime startDate = OffsetDateTime.parse("2026-07-07T23:30:00-02:00");

    assertThatThrownBy(() -> HistoryControllerUtils.normalize(clock, startDate, null))
      .hasMessageContaining("must be less than or equal to the current date");
  }

  private static ProjectDto project(String uuid, String qualifier) {
    return new ProjectDto()
      .setUuid(uuid)
      .setQualifier(qualifier);
  }
}
