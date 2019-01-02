/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.issue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.issue.Issue;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newDto;
import static org.sonar.db.rule.RuleTesting.newRuleDto;

public class TransitionActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private IssueFieldsSetter updater = new IssueFieldsSetter();
  private IssueWorkflow workflow = new IssueWorkflow(new FunctionExecutor(updater), updater);
  private TransitionService transitionService = new TransitionService(userSession, workflow);

  private Action.Context context = mock(Action.Context.class);
  private DefaultIssue issue = newIssue().toDefaultIssue();

  private TransitionAction action = new TransitionAction(transitionService);

  @Before
  public void setUp() throws Exception {
    workflow.start();
    when(context.issue()).thenReturn(issue);
    when(context.issueChangeContext()).thenReturn(IssueChangeContext.createUser(new Date(), "user_uuid"));
  }

  @Test
  public void execute() {
    loginAndAddProjectPermission("john", ISSUE_ADMIN);
    issue.setStatus(Issue.STATUS_RESOLVED);
    issue.setResolution(Issue.RESOLUTION_FIXED);

    action.execute(ImmutableMap.of("transition", "reopen"), context);

    assertThat(issue.status()).isEqualTo(Issue.STATUS_REOPENED);
    assertThat(issue.resolution()).isNull();
  }

  @Test
  public void does_not_execute_if_transition_is_not_available() {
    loginAndAddProjectPermission("john", ISSUE_ADMIN);
    issue.setStatus(STATUS_CLOSED);

    action.execute(ImmutableMap.of("transition", "reopen"), context);

    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
  }

  @Test
  public void test_verify() {
    assertThat(action.verify(ImmutableMap.of("transition", "reopen"), emptyList(), userSession)).isTrue();
    assertThat(action.verify(ImmutableMap.of("transition", "close"), emptyList(), userSession)).isTrue();
  }

  @Test
  public void fail_to_verify_when_parameter_not_found() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Missing parameter : 'transition'");
    action.verify(ImmutableMap.of("unknwown", "reopen"), Lists.newArrayList(), userSession);
  }

  @Test
  public void should_support_all_issues() {
    assertThat(action.supports(new DefaultIssue().setResolution(null))).isTrue();
    assertThat(action.supports(new DefaultIssue().setResolution(Issue.RESOLUTION_FIXED))).isTrue();
  }

  private IssueDto newIssue() {
    RuleDto rule = newRuleDto().setId(10);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(OrganizationTesting.newOrganizationDto());
    ComponentDto file = (newFileDto(project));
    return newDto(rule, file, project);
  }

  private void loginAndAddProjectPermission(String login, String permission) {
    userSession.logIn(login).addProjectPermission(permission, ComponentTesting.newPrivateProjectDto(OrganizationTesting.newOrganizationDto(), issue.projectUuid()));
  }

}
