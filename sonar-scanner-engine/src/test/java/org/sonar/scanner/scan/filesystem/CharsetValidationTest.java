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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.sonar.scanner.scan.filesystem.CharsetValidation.Validation;

import static org.assertj.core.api.Assertions.assertThat;

public class CharsetValidationTest {
  private CharsetValidation charsets;

  @Before
  public void setUp() {
    charsets = new CharsetValidation();
  }

  @Test
  public void testWithSourceCode() throws IOException, URISyntaxException {
    Path path = Paths.get(this.getClass().getClassLoader().getResource("mediumtest/xoo/sample/xources/hello/HelloJava.xoo").toURI());
    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
    String text = lines.stream().collect(StringBuffer::new, StringBuffer::append, StringBuffer::append).toString();

    byte[] utf8 = encode(text, StandardCharsets.UTF_8);
    byte[] utf16be = encode(text, StandardCharsets.UTF_16BE);
    byte[] utf16le = encode(text, StandardCharsets.UTF_16LE);

    assertThat(charsets.isUTF8(utf8, true).charset()).isEqualTo(StandardCharsets.UTF_8);
    assertThat(charsets.isUTF16(utf16be, true).charset()).isEqualTo(StandardCharsets.UTF_16BE);
    assertThat(charsets.isUTF16(utf16le, true).charset()).isEqualTo(StandardCharsets.UTF_16LE);

    assertThat(charsets.isValidUTF16(utf16be, false)).isTrue();
    assertThat(charsets.isValidUTF16(utf16le, true)).isTrue();
  }

  @Test
  public void detectUTF16NewLine() throws CharacterCodingException {
    // the first char will be encoded with a null on the second byte, but we should still detect it due to the new line
    String text = "\uA100" + "\uA212" + "\n";

    byte[] utf16be = encode(text, StandardCharsets.UTF_16BE);
    byte[] utf16le = encode(text, StandardCharsets.UTF_16LE);
    byte[] utf8 = encode(text, StandardCharsets.UTF_8);
    byte[] utf32 = encode(text, Charset.forName("UTF-32LE"));

    System.out.println(Arrays.toString(utf32));

    assertThat(charsets.isUTF16(utf16le, true).charset()).isEqualTo(StandardCharsets.UTF_16LE);
    assertThat(charsets.isUTF16(utf16be, true).charset()).isEqualTo(StandardCharsets.UTF_16BE);
    assertThat(charsets.isUTF16(utf8, true).valid()).isEqualTo(Validation.MAYBE);
    // this will have a double null, so it will be yes or no based on failOnNull
    assertThat(charsets.isUTF16(utf32, true).valid()).isEqualTo(Validation.NO);
    assertThat(charsets.isUTF16(utf32, false).valid()).isEqualTo(Validation.YES);
  }

  @Test
  public void detectUTF16Ascii() throws CharacterCodingException {
    String text = "some text to test";
    byte[] utf16be = encode(text, StandardCharsets.UTF_16BE);
    byte[] utf16le = encode(text, StandardCharsets.UTF_16LE);
    byte[] utf8 = encode(text, StandardCharsets.UTF_8);
    byte[] iso88591 = encode(text, StandardCharsets.ISO_8859_1);
    byte[] utf32 = encode(text, Charset.forName("UTF-32LE"));

    assertThat(charsets.isUTF16(utf16le, true).charset()).isEqualTo(StandardCharsets.UTF_16LE);
    assertThat(charsets.isUTF16(utf16be, true).charset()).isEqualTo(StandardCharsets.UTF_16BE);
    // not enough nulls -> we don't know
    assertThat(charsets.isUTF16(iso88591, true).valid()).isEqualTo(Validation.MAYBE);
    assertThat(charsets.isUTF16(utf8, true).valid()).isEqualTo(Validation.MAYBE);
    // fail based on double nulls
    assertThat(charsets.isUTF16(utf32, true).valid()).isEqualTo(Validation.NO);
  }

  @Test
  public void validUTF8() {
    // UTF8 with 3 bytes
    byte[] b = hexToByte("E2 80 A6");
    assertThat(charsets.isUTF8(b, true).valid()).isEqualTo(Validation.YES);
  }

  @Test
  public void invalidUTF16() {
    // UTF-16 will accept anything in direct 2 byte block unless it's between D800-DFFF (high and low surrogates).
    // In that case, it's a 4 byte encoding it's not a direct encoding.
    byte[] b1 = hexToByte("D800 0000");
    assertThat(charsets.isValidUTF16(b1)).isFalse();

    byte[] b1le = hexToByte("0000 D800");
    assertThat(charsets.isValidUTF16(b1le, true)).isFalse();

    // not enough bytes (any byte following this one would make it valid)
    byte[] b2 = {(byte) 0x01};
    assertThat(charsets.isValidUTF16(b2)).isFalse();

    // we reject double 0
    byte[] b3 = {(byte) 0, (byte) 0};
    assertThat(charsets.isValidUTF16(b3)).isFalse();
  }

  @Test
  public void invalidUTF8() {
    // never expects to see 0xFF or 0xC0..
    byte[] b1 = {(byte) 0xFF};
    assertThat(charsets.isUTF8(b1, true).valid()).isEqualTo(Validation.NO);

    byte[] b1c = {(byte) 0xC0};
    assertThat(charsets.isUTF8(b1c, true).valid()).isEqualTo(Validation.NO);

    // the first byte indicates a 2-byte encoding, but second byte is not valid
    byte[] b2 = {(byte) 0b11000010, (byte) 0b11000000};
    assertThat(charsets.isUTF8(b2, true).valid()).isEqualTo(Validation.NO);

    // we reject nulls (mainly to reject UTF-16)
    byte[] b3 = {(byte) 0};
    assertThat(charsets.isUTF8(b3, true).valid()).isEqualTo(Validation.NO);
  }

  @Test
  public void windows_1252() {
    assertThat(charsets.isValidWindows1252(new byte[]{(byte) 129}).valid()).isEqualTo(Validation.NO);
    assertThat(charsets.isValidWindows1252(new byte[]{(byte) 141}).valid()).isEqualTo(Validation.NO);
    assertThat(charsets.isValidWindows1252(new byte[]{(byte) 143}).valid()).isEqualTo(Validation.NO);
    assertThat(charsets.isValidWindows1252(new byte[]{(byte) 144}).valid()).isEqualTo(Validation.NO);
    assertThat(charsets.isValidWindows1252(new byte[]{(byte) 157}).valid()).isEqualTo(Validation.NO);
    assertThat(charsets.isValidWindows1252(new byte[]{(byte) 189}).valid()).isEqualTo(Validation.MAYBE);
    assertThat(charsets.isUTF8(new byte[]{(byte) 189}, true).valid()).isEqualTo(Validation.NO);
  }

  @Test
  public void dontFailIfNotEnoughBytes() {
    byte[] b1 = hexToByte("D800");
    assertThat(charsets.isValidUTF16(b1)).isTrue();

    // the first byte indicates a 2-byte encoding, but there is no second byte
    byte[] b2 = {(byte) 0b11000010};
    assertThat(charsets.isUTF8(b2, true).valid()).isEqualTo(Validation.MAYBE);
  }

  private byte[] encode(String txt, Charset charset) throws CharacterCodingException {
    CharsetEncoder encoder = charset.newEncoder()
      .onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT);
    ByteBuffer encoded = encoder.encode(CharBuffer.wrap(txt));
    byte[] b = new byte[encoded.remaining()];
    encoded.get(b);
    return b;
  }

  private static byte[] hexToByte(String str) {
    String s = StringUtils.deleteWhitespace(str);
    int len = s.length();
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
        + Character.digit(s.charAt(i + 1), 16));
    }
    return data;
  }

}
