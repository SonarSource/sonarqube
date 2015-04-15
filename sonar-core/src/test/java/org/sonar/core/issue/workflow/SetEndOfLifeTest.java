/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.issue.workflow;

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class SetEndOfLifeTest {

  Function.Context context = mock(Function.Context.class);
  SetEndOfLife function = new SetEndOfLife();

  @Test
  public void should_resolve_as_fixed() throws Exception {
    Issue issue = new DefaultIssue().setEndOfLife(true).setOnDisabledRule(false);
    when(context.issue()).thenReturn(issue);
    function.execute(context);
    verify(context, times(1)).setResolution(Issue.RESOLUTION_FIXED);
  }

  @Test
  public void should_resolve_as_removed_when_rule_is_disabled() throws Exception {
    Issue issue = new DefaultIssue().setEndOfLife(true).setOnDisabledRule(true);
    when(context.issue()).thenReturn(issue);
    function.execute(context);
    verify(context, times(1)).setResolution(Issue.RESOLUTION_REMOVED);
  }

  @Test
  public void should_fail_if_issue_is_not_resolved() throws Exception {
    Issue issue = new DefaultIssue().setEndOfLife(false);
    when(context.issue()).thenReturn(issue);
    try {
      function.execute(context);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).contains("Issue is still alive");
      verify(context, never()).setResolution(anyString());
    }
  }

  @Test
  public void line_number_must_be_unset() throws Exception {
    Issue issue = new DefaultIssue().setEndOfLife(true).setLine(10);
    when(context.issue()).thenReturn(issue);
    function.execute(context);
    verify(context).setLine(null);
  }
}
