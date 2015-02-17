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

package org.sonar.server.view.index;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.permission.InternalPermissionService;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * It's not possible to only used EsTester as IssueIndex does not support it yet.
 *
 * Only clear of the views lookup cache is tested here.
 * See {@link ViewIndexerTest} for tests on common use cases.
 */
public class ViewIndexerMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbSession dbSession;

  ViewIndexer indexer;

  IssueIndex index;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    dbSession = tester.get(DbClient.class).openSession(false);
    index = tester.get(IssueIndex.class);
    indexer = tester.get(ViewIndexer.class);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void clear_views_lookup_cache_on_index_view_uuid() throws Exception {
    String viewUuid = "ABCD";

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, rule);
    ComponentDto project1 = addProjectWithIssue(rule);

    ComponentDto view = ComponentTesting.newView("ABCD");
    ComponentDto techProject1 = ComponentTesting.newProjectCopy("CDEF", project1, view);
    tester.get(ComponentDao.class).insert(dbSession, view, techProject1);
    dbSession.commit();

    // First view indexation
    indexer.index(viewUuid);

    // Execute issue query on view -> 1 issue on view
    SearchResult<IssueDoc> docs = tester.get(IssueIndex.class).search(IssueQuery.builder().viewUuids(newArrayList(viewUuid)).build(),
      new SearchOptions());
    assertThat(docs.getDocs()).hasSize(1);

    // Add a project to the view and index it again
    ComponentDto project2 = addProjectWithIssue(rule);
    ComponentDto techProject2 = ComponentTesting.newProjectCopy("EFGH", project2, view);
    tester.get(ComponentDao.class).insert(dbSession, techProject2);
    dbSession.commit();
    indexer.index(viewUuid);

    // Execute issue query on view -> issue of project2 are well taken into account : the cache has been cleared
    assertThat(tester.get(IssueIndex.class).search(IssueQuery.builder().viewUuids(newArrayList(viewUuid)).build(), new SearchOptions()).getDocs()).hasSize(2);
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
