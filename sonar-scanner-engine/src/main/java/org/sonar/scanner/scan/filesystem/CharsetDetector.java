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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;

public class CharsetDetector {
  private static final int BYTES_TO_DECODE = 4192;
  private Path filePath;
  private BufferedInputStream stream;
  private Charset detectedCharset;
  private Charset userEncoding;

  public CharsetDetector(Path filePath, Charset userEncoding) {
    this.filePath = filePath;
    this.userEncoding = userEncoding;
  }

  public boolean run() {
    try {
      byte[] buf = readBuffer();
      return detectCharset(buf);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read file " + filePath.toAbsolutePath().toString(), e);
    }
  }

  @CheckForNull
  public Charset charset() {
    assertRun();
    return detectedCharset;
  }

  public InputStream inputStream() {
    assertRun();
    return stream;
  }

  private byte[] readBuffer() throws IOException {
    stream = new BufferedInputStream(Files.newInputStream(filePath), BYTES_TO_DECODE * 2);
    stream.mark(BYTES_TO_DECODE);
    byte[] buf = new byte[BYTES_TO_DECODE];
    int read = IOUtils.read(stream, buf, 0, BYTES_TO_DECODE);
    stream.reset();
    stream.mark(-1);
    return Arrays.copyOf(buf, read);
  }

  private boolean detectCharset(byte[] buf) throws IOException {
    ByteCharsetDetector detector = new ByteCharsetDetector(new CharsetValidation(), userEncoding);
    ByteOrderMark bom = detector.detectBOM(buf);
    if (bom != null) {
      detectedCharset = Charset.forName(bom.getCharsetName());
      stream.skip(bom.length());
      return true;
    }

    detectedCharset = detector.detect(buf);
    return detectedCharset != null;
  }

  private void assertRun() {
    if (stream == null) {
      throw new IllegalStateException("Charset detection did not run");
    }
  }
}
