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
package org.sonar.api.issue;

import org.sonar.api.scan.issue.filter.FilterableIssue;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.api.rule.RuleKey;

import java.util.Set;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NoSonarFilterTest {

  NoSonarFilter filter = new NoSonarFilter();
  IssueFilterChain chain = mock(IssueFilterChain.class);

  @Before
  public void setupChain() {
    when(chain.accept(isA(FilterableIssue.class))).thenReturn(true);
  }

  @Test
  public void should_ignore_lines_commented_with_nosonar() {
    FilterableIssue issue = mock(FilterableIssue.class);
    when(issue.componentKey()).thenReturn("struts:org.apache.Action");
    when(issue.ruleKey()).thenReturn(RuleKey.of("squid", "AvoidCycles"));

    Set<Integer> noSonarLines = ImmutableSet.of(31, 55);
    filter.addComponent("struts:org.apache.Action", noSonarLines);

    // issue on file
    when(issue.line()).thenReturn(null);
    assertThat(filter.accept(issue, chain)).isTrue();

    // issue on lines
    when(issue.line()).thenReturn(31);
    assertThat(filter.accept(issue, chain)).isFalse();

    when(issue.line()).thenReturn(222);
    assertThat(filter.accept(issue, chain)).isTrue();

    verify(chain, times(2)).accept(issue);
  }

  @Test
  public void should_accept_issues_on_no_sonar_rules() {
    // The "No Sonar" rule logs violations on the lines that are flagged with "NOSONAR" !!
    FilterableIssue issue = mock(FilterableIssue.class);
    when(issue.componentKey()).thenReturn("struts:org.apache.Action");
    when(issue.ruleKey()).thenReturn(RuleKey.of("squid", "NoSonarCheck"));

    Set<Integer> noSonarLines = ImmutableSet.of(31, 55);
    filter.addComponent("struts:org.apache.Action", noSonarLines);

    when(issue.line()).thenReturn(31);
    assertThat(filter.accept(issue, chain)).isTrue();

    when(issue.line()).thenReturn(222);
    assertThat(filter.accept(issue, chain)).isTrue();

    verify(chain, times(2)).accept(issue);
  }
}
