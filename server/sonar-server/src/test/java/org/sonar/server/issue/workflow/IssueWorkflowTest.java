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
package org.sonar.server.issue.workflow;

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
import org.sonar.server.issue.IssueFieldsSetter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;

public class IssueWorkflowTest {

  IssueFieldsSetter updater = new IssueFieldsSetter();
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
    assertThat(workflow.statusKeys()).containsSubsequence(STATUS_OPEN, STATUS_CONFIRMED, STATUS_REOPENED, STATUS_RESOLVED, STATUS_CLOSED);
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

  private Collection<String> keys(List<Transition> transitions) {
    return Collections2.transform(transitions, new Function<Transition, String>() {
      @Override
      public String apply(@Nullable Transition transition) {
        return transition.key();
      }
    });
  }
}
