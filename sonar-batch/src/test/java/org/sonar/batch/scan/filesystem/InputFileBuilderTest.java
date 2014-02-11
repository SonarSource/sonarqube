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
package org.sonar.batch.scan.filesystem;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.scan.filesystem.InputFile;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.scan.filesystem.internal.DefaultInputFile;
import org.sonar.api.utils.PathUtils;

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

  @Test
  public void create_input_file() throws Exception {
    // file system
    File basedir = temp.newFolder();
    File srcFile = new File(basedir, "src/main/java/foo/Bar.java");
    FileUtils.touch(srcFile);
    FileUtils.write(srcFile, "single line");
    when(fs.baseDir()).thenReturn(basedir);
    when(fs.sourceCharset()).thenReturn(Charsets.UTF_8);

    // lang
    when(langDetection.language(any(InputFile.class))).thenReturn("java");

    // status
    when(statusDetection.status("src/main/java/foo/Bar.java", "6c1d64c0b3555892fe7273e954f6fb5a")).thenReturn(InputFile.STATUS_ADDED);

    InputFileBuilder builder = new InputFileBuilder("struts", new PathResolver(),
      langDetection, statusDetection, fs);
    DefaultInputFile inputFile = builder.create(srcFile, InputFile.TYPE_MAIN);

    assertThat(inputFile.name()).isEqualTo("Bar.java");
    assertThat(inputFile.file()).isEqualTo(srcFile.getAbsoluteFile());
    assertThat(inputFile.absolutePath()).isEqualTo(PathUtils.sanitize(srcFile.getAbsolutePath()));
    assertThat(inputFile.language()).isEqualTo("java");
    assertThat(inputFile.attribute(DefaultInputFile.ATTRIBUTE_COMPONENT_KEY)).isEqualTo("struts:src/main/java/foo/Bar.java");
    assertThat(inputFile.path()).isEqualTo("src/main/java/foo/Bar.java");

    assertThat(inputFile.attribute(InputFile.ATTRIBUTE_LINE_COUNT)).isEqualTo("1");
    assertThat(inputFile.attribute(DefaultInputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH)).isNull();
    assertThat(inputFile.attribute(DefaultInputFile.ATTRIBUTE_SOURCEDIR_PATH)).isNull();
    assertThat(inputFile.attribute(DefaultInputFile.ATTRIBUTE_COMPONENT_DEPRECATED_KEY)).isNull();
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
      langDetection, statusDetection, fs);
    DefaultInputFile inputFile = builder.create(srcFile, InputFile.TYPE_MAIN);

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
    when(fs.sourceCharset()).thenReturn(Charsets.UTF_8);

    // lang
    when(langDetection.language(any(InputFile.class))).thenReturn(null);

    InputFileBuilder builder = new InputFileBuilder("struts", new PathResolver(),
      langDetection, statusDetection, fs);
    DefaultInputFile inputFile = builder.create(srcFile, InputFile.TYPE_MAIN);

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
    when(fs.sourceCharset()).thenReturn(Charsets.UTF_8);
    File sourceDir = new File(basedir, "src/main/java");
    when(fs.sourceDirs()).thenReturn(Arrays.asList(sourceDir));

    // lang
    when(langDetection.language(any(InputFile.class))).thenReturn("java");

    // status
    when(statusDetection.status("src/main/java/foo/Bar.java", "6c1d64c0b3555892fe7273e954f6fb5a")).thenReturn(InputFile.STATUS_ADDED);

    InputFileBuilder builder = new InputFileBuilder("struts", new PathResolver(),
      langDetection, statusDetection, fs);
    DefaultInputFile inputFile = builder.create(srcFile, InputFile.TYPE_MAIN);

    assertThat(inputFile.attribute(DefaultInputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH)).isEqualTo("foo/Bar.java");
    assertThat(inputFile.attribute(DefaultInputFile.ATTRIBUTE_SOURCEDIR_PATH)).isEqualTo(PathUtils.sanitize(sourceDir.getAbsolutePath()));
    assertThat(inputFile.attribute(DefaultInputFile.ATTRIBUTE_COMPONENT_DEPRECATED_KEY)).isEqualTo("struts:foo.Bar");
  }

  @Test
  public void fill_deprecated_data_of_non_java_file() throws Exception {
    // file system
    File basedir = temp.newFolder();
    File srcFile = new File(basedir, "src/foo/Bar.php");
    FileUtils.touch(srcFile);
    FileUtils.write(srcFile, "single line");
    when(fs.baseDir()).thenReturn(basedir);
    when(fs.sourceCharset()).thenReturn(Charsets.UTF_8);
    File sourceDir = new File(basedir, "src");
    when(fs.sourceDirs()).thenReturn(Arrays.asList(sourceDir));

    // lang
    when(langDetection.language(any(InputFile.class))).thenReturn("php");

    // status
    when(statusDetection.status("src/Bar.php", "6c1d64c0b3555892fe7273e954f6fb5a")).thenReturn(InputFile.STATUS_ADDED);

    InputFileBuilder builder = new InputFileBuilder("struts", new PathResolver(),
      langDetection, statusDetection, fs);
    DefaultInputFile inputFile = builder.create(srcFile, InputFile.TYPE_MAIN);

    assertThat(inputFile.attribute(DefaultInputFile.ATTRIBUTE_SOURCE_RELATIVE_PATH)).isEqualTo("foo/Bar.php");
    assertThat(inputFile.attribute(DefaultInputFile.ATTRIBUTE_SOURCEDIR_PATH)).isEqualTo(PathUtils.sanitize(sourceDir.getAbsolutePath()));
    assertThat(inputFile.attribute(DefaultInputFile.ATTRIBUTE_COMPONENT_DEPRECATED_KEY)).isEqualTo("struts:foo/Bar.php");

  }
}
