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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.tester.AnonymousMockUserSession;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class CommentActionTest {

  private CommentAction action;

  private IssueFieldsSetter issueUpdater = mock(IssueFieldsSetter.class);

  @Before
  public void before() {
    action = new CommentAction(issueUpdater);
  }

  @Test
  public void should_execute() {
    String comment = "My bulk change comment";
    Map<String, Object> properties = new HashMap<>();
    properties.put("comment", comment);
    DefaultIssue issue = mock(DefaultIssue.class);

    Action.Context context = mock(Action.Context.class);
    when(context.issue()).thenReturn(issue);

    action.execute(properties, context);
    verify(issueUpdater).addComment(eq(issue), eq(comment), any());
  }

  @Test
  public void should_verify_fail_if_parameter_not_found() {
    Map<String, Object> properties = singletonMap("unknwown", "unknown value");
    try {
      action.verify(properties, new ArrayList<>(), new AnonymousMockUserSession());
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Missing parameter : 'comment'");
    }
    verifyNoInteractions(issueUpdater);
  }

  @Test
  public void should_support_all_issues() {
    assertThat(action.supports(new DefaultIssue().setResolution(null))).isTrue();
    assertThat(action.supports(new DefaultIssue().setResolution(Issue.RESOLUTION_FIXED))).isTrue();
  }

}
