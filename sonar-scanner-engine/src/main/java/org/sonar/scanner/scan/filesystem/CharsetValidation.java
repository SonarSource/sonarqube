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

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class CharsetValidation {

  private static final double UTF_16_NULL_PASS_THRESHOLD = 0.7;
  private static final double UTF_16_NULL_FAIL_THRESHOLD = 0.1;

  private static final boolean[] VALID_WINDOWS_1252 = new boolean[256];
  static {
    Arrays.fill(VALID_WINDOWS_1252, true);
    // See the Undefined cells in the charset table on https://en.wikipedia.org/wiki/Windows-1252
    VALID_WINDOWS_1252[129 - 128] = false;
    VALID_WINDOWS_1252[141 - 128] = false;
    VALID_WINDOWS_1252[143 - 128] = false;
    VALID_WINDOWS_1252[144 - 128] = false;
    VALID_WINDOWS_1252[157 - 128] = false;
  }

  /**
   * Checks if an array of bytes looks UTF-16 encoded.
   * We look for clues by checking the presence of nulls and new line control chars in both little and big endian byte orders.
   * Failing on nulls will greatly reduce FPs if the buffer is actually encoded in UTF-32.
   *
   * Note that for any unicode between 0-255, UTF-16 encodes it directly in 2 bytes, being the first 0 (null). Since ASCII, ANSI and control chars are
   * within this range, we look for number of nulls and see if it is above a certain threshold.
   * It's possible to have valid chars that map to the opposite (non-null followed by a null) even though it is very unlike.
   * That will happen, for example, for any unicode 0x??00, being ?? between 00 and D7. For this reason, we give a small maximum tolerance
   * for opposite nulls (10%).
   *
   * Line feed code point (0x000A) reversed would be (0x0A00). This code point is reserved and should never be found.
   *
   */
  public Result isUTF16(byte[] buffer, boolean failOnNull) {
    if (buffer.length < 2) {
      return Result.INVALID;
    }

    int beAscii = 0;
    int beLines = 0;
    int leAscii = 0;
    int leLines = 0;

    for (int i = 0; i < buffer.length / 2; i++) {
      // using bytes is fine, since we will compare with positive numbers only
      byte c1 = buffer[i * 2];
      byte c2 = buffer[i * 2 + 1];

      if (c1 == 0) {
        if (c2 != 0) {
          if (c2 == 0x0a || c2 == 0x0d) {
            beLines++;
          }
          beAscii++;
        } else if (failOnNull) {
          // it's probably UTF-32 or binary
          return Result.INVALID;
        }
      } else if (c2 == 0) {
        leAscii++;
        if (c1 == 0x0a || c1 == 0x0d) {
          leLines++;
        }
      }
    }

    double beAsciiPerc = beAscii * 2.0 / (double) buffer.length;
    double leAsciiPerc = leAscii * 2.0 / (double) buffer.length;

    if (leLines == 0) {
      // could be BE
      if (beAsciiPerc >= UTF_16_NULL_PASS_THRESHOLD && leAsciiPerc < UTF_16_NULL_FAIL_THRESHOLD) {
        return Result.newValid(StandardCharsets.UTF_16BE);
      }
      if (beLines > 0) {
        // this gives FPs for UTF-32 if !failOnNull
        return Result.newValid(StandardCharsets.UTF_16BE);
      }
    } else if (beLines > 0) {
      // lines detected with both endiness -> can't be utf-16
      return Result.INVALID;
    }
    if (beLines == 0) {
      // could be BE
      if (leAsciiPerc >= UTF_16_NULL_PASS_THRESHOLD && beAsciiPerc < UTF_16_NULL_FAIL_THRESHOLD) {
        return Result.newValid(StandardCharsets.UTF_16LE);
      }
      if (leLines > 0) {
        // this gives FPs for UTF-32 if !failOnNull
        return Result.newValid(StandardCharsets.UTF_16LE);
      }
    }

    // if we reach here, means that there wasn't a line feed for a single endiness and we didn't see a strong null pattern for any of the
    // endiness.
    // It could happen if there are no line feeds in the text and it's a language that does not use ANSI (unicode > 255).
    return new Result(Validation.MAYBE, null);
  }

  /**
   * Checks whether it's a valid UTF-16-encoded buffer.
   * Most sequences of bytes of any encoding will be valid UTF-16, so this is not very effective and gives
   * often false positives.
   *
   * Possible 16bit values in UTF-16:
   *
   * 0x0000-0xD7FF: single 16bit block
   * 0xD800-0xDBFF: first block
   * 0xDC00-0xDFFF: second block
   * 0XE000-0xFFFF: single 16 bit block
   *
   * The following UTF code points get mapped into 1 or 2 blocks:
   * 0x0000 -0xD7FF   (0    -55295)  : 2 bytes, direct mapping
   * 0xE000 -0xFFFF   (57344-65535)  : 2 bytes, direct mapping
   * 0x10000-0x10FFFF (65536-1114111): 2 blocks of 2 bytes (not direct..)
   *
   * Note that Unicode 55296-57345 (0xD800 to 0xDFFF) are not used, since it's reserved and used in UTF-16 for the high/low surrogates.
   *
   * We reject 2-byte blocks with 0 (we consider it's binary) even though it's a valid UTF-16 encoding.
   *
   */
  public boolean isValidUTF16(byte[] buffer) {
    return isValidUTF16(buffer, false);
  }

  public boolean isValidUTF16(byte[] buffer, boolean le) {
    if (buffer.length < 2) {
      return false;
    }
    for (int i = 0; i < buffer.length / 2; i++) {
      boolean extraByte = false;
      int c = read16bit(buffer, i, le);

      if (c >= 0xD800 && c < 0xDC00) {
        // it's a higher surrogate (10 bits)
        extraByte = true;
        i++;
      } else if ((c >= 0xDC00 && c < 0xE000) || c == 0) {
        return false;
      }
      // else it is a simple 2 byte encoding (code points in BMP), and it's valid

      if (extraByte && i < buffer.length / 2) {
        c = read16bit(buffer, i, le);
        if (c < 0xDC00 || c >= 0xE000) {
          // invalid lower surrogate (10 bits)
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Checks if a buffer contains only valid UTF8 encoded bytes.
   * It's very effective, giving a clear YES/NO, unless it's ASCII  (unicode < 127), in which case it returns MAYBE.
   *
   *
   * First byte:
   * 0xxxxxxx: only one byte (0-127)
   * 110xxxxx: 2 bytes       (194-223, as 192/193 are invalid)
   * 1110xxxx: 3 bytes       (224-239)
   * 11110xxx: 4 bytes       (240-244)
   *
   * Bytes 2,3 and 4 are always 10xxxxxx (0x80-0xBF or 128-191).
   *
   * So depending on the number of significant bits in the unicode code point, the length will be 1,2,3 or 4 bytes:
   * 0 -7 bits  (0x0000-007F):  1 byte encoding
   * 8 -11 bits (0x0080-07FF): 2 bytes encoding
   * 12-16 bits (0x0800-FFFF): 3 bytes encoding
   * 17-21 bits (0x10000-10FFFF): 4 bytes encoding
   */
  public Result isUTF8(byte[] buffer, boolean rejectNulls) {
    boolean onlyAscii = true;

    for (int i = 0; i < buffer.length; i++) {
      byte len;
      // make it unsigned for the comparisons
      int c = (0xFF) & buffer[i];

      if (rejectNulls && c == 0) {
        return Result.INVALID;
      }
      if ((c & 0b10000000) == 0) {
        len = 0;
      } else if (c >= 194 && c < 224) {
        len = 1;
      } else if ((c & 0b11110000) == 0b11100000) {
        len = 2;
      } else if ((c & 0b11111000) == 0b11110000) {
        len = 3;
      } else {
        return Result.INVALID;
      }

      while (len > 0) {
        i++;
        if (i >= buffer.length) {
          break;
        }
        c = (0xFF) & buffer[i];
        onlyAscii = false;

        // first 2 bits should be 10
        if ((c & 0b11000000) != 0b10000000) {
          return Result.INVALID;
        }
        len--;
      }
    }

    return onlyAscii ? new Result(Validation.MAYBE, StandardCharsets.UTF_8) : Result.newValid(StandardCharsets.UTF_8);
  }

  /**
   * Tries to use the given charset to decode the byte array.
   * @return true if decoding succeeded, false if there was a decoding error.
   */
  public boolean tryDecode(byte[] bytes, @Nullable Charset charset) {
    if (charset == null) {
      return false;
    }
    CharsetDecoder decoder = charset.newDecoder()
      .onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT);

    try {
      decoder.decode(ByteBuffer.wrap(bytes));
    } catch (CharacterCodingException e) {
      return false;
    }
    return true;
  }

  private static int read16bit(byte[] buffer, int i, boolean le) {
    return le ? (buffer[i / 2] & 0xff) | ((buffer[i / 2 + 1] & 0xff) << 8)
      : ((buffer[i / 2] & 0xff) << 8) | (buffer[i / 2 + 1] & 0xff);
  }

  /**
   * Verify that the buffer doesn't contain bytes that are not supposed to be used by Windows-1252.
   *
   * @return Result object with Validation.MAYBE and Windows-1252 if no unknown characters are used,
   * otherwise Result.INVALID
   * @param buf byte buffer to validate
   */
  public Result isValidWindows1252(byte[] buf) {
    for (byte b : buf) {
      if (!VALID_WINDOWS_1252[b + 128]) {
        return Result.INVALID;
      }
    }

    try {
      return new Result(Validation.MAYBE, Charset.forName("Windows-1252"));
    } catch (UnsupportedCharsetException e) {
      return Result.INVALID;
    }
  }

  public enum Validation {
    NO,
    YES,
    MAYBE
  }

  public static class Result {
    static final Result INVALID = new Result(Validation.NO, null);
    private Validation valid;
    private Charset charset;

    public Result(Validation valid, @Nullable Charset charset) {
      this.valid = valid;
      this.charset = charset;
    }

    public static Result newValid(Charset charset) {
      return new Result(Validation.YES, charset);
    }

    public Validation valid() {
      return valid;
    }

    /**
     * Only non-null if Valid.Yes
     */
    @CheckForNull
    public Charset charset() {
      return charset;
    }
  }
}
