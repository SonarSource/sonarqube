/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newIssue;
import static org.sonar.db.rule.RuleTesting.newRule;

public class IssueFinderIT {


  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private RuleDbTester ruleDbTester = new RuleDbTester(db);
  private IssueDbTester issueDbTester = new IssueDbTester(db);

  private IssueFinder underTest = new IssueFinder(db.getDbClient(), userSession);

  @Test
  public void get_by_issue_key() {
    IssueDto issueDto = insertIssue();
    addProjectPermission(issueDto, USER);

    IssueDto result = underTest.getByKey(db.getSession(), issueDto.getKey());

    assertThat(result).isNotNull();
    assertThat(result.getKey()).isEqualTo(issueDto.getKey());
  }

  @Test
  public void fail_when_issue_key_does_not_exist() {
    IssueDto issueDto = insertIssue();
    addProjectPermission(issueDto, USER);

    assertThatThrownBy(() -> underTest.getByKey(db.getSession(), "UNKNOWN"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Issue with key 'UNKNOWN' does not exist");
  }

  @Test
  public void fail_when_not_enough_permission() {
    IssueDto issueDto = insertIssue();
    addProjectPermission(issueDto, CODEVIEWER);

    assertThatThrownBy(() -> underTest.getByKey(db.getSession(), issueDto.getKey()))
      .isInstanceOf(ForbiddenException.class);
  }

  private IssueDto insertIssue() {
    RuleDto rule = ruleDbTester.insert(newRule());
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(project.getMainBranchComponent()));
    return issueDbTester.insert(newIssue(rule, project.getMainBranchComponent(), file));
  }

  private void addProjectPermission(IssueDto issueDto, String permission) {
    BranchDto branchDto = db.getDbClient().branchDao().selectByUuid(db.getSession(), issueDto.getProjectUuid())
      .orElseThrow(() -> new IllegalStateException("Couldn't find branch :" + issueDto.getProjectUuid()));
    ProjectDto projectDto = db.getDbClient().projectDao().selectByUuid(db.getSession(), branchDto.getProjectUuid()).get();
    userSession.addProjectPermission(permission, projectDto)
      .registerBranches(branchDto);
  }
}
