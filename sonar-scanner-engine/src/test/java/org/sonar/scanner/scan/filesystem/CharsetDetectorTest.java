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
package org.sonar.scanner.scan.filesystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class CharsetDetectorTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void should_detect_charset_from_BOM() {
    Path basedir = Paths.get("src/test/resources/org/sonar/scanner/scan/filesystem/");

    assertThat(detectCharset(basedir.resolve("without_BOM.txt"), StandardCharsets.US_ASCII)).isEqualTo(StandardCharsets.US_ASCII);
    assertThat(detectCharset(basedir.resolve("UTF-8.txt"), StandardCharsets.US_ASCII)).isEqualTo(StandardCharsets.UTF_8);
    assertThat(detectCharset(basedir.resolve("UTF-16BE.txt"), StandardCharsets.US_ASCII)).isEqualTo(StandardCharsets.UTF_16BE);
    assertThat(detectCharset(basedir.resolve("UTF-16LE.txt"), StandardCharsets.US_ASCII)).isEqualTo(StandardCharsets.UTF_16LE);
    assertThat(detectCharset(basedir.resolve("UTF-32BE.txt"), StandardCharsets.US_ASCII)).isEqualTo(MetadataGenerator.UTF_32BE);
    assertThat(detectCharset(basedir.resolve("UTF-32LE.txt"), StandardCharsets.US_ASCII)).isEqualTo(MetadataGenerator.UTF_32LE);
  }

  @Test
  public void always_try_utf8() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8")) {
      // UTF-16 can't read 1 byte only
      writer.write("t");
    }

    Path filePath = temp.newFile().toPath();
    Files.write(filePath, out.toByteArray());
    assertThat(detectCharset(filePath, StandardCharsets.UTF_16)).isEqualByComparingTo(StandardCharsets.UTF_8);

  }

  @Test
  public void fail_if_file_doesnt_exist() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("Unable to read file " + Paths.get("non_existing").toAbsolutePath());
    detectCharset(Paths.get("non_existing"), StandardCharsets.UTF_8);
  }

  @Test
  public void no_encoding_found() throws IOException {
    Path filePath = temp.newFile().toPath();
    byte[] b = new byte[512];
    new Random().nextBytes(b);
    Files.write(filePath, b);

    CharsetDetector detector = new CharsetDetector(filePath, StandardCharsets.UTF_8);
    assertThat(detector.run()).isFalse();
    assertThat(detector.charset()).isEqualTo(StandardCharsets.UTF_8);
  }

  private Charset detectCharset(Path file, Charset defaultEncoding) {
    CharsetDetector detector = new CharsetDetector(file, defaultEncoding);
    assertThat(detector.run()).isTrue();
    return detector.charset();
  }
}
