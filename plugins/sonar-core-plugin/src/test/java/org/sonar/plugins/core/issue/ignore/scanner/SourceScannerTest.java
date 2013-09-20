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

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.SonarException;
import org.sonar.plugins.core.issue.ignore.pattern.ExclusionPatternInitializer;
import org.sonar.plugins.core.issue.ignore.pattern.InclusionPatternInitializer;
import org.sonar.plugins.core.issue.ignore.pattern.PatternMatcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
  private ModuleFileSystem fileSystem;

  private Project project;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);

    Project realProject = new Project("polop");
    project = Mockito.spy(realProject);
    Mockito.doReturn("java").when(project).getLanguageKey();
    when(fileSystem.sourceCharset()).thenReturn(UTF_8);

    scanner = new SourceScanner(regexpScanner, exclusionPatternInitializer, inclusionPatternInitializer, fileSystem);
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
  public void shouldAnalyseJavaProject() throws IOException {
    File sourceFile = new File("src/main/java/Foo.java");
    File testFile = new File("src/test/java/FooTest.java");

    when(project.getLanguageKey()).thenReturn("java");
    when(fileSystem.files(Mockito.isA(FileQuery.class)))
      .thenReturn(Arrays.asList(sourceFile))
      .thenReturn(Arrays.asList(testFile));
    when(fileSystem.sourceDirs()).thenReturn(Arrays.asList(new File("src/main/java")));
    when(fileSystem.testDirs()).thenReturn(Arrays.asList(new File("src/test/java")));
    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(true);

    scanner.analyse(project, null);

    verify(inclusionPatternInitializer).initializePatternsForPath("Foo.java", "polop:[default].Foo");
    verify(inclusionPatternInitializer).initializePatternsForPath("FooTest.java", "polop:[default].FooTest");
    verify(exclusionPatternInitializer).initializePatternsForPath("Foo.java", "polop:[default].Foo");
    verify(exclusionPatternInitializer).initializePatternsForPath("FooTest.java", "polop:[default].FooTest");
    verify(regexpScanner).scan("polop:[default].Foo", sourceFile, UTF_8);
    verify(regexpScanner).scan("polop:[default].FooTest", testFile, UTF_8);
  }

  @Test
  public void shouldAnalyseFilesOnlyWhenRegexConfigured() throws IOException {
    File sourceFile = new File("src/main/java/Foo.java");
    File testFile = new File("src/test/java/FooTest.java");

    when(project.getLanguageKey()).thenReturn("java");
    when(fileSystem.files(Mockito.isA(FileQuery.class)))
      .thenReturn(Arrays.asList(sourceFile))
      .thenReturn(Arrays.asList(testFile));
    when(fileSystem.sourceDirs()).thenReturn(Arrays.asList(new File("src/main/java")));
    when(fileSystem.testDirs()).thenReturn(Arrays.asList(new File("src/test/java")));
    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(false);

    scanner.analyse(project, null);

    verify(inclusionPatternInitializer).initializePatternsForPath("Foo.java", "polop:[default].Foo");
    verify(inclusionPatternInitializer).initializePatternsForPath("FooTest.java", "polop:[default].FooTest");
    verify(exclusionPatternInitializer).initializePatternsForPath("Foo.java", "polop:[default].Foo");
    verify(exclusionPatternInitializer).initializePatternsForPath("FooTest.java", "polop:[default].FooTest");
    verifyZeroInteractions(regexpScanner);
  }

  @Test
  public void shouldAnalyseOtherProject() throws IOException {
    File sourceFile = new File("Foo.php");
    File testFile = new File("FooTest.php");

    when(project.getLanguageKey()).thenReturn("php");
    when(fileSystem.files(Mockito.isA(FileQuery.class)))
      .thenReturn(Arrays.asList(sourceFile))
      .thenReturn(Arrays.asList(testFile));
    when(fileSystem.sourceDirs()).thenReturn(ImmutableList.of(new File("")));
    when(fileSystem.testDirs()).thenReturn(ImmutableList.of(new File("")));
    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(true);

    scanner.analyse(project, null);

    verify(inclusionPatternInitializer).initializePatternsForPath("Foo.php", "polop:Foo.php");
    verify(inclusionPatternInitializer).initializePatternsForPath("FooTest.php", "polop:FooTest.php");
    verify(exclusionPatternInitializer).initializePatternsForPath("Foo.php", "polop:Foo.php");
    verify(exclusionPatternInitializer).initializePatternsForPath("FooTest.php", "polop:FooTest.php");
    verify(regexpScanner).scan("polop:Foo.php", sourceFile, UTF_8);
    verify(regexpScanner).scan("polop:FooTest.php", testFile, UTF_8);
  }

  @Test
  public void shouldAnalyseJavaProjectWithNonJavaFile() throws IOException {
    File sourceFile = new File("src/main/java/Foo.java");
    File otherFile = new File("other.js");

    when(project.getLanguageKey()).thenReturn("java");
    List<File> empty = Collections.emptyList();
    when(fileSystem.files(Mockito.isA(FileQuery.class)))
      .thenReturn(Arrays.asList(sourceFile, otherFile))
      .thenReturn(empty);
    when(fileSystem.sourceDirs()).thenReturn(ImmutableList.of(new File("src/main/java"), new File("")));
    when(fileSystem.testDirs()).thenReturn(ImmutableList.of(new File("src/test/java")));
    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(true);

    scanner.analyse(project, null);

    verify(inclusionPatternInitializer).initializePatternsForPath("Foo.java", "polop:[default].Foo");
    verify(exclusionPatternInitializer).initializePatternsForPath("Foo.java", "polop:[default].Foo");
    verify(regexpScanner).scan("polop:[default].Foo", sourceFile, UTF_8);
    verify(regexpScanner, never()).scan("other.js", otherFile, UTF_8);
  }

  @Test
  public void shouldIgnoreInvalidFile() throws IOException {
    File sourceFile = new File("invalid.java");

    when(project.getLanguageKey()).thenReturn("java");
    List<File> empty = Collections.emptyList();
    when(fileSystem.files(Mockito.isA(FileQuery.class)))
      .thenReturn(Arrays.asList(sourceFile))
      .thenReturn(empty);
    when(fileSystem.sourceDirs()).thenReturn(Arrays.asList(new File("src/main/java")));
    when(fileSystem.testDirs()).thenReturn(Arrays.asList(new File("src/test/java")));
    when(exclusionPatternInitializer.hasFileContentPattern()).thenReturn(true);

    scanner.analyse(project, null);

    verifyNoMoreInteractions(regexpScanner);
  }

  @Test
  public void shouldReportFailure() throws IOException {
    File sourceFile = new File("Foo.php");

    when(project.getLanguageKey()).thenReturn("php");
    when(fileSystem.files(Mockito.isA(FileQuery.class))).thenReturn(Arrays.asList(sourceFile));
    doThrow(new IOException("BUG")).when(regexpScanner).scan("polop:Foo.php", sourceFile, UTF_8);

    thrown.expect(SonarException.class);
    thrown.expectMessage("Unable to read the source file");

    scanner.analyse(project, null);
  }
}
