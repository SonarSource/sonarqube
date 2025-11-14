/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflow;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowActionsFactory;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowDefinition;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflow;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowActionsFactory;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowDefinition;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.core.rule.RuleType.CODE_SMELL;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.permission.ProjectPermission.ISSUE_ADMIN;

public class TransitionServiceIT {

  @Rule
  public DbTester db = DbTester.create();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final IssueFieldsSetter updater = new IssueFieldsSetter();
  private final IssueWorkflow workflow = new IssueWorkflow(
    new CodeQualityIssueWorkflow(new CodeQualityIssueWorkflowActionsFactory(updater), new CodeQualityIssueWorkflowDefinition(), mock(TaintChecker.class)),
    new SecurityHotspotWorkflow(new SecurityHotspotWorkflowActionsFactory(updater), new SecurityHotspotWorkflowDefinition()));

  private final TransitionService underTest = new TransitionService(userSession, workflow);

  @Test
  public void list_transitions() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project.getMainBranchComponent(), file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    userSession.logIn().addProjectPermission(ISSUE_ADMIN, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());

    List<String> result = underTest.listTransitionKeys(issue.toDefaultIssue());

    assertThat(result).containsExactly("accept", "falsepositive", "confirm", "resolve", "wontfix");
  }

  @Test
  public void list_transitions_on_external_issue() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    RuleDto externalRule = db.rules().insert(r -> r.setIsExternal(true));
    IssueDto externalIssue = db.issues().insert(externalRule, project.getMainBranchComponent(), file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    userSession.logIn().addProjectPermission(ISSUE_ADMIN, project.getProjectDto())
      .registerBranches(project.getMainBranchDto());

    List<String> result = underTest.listTransitionKeys(externalIssue.toDefaultIssue());

    assertThat(result).containsExactly("accept", "falsepositive", "confirm", "resolve", "wontfix");
  }

  @Test
  public void list_transitions_returns_only_transitions_that_do_not_requires_issue_admin_permission() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));
    userSession.logIn();

    List<String> result = underTest.listTransitionKeys(issue.toDefaultIssue());

    assertThat(result).containsOnly("confirm");
  }

  @Test
  public void list_transitions_returns_nothing_when_not_logged() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));

    List<String> result = underTest.listTransitionKeys(issue.toDefaultIssue());

    assertThat(result).isEmpty();
  }

  @Test
  public void do_transition() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto rule = db.rules().insert();
    IssueDto issue = db.issues().insert(rule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));

    DefaultIssue defaultIssue = issue.toDefaultIssue();
    boolean result = underTest.doTransition(defaultIssue, issueChangeContextByUserBuilder(new Date(), "user_uuid").build(), "confirm");

    assertThat(result).isTrue();
    assertThat(defaultIssue.status()).isEqualTo(STATUS_CONFIRMED);
  }

  @Test
  public void do_transition_on_external_issue() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    RuleDto externalRule = db.rules().insert(r -> r.setIsExternal(true));
    IssueDto externalIssue = db.issues().insert(externalRule, project, file, i -> i.setStatus(STATUS_OPEN).setResolution(null).setType(CODE_SMELL));

    DefaultIssue defaultIssue = externalIssue.toDefaultIssue();
    boolean result = underTest.doTransition(defaultIssue, issueChangeContextByUserBuilder(new Date(), "user_uuid").build(), "confirm");

    assertThat(result).isTrue();
    assertThat(defaultIssue.status()).isEqualTo(STATUS_CONFIRMED);
  }
}
