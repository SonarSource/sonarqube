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

import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilter;
import org.sonar.api.issue.batch.IssueFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class DefaultIssueFilterChainTest {

  private final Issue issue = mock(Issue.class);

  @Test
  public void should_accept_when_no_filter() {
    assertThat(new DefaultIssueFilterChain().accept(issue)).isTrue();
  }

  class PassingFilter implements IssueFilter {
    @Override
    public boolean accept(Issue issue, IssueFilterChain chain) {
      return chain.accept(issue);
    }
  }

  class AcceptingFilter implements IssueFilter {
    @Override
    public boolean accept(Issue issue, IssueFilterChain chain) {
      return true;
    }
  }

  class RefusingFilter implements IssueFilter {
    @Override
    public boolean accept(Issue issue, IssueFilterChain chain) {
      return false;
    }
  }

  class FailingFilter implements IssueFilter {
    @Override
    public boolean accept(Issue issue, IssueFilterChain chain) {
      fail();
      return false;
    }

  }

  @Test
  public void should_accept_if_all_filters_pass() {
    assertThat(new DefaultIssueFilterChain(
      new PassingFilter(),
      new PassingFilter(),
      new PassingFilter()
      ).accept(issue)).isTrue();
  }

  @Test
  public void should_accept_and_not_go_further_if_filter_accepts() {
    assertThat(new DefaultIssueFilterChain(
      new PassingFilter(),
      new AcceptingFilter(),
      new FailingFilter()
      ).accept(issue)).isTrue();
  }

  @Test
  public void should_refuse_and_not_go_further_if_filter_refuses() {
    assertThat(new DefaultIssueFilterChain(
      new PassingFilter(),
      new RefusingFilter(),
      new FailingFilter()
      ).accept(issue)).isFalse();
  }
}
