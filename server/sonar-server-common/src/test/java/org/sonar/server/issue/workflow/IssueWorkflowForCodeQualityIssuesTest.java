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
import static org.sonar.api.issue.Issue.STATUS_IN_SANDBOX;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByScanBuilder;
import static org.sonar.core.rule.RuleType.VULNERABILITY;

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
  void outTransitionsKeys_whenStatusOpen_shouldReturnCorrectTransitions() {
    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_OPEN);
    List<String> transitions = underTest.outTransitionsKeys(issue);
    assertThat(transitions).containsOnly("confirm", "falsepositive", "resolve", "wontfix", "accept");
  }

  @Test
  void outTransitionsKeys_whenStatusConfirmed_shouldReturnCorrectTransitions() {
    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_CONFIRMED);
    List<String> transitions = underTest.outTransitionsKeys(issue);
    assertThat(transitions).containsOnly("unconfirm", "falsepositive", "resolve", "wontfix", "accept");
  }

  @Test
  void outTransitionsKeys_whenStatusResolved_shouldReturnCorrectTransitions() {
    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_RESOLVED);
    List<String> transitions = underTest.outTransitionsKeys(issue);
    assertThat(transitions).containsOnly("reopen");
  }

  @Test
  void outTransitionsKeys_whenStatusReopened_shouldReturnCorrectTransitions() {
    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_REOPENED);
    List<String> transitions = underTest.outTransitionsKeys(issue);
    assertThat(transitions).containsOnly("confirm", "resolve", "falsepositive", "wontfix", "accept");
  }

  @Test
  void outTransitionsKeys_whenStatusClosed_shouldReturnEmpty() {
    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_CLOSED).setRuleKey(RuleKey.of("java", "R1  "));
    List<String> transitions = underTest.outTransitionsKeys(issue);
    assertThat(transitions).isEmpty();
  }

  @Test
  void outTransitionsKeys_whenUnknownStatus_shouldThrowException() {
    DefaultIssue issue = new DefaultIssue().setStatus("xxx");
    try {
      underTest.outTransitionsKeys(issue);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Unknown status: xxx");
    }
  }

  @Test
  void doAutomaticTransition_whenResolvedIssueBeingClosed_shouldCloseIssue() {
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
  void doAutomaticTransition_whenClosedIssueReopened_shouldRestorePreviousStatusFromChangelog(String previousStatus) {
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
  void doAutomaticTransition_whenClosedIssueReopened_shouldRestoreMostRecentPreviousStatusFromChangelog(String previousStatus) {
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
  void doAutomaticTransition_whenClosedIssueReopened_shouldRestorePreviousResolutionFromChangelog(String resolutionBeforeClosed) {
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
  void doAutomaticTransition_whenClosedIssueReopenedWithNoPreviousResolution_shouldSetNoResolution() {
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
  void doAutomaticTransition_whenClosedIssueReopened_shouldRestorePreviousResolutionIfMostRecent(String resolutionBeforeClosed) {
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
  void doAutomaticTransition_whenClosedIssueWithNoPreviousStatus_shouldNotReopen() {
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
  void doAutomaticTransition_whenTaintVulnerabilityFlowChanged_shouldReopenIssue() {
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
  void doAutomaticTransition_whenFlowChangedButNotTaintVulnerability_shouldNotReopen() {
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
  void doAutomaticTransition_whenTaintVulnerabilityFlowNotChanged_shouldNotReopen() {
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
  void doAutomaticTransition_whenOpenDeadIssue_shouldClose() {
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
  void doAutomaticTransition_whenReopenedDeadIssue_shouldClose() {
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
  void doAutomaticTransition_whenConfirmedDeadIssue_shouldClose() {
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
  void doAutomaticTransition_whenUnknownStatus_shouldThrowException() {
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
  void doManualTransition_whenFalsePositiveTransition_shouldResolveAsFalsePositive() {
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
  void doManualTransition_whenWontFixTransition_shouldResolveAsWontFix() {
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

  @Test
  void doManualTransition_shouldUseAutomaticReopenTransitionOnTaintVulnerability_whenMarkedAsResolvedButStillAlive() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("issue_key")
      .setRuleKey(RuleKey.of("xoo", "S001"))
      .setStatus(STATUS_RESOLVED)
      .setResolution(RESOLUTION_FIXED)
      .setLocationsChanged(true)
      .setNew(false)
      .setType(VULNERABILITY)
      .setBeingClosed(false);
    when(taintChecker.isTaintVulnerability(issue))
      .thenReturn(true);

    underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(new Date()).build());

    assertThat(issue.issueStatus()).isEqualTo(IssueStatus.OPEN);
    List<DefaultIssueComment> issueComments = issue.defaultIssueComments();
    assertThat(issueComments).isEmpty();
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

  @Test
  void outTransitionsKeys_whenStatusInSandbox_shouldReturnCorrectTransitions() {
    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_IN_SANDBOX);
    
    List<String> transitions = underTest.outTransitionsKeys(issue);
    
    assertThat(transitions).containsExactlyInAnyOrder(
      "reopen",
      "confirm",
      "resolve",
      "falsepositive",
      "accept",
      "wontfix"
    );
  }

  @Test
  void doManualTransition_whenInSandboxReopenTransition_shouldTransitionToOpen() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ISSUE-1")
      .setStatus(STATUS_IN_SANDBOX)
      .setResolution(null);
    Date now = new Date();
    
    boolean result = underTest.doManualTransition(issue, "reopen", issueChangeContextByScanBuilder(now).build());
    
    assertThat(result).isTrue();
    assertThat(issue.status()).isEqualTo(STATUS_OPEN);
    assertThat(issue.resolution()).isNull();
    assertThat(issue.issueStatus()).isEqualTo(IssueStatus.OPEN);
  }

  @Test
  void doManualTransition_whenInSandboxConfirmTransition_shouldTransitionToConfirmed() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ISSUE-1")
      .setStatus(STATUS_IN_SANDBOX)
      .setResolution(null);
    Date now = new Date();
    
    boolean result = underTest.doManualTransition(issue, "confirm", issueChangeContextByScanBuilder(now).build());
    
    assertThat(result).isTrue();
    assertThat(issue.status()).isEqualTo(STATUS_CONFIRMED);
    assertThat(issue.resolution()).isNull();
    assertThat(issue.issueStatus()).isEqualTo(IssueStatus.CONFIRMED);
  }

  @Test
  void doManualTransition_whenInSandboxResolveTransition_shouldTransitionToResolvedFixed() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ISSUE-1")
      .setStatus(STATUS_IN_SANDBOX)
      .setResolution(null);
    Date now = new Date();
    
    boolean result = underTest.doManualTransition(issue, "resolve", issueChangeContextByScanBuilder(now).build());
    
    assertThat(result).isTrue();
    assertThat(issue.status()).isEqualTo(STATUS_RESOLVED);
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.issueStatus()).isEqualTo(IssueStatus.FIXED);
  }

  @Test
  void doManualTransition_whenInSandboxFalsePositiveTransition_shouldTransitionToResolvedFalsePositive() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ISSUE-1")
      .setStatus(STATUS_IN_SANDBOX)
      .setResolution(null);
    Date now = new Date();
    
    boolean result = underTest.doManualTransition(issue, "falsepositive", issueChangeContextByScanBuilder(now).build());
    
    assertThat(result).isTrue();
    assertThat(issue.status()).isEqualTo(STATUS_RESOLVED);
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FALSE_POSITIVE);
    assertThat(issue.issueStatus()).isEqualTo(IssueStatus.FALSE_POSITIVE);
  }

  @Test
  void doManualTransition_whenInSandboxAcceptTransition_shouldTransitionToResolvedWontFix() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ISSUE-1")
      .setStatus(STATUS_IN_SANDBOX)
      .setResolution(null);
    Date now = new Date();
    
    boolean result = underTest.doManualTransition(issue, "accept", issueChangeContextByScanBuilder(now).build());
    
    assertThat(result).isTrue();
    assertThat(issue.status()).isEqualTo(STATUS_RESOLVED);
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_WONT_FIX);
    assertThat(issue.issueStatus()).isEqualTo(IssueStatus.ACCEPTED);
  }

  @Test
  void doManualTransition_whenInSandboxWontFixTransition_shouldTransitionToResolvedWontFix() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ISSUE-1")
      .setStatus(STATUS_IN_SANDBOX)
      .setResolution(null);
    Date now = new Date();
    
    boolean result = underTest.doManualTransition(issue, "wontfix", issueChangeContextByScanBuilder(now).build());
    
    assertThat(result).isTrue();
    assertThat(issue.status()).isEqualTo(STATUS_RESOLVED);
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_WONT_FIX);
    assertThat(issue.issueStatus()).isEqualTo(IssueStatus.ACCEPTED);
  }
}
