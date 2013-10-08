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

package org.sonar.plugins.core.issue.ignore;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilterChain;
import org.sonar.plugins.core.issue.ignore.pattern.ExclusionPatternInitializer;
import org.sonar.plugins.core.issue.ignore.pattern.IssuePattern;
import org.sonar.plugins.core.issue.ignore.pattern.PatternMatcher;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IgnoreIssuesFilterTest {

  private ExclusionPatternInitializer exclusionPatternInitializer;
  private PatternMatcher exclusionPatternMatcher;
  private IgnoreIssuesFilter ignoreFilter;
  private Issue issue;
  private IssueFilterChain chain;

  @Before
  public void init() {
    exclusionPatternMatcher = mock(PatternMatcher.class);
    exclusionPatternInitializer = mock(ExclusionPatternInitializer.class);
    when(exclusionPatternInitializer.getPatternMatcher()).thenReturn(exclusionPatternMatcher);
    issue = mock(Issue.class);
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
