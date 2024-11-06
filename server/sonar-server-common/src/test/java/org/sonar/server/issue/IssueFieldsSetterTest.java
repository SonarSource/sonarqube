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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultImpact;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.db.protobuf.DbIssues.MessageFormattingType;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserIdDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;
import static org.sonar.db.protobuf.DbIssues.MessageFormattingType.CODE;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.server.issue.IssueFieldsSetter.ASSIGNEE;
import static org.sonar.server.issue.IssueFieldsSetter.SEVERITY;
import static org.sonar.server.issue.IssueFieldsSetter.STATUS;
import static org.sonar.server.issue.IssueFieldsSetter.TECHNICAL_DEBT;
import static org.sonar.server.issue.IssueFieldsSetter.TYPE;
import static org.sonar.server.issue.IssueFieldsSetter.UNUSED;

class IssueFieldsSetterTest {

  private final String DEFAULT_RULE_DESCRIPTION_CONTEXT_KEY = "spring";

  private final DefaultIssue issue = new DefaultIssue();
  private final IssueChangeContext context = issueChangeContextByUserBuilder(new Date(), "user_uuid").build();
  private final IssueFieldsSetter underTest = new IssueFieldsSetter();

  @Test
  void assign() {
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
  void unassign() {
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
  void change_assignee() {
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
  void not_change_assignee() {
    UserDto user = newUserDto().setLogin("morgan").setName("Morgan");

    issue.setAssigneeUuid(user.getUuid());
    boolean updated = underTest.assign(issue, user, context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void set_new_assignee() {
    boolean updated = underTest.setNewAssignee(issue, new UserIdDto("user_uuid", "user_login"), context);
    assertThat(updated).isTrue();
    assertThat(issue.assignee()).isEqualTo("user_uuid");
    assertThat(issue.assigneeLogin()).isEqualTo("user_login");
    assertThat(issue.mustSendNotifications()).isTrue();
    FieldDiffs.Diff diff = issue.currentChange().get(ASSIGNEE);
    assertThat(diff.oldValue()).isEqualTo(UNUSED);
    assertThat(diff.newValue()).isEqualTo("user_uuid");
  }

  @Test
  void not_set_new_assignee_if_new_assignee_is_null() {
    boolean updated = underTest.setNewAssignee(issue, null, context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void fail_with_ISE_when_setting_new_assignee_on_already_assigned_issue() {
    issue.setAssigneeUuid("user_uuid");

    UserIdDto userId = new UserIdDto("another_user_uuid", "another_user_login");
    assertThatThrownBy(() -> underTest.setNewAssignee(issue, userId, context))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("It's not possible to update the assignee with this method, please use assign()");
  }

  @Test
  void set_type() {
    issue.setType(RuleType.CODE_SMELL);
    boolean updated = underTest.setType(issue, RuleType.BUG, context);
    assertThat(updated).isTrue();
    assertThat(issue.type()).isEqualTo(RuleType.BUG);
    assertThat(issue.manualSeverity()).isFalse();
    assertThat(issue.mustSendNotifications()).isFalse();

    FieldDiffs.Diff diff = issue.currentChange().get(TYPE);
    assertThat(diff.oldValue()).isEqualTo(RuleType.CODE_SMELL);
    assertThat(diff.newValue()).isEqualTo(RuleType.BUG);
  }

  @Test
  void set_severity() {
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
  void set_past_severity() {
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
  void update_severity() {
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
  void not_change_severity() {
    issue.setSeverity("MINOR");
    boolean updated = underTest.setSeverity(issue, "MINOR", context);
    assertThat(updated).isFalse();
    assertThat(issue.mustSendNotifications()).isFalse();
    assertThat(issue.currentChange()).isNull();
  }

  @Test
  void not_revert_manual_severity() {
    issue.setSeverity("MINOR").setManualSeverity(true);
    try {
      underTest.setSeverity(issue, "MAJOR", context);
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Severity can't be changed");
    }
  }

  @Test
  void set_manual_severity() {
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
  void not_change_manual_severity() {
    issue.setSeverity("MINOR").setManualSeverity(true);
    boolean updated = underTest.setManualSeverity(issue, "MINOR", context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void unset_line() {
    int line = 1 + new Random().nextInt(500);
    issue.setLine(line);

    boolean updated = underTest.unsetLine(issue, context);

    assertThat(updated).isTrue();
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.line()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
    assertThat(issue.currentChange())
      .extracting(f -> f.diffs().size())
      .isEqualTo(1);

    FieldDiffs.Diff diff = issue.currentChange().diffs().get("line");
    assertThat(diff.oldValue()).isEqualTo(line);
    assertThat(diff.newValue()).isEqualTo("");
  }

  @Test
  void unset_line_has_no_effect_if_line_is_already_null() {
    issue.setLine(null);

    boolean updated = underTest.unsetLine(issue, context);

    assertThat(updated).isFalse();
    assertThat(issue.line()).isNull();
    assertThat(issue.isChanged()).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void set_past_line() {
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
  void set_past_line_has_no_effect_if_line_already_had_value() {
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
  void change_locations_if_primary_text_rage_changed() {
    DbCommons.TextRange range = DbCommons.TextRange.newBuilder().setStartLine(1).build();
    DbIssues.Locations locations = DbIssues.Locations.newBuilder()
      .setTextRange(range)
      .build();
    DbIssues.Locations locations2 = locations.toBuilder().setTextRange(range.toBuilder().setEndLine(2).build()).build();
    issue.setLocations(locations);
    boolean updated = underTest.setLocations(issue, locations2);
    assertThat(updated).isTrue();
    assertThat((Object) issue.getLocations()).isEqualTo(locations2);
    assertThat(issue.locationsChanged()).isTrue();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void change_locations_if_secondary_text_rage_changed() {
    DbCommons.TextRange range = DbCommons.TextRange.newBuilder().setStartLine(1).build();
    DbIssues.Locations locations = DbIssues.Locations.newBuilder()
      .addFlow(DbIssues.Flow.newBuilder()
        .addLocation(DbIssues.Location.newBuilder().setTextRange(range))
        .build())
      .build();
    issue.setLocations(locations);
    DbIssues.Locations.Builder builder = locations.toBuilder();
    builder.getFlowBuilder(0).getLocationBuilder(0).setTextRange(range.toBuilder().setEndLine(2));
    boolean updated = underTest.setLocations(issue, builder.build());
    assertThat(updated).isTrue();
  }

  @Test
  void change_locations_if_secondary_message_changed() {
    DbIssues.Locations locations = DbIssues.Locations.newBuilder()
      .addFlow(DbIssues.Flow.newBuilder()
        .addLocation(DbIssues.Location.newBuilder().setMsg("msg1"))
        .build())
      .build();
    issue.setLocations(locations);
    DbIssues.Locations.Builder builder = locations.toBuilder();
    builder.getFlowBuilder(0).getLocationBuilder(0).setMsg("msg2");
    boolean updated = underTest.setLocations(issue, builder.build());
    assertThat(updated).isTrue();
  }

  @Test
  void change_locations_if_different_flow_count() {
    DbIssues.Locations locations = DbIssues.Locations.newBuilder()
      .addFlow(DbIssues.Flow.newBuilder()
        .addLocation(DbIssues.Location.newBuilder())
        .build())
      .build();
    issue.setLocations(locations);
    DbIssues.Locations.Builder builder = locations.toBuilder();
    builder.clearFlow();
    boolean updated = underTest.setLocations(issue, builder.build());
    assertThat(updated).isTrue();
  }

  @Test
  void do_not_change_locations_if_primary_hash_changed() {
    DbCommons.TextRange range = DbCommons.TextRange.newBuilder().setStartLine(1).build();
    DbIssues.Locations locations = DbIssues.Locations.newBuilder()
      .setTextRange(range)
      .setChecksum("1")
      .build();
    issue.setLocations(locations);
    boolean updated = underTest.setLocations(issue, locations.toBuilder().setChecksum("2").build());
    assertThat(updated).isFalse();
  }

  @Test
  void do_not_change_locations_if_secondary_hash_changed() {
    DbCommons.TextRange range = DbCommons.TextRange.newBuilder().setStartLine(1).build();
    DbIssues.Locations locations = DbIssues.Locations.newBuilder()
      .addFlow(DbIssues.Flow.newBuilder()
        .addLocation(DbIssues.Location.newBuilder().setTextRange(range))
        .build())
      .setChecksum("1")
      .build();
    issue.setLocations(locations);
    DbIssues.Locations.Builder builder = locations.toBuilder();
    builder.getFlowBuilder(0).getLocationBuilder(0).setChecksum("2");
    boolean updated = underTest.setLocations(issue, builder.build());
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void set_locations_for_the_first_time() {
    issue.setLocations(null);
    boolean updated = underTest.setLocations(issue, "[1-4]");
    assertThat(updated).isTrue();
    assertThat(issue.getLocations().toString()).isEqualTo("[1-4]");
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void setResolution_shouldNotTriggerFieldChange() {
    boolean updated = underTest.setResolution(issue, Issue.STATUS_OPEN, context);
    assertThat(updated).isTrue();
    assertThat(issue.resolution()).isEqualTo(Issue.STATUS_OPEN);

    FieldDiffs.Diff diff = issue.currentChange().get(IssueFieldsSetter.RESOLUTION);
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("OPEN");
    assertThat(issue.mustSendNotifications()).isTrue();
  }

  @Test
  void not_change_resolution() {
    issue.setResolution(Issue.RESOLUTION_FIXED);
    boolean updated = underTest.setResolution(issue, Issue.RESOLUTION_FIXED, context);
    assertThat(updated).isFalse();
    assertThat(issue.resolution()).isEqualTo(Issue.RESOLUTION_FIXED);
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void set_status() {
    boolean updated = underTest.setStatus(issue, Issue.STATUS_OPEN, context);
    assertThat(updated).isTrue();
    assertThat(issue.status()).isEqualTo(Issue.STATUS_OPEN);

    FieldDiffs.Diff diff = issue.currentChange().get(STATUS);
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo(Issue.STATUS_OPEN);
    assertThat(issue.mustSendNotifications()).isTrue();
  }

  @Test
  void setIssueStatus_shouldTriggerFieldChange() {
    issue.setResolution(null);
    issue.setStatus(Issue.STATUS_OPEN);

    IssueStatus issueStatus = issue.issueStatus();

    underTest.setResolution(issue, Issue.RESOLUTION_WONT_FIX, context);
    underTest.setStatus(issue, Issue.STATUS_RESOLVED, context);
    underTest.setIssueStatus(issue, issueStatus, issue.issueStatus(), context);

    FieldDiffs.Diff diff = issue.currentChange().diffs().get(IssueFieldsSetter.ISSUE_STATUS);
    assertThat(diff.oldValue()).isEqualTo(IssueStatus.OPEN);
    assertThat(diff.newValue()).isEqualTo(IssueStatus.ACCEPTED);
  }

  @Test
  void setIssueStatus_shouldNotTriggerFieldChange_whenNoChanges() {
    issue.setResolution(null);
    issue.setStatus(Issue.STATUS_OPEN);

    IssueStatus issueStatus = issue.issueStatus();
    underTest.setIssueStatus(issue, issueStatus, issue.issueStatus(), context);

    assertThat(issue.currentChange()).isNull();
  }

  @Test
  void setIssueStatus_shouldNotTriggerFieldChange_whenSecurityHotspot() {
    issue.setResolution(null);
    issue.setStatus(Issue.STATUS_TO_REVIEW);

    IssueStatus issueStatus = issue.issueStatus();

    issue.setResolution(Issue.RESOLUTION_SAFE);
    issue.setStatus(Issue.STATUS_REVIEWED);
    underTest.setIssueStatus(issue, issueStatus, issue.issueStatus(), context);

    assertThat(issue.currentChange()).isNull();
  }

  @Test
  void not_change_status() {
    issue.setStatus("CLOSED");
    boolean updated = underTest.setStatus(issue, "CLOSED", context);
    assertThat(updated).isFalse();
    assertThat(issue.status()).isEqualTo("CLOSED");
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void set_gap_to_fix() {
    boolean updated = underTest.setGap(issue, 3.14, context);
    assertThat(updated).isTrue();
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.gap()).isEqualTo(3.14);
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void not_set_gap_to_fix_if_unchanged() {
    issue.setGap(3.14);
    boolean updated = underTest.setGap(issue, 3.14, context);
    assertThat(updated).isFalse();
    assertThat(issue.isChanged()).isFalse();
    assertThat(issue.gap()).isEqualTo(3.14);
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void set_past_gap() {
    issue.setGap(3.14);
    boolean updated = underTest.setPastGap(issue, 1.0, context);
    assertThat(updated).isTrue();
    assertThat(issue.gap()).isEqualTo(3.14);

    // do not save change
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void set_past_technical_debt() {
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
  void set_past_technical_debt_without_previous_value() {
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
  void set_past_technical_debt_with_null_new_value() {
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
  void setCodeVariants_whenCodeVariantAdded_shouldBeUpdated() {
    Set<String> currentCodeVariants = new HashSet<>(Arrays.asList("linux"));
    Set<String> newCodeVariants = new HashSet<>(Arrays.asList("linux", "windows"));

    issue.setCodeVariants(newCodeVariants);
    boolean updated = underTest.setCodeVariants(issue, currentCodeVariants, context);
    assertThat(updated).isTrue();
    assertThat(issue.codeVariants()).contains("linux", "windows");

    FieldDiffs.Diff diff = issue.currentChange().get("code_variants");
    assertThat(diff.oldValue()).isEqualTo("linux");
    assertThat(diff.newValue()).isEqualTo("linux windows");
    assertThat(issue.mustSendNotifications()).isTrue();
  }

  @Test
  void setImpacts_whenImpactAdded_shouldBeUpdated() {
    Set<DefaultImpact> currentImpacts = Set.of(new DefaultImpact(SoftwareQuality.RELIABILITY, Severity.LOW, false));
    Set<DefaultImpact> newImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH, false));

    newImpacts
      .forEach(e -> issue.addImpact(e.softwareQuality(), e.severity(), e.manualSeverity()));
    boolean updated = underTest.setImpacts(issue, currentImpacts, context);
    assertThat(updated).isTrue();
    assertThat(issue.getImpacts()).isEqualTo(newImpacts);
  }

  @Test
  void setImpacts_whenImpactExists_shouldNotBeUpdated() {
    Set<DefaultImpact> currentImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW, false));
    Set<DefaultImpact> newImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW, false));

    newImpacts
      .forEach(e -> issue.addImpact(e.softwareQuality(), e.severity(), e.manualSeverity()));
    boolean updated = underTest.setImpacts(issue, currentImpacts, context);
    assertThat(updated).isFalse();
    assertThat(issue.getImpacts()).isEqualTo(newImpacts);
  }

  @Test
  void setImpacts_whenImpactExistsWithManualSeverity_shouldNotBeUpdated() {
    Set<DefaultImpact> currentImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH, true));
    Set<DefaultImpact> newImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW, false));

    newImpacts
      .forEach(e -> issue.addImpact(e.softwareQuality(), e.severity(), e.manualSeverity()));
    boolean updated = underTest.setImpacts(issue, currentImpacts, context);
    assertThat(updated).isFalse();
    assertThat(issue.getImpacts()).isEqualTo(currentImpacts);
  }

  @Test
  void setImpacts_whenImpactExistsWithManualSeverityAndNewImpact_shouldBeUpdated() {
    Set<DefaultImpact> currentImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH, true));
    Set<DefaultImpact> newImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW, false),
      new DefaultImpact(SoftwareQuality.RELIABILITY, Severity.HIGH, false));

    newImpacts
      .forEach(e -> issue.addImpact(e.softwareQuality(), e.severity(), e.manualSeverity()));
    boolean updated = underTest.setImpacts(issue, currentImpacts, context);
    assertThat(updated).isTrue();
    assertThat(issue.getImpacts()).isEqualTo(Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH, true),
      new DefaultImpact(SoftwareQuality.RELIABILITY, Severity.HIGH, false)));
  }

  @Test
  void setImpacts_whenIssueHasManualSeverityAndHasEquivalentImpact_ImpactShouldBeSetToManualSeverity() {
    Set<DefaultImpact> currentImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.HIGH, false));
    Set<DefaultImpact> newImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW, false));

    newImpacts
      .forEach(e -> issue.addImpact(e.softwareQuality(), e.severity(), e.manualSeverity()));
    issue.setManualSeverity(true);
    issue.setSeverity(org.sonar.api.rule.Severity.BLOCKER);
    issue.setType(RuleType.CODE_SMELL);
    boolean updated = underTest.setImpacts(issue, currentImpacts, context);
    assertThat(updated).isTrue();
    assertThat(issue.getImpacts()).isEqualTo(Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.BLOCKER, true)));
  }

  @Test
  void setImpacts_whenIssueHasManualSeverityAndHasNoEquivalentImpact_ImpactShouldNotBeUpdated() {
    Set<DefaultImpact> currentImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW, false));
    Set<DefaultImpact> newImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW, false));

    newImpacts
      .forEach(e -> issue.addImpact(e.softwareQuality(), e.severity(), e.manualSeverity()));
    issue.setManualSeverity(true);
    issue.setSeverity(org.sonar.api.rule.Severity.BLOCKER);
    issue.setType(RuleType.BUG);
    boolean updated = underTest.setImpacts(issue, currentImpacts, context);
    assertThat(updated).isFalse();
  }

  @Test
  void setImpacts_whenIssueHasManualSeverityAndImpactHasManualSeverity_ImpactShouldNotBeUpdated() {
    Set<DefaultImpact> currentImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW, true));
    Set<DefaultImpact> newImpacts = Set.of(new DefaultImpact(SoftwareQuality.MAINTAINABILITY, Severity.LOW, true));

    newImpacts
      .forEach(e -> issue.addImpact(e.softwareQuality(), e.severity(), e.manualSeverity()));
    issue.setManualSeverity(true);
    issue.setSeverity(org.sonar.api.rule.Severity.BLOCKER);
    issue.setType(RuleType.BUG);
    boolean updated = underTest.setImpacts(issue, currentImpacts, context);
    assertThat(updated).isFalse();
  }

  @Test
  void setCodeVariants_whenCodeVariantsUnchanged_shouldNotBeUpdated() {
    Set<String> currentCodeVariants = new HashSet<>(Arrays.asList("linux", "windows"));
    Set<String> newCodeVariants = new HashSet<>(Arrays.asList("windows", "linux"));

    issue.setCodeVariants(newCodeVariants);
    boolean updated = underTest.setCodeVariants(issue, currentCodeVariants, context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
  }

  @Test
  void set_message() {
    boolean updated = underTest.setMessage(issue, "the message", context);
    assertThat(updated).isTrue();
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.message()).isEqualTo("the message");
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void set_past_message() {
    issue.setMessage("new message");
    boolean updated = underTest.setPastMessage(issue, "past message", null, context);
    assertThat(updated).isTrue();
    assertThat(issue.message()).isEqualTo("new message");

    // do not save change
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void set_past_message_formatting() {
    issue.setMessage("past message");
    DbIssues.MessageFormattings newFormatting = formattings(formatting(0, 3, CODE));
    DbIssues.MessageFormattings pastFormatting = formattings(formatting(0, 7, CODE));
    issue.setMessageFormattings(newFormatting);
    boolean updated = underTest.setPastMessage(issue, "past message", pastFormatting, context);
    assertThat(updated).isTrue();
    assertThat(issue.message()).isEqualTo("past message");
    assertThat((DbIssues.MessageFormattings) issue.getMessageFormattings()).isEqualTo(newFormatting);

    // do not save change
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void set_past_message_formatting_no_changes() {
    issue.setMessage("past message");
    DbIssues.MessageFormattings sameFormatting = formattings(formatting(0, 3, CODE));
    issue.setMessageFormattings(sameFormatting);
    boolean updated = underTest.setPastMessage(issue, "past message", sameFormatting, context);
    assertThat(updated).isFalse();
    assertThat(issue.message()).isEqualTo("past message");
    assertThat((DbIssues.MessageFormattings) issue.getMessageFormattings()).isEqualTo(sameFormatting);

    // do not save change
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void message_formatting_different_size_is_changed() {
    issue.setMessageFormattings(formattings(formatting(0, 3, CODE)));
    boolean updated = underTest.setLocations(issue, formattings(formatting(0, 3, CODE), formatting(4, 6, CODE)));
    assertThat(updated).isTrue();
  }

  @Test
  void message_formatting_different_start_is_changed() {
    issue.setMessageFormattings(formattings(formatting(0, 3, CODE)));
    boolean updated = underTest.setLocations(issue, formattings(formatting(1, 3, CODE)));
    assertThat(updated).isTrue();
  }

  @Test
  void message_formatting_different_end_is_changed() {
    issue.setMessageFormattings(formattings(formatting(0, 3, CODE)));
    boolean updated = underTest.setLocations(issue, formattings(formatting(0, 4, CODE)));
    assertThat(updated).isTrue();
  }

  private static DbIssues.MessageFormatting formatting(int start, int end, MessageFormattingType type) {
    return DbIssues.MessageFormatting
      .newBuilder()
      .setStart(start)
      .setEnd(end)
      .setType(type)
      .build();
  }

  private static DbIssues.MessageFormattings formattings(DbIssues.MessageFormatting... messageFormatting) {
    return DbIssues.MessageFormattings.newBuilder()
      .addAllMessageFormatting(List.of(messageFormatting))
      .build();
  }

  @Test
  void set_author() {
    boolean updated = underTest.setAuthorLogin(issue, "eric", context);
    assertThat(updated).isTrue();
    assertThat(issue.authorLogin()).isEqualTo("eric");

    FieldDiffs.Diff diff = issue.currentChange().get("author");
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("eric");
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void set_new_author() {
    boolean updated = underTest.setNewAuthor(issue, "simon", context);
    assertThat(updated).isTrue();

    FieldDiffs.Diff diff = issue.currentChange().get("author");
    assertThat(diff.oldValue()).isNull();
    assertThat(diff.newValue()).isEqualTo("simon");
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void not_set_new_author_if_new_author_is_null() {
    boolean updated = underTest.setNewAuthor(issue, null, context);
    assertThat(updated).isFalse();
    assertThat(issue.currentChange()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void fail_with_ISE_when_setting_new_author_on_issue() {
    issue.setAuthorLogin("simon");

    assertThatThrownBy(() -> underTest.setNewAuthor(issue, "julien", context))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("It's not possible to update the author with this method, please use setAuthorLogin()");
  }

  @Test
  void setIssueComponent_has_no_effect_if_component_uuid_is_not_changed() {
    String componentKey = "key";
    String componentUuid = "uuid";

    issue.setComponentUuid(componentUuid);
    issue.setComponentKey(componentKey);

    underTest.setIssueComponent(issue, componentUuid, componentKey, context.date());

    assertThat(issue.componentUuid()).isEqualTo(componentUuid);
    assertThat(issue.componentKey()).isEqualTo(componentKey);
    assertThat(issue.isChanged()).isFalse();
    assertThat(issue.updateDate()).isNull();
    assertThat(issue.mustSendNotifications()).isFalse();
  }

  @Test
  void setIssueComponent_changes_component_uuid() {
    String oldComponentUuid = "a";
    String newComponentUuid = "b";
    String componentKey = "key";

    issue.setComponentUuid(oldComponentUuid);

    underTest.setIssueComponent(issue, newComponentUuid, componentKey, context.date());

    assertThat(issue.componentUuid()).isEqualTo(newComponentUuid);
    assertThat(issue.componentKey()).isEqualTo(componentKey);
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(context.date(), Calendar.SECOND));
  }

  @Test
  void setRuleDescriptionContextKey_setContextKeyIfPreviousValueIsNull() {
    issue.setRuleDescriptionContextKey(DEFAULT_RULE_DESCRIPTION_CONTEXT_KEY);
    boolean updated = underTest.setRuleDescriptionContextKey(issue, null);

    assertThat(updated).isTrue();
    assertThat(issue.getRuleDescriptionContextKey()).contains(DEFAULT_RULE_DESCRIPTION_CONTEXT_KEY);
  }

  @Test
  void setRuleDescriptionContextKey_dontSetContextKeyIfPreviousValueIsTheSame() {
    issue.setRuleDescriptionContextKey(DEFAULT_RULE_DESCRIPTION_CONTEXT_KEY);
    boolean updated = underTest.setRuleDescriptionContextKey(issue, DEFAULT_RULE_DESCRIPTION_CONTEXT_KEY);

    assertThat(updated).isFalse();
    assertThat(issue.getRuleDescriptionContextKey()).contains(DEFAULT_RULE_DESCRIPTION_CONTEXT_KEY);
  }

  @Test
  void setRuleDescriptionContextKey_dontSetContextKeyIfBothValuesAreNull() {
    issue.setRuleDescriptionContextKey(null);
    boolean updated = underTest.setRuleDescriptionContextKey(issue, null);

    assertThat(updated).isFalse();
    assertThat(issue.getRuleDescriptionContextKey()).isEmpty();
  }

  @Test
  void setRuleDescriptionContextKey_setContextKeyIfValuesAreDifferent() {
    issue.setRuleDescriptionContextKey(DEFAULT_RULE_DESCRIPTION_CONTEXT_KEY);
    boolean updated = underTest.setRuleDescriptionContextKey(issue, "hibernate");

    assertThat(updated).isTrue();
    assertThat(issue.getRuleDescriptionContextKey()).contains(DEFAULT_RULE_DESCRIPTION_CONTEXT_KEY);
  }

  @Test
  void setCleanCodeAttribute_whenCleanCodeAttributeChanged_shouldUpdateIssue() {
    issue.setCleanCodeAttribute(CleanCodeAttribute.CLEAR);
    boolean updated = underTest.setCleanCodeAttribute(issue, CleanCodeAttribute.COMPLETE, context);

    assertThat(updated).isTrue();
    assertThat(issue.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.CLEAR);
    assertThat(issue.currentChange().get("cleanCodeAttribute"))
      .extracting(FieldDiffs.Diff::oldValue, FieldDiffs.Diff::newValue)
      .containsExactly(CleanCodeAttribute.COMPLETE, CleanCodeAttribute.CLEAR.name());
  }

  @Test
  void setCleanCodeAttribute_whenCleanCodeAttributeNotChanged_shouldNotUpdateIssue() {
    issue.setCleanCodeAttribute(CleanCodeAttribute.CLEAR);
    boolean updated = underTest.setCleanCodeAttribute(issue, CleanCodeAttribute.CLEAR, context);

    assertThat(updated).isFalse();
    assertThat(issue.getCleanCodeAttribute()).isEqualTo(CleanCodeAttribute.CLEAR);
  }

  @Test
  void setPrioritizedRule_whenNotChanged_shouldNotUpdateIssue() {
    issue.setPrioritizedRule(true);
    underTest.setPrioritizedRule(issue, true, context);
    assertThat(issue.isChanged()).isFalse();
    assertThat(issue.isPrioritizedRule()).isTrue();
  }

  @Test
  void setPrioritizedRule_whenNotNewIssueChanged_shouldUpdateIssue() {
    issue.setPrioritizedRule(true);
    issue.setNew(false);
    underTest.setPrioritizedRule(issue, false, context);
    assertThat(issue.isChanged()).isTrue();
    assertThat(issue.isPrioritizedRule()).isFalse();
    assertThat(issue.getUpdateDate()).isNotNull();
  }

}
