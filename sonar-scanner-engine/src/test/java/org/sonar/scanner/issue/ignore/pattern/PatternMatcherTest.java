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
package org.sonar.scanner.issue.ignore.pattern;

import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.scanner.issue.ignore.pattern.IssuePattern;
import org.sonar.scanner.issue.ignore.pattern.LineRange;
import org.sonar.scanner.issue.ignore.pattern.PatternDecoder;
import org.sonar.scanner.issue.ignore.pattern.PatternMatcher;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PatternMatcherTest {

  public static final Rule CHECKSTYLE_RULE = Rule.create("checkstyle", "MagicNumber", "");
  public static final String JAVA_FILE = "org.foo.Hello";

  private PatternMatcher patternMatcher;

  @Before
  public void setUp() {
    patternMatcher = new PatternMatcher();
  }

  @Test
  public void shouldReturnExtraPatternForResource() {
    String file = "foo";
    patternMatcher.addPatternToExcludeResource(file);

    IssuePattern extraPattern = patternMatcher.getPatternsForComponent(file).iterator().next();
    assertThat(extraPattern.matchResource(file)).isTrue();
    assertThat(extraPattern.isCheckLines()).isFalse();
  }

  @Test
  public void shouldReturnExtraPatternForLinesOfResource() {
    String file = "foo";
    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(25, 28));
    patternMatcher.addPatternToExcludeLines(file, lineRanges);

    IssuePattern extraPattern = patternMatcher.getPatternsForComponent(file).iterator().next();
    assertThat(extraPattern.matchResource(file)).isTrue();
    assertThat(extraPattern.getAllLines()).isEqualTo(Sets.newHashSet(25, 26, 27, 28));
  }

  @Test
  public void shouldHaveNoMatcherIfNoneDefined() {
    assertThat(patternMatcher.getMatchingPattern(create(CHECKSTYLE_RULE, JAVA_FILE, null))).isNull();
  }

  @Test
  public void shouldMatchWithStandardPatterns() {
    patternMatcher.addPatternForComponent(JAVA_FILE, createPattern("org.foo.Hello;checkstyle:MagicNumber;[15-200]"));

    assertThat(patternMatcher.getMatchingPattern(create(CHECKSTYLE_RULE, JAVA_FILE, 150))).isNotNull();
  }

  @Test
  public void shouldNotMatchWithStandardPatterns() {
    patternMatcher.addPatternForComponent(JAVA_FILE, createPattern("org.foo.Hello;checkstyle:MagicNumber;[15-200]"));

    assertThat(patternMatcher.getMatchingPattern(create(CHECKSTYLE_RULE, JAVA_FILE, 5))).isNull();
  }

  @Test
  public void shouldMatchWithExtraPattern() {
    patternMatcher.addPatternForComponent(JAVA_FILE, createPattern("org.foo.Hello;*;[15-200]"));

    assertThat(patternMatcher.getMatchingPattern(create(CHECKSTYLE_RULE, JAVA_FILE, 150))).isNotNull();
  }

  @Test
  public void shouldNotMatchWithExtraPattern() {
    patternMatcher.addPatternForComponent(JAVA_FILE, createPattern("org.foo.Hello;*;[15-200]"));

    assertThat(patternMatcher.getMatchingPattern(create(CHECKSTYLE_RULE, JAVA_FILE, 5))).isNull();
  }

  private FilterableIssue create(Rule rule, String component, Integer line) {
    FilterableIssue mockIssue = mock(FilterableIssue.class);
    RuleKey ruleKey = null;
    if (rule != null) {
      ruleKey = rule.ruleKey();
    }
    when(mockIssue.ruleKey()).thenReturn(ruleKey);
    when(mockIssue.componentKey()).thenReturn(component);
    when(mockIssue.line()).thenReturn(line);
    return mockIssue;
  }

  private IssuePattern createPattern(String line) {
    return new PatternDecoder().decode(line).get(0);
  }

}
