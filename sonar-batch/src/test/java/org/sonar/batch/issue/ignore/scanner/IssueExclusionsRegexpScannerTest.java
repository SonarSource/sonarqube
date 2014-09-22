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

package org.sonar.batch.issue.ignore.scanner;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.batch.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.batch.issue.ignore.pattern.IssuePattern;
import org.sonar.batch.issue.ignore.pattern.LineRange;
import org.sonar.batch.issue.ignore.pattern.PatternMatcher;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

import static com.google.common.base.Charsets.UTF_8;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class IssueExclusionsRegexpScannerTest {

  private IssueExclusionsRegexpScanner regexpScanner;

  private String javaFile;
  @Mock
  private IssueExclusionPatternInitializer patternsInitializer;
  @Mock
  private PatternMatcher patternMatcher;
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
    when(patternsInitializer.getPatternMatcher()).thenReturn(patternMatcher);

    regexpScanner = new IssueExclusionsRegexpScanner(patternsInitializer);
    verify(patternsInitializer, times(1)).getAllFilePatterns();
    verify(patternsInitializer, times(1)).getBlockPatterns();

    javaFile = "org.sonar.test.MyFile";
  }

  @Test
  public void shouldDoNothing() throws Exception {
    regexpScanner.scan(javaFile, new File(Resources.getResource(
      "org/sonar/batch/issue/ignore/scanner/IssueExclusionsRegexpScannerTest/file-with-no-regexp.txt").toURI()), UTF_8);

    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeFile() throws Exception {
    regexpScanner.scan(javaFile, new File(Resources.getResource(
      "org/sonar/batch/issue/ignore/scanner/IssueExclusionsRegexpScannerTest/file-with-single-regexp.txt").toURI()), UTF_8);

    verify(patternsInitializer).getPatternMatcher();
    verify(patternMatcher, times(1)).addPatternToExcludeResource(javaFile);
    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeFileEvenIfAlsoDoubleRegexps() throws Exception {
    regexpScanner.scan(javaFile, new File(Resources.getResource(
      "org/sonar/batch/issue/ignore/scanner/IssueExclusionsRegexpScannerTest/file-with-single-regexp-and-double-regexp.txt").toURI()), UTF_8);

    verify(patternsInitializer).getPatternMatcher();
    verify(patternMatcher, times(1)).addPatternToExcludeResource(javaFile);
    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeLines() throws Exception {
    regexpScanner.scan(javaFile, new File(Resources.getResource(
      "org/sonar/batch/issue/ignore/scanner/IssueExclusionsRegexpScannerTest/file-with-double-regexp.txt").toURI()), UTF_8);

    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(21, 25));
    verify(patternsInitializer).getPatternMatcher();
    verify(patternMatcher, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeLinesTillTheEnd() throws Exception {
    regexpScanner.scan(javaFile, new File(Resources.getResource(
      "org/sonar/batch/issue/ignore/scanner/IssueExclusionsRegexpScannerTest/file-with-double-regexp-unfinished.txt").toURI()), UTF_8);

    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(21, 34));
    verify(patternsInitializer).getPatternMatcher();
    verify(patternMatcher, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeSeveralLineRanges() throws Exception {
    regexpScanner.scan(javaFile, new File(Resources.getResource(
      "org/sonar/batch/issue/ignore/scanner/IssueExclusionsRegexpScannerTest/file-with-double-regexp-twice.txt").toURI()), UTF_8);

    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(21, 25));
    lineRanges.add(new LineRange(29, 33));
    verify(patternsInitializer).getPatternMatcher();
    verify(patternMatcher, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeLinesWithWrongOrder() throws Exception {
    regexpScanner.scan(javaFile, new File(Resources.getResource(
      "org/sonar/batch/issue/ignore/scanner/IssueExclusionsRegexpScannerTest/file-with-double-regexp-wrong-order.txt").toURI()), UTF_8);

    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(25, 35));
    verify(patternsInitializer).getPatternMatcher();
    verify(patternMatcher, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternsInitializer);
  }

  @Test
  public void shouldAddPatternToExcludeLinesWithMess() throws Exception {
    regexpScanner.scan(javaFile, new File(Resources.getResource(
      "org/sonar/batch/issue/ignore/scanner/IssueExclusionsRegexpScannerTest/file-with-double-regexp-mess.txt").toURI()), UTF_8);

    Set<LineRange> lineRanges = Sets.newHashSet();
    lineRanges.add(new LineRange(21, 29));
    verify(patternsInitializer).getPatternMatcher();
    verify(patternMatcher, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternsInitializer);
  }

}
