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
package org.sonar.core.issue.workflow;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.rule.RuleKey.MANUAL_REPOSITORY_KEY;

public class IssueWorkflowTest {

  IssueUpdater updater = new IssueUpdater();
  IssueWorkflow workflow = new IssueWorkflow(new FunctionExecutor(updater), updater);

  @Test
  public void init_state_machine() {
    assertThat(workflow.machine()).isNull();
    workflow.start();
    assertThat(workflow.machine()).isNotNull();
    assertThat(workflow.machine().state(STATUS_OPEN)).isNotNull();
    assertThat(workflow.machine().state(STATUS_CONFIRMED)).isNotNull();
    assertThat(workflow.machine().state(STATUS_CLOSED)).isNotNull();
    assertThat(workflow.machine().state(STATUS_REOPENED)).isNotNull();
    assertThat(workflow.machine().state(STATUS_RESOLVED)).isNotNull();
    workflow.stop();
  }

  @Test
  public void list_statuses() {
    workflow.start();
    // order is important for UI
    assertThat(workflow.statusKeys()).containsSequence(STATUS_OPEN, STATUS_CONFIRMED, STATUS_REOPENED, STATUS_RESOLVED, STATUS_CLOSED);
  }

  @Test
  public void list_out_transitions_from_status_open() {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_OPEN);
    List<Transition> transitions = workflow.outTransitions(issue);
    assertThat(keys(transitions)).containsOnly("confirm", "falsepositive", "resolve", "wontfix");
  }

  @Test
  public void list_out_transitions_from_status_confirmed() {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_CONFIRMED);
    List<Transition> transitions = workflow.outTransitions(issue);
    assertThat(keys(transitions)).containsOnly("unconfirm", "falsepositive", "resolve", "wontfix");
  }

  @Test
  public void list_out_transitions_from_status_resolved() {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_RESOLVED);
    List<Transition> transitions = workflow.outTransitions(issue);
    assertThat(keys(transitions)).containsOnly("reopen");
  }

  @Test
  public void list_out_transitions_from_status_reopen() {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_REOPENED);
    List<Transition> transitions = workflow.outTransitions(issue);
    assertThat(keys(transitions)).containsOnly("confirm", "resolve", "falsepositive", "wontfix");
  }

  @Test
  public void list_no_out_transition_from_status_closed() {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(STATUS_CLOSED).setRuleKey(RuleKey.of("java", "R1  "));
    List<Transition> transitions = workflow.outTransitions(issue);
    assertThat(transitions).isEmpty();
  }

  @Test
  public void list_out_transitions_from_status_closed_on_manual_issue() {
    workflow.start();

    // Manual issue because of reporter
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_CLOSED)
      .setRuleKey(RuleKey.of("manual", "Performance"))
      .setReporter("simon");

    List<Transition> transitions = workflow.outTransitions(issue);
    assertThat(keys(transitions)).containsOnly("reopen");
  }

  @Test
  public void fail_if_unknown_status_when_listing_transitions() {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus("xxx");
    try {
      workflow.outTransitions(issue);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Unknown status: xxx");
    }
  }

  @Test
  public void automatically_close_resolved_issue() {
    workflow.start();

    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(RuleKey.of("js", "S001"))
      .setResolution(RESOLUTION_FIXED)
      .setStatus(STATUS_RESOLVED)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();
    workflow.doAutomaticTransition(issue, IssueChangeContext.createScan(now));
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @Test
  public void close_open_dead_issue() {
    workflow.start();

    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setResolution(null)
      .setStatus(STATUS_OPEN)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();
    workflow.doAutomaticTransition(issue, IssueChangeContext.createScan(now));
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @Test
  public void close_reopened_dead_issue() {
    workflow.start();

    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setResolution(null)
      .setStatus(STATUS_REOPENED)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();
    workflow.doAutomaticTransition(issue, IssueChangeContext.createScan(now));
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @Test
  public void close_confirmed_dead_issue() {
    workflow.start();

    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setResolution(null)
      .setStatus(STATUS_CONFIRMED)
      .setNew(false)
      .setBeingClosed(true);
    Date now = new Date();
    workflow.doAutomaticTransition(issue, IssueChangeContext.createScan(now));
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(DateUtils.truncate(now, Calendar.SECOND));
  }

  @Test
  public void fail_if_unknown_status_on_automatic_trans() {
    workflow.start();

    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setResolution(RESOLUTION_FIXED)
      .setStatus("xxx")
      .setNew(false)
      .setBeingClosed(true);
    try {
      workflow.doAutomaticTransition(issue, IssueChangeContext.createScan(new Date()));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Unknown status: xxx [issue=ABCDE]");
    }
  }

  @Test
  public void flag_as_false_positive() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_OPEN)
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setAssignee("morgan");

    workflow.start();
    workflow.doTransition(issue, DefaultTransitions.FALSE_POSITIVE, IssueChangeContext.createScan(new Date()));

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
      .setRuleKey(RuleKey.of("squid", "AvoidCycle"))
      .setAssignee("morgan");

    workflow.start();
    workflow.doTransition(issue, DefaultTransitions.WONT_FIX, IssueChangeContext.createScan(new Date()));

    assertThat(issue.resolution()).isEqualTo(RESOLUTION_WONT_FIX);
    assertThat(issue.status()).isEqualTo(STATUS_RESOLVED);

    // should remove assignee
    assertThat(issue.assignee()).isNull();
  }

  /**
   * User marks the manual issue as resolved -> issue is automatically
   * closed.
   */
  @Test
  public void automatically_close_resolved_manual_issue() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_OPEN)
      .setRuleKey(RuleKey.of(MANUAL_REPOSITORY_KEY, "Performance"));

    workflow.start();

    assertThat(workflow.outTransitions(issue)).containsOnly(
      Transition.create("confirm", "OPEN", "CONFIRMED"),
      Transition.create("resolve", "OPEN", "RESOLVED"),
      Transition.create("falsepositive", "OPEN", "RESOLVED"),
      Transition.create("wontfix", "OPEN", "RESOLVED"));

    workflow.doTransition(issue, "resolve", mock(IssueChangeContext.class));
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo("RESOLVED");

    assertThat(workflow.outTransitions(issue)).containsOnly(
      Transition.create("reopen", "RESOLVED", "REOPENED"));

    workflow.doAutomaticTransition(issue, mock(IssueChangeContext.class));
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
  }

  /**
   * Manual issue is fixed because the file does not exist anymore
   * or the tracking engine did not find the associated code
   * -> the issue is closed
   */
  @Test
  public void automatically_close_manual_issue_on_deleted_code() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_OPEN)
      .setRuleKey(RuleKey.of(MANUAL_REPOSITORY_KEY, "Performance"))
      .setBeingClosed(true);

    workflow.start();

    workflow.doAutomaticTransition(issue, mock(IssueChangeContext.class));
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
  }

  /**
   * Corner-case : the manual issue was marked as resolved by user but at the same 
   * time the file or the associated line was deleted.
   */
  @Test
  public void automatically_close_resolved_manual_issue_on_deleted_code() {
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setRuleKey(RuleKey.of(MANUAL_REPOSITORY_KEY, "Performance"))

    // resolved by user
      .setResolution(RESOLUTION_FIXED)
      .setStatus(STATUS_RESOLVED)

    // but unmatched by tracking engine
      .setBeingClosed(true);

    workflow.start();

    workflow.doAutomaticTransition(issue, mock(IssueChangeContext.class));
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
  }

  @Test
  public void manual_issues_be_confirmed_then_kept_open() {
    // Manual issue because of reporter
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_OPEN)
      .setRuleKey(RuleKey.of("manual", "Performance"))
      .setReporter("simon");

    workflow.start();

    assertThat(workflow.outTransitions(issue)).containsOnly(
      Transition.create("confirm", "OPEN", "CONFIRMED"),
      Transition.create("resolve", "OPEN", "RESOLVED"),
      Transition.create("falsepositive", "OPEN", "RESOLVED"),
      Transition.create("wontfix", "OPEN", "RESOLVED"));

    workflow.doTransition(issue, "confirm", mock(IssueChangeContext.class));
    assertThat(issue.resolution()).isNull();
    assertThat(issue.status()).isEqualTo("CONFIRMED");

    assertThat(workflow.outTransitions(issue)).containsOnly(
      Transition.create("unconfirm", "CONFIRMED", "REOPENED"),
      Transition.create("resolve", "CONFIRMED", "RESOLVED"),
      Transition.create("falsepositive", "CONFIRMED", "RESOLVED"),
      Transition.create("wontfix", "CONFIRMED", "RESOLVED"));

    // keep confirmed and unresolved
    workflow.doAutomaticTransition(issue, mock(IssueChangeContext.class));
    assertThat(issue.resolution()).isNull();
    assertThat(issue.status()).isEqualTo("CONFIRMED");

    // unconfirm
    workflow.doTransition(issue, "unconfirm", mock(IssueChangeContext.class));
    assertThat(issue.resolution()).isNull();
    assertThat(issue.status()).isEqualTo("REOPENED");
  }

  @Test
  public void manual_issue_on_removed_rule_be_closed() {
    // Manual issue because of reporter
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_OPEN)
      .setRuleKey(RuleKey.of("manual", "Performance"))
      .setReporter("simon")
      .setBeingClosed(true)
      .setOnDisabledRule(true);

    workflow.start();

    workflow.doAutomaticTransition(issue, mock(IssueChangeContext.class));
    assertThat(issue.resolution()).isEqualTo("REMOVED");
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
  }

  @Test
  public void manual_issue_on_removed_component_be_closed() {
    // Manual issue because of reporter
    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setStatus(STATUS_OPEN)
      .setRuleKey(RuleKey.of("manual", "Performance"))
      .setReporter("simon")
      .setBeingClosed(true)
      .setOnDisabledRule(false);

    workflow.start();

    workflow.doAutomaticTransition(issue, mock(IssueChangeContext.class));
    assertThat(issue.resolution()).isEqualTo(RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(STATUS_CLOSED);
  }

  private Collection<String> keys(List<Transition> transitions) {
    return Collections2.transform(transitions, new Function<Transition, String>() {
      @Override
      public String apply(@Nullable Transition transition) {
        return transition.key();
      }
    });
  }
}
