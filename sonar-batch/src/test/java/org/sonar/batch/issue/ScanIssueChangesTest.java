/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.batch.issue;

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueChange;
import org.sonar.api.rule.Severity;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.workflow.IssueWorkflow;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.*;

public class ScanIssueChangesTest {

  IssueCache cache = mock(IssueCache.class);
  IssueWorkflow workflow = mock(IssueWorkflow.class);
  ScanIssueChanges changes = new ScanIssueChanges(cache, workflow);

  @Test
  public void should_ignore_empty_change() throws Exception {
    Issue issue = new DefaultIssue().setComponentKey("org/struts/Action.java").setKey("ABCDE");
    when(cache.componentIssue("org/struts/Action.java", "ABCDE")).thenReturn(issue);
    Issue changed = changes.apply(issue, IssueChange.create());
    verifyZeroInteractions(cache);
    assertThat(changed).isSameAs(issue);
    assertThat(changed.updatedAt()).isNull();
  }

  @Test
  public void unknown_issue_is_a_bad_api_usage() throws Exception {
    Issue issue = new DefaultIssue().setComponentKey("org/struts/Action.java").setKey("ABCDE");
    when(cache.componentIssue("org/struts/Action.java", "ABCDE")).thenReturn(null);
    try {
      changes.apply(issue, IssueChange.create().setLine(200));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Bad API usage. Unregistered issues can't be changed.");
    }
  }

  @Test
  public void should_change_fields() throws Exception {
    DefaultIssue issue = new DefaultIssue().setComponentKey("org/struts/Action.java").setKey("ABCDE");
    when(cache.componentIssue("org/struts/Action.java", "ABCDE")).thenReturn(issue);

    IssueChange change = IssueChange.create().setTransition("resolve");
    changes.apply(issue, change);

    verify(cache).addOrUpdate(issue);
    verify(workflow).apply(issue, change);
  }
}
