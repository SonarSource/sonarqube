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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Date;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.issue.IssueTesting;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.core.rule.RuleType.BUG;
import static org.sonar.core.rule.RuleType.VULNERABILITY;
import static org.sonar.db.permission.ProjectPermission.ISSUE_ADMIN;
import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.rule.RuleTesting.newRule;

public class SetTypeActionIT {

  private static final Date NOW = new Date(10_000_000_000L);
  private static final String USER_LOGIN = "john";

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create();

  private IssueFieldsSetter issueUpdater = new IssueFieldsSetter();

  private SetTypeAction action = new SetTypeAction(issueUpdater, userSession);

  @Test
  public void set_type() {
    IssueDto issueDto = newIssue().setType(BUG);
    DefaultIssue issue = issueDto.toDefaultIssue();
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    action.execute(ImmutableMap.of("type", VULNERABILITY.name()),
      new ActionContext(issue, issueDto, issueChangeContextByUserBuilder(NOW, userSession.getUuid()).build(), null));

    assertThat(issue.type()).isEqualTo(VULNERABILITY);
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.updateDate()).isEqualTo(NOW);
    assertThat(issue.mustSendNotifications()).isFalse();
    Map<String, FieldDiffs.Diff> change = issue.currentChange().diffs();
    assertThat(change.get("type").newValue()).isEqualTo(VULNERABILITY);
    assertThat(change.get("type").oldValue()).isEqualTo(BUG);
  }

  @Test
  public void verify_fail_if_parameter_not_found() {
    assertThatThrownBy(() -> action.verify(ImmutableMap.of("unknwown", VULNERABILITY.name()), Lists.newArrayList(), userSession))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Missing parameter : 'type'");
  }

  @Test
  public void verify_fail_if_type_is_invalid() {
    assertThatThrownBy(() -> action.verify(ImmutableMap.of("type", "unknown"), Lists.newArrayList(), userSession))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unknown type : unknown");
  }

  @Test
  public void support_only_unresolved_issues() {
    IssueDto issueDto = newIssue().setType(BUG);
    DefaultIssue issue = issueDto.toDefaultIssue();
    setUserWithBrowseAndAdministerIssuePermission(issueDto);

    assertThat(action.supports(issue.setResolution(null))).isTrue();
    assertThat(action.supports(issue.setResolution(Issue.RESOLUTION_FIXED))).isFalse();
  }

  @Test
  public void support_only_issues_with_issue_admin_permission() {
    IssueDto authorizedIssueDto = newIssue().setType(BUG);
    DefaultIssue authorizedIssue = authorizedIssueDto.toDefaultIssue();
    setUserWithBrowseAndAdministerIssuePermission(authorizedIssueDto);
    DefaultIssue unauthorizedIssue = newIssue().setType(BUG).toDefaultIssue();

    assertThat(action.supports(authorizedIssue.setResolution(null))).isTrue();
    assertThat(action.supports(unauthorizedIssue.setResolution(null))).isFalse();
  }

  private void setUserWithBrowseAndAdministerIssuePermission(IssueDto issueDto) {
    BranchDto branchDto = db.getDbClient().branchDao().selectByUuid(db.getSession(), issueDto.getProjectUuid())
      .orElseThrow(() -> new IllegalStateException("Couldn't find branch :" + issueDto.getProjectUuid()));
    ProjectDto project = db.getDbClient().projectDao().selectByUuid(db.getSession(), branchDto.getProjectUuid()).get();
    userSession.logIn(USER_LOGIN)
      .addProjectPermission(ISSUE_ADMIN, project)
      .addProjectPermission(USER, project)
      .registerBranches(branchDto);
  }

  private IssueDto newIssue() {
    RuleDto rule = db.rules().insert(newRule());
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(project));
    return IssueTesting.newIssue(rule, project, file);
  }

}
