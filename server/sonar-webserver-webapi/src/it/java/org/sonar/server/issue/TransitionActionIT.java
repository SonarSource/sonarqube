/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.Uuids;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.rule.RuleTesting.newRule;

public class TransitionActionIT {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final IssueFieldsSetter updater = new IssueFieldsSetter();
  private final IssueWorkflow workflow = new IssueWorkflow(new FunctionExecutor(updater), updater, mock(TaintChecker.class));
  private final TransitionService transitionService = new TransitionService(userSession, workflow);
  private final Action.Context context = mock(Action.Context.class);
  private final DefaultIssue issue = newIssue().toDefaultIssue();
  private final TransitionAction action = new TransitionAction(transitionService);

  @Before
  public void setUp() {
    workflow.start();
    when(context.issue()).thenReturn(issue);
    when(context.issueChangeContext()).thenReturn(issueChangeContextByUserBuilder(new Date(), "user_uuid").build());
  }

  @Test
  public void execute() {
    loginAndAddProjectPermission("john", ISSUE_ADMIN);
    if (issue.type() == RuleType.SECURITY_HOTSPOT) {
      // this transition is not done for hotspots
      issue.setType(RuleType.CODE_SMELL);
    }
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
    assertThatThrownBy(() -> action.verify(ImmutableMap.of("unknown", "reopen"), Lists.newArrayList(), userSession))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Missing parameter : 'transition'");
  }

  @Test
  public void should_support_all_issues() {
    assertThat(action.supports(new DefaultIssue().setResolution(null))).isTrue();
    assertThat(action.supports(new DefaultIssue().setResolution(Issue.RESOLUTION_FIXED))).isTrue();
  }


  private IssueDto newIssue() {
    RuleDto rule = newRule().setUuid(Uuids.createFast());
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto file = (newFileDto(project));
    return IssueTesting.newIssue(rule, project, file);
  }

  private void loginAndAddProjectPermission(String login, String permission) {
    ProjectDto projectDto = ComponentTesting.newProjectDto();
    BranchDto branchDto = ComponentTesting.newBranchDto(projectDto.getUuid(), BranchType.BRANCH).setIsMain(true).setUuid(issue.projectUuid());
    userSession.logIn(login).addProjectPermission(permission, projectDto)
      .registerBranches(branchDto);
  }

}
