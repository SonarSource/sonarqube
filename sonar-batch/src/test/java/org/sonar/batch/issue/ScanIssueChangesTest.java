/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.issue;

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueChange;
import org.sonar.api.rule.Severity;
import org.sonar.core.issue.DefaultIssue;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.*;

public class ScanIssueChangesTest {

  IssueCache cache = mock(IssueCache.class);
  ScanIssueChanges changes = new ScanIssueChanges(cache);

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
    Issue issue = new DefaultIssue().setComponentKey("org/struts/Action.java").setKey("ABCDE");
    when(cache.componentIssue("org/struts/Action.java", "ABCDE")).thenReturn(issue);
    Issue changed = changes.apply(issue, IssueChange.create()
      .setLine(200)
      .setResolution(Issue.RESOLUTION_FALSE_POSITIVE)
      .setAttribute("JIRA", "FOO-123")
      .setManualSeverity(true)
      .setSeverity(Severity.CRITICAL)
      .setAssignee("arthur")
      .setCost(4.2)
    );
    verify(cache).addOrUpdate(issue);
    assertThat(changed.line()).isEqualTo(200);
    assertThat(changed.resolution()).isEqualTo(Issue.RESOLUTION_FALSE_POSITIVE);
    assertThat(changed.attribute("JIRA")).isEqualTo("FOO-123");
    assertThat(changed.severity()).isEqualTo(Severity.CRITICAL);
    assertThat(changed.assignee()).isEqualTo("arthur");
    assertThat(changed.cost()).isEqualTo(4.2);
    assertThat(changed.updatedAt()).isNotNull();
  }
}
