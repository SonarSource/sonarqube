/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.scanner.issue.DefaultFilterableIssue;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class IgnoreIssuesFilterTest {
  @Rule
  public LogTester logTester = new LogTester();

  private final DefaultFilterableIssue issue = mock(DefaultFilterableIssue.class);
  private final IssueFilterChain chain = mock(IssueFilterChain.class);
  private final AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
  private final RuleKey ruleKey = RuleKey.of("foo", "bar");

  private DefaultInputFile component;

  @Before
  public void prepare() {
    component = mock(DefaultInputFile.class);
    when(issue.getComponent()).thenReturn(component);
    when(issue.ruleKey()).thenReturn(ruleKey);
  }

  @Test
  public void shouldPassToChainIfMatcherHasNoPatternForIssue() {
    DefaultActiveRules activeRules = new DefaultActiveRules(ImmutableSet.of());
    IgnoreIssuesFilter underTest = new IgnoreIssuesFilter(activeRules, analysisWarnings);

    when(chain.accept(issue)).thenReturn(true);
    assertThat(underTest.accept(issue, chain)).isTrue();
    verify(chain).accept(any());
  }

  @Test
  public void shouldRejectIfRulePatternMatches() {
    DefaultActiveRules activeRules = new DefaultActiveRules(ImmutableSet.of());
    IgnoreIssuesFilter underTest = new IgnoreIssuesFilter(activeRules, analysisWarnings);

    WildcardPattern pattern = mock(WildcardPattern.class);
    when(pattern.match(ruleKey.toString())).thenReturn(true);
    underTest.addRuleExclusionPatternForComponent(component, pattern);

    assertThat(underTest.accept(issue, chain)).isFalse();
    verifyNoInteractions(analysisWarnings);
  }

  @Test
  public void shouldRejectIfRulePatternMatchesDeprecatedRule() {
    DefaultActiveRules activeRules = new DefaultActiveRules(ImmutableSet.of(new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .setDeprecatedKeys(singleton(RuleKey.of("repo", "rule")))
      .build()));
    IgnoreIssuesFilter underTest = new IgnoreIssuesFilter(activeRules, analysisWarnings);

    WildcardPattern pattern = WildcardPattern.create("repo:rule");
    underTest.addRuleExclusionPatternForComponent(component, pattern);
    assertThat(underTest.accept(issue, chain)).isFalse();

    verify(analysisWarnings).addUnique("A multicriteria issue exclusion uses the rule key 'repo:rule' that has been changed. The pattern should be updated to 'foo:bar'");
    assertThat(logTester.logs())
      .contains("A multicriteria issue exclusion uses the rule key 'repo:rule' that has been changed. The pattern should be updated to 'foo:bar'");
  }

  @Test
  public void shouldAcceptIfRulePatternDoesNotMatch() {
    DefaultActiveRules activeRules = new DefaultActiveRules(ImmutableSet.of());
    IgnoreIssuesFilter underTest = new IgnoreIssuesFilter(activeRules, analysisWarnings);

    WildcardPattern pattern = mock(WildcardPattern.class);
    when(pattern.match(ruleKey.toString())).thenReturn(false);
    underTest.addRuleExclusionPatternForComponent(component, pattern);

    assertThat(underTest.accept(issue, chain)).isFalse();
  }
}
