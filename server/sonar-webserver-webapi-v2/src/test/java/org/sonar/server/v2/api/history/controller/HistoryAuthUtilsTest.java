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
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarsource.history.model.EntityType;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class HistoryAuthUtilsTest {

  private static final String PROJECT_BRANCH_ID = "branch-1";
  private static final String PROJECT_UUID = "project-1";
  private final UserSession userSession = mock();
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final BranchDao branchDao = mock();
  private final PortfolioDao portfolioDao = mock();
  private final ProjectDao projectDao = mock();

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.portfolioDao()).thenReturn(portfolioDao);
    when(dbClient.projectDao()).thenReturn(projectDao);
  }

  @Test
  public void assertUserHasPermission_whenEntityIsPortfolio_shouldAssertUserHasEntityPermission() {
    PortfolioDto portfolio = new PortfolioDto()
      .setUuid("portfolio-1")
      .setRootUuid("portfolio-1");
    when(portfolioDao.selectByUuid(dbSession, "portfolio-1")).thenReturn(Optional.of(portfolio));

    HistoryAuthUtils.assertUserHasPermission(userSession, dbClient, "portfolio-1", EntityType.PORTFOLIO);

    verify(portfolioDao).selectByUuid(dbSession, "portfolio-1");
    verify(userSession).checkEntityPermission(ProjectPermission.USER, portfolio);
    verify(dbSession).close();
  }

  @Test
  public void assertUserHasPermission_whenEntityIsSubPortfolio_shouldAssertUserHasEntityPermission() {
    PortfolioDto subPortfolio = new PortfolioDto()
      .setUuid("portfolio-1")
      .setRootUuid("root-portfolio")
      .setParentUuid("root-portfolio");
    when(portfolioDao.selectByUuid(dbSession, "portfolio-1")).thenReturn(Optional.of(subPortfolio));

    HistoryAuthUtils.assertUserHasPermission(userSession, dbClient, "portfolio-1", EntityType.PORTFOLIO);

    verify(userSession).checkEntityPermission(ProjectPermission.USER, subPortfolio);
    verify(dbSession).close();
  }

  @Test
  public void assertUserHasPermission_whenPortfolioIsMissing_shouldReturnNotFound() {
    when(portfolioDao.selectByUuid(dbSession, "portfolio-1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> HistoryAuthUtils.assertUserHasPermission(userSession, dbClient, "portfolio-1", EntityType.PORTFOLIO))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Portfolio with uuid 'portfolio-1' not found");

    verifyNoInteractions(branchDao, projectDao, userSession);
    verify(dbSession).close();
  }

  @Test
  public void assertUserHasPermission_whenPortfolioIsUnauthorized_shouldReturnForbidden() {
    PortfolioDto portfolio = new PortfolioDto()
      .setUuid("portfolio-1")
      .setRootUuid("portfolio-1");
    when(portfolioDao.selectByUuid(dbSession, "portfolio-1")).thenReturn(Optional.of(portfolio));
    doThrow(new ForbiddenException("Access forbidden"))
      .when(userSession).checkEntityPermission(ProjectPermission.USER, portfolio);

    assertThatThrownBy(() -> HistoryAuthUtils.assertUserHasPermission(userSession, dbClient, "portfolio-1", EntityType.PORTFOLIO))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void assertUserHasPermission_whenProjectBranchBelongsToApplication_shouldAssertUserHasEntityAndChildPermissions() {
    ProjectDto application = project(PROJECT_UUID, ComponentQualifiers.APP);
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(application));

    HistoryAuthUtils.assertUserHasPermission(userSession, dbClient, PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH);

    verify(userSession).checkEntityPermission(ProjectPermission.USER, application);
    verify(userSession).checkChildProjectsPermission(ProjectPermission.USER, application);
    verify(dbSession).close();
  }

  @Test
  public void assertUserHasPermission_whenApplicationBranchIsAuthorized_shouldAssertApplicationAndChildPermissions() {
    ProjectDto application = project(PROJECT_UUID, ComponentQualifiers.APP);
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(application));

    HistoryAuthUtils.assertUserHasPermission(userSession, dbClient, PROJECT_BRANCH_ID, EntityType.APPLICATION);

    verify(userSession).checkEntityPermission(ProjectPermission.USER, application);
    verify(userSession).checkChildProjectsPermission(ProjectPermission.USER, application);
    verify(dbSession).close();
  }

  @Test
  public void assertUserHasPermission_whenProjectBranchBelongsToProject_shouldOnlyAssertEntityPermission() {
    ProjectDto project = project(PROJECT_UUID, ComponentQualifiers.PROJECT);
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(project));

    HistoryAuthUtils.assertUserHasPermission(userSession, dbClient, PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH);

    verify(userSession).checkEntityPermission(ProjectPermission.USER, project);
    verify(userSession, never()).checkChildProjectsPermission(ProjectPermission.USER, project);
    verify(dbSession).close();
  }

  @Test
  public void assertUserHasPermission_whenProjectBranchIsUnauthorized_shouldReturnForbidden() {
    ProjectDto project = project(PROJECT_UUID, ComponentQualifiers.PROJECT);
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(project));
    doThrow(new ForbiddenException("Insufficient privileges"))
      .when(userSession).checkEntityPermission(ProjectPermission.USER, project);

    assertThatThrownBy(() -> HistoryAuthUtils.assertUserHasPermission(
      userSession, dbClient, PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");

    verify(userSession, never()).checkChildProjectsPermission(ProjectPermission.USER, project);
  }

  @Test
  public void assertUserHasPermission_whenApplicationChildProjectsAreUnauthorized_shouldReturnForbidden() {
    ProjectDto application = project(PROJECT_UUID, ComponentQualifiers.APP);
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(application));
    doThrow(new ForbiddenException("Insufficient privileges"))
      .when(userSession).checkChildProjectsPermission(ProjectPermission.USER, application);

    assertThatThrownBy(() -> HistoryAuthUtils.assertUserHasPermission(
      userSession, dbClient, PROJECT_BRANCH_ID, EntityType.APPLICATION))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void assertUserHasPermission_whenApplicationIsUnauthorized_shouldReturnForbiddenWithoutCheckingChildProjects() {
    ProjectDto application = project(PROJECT_UUID, ComponentQualifiers.APP);
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(application));
    doThrow(new ForbiddenException("Insufficient privileges"))
      .when(userSession).checkEntityPermission(ProjectPermission.USER, application);

    assertThatThrownBy(() -> HistoryAuthUtils.assertUserHasPermission(
      userSession, dbClient, PROJECT_BRANCH_ID, EntityType.APPLICATION))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");

    verify(userSession, never()).checkChildProjectsPermission(ProjectPermission.USER, application);
    verify(dbSession).close();
  }

  @Test
  public void assertUserHasPermission_whenApplicationBranchDoesNotBelongToApplication_shouldReturnNotFound() {
    ProjectDto project = project(PROJECT_UUID, ComponentQualifiers.PROJECT);
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(project));

    assertThatThrownBy(() -> HistoryAuthUtils.assertUserHasPermission(
      userSession, dbClient, PROJECT_BRANCH_ID, EntityType.APPLICATION))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Portfolio or application branch 'branch-1' not found");

    verifyNoInteractions(userSession);
    verify(dbSession).close();
  }

  @Test
  public void assertUserHasPermission_whenApplicationBranchIsMissing_shouldReturnNotFoundWithoutLoadingProject() {
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> HistoryAuthUtils.assertUserHasPermission(
      userSession, dbClient, PROJECT_BRANCH_ID, EntityType.APPLICATION))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Portfolio or application branch 'branch-1' not found");

    verifyNoInteractions(projectDao, userSession);
    verify(dbSession).close();
  }

  @Test
  public void assertUserHasPermission_whenApplicationIsMissing_shouldReturnNotFoundWithoutCheckingUserPermission() {
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> HistoryAuthUtils.assertUserHasPermission(
      userSession, dbClient, PROJECT_BRANCH_ID, EntityType.APPLICATION))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Portfolio or application branch 'branch-1' not found");

    verifyNoInteractions(userSession);
    verify(dbSession).close();
  }

  @Test
  public void assertUserHasPermission_whenProjectBranchIsMissing_shouldReturnNotFound() {
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> HistoryAuthUtils.assertUserHasPermission(userSession, dbClient, PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project branch with uuid 'branch-1' not found");

    verify(dbSession).close();
  }

  @Test
  public void assertUserHasPermission_whenProjectIsMissing_shouldReturnNotFound() {
    when(branchDao.selectByUuid(dbSession, PROJECT_BRANCH_ID))
      .thenReturn(Optional.of(new BranchDto().setUuid(PROJECT_BRANCH_ID).setProjectUuid(PROJECT_UUID)));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> HistoryAuthUtils.assertUserHasPermission(userSession, dbClient, PROJECT_BRANCH_ID, EntityType.PROJECT_BRANCH))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project with uuid 'project-1' not found");

    verify(dbSession).close();
  }

  @Test
  public void assertUserHasPermission_whenEntityTypeIsNull_shouldOnlyCloseDatabaseSession() {
    HistoryAuthUtils.assertUserHasPermission(userSession, dbClient, "entity-1", null);

    verifyNoInteractions(branchDao, portfolioDao, projectDao, userSession);
    verify(dbSession).close();
  }

  private static ProjectDto project(String uuid, String qualifier) {
    return new ProjectDto()
      .setUuid(uuid)
      .setQualifier(qualifier);
  }
}
