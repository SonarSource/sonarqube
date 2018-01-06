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

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.scanner.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.IssuePattern;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class EnforceIssuesFilterTest {

  private IssueInclusionPatternInitializer exclusionPatternInitializer;
  private InputComponentStore inputComponentStore;
  private EnforceIssuesFilter ignoreFilter;
  private FilterableIssue issue;
  private IssueFilterChain chain;

  @Before
  public void init() {
    inputComponentStore = mock(InputComponentStore.class);
    exclusionPatternInitializer = mock(IssueInclusionPatternInitializer.class);
    issue = mock(FilterableIssue.class);
    chain = mock(IssueFilterChain.class);
    when(chain.accept(issue)).thenReturn(true);
  }

  @Test
  public void shouldPassToChainIfNoConfiguredPatterns() {
    ignoreFilter = new EnforceIssuesFilter(exclusionPatternInitializer, inputComponentStore);
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

    ignoreFilter = new EnforceIssuesFilter(exclusionPatternInitializer, inputComponentStore);
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
    when(inputComponentStore.getByKey(componentKey)).thenReturn(createComponentWithPath(path));

    ignoreFilter = new EnforceIssuesFilter(exclusionPatternInitializer, inputComponentStore);
    assertThat(ignoreFilter.accept(issue, chain)).isTrue();
    verifyZeroInteractions(chain);
  }

  private InputComponent createComponentWithPath(String path) {
    return new TestInputFileBuilder("", path).build();
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
    when(inputComponentStore.getByKey(componentKey)).thenReturn(createComponentWithPath(path));

    ignoreFilter = new EnforceIssuesFilter(exclusionPatternInitializer, inputComponentStore);
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
    when(inputComponentStore.getByKey(componentKey)).thenReturn(null);

    ignoreFilter = new EnforceIssuesFilter(exclusionPatternInitializer, inputComponentStore);
    assertThat(ignoreFilter.accept(issue, chain)).isFalse();
    verifyZeroInteractions(chain);
  }
}
