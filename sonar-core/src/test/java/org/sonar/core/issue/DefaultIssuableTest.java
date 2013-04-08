/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.core.issue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueChange;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class DefaultIssuableTest {

  private org.sonar.api.issue.Issuable issuable;
  private List<Issue> issueList;

  @Before
  public void before(){
    issueList = newArrayList();
    issuable = new DefaultIssuable(issueList);
  }

  @Test
  public void should_apply_issue() throws Exception {
    Issue issue = new Issue.Builder()
        .ruleKey("ruleKey")
        .ruleRepositoryKey("ruleRepositoryKey")
        .severity(Issue.SEVERITY_BLOCKER)
        .status(Issue.STATUS_REOPENED)
        .resolution(Issue.RESOLUTION_FALSE_POSITIVE)
        .line(10)
        .componentKey("componentKey")
        .cost(10.0)
        .message("issue message")
        .userLogin("user")
        .assigneeLogin("user")
        .build();
    issueList.add(issue);

    IssueChange issueChangelog = new IssueChange.Builder()
        .severity(Issue.SEVERITY_MAJOR)
        .status(Issue.STATUS_CLOSED)
        .resolution(Issue.RESOLUTION_FIXED)
        .message("changelog message")
        .line(1)
        .build();

    Issue resultIssue = issuable.apply(issue, issueChangelog);
    assertThat(resultIssue).isNotNull();
    assertThat(resultIssue.key()).isNotEmpty();
    assertThat(resultIssue.severity()).isEqualTo(Issue.SEVERITY_MAJOR);
    assertThat(resultIssue.status()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(resultIssue.resolution()).isEqualTo(Issue.RESOLUTION_FIXED);
    assertThat(resultIssue.line()).isEqualTo(1);
  }

  @Test
  public void should_find_issues() throws Exception {
    Issue issue = new Issue.Builder() .build();
    issueList.add(issue);

    assertThat(issuable.issues()).hasSize(1);
  }

  @Test
  public void should_return_null_component() throws Exception {
    assertThat(issuable.component()).isNull();
  }
}
