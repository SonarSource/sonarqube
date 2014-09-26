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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
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
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.IssueTesting;
import org.sonar.server.issue.db.IssueDao;
import org.sonar.server.rule.RuleTesting;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class IssueIndexMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester()
    .setProperty("sonar.issues.use_es_backend", "true");

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

    project = new ComponentDto()
      .setId(1L)
      .setKey("MyProject")
      .setProjectId_unit_test_only(1L);
    tester.get(ComponentDao.class).insert(session, project);

    file = new ComponentDto()
      .setProjectId_unit_test_only(1L)
      .setKey("MyComponent")
      .setId(2L);
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
    IssueDto issue = createIssue();
    db.issueDao().insert(session, issue);
    session.commit();

    Issue result = index.getByKey(issue.getKey());
    IssueTesting.assertIsEquivalent(issue, result);
  }

  @Test
  public void get_by_key_with_attributes() throws Exception {
    IssueDto issue = createIssue();
    db.issueDao().insert(session, issue).setIssueAttributes(KeyValueFormat.format(ImmutableMap.of("jira-issue-key", "SONAR-1234")));
    session.commit();

    Issue result = index.getByKey(issue.getKey());
    IssueTesting.assertIsEquivalent(issue, result);

    assertThat(result.attribute("jira-issue-key")).isEqualTo("SONAR-1234");
  }

  @Test(expected = IllegalStateException.class)
  public void comments_field_is_not_available() throws Exception {
    IssueDto issue = createIssue();
    db.issueDao().insert(session, issue);
    session.commit();

    Issue result = index.getByKey(issue.getKey());
    result.comments();
  }

  @Test(expected = IllegalStateException.class)
  public void is_new_field_is_not_available() throws Exception {
    IssueDto issue = createIssue();
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
      createIssue().setKee("1"),
      createIssue().setKee("2"));
    session.commit();

    assertThat(index.search(IssueQuery.builder().issueKeys(newArrayList("1", "2")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().issueKeys(newArrayList("1")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().issueKeys(newArrayList("3", "4")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_projects() throws Exception {
    db.issueDao().insert(session,
      createIssue().setRootComponentKey(project.key()));
    session.commit();

    assertThat(index.search(IssueQuery.builder().componentRoots(newArrayList(project.key())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().componentRoots(newArrayList(file.key())).build(), new QueryContext()).getHits()).isEmpty();
    assertThat(index.search(IssueQuery.builder().componentRoots(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_components() throws Exception {
    db.issueDao().insert(session,
      createIssue().setComponentKey("file1"),
      createIssue().setComponentKey("file2"));
    session.commit();

    assertThat(index.search(IssueQuery.builder().components(newArrayList("file1", "file2")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().components(newArrayList("file1")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().components(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
    assertThat(index.search(IssueQuery.builder().components(newArrayList(project.key())).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_severities() throws Exception {
    db.issueDao().insert(session,
      createIssue().setSeverity(Severity.INFO),
      createIssue().setSeverity(Severity.MAJOR));
    session.commit();

    assertThat(index.search(IssueQuery.builder().severities(newArrayList(Severity.INFO, Severity.MAJOR)).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().severities(newArrayList(Severity.INFO)).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().severities(newArrayList(Severity.BLOCKER)).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_statuses() throws Exception {
    db.issueDao().insert(session,
      createIssue().setStatus(Issue.STATUS_CLOSED),
      createIssue().setStatus(Issue.STATUS_OPEN));
    session.commit();

    assertThat(index.search(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_CLOSED, Issue.STATUS_OPEN)).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_CLOSED)).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().statuses(newArrayList(Issue.STATUS_CONFIRMED)).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_resolutions() throws Exception {
    db.issueDao().insert(session,
      createIssue().setResolution(Issue.RESOLUTION_FALSE_POSITIVE),
      createIssue().setResolution(Issue.RESOLUTION_FIXED));
    session.commit();

    assertThat(index.search(IssueQuery.builder().resolutions(newArrayList(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_FIXED)).build(), new QueryContext()).getHits())
      .hasSize(2);
    assertThat(index.search(IssueQuery.builder().resolutions(newArrayList(Issue.RESOLUTION_FALSE_POSITIVE)).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().resolutions(newArrayList(Issue.RESOLUTION_REMOVED)).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_resolved() throws Exception {
    db.issueDao().insert(session,
      createIssue().setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED),
      createIssue().setStatus(Issue.STATUS_OPEN).setResolution(null),
      createIssue().setStatus(Issue.STATUS_OPEN).setResolution(null));
    session.commit();

    assertThat(index.search(IssueQuery.builder().resolved(true).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().resolved(false).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().resolved(null).build(), new QueryContext()).getHits()).hasSize(3);
  }

  @Test
  public void filter_by_action_plans() throws Exception {
    db.issueDao().insert(session,
      createIssue().setActionPlanKey("plan1"),
      createIssue().setActionPlanKey("plan2"));
    session.commit();

    assertThat(index.search(IssueQuery.builder().actionPlans(newArrayList("plan1")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().actionPlans(newArrayList("plan1", "plan2")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().actionPlans(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_planned() throws Exception {
    db.issueDao().insert(session,
      createIssue().setActionPlanKey("AP-KEY"),
      createIssue().setActionPlanKey(null),
      createIssue().setActionPlanKey(null));
    session.commit();

    assertThat(index.search(IssueQuery.builder().planned(true).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().planned(false).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().planned(null).build(), new QueryContext()).getHits()).hasSize(3);
  }

  @Test
  public void filter_by_rules() throws Exception {
    db.issueDao().insert(session, createIssue().setRule(rule));

    tester.get(RuleDao.class).insert(session, RuleTesting.newDto(RuleKey.of("rule", "without issue")));
    session.commit();

    assertThat(index.search(IssueQuery.builder().rules(newArrayList(rule.getKey())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().rules(newArrayList(RuleKey.of("rule", "without issue"))).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_languages() throws Exception {
    db.issueDao().insert(session, createIssue().setRule(rule));
    session.commit();

    assertThat(index.search(IssueQuery.builder().languages(newArrayList(rule.getLanguage())).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().languages(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_assignees() throws Exception {
    db.issueDao().insert(session,
      createIssue().setAssignee("steph"),
      createIssue().setAssignee("simon"),
      createIssue());
    session.commit();

    assertThat(index.search(IssueQuery.builder().assignees(newArrayList("steph")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().assignees(newArrayList("steph", "simon")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().assignees(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_assigned() throws Exception {
    db.issueDao().insert(session,
      createIssue().setAssignee("steph"),
      createIssue().setAssignee(null),
      createIssue().setAssignee(null));
    session.commit();

    assertThat(index.search(IssueQuery.builder().assigned(true).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().assigned(false).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().assigned(null).build(), new QueryContext()).getHits()).hasSize(3);
  }

  @Test
  public void filter_by_reporters() throws Exception {
    db.issueDao().insert(session,
      createIssue().setReporter("fabrice"),
      createIssue().setReporter("stephane"));
    session.commit();

    assertThat(index.search(IssueQuery.builder().reporters(newArrayList("fabrice", "stephane")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().reporters(newArrayList("fabrice")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().reporters(newArrayList("unknown")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_created_after() throws Exception {
    IssueDto issue1 = createIssue().setIssueCreationDate(DateUtils.parseDate("2014-09-20"));
    IssueDto issue2 = createIssue().setIssueCreationDate(DateUtils.parseDate("2014-09-23"));
    db.issueDao().insert(session, issue1, issue2);
    session.commit();

    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-19")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-20")).build(), new QueryContext()).getHits()).hasSize(2);
    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-21")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdAfter(DateUtils.parseDate("2014-09-25")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void filter_by_created_before() throws Exception {
    IssueDto issue1 = createIssue().setIssueCreationDate(DateUtils.parseDate("2014-09-20"));
    IssueDto issue2 = createIssue().setIssueCreationDate(DateUtils.parseDate("2014-09-23"));
    db.issueDao().insert(session, issue1, issue2);
    session.commit();

    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-19")).build(), new QueryContext()).getHits()).isEmpty();
    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-20")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-21")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdBefore(DateUtils.parseDate("2014-09-25")).build(), new QueryContext()).getHits()).hasSize(2);
  }

  @Test
  public void filter_by_created_at() throws Exception {
    IssueDto issue = createIssue().setIssueCreationDate(DateUtils.parseDate("2014-09-20"));
    db.issueDao().insert(session, issue);
    session.commit();

    assertThat(index.search(IssueQuery.builder().createdAt(DateUtils.parseDate("2014-09-20")).build(), new QueryContext()).getHits()).hasSize(1);
    assertThat(index.search(IssueQuery.builder().createdAt(DateUtils.parseDate("2014-09-21")).build(), new QueryContext()).getHits()).isEmpty();
  }

  @Test
  public void paging() throws Exception {
    for (int i = 0; i < 12; i++) {
      IssueDto issue = createIssue();
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
      IssueDto issue = createIssue();
      tester.get(IssueDao.class).insert(session, issue);
    }
    session.commit();

  }

  @Test
  public void search_with_max_limit() throws Exception {
    List<String> issueKeys = newArrayList();
    for (int i = 0; i < 500; i++) {
      IssueDto issue = createIssue();
      tester.get(IssueDao.class).insert(session, issue);
      issueKeys.add(issue.getKey());
    }
    session.commit();

    IssueQuery.Builder query = IssueQuery.builder();
    Result<Issue> result = index.search(query.build(), new QueryContext().setMaxLimit());
    assertThat(result.getHits()).hasSize(500);
  }

  @Test
  public void sort_by_assignee() throws Exception {
    IssueDto issue1 = createIssue().setAssignee("steph");
    IssueDto issue2 = createIssue().setAssignee("simon");
    db.issueDao().insert(session, issue1, issue2);
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
    IssueDto issue1 = createIssue().setIssueCreationDate(DateUtils.parseDate("2014-09-23"));
    IssueDto issue2 = createIssue().setIssueCreationDate(DateUtils.parseDate("2014-09-24"));
    db.issueDao().insert(session, issue1, issue2);
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
    IssueDto issue1 = createIssue().setIssueUpdateDate(DateUtils.parseDate("2014-09-23"));
    IssueDto issue2 = createIssue().setIssueUpdateDate(DateUtils.parseDate("2014-09-24"));
    db.issueDao().insert(session, issue1, issue2);
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
    IssueDto issue1 = createIssue().setIssueCloseDate(DateUtils.parseDate("2014-09-23"));
    IssueDto issue2 = createIssue().setIssueCloseDate(DateUtils.parseDate("2014-09-24"));
    IssueDto issue3 = createIssue().setIssueCloseDate(null);
    db.issueDao().insert(session, issue1, issue2, issue3);
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
  public void authorized_issues_on_groups() throws Exception {
    ComponentDto project1 = new ComponentDto()
      .setId(10L)
      .setKey("project1");
    ComponentDto project2 = new ComponentDto()
      .setId(11L)
      .setKey("project2");
    tester.get(ComponentDao.class).insert(session, project1, project2);

    // project1 can be seen by sonar-users
    GroupDto groupDto = new GroupDto().setName("sonar-users");
    db.groupDao().insert(session, groupDto);
    tester.get(PermissionFacade.class).insertGroupPermission(project1.getId(), groupDto.getName(), UserRole.USER, session);

    // project2 can be seen by sonar-admins
    groupDto = new GroupDto().setName("sonar-admins");
    db.groupDao().insert(session, groupDto);
    tester.get(PermissionFacade.class).insertGroupPermission(project2.getId(), groupDto.getName(), UserRole.USER, session);

    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    IssueDto issue1 = createIssue().setRootComponent(project1);
    IssueDto issue2 = createIssue().setRootComponent(project2);
    db.issueDao().insert(session, issue1, issue2);

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
  }

  @Test
  public void authorized_issues_on_user() throws Exception {
    ComponentDto project1 = new ComponentDto()
      .setId(10L)
      .setKey("project1");
    ComponentDto project2 = new ComponentDto()
      .setId(11L)
      .setKey("project2");
    tester.get(ComponentDao.class).insert(session, project1, project2);

    // project1 can be seen by john
    UserDto john = new UserDto().setLogin("john").setName("john").setActive(true);
    db.userDao().insert(session, john);
    tester.get(PermissionFacade.class).insertUserPermission(project1.getId(), john.getId(), UserRole.USER, session);

    // project2 can be seen by max
    UserDto max = new UserDto().setLogin("max").setName("max").setActive(true);
    db.userDao().insert(session, max);
    tester.get(PermissionFacade.class).insertUserPermission(project2.getId(), max.getId(), UserRole.USER, session);

    db.issueAuthorizationDao().synchronizeAfter(session, new Date(0));

    IssueDto issue1 = createIssue().setRootComponent(project1);
    IssueDto issue2 = createIssue().setRootComponent(project2);
    db.issueDao().insert(session, issue1, issue2);

    session.commit();
    session.clearCache();

    IssueQuery.Builder query = IssueQuery.builder();

    MockUserSession.set().setLogin("john");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setLogin("max");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);

    MockUserSession.set().setLogin("another guy");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(0);
  }

  @Test
  public void authorized_issues_on_user_with_group() throws Exception {
    ComponentDto project1 = new ComponentDto()
      .setId(10L)
      .setKey("project1");
    ComponentDto project2 = new ComponentDto()
      .setId(11L)
      .setKey("project2");
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

    IssueDto issue1 = createIssue().setRootComponent(project1);
    IssueDto issue2 = createIssue().setRootComponent(project2);
    db.issueDao().insert(session, issue1, issue2);

    session.commit();
    session.clearCache();

    IssueQuery.Builder query = IssueQuery.builder();

    MockUserSession.set().setLogin("john").setUserGroups("sonar-users");
    assertThat(index.search(query.build(), new QueryContext()).getHits()).hasSize(1);
  }

  private IssueDto createIssue() {
    return new IssueDto()
      .setIssueCreationDate(DateUtils.parseDate("2014-09-04"))
      .setIssueUpdateDate(DateUtils.parseDate("2014-12-04"))
      .setRule(rule)
      .setDebt(10L)
      .setRootComponent(project)
      .setComponent(file)
      .setStatus("OPEN").setResolution("OPEN")
      .setSeverity("MAJOR")
      .setKee(UUID.randomUUID().toString());
  }
}
