/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflow;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowActionsFactory;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowDefinition;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByScanBuilder;

class IssueWorkflowForCodeQualityIssuesTest {

  private static final String[] ALL_STATUSES_LEADING_TO_CLOSED = new String[] {STATUS_OPEN, STATUS_REOPENED, STATUS_CONFIRMED, STATUS_RESOLVED};
  private static final String[] ALL_RESOLUTIONS_BEFORE_CLOSING = new String[] {
    null,
    RESOLUTION_FIXED,
    RESOLUTION_WONT_FIX,
    RESOLUTION_FALSE_POSITIVE
  };
  private static final String[] SUPPORTED_RESOLUTIONS_FOR_UNCLOSING = new String[] {RESOLUTION_FIXED, RESOLUTION_REMOVED};

  private final IssueFieldsSetter updater = new IssueFieldsSetter();

  private final TaintChecker taintChecker = mock(TaintChecker.class);

  private final IssueWorkflow underTest = new IssueWorkflow(
    new CodeQualityIssueWorkflow(new CodeQualityIssueWorkflowActionsFactory(updater), new CodeQualityIssueWorkflowDefinition(), taintChecker), null);

  @Test
  void list_out_transitions_from_status_open() {
    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_OPEN);
    List<String> transitions = underTest.outTransitionsKeys(issue);
    assertThat(transitions).containsOnly("confirm", "falsepositive", "resolve", "wontfix", "accept");
  }

  @Test
  void list_out_transitions_from_status_confirmed() {
    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_CONFIRMED);
    List<String> transitions = underTest.outTransitionsKeys(issue);
    assertThat(transitions).containsOnly("unconfirm", "falsepositive", "resolve", "wontfix", "accept");
  }

  @Test
  void list_out_transitions_from_status_resolved() {
    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_RESOLVED);
    List<String> transitions = underTest.outTransitionsKeys(issue);
    assertThat(transitions).containsOnly("reopen");
  }

  @Test
  void list_out_transitions_from_status_reopen() {
    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_REOPENED);
    List<String> transitions = underTest.outTransitionsKeys(issue);
    assertThat(transitions).containsOnly("confirm", "resolve", "falsepositive", "wontfix", "accept");
  }

  @Test
  void list_no_out_transition_from_status_closed() {
    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_CLOSED).setRuleKey(RuleKey.of("java", "R1  "));
    List<String> transitions = underTest.outTransitionsKeys(issue);
    assertThat(transitions).isEmpty();
  }

  @Test
  void fail_if_unknown_status_when_listing_transitions() {
    DefaultIssue issue = new DefaultIssue().setStatus("xxx");
    try {
      underTest.outTransitionsKeys(issue);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Unknown status: xxx");
    }
  }

  @Test
  void automatically_close_resolved_issue() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(RuleKey.of("js", "S001"))
      .setResolution(RESOLUTION_FIXED)
      .setStatus(STATUS_RESOLVED)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();
    underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @ParameterizedTest
  @MethodSource("allStatusesLeadingToClosed")
  void automatically_reopen_closed_issue_to_its_previous_status_from_changelog(String previousStatus) {
    DefaultIssue[] issues = Arrays.stream(SUPPORTED_RESOLUTIONS_FOR_UNCLOSING)
      .map(resolution -> {
        DefaultIssue issue = newClosedIssue(resolution);
        setStatusPreviousToClosed(issue, previousStatus);
        return issue;
      })
      .toArray(DefaultIssue[]::new);
    Date now = new Date();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());

      assertThat(issue.status()).isEqualTo(previousStatus);
      assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
      assertThat(issue.closeDate()).isNull();
      assertThat(issue.isChanged()).isTrue();
    });
  }

  @ParameterizedTest
  @MethodSource("allStatusesLeadingToClosed")
  void automatically_reopen_closed_issue_to_most_recent_previous_status_from_changelog(String previousStatus) {
    DefaultIssue[] issues = Arrays.stream(SUPPORTED_RESOLUTIONS_FOR_UNCLOSING)
      .map(resolution -> {
        DefaultIssue issue = newClosedIssue(resolution);
        Date now = new Date();
        addStatusChange(issue, addDays(now, -60), STATUS_OPEN, STATUS_CONFIRMED);
        addStatusChange(issue, addDays(now, -10), STATUS_CONFIRMED, previousStatus);
        setStatusPreviousToClosed(issue, previousStatus);
        return issue;
      })
      .toArray(DefaultIssue[]::new);
    Date now = new Date();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());

      assertThat(issue.status()).isEqualTo(previousStatus);
      assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
      assertThat(issue.closeDate()).isNull();
      assertThat(issue.isChanged()).isTrue();
    });
  }

  @ParameterizedTest
  @MethodSource("allResolutionsBeforeClosing")
  void automatically_reopen_closed_issue_to_previous_resolution_from_changelog(String resolutionBeforeClosed) {
    String randomPreviousStatus = ALL_STATUSES_LEADING_TO_CLOSED[new Random().nextInt(ALL_STATUSES_LEADING_TO_CLOSED.length)];
    DefaultIssue[] issues = Arrays.stream(SUPPORTED_RESOLUTIONS_FOR_UNCLOSING)
      .map(resolution -> {
        DefaultIssue issue = newClosedIssue(resolution);
        addResolutionAndStatusChange(issue, new Date(), randomPreviousStatus, STATUS_CLOSED, resolutionBeforeClosed, resolution);
        return issue;
      })
      .toArray(DefaultIssue[]::new);
    Date now = new Date();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());

      assertThat(issue.status()).isEqualTo(randomPreviousStatus);
      assertThat(issue.resolution()).isEqualTo(resolutionBeforeClosed);
      assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
      assertThat(issue.closeDate()).isNull();
      assertThat(issue.isChanged()).isTrue();
    });
  }

  @Test
  void automatically_reopen_closed_issue_to_no_resolution_if_no_previous_one_changelog() {
    String randomPreviousStatus = ALL_STATUSES_LEADING_TO_CLOSED[new Random().nextInt(ALL_STATUSES_LEADING_TO_CLOSED.length)];
    DefaultIssue[] issues = Arrays.stream(SUPPORTED_RESOLUTIONS_FOR_UNCLOSING)
      .map(resolution -> {
        DefaultIssue issue = newClosedIssue(resolution);
        setStatusPreviousToClosed(issue, randomPreviousStatus);
        return issue;
      })
      .toArray(DefaultIssue[]::new);
    Date now = new Date();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());

      assertThat(issue.status()).isEqualTo(randomPreviousStatus);
      assertThat(issue.resolution()).isNull();
      assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
      assertThat(issue.closeDate()).isNull();
      assertThat(issue.isChanged()).isTrue();
    });
  }

  @ParameterizedTest
  @MethodSource("allResolutionsBeforeClosing")
  void automatically_reopen_closed_issue_to_previous_resolution_of_closing_the_issue_if_most_recent_of_all_resolution_changes(String resolutionBeforeClosed) {
    String randomPreviousStatus = ALL_STATUSES_LEADING_TO_CLOSED[new Random().nextInt(ALL_STATUSES_LEADING_TO_CLOSED.length)];
    DefaultIssue[] issues = Arrays.stream(SUPPORTED_RESOLUTIONS_FOR_UNCLOSING)
      .map(resolution -> {
        DefaultIssue issue = newClosedIssue(resolution);
        Date now = new Date();
        addResolutionChange(issue, addDays(now, -60), null, RESOLUTION_FALSE_POSITIVE);
        addResolutionChange(issue, addDays(now, -10), RESOLUTION_FALSE_POSITIVE, resolutionBeforeClosed);
        addResolutionAndStatusChange(issue, now, randomPreviousStatus, STATUS_CLOSED, resolutionBeforeClosed, resolution);
        return issue;
      })
      .toArray(DefaultIssue[]::new);
    Date now = new Date();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());

      assertThat(issue.status()).isEqualTo(randomPreviousStatus);
      assertThat(issue.resolution()).isEqualTo(resolutionBeforeClosed);
      assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
      assertThat(issue.closeDate()).isNull();
      assertThat(issue.isChanged()).isTrue();
    });
  }

  static Stream<String> allResolutionsBeforeClosing() {
    return Stream.of(ALL_RESOLUTIONS_BEFORE_CLOSING);
  }

  @Test
  void do_not_automatically_reopen_closed_issue_which_have_no_previous_status_in_changelog() {
    DefaultIssue[] issues = Arrays.stream(SUPPORTED_RESOLUTIONS_FOR_UNCLOSING)
      .map(IssueWorkflowForCodeQualityIssuesTest::newClosedIssue)
      .toArray(DefaultIssue[]::new);
    Date now = new Date();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());

      assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
      assertThat(issue.updateDate()).isNull();
    });
  }

  @Test
  void automatically_reopen_taint_vulnerability_when_flow_changed() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("issue_key")
      .setRuleKey(RuleKey.of("xoo", "S001"))
      .setStatus(STATUS_RESOLVED)
      .setResolution(RESOLUTION_FALSE_POSITIVE)
      .setLocationsChanged(true)
      .setNew(false);
    when(taintChecker.isTaintVulnerability(issue))
      .thenReturn(true);

    underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(new Date()).build());

    assertThat(issue.issueStatus()).isEqualTo(IssueStatus.OPEN);
    List<DefaultIssueComment> issueComments = issue.defaultIssueComments();
    assertThat(issueComments).hasSize(1);
    DefaultIssueComment defaultIssueComment = issueComments.get(0);
    assertThat(defaultIssueComment.markdownText()).isEqualTo("Automatically reopened because the vulnerability flow changed.");
  }

  @Test
  void do_not_automatically_reopen_issue_when_flow_changed_but_not_taint_vulnerability() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("issue_key")
      .setRuleKey(RuleKey.of("xoo", "S001"))
      .setStatus(STATUS_RESOLVED)
      .setResolution(RESOLUTION_FALSE_POSITIVE)
      .setLocationsChanged(true)
      .setNew(false);
    when(taintChecker.isTaintVulnerability(issue))
      .thenReturn(false);

    underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(new Date()).build());

    assertThat(issue.issueStatus()).isEqualTo(IssueStatus.FALSE_POSITIVE);
  }

  @Test
  void do_not_automatically_reopen_taint_vulnerability_when_flow_did_not_change() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("issue_key")
      .setRuleKey(RuleKey.of("xoo", "S001"))
      .setStatus(STATUS_RESOLVED)
      .setResolution(RESOLUTION_FALSE_POSITIVE)
      .setLocationsChanged(false)
      .setNew(false);
    when(taintChecker.isTaintVulnerability(issue))
      .thenReturn(true);

    underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(new Date()).build());

    assertThat(issue.issueStatus()).isEqualTo(IssueStatus.FALSE_POSITIVE);
  }

  static Stream<String> allStatusesLeadingToClosed() {
    return Stream.of(ALL_STATUSES_LEADING_TO_CLOSED);
  }

  @Test
  void close_open_dead_issue() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setResolution(null)
      .setStatus(STATUS_OPEN)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();
    underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @Test
  void close_reopened_dead_issue() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setResolution(null)
      .setStatus(STATUS_REOPENED)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();
    underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @Test
  void close_confirmed_dead_issue() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setResolution(null)
      .setStatus(STATUS_CONFIRMED)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();
    underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @Test
  void fail_if_unknown_status_on_automatic_trans() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setResolution(RESOLUTION_FIXED)
      .setStatus("xxx")
      .setNew(false)
      .setBeingClosed(true);

    IssueChangeContext issueChangeContext = issueChangeContextByScanBuilder(new Date()).build();
    assertThatThrownBy(() -> underTest.doAutomaticTransition(issue, issueChangeContext))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Unknown status: xxx [issue=ABCDE]");
  }

  @Test
  void flag_as_false_positive() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_OPEN)
      .setRuleKey(RuleKey.of("java", "AvoidCycle"))
      .setAssigneeUuid("morgan");

    underTest.doManualTransition(issue, CodeQualityIssueWorkflowTransition.FALSE_POSITIVE, issueChangeContextByScanBuilder(new Date()).build());

    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FALSE_POSITIVE);
    assertThat(issue.status()).isEqualTo(STATUS_RESOLVED);

    // should remove assignee
    assertThat(issue.assignee()).isNull();
  }

  @Test
  void wont_fix() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_OPEN)
      .setRuleKey(RuleKey.of("java", "AvoidCycle"))
      .setAssigneeUuid("morgan");

    underTest.doManualTransition(issue, CodeQualityIssueWorkflowTransition.WONT_FIX, issueChangeContextByScanBuilder(new Date()).build());

    assertThat(issue.resolution()).isEqualTo(RESOLUTION_WONT_FIX);
    assertThat(issue.status()).isEqualTo(STATUS_RESOLVED);

    // should remove assignee
    assertThat(issue.assignee()).isNull();
  }

  @Test
  void doManualTransition_shouldTransitionToResolutionWontFix_whenAccepted() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_OPEN)
      .setRuleKey(RuleKey.of("java", "AvoidCycle"))
      .setAssigneeUuid("morgan");

    underTest.doManualTransition(issue, CodeQualityIssueWorkflowTransition.ACCEPT, issueChangeContextByScanBuilder(new Date()).build());

    assertThat(issue.resolution()).isEqualTo(RESOLUTION_WONT_FIX);
    assertThat(issue.status()).isEqualTo(STATUS_RESOLVED);

    // should remove assignee
    assertThat(issue.assignee()).isNull();
  }

  private static DefaultIssue newClosedIssue(String resolution) {
    return new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(RuleKey.of("js", "S001"))
      .setResolution(resolution)
      .setStatus(STATUS_CLOSED)
      .setNew(false)
      .setCloseDate(new Date(5_999_999L));
  }

  private static void setStatusPreviousToClosed(DefaultIssue issue, String previousStatus) {
    addStatusChange(issue, new Date(), previousStatus, STATUS_CLOSED);
  }

  private static void addStatusChange(DefaultIssue issue, Date date, String previousStatus, String newStatus) {
    issue.addChange(new FieldDiffs().setCreationDate(date).setDiff("status", previousStatus, newStatus));
  }

  private void addResolutionChange(DefaultIssue issue, Date creationDate,
    @Nullable String previousResolution, @Nullable String newResolution) {
    checkArgument(previousResolution != null || newResolution != null, "At least one resolution must be non null");

    FieldDiffs fieldDiffs = new FieldDiffs().setCreationDate(creationDate)
      .setDiff("resolution", emptyIfNull(previousResolution), emptyIfNull(newResolution));
    issue.addChange(fieldDiffs);
  }

  private void addResolutionAndStatusChange(DefaultIssue issue, Date creationDate,
    String previousStatus, String newStatus,
    @Nullable String previousResolution, @Nullable String newResolution) {
    checkArgument(previousResolution != null || newResolution != null, "At least one resolution must be non null");

    FieldDiffs fieldDiffs = new FieldDiffs().setCreationDate(creationDate)
      .setDiff("status", previousStatus, newStatus)
      .setDiff("resolution", emptyIfNull(previousResolution), emptyIfNull(newResolution));
    issue.addChange(fieldDiffs);
  }

  static String emptyIfNull(@Nullable String newResolution) {
    return newResolution == null ? "" : newResolution;
  }

}
