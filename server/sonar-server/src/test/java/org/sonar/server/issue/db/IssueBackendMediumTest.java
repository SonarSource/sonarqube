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
package org.sonar.server.issue.db;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.component.persistence.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.issue.index.IssueDoc;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.platform.Platform;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.SearchClient;
import org.sonar.server.tester.ServerTester;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static org.fest.assertions.Assertions.assertThat;

public class IssueBackendMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient dbClient;
  IndexClient indexClient;
  Platform platform;
  DbSession dbSession;

  @Before
  public void setUp() throws Exception {
    dbClient = tester.get(DbClient.class);
    indexClient = tester.get(IndexClient.class);
    platform = tester.get(Platform.class);
    dbSession = dbClient.openSession(false);
    tester.clearDbAndIndexes();
  }

  @After
  public void tearDown() throws Exception {
    if (dbSession != null) {
      dbSession.close();
    }
  }

  @Test
  public void insert_and_find_by_key() throws Exception {
    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, rule);

    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setKey("MyProject")
      .setProjectId(1L);
    tester.get(ComponentDao.class).insert(dbSession, project);

    ComponentDto resource = new ComponentDto()
      .setProjectId(1L)
      .setKey("MyComponent")
      .setId(2L);
    tester.get(ComponentDao.class).insert(dbSession, resource);

    IssueDto issue = new IssueDto()
      .setIssueCreationDate(new Date())
      .setIssueUpdateDate(new Date())
      .setRule(rule)
      .setRootComponent(project)
      .setComponent(resource)
      .setStatus("OPEN").setResolution("OPEN")
      .setKee(UUID.randomUUID().toString())
      .setSeverity("MAJOR");
    dbClient.issueDao().insert(dbSession, issue);

    dbSession.commit();

    // Check that Issue is in Index
    assertThat(indexClient.get(IssueIndex.class).countAll()).isEqualTo(1);

    // should find by key
    IssueDoc issueDoc = indexClient.get(IssueIndex.class).getByKey(issue.getKey());
    assertThat(issueDoc).isNotNull();

    // Check all normalized fields
    assertThat(issueDoc.actionPlanKey()).isEqualTo(issue.getActionPlanKey());
    assertThat(issueDoc.assignee()).isEqualTo(issue.getAssignee());
    assertThat(issueDoc.authorLogin()).isEqualTo(issue.getAuthorLogin());
    assertThat(issueDoc.closeDate()).isEqualTo(issue.getIssueCloseDate());
    assertThat(issueDoc.componentKey()).isEqualTo(issue.getComponentKey());
    assertThat(issueDoc.creationDate()).isEqualTo(issue.getCreatedAt());
    assertThat(issueDoc.effortToFix()).isEqualTo(issue.getEffortToFix());
    assertThat(issueDoc.resolution()).isEqualTo(issue.getResolution());
    assertThat(issueDoc.ruleKey()).isEqualTo(RuleKey.of(issue.getRuleRepo(), issue.getRule()));
    assertThat(issueDoc.line()).isEqualTo(issue.getLine());
    assertThat(issueDoc.message()).isEqualTo(issue.getMessage());
    assertThat(issueDoc.reporter()).isEqualTo(issue.getReporter());
    assertThat(issueDoc.key()).isEqualTo(issue.getKey());
    assertThat(issueDoc.updateDate()).isEqualTo(issue.getIssueUpdateDate());
    assertThat(issueDoc.status()).isEqualTo(issue.getStatus());
    assertThat(issueDoc.severity()).isEqualTo(issue.getSeverity());

    // assertThat(issueDoc.attributes()).isEqualTo(issue.getIssueAttributes());
    // assertThat(issueDoc.isNew()).isEqualTo(issue.isN());
    // assertThat(issueDoc.comments()).isEqualTo(issue.());
  }

  @Test
  public void insert_and_find_after_date() throws Exception {

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, rule);

    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setKey("MyProject")
      .setProjectId(1L);
    tester.get(ComponentDao.class).insert(dbSession, project);

    ComponentDto resource = new ComponentDto()
      .setId(2L)
      .setKey("MyComponent")
      .setProjectId(1L);
    tester.get(ComponentDao.class).insert(dbSession, resource);

    IssueDto issue = new IssueDto().setId(1L)
      .setRuleId(rule.getId())
      .setRootComponentId(project.getId())
      .setRootComponentKey_unit_test_only(project.key())
      .setComponentId(resource.getId())
      .setComponentKey_unit_test_only(resource.key())
      .setStatus("OPEN").setResolution("OPEN")
      .setKee(UUID.randomUUID().toString());
    dbClient.issueDao().insert(dbSession, issue);

    dbSession.commit();
    assertThat(issue.getId()).isNotNull();

    // Find Issues since forever
    Date t0 = new Date(0);
    assertThat(dbClient.issueDao().findAfterDate(dbSession, t0)).hasSize(1);

    // Should not find any new issues
    Date t1 = new Date();
    assertThat(dbClient.issueDao().findAfterDate(dbSession, t1)).hasSize(0);

    // Should synchronise
    tester.clearIndexes();
    assertThat(indexClient.get(IssueIndex.class).countAll()).isEqualTo(0);
    tester.get(Platform.class).executeStartupTasks();
    assertThat(indexClient.get(IssueIndex.class).countAll()).isEqualTo(1);
  }

  @Test
  public void issue_authorization_on_group() throws Exception {
    SearchClient searchClient = tester.get(SearchClient.class);
    createIssueAuthorizationIndex(searchClient, IndexDefinition.ISSUES_AUTHENTICATION.getIndexName(), IndexDefinition.ISSUES_AUTHENTICATION.getIndexType());

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, rule);

    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setProjectId(1L)
      .setKey("SonarQube");
    tester.get(ComponentDao.class).insert(dbSession, project);

    ComponentDto resource = new ComponentDto()
      .setProjectId(1L)
      .setId(2L)
      .setKey("IssueAction.java");
    tester.get(ComponentDao.class).insert(dbSession, resource);

    IssueDto issue = new IssueDto().setId(1L)
      .setRuleId(rule.getId())
      .setRootComponentKey_unit_test_only(project.key())
      .setRootComponentId(project.getId())
      .setComponentKey_unit_test_only(resource.key())
      .setComponentId(resource.getId())
      .setStatus("OPEN").setResolution("OPEN")
      .setKee(UUID.randomUUID().toString());
    dbClient.issueDao().insert(dbSession, issue);

    dbSession.commit();

    searchClient.prepareIndex(IndexDefinition.ISSUES_AUTHENTICATION.getIndexName(), IndexDefinition.ISSUES_AUTHENTICATION.getIndexType())
      .setId(project.key())
      .setSource(ImmutableMap.<String, Object>of("permission", "read", "project", project.key(), "group", "user"))
      .setRefresh(true)
      .get();

    // The issue is only visible for group user
    assertThat(searchIssueWithAuthorization(searchClient, "user", "").getHits().getTotalHits()).isEqualTo(1);
    // The issue is not visible for group reviewer
    assertThat(searchIssueWithAuthorization(searchClient, "reviewer", "").getHits().getTotalHits()).isEqualTo(0);
  }

  @Test
  public void issue_authorization_on_user() throws Exception {
    SearchClient searchClient = tester.get(SearchClient.class);
    createIssueAuthorizationIndex(searchClient, IndexDefinition.ISSUES_AUTHENTICATION.getIndexName(), IndexDefinition.ISSUES_AUTHENTICATION.getIndexType());

    RuleDto rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(dbSession, rule);

    ComponentDto project = new ComponentDto()
      .setId(1L)
      .setProjectId(1L)
      .setKey("SonarQube");
    tester.get(ComponentDao.class).insert(dbSession, project);

    ComponentDto resource = new ComponentDto()
      .setProjectId(1L)
      .setId(2L)
      .setKey("IssueAction.java");
    tester.get(ComponentDao.class).insert(dbSession, resource);

    IssueDto issue = new IssueDto().setId(1L)
      .setRuleId(rule.getId())
      .setRootComponentKey_unit_test_only(project.key())
      .setRootComponentId(project.getId())
      .setComponentKey_unit_test_only(resource.key())
      .setComponentId(resource.getId())
      .setStatus("OPEN").setResolution("OPEN")
      .setKee(UUID.randomUUID().toString());
    dbClient.issueDao().insert(dbSession, issue);

    dbSession.commit();

    searchClient.prepareIndex(IndexDefinition.ISSUES_AUTHENTICATION.getIndexName(), IndexDefinition.ISSUES_AUTHENTICATION.getIndexType())
      .setId(project.key())
      .setSource(ImmutableMap.<String, Object>of("permission", "read", "project", project.key(), "user", "julien"))
      .setRefresh(true)
      .get();

    // The issue is visible for user julien
    assertThat(searchIssueWithAuthorization(searchClient, "", "julien").getHits().getTotalHits()).isEqualTo(1);
    // The issue is not visible for user simon
    assertThat(searchIssueWithAuthorization(searchClient, "", "simon").getHits().getTotalHits()).isEqualTo(0);
  }

  private SearchResponse searchIssueWithAuthorization(SearchClient searchClient, String group, String user){
    SearchRequestBuilder request = searchClient.prepareSearch(IndexDefinition.ISSUES.getIndexName())
      .setQuery(
        QueryBuilders.filteredQuery(
          QueryBuilders.matchAllQuery(),
          FilterBuilders.hasParentFilter(
            IndexDefinition.ISSUES_AUTHENTICATION.getIndexType(),
            FilterBuilders.boolFilter().must(
              FilterBuilders.termFilter("permission", "read"),
              FilterBuilders.orFilter(
                FilterBuilders.termFilter("group", group),
                FilterBuilders.termFilter("user", user)
              )
            ).cache(true)
          )
        )
      )
      .setSize(Integer.MAX_VALUE);
    return searchClient.execute(request);
  }

  private BaseIndex createIssueAuthorizationIndex(final SearchClient searchClient, String index, String type) {
    BaseIndex baseIndex = new BaseIndex(
      IndexDefinition.createFor(index, type),
      null, searchClient) {
      @Override
      protected String getKeyValue(Serializable key) {
        return null;
      }

      @Override
      protected org.elasticsearch.common.settings.Settings getIndexSettings() throws IOException {
        return ImmutableSettings.builder().build();
      }

      @Override
      protected Map mapProperties() {
        Map<String, Object> mapping = new HashMap<String, Object>();
        mapping.put("permission", mapStringField());
        mapping.put("project", mapStringField());
        mapping.put("group", mapStringField());
        mapping.put("user", mapStringField());
        return mapping;
      }

      protected Map mapStringField() {
        Map<String, Object> mapping = new HashMap<String, Object>();
        mapping.put("type", "string");
        mapping.put("index", "analyzed");
        mapping.put("index_analyzer", "keyword");
        mapping.put("search_analyzer", "whitespace");
        return mapping;
      }

      @Override
      protected Map mapKey() {
        return Collections.emptyMap();
      }

      @Override
      public Object toDoc(Map fields) {
        return null;
      }
    };
    baseIndex.start();
    return baseIndex;
  }
}
