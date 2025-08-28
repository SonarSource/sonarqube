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
package org.sonar.ce.task.projectanalysis.issue;

import org.junit.jupiter.api.Test;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleOpenIssuesCounterTest {

  @Test
  void getCount_shouldReturnZeroInitially() {
    SimpleOpenIssuesCounter counter = new SimpleOpenIssuesCounter();
    
    assertThat(counter.getCount()).isZero();
  }

  @Test
  void add_shouldCountOpenIssue() {
    SimpleOpenIssuesCounter counter = new SimpleOpenIssuesCounter();
    DefaultIssue issue = createIssueWithStatus(IssueStatus.OPEN);
    
    counter.add(issue);
    
    assertThat(counter.getCount()).isEqualTo(1);
  }

  @Test
  void add_shouldCountConfirmedIssue() {
    SimpleOpenIssuesCounter counter = new SimpleOpenIssuesCounter();
    DefaultIssue issue = createIssueWithStatus(IssueStatus.CONFIRMED);
    
    counter.add(issue);
    
    assertThat(counter.getCount()).isEqualTo(1);
  }

  @Test
  void add_shouldNotCountClosedIssue() {
    SimpleOpenIssuesCounter counter = new SimpleOpenIssuesCounter();
    DefaultIssue issue = createIssueWithStatus(IssueStatus.FIXED);
    
    counter.add(issue);
    
    assertThat(counter.getCount()).isZero();
  }

  @Test
  void add_shouldNotCountFalsePositiveIssue() {
    SimpleOpenIssuesCounter counter = new SimpleOpenIssuesCounter();
    DefaultIssue issue = createIssueWithStatus(IssueStatus.FALSE_POSITIVE);
    
    counter.add(issue);
    
    assertThat(counter.getCount()).isZero();
  }

  @Test
  void add_shouldNotCountAcceptedIssue() {
    SimpleOpenIssuesCounter counter = new SimpleOpenIssuesCounter();
    DefaultIssue issue = createIssueWithStatus(IssueStatus.ACCEPTED);
    
    counter.add(issue);
    
    assertThat(counter.getCount()).isZero();
  }

  @Test
  void add_shouldCountMultipleOpenAndConfirmedIssues() {
    SimpleOpenIssuesCounter counter = new SimpleOpenIssuesCounter();
    DefaultIssue openIssue = createIssueWithStatus(IssueStatus.OPEN);
    DefaultIssue confirmedIssue = createIssueWithStatus(IssueStatus.CONFIRMED);
    DefaultIssue closedIssue = createIssueWithStatus(IssueStatus.FIXED);
    
    counter.add(openIssue);
    counter.add(confirmedIssue);
    counter.add(closedIssue);
    
    assertThat(counter.getCount()).isEqualTo(2);
  }

  @Test
  void add_shouldAggregateFromAnotherCounter() {
    SimpleOpenIssuesCounter counter1 = new SimpleOpenIssuesCounter();
    SimpleOpenIssuesCounter counter2 = new SimpleOpenIssuesCounter();
    
    counter1.add(createIssueWithStatus(IssueStatus.OPEN));
    counter1.add(createIssueWithStatus(IssueStatus.CONFIRMED));
    
    counter2.add(createIssueWithStatus(IssueStatus.OPEN));
    
    counter1.add(counter2);
    
    assertThat(counter1.getCount()).isEqualTo(3);
    assertThat(counter2.getCount()).isEqualTo(1);
  }

  @Test
  void add_shouldAggregateFromEmptyCounter() {
    SimpleOpenIssuesCounter counter1 = new SimpleOpenIssuesCounter();
    SimpleOpenIssuesCounter counter2 = new SimpleOpenIssuesCounter();
    
    counter1.add(createIssueWithStatus(IssueStatus.OPEN));
    
    counter1.add(counter2);
    
    assertThat(counter1.getCount()).isEqualTo(1);
  }

  private DefaultIssue createIssueWithStatus(IssueStatus status) {
    DefaultIssue issue = new DefaultIssue();
    issue.setKey("issue-key");
    issue.setRuleKey(RuleKey.of("java", "S001"));
    issue.setStatus(status.name());
    return issue;
  }
}