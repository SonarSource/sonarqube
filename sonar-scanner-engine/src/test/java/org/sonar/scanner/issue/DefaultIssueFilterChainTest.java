/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.List;
import org.junit.Test;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class DefaultIssueFilterChainTest {
  private final FilterableIssue issue = mock(FilterableIssue.class);

  @Test
  public void should_accept_when_no_filter() {
    assertThat(new DefaultIssueFilterChain(List.of()).accept(issue)).isTrue();
  }

  static class PassingFilter implements IssueFilter {
    @Override
    public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
      return chain.accept(issue);
    }
  }

  static class AcceptingFilter implements IssueFilter {
    @Override
    public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
      return true;
    }
  }

  static class RefusingFilter implements IssueFilter {
    @Override
    public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
      return false;
    }
  }

  static class FailingFilter implements IssueFilter {
    @Override
    public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
      fail();
      return false;
    }

  }

  @Test
  public void should_accept_if_all_filters_pass() {
    assertThat(new DefaultIssueFilterChain(List.of(
      new PassingFilter(),
      new PassingFilter(),
      new PassingFilter())
      ).accept(issue)).isTrue();
  }

  @Test
  public void should_accept_and_not_go_further_if_filter_accepts() {
    assertThat(new DefaultIssueFilterChain(List.of(
      new PassingFilter(),
      new AcceptingFilter(),
      new FailingFilter())
      ).accept(issue)).isTrue();
  }

  @Test
  public void should_refuse_and_not_go_further_if_filter_refuses() {
    assertThat(new DefaultIssueFilterChain(List.of(
      new PassingFilter(),
      new RefusingFilter(),
      new FailingFilter())
      ).accept(issue)).isFalse();
  }
}
