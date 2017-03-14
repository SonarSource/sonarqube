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

import com.google.common.collect.ImmutableMap;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.issue.Issue;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.issue.ws.BulkChangeAction;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.UserTesting.newUserDto;

public class AssignActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public DbTester dbTester = DbTester.create();

  private IssueFieldsSetter issueUpdater = new IssueFieldsSetter();

  private IssueChangeContext issueChangeContext = IssueChangeContext.createUser(new Date(), "emmerik");

  private DefaultIssue issue = new DefaultIssue().setKey("ABC");
  private Action.Context context = new BulkChangeAction.ActionContext(issue, issueChangeContext);

  private AssignAction action = new AssignAction(dbTester.getDbClient(), issueUpdater);

  @Test
  public void assign_issue() {
    UserDto assignee = newUserDto();

    action.execute(ImmutableMap.of("verifiedAssignee", assignee), context);

    assertThat(issue.assignee()).isEqualTo(assignee.getLogin());
  }

  @Test
  public void fail_if_assignee_is_not_verified() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Assignee is missing from the execution parameters");

    action.execute(emptyMap(), context);
  }

  @Test
  public void verify_that_assignee_exists() {
    String assignee = "arthur";
    Map<String, Object> properties = new HashMap<>(ImmutableMap.of("assignee", assignee));
    UserDto user = dbTester.users().insertUser(assignee);

    boolean verify = action.verify(properties, singletonList(issue), userSession);

    assertThat(verify).isTrue();
    UserDto verifiedUser = (UserDto) properties.get(AssignAction.VERIFIED_ASSIGNEE);
    assertThat(verifiedUser.getLogin()).isEqualTo(user.getLogin());
  }

  @Test
  public void fail_if_assignee_does_not_exists() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("Unknown user: arthur");

    action.verify(ImmutableMap.of("assignee", "arthur"), singletonList(issue), userSession);
  }

  @Test
  public void unassign_issue_if_assignee_is_empty() {
    Map<String, Object> properties = new HashMap<>(ImmutableMap.of("assignee", ""));

    boolean verify = action.verify(properties, singletonList(issue), userSession);

    assertThat(verify).isTrue();
    assertThat(properties.containsKey(AssignAction.VERIFIED_ASSIGNEE)).isTrue();
    assertThat(properties.get(AssignAction.VERIFIED_ASSIGNEE)).isNull();
  }

  @Test
  public void support_only_unresolved_issues() {
    assertThat(action.supports(new DefaultIssue().setResolution(null))).isTrue();
    assertThat(action.supports(new DefaultIssue().setResolution(Issue.RESOLUTION_FIXED))).isFalse();
  }

}
