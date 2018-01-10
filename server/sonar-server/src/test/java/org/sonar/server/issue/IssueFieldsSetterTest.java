/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.user.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.issue.IssueFieldsSetter.ASSIGNEE;
import static org.sonar.server.issue.IssueFieldsSetter.RESOLUTION;
import static org.sonar.server.issue.IssueFieldsSetter.SEVERITY;
import static org.sonar.server.issue.IssueFieldsSetter.STATUS;
import static org.sonar.server.issue.IssueFieldsSetter.TECHNICAL_DEBT;
import static org.sonar.server.issue.IssueFieldsSetter.UNUSED;

public class IssueFieldsSetterTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  DefaultIssue issue = new DefaultIssue();
  IssueChangeContext context = IssueChangeContext.createUser(new Date(), "emmerik");

  IssueFieldsSetter updater = new IssueFieldsSetter();

  @Test
  public void assign() {
    UserDto user = newUserDto().setLogin("emmerik").setName("Emmerik");

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
    UserDto user = newUserDto().setLogin("emmerik").setName("Emmerik");

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
    UserDto user = newUserDto().setLogin("morgan").setName("Morgan");

    issue.setAssignee("morgan");
    boolean updated = updater.assign(issue, user, context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_new_assignee() {
    boolean updated = updater.setNewAssignee(issue, "simon", context);
    assertThat(updated).isTrue();
    assertThat(issue.assignee()).isEqualTo("simon");
    assertThat(issue.mustSendNotifications()).isTrue();
    FieldDiffs.Diff diff = issue.currentChange().get(ASSIGNEE);
    assertThat(diff.oldValue()).isEqualTo(UNUSED);
    assertThat(diff.newValue()).isEqualTo("simon");
  }

  @Test
  public void not_set_new_assignee_if_new_assignee_is_null() {
    boolean updated = updater.setNewAssignee(issue, null, context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void fail_with_ISE_when_setting_new_assignee_on_already_assigned_issue() {
    issue.setAssignee("simon");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("It's not possible to update the assignee with this method, please use assign()");
    updater.setNewAssignee(issue, "julien", context);
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
  public void line_is_not_changed() {
    issue.setLine(123);
    boolean updated = updater.setLine(issue, 123);
    assertThat(updated).isFalse();
    assertThat(issue.line()).isEqualTo(123);
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void change_locations() {
    issue.setLocations("[1-3]");
    boolean updated = updater.setLocations(issue, "[1-4]");
    assertThat(updated).isTrue();
    assertThat(issue.getLocations().toString()).isEqualTo("[1-4]");
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void do_not_change_locations() {
    issue.setLocations("[1-3]");
    boolean updated = updater.setLocations(issue, "[1-3]");
    assertThat(updated).isFalse();
    assertThat(issue.getLocations().toString()).isEqualTo("[1-3]");
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_locations_for_the_first_time() {
    issue.setLocations(null);
    boolean updated = updater.setLocations(issue, "[1-4]");
    assertThat(updated).isTrue();
    assertThat(issue.getLocations().toString()).isEqualTo("[1-4]");
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
  public void set_effort_to_fix() {
    boolean updated = updater.setGap(issue, 3.14, context);
    assertThat(updated).isTrue();
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.effortToFix()).isEqualTo(3.14);
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void not_set_effort_to_fix_if_unchanged() {
    issue.setGap(3.14);
    boolean updated = updater.setGap(issue, 3.14, context);
    assertThat(updated).isFalse();
    assertThat(issue.isChanged()).isFalse();
    assertThat(issue.effortToFix()).isEqualTo(3.14);
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_past_effort() {
    issue.setGap(3.14);
    boolean updated = updater.setPastGap(issue, 1.0, context);
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
    issue.setEffort(newDebt);
    boolean updated = updater.setPastEffort(issue, previousDebt, context);
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
    issue.setEffort(newDebt);
    boolean updated = updater.setPastEffort(issue, null, context);
    assertThat(updated).isTrue();
    assertThat(issue.debt()).isEqualTo(newDebt);
    assertThat(issue.mustSendNotifications()).isFalse();

    FieldDiffs.Diff diff = issue.currentChange().get(TECHNICAL_DEBT);
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo(15L * 8 * 60);
  }

  @Test
  public void set_past_technical_debt_with_null_new_value() {
    issue.setEffort(null);
    Duration previousDebt = Duration.create(10 * 8 * 60);
    boolean updated = updater.setPastEffort(issue, previousDebt, context);
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
  public void set_new_author() {
    boolean updated = updater.setNewAuthor(issue, "simon", context);
    assertThat(updated).isTrue();

    FieldDiffs.Diff diff = issue.currentChange().get("author");
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("simon");
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void not_set_new_author_if_new_author_is_null() {
    boolean updated = updater.setNewAuthor(issue, null, context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void fail_with_ISE_when_setting_new_author_on_issue() {
    issue.setAuthorLogin("simon");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("It's not possible to update the author with this method, please use setAuthorLogin()");
    updater.setNewAuthor(issue, "julien", context);
  }

  @Test
  public void setIssueMoved_has_no_effect_if_component_uuid_is_not_changed() {
    String componentUuid = "a";
    issue.setComponentUuid(componentUuid);

    updater.setIssueMoved(issue, componentUuid, context);

    assertThat(issue.changes()).isEmpty();
    assertThat(issue.componentUuid()).isEqualTo(componentUuid);
    assertThat(issue.isChanged()).isFalse();
    assertThat(issue.updateDate()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void setIssueMoved_changes_componentUuid_adds_a_change() {
    String oldComponentUuid = "a";
    String newComponentUuid = "b";
    issue.setComponentUuid(oldComponentUuid);

    updater.setIssueMoved(issue, newComponentUuid, context);

    assertThat(issue.changes()).hasSize(1);
    FieldDiffs fieldDiffs = issue.changes().get(0);
    assertThat(fieldDiffs.creationDate()).isEqualTo(context.date());
    assertThat(fieldDiffs.diffs()).hasSize(1);
    Map.Entry<String, FieldDiffs.Diff> entry = fieldDiffs.diffs().entrySet().iterator().next();
    assertThat(entry.getKey()).isEqualTo("file");
    assertThat(entry.getValue().oldValue()).isEqualTo(oldComponentUuid);
    assertThat(entry.getValue().newValue()).isEqualTo(newComponentUuid);
    assertThat(issue.componentUuid()).isEqualTo(newComponentUuid);
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(context.date(), Calendar.SECOND));
  }
}
