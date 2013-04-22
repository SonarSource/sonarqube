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
package org.sonar.api.issue;

import org.junit.Test;
import org.sonar.api.rule.Severity;

import static org.fest.assertions.Assertions.assertThat;

public class IssueChangeTest {
  @Test
  public void should_not_have_changes_by_default() throws Exception {
    IssueChange change = IssueChange.create();
    assertThat(change.hasChanges()).isFalse();
    assertThat(change.severity()).isNull();
    assertThat(change.isCostChanged()).isFalse();
    assertThat(change.cost()).isNull();
    assertThat(change.isAssigneeChanged()).isFalse();
    assertThat(change.assignee()).isNull();
    assertThat(change.isLineChanged()).isFalse();
    assertThat(change.line()).isNull();
    assertThat(change.comment()).isNull();
    assertThat(change.description()).isNull();
    assertThat(change.transition()).isNull();
    assertThat(change.manualSeverity()).isNull();
    assertThat(change.attributes()).isEmpty();
  }


  @Test
  public void should_change_line() {
    IssueChange change = IssueChange.create();
    change.setLine(123);
    assertThat(change.isLineChanged()).isTrue();
    assertThat(change.line()).isEqualTo(123);
  }

  @Test
  public void should_reset_line() {
    IssueChange change = IssueChange.create();
    assertThat(change.isLineChanged()).isFalse();
    assertThat(change.hasChanges()).isFalse();
    change.setLine(null);
    assertThat(change.isLineChanged()).isTrue();
    assertThat(change.hasChanges()).isTrue();
  }

  @Test
  public void should_change_cost() {
    IssueChange change = IssueChange.create();
    change.setCost(500.0);
    assertThat(change.isCostChanged()).isTrue();
    assertThat(change.cost()).isEqualTo(500.0);
  }

  @Test
  public void should_reset_cost() {
    IssueChange change = IssueChange.create();
    assertThat(change.isCostChanged()).isFalse();
    assertThat(change.hasChanges()).isFalse();
    change.setCost(null);
    assertThat(change.isCostChanged()).isTrue();
    assertThat(change.hasChanges()).isTrue();
  }

  @Test
  public void should_change_assignne() {
    IssueChange change = IssueChange.create();
    change.setAssignee("karadoc");
    assertThat(change.isAssigneeChanged()).isTrue();
    assertThat(change.assignee()).isEqualTo("karadoc");
  }

  @Test
  public void should_reset_assignee() {
    IssueChange change = IssueChange.create();
    assertThat(change.isAssigneeChanged()).isFalse();
    assertThat(change.hasChanges()).isFalse();
    change.setAssignee(null);
    assertThat(change.isAssigneeChanged()).isTrue();
    assertThat(change.hasChanges()).isTrue();
  }

  @Test
  public void should_change_message() {
    IssueChange change = IssueChange.create();
    change.setDescription("foo");
    assertThat(change.description()).isEqualTo("foo");
    assertThat(change.hasChanges()).isTrue();
  }

  @Test
  public void should_add_comment() {
    IssueChange change = IssueChange.create();
    change.setComment("foo").setLogin("perceval");
    assertThat(change.comment()).isEqualTo("foo");
    assertThat(change.login()).isEqualTo("perceval");
    assertThat(change.hasChanges()).isTrue();
  }

  @Test
  public void should_change_resolution() {
    IssueChange change = IssueChange.create();
    change.setTransition("resolve");
    assertThat(change.transition()).isEqualTo("resolve");
    assertThat(change.hasChanges()).isTrue();
  }

  @Test
  public void should_change_severity() {
    IssueChange change = IssueChange.create();
    change.setSeverity(Severity.INFO);
    assertThat(change.severity()).isEqualTo(Severity.INFO);
    assertThat(change.hasChanges()).isTrue();
  }

  @Test
  public void should_set_manual_severity() {
    IssueChange change = IssueChange.create();
    change.setManualSeverity(false);
    assertThat(change.manualSeverity()).isFalse();
    assertThat(change.hasChanges()).isTrue();
  }

  @Test
  public void should_set_attribute() {
    IssueChange change = IssueChange.create();
    change.setAttribute("JIRA", "FOO-1234");
    assertThat(change.attributes()).isNotEmpty();
    assertThat(change.attributes().get("JIRA")).isEqualTo("FOO-1234");
    assertThat(change.hasChanges()).isTrue();
  }

  @Test
  public void should_unset_attribute() {
    IssueChange change = IssueChange.create();
    change.setAttribute("JIRA", null);
    assertThat(change.attributes()).hasSize(1);
    assertThat(change.attributes().get("JIRA")).isNull();
    assertThat(change.attributes().containsKey("JIRA")).isTrue();
    assertThat(change.hasChanges()).isTrue();
  }
}
