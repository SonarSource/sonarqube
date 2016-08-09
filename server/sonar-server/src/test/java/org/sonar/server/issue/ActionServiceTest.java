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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().login("arthur");

  DbClient dbClient = mock(DbClient.class);
  DbSession session = mock(DbSession.class);

  IssueService issueService = mock(IssueService.class);
  ActionService underTest;

  ComponentDto project;
  IssueDto issue;

  @Before
  public void before() {
    when(dbClient.openSession(false)).thenReturn(session);

    project = newProjectDto(PROJECT_UUID).setKey(PROJECT_KEY);
    issue = IssueTesting.newDto(newXooX1().setId(10), newFileDto(project, null), project).setKee(ISSUE_KEY);

    underTest = new ActionService(dbClient, userSession, issueService);
  }

  @Test
  public void return_provided_actions_without_set_severity_when_not_issue_admin() {
    assertThat(underTest.listAvailableActions(issue.toDefaultIssue())).containsOnly("comment", "assign", "set_tags", "set_type", "assign_to_me");
  }

  @Test
  public void return_provided_actions_with_set_severity_when_issue_admin() {
    userSession.addProjectUuidPermissions(ISSUE_ADMIN, PROJECT_UUID);
    assertThat(underTest.listAvailableActions(issue.toDefaultIssue())).containsOnly("comment", "assign", "set_tags", "set_type", "assign_to_me", "set_severity");
  }

  @Test
  public void return_no_actions_when_not_logged() {
    userSession.anonymous();
    assertThat(underTest.listAvailableActions(issue.toDefaultIssue())).isEmpty();
  }

  @Test
  public void doest_not_return_assign_to_me_action_when_issue_already_assigned_to_user() {
    userSession.login("julien");
    IssueDto issue = IssueTesting.newDto(newXooX1().setId(10), newFileDto(project, null), project).setKee(ISSUE_KEY).setAssignee("julien");
    assertThat(underTest.listAvailableActions(issue.toDefaultIssue())).doesNotContain("assign_to_me");
  }

  @Test
  public void return_only_comment_action_when_issue_has_a_resolution() {
    IssueDto issue = IssueTesting.newDto(newXooX1().setId(10), newFileDto(project, null), project).setKee(ISSUE_KEY).setResolution(RESOLUTION_FIXED);
    assertThat(underTest.listAvailableActions(issue.toDefaultIssue())).containsOnly("comment");
  }

  @Test
  public void return_actions_by_issue_key() {
    when(issueService.getByKeyForUpdate(session, ISSUE_KEY)).thenReturn(issue);
    assertThat(underTest.listAvailableActions(ISSUE_KEY)).isNotEmpty();
  }

}
