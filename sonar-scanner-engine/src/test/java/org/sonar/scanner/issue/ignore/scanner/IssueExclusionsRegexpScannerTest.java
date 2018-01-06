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

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.scanner.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.LineRange;
import org.sonar.scanner.issue.ignore.pattern.PatternMatcher;
import org.sonar.scanner.issue.ignore.scanner.IssueExclusionsLoader.DoubleRegexpMatcher;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class IssueExclusionsRegexpScannerTest {
  private String javaFile;

  @Mock
  private IssueExclusionPatternInitializer patternsInitializer;
  @Mock
  private PatternMatcher patternMatcher;

  private List<Pattern> allFilePatterns;
  private List<DoubleRegexpMatcher> blockPatterns;
  private IssueExclusionsRegexpScanner regexpScanner;
  private FileMetadata fileMetadata = new FileMetadata();

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);

    blockPatterns = Arrays.asList(new DoubleRegexpMatcher[] {
      new DoubleRegexpMatcher(Pattern.compile("// SONAR-OFF"), Pattern.compile("// SONAR-ON")),
      new DoubleRegexpMatcher(Pattern.compile("// FOO-OFF"), Pattern.compile("// FOO-ON"))
    });
    allFilePatterns = Collections.singletonList(Pattern.compile("@SONAR-IGNORE-ALL"));

    javaFile = "org.sonar.test.MyFile";
    regexpScanner = new IssueExclusionsRegexpScanner(javaFile, allFilePatterns, blockPatterns, patternMatcher);
  }

  @Test
  public void shouldDetectPatternLastLine() throws URISyntaxException, IOException {
    Path filePath = getResource("file-with-single-regexp-last-line.txt");
    fileMetadata.readMetadata(Files.newInputStream(filePath), UTF_8, filePath.toString(), regexpScanner);

    verify(patternMatcher, times(1)).addPatternToExcludeResource(javaFile);
    verifyNoMoreInteractions(patternMatcher);
  }

  @Test
  public void shouldDoNothing() throws Exception {
    Path filePath = getResource("file-with-no-regexp.txt");
    fileMetadata.readMetadata(Files.newInputStream(filePath), UTF_8, filePath.toString(), regexpScanner);

    verifyNoMoreInteractions(patternMatcher);
  }

  @Test
  public void shouldAddPatternToExcludeFile() throws Exception {
    Path filePath = getResource("file-with-single-regexp.txt");
    fileMetadata.readMetadata(Files.newInputStream(filePath), UTF_8, filePath.toString(), regexpScanner);

    verify(patternMatcher, times(1)).addPatternToExcludeResource(javaFile);
    verifyNoMoreInteractions(patternMatcher);
  }

  @Test
  public void shouldAddPatternToExcludeFileEvenIfAlsoDoubleRegexps() throws Exception {
    Path filePath = getResource("file-with-single-regexp-and-double-regexp.txt");
    fileMetadata.readMetadata(Files.newInputStream(filePath), UTF_8, filePath.toString(), regexpScanner);

    Set<LineRange> lineRanges = new HashSet<>();
    lineRanges.add(new LineRange(5, 26));
    verify(patternMatcher, times(1)).addPatternToExcludeResource(javaFile);
    verify(patternMatcher, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternMatcher);
  }

  @Test
  public void shouldAddPatternToExcludeLines() throws Exception {
    Path filePath = getResource("file-with-double-regexp.txt");
    fileMetadata.readMetadata(Files.newInputStream(filePath), UTF_8, filePath.toString(), regexpScanner);

    Set<LineRange> lineRanges = new HashSet<>();
    lineRanges.add(new LineRange(21, 25));
    verify(patternMatcher, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternMatcher);
  }

  @Test
  public void shouldAddPatternToExcludeLinesTillTheEnd() throws Exception {
    Path filePath = getResource("file-with-double-regexp-unfinished.txt");
    fileMetadata.readMetadata(Files.newInputStream(filePath), UTF_8, filePath.toString(), regexpScanner);

    Set<LineRange> lineRanges = new HashSet<>();
    lineRanges.add(new LineRange(21, 34));
    verify(patternMatcher, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternMatcher);
  }

  @Test
  public void shouldAddPatternToExcludeSeveralLineRanges() throws Exception {
    Path filePath = getResource("file-with-double-regexp-twice.txt");
    fileMetadata.readMetadata(Files.newInputStream(filePath), UTF_8, filePath.toString(), regexpScanner);

    Set<LineRange> lineRanges = new HashSet<>();
    lineRanges.add(new LineRange(21, 25));
    lineRanges.add(new LineRange(29, 33));
    verify(patternMatcher, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternMatcher);
  }

  @Test
  public void shouldAddPatternToExcludeLinesWithWrongOrder() throws Exception {
    Path filePath = getResource("file-with-double-regexp-wrong-order.txt");
    fileMetadata.readMetadata(Files.newInputStream(filePath), UTF_8, filePath.toString(), regexpScanner);

    Set<LineRange> lineRanges = new HashSet<>();
    lineRanges.add(new LineRange(25, 35));
    verify(patternMatcher, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternMatcher);
  }

  @Test
  public void shouldAddPatternToExcludeLinesWithMess() throws Exception {
    Path filePath = getResource("file-with-double-regexp-mess.txt");
    fileMetadata.readMetadata(Files.newInputStream(filePath), UTF_8, filePath.toString(), regexpScanner);

    Set<LineRange> lineRanges = new HashSet<>();
    lineRanges.add(new LineRange(21, 29));
    verify(patternMatcher, times(1)).addPatternToExcludeLines(javaFile, lineRanges);
    verifyNoMoreInteractions(patternMatcher);
  }

  private Path getResource(String fileName) throws URISyntaxException {
    return Paths.get(Resources.getResource("org/sonar/scanner/issue/ignore/scanner/IssueExclusionsRegexpScannerTest/" + fileName).toURI());
  }

}
