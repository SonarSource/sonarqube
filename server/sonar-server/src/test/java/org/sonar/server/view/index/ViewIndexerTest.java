/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.view.index;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.issue.index.IssueIndexDefinition;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.index.IssueIteratorFactory;
import org.sonar.server.permission.index.AuthorizationTypeSupport;
import org.sonar.server.permission.index.PermissionIndexer;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ViewIndexerTest {

  private System2 system2 = System2.INSTANCE;

  @Rule
  public DbTester dbTester = DbTester.create(system2);

  @Rule
  public EsTester esTester = new EsTester(new IssueIndexDefinition(new MapSettings()), new ViewIndexDefinition(new MapSettings()));

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private DbClient dbClient = dbTester.getDbClient();
  private DbSession dbSession = dbTester.getSession();
  private IssueIndexer issueIndexer = new IssueIndexer(esTester.client(), new IssueIteratorFactory(dbClient));
  private PermissionIndexer permissionIndexer = new PermissionIndexer(dbClient, esTester.client(), issueIndexer);
  private ViewIndexer underTest = new ViewIndexer(dbClient, esTester.client());

  @Test
  public void index_nothing() {
    underTest.indexOnStartup(null);
    assertThat(esTester.countDocuments(ViewIndexDefinition.INDEX_TYPE_VIEW)).isEqualTo(0L);
  }

  @Test
  public void index() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    underTest.indexOnStartup(null);

    List<ViewDoc> docs = esTester.getDocuments(ViewIndexDefinition.INDEX_TYPE_VIEW, ViewDoc.class);
    assertThat(docs).hasSize(4);

    Map<String, ViewDoc> viewsByUuid = Maps.uniqueIndex(docs, ViewDoc::uuid);

    assertThat(viewsByUuid.get("ABCD").projects()).containsOnly("JKLM");
    assertThat(viewsByUuid.get("EFGH").projects()).containsOnly("KLMN", "JKLM");
    assertThat(viewsByUuid.get("FGHI").projects()).containsOnly("JKLM");
    assertThat(viewsByUuid.get("IJKL").projects()).isEmpty();
  }

  @Test
  public void index_root_view() {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    underTest.index("EFGH");

    List<ViewDoc> docs = esTester.getDocuments(ViewIndexDefinition.INDEX_TYPE_VIEW, ViewDoc.class);
    assertThat(docs).hasSize(2);

    Map<String, ViewDoc> viewsByUuid = Maps.uniqueIndex(docs, ViewDoc::uuid);

    assertThat(viewsByUuid.get("EFGH").projects()).containsOnly("KLMN", "JKLM");
    assertThat(viewsByUuid.get("FGHI").projects()).containsOnly("JKLM");
  }

  @Test
  public void index_view_doc() {
    underTest.index(new ViewDoc().setUuid("EFGH").setProjects(newArrayList("KLMN", "JKLM")));

    List<ViewDoc> docs = esTester.getDocuments(ViewIndexDefinition.INDEX_TYPE_VIEW, ViewDoc.class);
    assertThat(docs).hasSize(1);

    ViewDoc view = docs.get(0);
    assertThat(view.uuid()).isEqualTo("EFGH");
    assertThat(view.projects()).containsOnly("KLMN", "JKLM");
  }

  @Test
  public void clear_views_lookup_cache_on_index_view_uuid() {
    IssueIndex issueIndex = new IssueIndex(esTester.client(), System2.INSTANCE, userSessionRule, new AuthorizationTypeSupport(userSessionRule)
    );
    IssueIndexer issueIndexer = new IssueIndexer(esTester.client(), new IssueIteratorFactory(dbClient));

    String viewUuid = "ABCD";

    RuleDto rule = RuleTesting.newXooX1();
    dbClient.ruleDao().insert(dbSession, rule.getDefinition());
    ComponentDto project1 = addProjectWithIssue(rule, dbTester.organizations().insert());
    issueIndexer.indexOnStartup(issueIndexer.getIndexTypes());
    permissionIndexer.indexProjectsByUuids(dbSession, asList(project1.uuid()));

    OrganizationDto organizationDto = dbTester.organizations().insert();
    ComponentDto view = ComponentTesting.newView(organizationDto, "ABCD");
    ComponentDto techProject1 = ComponentTesting.newProjectCopy("CDEF", project1, view);
    dbClient.componentDao().insert(dbSession, view, techProject1);
    dbSession.commit();

    // First view indexation
    underTest.index(viewUuid);

    // Execute issue query on view -> 1 issue on view
    SearchResult<IssueDoc> docs = issueIndex.search(IssueQuery.builder().viewUuids(newArrayList(viewUuid)).build(), new SearchOptions());
    assertThat(docs.getDocs()).hasSize(1);

    // Add a project to the view and index it again
    ComponentDto project2 = addProjectWithIssue(rule, organizationDto);
    issueIndexer.indexOnStartup(issueIndexer.getIndexTypes());
    permissionIndexer.indexProjectsByUuids(dbSession, asList(project2.uuid()));

    ComponentDto techProject2 = ComponentTesting.newProjectCopy("EFGH", project2, view);
    dbClient.componentDao().insert(dbSession, techProject2);
    dbSession.commit();
    underTest.index(viewUuid);

    // Execute issue query on view -> issue of project2 are well taken into account : the cache has been cleared
    assertThat(issueIndex.search(IssueQuery.builder().viewUuids(newArrayList(viewUuid)).build(), new SearchOptions()).getDocs()).hasSize(2);
  }

  private ComponentDto addProjectWithIssue(RuleDto rule, OrganizationDto org) {
    ComponentDto project = ComponentTesting.newPublicProjectDto(org);
    ComponentDto file = ComponentTesting.newFileDto(project, null);
    dbTester.components().insertComponents(project, file);

    IssueDto issue = IssueTesting.newDto(rule, file, project);
    dbClient.issueDao().insert(dbSession, issue);
    dbSession.commit();

    return project;
  }

}
