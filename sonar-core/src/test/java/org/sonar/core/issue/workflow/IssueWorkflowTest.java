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
package org.sonar.core.issue.workflow;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class IssueWorkflowTest {


  IssueUpdater updater = new IssueUpdater();
  IssueWorkflow workflow = new IssueWorkflow(new FunctionExecutor(updater), updater);

  @Test
  public void should_init_state_machine() throws Exception {
    assertThat(workflow.machine()).isNull();
    workflow.start();
    assertThat(workflow.machine()).isNotNull();
    assertThat(workflow.machine().state(Issue.STATUS_OPEN)).isNotNull();
    assertThat(workflow.machine().state(Issue.STATUS_CONFIRMED)).isNotNull();
    assertThat(workflow.machine().state(Issue.STATUS_CLOSED)).isNotNull();
    assertThat(workflow.machine().state(Issue.STATUS_REOPENED)).isNotNull();
    assertThat(workflow.machine().state(Issue.STATUS_RESOLVED)).isNotNull();
    workflow.stop();
  }

  @Test
  public void should_list_statuses() throws Exception {
    workflow.start();
    // order is important for UI
    assertThat(workflow.statusKeys()).containsSequence(Issue.STATUS_OPEN, Issue.STATUS_CONFIRMED, Issue.STATUS_REOPENED, Issue.STATUS_RESOLVED, Issue.STATUS_CLOSED);
  }

  @Test
  public void should_list_out_manual_transitions_from_status_open() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(Issue.STATUS_OPEN);
    List<Transition> transitions = workflow.outTransitions(issue);
    assertThat(transitions).hasSize(3);
    assertThat(keys(transitions)).containsOnly("confirm", "falsepositive", "resolve");
  }

  @Test
  public void should_list_out_manual_transitions_from_status_confirmed() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(Issue.STATUS_CONFIRMED);
    List<Transition> transitions = workflow.outTransitions(issue);
    assertThat(transitions).hasSize(3);
    assertThat(keys(transitions)).containsOnly("unconfirm", "falsepositive", "resolve");
  }

  @Test
  public void should_list_out_manual_transitions_from_status_resolved() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(Issue.STATUS_RESOLVED);
    List<Transition> transitions = workflow.outTransitions(issue);
    assertThat(transitions).hasSize(1);
    assertThat(keys(transitions)).containsOnly("reopen");
  }

  @Test
  public void should_list_out_manual_transitions_from_status_reopen() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(Issue.STATUS_REOPENED);
    List<Transition> transitions = workflow.outTransitions(issue);
    assertThat(transitions).hasSize(3);
    assertThat(keys(transitions)).containsOnly("confirm", "resolve", "falsepositive");
  }

  @Test
  public void should_do_automatic_transition() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setResolution(Issue.RESOLUTION_FIXED)
      .setStatus(Issue.STATUS_RESOLVED)
      .setNew(false)
      .setAlive(false);
    Date now = new Date();
    workflow.doAutomaticTransition(issue, IssueChangeContext.createScan(now));
    assertThat(issue.resolution()).isEqualTo(Issue.RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(issue.closeDate()).isNotNull();
    assertThat(issue.updateDate()).isEqualTo(now);
  }

  @Test
  public void should_fail_if_unknown_status() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue()
      .setKey("ABCDE")
      .setResolution(Issue.RESOLUTION_FIXED)
      .setStatus("xxx")
      .setNew(false)
      .setAlive(false);
    try {
      workflow.doAutomaticTransition(issue, IssueChangeContext.createScan(new Date()));
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Unknown status: xxx [issue=ABCDE]");
    }

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
