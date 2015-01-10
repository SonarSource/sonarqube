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

package org.sonar.batch.issue.ignore;

import org.sonar.batch.issue.ignore.EnforceIssuesFilter;
import org.sonar.batch.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.batch.issue.ignore.pattern.IssuePattern;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilterChain;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.WildcardPattern;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class EnforceIssuesFilterTest {

  private IssueInclusionPatternInitializer exclusionPatternInitializer;
  private EnforceIssuesFilter ignoreFilter;
  private Issue issue;
  private IssueFilterChain chain;

  @Before
  public void init() {
    exclusionPatternInitializer = mock(IssueInclusionPatternInitializer.class);
    issue = mock(Issue.class);
    chain = mock(IssueFilterChain.class);
    when(chain.accept(issue)).thenReturn(true);

    ignoreFilter = new EnforceIssuesFilter(exclusionPatternInitializer);
  }

  @Test
  public void shouldPassToChainIfNoConfiguredPatterns() {
    assertThat(ignoreFilter.accept(issue, chain)).isTrue();
    verify(chain).accept(issue);
  }

  @Test
  public void shouldPassToChainIfRuleDoesNotMatch() {
    String rule = "rule";
    RuleKey ruleKey = mock(RuleKey.class);
    when(ruleKey.toString()).thenReturn(rule);
    when(issue.ruleKey()).thenReturn(ruleKey);

    IssuePattern matching = mock(IssuePattern.class);
    WildcardPattern rulePattern = mock(WildcardPattern.class);
    when(matching.getRulePattern()).thenReturn(rulePattern);
    when(rulePattern.match(rule)).thenReturn(false);
    when(exclusionPatternInitializer.getMulticriteriaPatterns()).thenReturn(ImmutableList.of(matching));

    assertThat(ignoreFilter.accept(issue, chain)).isTrue();
    verify(chain).accept(issue);
  }

  @Test
  public void shouldAcceptIssueIfFullyMatched() {
    String rule = "rule";
    String path = "org/sonar/api/Issue.java";
    String componentKey = "org.sonar.api.Issue";
    RuleKey ruleKey = mock(RuleKey.class);
    when(ruleKey.toString()).thenReturn(rule);
    when(issue.ruleKey()).thenReturn(ruleKey);
    when(issue.componentKey()).thenReturn(componentKey);

    IssuePattern matching = mock(IssuePattern.class);
    WildcardPattern rulePattern = mock(WildcardPattern.class);
    when(matching.getRulePattern()).thenReturn(rulePattern);
    when(rulePattern.match(rule)).thenReturn(true);
    WildcardPattern pathPattern = mock(WildcardPattern.class);
    when(matching.getResourcePattern()).thenReturn(pathPattern);
    when(pathPattern.match(path)).thenReturn(true);
    when(exclusionPatternInitializer.getMulticriteriaPatterns()).thenReturn(ImmutableList.of(matching));
    when(exclusionPatternInitializer.getPathForComponent(componentKey)).thenReturn(path);

    assertThat(ignoreFilter.accept(issue, chain)).isTrue();
    verifyZeroInteractions(chain);
  }

  @Test
  public void shouldRefuseIssueIfRuleMatchesButNotPath() {
    String rule = "rule";
    String path = "org/sonar/api/Issue.java";
    String componentKey = "org.sonar.api.Issue";
    RuleKey ruleKey = mock(RuleKey.class);
    when(ruleKey.toString()).thenReturn(rule);
    when(issue.ruleKey()).thenReturn(ruleKey);
    when(issue.componentKey()).thenReturn(componentKey);

    IssuePattern matching = mock(IssuePattern.class);
    WildcardPattern rulePattern = mock(WildcardPattern.class);
    when(matching.getRulePattern()).thenReturn(rulePattern);
    when(rulePattern.match(rule)).thenReturn(true);
    WildcardPattern pathPattern = mock(WildcardPattern.class);
    when(matching.getResourcePattern()).thenReturn(pathPattern);
    when(pathPattern.match(path)).thenReturn(false);
    when(exclusionPatternInitializer.getMulticriteriaPatterns()).thenReturn(ImmutableList.of(matching));
    when(exclusionPatternInitializer.getPathForComponent(componentKey)).thenReturn(path);

    assertThat(ignoreFilter.accept(issue, chain)).isFalse();
    verifyZeroInteractions(chain);
  }

  @Test
  public void shouldRefuseIssueIfRuleMatchesAndPathUnknown() {
    String rule = "rule";
    String path = "org/sonar/api/Issue.java";
    String componentKey = "org.sonar.api.Issue";
    RuleKey ruleKey = mock(RuleKey.class);
    when(ruleKey.toString()).thenReturn(rule);
    when(issue.ruleKey()).thenReturn(ruleKey);
    when(issue.componentKey()).thenReturn(componentKey);

    IssuePattern matching = mock(IssuePattern.class);
    WildcardPattern rulePattern = mock(WildcardPattern.class);
    when(matching.getRulePattern()).thenReturn(rulePattern);
    when(rulePattern.match(rule)).thenReturn(true);
    WildcardPattern pathPattern = mock(WildcardPattern.class);
    when(matching.getResourcePattern()).thenReturn(pathPattern);
    when(pathPattern.match(path)).thenReturn(false);
    when(exclusionPatternInitializer.getMulticriteriaPatterns()).thenReturn(ImmutableList.of(matching));
    when(exclusionPatternInitializer.getPathForComponent(componentKey)).thenReturn(null);

    assertThat(ignoreFilter.accept(issue, chain)).isFalse();
    verifyZeroInteractions(chain);
  }
}
