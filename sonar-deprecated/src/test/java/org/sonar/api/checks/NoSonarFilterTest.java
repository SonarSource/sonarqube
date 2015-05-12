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
package org.sonar.api.checks;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilterChain;
import org.sonar.api.resources.File;
import org.sonar.api.rule.RuleKey;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NoSonarFilterTest {

  private SonarIndex sonarIndex = mock(SonarIndex.class);
  NoSonarFilter filter = new NoSonarFilter(sonarIndex);
  private File javaFile;
  IssueFilterChain chain = mock(IssueFilterChain.class);

  @Before
  public void prepare() {
    when(chain.accept(isA(Issue.class))).thenReturn(true);
    javaFile = File.create("org/foo/Bar.java");
    javaFile.setEffectiveKey("struts:org/foo/Bar.java");
    when(sonarIndex.getResource(javaFile)).thenReturn(javaFile);
  }

  @Test
  public void ignoreLinesCommentedWithNoSonar() {
    Set<Integer> noSonarLines = new HashSet<>();
    noSonarLines.add(31);
    noSonarLines.add(55);
    filter.addResource(javaFile, noSonarLines);

    Issue issue = mock(Issue.class);
    when(issue.componentKey()).thenReturn("struts:org/foo/Bar.java");
    when(issue.ruleKey()).thenReturn(RuleKey.of("squid", "Foo"));

    // violation on class
    assertThat(filter.accept(issue, chain)).isTrue();

    // violation on lines
    when(issue.line()).thenReturn(30);
    assertThat(filter.accept(issue, chain)).isTrue();
    when(issue.line()).thenReturn(31);
    assertThat(filter.accept(issue, chain)).isFalse();
  }

  @Test
  public void should_accept_violations_from_no_sonar_rules() {
    // The "No Sonar" rule logs violations on the lines that are flagged with "NOSONAR" !!

    Set<Integer> noSonarLines = new HashSet<>();
    noSonarLines.add(31);
    filter.addResource(javaFile, noSonarLines);

    Issue issue = mock(Issue.class);
    when(issue.componentKey()).thenReturn("struts:org.apache.Action");
    when(issue.ruleKey()).thenReturn(RuleKey.of("squid", "NoSonarCheck"));

    when(issue.line()).thenReturn(31);
    assertThat(filter.accept(issue, chain)).isTrue();

  }
}
