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
package org.sonar.core.issue;

import org.junit.Test;

import java.util.Date;

import static org.fest.assertions.Assertions.assertThat;

public class IssueUpdaterTest {

  IssueUpdater updater = new IssueUpdater();
  DefaultIssue issue = new DefaultIssue();
  IssueChangeContext context = IssueChangeContext.createUser(new Date(), "emmerik");

  @Test
  public void should_assign() throws Exception {
    boolean updated = updater.assign(issue, "emmerik", context);
    assertThat(updated).isTrue();
    assertThat(issue.assignee()).isEqualTo("emmerik");
    FieldDiffs.Diff diff = issue.diffs().get("assignee");
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("emmerik");
  }

  @Test
  public void should_unassign() throws Exception {
    issue.setAssignee("morgan");
    boolean updated = updater.assign(issue, null, context);
    assertThat(updated).isTrue();
    assertThat(issue.assignee()).isNull();
    FieldDiffs.Diff diff = issue.diffs().get("assignee");
    assertThat(diff.oldValue()).isEqualTo("morgan");
    assertThat(diff.newValue()).isNull();
  }

  @Test
  public void should_change_assignee() throws Exception {
    issue.setAssignee("morgan");
    boolean updated = updater.assign(issue, "emmerik", context);
    assertThat(updated).isTrue();
    assertThat(issue.assignee()).isEqualTo("emmerik");
    FieldDiffs.Diff diff = issue.diffs().get("assignee");
    assertThat(diff.oldValue()).isEqualTo("morgan");
    assertThat(diff.newValue()).isEqualTo("emmerik");
  }

  @Test
  public void should_not_change_assignee() throws Exception {
    issue.setAssignee("morgan");
    boolean updated = updater.assign(issue, "morgan", context);
    assertThat(updated).isFalse();
    assertThat(issue.diffs()).isNull();
  }


  @Test
  public void should_set_severity() throws Exception {
    boolean updated = updater.setSeverity(issue, "BLOCKER", context);
    assertThat(updated).isTrue();
    assertThat(issue.severity()).isEqualTo("BLOCKER");
    assertThat(issue.manualSeverity()).isFalse();

    FieldDiffs.Diff diff = issue.diffs().get("severity");
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("BLOCKER");
  }

  @Test
  public void should_update_severity() throws Exception {
    issue.setSeverity("BLOCKER");
    boolean updated = updater.setSeverity(issue, "MINOR", context);

    assertThat(updated).isTrue();
    assertThat(issue.severity()).isEqualTo("MINOR");
    FieldDiffs.Diff diff = issue.diffs().get("severity");
    assertThat(diff.oldValue()).isEqualTo("BLOCKER");
    assertThat(diff.newValue()).isEqualTo("MINOR");
  }

  @Test
  public void should_not_change_severity() throws Exception {
    issue.setSeverity("MINOR");
    boolean updated = updater.setSeverity(issue, "MINOR", context);
    assertThat(updated).isFalse();
    assertThat(issue.diffs()).isNull();
  }

  @Test
  public void should_not_revert_manual_severity() throws Exception {
    issue.setSeverity("MINOR").setManualSeverity(true);
    try {
      updater.setSeverity(issue, "MAJOR", context);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Severity can't be changed");
    }
  }

  @Test
  public void should_set_manual_severity() throws Exception {
    issue.setSeverity("BLOCKER");
    boolean updated = updater.setManualSeverity(issue, "MINOR", context);

    assertThat(updated).isTrue();
    assertThat(issue.severity()).isEqualTo("MINOR");
    assertThat(issue.manualSeverity()).isTrue();
    FieldDiffs.Diff diff = issue.diffs().get("severity");
    assertThat(diff.oldValue()).isEqualTo("BLOCKER");
    assertThat(diff.newValue()).isEqualTo("MINOR");
  }

  @Test
  public void should_not_change_manual_severity() throws Exception {
    issue.setSeverity("MINOR").setManualSeverity(true);
    boolean updated = updater.setManualSeverity(issue, "MINOR", context);
    assertThat(updated).isFalse();
    assertThat(issue.diffs()).isNull();
  }

  @Test
  public void should_set_line() throws Exception {
    boolean updated = updater.setLine(issue, 123);
    assertThat(updated).isTrue();
    assertThat(issue.line()).isEqualTo(123);

    // do not save change
    assertThat(issue.diffs()).isNull();
  }

  @Test
  public void should_not_change_line() throws Exception {
    issue.setLine(123);
    boolean updated = updater.setLine(issue, 123);
    assertThat(updated).isFalse();
    assertThat(issue.line()).isEqualTo(123);
    assertThat(issue.diffs()).isNull();
  }

  @Test
  public void should_set_resolution() throws Exception {
    boolean updated = updater.setResolution(issue, "OPEN", context);
    assertThat(updated).isTrue();
    assertThat(issue.resolution()).isEqualTo("OPEN");

    FieldDiffs.Diff diff = issue.diffs().get("resolution");
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("OPEN");
  }

  @Test
  public void should_not_change_resolution() throws Exception {
    issue.setResolution("FIXED");
    boolean updated = updater.setResolution(issue, "FIXED", context);
    assertThat(updated).isFalse();
    assertThat(issue.resolution()).isEqualTo("FIXED");
    assertThat(issue.diffs()).isNull();
  }

  @Test
  public void should_set_status() throws Exception {
    boolean updated = updater.setStatus(issue, "OPEN", context);
    assertThat(updated).isTrue();
    assertThat(issue.status()).isEqualTo("OPEN");

    FieldDiffs.Diff diff = issue.diffs().get("status");
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("OPEN");
  }

  @Test
  public void should_not_change_status() throws Exception {
    issue.setStatus("CLOSED");
    boolean updated = updater.setStatus(issue, "CLOSED", context);
    assertThat(updated).isFalse();
    assertThat(issue.status()).isEqualTo("CLOSED");
    assertThat(issue.diffs()).isNull();
  }

  @Test
  public void should_set_new_attribute_value() throws Exception {
    boolean updated = updater.setAttribute(issue, "JIRA", "FOO-123", context);
    assertThat(updated).isTrue();
    assertThat(issue.attribute("JIRA")).isEqualTo("FOO-123");
    assertThat(issue.diffs().diffs()).hasSize(1);
    assertThat(issue.diffs().get("JIRA").oldValue()).isNull();
    assertThat(issue.diffs().get("JIRA").newValue()).isEqualTo("FOO-123");
  }

  @Test
  public void should_unset_attribute() throws Exception {
    issue.setAttribute("JIRA", "FOO-123");
    boolean updated = updater.setAttribute(issue, "JIRA", null, context);
    assertThat(updated).isTrue();
    assertThat(issue.attribute("JIRA")).isNull();
    assertThat(issue.diffs().diffs()).hasSize(1);
    assertThat(issue.diffs().get("JIRA").oldValue()).isEqualTo("FOO-123");
    assertThat(issue.diffs().get("JIRA").newValue()).isNull();
  }

  @Test
  public void should_not_update_attribute() throws Exception {
    issue.setAttribute("JIRA", "FOO-123");
    boolean updated = updater.setAttribute(issue, "JIRA", "FOO-123", context);
    assertThat(updated).isFalse();
  }

  @Test
  public void should_plan() throws Exception {
    boolean updated = updater.plan(issue, "ABCD", context);
    assertThat(updated).isTrue();
    assertThat(issue.actionPlanKey()).isEqualTo("ABCD");

    FieldDiffs.Diff diff = issue.diffs().get("actionPlanKey");
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("ABCD");
  }
}
