/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

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
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.scanner.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.PatternMatcher;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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

  private DefaultFileSystem fs;
  private IssueExclusionsLoader scanner;
  private Path baseDir;

  @Before
  public void before() throws Exception {
    baseDir = temp.newFolder().toPath();
    fs = new DefaultFileSystem(baseDir).setEncoding(UTF_8);
    MockitoAnnotations.initMocks(this);
    scanner = new IssueExclusionsLoader(regexpScanner, exclusionPatternInitializer, inclusionPatternInitializer, fs);
  }

  @Test
  public void testToString() throws Exception {
    assertThat(scanner.toString()).isEqualTo("Issues Exclusions - Source Scanner");
  }

  @Test
  public void shouldExecute() {
    when(exclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(true);
    when(inclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(true);
    assertThat(scanner.shouldExecute()).isTrue();

    when(exclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(true);
    when(inclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(false);
    assertThat(scanner.shouldExecute()).isTrue();

    when(exclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(false);
    when(inclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(true);
    assertThat(scanner.shouldExecute()).isTrue();

    when(exclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(false);
    when(inclusionPatternInitializer.hasConfiguredPatterns()).thenReturn(false);
    assertThat(scanner.shouldExecute()).isFalse();
  }

  @Test
  public void shouldAnalyzeProject() throws IOException {
    Path javaFile1 = baseDir.resolve("src/main/java/Foo.java");
    fs.add(new TestInputFileBuilder("polop", "src/main/java/Foo.java")
      .setModuleBaseDir(baseDir)
      .setCharset(StandardCharsets.UTF_8)
      .setType(InputFile.Type.MAIN)
      .build());
    Path javaTestFile1 = baseDir.resolve("src/test/java/FooTest.java");
    fs.add(new TestInputFileBuilder("polop", "src/test/java/FooTest.java")
      .setModuleBaseDir(baseDir)
      .setCharset(StandardCharsets.UTF_8)
      .setType(InputFile.Type.TEST)
      .build());

    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(true);

    scanner.preLoadAllFiles();

    verify(inclusionPatternInitializer).initializePatternsForPath("src/main/java/Foo.java", "polop:src/main/java/Foo.java");
    verify(inclusionPatternInitializer).initializePatternsForPath("src/test/java/FooTest.java", "polop:src/test/java/FooTest.java");
    verify(exclusionPatternInitializer).initializePatternsForPath("src/main/java/Foo.java", "polop:src/main/java/Foo.java");
    verify(exclusionPatternInitializer).initializePatternsForPath("src/test/java/FooTest.java", "polop:src/test/java/FooTest.java");
    verify(regexpScanner).scan("polop:src/main/java/Foo.java", javaFile1, UTF_8);
    verify(regexpScanner).scan("polop:src/test/java/FooTest.java", javaTestFile1, UTF_8);
  }

  @Test
  public void isLoaded() {
    DefaultInputFile inputFile1 = new TestInputFileBuilder("polop", "src/test/java/FooTest1.java")
      .setModuleBaseDir(baseDir)
      .setCharset(StandardCharsets.UTF_8)
      .setType(InputFile.Type.TEST)
      .build();
    DefaultInputFile inputFile2 = new TestInputFileBuilder("polop", "src/test/java/FooTest2.java")
      .setModuleBaseDir(baseDir)
      .setCharset(StandardCharsets.UTF_8)
      .setType(InputFile.Type.TEST)
      .build();

    when(inclusionPatternInitializer.getPathForComponent(inputFile1.key())).thenReturn(null);
    when(inclusionPatternInitializer.getPathForComponent(inputFile2.key())).thenReturn("path1");

    assertFalse(scanner.isLoaded(inputFile1));
    assertTrue(scanner.isLoaded(inputFile2));

  }

  @Test
  public void shouldAnalyseFilesOnlyWhenRegexConfigured() {
    fs.add(new TestInputFileBuilder("polop", "src/main/java/Foo.java")
      .setType(InputFile.Type.MAIN)
      .setCharset(StandardCharsets.UTF_8)
      .build());
    fs.add(new TestInputFileBuilder("polop", "src/test/java/FooTest.java")
      .setType(InputFile.Type.TEST)
      .setCharset(StandardCharsets.UTF_8)
      .build());
    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(false);

    scanner.preLoadAllFiles();

    verify(inclusionPatternInitializer).initializePatternsForPath("src/main/java/Foo.java", "polop:src/main/java/Foo.java");
    verify(inclusionPatternInitializer).initializePatternsForPath("src/test/java/FooTest.java", "polop:src/test/java/FooTest.java");
    verify(exclusionPatternInitializer).initializePatternsForPath("src/main/java/Foo.java", "polop:src/main/java/Foo.java");
    verify(exclusionPatternInitializer).initializePatternsForPath("src/test/java/FooTest.java", "polop:src/test/java/FooTest.java");
    verifyZeroInteractions(regexpScanner);
  }

  @Test
  public void shouldReportFailure() throws IOException {
    Path phpFile1 = baseDir.resolve("src/Foo.php");
    fs.add(new TestInputFileBuilder("polop", "src/Foo.php")
      .setModuleBaseDir(baseDir)
      .setType(InputFile.Type.MAIN)
      .setCharset(StandardCharsets.UTF_8)
      .build());

    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(true);
    doThrow(new IOException("BUG")).when(regexpScanner).scan("polop:src/Foo.php", phpFile1, UTF_8);

    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("Unable to read the source file");

    scanner.preLoadAllFiles();
  }
}
