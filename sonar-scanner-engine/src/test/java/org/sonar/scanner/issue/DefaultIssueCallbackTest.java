/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.issue;

import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.bootstrapper.IssueListener;
import org.sonar.batch.bootstrapper.IssueListener.Issue;
import org.sonar.scanner.issue.tracking.TrackedIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultIssueCallbackTest {
  @Mock
  private IssueCache issueCache;
  @Mock
  private Rules rules;

  private TrackedIssue issue;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    RuleKey ruleKey = RuleKey.of("repo", "key");
    issue = new TrackedIssue();
    issue.setKey("key");
    issue.setAssignee("user");
    issue.setRuleKey(ruleKey);

    when(issueCache.all()).thenReturn(ImmutableList.of(issue));

    Rule r = mock(Rule.class);
    when(r.name()).thenReturn("rule name");
    when(rules.find(ruleKey)).thenReturn(r);
  }

  @Test
  public void testWithoutListener() {
    DefaultIssueCallback issueCallback = new DefaultIssueCallback(issueCache, rules);
    issueCallback.execute();
  }

  @Test
  public void testWithListener() {
    final List<IssueListener.Issue> issueList = new LinkedList<>();
    IssueListener listener = new IssueListener() {
      @Override
      public void handle(Issue issue) {
        issueList.add(issue);
      }
    };

    DefaultIssueCallback issueCallback = new DefaultIssueCallback(issueCache, listener, rules);
    issueCallback.execute();

    assertThat(issueList).hasSize(1);
    Issue callbackIssue = issueList.get(0);

    assertThat(callbackIssue.getAssigneeName()).isEqualTo("user");
    assertThat(callbackIssue.getRuleName()).isEqualTo("rule name");
  }

  @Test
  public void testWithNulls() {
    final List<IssueListener.Issue> issueList = new LinkedList<>();
    IssueListener listener = new IssueListener() {
      @Override
      public void handle(Issue issue) {
        issueList.add(issue);
      }
    };

    issue.setKey(null);
    issue.setAssignee(null);

    DefaultIssueCallback issueCallback = new DefaultIssueCallback(issueCache, listener, rules);
    issueCallback.execute();
  }

  @Test
  public void testDecorationNotFound() {
    final List<IssueListener.Issue> issueList = new LinkedList<>();
    IssueListener listener = new IssueListener() {
      @Override
      public void handle(Issue issue) {
        issueList.add(issue);
      }
    };

    when(rules.find(any(RuleKey.class))).thenReturn(null);

    DefaultIssueCallback issueCallback = new DefaultIssueCallback(issueCache, listener, rules);
    issueCallback.execute();
  }
}
