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
package org.sonar.server.issue.workflow;

import com.google.common.collect.Collections2;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.IssueFieldsSetter;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByScanBuilder;

@RunWith(DataProviderRunner.class)
public class IssueWorkflowTest {

  private IssueFieldsSetter updater = new IssueFieldsSetter();

  private IssueWorkflow underTest = new IssueWorkflow(new FunctionExecutor(updater), updater);

  @Test
  public void list_statuses() {
    underTest.start();

    List<String> expectedStatus = new ArrayList<>();
    // issues statuses
    expectedStatus.addAll(Arrays.asList(STATUS_OPEN, STATUS_CONFIRMED, STATUS_REOPENED, STATUS_RESOLVED, STATUS_CLOSED));
    // hostpots statuses
    expectedStatus.addAll(Arrays.asList(STATUS_TO_REVIEW, STATUS_REVIEWED));

    assertThat(underTest.statusKeys()).containsExactlyInAnyOrder(expectedStatus.toArray(new String[]{}));
  }

  @Test
  public void list_out_transitions_from_status_open() {
    underTest.start();

    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_OPEN);
    List<Transition> transitions = underTest.outTransitions(issue);
    assertThat(keys(transitions)).containsOnly("confirm", "falsepositive", "resolve", "wontfix", "accept");
  }

  @Test
  public void list_out_transitions_from_status_confirmed() {
    underTest.start();

    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_CONFIRMED);
    List<Transition> transitions = underTest.outTransitions(issue);
    assertThat(keys(transitions)).containsOnly("unconfirm", "falsepositive", "resolve", "wontfix", "accept");
  }

  @Test
  public void list_out_transitions_from_status_resolved() {
    underTest.start();

    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_RESOLVED);
    List<Transition> transitions = underTest.outTransitions(issue);
    assertThat(keys(transitions)).containsOnly("reopen");
  }

  @Test
  public void list_out_transitions_from_status_reopen() {
    underTest.start();

    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_REOPENED);
    List<Transition> transitions = underTest.outTransitions(issue);
    assertThat(keys(transitions)).containsOnly("confirm", "resolve", "falsepositive", "wontfix", "accept");
  }

  @Test
  public void list_no_out_transition_from_status_closed() {
    underTest.start();

    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_CLOSED).setRuleKey(RuleKey.of("java", "R1  "));
    List<Transition> transitions = underTest.outTransitions(issue);
    assertThat(transitions).isEmpty();
  }

  @Test
  public void fail_if_unknown_status_when_listing_transitions() {
    underTest.start();

    DefaultIssue issue = new DefaultIssue().setStatus("xxx");
    try {
      underTest.outTransitions(issue);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Unknown status: xxx");
    }
  }

  @Test
  public void automatically_close_resolved_issue() {
    underTest.start();

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

  @Test
  @UseDataProvider("allStatusesLeadingToClosed")
  public void automatically_reopen_closed_issue_to_its_previous_status_from_changelog(String previousStatus) {
    DefaultIssue[] issues = Arrays.stream(SUPPORTED_RESOLUTIONS_FOR_UNCLOSING)
      .map(resolution -> {
        DefaultIssue issue = newClosedIssue(resolution);
        setStatusPreviousToClosed(issue, previousStatus);
        return issue;
      })
      .toArray(DefaultIssue[]::new);
    Date now = new Date();
    underTest.start();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());

      assertThat(issue.status()).isEqualTo(previousStatus);
      assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
      assertThat(issue.closeDate()).isNull();
      assertThat(issue.isChanged()).isTrue();
    });
  }

  @Test
  @UseDataProvider("allStatusesLeadingToClosed")
  public void automatically_reopen_closed_issue_to_most_recent_previous_status_from_changelog(String previousStatus) {
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
    underTest.start();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());

      assertThat(issue.status()).isEqualTo(previousStatus);
      assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
      assertThat(issue.closeDate()).isNull();
      assertThat(issue.isChanged()).isTrue();
    });
  }

  @Test
  @UseDataProvider("allResolutionsBeforeClosing")
  public void automatically_reopen_closed_issue_to_previous_resolution_from_changelog(String resolutionBeforeClosed) {
    String randomPreviousStatus = ALL_STATUSES_LEADING_TO_CLOSED[new Random().nextInt(ALL_STATUSES_LEADING_TO_CLOSED.length)];
    DefaultIssue[] issues = Arrays.stream(SUPPORTED_RESOLUTIONS_FOR_UNCLOSING)
      .map(resolution -> {
        DefaultIssue issue = newClosedIssue(resolution);
        addResolutionAndStatusChange(issue, new Date(), randomPreviousStatus, STATUS_CLOSED, resolutionBeforeClosed, resolution);
        return issue;
      })
      .toArray(DefaultIssue[]::new);
    Date now = new Date();
    underTest.start();

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
  public void automatically_reopen_closed_issue_to_no_resolution_if_no_previous_one_changelog() {
    String randomPreviousStatus = ALL_STATUSES_LEADING_TO_CLOSED[new Random().nextInt(ALL_STATUSES_LEADING_TO_CLOSED.length)];
    DefaultIssue[] issues = Arrays.stream(SUPPORTED_RESOLUTIONS_FOR_UNCLOSING)
      .map(resolution -> {
        DefaultIssue issue = newClosedIssue(resolution);
        setStatusPreviousToClosed(issue, randomPreviousStatus);
        return issue;
      })
      .toArray(DefaultIssue[]::new);
    Date now = new Date();
    underTest.start();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());

      assertThat(issue.status()).isEqualTo(randomPreviousStatus);
      assertThat(issue.resolution()).isNull();
      assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
      assertThat(issue.closeDate()).isNull();
      assertThat(issue.isChanged()).isTrue();
    });
  }

  @Test
  @UseDataProvider("allResolutionsBeforeClosing")
  public void automatically_reopen_closed_issue_to_previous_resolution_of_closing_the_issue_if_most_recent_of_all_resolution_changes(String resolutionBeforeClosed) {
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
    underTest.start();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());

      assertThat(issue.status()).isEqualTo(randomPreviousStatus);
      assertThat(issue.resolution()).isEqualTo(resolutionBeforeClosed);
      assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
      assertThat(issue.closeDate()).isNull();
      assertThat(issue.isChanged()).isTrue();
    });
  }

  @DataProvider
  public static Object[][] allResolutionsBeforeClosing() {
    return Arrays.stream(ALL_RESOLUTIONS_BEFORE_CLOSING)
      .map(t -> new Object[] {t})
      .toArray(Object[][]::new);
  }

  @Test
  public void do_not_automatically_reopen_closed_issue_which_have_no_previous_status_in_changelog() {
    DefaultIssue[] issues = Arrays.stream(SUPPORTED_RESOLUTIONS_FOR_UNCLOSING)
      .map(IssueWorkflowTest::newClosedIssue)
      .toArray(DefaultIssue[]::new);
    Date now = new Date();
    underTest.start();

    Arrays.stream(issues).forEach(issue -> {
      underTest.doAutomaticTransition(issue, issueChangeContextByScanBuilder(now).build());

      assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
      assertThat(issue.updateDate()).isNull();
    });
  }

  private static final String[] ALL_STATUSES_LEADING_TO_CLOSED = new String[]{STATUS_OPEN, STATUS_REOPENED, STATUS_CONFIRMED, STATUS_RESOLVED};
  private static final String[] ALL_RESOLUTIONS_BEFORE_CLOSING = new String[]{
    null,
    RESOLUTION_FIXED,
    RESOLUTION_WONT_FIX,
    RESOLUTION_FALSE_POSITIVE
  };

  private static final String[] SUPPORTED_RESOLUTIONS_FOR_UNCLOSING = new String[] {RESOLUTION_FIXED, RESOLUTION_REMOVED};

  @DataProvider
  public static Object[][] allStatusesLeadingToClosed() {
    return Arrays.stream(ALL_STATUSES_LEADING_TO_CLOSED)
      .map(t -> new Object[]{t})
      .toArray(Object[][]::new);
  }

  @Test
  public void close_open_dead_issue() {
    underTest.start();

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
  public void close_reopened_dead_issue() {
    underTest.start();

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
  public void close_confirmed_dead_issue() {
    underTest.start();

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
  public void fail_if_unknown_status_on_automatic_trans() {
    underTest.start();

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
  public void flag_as_false_positive() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_OPEN)
      .setRuleKey(RuleKey.of("java", "AvoidCycle"))
      .setAssigneeUuid("morgan");

    underTest.start();
    underTest.doManualTransition(issue, DefaultTransitions.FALSE_POSITIVE, issueChangeContextByScanBuilder(new Date()).build());

    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FALSE_POSITIVE);
    assertThat(issue.status()).isEqualTo(STATUS_RESOLVED);

    // should remove assignee
    assertThat(issue.assignee()).isNull();
  }

  @Test
  public void wont_fix() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_OPEN)
      .setRuleKey(RuleKey.of("java", "AvoidCycle"))
      .setAssigneeUuid("morgan");

    underTest.start();
    underTest.doManualTransition(issue, DefaultTransitions.WONT_FIX, issueChangeContextByScanBuilder(new Date()).build());

    assertThat(issue.resolution()).isEqualTo(RESOLUTION_WONT_FIX);
    assertThat(issue.status()).isEqualTo(STATUS_RESOLVED);

    // should remove assignee
    assertThat(issue.assignee()).isNull();
  }

  @Test
  public void doManualTransition_shouldTransitionToResolutionWontFix_whenAccepted() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_OPEN)
      .setRuleKey(RuleKey.of("java", "AvoidCycle"))
      .setAssigneeUuid("morgan");

    underTest.start();
    underTest.doManualTransition(issue, DefaultTransitions.ACCEPT, issueChangeContextByScanBuilder(new Date()).build());

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

  private Collection<String> keys(List<Transition> transitions) {
    return Collections2.transform(transitions, Transition::key);
  }
}
