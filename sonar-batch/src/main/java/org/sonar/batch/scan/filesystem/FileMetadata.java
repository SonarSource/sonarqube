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
import com.google.common.primitives.Ints;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.AnalysisMode;

import javax.annotation.CheckForNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes hash of files. Ends of Lines are ignored, so files with
 * same content but different EOL encoding have the same hash.
 */
public class FileMetadata implements BatchComponent {

  private static final char LINE_FEED = '\n';
  private static final char CARRIAGE_RETURN = '\r';
  private final AnalysisMode analysisMode;

  public FileMetadata(AnalysisMode analysisMode) {
    this.analysisMode = analysisMode;
  }

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

    StringBuffer sb = new StringBuffer();

    @Override
    void handleIgnoreEoL(char c) {
      sb.append(c);
    }

    @Override
    void newLine() {
      sb.append(LINE_FEED);
      globalMd5Digest.update(sb.toString().getBytes(Charsets.UTF_8));
      sb.setLength(0);
    }

    @Override
    void eof() {
      if (sb.length() > 0) {
        globalMd5Digest.update(sb.toString().getBytes(Charsets.UTF_8));
      }
    }

    @CheckForNull
    public String getHash() {
      return Hex.encodeHexString(globalMd5Digest.digest());
    }
  }

  private class LineOffsetCounter extends CharHandler {
    private int currentOriginalOffset = 0;
    private List<Integer> originalLineOffsets = new ArrayList<Integer>();

    public LineOffsetCounter() {
      originalLineOffsets.add(0);
    }

    @Override
    void handleAll(char c) {
      currentOriginalOffset++;
    }

    @Override
    void newLine() {
      originalLineOffsets.add(currentOriginalOffset);
    }

    public List<Integer> getOriginalLineOffsets() {
      return originalLineOffsets;
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
    CharHandler[] handlers;
    if (analysisMode.isPreview()) {
      // No need to compute line offsets in preview mode since there is no syntax highlighting
      handlers = new CharHandler[] {lineCounter, fileHashComputer};
    } else {
      handlers = new CharHandler[] {lineCounter, fileHashComputer, lineOffsetCounter};
    }
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
        lineCounter.isEmpty());

    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to read file '%s' with encoding '%s'", file.getAbsolutePath(), encoding), e);
    }
  }

  static class Metadata {
    final int lines;
    final int nonBlankLines;
    final String hash;
    final int[] originalLineOffsets;
    final boolean empty;

    private Metadata(int lines, int nonBlankLines, String hash, List<Integer> originalLineOffsets, boolean empty) {
      this.lines = lines;
      this.nonBlankLines = nonBlankLines;
      this.hash = hash;
      this.empty = empty;
      this.originalLineOffsets = Ints.toArray(originalLineOffsets);
    }
  }
}
