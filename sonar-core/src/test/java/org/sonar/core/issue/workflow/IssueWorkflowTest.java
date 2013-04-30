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
import static org.mockito.Mockito.mock;

public class IssueWorkflowTest {
  IssueWorkflow workflow = new IssueWorkflow(new FunctionExecutor(new IssueUpdater()));

  @Test
  public void should_init_state_machine() throws Exception {
    assertThat(workflow.machine()).isNull();
    workflow.start();
    assertThat(workflow.machine()).isNotNull();
    assertThat(workflow.machine().state(Issue.STATUS_OPEN)).isNotNull();
    assertThat(workflow.machine().state(Issue.STATUS_CLOSED)).isNotNull();
    assertThat(workflow.machine().state(Issue.STATUS_REOPENED)).isNotNull();
    assertThat(workflow.machine().state(Issue.STATUS_RESOLVED)).isNotNull();
    workflow.stop();
  }

  @Test
  public void should_list_out_manual_transitions() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(Issue.STATUS_OPEN);
    List<Transition> transitions = workflow.outTransitions(issue);
    assertThat(transitions).hasSize(2);
    assertThat(keys(transitions)).containsOnly("falsepositive", "resolve");
  }

  @Test
  public void should_do_automatic_transition() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue()
      .setResolution(Issue.RESOLUTION_FIXED)
      .setStatus(Issue.STATUS_RESOLVED)
      .setNew(false)
      .setAlive(false);
    workflow.doAutomaticTransition(issue, IssueChangeContext.createScan(new Date()));
    assertThat(issue.resolution()).isEqualTo(Issue.RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(issue.closedAt()).isNotNull();
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
