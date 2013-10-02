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
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.utils.SonarException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * @since 4.0
 */
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public final class HashBuilder implements BatchExtension {

  /**
   * Compute hash of a file ignoring line ends differences.
   * Maximum performance is needed.
   */
  public String computeHashNormalizeLineEnds(File file, Charset charset) {
    Reader reader = null;
    try {
      MessageDigest md5Digest = DigestUtils.getMd5Digest();
      md5Digest.reset();
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
      int i = reader.read();
      boolean afterCR = true;
      while (i != -1) {
        char c = (char) i;
        if (afterCR) {
          afterCR = false;
          if (c == '\n') {
            // Ignore
            i = reader.read();
            continue;
          }
        }
        if (c == '\r') {
          afterCR = true;
          c = '\n';
        }
        md5Digest.update(charToBytesUTF(c));
        i = reader.read();
      }
      return Hex.encodeHexString(md5Digest.digest());
    } catch (IOException e) {
      throw new SonarException("Unable to compute file hash", e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  public static byte[] charToBytesUTF(char c) {
    char[] buffer = new char[] {c};
    byte[] b = new byte[buffer.length << 1];
    for (int i = 0; i < buffer.length; i++) {
      int bpos = i << 1;
      b[bpos] = (byte) ((buffer[i] & 0xFF00) >> 8);
      b[bpos + 1] = (byte) (buffer[i] & 0x00FF);
    }
    return b;
  }
}
