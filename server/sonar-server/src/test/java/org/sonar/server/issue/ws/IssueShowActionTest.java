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

package org.sonar.server.issue.ws;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.i18n.I18n;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.FieldDiffs;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.debt.internal.DefaultDebtCharacteristic;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.Durations;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.issue.DefaultActionPlan;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.user.DefaultUser;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.debt.DebtModelService;
import org.sonar.server.issue.ActionService;
import org.sonar.server.issue.IssueChangelog;
import org.sonar.server.issue.IssueChangelogService;
import org.sonar.server.issue.IssueCommentService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.issue.actionplan.ActionPlanService;
import org.sonar.server.rule.Rule;
import org.sonar.server.rule.RuleService;
import org.sonar.server.source.SourceService;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsTester;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * TODO Should be replaced by medium tests, as there are too many dependencies
 */
@RunWith(MockitoJUnitRunner.class)
public class IssueShowActionTest {

  @Mock
  DbClient dbClient;

  @Mock
  DbSession session;

  @Mock
  ComponentDao componentDao;

  @Mock
  IssueService issueService;

  @Mock
  IssueChangelogService issueChangelogService;

  @Mock
  IssueCommentService commentService;

  @Mock
  ActionPlanService actionPlanService;

  @Mock
  ActionService actionService;

  @Mock
  UserFinder userFinder;

  @Mock
  DebtModelService debtModel;

  @Mock
  RuleService ruleService;

  @Mock
  I18n i18n;

  @Mock
  Durations durations;

  @Mock
  SourceService sourceService;

  Date issueCreationDate;

  Rule rule;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.componentDao()).thenReturn(componentDao);

    rule = mock(Rule.class);
    when(rule.key()).thenReturn(RuleKey.of("squid", "AvoidCycle"));
    when(rule.name()).thenReturn("Avoid cycle");
    when(ruleService.getNonNullByKey(rule.key())).thenReturn(rule);

    when(issueChangelogService.changelog(any(Issue.class))).thenReturn(mock(IssueChangelog.class));

    issueCreationDate = DateUtils.parseDateTime("2014-01-22T19:10:03+0100");
    when(i18n.formatDateTime(any(Locale.class), eq(issueCreationDate))).thenReturn("Jan 22, 2014 10:03 AM");

    when(i18n.message(any(Locale.class), eq("created"), eq((String) null))).thenReturn("Created");

    tester = new WsTester(new IssuesWs(
      new IssueShowAction(dbClient, issueService, issueChangelogService, commentService,
        new IssueActionsWriter(issueService, actionService), actionPlanService, userFinder, debtModel, ruleService, i18n, durations)
      ));
  }

  @Test
  public void show_issue() throws Exception {
    String issueKey = "ABCD";

    ComponentDto project = ComponentTesting.newProjectDto()
      .setId(1L)
      .setKey("org.sonar.Sonar")
      .setLongName("SonarQube")
      .setName("SonarQube");
    when(componentDao.getByUuid(session, project.uuid())).thenReturn(project);

    ComponentDto file = ComponentTesting.newFileDto(project)
      .setId(10L)
      .setKey("org.sonar.server.issue.IssueClient")
      .setLongName("SonarQube :: Issue Client")
      .setName("SonarQube :: Issue Client")
      .setQualifier("FIL")
      .setParentProjectId(1L);
    when(componentDao.getByUuid(session, file.uuid())).thenReturn(file);

    DefaultIssue issue = new DefaultIssue()
      .setKey(issueKey)
      .setComponentKey("org.sonar.server.issue.IssueClient")
      .setComponentUuid(file.uuid())
      .setProjectKey("org.sonar.Sonar")
      .setProjectUuid(project.uuid())
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setLine(12)
      .setMessage("Fix it")
      .setResolution("FIXED")
      .setStatus("CLOSED")
      .setSeverity("MAJOR")
      .setCreationDate(issueCreationDate);
    when(issueService.getByKey(issueKey)).thenReturn(issue);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issueKey);
    request.execute().assertJson(getClass(), "show_issue.json");
  }

  @Test
  public void show_issue_with_sub_project() throws Exception {
    String issueKey = "ABCD";

    // Project
    ComponentDto project = ComponentTesting.newProjectDto()
      .setId(1L)
      .setKey("org.sonar.Sonar")
      .setLongName("SonarQube");
    when(componentDao.getByUuid(session, project.uuid())).thenReturn(project);

    // Module
    ComponentDto module = ComponentTesting.newModuleDto(project)
      .setId(2L)
      .setKey("org.sonar.server.Server")
      .setLongName("SonarQube :: Server")
      .setQualifier("BRC")
      .setParentProjectId(1L);
    when(componentDao.getNullableById(module.getId(), session)).thenReturn(module);

    // File
    ComponentDto file = ComponentTesting.newFileDto(module)
      .setId(10L)
      .setKey("org.sonar.server.issue.IssueClient")
      .setLongName("SonarQube :: Issue Client")
      .setQualifier("FIL")
      .setParentProjectId(2L);
    when(componentDao.getByUuid(session, file.uuid())).thenReturn(file);

    DefaultIssue issue = new DefaultIssue()
      .setKey(issueKey)
      .setComponentKey("org.sonar.server.issue.IssueClient")
      .setComponentUuid(file.uuid())
      .setProjectKey("org.sonar.Sonar")
      .setProjectUuid(project.uuid())
      .setModuleUuid(module.uuid())
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setLine(12)
      .setMessage("Fix it")
      .setResolution("FIXED")
      .setStatus("CLOSED")
      .setSeverity("MAJOR")
      .setCreationDate(issueCreationDate);
    when(issueService.getByKey(issueKey)).thenReturn(issue);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issueKey);
    request.execute().assertJson(getClass(), "show_issue_with_sub_project.json");
  }

  @Test
  public void use_project_and_sub_project_names_if_no_long_name() throws Exception {
    String issueKey = "ABCD";

    // Project
    ComponentDto project = ComponentTesting.newProjectDto()
      .setId(1L)
      .setKey("org.sonar.Sonar")
      .setName("SonarQube")
      .setLongName(null);
    when(componentDao.getByUuid(session, project.uuid())).thenReturn(project);

    // Module
    ComponentDto module = ComponentTesting.newModuleDto(project)
      .setId(2L)
      .setKey("org.sonar.server.Server")
      .setName("SonarQube :: Server")
      .setLongName(null)
      .setQualifier("BRC")
      .setParentProjectId(1L);
    when(componentDao.getNullableById(module.getId(), session)).thenReturn(module);

    // File
    ComponentDto file = ComponentTesting.newFileDto(module)
      .setId(10L)
      .setKey("org.sonar.server.issue.IssueClient")
      .setLongName("SonarQube :: Issue Client")
      .setQualifier("FIL")
      .setParentProjectId(2L);
    when(componentDao.getByUuid(session, file.uuid())).thenReturn(file);

    DefaultIssue issue = new DefaultIssue()
      .setKey(issueKey)
      .setComponentKey("org.sonar.server.issue.IssueClient")
      .setComponentUuid(file.uuid())
      .setProjectKey("org.sonar.Sonar")
      .setProjectUuid(project.uuid())
      .setModuleUuid(module.uuid())
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setLine(12)
      .setEffortToFix(2.0)
      .setMessage("Fix it")
      .setResolution("FIXED")
      .setStatus("CLOSED")
      .setSeverity("MAJOR")
      .setCreationDate(issueCreationDate);
    when(issueService.getByKey(issueKey)).thenReturn(issue);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issueKey);
    request.execute().assertJson(getClass(), "show_issue_with_sub_project.json");
  }

  @Test
  public void show_issue_on_removed_component() throws Exception {
    String issueKey = "ABCD";

    ComponentDto project = ComponentTesting.newProjectDto()
      .setId(1L)
      .setKey("org.sonar.Sonar")
      .setLongName("SonarQube")
      .setName("SonarQube");
    when(componentDao.getByUuid(session, project.uuid())).thenReturn(project);

    ComponentDto file = ComponentTesting.newFileDto(project)
      .setId(10L)
      .setEnabled(false)
      .setKey("org.sonar.server.issue.IssueClient")
      .setLongName("SonarQube :: Issue Client")
      .setName("SonarQube :: Issue Client")
      .setQualifier("FIL")
      .setParentProjectId(1L);
    when(componentDao.getByUuid(session, file.uuid())).thenReturn(file);

    DefaultIssue issue = createIssue()
      .setComponentUuid(file.uuid())
      .setProjectUuid(project.uuid());
    when(issueService.getByKey(issueKey)).thenReturn(issue);

    MockUserSession.set();
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issueKey);
    request.execute().assertJson(getClass(), "show_issue_on_removed_component.json");
  }

  @Test
  public void show_issue_with_action_plan() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setActionPlanKey("AP-ABCD");
    when(issueService.getByKey(issue.key())).thenReturn(issue);

    when(actionPlanService.findByKey(eq(issue.actionPlanKey()), any(UserSession.class))).thenReturn(new DefaultActionPlan().setKey("AP-ABCD").setName("Version 4.2"));

    MockUserSession.set();
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_action_plan.json");
  }

  @Test
  public void show_issue_with_users() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setAssignee("john")
      .setReporter("steven")
      .setAuthorLogin("Henry");
    when(issueService.getByKey(issue.key())).thenReturn(issue);

    when(userFinder.findByLogin("john")).thenReturn(new DefaultUser().setLogin("john").setName("John"));
    when(userFinder.findByLogin("steven")).thenReturn(new DefaultUser().setLogin("steven").setName("Steven"));

    MockUserSession.set();
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_users.json");
  }

  @Test
  public void show_issue_with_technical_debt() throws Exception {
    Duration debt = (Duration.create(7260L));
    DefaultIssue issue = createStandardIssue().setDebt(debt);
    when(issueService.getByKey(issue.key())).thenReturn(issue);

    when(durations.encode(debt)).thenReturn("2h1min");

    MockUserSession.set();
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_technical_debt.json");
  }

  @Test
  public void show_issue_with_user_characteristics() throws Exception {
    DefaultIssue issue = createStandardIssue().setDebt(Duration.create(7260L));
    when(issueService.getByKey(issue.key())).thenReturn(issue);

    when(rule.debtCharacteristicKey()).thenReturn("K2");
    when(debtModel.characteristicById(1)).thenReturn(new DefaultDebtCharacteristic().setKey("K1").setId(1).setName("Maintainability"));
    when(debtModel.characteristicById(2)).thenReturn(new DefaultDebtCharacteristic().setKey("K2").setId(2).setName("Readability").setParentId(1));
    when(debtModel.characteristicByKey("K2")).thenReturn(new DefaultDebtCharacteristic().setKey("K2").setId(2).setName("Readability").setParentId(1));

    MockUserSession.set();
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_characteristics.json");
  }

  @Test
  public void show_issue_with_default_characteristics() throws Exception {
    DefaultIssue issue = createStandardIssue().setDebt(Duration.create(7260L));
    when(issueService.getByKey(issue.key())).thenReturn(issue);

    when(rule.debtCharacteristicKey()).thenReturn("K2");
    when(debtModel.characteristicById(1)).thenReturn(new DefaultDebtCharacteristic().setKey("K1").setId(1).setName("Maintainability"));
    when(debtModel.characteristicById(2)).thenReturn(new DefaultDebtCharacteristic().setKey("K2").setId(2).setName("Readability").setParentId(1));
    when(debtModel.characteristicByKey("K2")).thenReturn(new DefaultDebtCharacteristic().setKey("K2").setId(2).setName("Readability").setParentId(1));

    MockUserSession.set();
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_characteristics.json");
  }

  @Test
  public void show_issue_with_dates() throws Exception {
    Date creationDate = DateUtils.parseDateTime("2014-01-22T19:10:03+0100");
    Date updateDate = DateUtils.parseDateTime("2014-01-23T19:10:03+0100");
    Date closedDate = DateUtils.parseDateTime("2014-01-24T19:10:03+0100");

    DefaultIssue issue = createStandardIssue()
      .setCreationDate(creationDate)
      .setUpdateDate(updateDate)
      .setCloseDate(closedDate);
    when(issueService.getByKey(issue.key())).thenReturn(issue);

    when(i18n.formatDateTime(any(Locale.class), eq(creationDate))).thenReturn("Jan 22, 2014 10:03 AM");
    when(i18n.formatDateTime(any(Locale.class), eq(updateDate))).thenReturn("Jan 23, 2014 10:03 AM");
    when(i18n.ageFromNow(any(Locale.class), eq(updateDate))).thenReturn("9 days");
    when(i18n.formatDateTime(any(Locale.class), eq(closedDate))).thenReturn("Jan 24, 2014 10:03 AM");

    MockUserSession.set();
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_dates.json");
  }

  @Test
  public void show_issue_with_comments() throws Exception {
    Date date1 = DateUtils.parseDateTime("2014-02-22T19:10:03+0100");
    Date date2 = DateUtils.parseDateTime("2014-02-23T19:10:03+0100");

    DefaultIssue issue = createStandardIssue();
    when(issueService.getByKey(issue.key())).thenReturn(issue);

    when(commentService.findComments(issue.key())).thenReturn(newArrayList(
      new DefaultIssueComment()
        .setKey("COMMENT-ABCD")
        .setMarkdownText("*My comment*")
        .setUserLogin("john")
        .setCreatedAt(date1),
      new DefaultIssueComment()
        .setKey("COMMENT-ABCE")
        .setMarkdownText("Another comment")
        .setUserLogin("arthur")
        .setCreatedAt(date2)
      ));

    when(userFinder.findByLogin("john")).thenReturn(new DefaultUser().setLogin("john").setName("John"));
    when(userFinder.findByLogin("arthur")).thenReturn(new DefaultUser().setLogin("arthur").setName("Arthur"));

    when(i18n.ageFromNow(any(Locale.class), eq(date1))).thenReturn("9 days");
    when(i18n.ageFromNow(any(Locale.class), eq(date2))).thenReturn("10 days");

    MockUserSession.set().setLogin("arthur");
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_comments.json");
  }

  @Test
  public void show_issue_with_transitions() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setStatus("RESOLVED")
      .setResolution("FIXED");
    when(issueService.getByKey(issue.key())).thenReturn(issue);

    when(issueService.listTransitions(eq(issue))).thenReturn(newArrayList(Transition.create("reopen", "RESOLVED", "REOPEN")));

    MockUserSession.set().setLogin("john");
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_transitions.json");
  }

  @Test
  public void show_issue_with_actions() throws Exception {
    DefaultIssue issue = createStandardIssue()
      .setStatus("OPEN");
    when(issueService.getByKey(issue.key())).thenReturn(issue);

    MockUserSession.set().setLogin("john");
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_actions.json");
  }

  @Test
  public void show_issue_with_changelog() throws Exception {
    DefaultIssue issue = createStandardIssue();
    when(issueService.getByKey(issue.key())).thenReturn(issue);

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
    WsTester.TestRequest request = tester.newGetRequest("api/issues", "show").setParam("key", issue.key());
    request.execute().assertJson(getClass(), "show_issue_with_changelog.json");
  }

  private DefaultIssue createStandardIssue() {
    ComponentDto project = ComponentTesting.newProjectDto()
      .setId(1L)
      .setKey("org.sonar.Sonar")
      .setLongName("SonarQube")
      .setName("SonarQube");
    when(componentDao.getByUuid(session, project.uuid())).thenReturn(project);

    ComponentDto file = ComponentTesting.newFileDto(project)
      .setId(10L)
      .setKey("org.sonar.server.issue.IssueClient")
      .setLongName("SonarQube :: Issue Client")
      .setName("SonarQube :: Issue Client")
      .setQualifier("FIL")
      .setParentProjectId(1L);
    when(componentDao.getByUuid(session, file.uuid())).thenReturn(file);

    return createIssue()
      .setComponentUuid(file.uuid())
      .setProjectUuid(project.uuid());
  }

  private DefaultIssue createIssue() {
    return new DefaultIssue()
      .setKey("ABCD")
      .setComponentKey("org.sonar.server.issue.IssueClient")
      .setProjectKey("org.sonar.Sonar")
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setCreationDate(issueCreationDate);
  }

}
