/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import com.google.common.base.Charsets;
import com.google.common.primitives.Longs;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes hash of files. Ends of Lines are ignored, so files with
 * same content but different EOL encoding have the same hash.
 */
class FileMetadata {

  private static final char LINE_FEED = '\n';
  private static final char CARRIAGE_RETURN = '\r';
  private static final char BOM = '\uFEFF';

  /**
   * Compute hash of a file ignoring line ends differences.
   * Maximum performance is needed.
   */
  Metadata read(File file, Charset encoding) {
    long currentOriginalOffset = 0;
    List<Long> originalLineOffsets = new ArrayList<Long>();
    List<Object> lineHashes = new ArrayList<Object>();
    int lines = 1;
    char c = (char) 0;
    try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding))) {
      MessageDigest globalMd5Digest = DigestUtils.getMd5Digest();
      MessageDigest lineMd5Digest = DigestUtils.getMd5Digest();
      int i = reader.read();
      boolean afterCR = false;
      // First offset of first line is always 0
      originalLineOffsets.add(0L);
      boolean blankline = true;
      while (i != -1) {
        c = (char) i;
        if (c == BOM) {
          // Ignore
          i = reader.read();
          continue;
        }
        currentOriginalOffset++;
        if (afterCR) {
          afterCR = false;
          if (c == LINE_FEED) {
            originalLineOffsets.set(originalLineOffsets.size() - 1, originalLineOffsets.get(originalLineOffsets.size() - 1) + 1);
            // Ignore
            i = reader.read();
            continue;
          }
        }
        if (c == CARRIAGE_RETURN) {
          afterCR = true;
          c = LINE_FEED;
        }
        if (c == LINE_FEED) {
          lines++;
          originalLineOffsets.add(currentOriginalOffset);
          lineHashes.add(blankline ? null : lineMd5Digest.digest());
          blankline = true;
        } else {
          if (!Character.isWhitespace(c)) {
            blankline = false;
            updateDigestUTF8Char(c, lineMd5Digest);
          }
        }
        updateDigestUTF8Char(c, globalMd5Digest);
        i = reader.read();
      }
      if (c != (char) -1) {
        // Last line
        lineHashes.add(blankline ? null : lineMd5Digest.digest());
      }
      boolean empty = lines == 1 && blankline;
      String filehash = empty ? null : Hex.encodeHexString(globalMd5Digest.digest());
      return new Metadata(lines, filehash, originalLineOffsets, lineHashes.toArray(new byte[0][]), empty);

    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to read file '%s' with encoding '%s'", file.getAbsolutePath(), encoding), e);
    }
  }

  private void updateDigestUTF8Char(char c, MessageDigest md5Digest) {
    CharBuffer cb = CharBuffer.allocate(1);
    cb.put(c);
    cb.flip();
    ByteBuffer bb = Charsets.UTF_8.encode(cb);
    byte[] array = bb.array();
    for (int i = 0; i < array.length; i++) {
      if (array[i] != 0) {
        md5Digest.update(array[i]);
      }
    }
  }

  static class Metadata {
    final int lines;
    final String hash;
    final long[] originalLineOffsets;
    final byte[][] lineHashes;
    final boolean empty;

    private Metadata(int lines, String hash, List<Long> originalLineOffsets, byte[][] lineHashes, boolean empty) {
      this.lines = lines;
      this.hash = hash;
      this.empty = empty;
      this.originalLineOffsets = Longs.toArray(originalLineOffsets);
      this.lineHashes = lineHashes;
    }
  }
}
