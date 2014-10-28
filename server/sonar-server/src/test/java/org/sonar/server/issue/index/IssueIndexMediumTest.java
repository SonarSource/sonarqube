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
package org.sonar.server.issue.index;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.settings.Settings;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.permission.PermissionFacade;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.user.GroupDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.platform.BackendCleanup;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.IndexDefinition;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.search.SearchClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class IssueIndexMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession session;
  IssueIndex index;

  RuleDto rule;
  ComponentDto project;
  ComponentDto file;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();
    db = tester.get(DbClient.class);
    session = db.openSession(false);
    index = tester.get(IssueIndex.class);

    rule = RuleTesting.newXooX1();
    tester.get(RuleDao.class).insert(session, rule);

    project = ComponentTesting.newProjectDto();
    tester.get(ComponentDao.class).insert(session, project);

    file = ComponentTesting.newFileDto(project);
    tester.get(ComponentDao.class).insert(session, file);

    // project can be seen by anyone
    tester.get(PermissionFacade.class).insertGroupPermission(project.getId(), DefaultGroups.ANYONE, UserRole.USER, session);
    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    MockUserSession.set();

    session.commit();
    session.clearCache();
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void get_by_key() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project);
    db.issueDao().insert(session, issue);
    session.commit();

    Issue result = index.getByKey(issue.getKey());
    IssueTesting.assertIsEquivalent(issue, (IssueDoc) result);
  }

  @Test
  public void get_by_key_with_attributes() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project);
    db.issueDao().insert(session, issue).setIssueAttributes(KeyValueFormat.format(ImmutableMap.of("jira-issue-key", "SONAR-1234")));
    session.commit();

    Issue result = index.getByKey(issue.getKey());
    IssueTesting.assertIsEquivalent(issue, (IssueDoc) result);

    assertThat(result.attribute("jira-issue-key")).isEqualTo("SONAR-1234");
  }

  @Test(expected = IllegalStateException.class)
  public void comments_field_is_not_available() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project);
    db.issueDao().insert(session, issue);
    session.commit();

    Issue result = index.getByKey(issue.getKey());
    result.comments();
  }

  @Test(expected = IllegalStateException.class)
  public void is_new_field_is_not_available() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project);
    db.issueDao().insert(session, issue);
    session.commit();

    Issue result = index.getByKey(issue.getKey());
    result.isNew();
  }

  @Test(expected = NotFoundException.class)
  public void fail_to_get_unknown_key() throws Exception {
    index.getByKey("unknown");
  }

  @Test
  public void filter_by_keys() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setKee("1"),
      IssueTesting.newDto(rule, file, project).setKee("2"));
    session.commit();

    assertThat(index.search(IssueQuery.builder().issueKeys(newArrayList("1", "2")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().issueKeys(newArrayList("1")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().issueKeys(newArrayList("3", "4")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_component_roots() throws Exception {
    ComponentDto module = ComponentTesting.newModuleDto(project);
    ComponentDto subModule = ComponentTesting.newModuleDto(module);
    ComponentDto file = ComponentTesting.newFileDto(subModule);
    tester.get(ComponentDao.class).insert(session, module, subModule, file);

    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project));
    session.commit();

    assertThat(index.search(IssueQuery.builder().componentRootUuids(newArrayList(file.uuid())).build(), new QueryContext()).getHits()).isEmpty();
    assertThat(index.search(IssueQuery.builder().componentRootUuids(newArrayList(module.uuid())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().componentRootUuids(newArrayList(subModule.uuid())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().componentRootUuids(newArrayList(project.uuid())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().componentRootUuids(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_components() throws Exception {
    ComponentDto file1 = ComponentTesting.newFileDto(project);
    ComponentDto file2 = ComponentTesting.newFileDto(project);
    tester.get(ComponentDao.class).insert(session, file1, file2);

    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file1, project),
      IssueTesting.newDto(rule, file2, project));
    session.commit();

    assertThat(index.search(IssueQuery.builder().componentUuids(newArrayList(file1.uuid(), file2.uuid())).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().componentUuids(newArrayList(file1.uuid())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().componentUuids(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
    assertThat(index.search(IssueQuery.builder().componentUuids(newArrayList(project.uuid())).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_severities() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setSeverity(Severity.INFO),
      IssueTesting.newDto(rule, file, project).setSeverity(Severity.MAJOR));
    session.commit();

    assertThat(index.search(IssueQuery.builder().severities(newArrayList(Severity.INFO, Severity.MAJOR)).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().severities(newArrayList(Severity.INFO)).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().severities(newArrayList(Severity.BLOCKER)).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_statuses() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_OPEN));
    session.commit();

    assertThat(index.search(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_CLOSED, Issue.STATUS_OPEN)).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_CLOSED)).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_CONFIRMED)).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_resolutions() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      IssueTesting.newDto(rule, file, project).setResolution(Issue.RESOLUTION_FIXED));
    session.commit();

    assertThat(index.search(IssueQuery.builder().resolutions(newArrayList(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_FIXED)).build(), new QueryContext()).getHits())
      .hasSize(2);
    assertThat(index.search(IssueQuery.builder().resolutions(newArrayList(Issue.RESOLUTION_FALSE_POSITIVE)).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().resolutions(newArrayList(Issue.RESOLUTION_REMOVED)).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_resolved() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED),
      IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_OPEN).setResolution(null),
      IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_OPEN).setResolution(null));
    session.commit();

    assertThat(index.search(IssueQuery.builder().resolved(true).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().resolved(false).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().resolved(null).build(), new QueryContext()).getHits()).hasSize(3);
  }

  @Test
  public void filter_by_action_plans() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setActionPlanKey("plan1"),
      IssueTesting.newDto(rule, file, project).setActionPlanKey("plan2"));
    session.commit();

    assertThat(index.search(IssueQuery.builder().actionPlans(newArrayList("plan1")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().actionPlans(newArrayList("plan1", "plan2")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().actionPlans(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_planned() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setActionPlanKey("AP-KEY"),
      IssueTesting.newDto(rule, file, project).setActionPlanKey(null),
      IssueTesting.newDto(rule, file, project).setActionPlanKey(null));
    session.commit();

    assertThat(index.search(IssueQuery.builder().planned(true).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().planned(false).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().planned(null).build(), new QueryContext()).getHits()).hasSize(3);
  }

  @Test
  public void filter_by_rules() throws Exception {
    db.issueDao().insert(session, IssueTesting.newDto(rule, file, project).setRule(rule));

    tester.get(RuleDao.class).insert(session, RuleTesting.newDto(RuleKey.of("rule", "without issue")));
    session.commit();

    assertThat(index.search(IssueQuery.builder().rules(newArrayList(rule.getKey())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().rules(newArrayList(RuleKey.of("rule", "without issue"))).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_languages() throws Exception {
    db.issueDao().insert(session, IssueTesting.newDto(rule, file, project).setRule(rule));
    session.commit();

    assertThat(index.search(IssueQuery.builder().languages(newArrayList(rule.getLanguage())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().languages(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_assignees() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setAssignee("steph"),
      IssueTesting.newDto(rule, file, project).setAssignee("simon"),
      IssueTesting.newDto(rule, file, project));
    session.commit();

    assertThat(index.search(IssueQuery.builder().assignees(newArrayList("steph")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().assignees(newArrayList("steph", "simon")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().assignees(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_assigned() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setAssignee("steph"),
      IssueTesting.newDto(rule, file, project).setAssignee(null),
      IssueTesting.newDto(rule, file, project).setAssignee(null));
    session.commit();

    assertThat(index.search(IssueQuery.builder().assigned(true).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().assigned(false).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().assigned(null).build(), new QueryContext()).getHits()).hasSize(3);
  }

  @Test
  public void filter_by_reporters() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setReporter("fabrice"),
      IssueTesting.newDto(rule, file, project).setReporter("stephane"));
    session.commit();

    assertThat(index.search(IssueQuery.builder().reporters(newArrayList("fabrice", "stephane")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().reporters(newArrayList("fabrice")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().reporters(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_created_after() throws Exception {
    IssueDto issue1 = IssueTesting.newDto(rule, file, project).setIssueCreationDate(DateUtils.parseDate("2014-09-20"));
    IssueDto issue2 = IssueTesting.newDto(rule, file, project).setIssueCreationDate(DateUtils.parseDate("2014-09-23"));
    db.issueDao().insert(session, issue1, issue2);
    session.commit();

    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-19")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-20")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-21")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-25")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_created_before() throws Exception {
    IssueDto issue1 = IssueTesting.newDto(rule, file, project).setIssueCreationDate(DateUtils.parseDate("2014-09-20"));
    IssueDto issue2 = IssueTesting.newDto(rule, file, project).setIssueCreationDate(DateUtils.parseDate("2014-09-23"));
    db.issueDao().insert(session, issue1, issue2);
    session.commit();

    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-19")).build(), new QueryContext()).getHits()).isEmpty();
    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-20")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-21")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-25")).build(), new QueryContext()).getHits()).hasSize(2);
  }

  @Test
  public void filter_by_created_at() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project).setIssueCreationDate(DateUtils.parseDate("2014-09-20"));
    db.issueDao().insert(session, issue);
    session.commit();

    assertThat(index.search(IssueQuery.builder().createdAt(DateUtils.parseDate("2014-09-20")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdAt(DateUtils.parseDate("2014-09-21")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void paging() throws Exception {
    for (int i = 0; i < 12; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder();
    // There are 12 issues in total, with 10 issues per page, the page 2 should only contain 2 elements
    Result<Issue> result = index.search(query.build(), new QueryContext().setPage(2, 10));
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getTotal()).isEqualTo(12);

    result = index.search(IssueQuery.builder().build(), new QueryContext().setOffset(0).setLimit(5));
    assertThat(result.getHits()).hasSize(5);
    assertThat(result.getTotal()).isEqualTo(12);

    result = index.search(IssueQuery.builder().build(), new QueryContext().setOffset(2).setLimit(0));
    assertThat(result.getHits()).hasSize(0);
    assertThat(result.getTotal()).isEqualTo(12);
  }

  @Test
  public void search_with_limit() throws Exception {
    for (int i = 0; i < 20; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();

  }

  @Test
  public void search_with_max_limit() throws Exception {
    List<String> issueKeys = newArrayList();
    for (int i = 0; i < 500; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
      issueKeys.add(issue.getKey());
    }
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder();
    Result<Issue> result = index.search(query.build(), new QueryContext().setMaxLimit());
    assertThat(result.getHits()).hasSize(500);
  }

  @Test
  public void sort_by_status() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_OPEN),
      IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_CLOSED),
      IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_REOPENED)
      );
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).asc(true);
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits().get(0).status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(result.getHits().get(1).status()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(result.getHits().get(2).status()).isEqualTo(Issue.STATUS_REOPENED);

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_STATUS).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits().get(0).status()).isEqualTo(Issue.STATUS_REOPENED);
    assertThat(result.getHits().get(1).status()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(result.getHits().get(2).status()).isEqualTo(Issue.STATUS_CLOSED);
  }

  @Test
  public void sort_by_severity() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setSeverity(Severity.BLOCKER),
      IssueTesting.newDto(rule, file, project).setSeverity(Severity.INFO),
      IssueTesting.newDto(rule, file, project).setSeverity(Severity.MINOR),
      IssueTesting.newDto(rule, file, project).setSeverity(Severity.CRITICAL),
      IssueTesting.newDto(rule, file, project).setSeverity(Severity.MAJOR)
      );
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(true);
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits().get(0).severity()).isEqualTo(Severity.INFO);
    assertThat(result.getHits().get(1).severity()).isEqualTo(Severity.MINOR);
    assertThat(result.getHits().get(2).severity()).isEqualTo(Severity.MAJOR);
    assertThat(result.getHits().get(3).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(result.getHits().get(4).severity()).isEqualTo(Severity.BLOCKER);

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_SEVERITY).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits().get(0).severity()).isEqualTo(Severity.BLOCKER);
    assertThat(result.getHits().get(1).severity()).isEqualTo(Severity.CRITICAL);
    assertThat(result.getHits().get(2).severity()).isEqualTo(Severity.MAJOR);
    assertThat(result.getHits().get(3).severity()).isEqualTo(Severity.MINOR);
    assertThat(result.getHits().get(4).severity()).isEqualTo(Severity.INFO);
  }

  @Test
  public void sort_by_assignee() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setAssignee("steph"),
      IssueTesting.newDto(rule, file, project).setAssignee("simon"));
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_ASSIGNEE).asc(true);
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getHits().get(0).assignee()).isEqualTo("simon");
    assertThat(result.getHits().get(1).assignee()).isEqualTo("steph");

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_ASSIGNEE).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getHits().get(0).assignee()).isEqualTo("steph");
    assertThat(result.getHits().get(1).assignee()).isEqualTo("simon");
  }

  @Test
  public void sort_by_creation_date() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setIssueCreationDate(DateUtils.parseDate("2014-09-23")),
      IssueTesting.newDto(rule, file, project).setIssueCreationDate(DateUtils.parseDate("2014-09-24")));
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(true);
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getHits().get(0).creationDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
    assertThat(result.getHits().get(1).creationDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CREATION_DATE).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getHits().get(0).creationDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));
    assertThat(result.getHits().get(1).creationDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
  }

  @Test
  public void sort_by_update_date() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setIssueUpdateDate(DateUtils.parseDate("2014-09-23")),
      IssueTesting.newDto(rule, file, project).setIssueUpdateDate(DateUtils.parseDate("2014-09-24")));
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(true);
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getHits().get(0).updateDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
    assertThat(result.getHits().get(1).updateDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_UPDATE_DATE).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(2);
    assertThat(result.getHits().get(0).updateDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));
    assertThat(result.getHits().get(1).updateDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
  }

  @Test
  public void sort_by_close_date() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setIssueCloseDate(DateUtils.parseDate("2014-09-23")),
      IssueTesting.newDto(rule, file, project).setIssueCloseDate(DateUtils.parseDate("2014-09-24")),
      IssueTesting.newDto(rule, file, project).setIssueCloseDate(null));
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(true);
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(3);
    assertThat(result.getHits().get(0).closeDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
    assertThat(result.getHits().get(1).closeDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));
    assertThat(result.getHits().get(2).closeDate()).isNull();

    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_CLOSE_DATE).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(3);
    assertThat(result.getHits().get(0).closeDate()).isEqualTo(DateUtils.parseDate("2014-09-24"));
    assertThat(result.getHits().get(1).closeDate()).isEqualTo(DateUtils.parseDate("2014-09-23"));
    assertThat(result.getHits().get(2).closeDate()).isNull();
  }

  @Test
  public void sort_by_file_and_line() throws Exception {
    db.issueDao().insert(session,
      // file Foo.java
      IssueTesting.newDto(rule, file, project).setLine(20).setFilePath("src/Foo.java"),
      IssueTesting.newDto(rule, file, project).setLine(null).setFilePath("src/Foo.java"),
      IssueTesting.newDto(rule, file, project).setLine(25).setFilePath("src/Foo.java"),

      // file Bar.java
      IssueTesting.newDto(rule, file, project).setLine(9).setFilePath("src/Bar.java"),
      IssueTesting.newDto(rule, file, project).setLine(109).setFilePath("src/Bar.java")
      );
    session.commit();

    // ascending sort -> Bar then Foo
    IssueQuery.Builder query = IssueQuery.builder().sort(IssueQuery.SORT_BY_FILE_LINE).asc(true);
    Result<Issue> result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(5);
    assertThat(result.getHits().get(0).line()).isEqualTo(9);
    assertThat(result.getHits().get(1).line()).isEqualTo(109);
    assertThat(result.getHits().get(2).line()).isEqualTo(20);
    assertThat(result.getHits().get(3).line()).isEqualTo(25);
    assertThat(result.getHits().get(4).line()).isEqualTo(null);

    // descending sort -> Foo then Bar
    query = IssueQuery.builder().sort(IssueQuery.SORT_BY_FILE_LINE).asc(false);
    result = index.search(query.build(), new QueryContext());
    assertThat(result.getHits()).hasSize(5);
    assertThat(result.getHits().get(0).line()).isEqualTo(25);
    assertThat(result.getHits().get(1).line()).isEqualTo(20);
    assertThat(result.getHits().get(2).line()).isEqualTo(null);
    assertThat(result.getHits().get(3).line()).isEqualTo(109);
    assertThat(result.getHits().get(4).line()).isEqualTo(9);
  }

  @Test
  public void authorized_issues_on_groups() throws Exception {
    ComponentDto project1 = ComponentTesting.newProjectDto().setKey("project1");
    ComponentDto project2 = ComponentTesting.newProjectDto().setKey("project2");
    ComponentDto project3 = ComponentTesting.newProjectDto().setKey("project3");

    ComponentDto file1 = ComponentTesting.newFileDto(project1).setKey("file1");
    ComponentDto file2 = ComponentTesting.newFileDto(project1).setKey("file2");
    ComponentDto file3 = ComponentTesting.newFileDto(project1).setKey("file3");

    tester.get(ComponentDao.class).insert(session, project1, project2, project3, file1, file2, file3);

    // project1 can be seen by sonar-users
    GroupDto groupDto = new GroupDto().setName("sonar-users");
    db.groupDao().insert(session, groupDto);
    tester.get(PermissionFacade.class).insertGroupPermission(project1.getId(), groupDto.getName(), UserRole.USER, session);

    // project2 can be seen by sonar-admins
    groupDto = new GroupDto().setName("sonar-admins");
    db.groupDao().insert(session, groupDto);
    tester.get(PermissionFacade.class).insertGroupPermission(project2.getId(), groupDto.getName(), UserRole.USER, session);

    // project3 cannot be seen by anyone

    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file1, project1),
      IssueTesting.newDto(rule, file2, project2),
      IssueTesting.newDto(rule, file3, project3));

    session.commit();
    session.clearCache();

    IssueQuery.Builder query = IssueQuery.builder();

    MockUserSession.set().setUserGroups("sonar-users");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setUserGroups("sonar-admins");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setUserGroups("sonar-users", "sonar-admins");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(2);

    MockUserSession.set().setUserGroups("another group");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(0);

    MockUserSession.set().setUserGroups("sonar-users", "sonar-admins");
    assertThat(index.search(query.componentRootUuids(newArrayList(project3.key())).build(), new QueryContext()).getHits()).hasSize(0);
  }

  @Test
  public void authorized_issues_on_user() throws Exception {
    ComponentDto project1 = ComponentTesting.newProjectDto().setKey("project1");
    ComponentDto project2 = ComponentTesting.newProjectDto().setKey("project2");
    ComponentDto project3 = ComponentTesting.newProjectDto().setKey("project3");

    ComponentDto file1 = ComponentTesting.newFileDto(project1).setKey("file1");
    ComponentDto file2 = ComponentTesting.newFileDto(project1).setKey("file2");
    ComponentDto file3 = ComponentTesting.newFileDto(project1).setKey("file3");

    tester.get(ComponentDao.class).insert(session, project1, project2, project3, file1, file2, file3);

    // project1 can be seen by john
    UserDto john = new UserDto().setLogin("john").setName("john").setActive(true);
    db.userDao().insert(session, john);
    tester.get(PermissionFacade.class).insertUserPermission(project1.getId(), john.getId(), UserRole.USER, session);

    // project2 can be seen by max
    UserDto max = new UserDto().setLogin("max").setName("max").setActive(true);
    db.userDao().insert(session, max);
    tester.get(PermissionFacade.class).insertUserPermission(project2.getId(), max.getId(), UserRole.USER, session);

    // project3 cannot be seen by anyone

    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file1, project1),
      IssueTesting.newDto(rule, file2, project2),
      IssueTesting.newDto(rule, file2, project3)
      );

    session.commit();
    session.clearCache();

    IssueQuery.Builder query = IssueQuery.builder();

    MockUserSession.set().setLogin("john");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setLogin("max");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setLogin("another guy");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(0);

    MockUserSession.set().setLogin("john");
    assertThat(index.search(query.componentRootUuids(newArrayList(project3.key())).build(), new QueryContext()).getHits()).hasSize(0);
  }

  @Test
  public void authorized_issues_on_user_and_group() throws Exception {
    ComponentDto project1 = ComponentTesting.newProjectDto().setKey("project1");
    ComponentDto project2 = ComponentTesting.newProjectDto().setKey("project2");
    tester.get(ComponentDao.class).insert(session, project1, project2);

    // project1 can be seen by john
    UserDto john = new UserDto().setLogin("john").setName("john").setActive(true);
    db.userDao().insert(session, john);
    tester.get(PermissionFacade.class).insertUserPermission(project1.getId(), john.getId(), UserRole.USER, session);

    // project1 can be seen by sonar-users
    GroupDto groupDto = new GroupDto().setName("sonar-users");
    db.groupDao().insert(session, groupDto);
    tester.get(PermissionFacade.class).insertGroupPermission(project1.getId(), "sonar-users", UserRole.USER, session);

    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project1),
      IssueTesting.newDto(rule, file, project2));

    session.commit();
    session.clearCache();

    IssueQuery.Builder query = IssueQuery.builder();

    MockUserSession.set().setLogin("john").setUserGroups("sonar-users");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);
  }

  @Test
  public void synchronize_issue() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project);
    tester.get(IssueDao.class).insert(session, issue);
    session.commit();

    // 0 Assert that all issues are both in ES and DB
    assertThat(db.issueDao().findAfterDate(session, new Date(0))).hasSize(1);
    assertThat(index.countAll()).isEqualTo(1);

    // Clear issue index in order to simulate these issues have been inserted without being indexed in E/S (from a previous version of SQ or
    // from batch)
    tester.get(BackendCleanup.class).clearIndex(IndexDefinition.ISSUES);
    tester.clearIndexes();
    assertThat(index.countAll()).isEqualTo(0);

    DbSession newSession = db.openSession(true);
    try {
      db.issueDao().synchronizeAfter(newSession, index.getLastSynchronization());
      newSession.commit();

    } finally {
      newSession.close();
    }

    assertThat(index.countAll()).isEqualTo(1);
  }

  @Test
  public void synchronize_all_issues() throws Exception {
    IssueDto issue = IssueTesting.newDto(rule, file, project);
    tester.get(IssueDao.class).insert(session, issue);
    session.commit();

    // 0 Assert that all issues are both in ES and DB
    assertThat(db.issueDao().findAfterDate(session, new Date(0))).hasSize(1);
    assertThat(index.countAll()).isEqualTo(1);

    // Clear issue index in order to simulate these issues have been inserted without being indexed in E/S (from a previous version of SQ or
    // from batch)
    tester.get(BackendCleanup.class).clearIndex(IndexDefinition.ISSUES);
    tester.clearIndexes();
    assertThat(index.countAll()).isEqualTo(0);

    DbSession newSession = db.openSession(true);
    try {
      db.issueDao().synchronizeAfter(newSession);
      newSession.commit();

    } finally {
      newSession.close();
    }

    assertThat(index.countAll()).isEqualTo(1);
  }

  @Test
  public void synchronize_a_lot_of_issues() throws Exception {
    Integer numberOfIssues = 1000;

    List<String> issueKeys = newArrayList();
    for (int i = 0; i < numberOfIssues; i++) {
      IssueDto issue = IssueTesting.newDto(rule, file, project);
      tester.get(IssueDao.class).insert(session, issue);
      issueKeys.add(issue.getKey());
    }
    session.commit();

    // 0 Assert that all issues are both in ES and DB
    assertThat(db.issueDao().findAfterDate(session, new Date(0))).hasSize(numberOfIssues);
    assertThat(index.countAll()).isEqualTo(numberOfIssues);

    // Clear issue index in order to simulate these issues have been inserted without being indexed in E/S (from a previous version of SQ or
    // from batch)
    tester.get(BackendCleanup.class).clearIndex(IndexDefinition.ISSUES);
    tester.clearIndexes();
    assertThat(index.countAll()).isEqualTo(0);

    DbSession newSession = db.openSession(true);
    try {
      db.issueDao().synchronizeAfter(newSession, index.getLastSynchronization());
      newSession.commit();

    } finally {
      newSession.close();
    }

    assertThat(index.countAll()).isEqualTo(numberOfIssues);
  }

  @Test
  public void list_assignees() throws Exception {
    db.issueDao().insert(session,
      IssueTesting.newDto(rule, file, project).setAssignee("steph").setStatus(Issue.STATUS_OPEN),
      IssueTesting.newDto(rule, file, project).setAssignee("simon").setStatus(Issue.STATUS_OPEN),
      IssueTesting.newDto(rule, file, project).setStatus(Issue.STATUS_OPEN),
      IssueTesting.newDto(rule, file, project).setAssignee("steph").setStatus(Issue.STATUS_OPEN),
      // julien should not be returned as the issue is closed
      IssueTesting.newDto(rule, file, project).setAssignee("julien").setStatus(Issue.STATUS_CLOSED)
      );
    session.commit();

    List<FacetValue> results = index.listAssignees(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_OPEN)).build());

    assertThat(results).hasSize(3);
    assertThat(results.get(0).getKey()).isEqualTo("steph");
    assertThat(results.get(0).getValue()).isEqualTo(2);

    assertThat(results.get(1).getKey()).isEqualTo("simon");
    assertThat(results.get(1).getValue()).isEqualTo(1);

    assertThat(results.get(2).getKey()).isEqualTo("_notAssigned_");
    assertThat(results.get(2).getValue()).isEqualTo(1);
  }

  @Test
  public void index_has_4_shards() {

    // 0 Assert configuration is correct
    String shardSettingKey = "index.number_of_shards";
    Settings settings = index.getIndexSettings();
    assertThat(settings.get(shardSettingKey)).isEqualTo("4");

    // 1 Assert index has 4 shards
    assertThat(tester.get(SearchClient.class).admin().indices().prepareGetSettings(IndexDefinition.ISSUES.getIndexName())
      .get().getSetting(IndexDefinition.ISSUES.getIndexName(), shardSettingKey)).isEqualTo("4");
  }
}
