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
package org.sonar.scanner.issue.ignore.scanner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.scanner.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.IssuePattern;
import org.sonar.scanner.issue.ignore.pattern.PatternMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class IssueExclusionsLoaderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Mock
  private IssueExclusionPatternInitializer exclusionPatternInitializer;

  private PatternMatcher patternMatcher;

  private IssueExclusionsLoader scanner;

  @Before
  public void before() throws Exception {
    patternMatcher = new PatternMatcher();
    MockitoAnnotations.initMocks(this);
    scanner = new IssueExclusionsLoader(exclusionPatternInitializer, patternMatcher);
  }

  @Test
  public void testToString() throws Exception {
    assertThat(scanner.toString()).isEqualTo("Issues Exclusions - Source Scanner");
  }

  @Test
  public void createComputer() {
    assertThat(scanner.createCharHandlerFor("src/main/java/Foo.java")).isNull();

    when(exclusionPatternInitializer.getAllFilePatterns()).thenReturn(Collections.singletonList("pattern"));
    scanner = new IssueExclusionsLoader(exclusionPatternInitializer, patternMatcher);
    assertThat(scanner.createCharHandlerFor("src/main/java/Foo.java")).isNotNull();


  }

  @Test
  public void shouldHavePatternsBasedOnMulticriteriaPattern() {
    IssuePattern pattern1 = new IssuePattern("org/foo/Bar.java", "*");
    IssuePattern pattern2 = new IssuePattern("org/foo/Hello.java", "checkstyle:MagicNumber");
    when(exclusionPatternInitializer.getMulticriteriaPatterns()).thenReturn(Arrays.asList(new IssuePattern[] {pattern1, pattern2}));

    IssueExclusionsLoader loader = new IssueExclusionsLoader(exclusionPatternInitializer, patternMatcher);
    loader.addMulticriteriaPatterns("org/foo/Bar.java", "org.foo.Bar");
    loader.addMulticriteriaPatterns("org/foo/Baz.java", "org.foo.Baz");
    loader.addMulticriteriaPatterns("org/foo/Hello.java", "org.foo.Hello");

    assertThat(patternMatcher.getPatternsForComponent("org.foo.Bar")).hasSize(1);
    assertThat(patternMatcher.getPatternsForComponent("org.foo.Baz")).hasSize(0);
    assertThat(patternMatcher.getPatternsForComponent("org.foo.Hello")).hasSize(1);
  }

  @Test
  public void shouldAnalyzeProject() throws IOException {
    IssuePattern pattern = new IssuePattern("**", "*");
    when(exclusionPatternInitializer.getMulticriteriaPatterns()).thenReturn(Collections.singletonList(pattern));
    when(exclusionPatternInitializer.hasMulticriteriaPatterns()).thenReturn(true);

    PatternMatcher patternMatcher = mock(PatternMatcher.class);
    IssueExclusionsLoader loader = new IssueExclusionsLoader(exclusionPatternInitializer, patternMatcher);
    assertThat(loader.shouldExecute()).isTrue();
    loader.addMulticriteriaPatterns("src/main/java/Foo.java", "polop:src/main/java/Foo.java");
    loader.addMulticriteriaPatterns("src/test/java/FooTest.java", "polop:src/test/java/FooTest.java");

    verify(patternMatcher).addPatternForComponent("polop:src/main/java/Foo.java", pattern);
    verify(patternMatcher).addPatternForComponent("polop:src/test/java/FooTest.java", pattern);
    verifyNoMoreInteractions(patternMatcher);
  }

  @Test
  public void shouldExecute() {
    when(exclusionPatternInitializer.hasMulticriteriaPatterns()).thenReturn(true);
    assertThat(scanner.shouldExecute()).isTrue();

    when(exclusionPatternInitializer.hasMulticriteriaPatterns()).thenReturn(false);
    assertThat(scanner.shouldExecute()).isFalse();
  }
}
