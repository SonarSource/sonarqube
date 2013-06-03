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

package org.sonar.server.issue;

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.action.Action;
import org.sonar.api.issue.action.Function;
import org.sonar.api.issue.condition.Condition;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.core.issue.DefaultIssueQueryResult;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.server.user.UserSession;

import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ActionServiceTest {

  private ActionService actionService;
  private DefaultIssueFinder finder = mock(DefaultIssueFinder.class);
  private IssueStorage issueStorage = mock(IssueStorage.class);
  private IssueUpdater updater = mock(IssueUpdater.class);

  @Test
  public void should_execute_functions() {
    Function function1 = mock(Function.class);
    Function function2 = mock(Function.class);

    Issue issue = new DefaultIssue().setKey("ABCD");
    IssueQueryResult issueQueryResult = new DefaultIssueQueryResult(newArrayList(issue));
    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);

    Action action = Action.builder("link-to-jira").conditions(new AlwaysMatch()).functions(function1, function2).build();

    actionService = new ActionService(finder, issueStorage, updater, newArrayList(action));
    assertThat(actionService.execute("ABCD", "link-to-jira", mock(UserSession.class))).isNotNull();

    verify(function1).execute(any(Function.Context.class));
    verify(function2).execute(any(Function.Context.class));
    verifyNoMoreInteractions(function1, function2);
  }

  @Test
  public void should_modify_issue_when_executing_a_function() {
    Function function = new TweetFunction();

    UserSession userSession = mock(UserSession.class);
    when(userSession.login()).thenReturn("arthur");

    DefaultIssue issue = new DefaultIssue().setKey("ABCD");
    IssueQueryResult issueQueryResult = new DefaultIssueQueryResult(newArrayList((Issue) issue));
    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);

    Action action = Action.builder("link-to-jira").conditions(new AlwaysMatch()).functions(function).build();

    actionService = new ActionService(finder, issueStorage, updater, newArrayList(action));
    assertThat(actionService.execute("ABCD", "link-to-jira", userSession)).isNotNull();

    verify(updater).addComment(eq(issue), eq("New tweet on issue ABCD"), any(IssueChangeContext.class));
    verify(updater).setAttribute(eq(issue), eq("tweet"), eq("tweet sent"), any(IssueChangeContext.class));
  }

  @Test
  public void should_not_execute_function_if_issue_not_found() {
    Function function = mock(Function.class);

    IssueQueryResult issueQueryResult = new DefaultIssueQueryResult(Collections.<Issue>emptyList());
    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);

    Action action = Action.builder("link-to-jira").conditions(new AlwaysMatch()).functions(function).build();

    actionService = new ActionService(finder, issueStorage, updater, newArrayList(action));

    try {
    actionService.execute("ABCD", "link-to-jira", mock(UserSession.class));
    } catch (Exception e){
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Issue is not found : ABCD");
    }
    verifyZeroInteractions(function);
  }

  @Test
  public void should_not_execute_function_if_action_not_found() {
    Function function = mock(Function.class);

    Issue issue = new DefaultIssue().setKey("ABCD");
    IssueQueryResult issueQueryResult = new DefaultIssueQueryResult(newArrayList(issue));
    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);

    Action action = Action.builder("link-to-jira").conditions(new AlwaysMatch()).functions(function).build();

    actionService = new ActionService(finder, issueStorage, updater, newArrayList(action));
    try {
      actionService.execute("ABCD", "tweet", mock(UserSession.class));
    } catch (Exception e){
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Action is not found : tweet");
    }
    verifyZeroInteractions(function);
  }

  @Test
  public void should_not_execute_function_if_action_is_not_supported() {
    Function function = mock(Function.class);

    Issue issue = new DefaultIssue().setKey("ABCD");
    IssueQueryResult issueQueryResult = new DefaultIssueQueryResult(newArrayList(issue));
    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);

    Action action = Action.builder("link-to-jira").conditions(new NeverMatch()).functions(function).build();

    actionService = new ActionService(finder, issueStorage, updater, newArrayList(action));
    try {
      actionService.execute("ABCD", "link-to-jira", mock(UserSession.class));
    } catch (Exception e){
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("A condition is not respected");
    }
    verifyZeroInteractions(function);
  }

  @Test
  public void should_list_available_supported_actions() {
    Issue issue = new DefaultIssue().setKey("ABCD");
    IssueQueryResult issueQueryResult = new DefaultIssueQueryResult(newArrayList(issue));
    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);

    Action action1 = Action.builder("link-to-jira").conditions(new AlwaysMatch()).build();
    Action action2 = Action.builder("tweet").conditions(new NeverMatch()).build();

    actionService = new ActionService(finder, issueStorage, updater, newArrayList(action1, action2));
    assertThat(actionService.listAvailableActions("ABCD")).containsOnly(action1);
  }

  @Test
  public void should_list_available_actions_throw_exception_if_issue_not_found() {
    IssueQueryResult issueQueryResult = new DefaultIssueQueryResult(Collections.<Issue>emptyList());
    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);

    Action action1 = Action.builder("link-to-jira").conditions(new AlwaysMatch()).build();
    Action action2 = Action.builder("tweet").conditions(new NeverMatch()).build();

    actionService = new ActionService(finder, issueStorage, updater, newArrayList(action1, action2));

    try {
      actionService.listAvailableActions("ABCD");
      fail();
    } catch (Exception e){
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Issue is not found : ABCD");
    }
  }

  @Test
  public void should_return_no_action() {
    Issue issue = new DefaultIssue().setKey("ABCD");
    IssueQueryResult issueQueryResult = new DefaultIssueQueryResult(newArrayList(issue));
    when(finder.find(any(IssueQuery.class))).thenReturn(issueQueryResult);

    actionService = new ActionService(finder, issueStorage, updater);
    assertThat(actionService.listAvailableActions("ABCD")).isEmpty();
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
      context.addComment("New tweet on issue "+ context.issue().key());
      context.setAttribute("tweet", "tweet sent");
    }
  }
}
