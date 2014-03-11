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
package org.sonar.batch.scan.filesystem;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.PathUtils;
import org.sonar.batch.bootstrap.AnalysisMode;

import java.io.File;
import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InputFileBuilderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  LanguageDetection langDetection = mock(LanguageDetection.class);
  StatusDetection statusDetection = mock(StatusDetection.class);
  DefaultModuleFileSystem fs = mock(DefaultModuleFileSystem.class);
  AnalysisMode analysisMode = mock(AnalysisMode.class);

  @Test
  public void complete_input_file() throws Exception {
    // file system
    File basedir = temp.newFolder();
    File srcFile = new File(basedir, "src/main/java/foo/Bar.java");
    FileUtils.touch(srcFile);
    FileUtils.write(srcFile, "single line");
    when(fs.baseDir()).thenReturn(basedir);
    when(fs.encoding()).thenReturn(Charsets.UTF_8);

    // lang
    when(langDetection.language(any(InputFile.class))).thenReturn("java");

    // status
    when(statusDetection.status("src/main/java/foo/Bar.java", "6c1d64c0b3555892fe7273e954f6fb5a"))
      .thenReturn(InputFile.Status.ADDED);

    InputFileBuilder builder = new InputFileBuilder("struts", new PathResolver(),
      langDetection, statusDetection, fs, analysisMode);
    DefaultInputFile inputFile = builder.create(srcFile);
    inputFile = builder.complete(inputFile, InputFile.Type.MAIN);

    assertThat(inputFile.type()).isEqualTo(InputFile.Type.MAIN);
    assertThat(inputFile.file()).isEqualTo(srcFile.getAbsoluteFile());
    assertThat(inputFile.absolutePath()).isEqualTo(PathUtils.sanitize(srcFile.getAbsolutePath()));
    assertThat(inputFile.language()).isEqualTo("java");
    assertThat(inputFile.key()).isEqualTo("struts:src/main/java/foo/Bar.java");
    assertThat(inputFile.relativePath()).isEqualTo("src/main/java/foo/Bar.java");
    assertThat(inputFile.lines()).isEqualTo(1);
    assertThat(inputFile.sourceDirAbsolutePath()).isNull();
    assertThat(inputFile.pathRelativeToSourceDir()).isNull();
    assertThat(inputFile.deprecatedKey()).isNull();
  }

  @Test
  public void return_null_if_file_outside_basedir() throws Exception {
    // file system
    File basedir = temp.newFolder();
    File otherDir = temp.newFolder();
    File srcFile = new File(otherDir, "src/main/java/foo/Bar.java");
    FileUtils.touch(srcFile);
    when(fs.baseDir()).thenReturn(basedir);

    InputFileBuilder builder = new InputFileBuilder("struts", new PathResolver(),
      langDetection, statusDetection, fs, analysisMode);
    DefaultInputFile inputFile = builder.create(srcFile);

    assertThat(inputFile).isNull();
  }

  @Test
  public void return_null_if_language_not_detected() throws Exception {
    // file system
    File basedir = temp.newFolder();
    File srcFile = new File(basedir, "src/main/java/foo/Bar.java");
    FileUtils.touch(srcFile);
    FileUtils.write(srcFile, "single line");
    when(fs.baseDir()).thenReturn(basedir);
    when(fs.encoding()).thenReturn(Charsets.UTF_8);

    // lang
    when(langDetection.language(any(InputFile.class))).thenReturn(null);

    InputFileBuilder builder = new InputFileBuilder("struts", new PathResolver(),
      langDetection, statusDetection, fs, analysisMode);
    DefaultInputFile inputFile = builder.create(srcFile);
    inputFile = builder.complete(inputFile, InputFile.Type.MAIN);

    assertThat(inputFile).isNull();
  }

  @Test
  public void fill_deprecated_data_of_java_file() throws Exception {
    // file system
    File basedir = temp.newFolder();
    File srcFile = new File(basedir, "src/main/java/foo/Bar.java");
    FileUtils.touch(srcFile);
    FileUtils.write(srcFile, "single line");
    when(fs.baseDir()).thenReturn(basedir);
    when(fs.encoding()).thenReturn(Charsets.UTF_8);
    File sourceDir = new File(basedir, "src/main/java");
    when(fs.sourceDirs()).thenReturn(Arrays.asList(sourceDir));

    // lang
    when(langDetection.language(any(InputFile.class))).thenReturn("java");

    // status
    when(statusDetection.status("src/main/java/foo/Bar.java", "6c1d64c0b3555892fe7273e954f6fb5a"))
      .thenReturn(InputFile.Status.ADDED);

    InputFileBuilder builder = new InputFileBuilder("struts", new PathResolver(),
      langDetection, statusDetection, fs, analysisMode);
    DefaultInputFile inputFile = builder.create(srcFile);
    inputFile = builder.complete(inputFile, InputFile.Type.MAIN);

    assertThat(inputFile.pathRelativeToSourceDir()).isEqualTo("foo/Bar.java");
    assertThat(inputFile.sourceDirAbsolutePath()).isEqualTo(PathUtils.sanitize(sourceDir.getAbsolutePath()));
    assertThat(inputFile.deprecatedKey()).isEqualTo("struts:foo.Bar");
  }

  @Test
  public void fill_deprecated_data_of_non_java_file() throws Exception {
    // file system
    File basedir = temp.newFolder();
    File srcFile = new File(basedir, "src/foo/Bar.php");
    FileUtils.touch(srcFile);
    FileUtils.write(srcFile, "single line");
    when(fs.baseDir()).thenReturn(basedir);
    when(fs.encoding()).thenReturn(Charsets.UTF_8);
    File sourceDir = new File(basedir, "src");
    when(fs.sourceDirs()).thenReturn(Arrays.asList(sourceDir));

    // lang
    when(langDetection.language(any(InputFile.class))).thenReturn("php");

    // status
    when(statusDetection.status("src/Bar.php", "6c1d64c0b3555892fe7273e954f6fb5a"))
      .thenReturn(InputFile.Status.ADDED);

    InputFileBuilder builder = new InputFileBuilder("struts", new PathResolver(),
      langDetection, statusDetection, fs, analysisMode);
    DefaultInputFile inputFile = builder.create(srcFile);
    inputFile = builder.complete(inputFile, InputFile.Type.MAIN);

    assertThat(inputFile.pathRelativeToSourceDir()).isEqualTo("foo/Bar.php");
    assertThat(inputFile.sourceDirAbsolutePath()).isEqualTo(PathUtils.sanitize(sourceDir.getAbsolutePath()));
    assertThat(inputFile.deprecatedKey()).isEqualTo("struts:foo/Bar.php");

  }
}
