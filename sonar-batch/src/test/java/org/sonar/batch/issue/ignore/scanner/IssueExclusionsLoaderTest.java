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

package org.sonar.batch.issue.ignore.scanner;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.batch.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.batch.issue.ignore.pattern.PatternMatcher;

import java.io.File;
import java.io.IOException;

import static com.google.common.base.Charsets.UTF_8;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class IssueExclusionsLoaderTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Mock
  private IssueExclusionsRegexpScanner regexpScanner;

  @Mock
  private IssueInclusionPatternInitializer inclusionPatternInitializer;

  @Mock
  private IssueExclusionPatternInitializer exclusionPatternInitializer;

  @Mock
  private PatternMatcher patternMatcher;

  DefaultFileSystem fs = new DefaultFileSystem().setEncoding(UTF_8);
  IssueExclusionsLoader scanner;
  File baseDir;

  @Before
  public void before() throws IOException {
    baseDir = temp.newFolder();
    MockitoAnnotations.initMocks(this);
    scanner = new IssueExclusionsLoader(regexpScanner, exclusionPatternInitializer, inclusionPatternInitializer, fs);
  }

  @Test
  public void testToString() throws Exception {
    assertThat(scanner.toString()).isEqualTo("Issues Exclusions - Source Scanner");
  }

  @Test
  public void shouldExecute() throws IOException {
    when(exclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(true);
    when(inclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(true);
    assertThat(scanner.shouldExecuteOnProject(null)).isTrue();

    when(exclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(true);
    when(inclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(false);
    assertThat(scanner.shouldExecuteOnProject(null)).isTrue();

    when(exclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(false);
    when(inclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(true);
    assertThat(scanner.shouldExecuteOnProject(null)).isTrue();

    when(exclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(false);
    when(inclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(false);
    assertThat(scanner.shouldExecuteOnProject(null)).isFalse();

  }

  @Test
  public void shouldAnalyzeProject() throws IOException {
    File javaFile1 = new File(baseDir, "src/main/java/Foo.java");
    fs.add(new DefaultInputFile("src/main/java/Foo.java")
      .setFile(javaFile1)
      .setType(InputFile.Type.MAIN)
      .setKey("polop:src/main/java/Foo.java"));
    File javaTestFile1 = new File(baseDir, "src/test/java/FooTest.java");
    fs.add(new DefaultInputFile("src/test/java/FooTest.java")
      .setFile(javaTestFile1)
      .setType(InputFile.Type.TEST)
      .setKey("polop:src/test/java/FooTest.java"));

    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(true);

    scanner.execute();

    verify(inclusionPatternInitializer).initializePatternsForPath("src/main/java/Foo.java", "polop:src/main/java/Foo.java");
    verify(inclusionPatternInitializer).initializePatternsForPath("src/test/java/FooTest.java", "polop:src/test/java/FooTest.java");
    verify(exclusionPatternInitializer).initializePatternsForPath("src/main/java/Foo.java", "polop:src/main/java/Foo.java");
    verify(exclusionPatternInitializer).initializePatternsForPath("src/test/java/FooTest.java", "polop:src/test/java/FooTest.java");
    verify(regexpScanner).scan("polop:src/main/java/Foo.java", javaFile1, UTF_8);
    verify(regexpScanner).scan("polop:src/test/java/FooTest.java", javaTestFile1, UTF_8);
  }

  @Test
  public void shouldAnalyseFilesOnlyWhenRegexConfigured() throws IOException {
    File javaFile1 = new File(baseDir, "src/main/java/Foo.java");
    fs.add(new DefaultInputFile("src/main/java/Foo.java")
      .setFile(javaFile1)
      .setType(InputFile.Type.MAIN)
      .setKey("polop:src/main/java/Foo.java"));
    File javaTestFile1 = new File(baseDir, "src/test/java/FooTest.java");
    fs.add(new DefaultInputFile("src/test/java/FooTest.java")
      .setFile(javaTestFile1)
      .setType(InputFile.Type.TEST)
      .setKey("polop:src/test/java/FooTest.java"));
    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(false);

    scanner.execute();

    verify(inclusionPatternInitializer).initializePatternsForPath("src/main/java/Foo.java", "polop:src/main/java/Foo.java");
    verify(inclusionPatternInitializer).initializePatternsForPath("src/test/java/FooTest.java", "polop:src/test/java/FooTest.java");
    verify(exclusionPatternInitializer).initializePatternsForPath("src/main/java/Foo.java", "polop:src/main/java/Foo.java");
    verify(exclusionPatternInitializer).initializePatternsForPath("src/test/java/FooTest.java", "polop:src/test/java/FooTest.java");
    verifyZeroInteractions(regexpScanner);
  }

  @Test
  public void shouldReportFailure() throws IOException {
    File phpFile1 = new File(baseDir, "src/Foo.php");
    fs.add(new DefaultInputFile("src/Foo.php")
      .setFile(phpFile1)
      .setType(InputFile.Type.MAIN)
      .setKey("polop:src/Foo.php"));

    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(true);
    doThrow(new IOException("BUG")).when(regexpScanner).scan("polop:src/Foo.php", phpFile1, UTF_8);

    thrown.expect(SonarException.class);
    thrown.expectMessage("Unable to read the source file");

    scanner.execute();
  }
}
