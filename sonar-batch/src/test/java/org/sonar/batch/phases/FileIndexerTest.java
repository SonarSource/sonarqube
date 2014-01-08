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
package org.sonar.batch.phases;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.AbstractLanguage;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.internal.InputFile;
import org.sonar.api.scan.filesystem.internal.InputFileBuilder;
import org.sonar.batch.scan.filesystem.DefaultModuleFileSystem;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileIndexerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void should_index_java_files() throws IOException {
    File baseDir = temp.newFolder();
    DefaultModuleFileSystem fs = mock(DefaultModuleFileSystem.class);
    File javaFile1 = new File(baseDir, "src/main/java/foo/bar/Foo.java");
    File javaFile2 = new File(baseDir, "src/main/java2/foo/bar/Foo.java");
    when(fs.inputFiles(FileQuery.onSource().onLanguage(Java.KEY))).thenReturn((Iterable) Arrays.asList(
      new InputFileBuilder(javaFile1, "src/main/java/foo/bar/Foo.java").attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "foo/bar/Foo.java").build(),
      new InputFileBuilder(javaFile2, "src/main/java2/foo/bar/Foo.java").attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "foo/bar/Foo.java").build()));
    File javaTestFile1 = new File(baseDir, "src/test/java/foo/bar/FooTest.java");
    when(fs.inputFiles(FileQuery.onTest().onLanguage(Java.KEY))).thenReturn((Iterable) Arrays.asList(
      new InputFileBuilder(javaTestFile1, "src/test/java/foo/bar/FooTest.java").attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "foo/bar/FooTest.java").build()));
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn(Java.KEY);
    FileIndexer indexer = new FileIndexer(project, fs, new Languages(Java.INSTANCE));
    SensorContext sensorContext = mock(SensorContext.class);
    indexer.execute(sensorContext);

    verify(sensorContext).index(new JavaFile("foo.bar.Foo", false).setPath("/src/main/java/foo/bar/Foo.java"));
    verify(sensorContext).index(new JavaFile("foo.bar.Foo", false).setPath("/src/main/java2/foo/bar/Foo.java"));
    verify(sensorContext).index(new JavaFile("foo.bar.FooTest", true).setPath("/src/test/java/foo/bar/FooTest.java"));
  }

  @Test
  public void should_index_cobol_files() throws IOException {
    File baseDir = temp.newFolder();
    DefaultModuleFileSystem fs = mock(DefaultModuleFileSystem.class);
    File cobolFile1 = new File(baseDir, "src/foo/bar/Foo.cbl");
    File cobolFile2 = new File(baseDir, "src2/foo/bar/Foo.cbl");
    when(fs.inputFiles(FileQuery.onSource().onLanguage("cobol"))).thenReturn((Iterable) Arrays.asList(
      new InputFileBuilder(cobolFile1, "src/foo/bar/Foo.cbl").attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "foo/bar/Foo.cbl").build(),
      new InputFileBuilder(cobolFile2, "src2/foo/bar/Foo.cbl").attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "foo/bar/Foo.cbl").build()));
    File cobolTestFile1 = new File(baseDir, "src/test/foo/bar/FooTest.cbl");
    when(fs.inputFiles(FileQuery.onTest().onLanguage("cobol"))).thenReturn((Iterable) Arrays.asList(
      new InputFileBuilder(cobolTestFile1, "src/test/foo/bar/FooTest.cbl").attribute(InputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH, "foo/bar/FooTest.cbl").build()));
    Project project = mock(Project.class);
    when(project.getLanguageKey()).thenReturn("cobol");
    FileIndexer indexer = new FileIndexer(project, fs, new Languages(new AbstractLanguage("cobol") {
      @Override
      public String[] getFileSuffixes() {
        return new String[] {"cbl"};
      }
    }));
    SensorContext sensorContext = mock(SensorContext.class);
    indexer.execute(sensorContext);

    verify(sensorContext).index(new org.sonar.api.resources.File("foo/bar/Foo.cbl").setPath("/src/foo/bar/Foo.cbl"));
    verify(sensorContext).index(new org.sonar.api.resources.File("foo/bar/Foo.cbl").setPath("/src2/foo/bar/Foo.cbl"));
    verify(sensorContext).index(new org.sonar.api.resources.File("foo/bar/FooTest.cbl").setPath("/src/test/foo/bar/FooTest.cbl"));
  }

}
