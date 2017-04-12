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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

public class CharsetDetector {
  private static final int BYTES_TO_DECODE = 512;
  private Path filePath;
  private BOMInputStream stream;
  private Charset detectedCharset;
  private Charset defaultEncoding;

  public CharsetDetector(Path filePath, Charset defaultEncoding) {
    this.filePath = filePath;
    this.defaultEncoding = defaultEncoding;
  }

  public boolean run() {
    try {
      stream = createInputStream(filePath);
      if (detectBOM()) {
        return true;
      }

      if (detectCharset()) {
        return true;
      }

      detectedCharset = defaultEncoding;
      return false;
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file " + filePath.toAbsolutePath().toString(), e);
    }
  }

  public Charset charset() {
    assertRun();
    return detectedCharset;
  }

  public InputStream inputStream() {
    assertRun();
    return stream;
  }

  private static BOMInputStream createInputStream(Path path) throws IOException {
    BufferedInputStream bufferedStream = new BufferedInputStream(Files.newInputStream(path));
    return new BOMInputStream(bufferedStream, ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE,
      ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);
  }

  private boolean detectBOM() throws IOException {
    String charsetName = stream.getBOMCharsetName();
    if (charsetName != null) {
      detectedCharset = Charset.forName(charsetName);
      return true;
    }
    return false;
  }

  private boolean detectCharset() throws IOException {
    stream.mark(BYTES_TO_DECODE);
    byte[] buf = new byte[BYTES_TO_DECODE];
    int len = IOUtils.read(stream, buf, 0, BYTES_TO_DECODE);
    stream.reset();

    Set<Charset> charsets = new LinkedHashSet<>();
    charsets.add(defaultEncoding);
    charsets.add(StandardCharsets.UTF_8);
    charsets.add(Charset.defaultCharset());

    for (Charset c : charsets) {
      if (tryDecode(buf, len, c)) {
        detectedCharset = c;
        return true;
      }
    }
    return false;
  }

  private static boolean tryDecode(byte[] bytes, int len, Charset charset) throws IOException {
    CharsetDecoder decoder = charset.newDecoder()
      .onMalformedInput(CodingErrorAction.REPORT)
      .onUnmappableCharacter(CodingErrorAction.REPORT);

    try {
      decoder.decode(ByteBuffer.wrap(bytes, 0, len));
    } catch (CharacterCodingException e) {
      return false;
    }
    return true;
  }

  private void assertRun() {
    if (stream == null) {
      throw new IllegalStateException("Charset detection did not run");
    }
  }
}
