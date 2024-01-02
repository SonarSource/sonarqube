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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.server.tester.UserSessionRule.standalone;

public class AssignActionTest {

  private static final String ISSUE_CURRENT_ASSIGNEE_UUID = "current assignee uuid";


  @Rule
  public UserSessionRule userSession = standalone();

  @Rule
  public DbTester db = DbTester.create();

  private final IssueChangeContext issueChangeContext = issueChangeContextByUserBuilder(new Date(), "user_uuid").build();
  private final DefaultIssue issue = new DefaultIssue().setKey("ABC").setAssigneeUuid(ISSUE_CURRENT_ASSIGNEE_UUID);
  private Action.Context context;

  private final AssignAction underTest = new AssignAction(db.getDbClient(), new IssueFieldsSetter());

  @Before
  public void setUp() {
    ComponentDto project = db.components().insertPrivateProject();
    context = new ActionContext(issue, issueChangeContext, project);
  }

  @Test
  public void assign_issue() {
    UserDto assignee = db.users().insertUser("john");
    Map<String, Object> properties = new HashMap<>(ImmutableMap.of("assignee", "john"));

    underTest.verify(properties, Collections.emptyList(), userSession);
    boolean executeResult = underTest.execute(properties, context);

    assertThat(executeResult).isTrue();
    assertThat(issue.assignee()).isEqualTo(assignee.getUuid());
  }

  @Test
  public void unassign_issue_if_assignee_is_empty() {
    Map<String, Object> properties = new HashMap<>(ImmutableMap.of("assignee", ""));

    underTest.verify(properties, Collections.emptyList(), userSession);
    boolean executeResult = underTest.execute(properties, context);

    assertThat(executeResult).isTrue();
    assertThat(issue.assignee()).isNull();
  }

  @Test
  public void unassign_issue_if_assignee_is_null() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("assignee", null);

    underTest.verify(properties, Collections.emptyList(), userSession);
    boolean executeResult = underTest.execute(properties, context);

    assertThat(executeResult).isTrue();
    assertThat(issue.assignee()).isNull();
  }

  @Test
  public void fail_if_assignee_is_not_verified() {
    assertThatThrownBy(() -> underTest.execute(emptyMap(), context))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Assignee is missing from the execution parameters");
  }

  @Test
  public void fail_if_assignee_does_not_exists() {
    assertThatThrownBy(() -> underTest.verify(ImmutableMap.of("assignee", "arthur"), singletonList(issue), userSession))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Unknown user: arthur");
  }

  @Test
  public void fail_if_assignee_is_disabled() {
    db.users().insertUser(user -> user.setLogin("arthur").setActive(false));

    assertThatThrownBy(() -> underTest.verify(new HashMap<>(ImmutableMap.of("assignee", "arthur")), singletonList(issue), userSession))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Unknown user: arthur");
  }

  @Test
  public void support_only_unresolved_issues() {
    assertThat(underTest.supports(new DefaultIssue().setResolution(null))).isTrue();
    assertThat(underTest.supports(new DefaultIssue().setResolution(Issue.RESOLUTION_FIXED))).isFalse();
  }

}
