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
package org.sonar.core.issue;

import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.ActionPlan;
import org.sonar.api.user.User;
import org.sonar.api.utils.Duration;
import org.sonar.core.user.DefaultUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.issue.IssueUpdater.ACTION_PLAN;
import static org.sonar.core.issue.IssueUpdater.ASSIGNEE;
import static org.sonar.core.issue.IssueUpdater.RESOLUTION;
import static org.sonar.core.issue.IssueUpdater.SEVERITY;
import static org.sonar.core.issue.IssueUpdater.STATUS;
import static org.sonar.core.issue.IssueUpdater.TECHNICAL_DEBT;
import static org.sonar.core.issue.IssueUpdater.UNUSED;

public class IssueUpdaterTest {

  DefaultIssue issue = new DefaultIssue();
  IssueChangeContext context = IssueChangeContext.createUser(new Date(), "emmerik");

  IssueUpdater updater;

  @Before
  public void setUp() {
    updater = new IssueUpdater();
  }

  @Test
  public void assign() {
    User user = new DefaultUser().setLogin("emmerik").setName("Emmerik");

    boolean updated = updater.assign(issue, user, context);
    assertThat(updated).isTrue();
    assertThat(issue.assignee()).isEqualTo("emmerik");
    assertThat(issue.mustSendNotifications()).isTrue();
    FieldDiffs.Diff diff = issue.currentChange().get(ASSIGNEE);
    assertThat(diff.oldValue()).isEqualTo(UNUSED);
    assertThat(diff.newValue()).isEqualTo("Emmerik");
  }

  @Test
  public void unassign() {
    issue.setAssignee("morgan");
    boolean updated = updater.assign(issue, null, context);
    assertThat(updated).isTrue();
    assertThat(issue.assignee()).isNull();
    assertThat(issue.mustSendNotifications()).isTrue();
    FieldDiffs.Diff diff = issue.currentChange().get(ASSIGNEE);
    assertThat(diff.oldValue()).isEqualTo(UNUSED);
    assertThat(diff.newValue()).isNull();
  }

  @Test
  public void change_assignee() {
    User user = new DefaultUser().setLogin("emmerik").setName("Emmerik");

    issue.setAssignee("morgan");
    boolean updated = updater.assign(issue, user, context);
    assertThat(updated).isTrue();
    assertThat(issue.assignee()).isEqualTo("emmerik");
    assertThat(issue.mustSendNotifications()).isTrue();
    FieldDiffs.Diff diff = issue.currentChange().get(ASSIGNEE);
    assertThat(diff.oldValue()).isEqualTo(UNUSED);
    assertThat(diff.newValue()).isEqualTo("Emmerik");
  }

  @Test
  public void not_change_assignee() {
    User user = new DefaultUser().setLogin("morgan").setName("Morgan");

    issue.setAssignee("morgan");
    boolean updated = updater.assign(issue, user, context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_severity() {
    boolean updated = updater.setSeverity(issue, "BLOCKER", context);
    assertThat(updated).isTrue();
    assertThat(issue.severity()).isEqualTo("BLOCKER");
    assertThat(issue.manualSeverity()).isFalse();
    assertThat(issue.mustSendNotifications()).isFalse();

    FieldDiffs.Diff diff = issue.currentChange().get(SEVERITY);
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("BLOCKER");
  }

  @Test
  public void set_past_severity() {
    issue.setSeverity("BLOCKER");
    boolean updated = updater.setPastSeverity(issue, "INFO", context);
    assertThat(updated).isTrue();
    assertThat(issue.severity()).isEqualTo("BLOCKER");
    assertThat(issue.mustSendNotifications()).isFalse();

    FieldDiffs.Diff diff = issue.currentChange().get(SEVERITY);
    assertThat(diff.oldValue()).isEqualTo("INFO");
    assertThat(diff.newValue()).isEqualTo("BLOCKER");
  }

  @Test
  public void update_severity() {
    issue.setSeverity("BLOCKER");
    boolean updated = updater.setSeverity(issue, "MINOR", context);

    assertThat(updated).isTrue();
    assertThat(issue.severity()).isEqualTo("MINOR");
    assertThat(issue.mustSendNotifications()).isFalse();
    FieldDiffs.Diff diff = issue.currentChange().get(SEVERITY);
    assertThat(diff.oldValue()).isEqualTo("BLOCKER");
    assertThat(diff.newValue()).isEqualTo("MINOR");
  }

  @Test
  public void not_change_severity() {
    issue.setSeverity("MINOR");
    boolean updated = updater.setSeverity(issue, "MINOR", context);
    assertThat(updated).isFalse();
    assertThat(issue.mustSendNotifications()).isFalse();
    assertThat(issue.currentChange()).isNull();
  }

  @Test
  public void not_revert_manual_severity() {
    issue.setSeverity("MINOR").setManualSeverity(true);
    try {
      updater.setSeverity(issue, "MAJOR", context);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Severity can't be changed");
    }
  }

  @Test
  public void set_manual_severity() {
    issue.setSeverity("BLOCKER");
    boolean updated = updater.setManualSeverity(issue, "MINOR", context);

    assertThat(updated).isTrue();
    assertThat(issue.severity()).isEqualTo("MINOR");
    assertThat(issue.manualSeverity()).isTrue();
    assertThat(issue.mustSendNotifications()).isTrue();
    FieldDiffs.Diff diff = issue.currentChange().get(SEVERITY);
    assertThat(diff.oldValue()).isEqualTo("BLOCKER");
    assertThat(diff.newValue()).isEqualTo("MINOR");
  }

  @Test
  public void not_change_manual_severity() {
    issue.setSeverity("MINOR").setManualSeverity(true);
    boolean updated = updater.setManualSeverity(issue, "MINOR", context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_line() {
    boolean updated = updater.setLine(issue, 123);
    assertThat(updated).isTrue();
    assertThat(issue.line()).isEqualTo(123);
    assertThat(issue.mustSendNotifications()).isFalse();
    // do not save change
    assertThat(issue.currentChange()).isNull();
  }

  @Test
  public void set_past_line() {
    issue.setLine(42);
    boolean updated = updater.setPastLine(issue, 123);
    assertThat(updated).isTrue();
    assertThat(issue.line()).isEqualTo(42);
    assertThat(issue.mustSendNotifications()).isFalse();

    // do not save change
    assertThat(issue.currentChange()).isNull();
  }

  @Test
  public void not_change_line() {
    issue.setLine(123);
    boolean updated = updater.setLine(issue, 123);
    assertThat(updated).isFalse();
    assertThat(issue.line()).isEqualTo(123);
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_resolution() {
    boolean updated = updater.setResolution(issue, "OPEN", context);
    assertThat(updated).isTrue();
    assertThat(issue.resolution()).isEqualTo("OPEN");

    FieldDiffs.Diff diff = issue.currentChange().get(RESOLUTION);
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("OPEN");
    assertThat(issue.mustSendNotifications()).isTrue();
  }

  @Test
  public void not_change_resolution() {
    issue.setResolution("FIXED");
    boolean updated = updater.setResolution(issue, "FIXED", context);
    assertThat(updated).isFalse();
    assertThat(issue.resolution()).isEqualTo("FIXED");
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_status() {
    boolean updated = updater.setStatus(issue, "OPEN", context);
    assertThat(updated).isTrue();
    assertThat(issue.status()).isEqualTo("OPEN");

    FieldDiffs.Diff diff = issue.currentChange().get(STATUS);
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("OPEN");
    assertThat(issue.mustSendNotifications()).isTrue();
  }

  @Test
  public void not_change_status() {
    issue.setStatus("CLOSED");
    boolean updated = updater.setStatus(issue, "CLOSED", context);
    assertThat(updated).isFalse();
    assertThat(issue.status()).isEqualTo("CLOSED");
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_new_attribute_value() {
    boolean updated = updater.setAttribute(issue, "JIRA", "FOO-123", context);
    assertThat(updated).isTrue();
    assertThat(issue.attribute("JIRA")).isEqualTo("FOO-123");
    assertThat(issue.currentChange().diffs()).hasSize(1);
    assertThat(issue.currentChange().get("JIRA").oldValue()).isNull();
    assertThat(issue.currentChange().get("JIRA").newValue()).isEqualTo("FOO-123");
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void unset_attribute() {
    issue.setAttribute("JIRA", "FOO-123");
    boolean updated = updater.setAttribute(issue, "JIRA", null, context);
    assertThat(updated).isTrue();
    assertThat(issue.attribute("JIRA")).isNull();
    assertThat(issue.currentChange().diffs()).hasSize(1);
    assertThat(issue.currentChange().get("JIRA").oldValue()).isEqualTo("FOO-123");
    assertThat(issue.currentChange().get("JIRA").newValue()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void not_update_attribute() {
    issue.setAttribute("JIRA", "FOO-123");
    boolean updated = updater.setAttribute(issue, "JIRA", "FOO-123", context);
    assertThat(updated).isFalse();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void plan_with_no_existing_plan() {
    ActionPlan newActionPlan = DefaultActionPlan.create("newName");

    boolean updated = updater.plan(issue, newActionPlan, context);
    assertThat(updated).isTrue();
    assertThat(issue.actionPlanKey()).isEqualTo(newActionPlan.key());

    FieldDiffs.Diff diff = issue.currentChange().get(ACTION_PLAN);
    assertThat(diff.oldValue()).isEqualTo(UNUSED);
    assertThat(diff.newValue()).isEqualTo("newName");
    assertThat(issue.mustSendNotifications()).isTrue();
  }

  @Test
  public void plan_with_existing_plan() {
    issue.setActionPlanKey("formerActionPlan");

    ActionPlan newActionPlan = DefaultActionPlan.create("newName").setKey("newKey");

    boolean updated = updater.plan(issue, newActionPlan, context);
    assertThat(updated).isTrue();
    assertThat(issue.actionPlanKey()).isEqualTo(newActionPlan.key());

    FieldDiffs.Diff diff = issue.currentChange().get(ACTION_PLAN);
    assertThat(diff.oldValue()).isEqualTo(UNUSED);
    assertThat(diff.newValue()).isEqualTo("newName");
    assertThat(issue.mustSendNotifications()).isTrue();
  }

  @Test
  public void unplan() {
    issue.setActionPlanKey("formerActionPlan");

    boolean updated = updater.plan(issue, null, context);
    assertThat(updated).isTrue();
    assertThat(issue.actionPlanKey()).isNull();

    FieldDiffs.Diff diff = issue.currentChange().get(ACTION_PLAN);
    assertThat(diff.oldValue()).isEqualTo(UNUSED);
    assertThat(diff.newValue()).isNull();
    assertThat(issue.mustSendNotifications()).isTrue();
  }

  @Test
  public void not_plan_again() {
    issue.setActionPlanKey("existingActionPlan");

    ActionPlan newActionPlan = DefaultActionPlan.create("existingActionPlan").setKey("existingActionPlan");

    boolean updated = updater.plan(issue, newActionPlan, context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_effort_to_fix() {
    boolean updated = updater.setEffortToFix(issue, 3.14, context);
    assertThat(updated).isTrue();
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.effortToFix()).isEqualTo(3.14);
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void not_set_effort_to_fix_if_unchanged() {
    issue.setEffortToFix(3.14);
    boolean updated = updater.setEffortToFix(issue, 3.14, context);
    assertThat(updated).isFalse();
    assertThat(issue.isChanged()).isFalse();
    assertThat(issue.effortToFix()).isEqualTo(3.14);
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_past_effort() {
    issue.setEffortToFix(3.14);
    boolean updated = updater.setPastEffortToFix(issue, 1.0, context);
    assertThat(updated).isTrue();
    assertThat(issue.effortToFix()).isEqualTo(3.14);

    // do not save change
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_past_technical_debt() {
    Duration newDebt = Duration.create(15 * 8 * 60);
    Duration previousDebt = Duration.create(10 * 8 * 60);
    issue.setDebt(newDebt);
    boolean updated = updater.setPastTechnicalDebt(issue, previousDebt, context);
    assertThat(updated).isTrue();
    assertThat(issue.debt()).isEqualTo(newDebt);
    assertThat(issue.mustSendNotifications()).isFalse();

    FieldDiffs.Diff diff = issue.currentChange().get(TECHNICAL_DEBT);
    assertThat(diff.oldValue()).isEqualTo(10L * 8 * 60);
    assertThat(diff.newValue()).isEqualTo(15L * 8 * 60);
  }

  @Test
  public void set_past_technical_debt_without_previous_value() {
    Duration newDebt = Duration.create(15 * 8 * 60);
    issue.setDebt(newDebt);
    boolean updated = updater.setPastTechnicalDebt(issue, null, context);
    assertThat(updated).isTrue();
    assertThat(issue.debt()).isEqualTo(newDebt);
    assertThat(issue.mustSendNotifications()).isFalse();

    FieldDiffs.Diff diff = issue.currentChange().get(TECHNICAL_DEBT);
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo(15L * 8 * 60);
  }

  @Test
  public void set_past_technical_debt_with_null_new_value() {
    issue.setDebt(null);
    Duration previousDebt = Duration.create(10 * 8 * 60);
    boolean updated = updater.setPastTechnicalDebt(issue, previousDebt, context);
    assertThat(updated).isTrue();
    assertThat(issue.debt()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();

    FieldDiffs.Diff diff = issue.currentChange().get(TECHNICAL_DEBT);
    assertThat(diff.oldValue()).isEqualTo(10L * 8 * 60);
    assertThat(diff.newValue()).isNull();
  }

  @Test
  public void set_message() {
    boolean updated = updater.setMessage(issue, "the message", context);
    assertThat(updated).isTrue();
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.message()).isEqualTo("the message");
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_past_message() {
    issue.setMessage("new message");
    boolean updated = updater.setPastMessage(issue, "past message", context);
    assertThat(updated).isTrue();
    assertThat(issue.message()).isEqualTo("new message");

    // do not save change
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_author() {
    boolean updated = updater.setAuthorLogin(issue, "eric", context);
    assertThat(updated).isTrue();
    assertThat(issue.authorLogin()).isEqualTo("eric");

    FieldDiffs.Diff diff = issue.currentChange().get("author");
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("eric");
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_project() {
    boolean updated = updater.setProject(issue, "sample", context);
    assertThat(updated).isTrue();
    assertThat(issue.projectKey()).isEqualTo("sample");

    // do not save change
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_past_project() {
    issue.setProjectKey("new project key");
    boolean updated = updater.setPastProject(issue, "past project key", context);
    assertThat(updated).isTrue();
    assertThat(issue.projectKey()).isEqualTo("new project key");

    // do not save change
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void not_set_past_project_if_no_change() {
    issue.setProjectKey("key");
    boolean updated = updater.setPastProject(issue, "key", context);
    assertThat(updated).isFalse();
    assertThat(issue.projectKey()).isEqualTo("key");
  }

}
