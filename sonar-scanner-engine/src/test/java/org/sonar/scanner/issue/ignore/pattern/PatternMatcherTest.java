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
package org.sonar.scanner.issue.ignore.pattern;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rules.Rule;

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(patternMatcher.getMatchingPattern(JAVA_FILE, CHECKSTYLE_RULE.ruleKey(), null)).isNull();
  }

  @Test
  public void shouldMatchWithStandardPatterns() {
    patternMatcher.addPatternForComponent(JAVA_FILE, createPattern("org.foo.Hello", "checkstyle:MagicNumber", createRanges(15, 200)));

    assertThat(patternMatcher.getMatchingPattern(JAVA_FILE, CHECKSTYLE_RULE.ruleKey(), 150)).isNotNull();
  }

  @Test
  public void shouldNotMatchWithStandardPatterns() {
    patternMatcher.addPatternForComponent(JAVA_FILE, createPattern("org.foo.Hello", "checkstyle:MagicNumber", createRanges(15, 200)));

    assertThat(patternMatcher.getMatchingPattern(JAVA_FILE, CHECKSTYLE_RULE.ruleKey(), 5)).isNull();
  }

  @Test
  public void shouldMatchWithExtraPattern() {
    patternMatcher.addPatternForComponent(JAVA_FILE, createPattern("org.foo.Hello", "*", createRanges(15, 200)));

    assertThat(patternMatcher.getMatchingPattern(JAVA_FILE, CHECKSTYLE_RULE.ruleKey(), 150)).isNotNull();
  }

  @Test
  public void shouldNotMatchWithExtraPattern() {
    patternMatcher.addPatternForComponent(JAVA_FILE, createPattern("org.foo.Hello", "*", createRanges(15, 200)));

    assertThat(patternMatcher.getMatchingPattern(JAVA_FILE, CHECKSTYLE_RULE.ruleKey(), 5)).isNull();
  }

  private IssuePattern createPattern(String resourcePattern, String rulePattern, @Nullable Set<LineRange> lineRanges) {
    if (lineRanges != null) {
      return new IssuePattern(resourcePattern, rulePattern, lineRanges);
    } else {
      return new IssuePattern(resourcePattern, rulePattern);
    }
  }

  private Set<LineRange> createRanges(int from, int to) {
    return Collections.singleton(new LineRange(from, to));
  }

}
