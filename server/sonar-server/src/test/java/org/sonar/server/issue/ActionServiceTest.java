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

package org.sonar.server.issue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.config.Settings;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.action.Actions;
import org.sonar.api.issue.action.Function;
import org.sonar.api.issue.condition.Condition;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.properties.ProjectSettingsFactory;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectDto;
import static org.sonar.db.rule.RuleTesting.newXooX1;

public class ActionServiceTest {

  static final String PROJECT_KEY = "PROJECT_KEY";
  static final String PROJECT_UUID = "PROJECT_UUID";

  static final String ISSUE_KEY = "ISSUE_KEY";
  static final String PLUGIN_ACTION = "link-to-jira";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().login("arthur");

  DbClient dbClient = mock(DbClient.class);
  DbSession session = mock(DbSession.class);

  IssueService issueService = mock(IssueService.class);
  IssueStorage issueStorage = mock(IssueStorage.class);
  IssueUpdater updater = mock(IssueUpdater.class);
  ProjectSettingsFactory projectSettingsFactory = mock(ProjectSettingsFactory.class);
  Settings settings = new Settings();
  Actions actions = new Actions();
  ActionService actionService;

  ComponentDto project;
  IssueDto issue;

  @Before
  public void before() {
    when(dbClient.openSession(false)).thenReturn(session);
    when(projectSettingsFactory.newProjectSettings(PROJECT_KEY)).thenReturn(settings);

    project = newProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    issue = IssueTesting.newDto(newXooX1().setId(10), newFileDto(project), project).setKee(ISSUE_KEY);

    actionService = new ActionService(dbClient, userSession, projectSettingsFactory, actions, issueService, updater, issueStorage);
  }

  @Test
  public void execute_functions() {
    Function function1 = mock(Function.class);
    Function function2 = mock(Function.class);

    when(issueService.getByKeyForUpdate(session, ISSUE_KEY)).thenReturn(issue);

    actions.add(PLUGIN_ACTION).setConditions(new AlwaysMatch()).setFunctions(function1, function2);

    assertThat(actionService.execute(ISSUE_KEY, PLUGIN_ACTION)).isNotNull();

    verify(function1).execute(any(Function.Context.class));
    verify(function2).execute(any(Function.Context.class));
    verifyNoMoreInteractions(function1, function2);
  }

  @Test
  public void modify_issue_when_executing_a_function() {
    Function function = new TweetFunction();
    when(issueService.getByKeyForUpdate(session, ISSUE_KEY)).thenReturn(issue);

    actions.add(PLUGIN_ACTION).setConditions(new AlwaysMatch()).setFunctions(function);
    assertThat(actionService.execute(ISSUE_KEY, PLUGIN_ACTION)).isNotNull();

    verify(updater).addComment(any(DefaultIssue.class), eq("New tweet on issue ISSUE_KEY"), any(IssueChangeContext.class));
    verify(updater).setAttribute(any(DefaultIssue.class), eq("tweet"), eq("tweet sent"), any(IssueChangeContext.class));
  }

  @Test
  public void use_project_settings_when_executing_a_function() {
    Function function = new SettingsFunction();
    when(issueService.getByKeyForUpdate(session, ISSUE_KEY)).thenReturn(issue);
    settings.setProperty("key", "value");

    actions.add(PLUGIN_ACTION).setConditions(new AlwaysMatch()).setFunctions(function);
    actionService.execute(ISSUE_KEY, PLUGIN_ACTION);

    verify(updater).addComment(any(DefaultIssue.class), eq("Property 'key' is 'value'"), any(IssueChangeContext.class));
  }

  @Test
  public void not_execute_function_if_action_not_found() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Action is not found : tweet");

    Function function = mock(Function.class);
    when(issueService.getByKeyForUpdate(session, ISSUE_KEY)).thenReturn(issue);
    actions.add(PLUGIN_ACTION).setConditions(new AlwaysMatch()).setFunctions(function);
    actionService.execute(ISSUE_KEY, "tweet");
  }

  @Test
  public void not_execute_function_if_action_is_not_supported() {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("A condition is not respected");

    Function function = mock(Function.class);

    when(issueService.getByKeyForUpdate(session, ISSUE_KEY)).thenReturn(issue);
    actions.add(PLUGIN_ACTION).setConditions(new NeverMatch()).setFunctions(function);
    actionService.execute(ISSUE_KEY, PLUGIN_ACTION);
  }

  @Test
  public void return_plugin_actions() {
    actions.add(PLUGIN_ACTION).setConditions(new AlwaysMatch());
    actions.add("tweet").setConditions(new NeverMatch());
    assertThat(actionService.listAvailableActions(issue.toDefaultIssue())).contains(PLUGIN_ACTION);
  }

  @Test
  public void return_plugin_actions_on_resolved_issue() {
    actions.add(PLUGIN_ACTION).setConditions(new AlwaysMatch());
    actions.add("tweet").setConditions(new NeverMatch());
    issue = IssueTesting.newDto(newXooX1().setId(10), newFileDto(project), project).setKee(ISSUE_KEY).setResolution(RESOLUTION_FIXED);

    assertThat(actionService.listAvailableActions(issue.toDefaultIssue())).contains(PLUGIN_ACTION);
  }

  @Test
  public void return_provided_actions_without_set_severity_when_not_issue_admin() {
    assertThat(actionService.listAvailableActions(issue.toDefaultIssue())).containsOnly("comment", "assign", "set_tags", "assign_to_me", "plan");
  }

  @Test
  public void return_provided_actions_with_set_severity_when_issue_admin() {
    userSession.addProjectUuidPermissions(ISSUE_ADMIN, PROJECT_UUID);
    assertThat(actionService.listAvailableActions(issue.toDefaultIssue())).containsOnly("comment", "assign", "set_tags", "assign_to_me", "plan", "set_severity");
  }

  @Test
  public void return_no_actions_when_not_logged() {
    userSession.anonymous();
    assertThat(actionService.listAvailableActions(issue.toDefaultIssue())).isEmpty();
  }

  @Test
  public void doest_not_return_assign_to_me_action_when_issue_already_assigned_to_user() {
    userSession.login("julien");
    IssueDto issue = IssueTesting.newDto(newXooX1().setId(10), newFileDto(project), project).setKee(ISSUE_KEY).setAssignee("julien");
    assertThat(actionService.listAvailableActions(issue.toDefaultIssue())).doesNotContain("assign_to_me");
  }

  @Test
  public void return_only_comment_action_when_issue_has_a_resolution() {
    IssueDto issue = IssueTesting.newDto(newXooX1().setId(10), newFileDto(project), project).setKee(ISSUE_KEY).setResolution(RESOLUTION_FIXED);
    assertThat(actionService.listAvailableActions(issue.toDefaultIssue())).containsOnly("comment");
  }

  @Test
  public void return_actions_by_issue_key() {
    when(issueService.getByKeyForUpdate(session, ISSUE_KEY)).thenReturn(issue);
    assertThat(actionService.listAvailableActions(ISSUE_KEY)).isNotEmpty();
  }

  public class AlwaysMatch implements Condition {
    @Override
    public boolean matches(Issue issue) {
      return true;
    }
  }

  public class NeverMatch implements Condition {
    @Override
    public boolean matches(Issue issue) {
      return false;
    }
  }

  public class TweetFunction implements Function {
    @Override
    public void execute(Context context) {
      context.addComment("New tweet on issue " + context.issue().key());
      context.setAttribute("tweet", "tweet sent");
    }
  }

  public class SettingsFunction implements Function {
    @Override
    public void execute(Context context) {
      context.addComment(String.format("Property 'key' is '%s'", context.projectSettings().getString("key")));
    }
  }

}
