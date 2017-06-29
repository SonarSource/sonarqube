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
package org.sonar.scanner.issue.ignore;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.scanner.issue.ignore.pattern.IssuePattern;
import org.sonar.scanner.issue.ignore.pattern.PatternMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IgnoreIssuesFilterTest {

  private PatternMatcher exclusionPatternMatcher;
  private IgnoreIssuesFilter ignoreFilter;
  private FilterableIssue issue;
  private IssueFilterChain chain;

  @Before
  public void init() {
    exclusionPatternMatcher = mock(PatternMatcher.class);
    issue = mock(FilterableIssue.class);
    chain = mock(IssueFilterChain.class);
    when(chain.accept(issue)).thenReturn(true);

    ignoreFilter = new IgnoreIssuesFilter(exclusionPatternMatcher);
  }

  @Test
  public void shouldPassToChainIfMatcherHasNoPatternForIssue() {
    when(exclusionPatternMatcher.getMatchingPattern(anyString(), any(RuleKey.class), any(Integer.class))).thenReturn(null);

    assertThat(ignoreFilter.accept(issue, chain)).isTrue();
  }

  @Test
  public void shouldAcceptOrRefuseIfMatcherHasPatternForIssue() {
    when(exclusionPatternMatcher.getMatchingPattern(anyString(), any(RuleKey.class), any(Integer.class))).thenReturn(mock(IssuePattern.class));

    assertThat(ignoreFilter.accept(issue, chain)).isFalse();
  }
}
