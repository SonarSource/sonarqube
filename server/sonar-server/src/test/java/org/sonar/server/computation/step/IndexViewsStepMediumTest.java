/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.step;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.QueryContext;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.view.index.ViewIndexer;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * It's still not possible to only used EsTester as IssueIndex does not support it yet.
 */
public class IndexViewsStepMediumTest {

  ComputationContext context;

  DbSession dbSession;

  ViewIndexer indexer;

  IndexViewsStep step;

  @ClassRule
  public static ServerTester tester = new ServerTester().addComponents(IndexViewsStep.class);

  IssueIndex index;

  @Before
  public void setUp() throws Exception {
    tester.clearIndexes();
    context = mock(ComputationContext.class);
    dbSession = tester.get(DbClient.class).openSession(false);
    index = tester.get(IssueIndex.class);
    step = tester.get(IndexViewsStep.class);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void clear_cache_of_issue_on_view_filter() throws Exception {
    String viewUuid = "ABCD";
    when(context.getProject()).thenReturn(ComponentTesting.newProjectDto(viewUuid).setQualifier(Qualifiers.VIEW));

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, rule);
    ComponentDto project1 = addProjectWithIssue(rule);

    ComponentDto view = ComponentTesting.newView("ABCD");
    ComponentDto techProject1 = ComponentTesting.newTechnicalProject("CDEF", project1, view);
    tester.get(ComponentDao.class).insert(dbSession, view, techProject1);
    dbSession.commit();
    tester.get(ViewIndexer.class).index(viewUuid);

    // Execute issue query on view -> 1 issue on view (and filter on view will be set in cache)
    assertThat(tester.get(IssueIndex.class).search(IssueQuery.builder().viewUuids(newArrayList(viewUuid)).build(), new QueryContext()).getHits()).hasSize(1);

    // Add a project to the view
    ComponentDto project2 = addProjectWithIssue(rule);
    ComponentDto techProject2 = ComponentTesting.newTechnicalProject("EFGH", project2, view);
    tester.get(ComponentDao.class).insert(dbSession, techProject2);
    dbSession.commit();

    // Execute issue query on view -> Still 1 issue on view, issue on project2 is not yet visible
    assertThat(tester.get(IssueIndex.class).search(IssueQuery.builder().viewUuids(newArrayList(viewUuid)).build(), new QueryContext()).getHits()).hasSize(1);

    step.execute(context);

    // Execute issue query on view -> issue of project2 are well taken into account
    assertThat(tester.get(IssueIndex.class).search(IssueQuery.builder().viewUuids(newArrayList(viewUuid)).build(), new QueryContext()).getHits()).hasSize(2);
  }

  private ComponentDto addProjectWithIssue(RuleDto rule) {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    tester.get(ComponentDao.class).insert(dbSession, project, file);

    IssueDto issue = IssueTesting.newDto(rule, file, project);
    tester.get(IssueDao.class).insert(dbSession, issue);
    dbSession.commit();

    setDefaultProjectPermission(project);
    tester.get(IssueIndexer.class).indexAll();

    return project;
  }

  private void setDefaultProjectPermission(ComponentDto project) {
    // project can be seen by anyone and by code viewer
    MockUserSession.set().setLogin("admin").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    tester.get(InternalPermissionService.class).addPermission(new PermissionChange().setComponentKey(project.getKey()).setGroup(DefaultGroups.ANYONE).setPermission(UserRole.USER));
    MockUserSession.set();
  }
}
