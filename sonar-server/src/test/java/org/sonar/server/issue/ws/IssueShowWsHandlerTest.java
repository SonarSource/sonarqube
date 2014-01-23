/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.issue.ws;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.component.Component;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.action.Action;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.issue.internal.WorkDayDuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.server.ws.SimpleRequest;
import org.sonar.api.user.User;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.DefaultIssueQueryResult;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.user.DefaultUser;
import org.sonar.server.issue.ActionService;
import org.sonar.server.issue.IssueChangelog;
import org.sonar.server.issue.IssueChangelogService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.technicaldebt.TechnicalDebtFormatter;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsTester;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IssueShowWsHandlerTest {

  @Mock
  IssueFinder issueFinder;

  @Mock
  IssueService issueService;

  @Mock
  IssueChangelogService issueChangelogService;

  @Mock
  ActionService actionService;

  @Mock
  TechnicalDebtFormatter technicalDebtFormatter;

  List<Issue> issues;
  DefaultIssueQueryResult result;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    issues = new ArrayList<Issue>();
    result = new DefaultIssueQueryResult(issues);
    Component component = mock(Component.class);
    when(component.key()).thenReturn("org.sonar.Sonar");
    result.addProjects(newArrayList(component));
    result.addRules(newArrayList(Rule.create("squid", "AvoidCycle").setName("Avoid cycle")));
    when(issueFinder.find(any(IssueQuery.class))).thenReturn(result);

    when(issueChangelogService.changelog(any(Issue.class))).thenReturn(mock(IssueChangelog.class));

    tester = new WsTester(new IssuesWs(new IssueShowWsHandler(issueFinder, issueService, issueChangelogService, actionService, technicalDebtFormatter)));
  }

  @Test
  public void show_issue() throws Exception {
    String issueKey = "ABCD";
    Issue issue = new DefaultIssue()
      .setKey(issueKey)
      .setComponentKey("org.sonar.server.issue.IssueClient")
      .setProjectKey("org.sonar.Sonar")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setLine(12)
      .setEffortToFix(2.0)
      .setMessage("Fix it")
      .setResolution("FIXED")
      .setStatus("CLOSED")
      .setSeverity("MAJOR")
      .setCreationDate(DateUtils.parseDateTime("2014-01-22T19:10:03+0100"));
    issues.add(issue);

    MockUserSession.set();
    SimpleRequest request = new SimpleRequest().setParam("key", issueKey);
    tester.execute("show", request).assertJson(getClass(), "show_issue.json");
  }

  @Test
  public void show_issue_with_action_plan() throws Exception {
    Issue issue = createStandardIssue()
      .setActionPlanKey("AP-ABCD");
    issues.add(issue);

    result.addActionPlans(newArrayList((ActionPlan) new DefaultActionPlan().setKey("AP-ABCD").setName("Version 4.2")));

    MockUserSession.set();
    SimpleRequest request = new SimpleRequest().setParam("key", issue.key());
    tester.execute("show", request).assertJson(getClass(), "show_issue_with_action_plan.json");
  }

  @Test
  public void show_issue_with_users() throws Exception {
    Issue issue = createStandardIssue()
      .setAssignee("john")
      .setReporter("steven")
      .setAuthorLogin("Henry");
    issues.add(issue);

    result.addUsers(Lists.<User>newArrayList(
      new DefaultUser().setLogin("john").setName("John"),
      new DefaultUser().setLogin("steven").setName("Steven")
    ));

    MockUserSession.set();
    SimpleRequest request = new SimpleRequest().setParam("key", issue.key());
    tester.execute("show", request).assertJson(getClass(), "show_issue_with_users.json");
  }

  @Test
  public void show_issue_with_technical_debt() throws Exception {
    WorkDayDuration technicalDebt = WorkDayDuration.of(1, 2, 0);
    Issue issue = createStandardIssue()
      .setTechnicalDebt(technicalDebt);
    issues.add(issue);

    when(technicalDebtFormatter.format(any(Locale.class), eq(technicalDebt))).thenReturn("2 hours 1 minutes");

    MockUserSession.set();
    SimpleRequest request = new SimpleRequest().setParam("key", issue.key());
    tester.execute("show", request).assertJson(getClass(), "show_issue_with_technical_debt.json");
  }

  @Test
  public void show_issue_with_dates() throws Exception {
    Issue issue = createStandardIssue()
      .setCreationDate(DateUtils.parseDateTime("2014-01-22T19:10:03+0100"))
      .setUpdateDate(DateUtils.parseDateTime("2014-01-23T19:10:03+0100"))
      .setCloseDate(DateUtils.parseDateTime("2014-01-24T19:10:03+0100"));
    issues.add(issue);

    MockUserSession.set();
    SimpleRequest request = new SimpleRequest().setParam("key", issue.key());
    tester.execute("show", request).assertJson(getClass(), "show_issue_with_dates.json");
  }

  @Test
  public void show_issue_with_comments() throws Exception {
    Issue issue = createStandardIssue()
      .addComment(
        new DefaultIssueComment()
          .setKey("COMMENT-ABCD")
          .setMarkdownText("*My comment*")
          .setUserLogin("john")
          .setCreatedAt(DateUtils.parseDateTime("2014-01-23T19:10:03+0100")))
      .addComment(
        new DefaultIssueComment()
          .setKey("COMMENT-ABCE")
          .setMarkdownText("Another comment")
          .setUserLogin("arthur")
          .setCreatedAt(DateUtils.parseDateTime("2014-01-23T19:10:03+0100"))
      );
    issues.add(issue);
    result.addUsers(Lists.<User>newArrayList(
      new DefaultUser().setLogin("john").setName("John"),
      new DefaultUser().setLogin("arthur").setName("Arthur")
    ));

    MockUserSession.set().setLogin("arthur");
    SimpleRequest request = new SimpleRequest().setParam("key", issue.key());
    tester.execute("show", request).assertJson(getClass(), "show_issue_with_comments.json");
  }

  @Test
  public void show_issue_with_transitions() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setStatus("RESOLVED")
      .setResolution("FIXED");
    issues.add(issue);

    when(issueService.listTransitions(eq(issue), any(UserSession.class))).thenReturn(newArrayList(Transition.create("reopen", "RESOLVED", "REOPEN")));

    MockUserSession.set().setLogin("john");
    SimpleRequest request = new SimpleRequest().setParam("key", issue.key());
    tester.execute("show", request).assertJson(getClass(), "show_issue_with_transitions.json");
  }

  @Test
  public void show_issue_with_actions() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setStatus("OPEN");
    issues.add(issue);

    MockUserSession.set().setLogin("john");
    SimpleRequest request = new SimpleRequest().setParam("key", issue.key());
    tester.execute("show", request).assertJson(getClass(), "show_issue_with_actions.json");
  }

  @Test
  public void show_issue_with_severity_action() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setStatus("OPEN");
    issues.add(issue);

    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.ISSUE_ADMIN, issue.projectKey());
    SimpleRequest request = new SimpleRequest().setParam("key", issue.key());
    tester.execute("show", request).assertJson(getClass(), "show_issue_with_severity_action.json");
  }

  @Test
  public void show_issue_with_assign_to_me_action() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setStatus("OPEN");
    issues.add(issue);

    MockUserSession.set().setLogin("john");
    SimpleRequest request = new SimpleRequest().setParam("key", issue.key());
    tester.execute("show", request).assertJson(getClass(), "show_issue_with_assign_to_me_action.json");
  }

  @Test
  public void show_issue_without_assign_to_me_action() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setStatus("OPEN")
      .setAssignee("john");
    issues.add(issue);

    result.addUsers(Lists.<User>newArrayList(
      new DefaultUser().setLogin("john").setName("John")
    ));

    MockUserSession.set().setLogin("john");
    SimpleRequest request = new SimpleRequest().setParam("key", issue.key());
    tester.execute("show", request).assertJson(getClass(), "show_issue_without_assign_to_me_action.json");
  }

  @Test
  public void show_issue_with_actions_defined_by_plugins() throws Exception {
    Issue issue = createStandardIssue()
      .setStatus("OPEN");
    issues.add(issue);

    Action action = mock(Action.class);
    when(action.key()).thenReturn("link-to-jira");
    when(actionService.listAvailableActions(issue)).thenReturn(newArrayList(action));

    MockUserSession.set().setLogin("john");
    SimpleRequest request = new SimpleRequest().setParam("key", issue.key());
    tester.execute("show", request).assertJson(getClass(), "show_issue_with_actions_defined_by_plugins.json");
  }

  @Test
  public void show_issue_with_changelog() throws Exception {
    Issue issue = createStandardIssue();
    issues.add(issue);

    List<User> users = Lists.<User>newArrayList(
      new DefaultUser().setLogin("john").setName("John")
    );

    FieldDiffs userChange = new FieldDiffs()
      .setUserLogin("john")
      .setDiff("actionPlan", null, "1.0")
      .setCreationDate(DateUtils.parseDateTime("2014-01-22T19:10:03+0100"));
    FieldDiffs scanChange = new FieldDiffs()
      .setDiff("severity", "INFO", "BLOCKER")
      .setDiff("status", "REOPEN", "RESOLVED")
      .setCreationDate(DateUtils.parseDateTime("2014-01-23T19:10:03+0100"));
    when(issueChangelogService.changelog(issue)).thenReturn(new IssueChangelog(newArrayList(userChange, scanChange), users));
    when(issueChangelogService.formatDiffs(userChange)).thenReturn(newArrayList("Action plan updated to 1.0"));
    when(issueChangelogService.formatDiffs(scanChange)).thenReturn(newArrayList("Severity updated from Info to Blocker", "Status updated from Reopen to Resolved"));

    MockUserSession.set();
    SimpleRequest request = new SimpleRequest().setParam("key", issue.key());
    tester.execute("show", request).assertJson(getClass(), "show_issue_with_changelog.json");
  }

  private DefaultIssue createStandardIssue() {
    return new DefaultIssue()
      .setKey("ABCD")
      .setComponentKey("org.sonar.server.issue.IssueClient")
      .setProjectKey("org.sonar.Sonar")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setCreationDate(DateUtils.parseDateTime("2014-01-22T19:10:03+0100"));
  }
}
