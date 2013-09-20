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

package org.sonar.plugins.core.issue.ignore.scanner;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.plugins.core.issue.ignore.pattern.LineRange;
import org.sonar.plugins.core.issue.ignore.pattern.IssuePattern;
import org.sonar.plugins.core.issue.ignore.pattern.PatternsInitializer;
import org.sonar.test.TestUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Charsets.UTF_8;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class RegexpScannerTest {

  private RegexpScanner regexpScanner;

  private String javaFile;
  @Mock
  private PatternsInitializer patternsInitializer;
  @Mock
  private IssuePattern allFilePattern;
  @Mock
  private IssuePattern blockPattern1;
  @Mock
  private IssuePattern blockPattern2;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);

    when(allFilePattern.getAllFileRegexp()).thenReturn("@SONAR-IGNORE-ALL");
    when(blockPattern1.getBeginBlockRegexp()).thenReturn("// SONAR-OFF");
    when(blockPattern1.getEndBlockRegexp()).thenReturn("// SONAR-ON");
    when(blockPattern2.getBeginBlockRegexp()).thenReturn("// FOO-OFF");
    when(blockPattern2.getEndBlockRegexp()).thenReturn("// FOO-ON");
    when(patternsInitializer.getAllFilePatterns()).thenReturn(Arrays.asList(allFilePattern));
    when(patternsInitializer.getBlockPatterns()).thenReturn(Arrays.asList(blockPattern1, blockPattern2));

    regexpScanner = new RegexpScanner(patternsInitializer);
    verify(patternsInitializer, times(1)).getAllFilePatterns();
    verify(patternsInitializer, times(1)).getBlockPatterns();

    javaFile = "org.sonar.test.MyFile";
  }

  @Test
  public void shouldDoNothing() throws IOException {
    regexpScanner.scan(javaFile, TestUtils.getResource(getClass(), "file-with-no-regexp.txt"), UTF_8);

    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeFile() throws IOException {
    regexpScanner.scan(javaFile, TestUtils.getResource(getClass(), "file-with-single-regexp.txt"), UTF_8);

    verify(patternsInitializer, times(1)).addPatternToExcludeResource(javaFile);
    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeFileEvenIfAlsoDoubleRegexps() throws IOException {
    regexpScanner.scan(javaFile, TestUtils.getResource(getClass(), "file-with-single-regexp-and-double-regexp.txt"), UTF_8);

    verify(patternsInitializer, times(1)).addPatternToExcludeResource(javaFile);
    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeLines() throws IOException {
    regexpScanner.scan(javaFile, TestUtils.getResource(getClass(), "file-with-double-regexp.txt"), UTF_8);

    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(21, 25));
    verify(patternsInitializer, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeLinesTillTheEnd() throws IOException {
    regexpScanner.scan(javaFile, TestUtils.getResource(getClass(), "file-with-double-regexp-unfinished.txt"), UTF_8);

    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(21, 34));
    verify(patternsInitializer, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeSeveralLineRanges() throws IOException {
    regexpScanner.scan(javaFile, TestUtils.getResource(getClass(), "file-with-double-regexp-twice.txt"), UTF_8);

    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(21, 25));
    lineRanges.add(new LineRange(29, 33));
    verify(patternsInitializer, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeLinesWithWrongOrder() throws IOException {
    regexpScanner.scan(javaFile, TestUtils.getResource(getClass(), "file-with-double-regexp-wrong-order.txt"), UTF_8);

    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(25, 35));
    verify(patternsInitializer, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeLinesWithMess() throws IOException {
    regexpScanner.scan(javaFile, TestUtils.getResource(getClass(), "file-with-double-regexp-mess.txt"), UTF_8);

    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(21, 29));
    verify(patternsInitializer, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternsInitializer);
  }

}
