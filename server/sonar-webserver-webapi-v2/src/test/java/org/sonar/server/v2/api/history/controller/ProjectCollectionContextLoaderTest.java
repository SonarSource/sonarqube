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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ApplicationProjectsDao;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTreeQuery;
import org.sonar.db.portfolio.PortfolioDao;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarsource.history.model.ProjectBranch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.LEAVES;
import static org.sonar.db.permission.ProjectPermission.USER;

public class ProjectCollectionContextLoaderTest {

  private static final String PORTFOLIO_ID = "portfolio-uuid";
  private static final String APPLICATION_ID = "application-uuid";
  private static final String APPLICATION_BRANCH_ID = "application-branch-uuid";
  private static final String BRANCH_ID = "branch-uuid";
  private static final String PROJECT_ID = "project-uuid";

  private final UserSession userSession = mock();
  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final PortfolioDao portfolioDao = mock();
  private final ApplicationProjectsDao applicationProjectsDao = mock();
  private final ComponentDao componentDao = mock();
  private final BranchDao branchDao = mock();
  private final ProjectDao projectDao = mock();
  private final ProjectCollectionContextLoader underTest = new ProjectCollectionContextLoader(userSession, dbClient);

  @Before
  public void setUp() {
    when(dbClient.portfolioDao()).thenReturn(portfolioDao);
    when(dbClient.applicationProjectsDao()).thenReturn(applicationProjectsDao);
    when(dbClient.componentDao()).thenReturn(componentDao);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.projectDao()).thenReturn(projectDao);
  }

  @Test
  public void loadPortfolioReturnsAllDescendantProjectsAndIgnoresComponentsWithoutCopiedBranches() {
    PortfolioDto portfolio = new PortfolioDto().setUuid(PORTFOLIO_ID);
    BranchDto branch = branch(BRANCH_ID, PROJECT_ID, "main");
    ProjectDto project = project(PROJECT_ID, "project-key", "Project");
    when(portfolioDao.selectByUuid(dbSession, PORTFOLIO_ID)).thenReturn(Optional.of(portfolio));
    when(componentDao.selectDescendants(eq(dbSession), argThat(ProjectCollectionContextLoaderTest::isProjectLeavesQuery)))
      .thenReturn(List.of(new ComponentDto(), new ComponentDto().setCopyComponentUuid(BRANCH_ID)));
    when(branchDao.selectByUuids(dbSession, List.of(BRANCH_ID))).thenReturn(List.of(branch));
    when(projectDao.selectByUuids(dbSession, Set.of(PROJECT_ID))).thenReturn(List.of(project));
    when(userSession.keepAuthorizedEntities(USER, List.of(project))).thenReturn(List.of(project));

    ProjectCollectionContext context = underTest.load(dbSession, PORTFOLIO_ID);

    assertThat(context.branches()).singleElement().satisfies(result -> {
      assertThat(result.branchId()).isEqualTo(BRANCH_ID);
      assertThat(result.projectKey()).isEqualTo("project-key");
    });
    assertThat(context.visibleBranchIds()).containsExactly(BRANCH_ID);
    verify(userSession).checkEntityPermission(USER, portfolio);
    verify(branchDao).selectByUuids(dbSession, List.of(BRANCH_ID));
  }

  @Test
  public void loadPortfolioMarksOnlyAuthorizedProjectBranchesAsVisible() {
    String hiddenBranchId = "hidden-branch-uuid";
    String hiddenProjectId = "hidden-project-uuid";
    PortfolioDto portfolio = new PortfolioDto().setUuid(PORTFOLIO_ID);
    BranchDto visibleBranch = branch(BRANCH_ID, PROJECT_ID, "main");
    BranchDto hiddenBranch = branch(hiddenBranchId, hiddenProjectId, "main");
    ProjectDto visibleProject = project(PROJECT_ID, "visible-project", "Visible project");
    ProjectDto hiddenProject = project(hiddenProjectId, "hidden-project", "Hidden project");
    when(portfolioDao.selectByUuid(dbSession, PORTFOLIO_ID)).thenReturn(Optional.of(portfolio));
    when(componentDao.selectDescendants(eq(dbSession), argThat(ProjectCollectionContextLoaderTest::isProjectLeavesQuery)))
      .thenReturn(List.of(
        new ComponentDto().setCopyComponentUuid(BRANCH_ID),
        new ComponentDto().setCopyComponentUuid(hiddenBranchId)));
    when(branchDao.selectByUuids(dbSession, List.of(BRANCH_ID, hiddenBranchId))).thenReturn(List.of(visibleBranch, hiddenBranch));
    when(projectDao.selectByUuids(dbSession, Set.of(PROJECT_ID, hiddenProjectId))).thenReturn(List.of(visibleProject, hiddenProject));
    when(userSession.keepAuthorizedEntities(USER, List.of(visibleProject, hiddenProject))).thenReturn(List.of(visibleProject));

    ProjectCollectionContext context = underTest.load(dbSession, PORTFOLIO_ID);

    assertThat(context.branches()).extracting(ProjectBranch::branchId).containsExactly(BRANCH_ID, hiddenBranchId);
    assertThat(context.visibleBranchIds()).containsExactly(BRANCH_ID);
  }

  @Test
  public void loadApplicationReturnsProjectsForSelectedMainBranch() {
    BranchDto applicationBranch = branch(APPLICATION_BRANCH_ID, APPLICATION_ID, "main").setIsMain(true);
    ProjectDto application = project(APPLICATION_ID, "application", "Application").setQualifier(APP);
    BranchDto branch = branch(BRANCH_ID, PROJECT_ID, "main");
    ProjectDto project = project(PROJECT_ID, "project-key", "Project");
    when(branchDao.selectByUuid(dbSession, APPLICATION_BRANCH_ID)).thenReturn(Optional.of(applicationBranch));
    when(projectDao.selectByUuid(dbSession, APPLICATION_ID)).thenReturn(Optional.of(application));
    when(applicationProjectsDao.selectProjectsMainBranchesOfApplication(dbSession, APPLICATION_ID)).thenReturn(List.of(branch));
    when(projectDao.selectByUuids(dbSession, Set.of(PROJECT_ID))).thenReturn(List.of(project));
    when(userSession.keepAuthorizedEntities(USER, List.of(project))).thenReturn(List.of(project));

    ProjectCollectionContext context = underTest.load(dbSession, "APPLICATION", APPLICATION_BRANCH_ID);

    assertThat(context.branches()).singleElement().satisfies(result -> assertThat(result.branchId()).isEqualTo(BRANCH_ID));
    verify(userSession).checkEntityPermission(USER, application);
    verify(userSession).checkChildProjectsPermission(USER, application);
  }

  @Test
  public void loadApplicationReturnsProjectsForSelectedNonMainBranch() {
    BranchDto applicationBranch = branch(APPLICATION_BRANCH_ID, APPLICATION_ID, "branch").setIsMain(false);
    ProjectDto application = project(APPLICATION_ID, "application", "Application").setQualifier(APP);
    BranchDto branch = branch(BRANCH_ID, PROJECT_ID, "project-branch");
    ProjectDto project = project(PROJECT_ID, "project-key", "Project");
    when(branchDao.selectByUuid(dbSession, APPLICATION_BRANCH_ID)).thenReturn(Optional.of(applicationBranch));
    when(projectDao.selectByUuid(dbSession, APPLICATION_ID)).thenReturn(Optional.of(application));
    when(applicationProjectsDao.selectProjectBranchesFromAppBranchUuid(dbSession, APPLICATION_BRANCH_ID)).thenReturn(Set.of(branch));
    when(projectDao.selectByUuids(dbSession, Set.of(PROJECT_ID))).thenReturn(List.of(project));
    when(userSession.keepAuthorizedEntities(USER, List.of(project))).thenReturn(List.of(project));

    ProjectCollectionContext context = underTest.load(dbSession, "APPLICATION", APPLICATION_BRANCH_ID);

    assertThat(context.branches()).singleElement().satisfies(result -> assertThat(result.branchId()).isEqualTo(BRANCH_ID));
    verify(applicationProjectsDao).selectProjectBranchesFromAppBranchUuid(dbSession, APPLICATION_BRANCH_ID);
  }

  @Test
  public void loadRejectsBranchWithoutMatchingProject() {
    PortfolioDto portfolio = new PortfolioDto().setUuid(PORTFOLIO_ID);
    BranchDto branch = branch(BRANCH_ID, PROJECT_ID, "main");
    when(portfolioDao.selectByUuid(dbSession, PORTFOLIO_ID)).thenReturn(Optional.of(portfolio));
    when(componentDao.selectDescendants(eq(dbSession), argThat(ProjectCollectionContextLoaderTest::isProjectLeavesQuery)))
      .thenReturn(List.of(new ComponentDto().setCopyComponentUuid(BRANCH_ID)));
    when(branchDao.selectByUuids(dbSession, List.of(BRANCH_ID))).thenReturn(List.of(branch));
    when(projectDao.selectByUuids(dbSession, Set.of(PROJECT_ID))).thenReturn(List.of());

    assertThatThrownBy(() -> underTest.load(dbSession, PORTFOLIO_ID))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Project '%s' for branch '%s' not found", PROJECT_ID, BRANCH_ID);
  }

  @Test
  public void loadRejectsUnknownPortfolio() {
    when(portfolioDao.selectByUuid(dbSession, PORTFOLIO_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.load(dbSession, PORTFOLIO_ID))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Portfolio or application branch '%s' not found", PORTFOLIO_ID);
  }

  @Test
  public void loadRejectsUnsupportedEntityType() {
    assertThatThrownBy(() -> underTest.load(dbSession, "PROJECT", PROJECT_ID))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("entityType must be one of: PORTFOLIO, APPLICATION");
  }

  private static boolean isProjectLeavesQuery(ComponentTreeQuery query) {
    return PORTFOLIO_ID.equals(query.getBaseUuid())
      && query.getStrategy() == LEAVES
      && Set.copyOf(query.getQualifiers()).equals(Set.of(PROJECT));
  }

  private static BranchDto branch(String uuid, String projectUuid, String key) {
    return new BranchDto().setUuid(uuid).setProjectUuid(projectUuid).setKey(key);
  }

  private static ProjectDto project(String uuid, String key, String name) {
    return new ProjectDto().setUuid(uuid).setKey(key).setName(name);
  }
}
