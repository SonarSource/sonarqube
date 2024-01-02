/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.api.batch.fs.internal.fs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultIndexedFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.fs.internal.Metadata;
import org.sonar.api.batch.fs.internal.SensorStrategy;
import org.sonar.api.notifications.AnalysisWarnings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class DefaultInputFileTest {

  private static final String PROJECT_RELATIVE_PATH = "module1/src/Foo.php";
  private static final String MODULE_RELATIVE_PATH = "src/Foo.php";
  private static final String OLD_RELATIVE_PATH = "src/previous/Foo.php";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private DefaultIndexedFile indexedFile;

  private Path baseDir;
  private SensorStrategy sensorStrategy;

  @Before
  public void prepare() throws IOException {
    baseDir = temp.newFolder().toPath();
    sensorStrategy = new SensorStrategy();
    indexedFile = new DefaultIndexedFile(baseDir.resolve(PROJECT_RELATIVE_PATH), "ABCDE", PROJECT_RELATIVE_PATH, MODULE_RELATIVE_PATH, InputFile.Type.TEST, "php", 0,
      sensorStrategy);
  }

  @Test
  public void status_whenScmAvailable_shouldUseScmToCompute() {
    Consumer<DefaultInputFile> metadata = mock(Consumer.class);
    Consumer<DefaultInputFile> scmStatus = f -> f.setStatus(InputFile.Status.SAME);

    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, metadata, scmStatus);
    assertThat(inputFile.status()).isEqualTo(InputFile.Status.SAME);
    assertThat(inputFile.isStatusSet()).isTrue();
    verifyNoInteractions(metadata);
  }

  @Test
  public void status_whenNoScmAvailable_shouldUseMetadataToCompute() {
    Consumer<DefaultInputFile> metadata = f -> f.setStatus(InputFile.Status.ADDED);
    Consumer<DefaultInputFile> scmStatus = mock(Consumer.class);

    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, metadata, scmStatus);
    assertThat(inputFile.status()).isEqualTo(InputFile.Status.ADDED);
    assertThat(inputFile.isStatusSet()).isTrue();
    verify(scmStatus).accept(inputFile);
  }

  @Test
  public void test() {
    Metadata metadata = new Metadata(42, 42, "", new int[0], new int[0], 10);
    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, f -> f.setMetadata(metadata), NO_OP)
      .setStatus(InputFile.Status.ADDED)
      .setCharset(StandardCharsets.ISO_8859_1);

    assertThat(inputFile.absolutePath()).endsWith("Foo.php");
    assertThat(inputFile.filename()).isEqualTo("Foo.php");
    assertThat(inputFile.uri()).hasPath(baseDir.resolve(PROJECT_RELATIVE_PATH).toUri().getPath());
    assertThat(new File(inputFile.absolutePath())).isAbsolute();
    assertThat(inputFile.language()).isEqualTo("php");
    assertThat(inputFile.status()).isEqualTo(InputFile.Status.ADDED);
    assertThat(inputFile.type()).isEqualTo(InputFile.Type.TEST);
    assertThat(inputFile.lines()).isEqualTo(42);
    assertThat(inputFile.charset()).isEqualTo(StandardCharsets.ISO_8859_1);

    assertThat(inputFile.getModuleRelativePath()).isEqualTo(MODULE_RELATIVE_PATH);
    assertThat(inputFile.getProjectRelativePath()).isEqualTo(PROJECT_RELATIVE_PATH);

    sensorStrategy.setGlobal(false);
    assertThat(inputFile.relativePath()).isEqualTo(MODULE_RELATIVE_PATH);
    assertThat(new File(inputFile.relativePath())).isRelative();
    sensorStrategy.setGlobal(true);
    assertThat(inputFile.relativePath()).isEqualTo(PROJECT_RELATIVE_PATH);
    assertThat(new File(inputFile.relativePath())).isRelative();
  }

  @Test
  public void test_moved_file() {
    DefaultIndexedFile indexedFileForMovedFile = new DefaultIndexedFile(baseDir.resolve(PROJECT_RELATIVE_PATH), "ABCDE", PROJECT_RELATIVE_PATH, MODULE_RELATIVE_PATH,
      InputFile.Type.TEST, "php", 0,
      sensorStrategy, OLD_RELATIVE_PATH);
    Metadata metadata = new Metadata(42, 42, "", new int[0], new int[0], 10);
    DefaultInputFile inputFile = new DefaultInputFile(indexedFileForMovedFile, f -> f.setMetadata(metadata), NO_OP)
      .setStatus(InputFile.Status.ADDED)
      .setCharset(StandardCharsets.ISO_8859_1);

    assertThat(inputFile.oldRelativePath()).isEqualTo(OLD_RELATIVE_PATH);
  }

  @Test
  public void test_content() throws IOException {
    Path testFile = baseDir.resolve(PROJECT_RELATIVE_PATH);
    Files.createDirectories(testFile.getParent());
    String content = "test é string";
    Files.writeString(testFile, content, StandardCharsets.ISO_8859_1);

    assertThat(Files.readAllLines(testFile, StandardCharsets.ISO_8859_1).get(0)).hasSize(content.length());

    Metadata metadata = new Metadata(42, 30, "", new int[0], new int[0], 10);

    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, f -> f.setMetadata(metadata), NO_OP)
      .setStatus(InputFile.Status.ADDED)
      .setCharset(StandardCharsets.ISO_8859_1);

    assertThat(inputFile.contents()).isEqualTo(content);
    try (InputStream inputStream = inputFile.inputStream()) {
      String result = new BufferedReader(new InputStreamReader(inputStream, inputFile.charset())).lines().collect(Collectors.joining());
      assertThat(result).isEqualTo(content);
    }

  }

  @Test
  public void test_content_exclude_bom() throws IOException {
    Path testFile = baseDir.resolve(PROJECT_RELATIVE_PATH);
    Files.createDirectories(testFile.getParent());
    try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(testFile.toFile()), StandardCharsets.UTF_8))) {
      out.write('\ufeff');
    }
    String content = "test é string €";
    Files.write(testFile, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);

    assertThat(Files.readAllLines(testFile, StandardCharsets.UTF_8).get(0)).hasSize(content.length() + 1);

    Metadata metadata = new Metadata(42, 30, "", new int[0], new int[0], 10);

    DefaultInputFile inputFile = new DefaultInputFile(indexedFile, f -> f.setMetadata(metadata), NO_OP)
      .setStatus(InputFile.Status.ADDED)
      .setCharset(StandardCharsets.UTF_8);

    assertThat(inputFile.contents()).isEqualTo(content);
    try (InputStream inputStream = inputFile.inputStream()) {
      String result = new BufferedReader(new InputStreamReader(inputStream, inputFile.charset())).lines().collect(Collectors.joining());
      assertThat(result).isEqualTo(content);
    }

  }

  @Test
  public void test_equals_and_hashcode() {
    DefaultInputFile f1 = new DefaultInputFile(new DefaultIndexedFile("ABCDE", Paths.get("module"), MODULE_RELATIVE_PATH, null), NO_OP, NO_OP);
    DefaultInputFile f1a = new DefaultInputFile(new DefaultIndexedFile("ABCDE", Paths.get("module"), MODULE_RELATIVE_PATH, null), NO_OP, NO_OP);
    DefaultInputFile f2 = new DefaultInputFile(new DefaultIndexedFile("ABCDE", Paths.get("module"), "src/Bar.php", null), NO_OP, NO_OP);

    assertThat(f1)
      .isEqualTo(f1)
      .isEqualTo(f1a)
      .isNotEqualTo(f2);
    assertThat(f1.equals("foo")).isFalse();
    assertThat(f1.equals(null)).isFalse();

    assertThat(f1)
      .hasSameHashCodeAs(f1)
      .hasSameHashCodeAs(f1a);
  }

  @Test
  public void test_toString() {
    DefaultInputFile file = new DefaultInputFile(new DefaultIndexedFile("ABCDE", Paths.get("module"), MODULE_RELATIVE_PATH, null), NO_OP, NO_OP);
    assertThat(file).hasToString(MODULE_RELATIVE_PATH);
  }

  @Test
  public void checkValidPointer() {
    Metadata metadata = new Metadata(2, 2, "", new int[] {0, 10}, new int[] {9, 15}, 16);
    DefaultInputFile file = new DefaultInputFile(new DefaultIndexedFile("ABCDE", Paths.get("module"), MODULE_RELATIVE_PATH, null),
      f -> f.setMetadata(metadata), NO_OP);
    assertThat(file.newPointer(1, 0).line()).isOne();
    assertThat(file.newPointer(1, 0).lineOffset()).isZero();
    // Don't fail
    file.newPointer(1, 9);
    file.newPointer(2, 0);
    file.newPointer(2, 5);

    try {
      file.newPointer(0, 1);
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("0 is not a valid line for a file");
    }
    try {
      file.newPointer(3, 1);
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("3 is not a valid line for pointer. File src/Foo.php has 2 line(s)");
    }
    try {
      file.newPointer(1, -1);
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("-1 is not a valid line offset for a file");
    }
    try {
      file.newPointer(1, 10);
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("10 is not a valid line offset for pointer. File src/Foo.php has 9 character(s) at line 1");
    }
  }

  @Test
  public void checkValidPointerUsingGlobalOffset() {
    Metadata metadata = new Metadata(2, 2, "", new int[] {0, 10}, new int[] {8, 15}, 16);
    DefaultInputFile file = new DefaultInputFile(new DefaultIndexedFile("ABCDE", Paths.get("module"), MODULE_RELATIVE_PATH, null),
      f -> f.setMetadata(metadata), f -> {
    });
    assertThat(file.newPointer(0).line()).isOne();
    assertThat(file.newPointer(0).lineOffset()).isZero();

    assertThat(file.newPointer(9).line()).isOne();
    // Ignore eol characters
    assertThat(file.newPointer(9).lineOffset()).isEqualTo(8);

    assertThat(file.newPointer(10).line()).isEqualTo(2);
    assertThat(file.newPointer(10).lineOffset()).isZero();

    assertThat(file.newPointer(15).line()).isEqualTo(2);
    assertThat(file.newPointer(15).lineOffset()).isEqualTo(5);

    assertThat(file.newPointer(16).line()).isEqualTo(2);
    // Ignore eol characters
    assertThat(file.newPointer(16).lineOffset()).isEqualTo(5);

    try {
      file.newPointer(-1);
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("-1 is not a valid offset for a file");
    }

    try {
      file.newPointer(17);
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("17 is not a valid offset for file src/Foo.php. Max offset is 16");
    }
  }

  @Test
  public void checkValidRange() {
    Metadata metadata = new FileMetadata(mock(AnalysisWarnings.class)).readMetadata(new StringReader("bla bla a\nabcde"));
    DefaultInputFile file = new DefaultInputFile(new DefaultIndexedFile("ABCDE", Paths.get("module"), MODULE_RELATIVE_PATH, null),
      f -> f.setMetadata(metadata), NO_OP);

    assertThat(file.newRange(file.newPointer(1, 0), file.newPointer(2, 1)).start().line()).isOne();
    // Don't fail
    file.newRange(file.newPointer(1, 0), file.newPointer(1, 1));
    file.newRange(file.newPointer(1, 0), file.newPointer(1, 9));
    file.newRange(file.newPointer(1, 0), file.newPointer(2, 0));
    assertThat(file.newRange(file.newPointer(1, 0), file.newPointer(2, 5))).isEqualTo(file.newRange(0, 15));

    try {
      file.newRange(file.newPointer(1, 0), file.newPointer(1, 0));
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("Start pointer [line=1, lineOffset=0] should be before end pointer [line=1, lineOffset=0]");
    }
    try {
      file.newRange(file.newPointer(1, 0), file.newPointer(1, 10));
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("10 is not a valid line offset for pointer. File src/Foo.php has 9 character(s) at line 1");
    }
  }

  @Test
  public void selectLine() {
    Metadata metadata = new FileMetadata(mock(AnalysisWarnings.class)).readMetadata(new StringReader("bla bla a\nabcde\n\nabc"));
    DefaultInputFile file = new DefaultInputFile(new DefaultIndexedFile("ABCDE", Paths.get("module"), MODULE_RELATIVE_PATH, null),
      f -> f.setMetadata(metadata), NO_OP);

    assertThat(file.selectLine(1).start().line()).isOne();
    assertThat(file.selectLine(1).start().lineOffset()).isZero();
    assertThat(file.selectLine(1).end().line()).isOne();
    assertThat(file.selectLine(1).end().lineOffset()).isEqualTo(9);

    // Don't fail when selecting empty line
    assertThat(file.selectLine(3).start().line()).isEqualTo(3);
    assertThat(file.selectLine(3).start().lineOffset()).isZero();
    assertThat(file.selectLine(3).end().line()).isEqualTo(3);
    assertThat(file.selectLine(3).end().lineOffset()).isZero();

    try {
      file.selectLine(5);
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("5 is not a valid line for pointer. File src/Foo.php has 4 line(s)");
    }
  }

  @Test
  public void checkValidRangeUsingGlobalOffset() {
    Metadata metadata = new Metadata(2, 2, "", new int[] {0, 10}, new int[] {9, 15}, 16);
    DefaultInputFile file = new DefaultInputFile(new DefaultIndexedFile("ABCDE", Paths.get("module"), MODULE_RELATIVE_PATH, null),
      f -> f.setMetadata(metadata), NO_OP);
    TextRange newRange = file.newRange(10, 13);
    assertThat(newRange.start().line()).isEqualTo(2);
    assertThat(newRange.start().lineOffset()).isZero();
    assertThat(newRange.end().line()).isEqualTo(2);
    assertThat(newRange.end().lineOffset()).isEqualTo(3);
  }

  @Test
  public void testRangeOverlap() {
    Metadata metadata = new Metadata(2, 2, "", new int[] {0, 10}, new int[] {9, 15}, 16);
    DefaultInputFile file = new DefaultInputFile(new DefaultIndexedFile("ABCDE", Paths.get("module"), MODULE_RELATIVE_PATH, null),
      f -> f.setMetadata(metadata), NO_OP);
    // Don't fail
    assertThat(file.newRange(file.newPointer(1, 0), file.newPointer(1, 1)).overlap(file.newRange(file.newPointer(1, 0), file.newPointer(1, 1)))).isTrue();
    assertThat(file.newRange(file.newPointer(1, 0), file.newPointer(1, 1)).overlap(file.newRange(file.newPointer(1, 0), file.newPointer(1, 2)))).isTrue();
    assertThat(file.newRange(file.newPointer(1, 0), file.newPointer(1, 1)).overlap(file.newRange(file.newPointer(1, 1), file.newPointer(1, 2)))).isFalse();
    assertThat(file.newRange(file.newPointer(1, 2), file.newPointer(1, 3)).overlap(file.newRange(file.newPointer(1, 0), file.newPointer(1, 2)))).isFalse();
  }

  private static final Consumer<DefaultInputFile> NO_OP = f -> {
  };
}
