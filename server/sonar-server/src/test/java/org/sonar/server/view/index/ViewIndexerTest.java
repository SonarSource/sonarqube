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

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.db.user.RoleDao;
import org.sonar.db.component.ComponentTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.index.IssueAuthorizationIndexer;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class ViewIndexerTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new IssueIndexDefinition(new Settings()), new ViewIndexDefinition(new Settings()));

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  DbClient dbClient;

  DbSession dbSession;

  ViewIndexer indexer;

  @Before
  public void setUp() {
    dbTester.truncateTables();
    esTester.truncateIndices();

    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new RuleDao(System2.INSTANCE), new ComponentDao(), new IssueDao(dbTester.myBatis()), new RoleDao());
    dbSession = dbClient.openSession(false);
    indexer = (ViewIndexer) new ViewIndexer(dbClient, esTester.client()).setEnabled(true);
  }

  @After
  public void after() {
    dbSession.close();
  }


  @Test
  public void index_nothing() {
    indexer.index();
    assertThat(esTester.countDocuments(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW)).isEqualTo(0L);
  }

  @Test
  public void index() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    indexer.index();

    List<ViewDoc> docs = esTester.getDocuments("views", "view", ViewDoc.class);
    assertThat(docs).hasSize(4);

    Map<String, ViewDoc> viewsByUuid = Maps.uniqueIndex(docs, new Function<ViewDoc, String>() {
      @Override
      public String apply(ViewDoc doc) {
        return doc.uuid();
      }
    });

    assertThat(viewsByUuid.get("ABCD").projects()).containsOnly("JKLM");
    assertThat(viewsByUuid.get("EFGH").projects()).containsOnly("KLMN", "JKLM");
    assertThat(viewsByUuid.get("FGHI").projects()).containsOnly("JKLM");
    assertThat(viewsByUuid.get("IJKL").projects()).isEmpty();
  }

  @Test
  public void index_only_if_empty_do_nothing_when_index_already_exists() throws Exception {
    // Some views are not in the db
    dbTester.prepareDbUnit(getClass(), "index.xml");
    esTester.putDocuments(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW,
      new ViewDoc().setUuid("ABCD").setProjects(newArrayList("BCDE")));

    indexer.index();

    // ... But they shouldn't be indexed
    assertThat(esTester.countDocuments(ViewIndexDefinition.INDEX, ViewIndexDefinition.TYPE_VIEW)).isEqualTo(1L);
  }

  @Test
  public void index_root_view() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    indexer.index("EFGH");

    List<ViewDoc> docs = esTester.getDocuments("views", "view", ViewDoc.class);
    assertThat(docs).hasSize(2);

    Map<String, ViewDoc> viewsByUuid = Maps.uniqueIndex(docs, new Function<ViewDoc, String>() {
      @Override
      public String apply(ViewDoc doc) {
        return doc.uuid();
      }
    });

    assertThat(viewsByUuid.get("EFGH").projects()).containsOnly("KLMN", "JKLM");
    assertThat(viewsByUuid.get("FGHI").projects()).containsOnly("JKLM");
  }

  @Test
  public void index_view_doc() {
    indexer.index(new ViewDoc().setUuid("EFGH").setProjects(newArrayList("KLMN", "JKLM")));

    List<ViewDoc> docs = esTester.getDocuments("views", "view", ViewDoc.class);
    assertThat(docs).hasSize(1);

    ViewDoc view = docs.get(0);
    assertThat(view.uuid()).isEqualTo("EFGH");
    assertThat(view.projects()).containsOnly("KLMN", "JKLM");
  }

  @Test
  public void clear_views_lookup_cache_on_index_view_uuid() {
    IssueIndex issueIndex = new IssueIndex(esTester.client(), System2.INSTANCE, userSessionRule);
    IssueIndexer issueIndexer = (IssueIndexer) new IssueIndexer(dbClient, esTester.client()).setEnabled(true);
    IssueAuthorizationIndexer issueAuthorizationIndexer = (IssueAuthorizationIndexer) new IssueAuthorizationIndexer(dbClient, esTester.client()).setEnabled(true);

    String viewUuid = "ABCD";

    RuleDto rule = RuleTesting.newXooX1();
    dbClient.deprecatedRuleDao().insert(dbSession, rule);
    ComponentDto project1 = addProjectWithIssue(rule);
    issueIndexer.indexAll();
    issueAuthorizationIndexer.index();

    ComponentDto view = ComponentTesting.newView("ABCD");
    ComponentDto techProject1 = ComponentTesting.newProjectCopy("CDEF", project1, view);
    dbClient.componentDao().insert(dbSession, view, techProject1);
    dbSession.commit();

    // First view indexation
    indexer.index(viewUuid);

    // Execute issue query on view -> 1 issue on view
    SearchResult<IssueDoc> docs = issueIndex.search(IssueQuery.builder(userSessionRule).viewUuids(newArrayList(viewUuid)).build(), new SearchOptions());
    assertThat(docs.getDocs()).hasSize(1);

    // Add a project to the view and index it again
    ComponentDto project2 = addProjectWithIssue(rule);
    issueIndexer.indexAll();
    issueAuthorizationIndexer.index();

    ComponentDto techProject2 = ComponentTesting.newProjectCopy("EFGH", project2, view);
    dbClient.componentDao().insert(dbSession, techProject2);
    dbSession.commit();
    indexer.index(viewUuid);

    // Execute issue query on view -> issue of project2 are well taken into account : the cache has been cleared
    assertThat(issueIndex.search(IssueQuery.builder(userSessionRule).viewUuids(newArrayList(viewUuid)).build(), new SearchOptions()).getDocs()).hasSize(2);
  }

  private ComponentDto addProjectWithIssue(RuleDto rule) {
    ComponentDto project = ComponentTesting.newProjectDto();
    ComponentDto file = ComponentTesting.newFileDto(project);
    dbClient.componentDao().insert(dbSession, project, file);
    dbClient.roleDao().insertGroupRole(dbSession, new GroupRoleDto().setRole(UserRole.USER).setGroupId(null).setResourceId(project.getId()));

    IssueDto issue = IssueTesting.newDto(rule, file, project);
    dbClient.issueDao().insert(dbSession, issue);
    dbSession.commit();

    return project;
  }

}
