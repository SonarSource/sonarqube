/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.ByteOrderMark;
import org.junit.Before;
import org.junit.Test;
import org.sonar.scanner.scan.filesystem.CharsetValidation.Result;
import org.sonar.scanner.scan.filesystem.CharsetValidation.Validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ByteCharsetDetectorTest {
  private CharsetValidation validation;
  private ByteCharsetDetector charsets;

  @Before
  public void setUp() {
    validation = mock(CharsetValidation.class);
    charsets = new ByteCharsetDetector(validation, null);
  }

  @Test
  public void detectBOM() throws URISyntaxException, IOException {
    byte[] b = ByteOrderMark.UTF_16BE.getBytes();
    assertThat(charsets.detectBOM(b)).isEqualTo(ByteOrderMark.UTF_16BE);

    assertThat(charsets.detectBOM(readFile("UTF-8"))).isEqualTo(ByteOrderMark.UTF_8);
    assertThat(charsets.detectBOM(readFile("UTF-16BE"))).isEqualTo(ByteOrderMark.UTF_16BE);
    assertThat(charsets.detectBOM(readFile("UTF-16LE"))).isEqualTo(ByteOrderMark.UTF_16LE);
    assertThat(charsets.detectBOM(readFile("UTF-32BE"))).isEqualTo(ByteOrderMark.UTF_32BE);
    assertThat(charsets.detectBOM(readFile("UTF-32LE"))).isEqualTo(ByteOrderMark.UTF_32LE);
  }

  private byte[] readFile(String fileName) throws URISyntaxException, IOException {
    Path path = Paths.get(this.getClass().getClassLoader().getResource("org/sonar/scanner/scan/filesystem/" + fileName + ".txt").toURI());
    return Files.readAllBytes(path);
  }

  @Test
  public void tryUTF8First() {
    when(validation.isUTF8(any(byte[].class), anyBoolean())).thenReturn(Result.newValid(StandardCharsets.UTF_8));
    assertThat(charsets.detect(new byte[1])).isEqualTo(StandardCharsets.UTF_8);
  }

  @Test
  public void tryUTF16heuristics() {
    when(validation.isUTF8(any(byte[].class), anyBoolean())).thenReturn(Result.INVALID);
    when(validation.isUTF16(any(byte[].class), anyBoolean())).thenReturn(Result.newValid(StandardCharsets.UTF_16));
    when(validation.isValidUTF16(any(byte[].class), anyBoolean())).thenReturn(true);

    assertThat(charsets.detect(new byte[1])).isEqualTo(StandardCharsets.UTF_16);
  }

  @Test
  public void failAll() {
    when(validation.isUTF8(any(byte[].class), anyBoolean())).thenReturn(Result.INVALID);
    when(validation.isUTF16(any(byte[].class), anyBoolean())).thenReturn(new Result(Validation.MAYBE, null));
    when(validation.isValidWindows1252(any(byte[].class))).thenReturn(Result.INVALID);

    assertThat(charsets.detect(new byte[1])).isEqualTo(null);
  }

  @Test
  public void failAnsii() {
    when(validation.isUTF8(any(byte[].class), anyBoolean())).thenReturn(new Result(Validation.MAYBE, null));
    when(validation.isUTF16(any(byte[].class), anyBoolean())).thenReturn(Result.newValid(StandardCharsets.UTF_16));
    when(validation.isValidUTF16(any(byte[].class), anyBoolean())).thenReturn(true);

    assertThat(charsets.detect(new byte[1])).isEqualTo(null);
  }

  @Test
  public void tryUserAnsii() {
    when(validation.isUTF8(any(byte[].class), anyBoolean())).thenReturn(new Result(Validation.MAYBE, null));
    when(validation.isUTF16(any(byte[].class), anyBoolean())).thenReturn(Result.newValid(StandardCharsets.UTF_16));
    when(validation.isValidUTF16(any(byte[].class), anyBoolean())).thenReturn(true);
    when(validation.tryDecode(any(byte[].class), eq(StandardCharsets.ISO_8859_1))).thenReturn(true);

    charsets = new ByteCharsetDetector(validation, StandardCharsets.ISO_8859_1);
    assertThat(charsets.detect(new byte[1])).isEqualTo(StandardCharsets.ISO_8859_1);
  }

  @Test
  public void tryOtherUserCharset() {
    when(validation.isUTF8(any(byte[].class), anyBoolean())).thenReturn(Result.INVALID);
    when(validation.isUTF16(any(byte[].class), anyBoolean())).thenReturn(new Result(Validation.MAYBE, null));
    when(validation.tryDecode(any(byte[].class), eq(StandardCharsets.ISO_8859_1))).thenReturn(true);

    charsets = new ByteCharsetDetector(validation, StandardCharsets.ISO_8859_1);
    assertThat(charsets.detect(new byte[1])).isEqualTo(StandardCharsets.ISO_8859_1);
  }

  @Test
  public void invalidBOM() {
    byte[] b1 = {(byte) 0xFF, (byte) 0xFF};
    assertThat(charsets.detectBOM(b1)).isNull();

    // not enough bytes
    byte[] b2 = {(byte) 0xFE};
    assertThat(charsets.detectBOM(b2)).isNull();

    // empty
    byte[] b3 = new byte[0];
    assertThat(charsets.detectBOM(b3)).isNull();
  }

  @Test
  public void windows1252() throws IOException, URISyntaxException {
    ByteCharsetDetector detector = new ByteCharsetDetector(new CharsetValidation(), StandardCharsets.UTF_8);
    assertThat(detector.detect(readFile("windows-1252"))).isEqualTo(Charset.forName("Windows-1252"));
  }
}
