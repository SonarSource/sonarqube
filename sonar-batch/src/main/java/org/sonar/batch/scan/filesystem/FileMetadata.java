/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.scan.filesystem;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * Computes hash of files. Ends of Lines are ignored, so files with
 * same content but different EOL encoding have the same hash.
 */
class FileMetadata {

  private static final char LINE_FEED = '\n';
  private static final char CARRIAGE_RETURN = '\r';

  // This singleton aims only to increase the coverage by allowing
  // to test the private method !
  static final FileMetadata INSTANCE = new FileMetadata();

  private FileMetadata() {
  }

  /**
   * Compute hash of a file ignoring line ends differences.
   * Maximum performance is needed.
   */
  Metadata read(File file, Charset encoding) {
    Reader reader = null;
    int lines = 0;
    char c = (char)-1;
    try {
      MessageDigest md5Digest = DigestUtils.getMd5Digest();
      md5Digest.reset();
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
      int i = reader.read();
      boolean afterCR = true;
      while (i != -1) {
        c = (char) i;
        if (afterCR) {
          afterCR = false;
          if (c == LINE_FEED) {
            // Ignore
            i = reader.read();
            lines++;
            continue;
          }
        }
        if (c == CARRIAGE_RETURN) {
          afterCR = true;
          c = LINE_FEED;
        } else if (c == LINE_FEED) {
          lines++;
        }
        md5Digest.update(charToBytesUTF(c));
        i = reader.read();
      }
      if (c != LINE_FEED) {
        lines++;
      }
      String hash = Hex.encodeHexString(md5Digest.digest());
      return new Metadata(lines, hash);

    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to read file '%s' with encoding '%s'", file.getAbsolutePath(), encoding), e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  private byte[] charToBytesUTF(char c) {
    char[] buffer = new char[]{c};
    byte[] b = new byte[buffer.length << 1];
    for (int i = 0; i < buffer.length; i++) {
      int bpos = i << 1;
      b[bpos] = (byte) ((buffer[i] & 0xFF00) >> 8);
      b[bpos + 1] = (byte) (buffer[i] & 0x00FF);
    }
    return b;
  }

  static class Metadata {
    int lines;
    String hash;

    private Metadata(int lines, String hash) {
      this.lines = lines;
      this.hash = hash;
    }
  }
}
