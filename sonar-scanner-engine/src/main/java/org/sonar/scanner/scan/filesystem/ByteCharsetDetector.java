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
package org.sonar.scanner.scan.filesystem;

import java.nio.charset.Charset;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import org.apache.commons.io.ByteOrderMark;
import org.sonar.scanner.scan.filesystem.CharsetValidation.Result;
import org.sonar.scanner.scan.filesystem.CharsetValidation.Validation;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_16BE;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ByteCharsetDetector {
  // these needs to be sorted by longer first!
  private static final ByteOrderMark[] boms = {ByteOrderMark.UTF_8, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE,
    ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE};

  private final Charset userConfiguration;
  private final CharsetValidation validator;

  public ByteCharsetDetector(CharsetValidation validator, Charset userConfiguration) {
    this.validator = validator;
    this.userConfiguration = userConfiguration;
  }

  @CheckForNull
  public Charset detect(byte[] buf) {
    // Try UTF-8 first since we are very confident in it if it's a yes.
    // Fail if we see nulls to not have FPs if the text is ASCII encoded in UTF-16.
    Result utf8Result = validator.isUTF8(buf, true);
    if (utf8Result.valid() == Validation.YES) {
      return utf8Result.charset();
    } else if (utf8Result.valid() == Validation.MAYBE) {
      return detectAscii(buf);
    }

    // try UTF16 with both endiness. Fail if we see nulls to not have FPs if it's UTF-32.
    Result utf16 = validator.isUTF16(buf, true);
    if (utf16.valid() == Validation.YES && validator.isValidUTF16(buf, UTF_16LE.equals(utf16.charset()))) {
      return utf16.charset();
    }

    // at this point we know it can't be UTF-8
    Charset c = userConfiguration;
    if (!UTF_8.equals(c) && (!isUtf16(c) || utf16.valid() == Validation.MAYBE) && validator.tryDecode(buf, c)) {
      return c;
    }

    Result windows1252 = validator.isValidWindows1252(buf);
    if (windows1252.valid() == Validation.MAYBE) {
      return windows1252.charset();
    }

    return null;
  }

  private Charset detectAscii(byte[] buf) {
    if (!isUtf16Or32(userConfiguration) && validator.tryDecode(buf, userConfiguration)) {
      return userConfiguration;
    }

    return null;
  }

  private static boolean isUtf16(Charset charset) {
    return UTF_16.equals(charset) || UTF_16BE.equals(charset) || UTF_16LE.equals(charset);
  }

  private static boolean isUtf16Or32(Charset charset) {
    return isUtf16(charset) || MetadataGenerator.UTF_32BE.equals(charset) || MetadataGenerator.UTF_32LE.equals(charset);
  }

  @CheckForNull
  public ByteOrderMark detectBOM(byte[] buffer) {
    return Arrays.stream(boms)
      .filter(b -> isBom(b, buffer))
      .findAny()
      .orElse(null);
  }

  private static boolean isBom(ByteOrderMark bom, byte[] buffer) {
    if (buffer.length < bom.length()) {
      return false;
    }
    for (int i = 0; i < bom.length(); i++) {
      if ((byte) bom.get(i) != buffer[i]) {
        return false;
      }
    }
    return true;
  }

}
