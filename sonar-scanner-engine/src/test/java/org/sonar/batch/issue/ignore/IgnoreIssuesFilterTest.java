/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.issue.ignore;

import org.sonar.api.scan.issue.filter.FilterableIssue;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.batch.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.batch.issue.ignore.pattern.IssuePattern;
import org.sonar.batch.issue.ignore.pattern.PatternMatcher;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IgnoreIssuesFilterTest {

  private IssueExclusionPatternInitializer exclusionPatternInitializer;
  private PatternMatcher exclusionPatternMatcher;
  private IgnoreIssuesFilter ignoreFilter;
  private FilterableIssue issue;
  private IssueFilterChain chain;

  @Before
  public void init() {
    exclusionPatternMatcher = mock(PatternMatcher.class);
    exclusionPatternInitializer = mock(IssueExclusionPatternInitializer.class);
    when(exclusionPatternInitializer.getPatternMatcher()).thenReturn(exclusionPatternMatcher);
    issue = mock(FilterableIssue.class);
    chain = mock(IssueFilterChain.class);
    when(chain.accept(issue)).thenReturn(true);

    ignoreFilter = new IgnoreIssuesFilter(exclusionPatternInitializer);
  }

  @Test
  public void shouldPassToChainIfMatcherHasNoPatternForIssue() {
    when(exclusionPatternMatcher.getMatchingPattern(issue)).thenReturn(null);

    assertThat(ignoreFilter.accept(issue, chain)).isTrue();
  }

  @Test
  public void shouldAcceptOrRefuseIfMatcherHasPatternForIssue() {
    when(exclusionPatternMatcher.getMatchingPattern(issue)).thenReturn(mock(IssuePattern.class));

    assertThat(ignoreFilter.accept(issue, chain)).isFalse();
  }
}
