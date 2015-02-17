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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.internal.DefaultInputFile;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

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

  private static final Logger LOG = LoggerFactory.getLogger(FileMetadata.class);

  private static final char LINE_FEED = '\n';
  private static final char CARRIAGE_RETURN = '\r';
  private final AnalysisMode analysisMode;

  public FileMetadata(AnalysisMode analysisMode) {
    this.analysisMode = analysisMode;
  }

  private abstract static class CharHandler {

    void handleAll(char c) {
    }

    void handleIgnoreEoL(char c) {
    }

    void newLine() {
    }

    void eof() {
    }
  }

  private static class LineCounter extends CharHandler {
    private boolean empty = true;
    private int lines = 1;
    private int nonBlankLines = 0;
    private boolean blankLine = true;
    boolean alreadyLoggedInvalidCharacter = false;
    private final File file;
    private final Charset encoding;

    LineCounter(File file, Charset encoding) {
      this.file = file;
      this.encoding = encoding;
    }

    @Override
    void handleAll(char c) {
      this.empty = false;
      if (!alreadyLoggedInvalidCharacter && c == '\ufffd') {
        LOG.warn("Invalid character encountered in file " + file + " at line " + lines
          + " for encoding " + encoding + ". Please fix file content or configure the encoding to be used using property '" + CoreProperties.ENCODING_PROPERTY + "'.");
        alreadyLoggedInvalidCharacter = true;
      }
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

  private static class FileHashComputer extends CharHandler {
    private MessageDigest globalMd5Digest = DigestUtils.getMd5Digest();
    private StringBuffer sb = new StringBuffer();

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

  private static class LineHashComputer extends CharHandler {
    private final MessageDigest lineMd5Digest = DigestUtils.getMd5Digest();
    private final StringBuilder sb = new StringBuilder();
    private final LineHashConsumer consumer;
    private int line = 1;

    public LineHashComputer(LineHashConsumer consumer) {
      this.consumer = consumer;
    }

    @Override
    void handleIgnoreEoL(char c) {
      if (!Character.isWhitespace(c)) {
        sb.append(c);
      }
    }

    @Override
    void newLine() {
      consumer.consume(line, sb.length() > 0 ? lineMd5Digest.digest(sb.toString().getBytes(Charsets.UTF_8)) : null);
      sb.setLength(0);
      line++;
    }

    @Override
    void eof() {
      consumer.consume(line, sb.length() > 0 ? lineMd5Digest.digest(sb.toString().getBytes(Charsets.UTF_8)) : null);
    }

  }

  private static class LineOffsetCounter extends CharHandler {
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
    LineCounter lineCounter = new LineCounter(file, encoding);
    FileHashComputer fileHashComputer = new FileHashComputer();
    LineOffsetCounter lineOffsetCounter = new LineOffsetCounter();
    if (!analysisMode.isPreview()) {
      scanFile(file, encoding, lineCounter, fileHashComputer, lineOffsetCounter);
    } else {
      // No need to compute line offsets in preview mode since there is no syntax highlighting
      scanFile(file, encoding, lineCounter, fileHashComputer);
    }
    return new Metadata(lineCounter.lines(), lineCounter.nonBlankLines(), fileHashComputer.getHash(), lineOffsetCounter.getOriginalLineOffsets(),
      lineCounter.isEmpty());
  }

  private static void scanFile(File file, Charset encoding, CharHandler... handlers) {
    char c = (char) 0;
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

  public static interface LineHashConsumer {

    void consume(int lineIdx, @Nullable byte[] hash);

  }

  /**
   * Compute a MD5 hash of each line of the file after removing of all blank chars
   */
  public static void computeLineHashesForIssueTracking(DefaultInputFile f, LineHashConsumer consumer) {
    scanFile(f.file(), f.charset(), new LineHashComputer(consumer));
  }
}
