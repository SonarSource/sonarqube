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
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.plugins.core.issue.ignore.pattern.IssuePattern;
import org.sonar.plugins.core.issue.ignore.pattern.PatternDecoder;
import org.sonar.plugins.core.issue.ignore.pattern.PatternsInitializer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IgnoreIssuesFilterTest {

  public static final Rule CHECKSTYLE_RULE = Rule.create("checkstyle", "MagicNumber", "");
  public static final String JAVA_FILE = "org.foo.Hello";

  private PatternsInitializer patternsInitializer;
  private IgnoreIssuesFilter filter;

  @Before
  public void init() {
    patternsInitializer = mock(PatternsInitializer.class);
    when(patternsInitializer.getMulticriteriaPatterns()).thenReturn(Collections.<IssuePattern> emptyList());

    filter = new IgnoreIssuesFilter(patternsInitializer);
  }

  @Test
  public void shouldBeDeactivatedWhenPropertyIsMissing() {
    assertThat(filter.accept(create(CHECKSTYLE_RULE, JAVA_FILE, null))).isTrue();
  }

  @Test
  public void shouldBeIgnoredWithStandardPatterns() throws IOException {
    when(patternsInitializer.getPatternsForComponent(JAVA_FILE)).thenReturn(createPatterns("org.foo.Hello;checkstyle:MagicNumber;[15-200]"));

    assertThat(filter.accept(create(CHECKSTYLE_RULE, JAVA_FILE, 150))).isFalse();
  }

  @Test
  public void shouldNotBeIgnoredWithStandardPatterns() throws IOException {
    when(patternsInitializer.getPatternsForComponent(JAVA_FILE)).thenReturn(createPatterns("org.foo.Hello;checkstyle:MagicNumber;[15-200]"));

    assertThat(filter.accept(create(CHECKSTYLE_RULE, JAVA_FILE, 5))).isTrue();
  }

  @Test
  public void shouldBeIgnoredWithExtraPattern() throws IOException {
    when(patternsInitializer.getPatternsForComponent(JAVA_FILE)).thenReturn(createPatterns("org.foo.Hello;*;[15-200]"));

    assertThat(filter.accept(create(CHECKSTYLE_RULE, JAVA_FILE, 150))).isFalse();
  }

  @Test
  public void shouldNotBeIgnoredWithExtraPattern() throws IOException {
    when(patternsInitializer.getPatternsForComponent(JAVA_FILE)).thenReturn(createPatterns("org.foo.Hello;*;[15-200]"));

    assertThat(filter.accept(create(CHECKSTYLE_RULE, JAVA_FILE, 5))).isTrue();
  }

  private Issue create(Rule rule, String component, Integer line) {
    Issue mockIssue = mock(Issue.class);
    RuleKey ruleKey = null;
    if (rule != null) {
      ruleKey = rule.ruleKey();
    }
    when(mockIssue.ruleKey()).thenReturn(ruleKey);
    when(mockIssue.componentKey()).thenReturn(component);
    when(mockIssue.line()).thenReturn(line);
    return mockIssue;
  }

  private List<IssuePattern> createPatterns(String line) {
    return new PatternDecoder().decode(line);
  }
}
