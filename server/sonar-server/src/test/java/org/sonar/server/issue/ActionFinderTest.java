/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.issue.IssueTesting.newDto;
import static org.sonar.db.rule.RuleTesting.newXooX1;

public class ActionFinderTest {

  static final String PROJECT_KEY = "PROJECT_KEY";
  static final String PROJECT_UUID = "PROJECT_UUID";

  static final String ISSUE_KEY = "ISSUE_KEY";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn("arthur");

  private ComponentDto project = newPrivateProjectDto(OrganizationTesting.newOrganizationDto(), PROJECT_UUID).setKey(PROJECT_KEY);
  private IssueDto issue = newDto(newXooX1().setId(10), newFileDto(project, null), project).setKee(ISSUE_KEY);

  private ActionFinder underTest = new ActionFinder(userSession);

  @Test
  public void return_provided_actions_without_set_severity_and_set_tpye_when_not_issue_admin() {
    assertThat(underTest.listAvailableActions(issue)).containsOnly("comment", "assign", "set_tags", "assign_to_me");
  }

  @Test
  public void return_provided_actions_with_set_severity_and_set_type_when_issue_admin() {
    userSession.addProjectPermission(ISSUE_ADMIN, ComponentTesting.newPrivateProjectDto(OrganizationTesting.newOrganizationDto(), PROJECT_UUID));
    assertThat(underTest.listAvailableActions(issue)).containsOnly("comment", "assign", "set_tags", "set_type", "assign_to_me", "set_severity");
  }

  @Test
  public void return_no_actions_when_not_logged() {
    userSession.anonymous();
    assertThat(underTest.listAvailableActions(issue)).isEmpty();
  }

  @Test
  public void doest_not_return_assign_to_me_action_when_issue_already_assigned_to_user() {

    userSession.logIn("julien");
    IssueDto issue = newDto(newXooX1().setId(10), newFileDto(project, null), project).setKee(ISSUE_KEY).setAssignee("julien");
    assertThat(underTest.listAvailableActions(issue)).doesNotContain("assign_to_me");
  }

  @Test
  public void return_only_comment_action_when_issue_has_a_resolution() {
    IssueDto issue = newDto(newXooX1().setId(10), newFileDto(project, null), project).setKee(ISSUE_KEY).setResolution(RESOLUTION_FIXED);
    assertThat(underTest.listAvailableActions(issue)).containsOnly("comment");
  }

}
