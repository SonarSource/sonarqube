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
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import javax.annotation.CheckForNull;

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

  private abstract class CharHandler {

    void handleAll(char c) {
    }

    void handleIgnoreEoL(char c) {
    }

    void newLine() {
    }

    void eof() {
    }
  }

  private class LineCounter extends CharHandler {
    private boolean empty = true;
    private int lines = 1;
    private int nonBlankLines = 0;
    private boolean blankLine = true;

    @Override
    void handleAll(char c) {
      this.empty = false;
    }

    @Override
    void newLine() {
      lines++;
      if (!blankLine) {
        nonBlankLines++;
      }
      blankLine = true;
    }

    @Override
    void handleIgnoreEoL(char c) {
      if (!Character.isWhitespace(c)) {
        blankLine = false;
      }
    }

    @Override
    void eof() {
      if (!blankLine) {
        nonBlankLines++;
      }
    }

    public int lines() {
      return lines;
    }

    public int nonBlankLines() {
      return nonBlankLines;
    }

    public boolean isEmpty() {
      return empty;
    }
  }

  private class FileHashComputer extends CharHandler {
    private MessageDigest globalMd5Digest = DigestUtils.getMd5Digest();
    private boolean emptyFile = true;;

    @Override
    void handleIgnoreEoL(char c) {
      emptyFile = false;
      updateDigestUTF8Char(c, globalMd5Digest);
    }

    @Override
    void newLine() {
      emptyFile = false;
      updateDigestUTF8Char(LINE_FEED, globalMd5Digest);
    }

    @CheckForNull
    public String getHash() {
      return emptyFile ? null : Hex.encodeHexString(globalMd5Digest.digest());
    }
  }

  private class LineOffsetCounter extends CharHandler {
    private long currentOriginalOffset = 0;
    private List<Long> originalLineOffsets = new ArrayList<Long>();

    public LineOffsetCounter() {
      originalLineOffsets.add(0L);
    }

    @Override
    void handleAll(char c) {
      currentOriginalOffset++;
    }

    @Override
    void newLine() {
      originalLineOffsets.add(currentOriginalOffset);
    }

    public List<Long> getOriginalLineOffsets() {
      return originalLineOffsets;
    }

  }

  private class LineHashesComputer extends CharHandler {
    private List<Object> lineHashes = new ArrayList<Object>();
    private MessageDigest lineMd5Digest = DigestUtils.getMd5Digest();
    private boolean blankLine = true;

    @Override
    void handleIgnoreEoL(char c) {
      if (!Character.isWhitespace(c)) {
        blankLine = false;
        updateDigestUTF8Char(c, lineMd5Digest);
      }
    }

    @Override
    void newLine() {
      lineHashes.add(blankLine ? null : lineMd5Digest.digest());
      blankLine = true;
    }

    @Override
    void eof() {
      lineHashes.add(blankLine ? null : lineMd5Digest.digest());
    }

    public byte[][] lineHashes() {
      return lineHashes.toArray(new byte[0][]);
    }
  }

  /**
   * Compute hash of a file ignoring line ends differences.
   * Maximum performance is needed.
   */
  Metadata read(File file, Charset encoding) {
    char c = (char) 0;
    LineCounter lineCounter = new LineCounter();
    FileHashComputer fileHashComputer = new FileHashComputer();
    LineOffsetCounter lineOffsetCounter = new LineOffsetCounter();
    LineHashesComputer lineHashesComputer = new LineHashesComputer();
    CharHandler[] handlers = new CharHandler[] {lineCounter, fileHashComputer, lineOffsetCounter, lineHashesComputer};
    try (BOMInputStream bomIn = new BOMInputStream(new FileInputStream(file),
      ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);
      Reader reader = new BufferedReader(new InputStreamReader(bomIn, encoding))) {
      int i = reader.read();
      boolean afterCR = false;
      while (i != -1) {
        c = (char) i;
        if (afterCR) {
          for (CharHandler handler : handlers) {
            if (c != CARRIAGE_RETURN && c != LINE_FEED) {
              handler.handleIgnoreEoL(c);
            }
            handler.handleAll(c);
            handler.newLine();
          }
          afterCR = c == CARRIAGE_RETURN;
        } else if (c == LINE_FEED) {
          for (CharHandler handler : handlers) {
            handler.handleAll(c);
            handler.newLine();
          }
        } else if (c == CARRIAGE_RETURN) {
          afterCR = true;
          for (CharHandler handler : handlers) {
            handler.handleAll(c);
          }
        } else {
          for (CharHandler handler : handlers) {
            handler.handleIgnoreEoL(c);
            handler.handleAll(c);
          }
        }
        i = reader.read();
      }
      for (CharHandler handler : handlers) {
        handler.eof();
      }
      return new Metadata(lineCounter.lines(), lineCounter.nonBlankLines(), fileHashComputer.getHash(), lineOffsetCounter.getOriginalLineOffsets(),
        lineHashesComputer.lineHashes(), lineCounter.isEmpty());

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
    final int nonBlankLines;
    final String hash;
    final long[] originalLineOffsets;
    final byte[][] lineHashes;
    final boolean empty;

    private Metadata(int lines, int nonBlankLines, String hash, List<Long> originalLineOffsets, byte[][] lineHashes, boolean empty) {
      this.lines = lines;
      this.nonBlankLines = nonBlankLines;
      this.hash = hash;
      this.empty = empty;
      this.originalLineOffsets = Longs.toArray(originalLineOffsets);
      this.lineHashes = lineHashes;
    }
  }
}
