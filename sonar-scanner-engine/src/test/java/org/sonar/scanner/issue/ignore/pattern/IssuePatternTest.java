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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuePatternTest {

  @Test
  public void shouldMatchLines() {
    Set<LineRange> lineRanges = new HashSet<>();
    lineRanges.add(new LineRange(12));
    lineRanges.add(new LineRange(15));
    lineRanges.add(new LineRange(20, 25));

    IssuePattern pattern = new IssuePattern("*", "*", lineRanges);

    assertThat(pattern.matchLine(3)).isFalse();
    assertThat(pattern.matchLine(12)).isTrue();
    assertThat(pattern.matchLine(14)).isFalse();
    assertThat(pattern.matchLine(21)).isTrue();
    assertThat(pattern.matchLine(6599)).isFalse();
  }

  @Test
  public void shouldMatchJavaFile() {
    String javaFile = "org.foo.Bar";
    assertThat(new IssuePattern("org.foo.Bar", "*").matchResource(javaFile)).isTrue();
    assertThat(new IssuePattern("org.foo.*", "*").matchResource(javaFile)).isTrue();
    assertThat(new IssuePattern("*Bar", "*").matchResource(javaFile)).isTrue();
    assertThat(new IssuePattern("*", "*").matchResource(javaFile)).isTrue();
    assertThat(new IssuePattern("org.*.?ar", "*").matchResource(javaFile)).isTrue();

    assertThat(new IssuePattern("org.other.Hello", "*").matchResource(javaFile)).isFalse();
    assertThat(new IssuePattern("org.foo.Hello", "*").matchResource(javaFile)).isFalse();
    assertThat(new IssuePattern("org.*.??ar", "*").matchResource(javaFile)).isFalse();
    assertThat(new IssuePattern("org.*.??ar", "*").matchResource(null)).isFalse();
    assertThat(new IssuePattern("org.*.??ar", "*").matchResource("plop")).isFalse();
  }

  @Test
  public void shouldMatchRule() {
    RuleKey rule = Rule.create("checkstyle", "IllegalRegexp", "").ruleKey();
    assertThat(new IssuePattern("*", "*").matchRule(rule)).isTrue();
    assertThat(new IssuePattern("*", "checkstyle:*").matchRule(rule)).isTrue();
    assertThat(new IssuePattern("*", "checkstyle:IllegalRegexp").matchRule(rule)).isTrue();
    assertThat(new IssuePattern("*", "checkstyle:Illegal*").matchRule(rule)).isTrue();
    assertThat(new IssuePattern("*", "*:*Illegal*").matchRule(rule)).isTrue();

    assertThat(new IssuePattern("*", "pmd:IllegalRegexp").matchRule(rule)).isFalse();
    assertThat(new IssuePattern("*", "pmd:*").matchRule(rule)).isFalse();
    assertThat(new IssuePattern("*", "*:Foo*IllegalRegexp").matchRule(rule)).isFalse();
  }

  @Test
  public void shouldMatchViolation() {
    Rule rule = Rule.create("checkstyle", "IllegalRegexp", "");
    String javaFile = "org.foo.Bar";

    IssuePattern pattern = new IssuePattern("*", "*", Collections.singleton(new LineRange(12)));

    assertThat(pattern.match(javaFile, rule.ruleKey(), null)).isFalse();
    assertThat(pattern.match(javaFile, rule.ruleKey(), 12)).isTrue();
    assertThat(pattern.match(null, rule.ruleKey(), null)).isFalse();
  }

  @Test
  public void shouldPrintPatternToString() {
    IssuePattern pattern = new IssuePattern("*", "checkstyle:*");

    assertThat(pattern.toString()).isEqualTo("IssuePattern[resourcePattern=*,rulePattern=checkstyle:*,lines=[],lineRanges=[],checkLines=false]");
  }
}
