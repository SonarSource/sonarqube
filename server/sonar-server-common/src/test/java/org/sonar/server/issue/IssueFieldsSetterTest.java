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
package org.sonar.server.issue;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Random;
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
import static org.junit.rules.ExpectedException.none;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.issue.IssueFieldsSetter.ASSIGNEE;
import static org.sonar.server.issue.IssueFieldsSetter.RESOLUTION;
import static org.sonar.server.issue.IssueFieldsSetter.SEVERITY;
import static org.sonar.server.issue.IssueFieldsSetter.STATUS;
import static org.sonar.server.issue.IssueFieldsSetter.TECHNICAL_DEBT;
import static org.sonar.server.issue.IssueFieldsSetter.UNUSED;

public class IssueFieldsSetterTest {

  @Rule
  public ExpectedException thrown = none();

  private DefaultIssue issue = new DefaultIssue();
  private IssueChangeContext context = IssueChangeContext.createUser(new Date(), "user_uuid");
  private IssueFieldsSetter underTest = new IssueFieldsSetter();

  @Test
  public void assign() {
    UserDto user = newUserDto().setLogin("emmerik").setName("Emmerik");

    boolean updated = underTest.assign(issue, user, context);
    assertThat(updated).isTrue();
    assertThat(issue.assignee()).isEqualTo(user.getUuid());
    assertThat(issue.mustSendNotifications()).isTrue();
    FieldDiffs.Diff diff = issue.currentChange().get(ASSIGNEE);
    assertThat(diff.oldValue()).isEqualTo(UNUSED);
    assertThat(diff.newValue()).isEqualTo(user.getName());
  }

  @Test
  public void unassign() {
    issue.setAssigneeUuid("user_uuid");
    boolean updated = underTest.assign(issue, null, context);
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

    issue.setAssigneeUuid("user_uuid");
    boolean updated = underTest.assign(issue, user, context);
    assertThat(updated).isTrue();
    assertThat(issue.assignee()).isEqualTo(user.getUuid());
    assertThat(issue.mustSendNotifications()).isTrue();
    FieldDiffs.Diff diff = issue.currentChange().get(ASSIGNEE);
    assertThat(diff.oldValue()).isEqualTo(UNUSED);
    assertThat(diff.newValue()).isEqualTo(user.getName());
  }

  @Test
  public void not_change_assignee() {
    UserDto user = newUserDto().setLogin("morgan").setName("Morgan");

    issue.setAssigneeUuid(user.getUuid());
    boolean updated = underTest.assign(issue, user, context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_new_assignee() {
    boolean updated = underTest.setNewAssignee(issue, "user_uuid", context);
    assertThat(updated).isTrue();
    assertThat(issue.assignee()).isEqualTo("user_uuid");
    assertThat(issue.mustSendNotifications()).isTrue();
    FieldDiffs.Diff diff = issue.currentChange().get(ASSIGNEE);
    assertThat(diff.oldValue()).isEqualTo(UNUSED);
    assertThat(diff.newValue()).isEqualTo("user_uuid");
  }

  @Test
  public void not_set_new_assignee_if_new_assignee_is_null() {
    boolean updated = underTest.setNewAssignee(issue, null, context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void fail_with_ISE_when_setting_new_assignee_on_already_assigned_issue() {
    issue.setAssigneeUuid("user_uuid");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("It's not possible to update the assignee with this method, please use assign()");
    underTest.setNewAssignee(issue, "another_user_uuid", context);
  }

  @Test
  public void set_severity() {
    boolean updated = underTest.setSeverity(issue, "BLOCKER", context);
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
    boolean updated = underTest.setPastSeverity(issue, "INFO", context);
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
    boolean updated = underTest.setSeverity(issue, "MINOR", context);

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
    boolean updated = underTest.setSeverity(issue, "MINOR", context);
    assertThat(updated).isFalse();
    assertThat(issue.mustSendNotifications()).isFalse();
    assertThat(issue.currentChange()).isNull();
  }

  @Test
  public void not_revert_manual_severity() {
    issue.setSeverity("MINOR").setManualSeverity(true);
    try {
      underTest.setSeverity(issue, "MAJOR", context);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Severity can't be changed");
    }
  }

  @Test
  public void set_manual_severity() {
    issue.setSeverity("BLOCKER");
    boolean updated = underTest.setManualSeverity(issue, "MINOR", context);

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
    boolean updated = underTest.setManualSeverity(issue, "MINOR", context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void unset_line() {
    int line = 1 + new Random().nextInt(500);
    issue.setLine(line);

    boolean updated = underTest.unsetLine(issue, context);

    assertThat(updated).isTrue();
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.line()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
    assertThat(issue.currentChange())
      .extracting(FieldDiffs::diffs)
      .hasSize(1);
    FieldDiffs.Diff diff = issue.currentChange().diffs().get("line");
    assertThat(diff.oldValue()).isEqualTo(line);
    assertThat(diff.newValue()).isEqualTo("");
  }

  @Test
  public void unset_line_has_no_effect_if_line_is_already_null() {
    issue.setLine(null);

    boolean updated = underTest.unsetLine(issue, context);

    assertThat(updated).isFalse();
    assertThat(issue.line()).isNull();
    assertThat(issue.isChanged()).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_past_line() {
    issue.setLine(42);

    boolean updated = underTest.setPastLine(issue, 123);

    assertThat(updated).isTrue();
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.line()).isEqualTo(42);
    assertThat(issue.mustSendNotifications()).isFalse();
    // do not save change
    assertThat(issue.currentChange()).isNull();
  }

  @Test
  public void set_past_line_has_no_effect_if_line_already_had_value() {
    issue.setLine(42);

    boolean updated = underTest.setPastLine(issue, 42);

    assertThat(updated).isFalse();
    assertThat(issue.isChanged()).isFalse();
    assertThat(issue.line()).isEqualTo(42);
    assertThat(issue.mustSendNotifications()).isFalse();
    // do not save change
    assertThat(issue.currentChange()).isNull();
  }

  @Test
  public void change_locations() {
    issue.setLocations("[1-3]");
    boolean updated = underTest.setLocations(issue, "[1-4]");
    assertThat(updated).isTrue();
    assertThat(issue.getLocations().toString()).isEqualTo("[1-4]");
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void do_not_change_locations() {
    issue.setLocations("[1-3]");
    boolean updated = underTest.setLocations(issue, "[1-3]");
    assertThat(updated).isFalse();
    assertThat(issue.getLocations().toString()).isEqualTo("[1-3]");
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_locations_for_the_first_time() {
    issue.setLocations(null);
    boolean updated = underTest.setLocations(issue, "[1-4]");
    assertThat(updated).isTrue();
    assertThat(issue.getLocations().toString()).isEqualTo("[1-4]");
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_resolution() {
    boolean updated = underTest.setResolution(issue, "OPEN", context);
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
    boolean updated = underTest.setResolution(issue, "FIXED", context);
    assertThat(updated).isFalse();
    assertThat(issue.resolution()).isEqualTo("FIXED");
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_status() {
    boolean updated = underTest.setStatus(issue, "OPEN", context);
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
    boolean updated = underTest.setStatus(issue, "CLOSED", context);
    assertThat(updated).isFalse();
    assertThat(issue.status()).isEqualTo("CLOSED");
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_new_attribute_value() {
    boolean updated = underTest.setAttribute(issue, "JIRA", "FOO-123", context);
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
    boolean updated = underTest.setAttribute(issue, "JIRA", null, context);
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
    boolean updated = underTest.setAttribute(issue, "JIRA", "FOO-123", context);
    assertThat(updated).isFalse();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_gap_to_fix() {
    boolean updated = underTest.setGap(issue, 3.14, context);
    assertThat(updated).isTrue();
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.gap()).isEqualTo(3.14);
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void not_set_gap_to_fix_if_unchanged() {
    issue.setGap(3.14);
    boolean updated = underTest.setGap(issue, 3.14, context);
    assertThat(updated).isFalse();
    assertThat(issue.isChanged()).isFalse();
    assertThat(issue.gap()).isEqualTo(3.14);
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_past_gap() {
    issue.setGap(3.14);
    boolean updated = underTest.setPastGap(issue, 1.0, context);
    assertThat(updated).isTrue();
    assertThat(issue.gap()).isEqualTo(3.14);

    // do not save change
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_past_technical_debt() {
    Duration newDebt = Duration.create(15 * 8 * 60);
    Duration previousDebt = Duration.create(10 * 8 * 60);
    issue.setEffort(newDebt);
    boolean updated = underTest.setPastEffort(issue, previousDebt, context);
    assertThat(updated).isTrue();
    assertThat(issue.effort()).isEqualTo(newDebt);
    assertThat(issue.mustSendNotifications()).isFalse();

    FieldDiffs.Diff diff = issue.currentChange().get(TECHNICAL_DEBT);
    assertThat(diff.oldValue()).isEqualTo(10L * 8 * 60);
    assertThat(diff.newValue()).isEqualTo(15L * 8 * 60);
  }

  @Test
  public void set_past_technical_debt_without_previous_value() {
    Duration newDebt = Duration.create(15 * 8 * 60);
    issue.setEffort(newDebt);
    boolean updated = underTest.setPastEffort(issue, null, context);
    assertThat(updated).isTrue();
    assertThat(issue.effort()).isEqualTo(newDebt);
    assertThat(issue.mustSendNotifications()).isFalse();

    FieldDiffs.Diff diff = issue.currentChange().get(TECHNICAL_DEBT);
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo(15L * 8 * 60);
  }

  @Test
  public void set_past_technical_debt_with_null_new_value() {
    issue.setEffort(null);
    Duration previousDebt = Duration.create(10 * 8 * 60);
    boolean updated = underTest.setPastEffort(issue, previousDebt, context);
    assertThat(updated).isTrue();
    assertThat(issue.effort()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();

    FieldDiffs.Diff diff = issue.currentChange().get(TECHNICAL_DEBT);
    assertThat(diff.oldValue()).isEqualTo(10L * 8 * 60);
    assertThat(diff.newValue()).isNull();
  }

  @Test
  public void set_message() {
    boolean updated = underTest.setMessage(issue, "the message", context);
    assertThat(updated).isTrue();
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.message()).isEqualTo("the message");
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_past_message() {
    issue.setMessage("new message");
    boolean updated = underTest.setPastMessage(issue, "past message", context);
    assertThat(updated).isTrue();
    assertThat(issue.message()).isEqualTo("new message");

    // do not save change
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_author() {
    boolean updated = underTest.setAuthorLogin(issue, "eric", context);
    assertThat(updated).isTrue();
    assertThat(issue.authorLogin()).isEqualTo("eric");

    FieldDiffs.Diff diff = issue.currentChange().get("author");
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("eric");
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void set_new_author() {
    boolean updated = underTest.setNewAuthor(issue, "simon", context);
    assertThat(updated).isTrue();

    FieldDiffs.Diff diff = issue.currentChange().get("author");
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("simon");
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void not_set_new_author_if_new_author_is_null() {
    boolean updated = underTest.setNewAuthor(issue, null, context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  public void fail_with_ISE_when_setting_new_author_on_issue() {
    issue.setAuthorLogin("simon");

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("It's not possible to update the author with this method, please use setAuthorLogin()");
    underTest.setNewAuthor(issue, "julien", context);
  }

  @Test
  public void setIssueMoved_has_no_effect_if_component_uuid_is_not_changed() {
    String componentUuid = "a";
    issue.setComponentUuid(componentUuid);

    underTest.setIssueMoved(issue, componentUuid, context);

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

    underTest.setIssueMoved(issue, newComponentUuid, context);

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
