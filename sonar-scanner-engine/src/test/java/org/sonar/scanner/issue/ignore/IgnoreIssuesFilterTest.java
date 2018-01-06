/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Test;
import org.mockito.Mockito;
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

  private PatternMatcher exclusionPatternMatcher = mock(PatternMatcher.class);
  private FilterableIssue issue = mock(FilterableIssue.class, Mockito.RETURNS_DEEP_STUBS);
  private IssueFilterChain chain = mock(IssueFilterChain.class);
  private IgnoreIssuesFilter underTest = new IgnoreIssuesFilter(exclusionPatternMatcher);

  @Test
  public void shouldPassToChainIfMatcherHasNoPatternForIssue() {
    when(exclusionPatternMatcher.getMatchingPattern(anyString(), any(RuleKey.class), any(Integer.class)))
      .thenReturn(null);
    when(chain.accept(issue)).thenReturn(true);
    assertThat(underTest.accept(issue, chain)).isTrue();
  }

  @Test
  public void shouldRejectIfPatternMatches() {
    IssuePattern pattern = mock(IssuePattern.class);
    when(exclusionPatternMatcher.getMatchingPattern(anyString(), any(RuleKey.class), any(Integer.class)))
      .thenReturn(pattern);

    assertThat(underTest.accept(issue, chain)).isFalse();
  }

}
