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

import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.server.issue.workflow.FunctionExecutor;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.issue.workflow.Transition;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class TransitionServiceTest {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private IssueFieldsSetter updater = new IssueFieldsSetter();
  private IssueWorkflow workflow = new IssueWorkflow(new FunctionExecutor(updater), updater);

  private TransitionService underTest = new TransitionService(userSession, workflow);

  @Before
  public void setUp() throws Exception {
    workflow.start();
  }

  @Test
  public void list_transitions() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    userSession.logIn().addProjectPermission(ISSUE_ADMIN, project);

    List<Transition> result = underTest.listTransitions(issue.toDefaultIssue());

    assertThat(result).extracting(Transition::key).containsOnly("confirm", "resolve", "falsepositive", "wontfix");
  }

  @Test
  public void list_transitions_returns_empty_list_on_external_issue() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto externalRule = db.rules().insert(r -> r.setIsExternal(true));
    IssueDto externalIssue = db.issues().insert(externalRule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    userSession.logIn().addProjectPermission(ISSUE_ADMIN, project);

    List<Transition> result = underTest.listTransitions(externalIssue.toDefaultIssue());

    assertThat(result).isEmpty();
  }

  @Test
  public void list_transitions_returns_only_transitions_that_do_not_requires_issue_admin_permission() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    userSession.logIn();

    List<Transition> result = underTest.listTransitions(issue.toDefaultIssue());

    assertThat(result).extracting(Transition::key).containsOnly("confirm");
  }

  @Test
  public void list_transitions_returns_nothing_when_not_logged() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));

    List<Transition> result = underTest.listTransitions(issue.toDefaultIssue());

    assertThat(result).isEmpty();
  }

  @Test
  public void do_transition() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));

    DefaultIssue defaultIssue = issue.toDefaultIssue();
    boolean result = underTest.doTransition(defaultIssue, IssueChangeContext.createUser(new Date(), "user_uuid"), "confirm");

    assertThat(result).isTrue();
    assertThat(defaultIssue.status()).isEqualTo(STATUS_CONFIRMED);
  }

  @Test
  public void do_transition_fail_on_external_issue() {
    ComponentDto project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDefinitionDto externalRule = db.rules().insert(r -> r.setIsExternal(true));
    IssueDto externalIssue = db.issues().insert(externalRule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    DefaultIssue defaultIssue = externalIssue.toDefaultIssue();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Transition is not allowed on issues imported from external rule engines");

    underTest.doTransition(defaultIssue, IssueChangeContext.createUser(new Date(), "user_uuid"), "confirm");
  }
}
