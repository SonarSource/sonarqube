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

import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;
import org.sonar.api.scan.filesystem.internal.InputFileBuilder;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;
import org.sonar.plugins.core.issue.ignore.pattern.ExclusionPatternInitializer;
import org.sonar.plugins.core.issue.ignore.pattern.InclusionPatternInitializer;
import org.sonar.plugins.core.issue.ignore.pattern.PatternMatcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static com.google.common.base.Charsets.UTF_8;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SourceScannerTest {

  private SourceScanner scanner;

  @Mock
  private RegexpScanner regexpScanner;
  @Mock
  private InclusionPatternInitializer inclusionPatternInitializer;
  @Mock
  private ExclusionPatternInitializer exclusionPatternInitializer;
  @Mock
  private PatternMatcher patternMatcher;
  @Mock
  private DefaultModuleFileSystem fs;

  private Project project;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File baseDir;

  @Before
  public void init() throws IOException {
    baseDir = temp.newFolder();
    MockitoAnnotations.initMocks(this);

    Project realProject = new Project("polop");
    project = Mockito.spy(realProject);
    Mockito.doReturn("java").when(project).getLanguageKey();
    when(fs.sourceCharset()).thenReturn(UTF_8);

    scanner = new SourceScanner(regexpScanner, exclusionPatternInitializer, inclusionPatternInitializer, fs);
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
  public void shouldAnalyseProject() throws IOException {
    File javaFile1 = new File(baseDir, "src/main/java/Foo.java");
    File javaTestFile1 = new File(baseDir, "src/test/java/FooTest.java");
    when(fs.inputFiles(FileQuery.all())).thenReturn((Iterable) Arrays.asList(
      new InputFileBuilder(javaFile1, Charsets.UTF_8, "src/main/java/Foo.java")
        .attribute(DefaultInputFile.ATTRIBUTE_COMPONENT_KEY, "polop:src/main/java/Foo.java")
        .build(),
      new InputFileBuilder(javaTestFile1, Charsets.UTF_8, "src/test/java/FooTest.java")
        .attribute(DefaultInputFile.ATTRIBUTE_COMPONENT_KEY, "polop:src/test/java/FooTest.java")
        .build()));

    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(true);

    scanner.analyse(project, null);

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
    File javaTestFile1 = new File(baseDir, "src/test/java/FooTest.java");
    when(fs.inputFiles(FileQuery.all())).thenReturn((Iterable) Arrays.asList(
      new InputFileBuilder(javaFile1, Charsets.UTF_8, "src/main/java/Foo.java")
        .attribute(DefaultInputFile.ATTRIBUTE_COMPONENT_KEY, "polop:src/main/java/Foo.java")
        .build(),
      new InputFileBuilder(javaTestFile1, Charsets.UTF_8, "src/test/java/FooTest.java")
        .attribute(DefaultInputFile.ATTRIBUTE_COMPONENT_KEY, "polop:src/test/java/FooTest.java")
        .build()));

    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(false);

    scanner.analyse(project, null);

    verify(inclusionPatternInitializer).initializePatternsForPath("src/main/java/Foo.java", "polop:src/main/java/Foo.java");
    verify(inclusionPatternInitializer).initializePatternsForPath("src/test/java/FooTest.java", "polop:src/test/java/FooTest.java");
    verify(exclusionPatternInitializer).initializePatternsForPath("src/main/java/Foo.java", "polop:src/main/java/Foo.java");
    verify(exclusionPatternInitializer).initializePatternsForPath("src/test/java/FooTest.java", "polop:src/test/java/FooTest.java");
    verifyZeroInteractions(regexpScanner);
  }

  @Test
  public void shouldReportFailure() throws IOException {
    File phpFile1 = new File(baseDir, "src/Foo.php");
    when(fs.inputFiles(FileQuery.all())).thenReturn((Iterable) Arrays.asList(
      new InputFileBuilder(phpFile1, Charsets.UTF_8, "src/Foo.php")
        .attribute(DefaultInputFile.ATTRIBUTE_COMPONENT_KEY, "polop:src/Foo.php")
        .build()));

    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(true);
    doThrow(new IOException("BUG")).when(regexpScanner).scan("polop:src/Foo.php", phpFile1, UTF_8);

    thrown.expect(SonarException.class);
    thrown.expectMessage("Unable to read the source file");

    scanner.analyse(project, null);
  }
}
