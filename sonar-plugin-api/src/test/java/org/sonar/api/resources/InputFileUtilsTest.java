/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.resources;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InputFileUtilsTest {
  private static final File BASE_DIR = new File("target/tmp/InputFileUtilsTest");

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void shouldCreateInputFileWithRelativePath() {
    String relativePath = "org/sonar/Foo.java";

    InputFile inputFile = InputFileUtils.create(BASE_DIR, relativePath);

    assertThat(inputFile.getFileBaseDir()).isEqualTo(BASE_DIR);
    assertThat(inputFile.getRelativePath()).isEqualTo(relativePath);
    assertThat(inputFile.getFile()).isEqualTo(new File("target/tmp/InputFileUtilsTest/org/sonar/Foo.java"));
  }

  @Test
  public void shouldNotAcceptFileWithWrongbaseDir() {
    File baseDir1 = new File(BASE_DIR, "baseDir1");
    File baseDir2 = new File(BASE_DIR, "baseDir2");

    InputFile inputFile = InputFileUtils.create(baseDir1, new File(baseDir2, "org/sonar/Foo.java"));

    assertThat(inputFile).isNull();
  }

  @Test
  public void shouldGuessRelativePath() {
    File file = new File(BASE_DIR, "org/sonar/Foo.java");

    InputFile inputFile = InputFileUtils.create(BASE_DIR, file);

    assertThat(inputFile.getFileBaseDir()).isEqualTo(BASE_DIR);
    assertThat(inputFile.getFile()).isEqualTo(file);
    assertThat(inputFile.getRelativePath()).isEqualTo("org/sonar/Foo.java");
  }

  @Test
  public void testEqualsAndHashCode() {
    InputFile inputFile1 = InputFileUtils.create(BASE_DIR, "org/sonar/Foo.java");
    InputFile inputFile2 = InputFileUtils.create(BASE_DIR, "org/sonar/Foo.java");
    InputFile inputFile3 = InputFileUtils.create(BASE_DIR, "org/sonar/Bar.java");

    assertThat(inputFile1).isEqualTo(inputFile1).isEqualTo(inputFile2);
    assertThat(inputFile1.hashCode()).isEqualTo(inputFile2.hashCode());
    assertThat(inputFile1).isNotEqualTo(inputFile3);
  }

  @Test
  public void shouldNotEqualFile() {
    File file = new File(BASE_DIR, "org/sonar/Foo.java");

    InputFile inputFile = InputFileUtils.create(BASE_DIR, file);

    assertThat(inputFile.getFile()).isEqualTo(file);
    assertThat(inputFile).isNotEqualTo(file);
  }

  @Test
  public void shouldNotEqualIfbaseDirAreDifferents() {
    InputFile inputFile1 = InputFileUtils.create(BASE_DIR, "org/sonar/Foo.java");
    InputFile inputFile2 = InputFileUtils.create(new File(BASE_DIR, "org"), "sonar/Foo.java");

    assertThat(inputFile1).isNotEqualTo(inputFile2);
  }

  @Test
  public void testToString() {
    InputFile inputFile = InputFileUtils.create(BASE_DIR, "org/sonar/Foo.java");

    assertThat(inputFile.toString()).endsWith("InputFileUtilsTest -> org/sonar/Foo.java");
  }

  @Test
  public void testToFiles() {
    List<InputFile> inputFiles = Arrays.asList(InputFileUtils.create(BASE_DIR, "Foo.java"), InputFileUtils.create(BASE_DIR, "Bar.java"));
    List<File> files = InputFileUtils.toFiles(inputFiles);

    assertThat(files).containsExactly(new File(BASE_DIR, "Foo.java"), new File(BASE_DIR, "Bar.java"));
  }

  @Test
  public void testCreateList() {
    File file1 = new File(BASE_DIR, "org/sonar/Foo.java");
    File file2 = new File(BASE_DIR, "org/sonar/Bar.java");
    File wrongFile = new File("somewhere/else/org/sonar/Foo.java");

    List<InputFile> inputFiles = InputFileUtils.create(BASE_DIR, Arrays.asList(file1, file2, wrongFile));

    assertThat(inputFiles).hasSize(2);
    assertThat(inputFiles.get(0).getFile()).isEqualTo(file1);
    assertThat(inputFiles.get(1).getFile()).isEqualTo(file2);
  }

  @Test
  public void shouldExtractRelativeDirectory() {
    InputFile inputFile = InputFileUtils.create(BASE_DIR, "org/sonar/Foo.java");
    assertThat(InputFileUtils.getRelativeDirectory(inputFile)).isEqualTo("org/sonar");

    inputFile = InputFileUtils.create(BASE_DIR, "Foo.java");
    assertThat(InputFileUtils.getRelativeDirectory(inputFile)).isEmpty();
  }

  @Test
  public void should_get_file_content_as_buffered_input_stream() throws IOException {
    InputFile inputFile = InputFileUtils.create(BASE_DIR, "org/sonar/Foo.java");
    write("<FILE CONTENT>", inputFile.getFile());

    InputStream inputStream = inputFile.getInputStream();

    assertThat(inputStream).isInstanceOf(BufferedInputStream.class);
    assertThat(read(inputStream)).isEqualTo("<FILE CONTENT>");
  }

  @Test
  public void should_fail_to_get_input_stream_of_unknown_file() throws IOException {
    InputFile inputFile = InputFileUtils.create(BASE_DIR, "UNKNOWN.java");

    exception.expect(FileNotFoundException.class);
    exception.expectMessage(BASE_DIR.getPath());
    exception.expectMessage("UNKNOWN.java");

    inputFile.getInputStream();
  }

  static void write(String content, File file) throws IOException {
    file.getParentFile().mkdirs();
    Files.write(content, file, StandardCharsets.UTF_8);
  }

  static String read(InputStream input) throws IOException {
    try {
      return new String(ByteStreams.toByteArray(input), StandardCharsets.UTF_8.displayName());
    } finally {
      Closeables.closeQuietly(input);
    }
  }
}
