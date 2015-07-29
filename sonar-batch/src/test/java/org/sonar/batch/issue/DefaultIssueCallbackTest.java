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
package org.sonar.batch.issue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.sonar.batch.bootstrapper.IssueListener;
import org.junit.Before;
import com.google.common.collect.ImmutableList;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.api.issue.Issue;

import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class DefaultIssueCallbackTest {
  private IssueCache issueCache;
  private DefaultIssue issue;

  @Before
  public void setUp() {
    issue = new DefaultIssue();
    issue.setKey("key");

    issueCache = mock(IssueCache.class);
    when(issueCache.all()).thenReturn(ImmutableList.of(issue));
  }

  @Test
  public void testWithoutListener() {
    DefaultIssueCallback issueCallback = new DefaultIssueCallback(issueCache);
    issueCallback.execute();
  }

  @Test
  public void testWithListener() {
    final List<Issue> issueList = new LinkedList<>();
    IssueListener listener = new IssueListener() {
      @Override
      public void handle(Issue issue) {
        issueList.add(issue);
      }
    };

    DefaultIssueCallback issueCallback = new DefaultIssueCallback(issueCache, listener);
    issueCallback.execute();
    
    assertThat(issueList).containsExactly(issue);
  }

}
