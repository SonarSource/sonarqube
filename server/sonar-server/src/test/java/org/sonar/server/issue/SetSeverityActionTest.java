/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.issue;

import com.google.common.collect.Lists;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.tester.AnonymousMockUserSession;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SetSeverityActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private UserSession userSessionMock = mock(UserSession.class);

  private SetSeverityAction action;

  private IssueFieldsSetter issueUpdater = mock(IssueFieldsSetter.class);

  @Before
  public void before() {
    action = new SetSeverityAction(issueUpdater, userSessionRule);
    userSessionRule.set(userSessionMock);
  }

  @Test
  public void should_execute() {
    String severity = "MINOR";
    Map<String, Object> properties = newHashMap();
    properties.put("severity", severity);
    DefaultIssue issue = mock(DefaultIssue.class);

    Action.Context context = mock(Action.Context.class);
    when(context.issue()).thenReturn(issue);

    action.execute(properties, context);
    verify(issueUpdater).setManualSeverity(eq(issue), eq(severity), any(IssueChangeContext.class));
  }

  @Test
  public void should_verify_fail_if_parameter_not_found() {
    Map<String, Object> properties = newHashMap();
    properties.put("unknwown", "unknown value");
    try {
      action.verify(properties, Lists.newArrayList(), new AnonymousMockUserSession());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Missing parameter : 'severity'");
    }
    verifyZeroInteractions(issueUpdater);
  }

  @Test
  public void should_support_only_unresolved_issues() {
    when(userSessionMock.hasComponentUuidPermission(UserRole.ISSUE_ADMIN, "foo:bar")).thenReturn(true);
    assertThat(action.supports(new DefaultIssue().setProjectUuid("foo:bar").setResolution(null))).isTrue();
    assertThat(action.supports(new DefaultIssue().setProjectUuid("foo:bar").setResolution(Issue.RESOLUTION_FIXED))).isFalse();
  }

  @Test
  public void should_support_only_issues_with_issue_admin_permission() {
    when(userSessionMock.hasComponentUuidPermission(UserRole.ISSUE_ADMIN, "foo:bar")).thenReturn(true);
    assertThat(action.supports(new DefaultIssue().setProjectUuid("foo:bar").setResolution(null))).isTrue();
    assertThat(action.supports(new DefaultIssue().setProjectUuid("foo:bar2").setResolution(null))).isFalse();
  }

}
