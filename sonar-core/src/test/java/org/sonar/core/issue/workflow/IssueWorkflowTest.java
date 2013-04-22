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
import org.sonar.api.issue.IssueChange;
import org.sonar.core.issue.DefaultIssue;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class IssueWorkflowTest {
  IssueWorkflow workflow = new IssueWorkflow();

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
  public void should_list_available_transitions() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(Issue.STATUS_OPEN);
    List<Transition> transitions = workflow.availableTransitions(issue);
    assertThat(transitions).hasSize(3);
    assertThat(keys(transitions)).containsOnly("close", "falsepositive", "resolve");
  }

  @Test
  public void should_not_change_anything() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(Issue.STATUS_OPEN);
    workflow.change(issue, IssueChange.create());

    assertThat(issue.updatedAt()).isNull();
  }

  @Test
  public void should_set_fields() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(Issue.STATUS_OPEN);
    IssueChange change = IssueChange.create()
      .setAssignee("arthur")
      .setAttribute("JIRA", "FOO-1234")
      .setCost(4.2)
      .setLine(123)
      .setDescription("the desc")
      .setSeverity("BLOCKER");
    workflow.change(issue, change);

    assertThat(issue.updatedAt()).isNotNull();
    assertThat(issue.assignee()).isEqualTo("arthur");
    assertThat(issue.attribute("JIRA")).isEqualTo("FOO-1234");
    assertThat(issue.cost()).isEqualTo(4.2);
    assertThat(issue.line()).isEqualTo(123);
    assertThat(issue.description()).isEqualTo("the desc");
    assertThat(issue.severity()).isEqualTo("BLOCKER");
  }

  @Test
  public void should_change_only_fields_with_new_values() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue()
      .setStatus(Issue.STATUS_OPEN)
      .setAssignee("karadoc")
      .setAttribute("YOUTRACK", "ABC")
      .setCost(3.4);
    IssueChange change = IssueChange.create()
      .setAttribute("JIRA", "FOO-1234")
      .setLine(123)
      .setSeverity("BLOCKER");
    workflow.change(issue, change);

    assertThat(issue.updatedAt()).isNotNull();
    assertThat(issue.assignee()).isEqualTo("karadoc");
    assertThat(issue.attribute("JIRA")).isEqualTo("FOO-1234");
    assertThat(issue.attribute("YOUTRACK")).isEqualTo("ABC");
    assertThat(issue.cost()).isEqualTo(3.4);
    assertThat(issue.line()).isEqualTo(123);
    assertThat(issue.severity()).isEqualTo("BLOCKER");
  }

  @Test
  public void should_change_issue_state() throws Exception {
    workflow.start();

    DefaultIssue issue = new DefaultIssue().setStatus(Issue.STATUS_OPEN);
    IssueChange change = IssueChange.create().setTransition("close");
    workflow.change(issue, change);

    assertThat(issue.updatedAt()).isNotNull();
    assertThat(issue.resolution()).isEqualTo(Issue.RESOLUTION_FIXED);
    assertThat(issue.status()).isEqualTo(Issue.STATUS_CLOSED);
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
