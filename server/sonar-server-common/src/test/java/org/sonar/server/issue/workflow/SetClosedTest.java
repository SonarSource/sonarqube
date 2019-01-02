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
package org.sonar.server.issue.workflow;

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.core.issue.DefaultIssue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.issue.workflow.SetClosed.INSTANCE;

public class SetClosedTest {

  private Function.Context context = mock(Function.Context.class);

  @Test
  public void should_resolve_as_fixed() {
    Issue issue = new DefaultIssue().setBeingClosed(true).setOnDisabledRule(false);
    when(context.issue()).thenReturn(issue);
    INSTANCE.execute(context);
    verify(context, times(1)).setResolution(Issue.RESOLUTION_FIXED);
  }

  @Test
  public void should_resolve_as_removed_when_rule_is_disabled() {
    Issue issue = new DefaultIssue().setBeingClosed(true).setOnDisabledRule(true);
    when(context.issue()).thenReturn(issue);
    INSTANCE.execute(context);
    verify(context, times(1)).setResolution(Issue.RESOLUTION_REMOVED);
  }

  @Test
  public void line_number_must_be_unset() {
    Issue issue = new DefaultIssue().setBeingClosed(true).setLine(10);
    when(context.issue()).thenReturn(issue);
    INSTANCE.execute(context);
    verify(context).unsetLine();
  }
}
