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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDbTester;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDbTester;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.web.UserRole.CODEVIEWER;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.issue.IssueTesting.newDto;
import static org.sonar.db.rule.RuleTesting.newRuleDto;

public class IssueFinderTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private RuleDbTester ruleDbTester = new RuleDbTester(db);
  private IssueDbTester issueDbTester = new IssueDbTester(db);
  private ComponentDbTester componentDbTester = new ComponentDbTester(db);

  private IssueFinder underTest = new IssueFinder(db.getDbClient(), userSession);

  @Test
  public void get_by_issue_key() {
    IssueDto issueDto = insertIssue();
    String permission = USER;
    addProjectPermission(issueDto, permission);

    IssueDto result = underTest.getByKey(db.getSession(), issueDto.getKey());

    assertThat(result).isNotNull();
    assertThat(result.getKey()).isEqualTo(issueDto.getKey());
  }

  @Test
  public void fail_when_issue_key_does_not_exist() {
    IssueDto issueDto = insertIssue();
    addProjectPermission(issueDto, USER);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Issue with key 'UNKNOWN' does not exist");
    underTest.getByKey(db.getSession(), "UNKNOWN");
  }

  @Test
  public void fail_when_not_enough_permission() {
    IssueDto issueDto = insertIssue();
    addProjectPermission(issueDto, CODEVIEWER);

    expectedException.expect(ForbiddenException.class);
    underTest.getByKey(db.getSession(), issueDto.getKey());
  }

  private IssueDto insertIssue() {
    RuleDto rule = ruleDbTester.insertRule(newRuleDto());
    ComponentDto project = componentDbTester.insertPrivateProject();
    ComponentDto file = componentDbTester.insertComponent(newFileDto(project));
    return issueDbTester.insertIssue(newDto(rule, file, project));
  }

  private void addProjectPermission(IssueDto issueDto, String permission) {
    userSession.addProjectPermission(permission, db.getDbClient().componentDao().selectByUuid(db.getSession(), issueDto.getProjectUuid()).get());
  }
}
