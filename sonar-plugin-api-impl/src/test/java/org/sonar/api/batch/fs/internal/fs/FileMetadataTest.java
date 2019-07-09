/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.batch.fs.internal.Metadata;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class FileMetadataTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void empty_file() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.touch(tempFile);

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_8, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(1);
    assertThat(metadata.nonBlankLines()).isEqualTo(0);
    assertThat(metadata.hash()).isNotEmpty();
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0);
    assertThat(metadata.originalLineEndOffsets()).containsOnly(0);
    assertThat(metadata.isEmpty()).isTrue();
  }

  @Test
  public void windows_without_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\r\nbar\r\nbaz", StandardCharsets.UTF_8, true);

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_8, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(3);
    assertThat(metadata.nonBlankLines()).isEqualTo(3);
    assertThat(metadata.hash()).isEqualTo(md5Hex("foo\nbar\nbaz"));
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0, 5, 10);
    assertThat(metadata.originalLineEndOffsets()).containsOnly(3, 8, 13);
    assertThat(metadata.isEmpty()).isFalse();
  }

  @Test
  public void read_with_wrong_encoding() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "marker´s\n", Charset.forName("cp1252"));

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_8, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(2);
    assertThat(metadata.hash()).isEqualTo(md5Hex("marker\ufffds\n"));
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0, 9);
  }

  @Test
  public void non_ascii_utf_8() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "föo\r\nbàr\r\n\u1D11Ebaßz\r\n", StandardCharsets.UTF_8, true);

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_8, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(4);
    assertThat(metadata.nonBlankLines()).isEqualTo(3);
    assertThat(metadata.hash()).isEqualTo(md5Hex("föo\nbàr\n\u1D11Ebaßz\n"));
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0, 5, 10, 18);
  }

  @Test
  public void non_ascii_utf_16() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "föo\r\nbàr\r\n\u1D11Ebaßz\r\n", StandardCharsets.UTF_16, true);
    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_16, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(4);
    assertThat(metadata.nonBlankLines()).isEqualTo(3);
    assertThat(metadata.hash()).isEqualTo(md5Hex("föo\nbàr\n\u1D11Ebaßz\n".getBytes(StandardCharsets.UTF_8)));
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0, 5, 10, 18);
  }

  @Test
  public void unix_without_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\nbar\nbaz", StandardCharsets.UTF_8, true);

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_8, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(3);
    assertThat(metadata.nonBlankLines()).isEqualTo(3);
    assertThat(metadata.hash()).isEqualTo(md5Hex("foo\nbar\nbaz"));
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0, 4, 8);
    assertThat(metadata.originalLineEndOffsets()).containsOnly(3, 7, 11);
    assertThat(metadata.isEmpty()).isFalse();
  }

  @Test
  public void unix_with_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\nbar\nbaz\n", StandardCharsets.UTF_8, true);

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_8, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(4);
    assertThat(metadata.nonBlankLines()).isEqualTo(3);
    assertThat(metadata.hash()).isEqualTo(md5Hex("foo\nbar\nbaz\n"));
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0, 4, 8, 12);
    assertThat(metadata.originalLineEndOffsets()).containsOnly(3, 7, 11, 12);
  }

  @Test
  public void mac_without_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\rbar\rbaz", StandardCharsets.UTF_8, true);

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_8, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(3);
    assertThat(metadata.nonBlankLines()).isEqualTo(3);
    assertThat(metadata.hash()).isEqualTo(md5Hex("foo\nbar\nbaz"));
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0, 4, 8);
    assertThat(metadata.originalLineEndOffsets()).containsOnly(3, 7, 11);
  }

  @Test
  public void mac_with_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\rbar\rbaz\r", StandardCharsets.UTF_8, true);

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_8, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(4);
    assertThat(metadata.nonBlankLines()).isEqualTo(3);
    assertThat(metadata.hash()).isEqualTo(md5Hex("foo\nbar\nbaz\n"));
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0, 4, 8, 12);
    assertThat(metadata.originalLineEndOffsets()).containsOnly(3, 7, 11, 12);
  }

  @Test
  public void mix_of_newlines_with_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\nbar\r\nbaz\n", StandardCharsets.UTF_8, true);

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_8, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(4);
    assertThat(metadata.nonBlankLines()).isEqualTo(3);
    assertThat(metadata.hash()).isEqualTo(md5Hex("foo\nbar\nbaz\n"));
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0, 4, 9, 13);
    assertThat(metadata.originalLineEndOffsets()).containsOnly(3, 7, 12, 13);
  }

  @Test
  public void several_new_lines() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\n\n\nbar", StandardCharsets.UTF_8, true);

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_8, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(4);
    assertThat(metadata.nonBlankLines()).isEqualTo(2);
    assertThat(metadata.hash()).isEqualTo(md5Hex("foo\n\n\nbar"));
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0, 4, 5, 6);
    assertThat(metadata.originalLineEndOffsets()).containsOnly(3, 4, 5, 9);
  }

  @Test
  public void mix_of_newlines_without_latest_eol() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "foo\nbar\r\nbaz", StandardCharsets.UTF_8, true);

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_8, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(3);
    assertThat(metadata.nonBlankLines()).isEqualTo(3);
    assertThat(metadata.hash()).isEqualTo(md5Hex("foo\nbar\nbaz"));
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0, 4, 9);
    assertThat(metadata.originalLineEndOffsets()).containsOnly(3, 7, 12);
  }

  @Test
  public void start_with_newline() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "\nfoo\nbar\r\nbaz", StandardCharsets.UTF_8, true);

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(tempFile), StandardCharsets.UTF_8, tempFile.getName());
    assertThat(metadata.lines()).isEqualTo(4);
    assertThat(metadata.nonBlankLines()).isEqualTo(3);
    assertThat(metadata.hash()).isEqualTo(md5Hex("\nfoo\nbar\nbaz"));
    assertThat(metadata.originalLineStartOffsets()).containsOnly(0, 1, 5, 10);
    assertThat(metadata.originalLineEndOffsets()).containsOnly(0, 4, 8, 13);
  }

  @Test
  public void ignore_whitespace_when_computing_line_hashes() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, " foo\nb ar\r\nbaz \t", StandardCharsets.UTF_8, true);

    DefaultInputFile f = new TestInputFileBuilder("foo", tempFile.getName())
      .setModuleBaseDir(tempFile.getParentFile().toPath())
      .setCharset(StandardCharsets.UTF_8)
      .build();
    FileMetadata.computeLineHashesForIssueTracking(f, new FileMetadata.LineHashConsumer() {

      @Override
      public void consume(int lineIdx, @Nullable byte[] hash) {
        switch (lineIdx) {
          case 1:
            assertThat(Hex.encodeHexString(hash)).isEqualTo(md5Hex("foo"));
            break;
          case 2:
            assertThat(Hex.encodeHexString(hash)).isEqualTo(md5Hex("bar"));
            break;
          case 3:
            assertThat(Hex.encodeHexString(hash)).isEqualTo(md5Hex("baz"));
            break;
          default:
            fail("Invalid line");
        }
      }
    });
  }

  @Test
  public void dont_fail_on_empty_file() throws Exception {
    File tempFile = temp.newFile();
    FileUtils.write(tempFile, "", StandardCharsets.UTF_8, true);

    DefaultInputFile f = new TestInputFileBuilder("foo", tempFile.getName())
      .setModuleBaseDir(tempFile.getParentFile().toPath())
      .setCharset(StandardCharsets.UTF_8)
      .build();
    FileMetadata.computeLineHashesForIssueTracking(f, new FileMetadata.LineHashConsumer() {

      @Override
      public void consume(int lineIdx, @Nullable byte[] hash) {
        switch (lineIdx) {
          case 1:
            assertThat(hash).isNull();
            break;
          default:
            fail("Invalid line");
        }
      }
    });
  }

  @Test
  public void line_feed_is_included_into_hash() throws Exception {
    File file1 = temp.newFile();
    FileUtils.write(file1, "foo\nbar\n", StandardCharsets.UTF_8, true);

    // same as file1, except an additional return carriage
    File file1a = temp.newFile();
    FileUtils.write(file1a, "foo\r\nbar\n", StandardCharsets.UTF_8, true);

    File file2 = temp.newFile();
    FileUtils.write(file2, "foo\nbar", StandardCharsets.UTF_8, true);

    String hash1 = new FileMetadata().readMetadata(new FileInputStream(file1), StandardCharsets.UTF_8, file1.getName()).hash();
    String hash1a = new FileMetadata().readMetadata(new FileInputStream(file1a), StandardCharsets.UTF_8, file1a.getName()).hash();
    String hash2 = new FileMetadata().readMetadata(new FileInputStream(file2), StandardCharsets.UTF_8, file2.getName()).hash();

    assertThat(hash1).isEqualTo(hash1a);
    assertThat(hash1).isNotEqualTo(hash2);
  }

  @Test
  public void binary_file_with_unmappable_character() throws Exception {
    File woff = new File(this.getClass().getResource("glyphicons-halflings-regular.woff").toURI());

    Metadata metadata = new FileMetadata().readMetadata(new FileInputStream(woff), StandardCharsets.UTF_8, woff.getAbsolutePath());

    assertThat(metadata.lines()).isEqualTo(135);
    assertThat(metadata.nonBlankLines()).isEqualTo(133);
    assertThat(metadata.hash()).isNotEmpty();

    assertThat(logTester.logs(LoggerLevel.WARN).get(0)).contains("Invalid character encountered in file");
    assertThat(logTester.logs(LoggerLevel.WARN).get(0)).contains(
      "glyphicons-halflings-regular.woff at line 1 for encoding UTF-8. Please fix file content or configure the encoding to be used using property 'sonar.sourceEncoding'.");
  }

}
