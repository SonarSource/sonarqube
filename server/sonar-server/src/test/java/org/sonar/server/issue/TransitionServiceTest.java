/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.issue.workflow.Transition;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newDto;
import static org.sonar.db.rule.RuleTesting.newRuleDto;

public class TransitionServiceTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private IssueFieldsSetter updater = new IssueFieldsSetter();
  private IssueWorkflow workflow = new IssueWorkflow(new FunctionExecutor(updater), updater);

  private TransitionService underTest = new TransitionService(userSession, workflow);

  @Before
  public void setUp() throws Exception {
    workflow.start();
  }

  @Test
  public void list_transitions() {
    IssueDto issue = newIssue().setStatus(STATUS_OPEN).setResolution(null);
    userSession.logIn("john").addProjectPermission(ISSUE_ADMIN, ComponentTesting.newPrivateProjectDto(OrganizationTesting.newOrganizationDto(), issue.getProjectUuid()));

    List<Transition> result = underTest.listTransitions(issue.toDefaultIssue());

    assertThat(result).extracting(Transition::key).containsOnly("confirm", "resolve", "falsepositive", "wontfix");
  }

  @Test
  public void list_transitions_returns_only_transitions_that_do_not_requires_issue_admin_permission() {
    userSession.logIn("john");
    IssueDto issue = newIssue().setStatus(STATUS_OPEN).setResolution(null);

    List<Transition> result = underTest.listTransitions(issue.toDefaultIssue());

    assertThat(result).extracting(Transition::key).containsOnly("confirm", "resolve");
  }

  @Test
  public void list_transitions_returns_nothing_when_not_logged() {
    IssueDto issue = newIssue().setStatus(STATUS_OPEN).setResolution(null);

    List<Transition> result = underTest.listTransitions(issue.toDefaultIssue());

    assertThat(result).isEmpty();
  }

  @Test
  public void do_transition() {
    DefaultIssue issue = newIssue().setStatus(STATUS_OPEN).setResolution(null).toDefaultIssue();

    boolean result = underTest.doTransition(issue, IssueChangeContext.createUser(new Date(), "john"), "confirm");

    assertThat(result).isTrue();
    assertThat(issue.status()).isEqualTo(STATUS_CONFIRMED);
  }

  private IssueDto newIssue() {
    RuleDto rule = newRuleDto().setId(10);
    ComponentDto project = ComponentTesting.newPrivateProjectDto(OrganizationTesting.newOrganizationDto());
    ComponentDto file = (newFileDto(project));
    return newDto(rule, file, project);
  }
}
