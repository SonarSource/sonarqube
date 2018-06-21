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
package org.sonar.server.computation.task.projectanalysis.issue.filter;

import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;

import static org.assertj.core.api.Assertions.assertThat;

public class IssuePatternTest {

  @Test
  public void match_file() {
    String javaFile = "org/foo/Bar.xoo";
    assertThat(new IssuePattern("org/foo/Bar*", "*").matchComponent(javaFile)).isTrue();
    assertThat(new IssuePattern("org/foo/*", "*").matchComponent(javaFile)).isTrue();
    assertThat(new IssuePattern("**/*ar*", "*").matchComponent(javaFile)).isTrue();
    assertThat(new IssuePattern("org/**/?ar.xoo", "*").matchComponent(javaFile)).isTrue();
    assertThat(new IssuePattern("**", "*").matchComponent(javaFile)).isTrue();

    assertThat(new IssuePattern("org/other/Hello", "*").matchComponent(javaFile)).isFalse();
    assertThat(new IssuePattern("org/foo/Hello", "*").matchComponent(javaFile)).isFalse();
    assertThat(new IssuePattern("org/**/??ar.xoo", "*").matchComponent(javaFile)).isFalse();
    assertThat(new IssuePattern("org/**/??ar.xoo", "*").matchComponent(null)).isFalse();
    assertThat(new IssuePattern("org/**/??ar.xoo", "*").matchComponent("plop")).isFalse();
  }

  @Test
  public void match_rule() {
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
  public void test_to_string() {
    IssuePattern pattern = new IssuePattern("*", "checkstyle:*");

    assertThat(pattern.toString()).isEqualTo(
      "IssuePattern{componentPattern=*, rulePattern=checkstyle:*}");
  }
}
