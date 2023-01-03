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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.scanner.issue.DefaultFilterableIssue;
import org.sonar.scanner.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.IssuePattern;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class EnforceIssuesFilterTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private final IssueInclusionPatternInitializer exclusionPatternInitializer = mock(IssueInclusionPatternInitializer.class);
  private final DefaultFilterableIssue issue = mock(DefaultFilterableIssue.class);
  private final IssueFilterChain chain = mock(IssueFilterChain.class);
  private final AnalysisWarnings analysisWarnings = mock(AnalysisWarnings.class);
  private EnforceIssuesFilter ignoreFilter;

  @Before
  public void init() {
    when(chain.accept(issue)).thenReturn(true);
  }

  @Test
  public void shouldPassToChainIfNoConfiguredPatterns() {
    DefaultActiveRules activeRules = new DefaultActiveRules(ImmutableSet.of());
    ignoreFilter = new EnforceIssuesFilter(exclusionPatternInitializer, analysisWarnings, activeRules);
    assertThat(ignoreFilter.accept(issue, chain)).isTrue();
    verify(chain).accept(issue);
  }

  @Test
  public void shouldPassToChainIfRuleDoesNotMatch() {
    DefaultActiveRules activeRules = new DefaultActiveRules(ImmutableSet.of());
    RuleKey ruleKey = RuleKey.of("repo", "rule");
    when(issue.ruleKey()).thenReturn(ruleKey);

    IssuePattern matching = new IssuePattern("**", "unknown");
    when(exclusionPatternInitializer.getMulticriteriaPatterns()).thenReturn(ImmutableList.of(matching));

    ignoreFilter = new EnforceIssuesFilter(exclusionPatternInitializer, analysisWarnings, activeRules);
    assertThat(ignoreFilter.accept(issue, chain)).isTrue();
    verify(chain).accept(issue);
  }

  @Test
  public void shouldAcceptIssueIfFullyMatched() {
    DefaultActiveRules activeRules = new DefaultActiveRules(ImmutableSet.of());
    String path = "org/sonar/api/Issue.java";
    RuleKey ruleKey = RuleKey.of("repo", "rule");
    when(issue.ruleKey()).thenReturn(ruleKey);

    IssuePattern matching = new IssuePattern(path, ruleKey.toString());
    when(exclusionPatternInitializer.getMulticriteriaPatterns()).thenReturn(ImmutableList.of(matching));
    when(issue.getComponent()).thenReturn(createComponentWithPath(path));

    ignoreFilter = new EnforceIssuesFilter(exclusionPatternInitializer, analysisWarnings, activeRules);
    assertThat(ignoreFilter.accept(issue, chain)).isTrue();
    verifyNoInteractions(chain);
  }

  @Test
  public void shouldAcceptIssueIfMatchesDeprecatedRuleKey() {
    RuleKey ruleKey = RuleKey.of("repo", "rule");
    DefaultActiveRules activeRules = new DefaultActiveRules(ImmutableSet.of(new NewActiveRule.Builder()
      .setRuleKey(ruleKey)
      .setDeprecatedKeys(singleton(RuleKey.of("repo2", "deprecated")))
      .build()));
    String path = "org/sonar/api/Issue.java";
    when(issue.ruleKey()).thenReturn(ruleKey);

    IssuePattern matching = new IssuePattern("org/**", "repo2:deprecated");
    when(exclusionPatternInitializer.getMulticriteriaPatterns()).thenReturn(ImmutableList.of(matching));
    when(issue.getComponent()).thenReturn(createComponentWithPath(path));

    ignoreFilter = new EnforceIssuesFilter(exclusionPatternInitializer, analysisWarnings, activeRules);
    assertThat(ignoreFilter.accept(issue, chain)).isTrue();
    verify(analysisWarnings)
      .addUnique("A multicriteria issue enforce uses the rule key 'repo2:deprecated' that has been changed. The pattern should be updated to 'repo:rule'");
    verifyNoInteractions(chain);
  }

  private InputComponent createComponentWithPath(String path) {
    return new TestInputFileBuilder("", path).build();
  }

  @Test
  public void shouldRefuseIssueIfRuleMatchesButNotPath() {
    DefaultActiveRules activeRules = new DefaultActiveRules(ImmutableSet.of());
    String path = "org/sonar/api/Issue.java";
    String componentKey = "org.sonar.api.Issue";
    RuleKey ruleKey = RuleKey.of("repo", "rule");
    when(issue.ruleKey()).thenReturn(ruleKey);
    when(issue.componentKey()).thenReturn(componentKey);

    IssuePattern matching = new IssuePattern("no match", "repo:rule");
    when(exclusionPatternInitializer.getMulticriteriaPatterns()).thenReturn(ImmutableList.of(matching));
    when(issue.getComponent()).thenReturn(createComponentWithPath(path));

    ignoreFilter = new EnforceIssuesFilter(exclusionPatternInitializer, analysisWarnings, activeRules);
    assertThat(ignoreFilter.accept(issue, chain)).isFalse();
    verifyNoInteractions(chain, analysisWarnings);
  }

  @Test
  public void shouldRefuseIssueIfRuleMatchesAndNotFile() throws IOException {
    DefaultActiveRules activeRules = new DefaultActiveRules(ImmutableSet.of());
    String path = "org/sonar/api/Issue.java";
    RuleKey ruleKey = RuleKey.of("repo", "key");
    when(issue.ruleKey()).thenReturn(ruleKey);

    IssuePattern matching = new IssuePattern(path, ruleKey.toString());
    when(exclusionPatternInitializer.getMulticriteriaPatterns()).thenReturn(ImmutableList.of(matching));
    when(issue.getComponent()).thenReturn(TestInputFileBuilder.newDefaultInputProject("foo", tempFolder.newFolder()));

    ignoreFilter = new EnforceIssuesFilter(exclusionPatternInitializer, analysisWarnings, activeRules);
    assertThat(ignoreFilter.accept(issue, chain)).isFalse();
    verifyNoInteractions(chain, analysisWarnings);
  }
}
