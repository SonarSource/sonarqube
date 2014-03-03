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
import org.sonar.api.i18n.I18n;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.action.Action;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.server.ws.WsTester;
import org.sonar.api.technicaldebt.server.Characteristic;
import org.sonar.api.technicaldebt.server.internal.DefaultCharacteristic;
import org.sonar.api.user.User;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.DefaultIssueQueryResult;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.technicaldebt.DefaultTechnicalDebtManager;
import org.sonar.core.user.DefaultUser;
import org.sonar.server.issue.ActionService;
import org.sonar.server.issue.IssueChangelog;
import org.sonar.server.issue.IssueChangelogService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.util.ArrayList;
import java.util.Date;
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
  DefaultTechnicalDebtManager technicalDebtManager;

  @Mock
  I18n i18n;

  List<Issue> issues;
  DefaultIssueQueryResult result;

  private Date issue_creation_date;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    issues = new ArrayList<Issue>();
    result = new DefaultIssueQueryResult(issues);
    result.addRules(newArrayList(Rule.create("squid", "AvoidCycle").setName("Avoid cycle")));
    when(issueFinder.find(any(IssueQuery.class))).thenReturn(result);

    when(issueChangelogService.changelog(any(Issue.class))).thenReturn(mock(IssueChangelog.class));

    issue_creation_date = DateUtils.parseDateTime("2014-01-22T19:10:03+0100");
    when(i18n.formatDateTime(any(Locale.class), eq(issue_creation_date))).thenReturn("Jan 22, 2014 10:03 AM");

    when(i18n.message(any(Locale.class), eq("created"), eq((String) null))).thenReturn("Created");

    tester = new WsTester(new IssuesWs(new IssueShowWsHandler(issueFinder, issueService, issueChangelogService, actionService, technicalDebtManager, i18n)));
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
      .setCreationDate(issue_creation_date);
    issues.add(issue);

    result.addComponents(Lists.<Component>newArrayList(new ComponentDto()
      .setId(10L)
      .setKey("org.sonar.server.issue.IssueClient")
      .setLongName("SonarQube :: Issue Client")
      .setQualifier("FIL")
      .setGroupId(1L)
      .setRootId(1L)
    ));

    result.addComponents(Lists.<Component>newArrayList(new ComponentDto()
      .setId(1L)
      .setKey("org.sonar.Sonar")
      .setLongName("SonarQube")
      .setRootId(1L)
    ));

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issueKey);
    request.execute().assertJson(getClass(), "show_issue.json");
  }

  @Test
  public void show_issue_with_module() throws Exception {
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
      .setCreationDate(issue_creation_date);
    issues.add(issue);

    // File
    result.addComponents(Lists.<Component>newArrayList(new ComponentDto()
      .setId(10L)
      .setKey("org.sonar.server.issue.IssueClient")
      .setLongName("SonarQube :: Issue Client")
      .setQualifier("FIL")
      .setGroupId(2L)
      .setRootId(1L)));

    // Module
    result.addComponents(Lists.<Component>newArrayList(new ComponentDto()
      .setId(2L)
      .setKey("org.sonar.server.Server")
      .setLongName("SonarQube :: Server")
      .setQualifier("BRC")
      .setGroupId(1L)
      .setRootId(1L)));

    // Project
    result.addComponents(Lists.<Component>newArrayList(new ComponentDto()
      .setId(1L)
      .setKey("org.sonar.Sonar")
      .setLongName("SonarQube")));

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issueKey);
    request.execute().assertJson(getClass(), "show_issue_with_module.json");
  }

  @Test
  public void show_issue_on_removed_component() throws Exception {
    String issueKey = "ABCD";
    Issue issue = createIssue();
    issues.add(issue);

    Component project = mock(Component.class);
    when(project.key()).thenReturn("org.sonar.Sonar");
    when(project.longName()).thenReturn("SonarQube");
    result.addProjects(newArrayList(project));

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issueKey);
    request.execute().assertJson(getClass(), "show_issue_on_removed_component.json");
  }

  @Test
  public void show_issue_on_removed_project_and_component() throws Exception {
    String issueKey = "ABCD";
    Issue issue = createIssue();
    issues.add(issue);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issueKey);
    request.execute().assertJson(getClass(), "show_issue_on_removed_project_and_component.json");
  }

  @Test
  public void show_issue_with_action_plan() throws Exception {
    Issue issue = createStandardIssue()
      .setActionPlanKey("AP-ABCD");
    issues.add(issue);

    result.addActionPlans(newArrayList((ActionPlan) new DefaultActionPlan().setKey("AP-ABCD").setName("Version 4.2")));

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_action_plan.json");
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
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_users.json");
  }

  @Test
  public void show_issue_with_technical_debt() throws Exception {
    Long technicalDebt = 7260L;
    Issue issue = createStandardIssue().setDebt(technicalDebt);
    issues.add(issue);

    when(i18n.formatWorkDuration(any(Locale.class), eq(technicalDebt))).thenReturn("2 hours 1 minutes");

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_technical_debt.json");
  }

  @Test
  public void show_issue_with_characteristics() throws Exception {
    Issue issue = createStandardIssue().setDebt(7260L);
    issues.add(issue);

    Characteristic requirement = new DefaultCharacteristic().setId(5).setParentId(2).setRootId(1);
    Characteristic characteristic = new DefaultCharacteristic().setId(1).setName("Maintainability");
    Characteristic subCharacteristic = new DefaultCharacteristic().setId(2).setName("Readability");
    when(technicalDebtManager.findRequirementByRule(result.rule(issue))).thenReturn(requirement);
    when(technicalDebtManager.findCharacteristicById(1)).thenReturn(characteristic);
    when(technicalDebtManager.findCharacteristicById(2)).thenReturn(subCharacteristic);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_characteristics.json");
  }

  @Test
  public void show_issue_with_dates() throws Exception {
    Date creationDate = DateUtils.parseDateTime("2014-01-22T19:10:03+0100");
    Date updateDate = DateUtils.parseDateTime("2014-01-23T19:10:03+0100");
    Date closedDate = DateUtils.parseDateTime("2014-01-24T19:10:03+0100");

    Issue issue = createStandardIssue()
      .setCreationDate(creationDate)
      .setUpdateDate(updateDate)
      .setCloseDate(closedDate);
    issues.add(issue);

    when(i18n.formatDateTime(any(Locale.class), eq(creationDate))).thenReturn("Jan 22, 2014 10:03 AM");
    when(i18n.formatDateTime(any(Locale.class), eq(updateDate))).thenReturn("Jan 23, 2014 10:03 AM");
    when(i18n.ageFromNow(any(Locale.class), eq(updateDate))).thenReturn("9 days");
    when(i18n.formatDateTime(any(Locale.class), eq(closedDate))).thenReturn("Jan 24, 2014 10:03 AM");

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_dates.json");
  }

  @Test
  public void show_issue_with_comments() throws Exception {
    Date date1 = DateUtils.parseDateTime("2014-02-22T19:10:03+0100");
    Date date2 = DateUtils.parseDateTime("2014-02-23T19:10:03+0100");

    Issue issue = createStandardIssue()
      .addComment(
        new DefaultIssueComment()
          .setKey("COMMENT-ABCD")
          .setMarkdownText("*My comment*")
          .setUserLogin("john")
          .setCreatedAt(date1))
      .addComment(
        new DefaultIssueComment()
          .setKey("COMMENT-ABCE")
          .setMarkdownText("Another comment")
          .setUserLogin("arthur")
          .setCreatedAt(date2)
      );
    issues.add(issue);
    result.addUsers(Lists.<User>newArrayList(
      new DefaultUser().setLogin("john").setName("John"),
      new DefaultUser().setLogin("arthur").setName("Arthur")
    ));

    when(i18n.ageFromNow(any(Locale.class), eq(date1))).thenReturn("9 days");
    when(i18n.ageFromNow(any(Locale.class), eq(date2))).thenReturn("10 days");

    MockUserSession.set().setLogin("arthur");
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_comments.json");
  }

  @Test
  public void show_issue_with_transitions() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setStatus("RESOLVED")
      .setResolution("FIXED");
    issues.add(issue);

    when(issueService.listTransitions(eq(issue), any(UserSession.class))).thenReturn(newArrayList(Transition.create("reopen", "RESOLVED", "REOPEN")));

    MockUserSession.set().setLogin("john");
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_transitions.json");
  }

  @Test
  public void show_issue_with_actions() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setStatus("OPEN");
    issues.add(issue);

    MockUserSession.set().setLogin("john");
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_actions.json");
  }

  @Test
  public void show_issue_with_set_severity_action() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setStatus("OPEN");
    issues.add(issue);

    MockUserSession.set().setLogin("john").addProjectPermissions(UserRole.ISSUE_ADMIN, issue.projectKey());
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_set_severity_action.json");
  }

  @Test
  public void show_issue_with_assign_to_me_action() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setStatus("OPEN");
    issues.add(issue);

    MockUserSession.set().setLogin("john");
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_assign_to_me_action.json");
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
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_without_assign_to_me_action.json");
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
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_actions_defined_by_plugins.json");
  }

  @Test
  public void show_issue_with_changelog() throws Exception {
    Issue issue = createStandardIssue();
    issues.add(issue);

    Date date1 = DateUtils.parseDateTime("2014-02-22T19:10:03+0100");
    Date date2 = DateUtils.parseDateTime("2014-02-23T19:10:03+0100");

    List<User> users = Lists.<User>newArrayList(new DefaultUser().setLogin("john").setName("John"));
    FieldDiffs userChange = new FieldDiffs()
      .setUserLogin("john")
      .setDiff("actionPlan", null, "1.0")
      .setCreationDate(date1);
    FieldDiffs scanChange = new FieldDiffs()
      .setDiff("severity", "INFO", "BLOCKER")
      .setDiff("status", "REOPEN", "RESOLVED")
      .setCreationDate(date2);
    when(issueChangelogService.changelog(issue)).thenReturn(new IssueChangelog(newArrayList(userChange, scanChange), users));
    when(issueChangelogService.formatDiffs(userChange)).thenReturn(newArrayList("Action plan updated to 1.0"));
    when(issueChangelogService.formatDiffs(scanChange)).thenReturn(newArrayList("Severity updated from Info to Blocker", "Status updated from Reopen to Resolved"));

    when(i18n.formatDateTime(any(Locale.class), eq(date1))).thenReturn("Fev 22, 2014 10:03 AM");
    when(i18n.formatDateTime(any(Locale.class), eq(date2))).thenReturn("Fev 23, 2014 10:03 AM");

    MockUserSession.set();
    WsTester.TestRequest request = tester.newRequest("show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_changelog.json");
  }

  private DefaultIssue createStandardIssue() {
    DefaultIssue issue = createIssue();
    addComponentAndProject();
    return issue;
  }

  private DefaultIssue createIssue() {
    return new DefaultIssue()
      .setKey("ABCD")
      .setComponentKey("org.sonar.server.issue.IssueClient")
      .setProjectKey("org.sonar.Sonar")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setCreationDate(issue_creation_date);
  }

  private void addComponentAndProject() {
    result.addComponents(Lists.<Component>newArrayList(new ComponentDto()
      .setId(10L)
      .setKey("org.sonar.server.issue.IssueClient")
      .setLongName("SonarQube :: Issue Client")
      .setQualifier("FIL")
      .setGroupId(1L)
      .setRootId(1L)
    ));

    result.addComponents(Lists.<Component>newArrayList(new ComponentDto()
      .setId(1L)
      .setKey("org.sonar.Sonar")
      .setLongName("SonarQube")
      .setRootId(1L)
    ));
  }

}
