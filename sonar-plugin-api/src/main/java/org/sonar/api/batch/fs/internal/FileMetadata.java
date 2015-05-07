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
package org.sonar.api.batch.fs.internal;

import org.sonar.api.BatchSide;

import com.google.common.base.Charsets;
import com.google.common.primitives.Ints;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.sonar.api.CoreProperties;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

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
@BatchSide
public class FileMetadata {

  private static final Logger LOG = Loggers.get(FileMetadata.class);

  private static final char LINE_FEED = '\n';
  private static final char CARRIAGE_RETURN = '\r';

  public abstract static class CharHandler {

    protected void handleAll(char c) {
    }

    protected void handleIgnoreEoL(char c) {
    }

    protected void newLine() {
    }

    protected void eof() {
    }
  }

  private static class LineCounter extends CharHandler {
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
    protected void handleAll(char c) {
      if (!alreadyLoggedInvalidCharacter && c == '\ufffd') {
        LOG.warn("Invalid character encountered in file {} at line {} for encoding {}. Please fix file content or configure the encoding to be used using property '{}'.", file,
          lines, encoding, CoreProperties.ENCODING_PROPERTY);
        alreadyLoggedInvalidCharacter = true;
      }
    }

    @Override
    protected void newLine() {
      lines++;
      if (!blankLine) {
        nonBlankLines++;
      }
      blankLine = true;
    }

    @Override
    protected void handleIgnoreEoL(char c) {
      if (!Character.isWhitespace(c)) {
        blankLine = false;
      }
    }

    @Override
    protected void eof() {
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

  }

  private static class FileHashComputer extends CharHandler {
    private MessageDigest globalMd5Digest = DigestUtils.getMd5Digest();
    private StringBuilder sb = new StringBuilder();

    @Override
    protected void handleIgnoreEoL(char c) {
      sb.append(c);
    }

    @Override
    protected void newLine() {
      sb.append(LINE_FEED);
      globalMd5Digest.update(sb.toString().getBytes(Charsets.UTF_8));
      sb.setLength(0);
    }

    @Override
    protected void eof() {
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
    protected void handleIgnoreEoL(char c) {
      if (!Character.isWhitespace(c)) {
        sb.append(c);
      }
    }

    @Override
    protected void newLine() {
      consumer.consume(line, sb.length() > 0 ? lineMd5Digest.digest(sb.toString().getBytes(Charsets.UTF_8)) : null);
      sb.setLength(0);
      line++;
    }

    @Override
    protected void eof() {
      if (this.line > 0) {
        consumer.consume(line, sb.length() > 0 ? lineMd5Digest.digest(sb.toString().getBytes(Charsets.UTF_8)) : null);
      }
    }

  }

  private static class LineOffsetCounter extends CharHandler {
    private int currentOriginalOffset = 0;
    private List<Integer> originalLineOffsets = new ArrayList<Integer>();
    private int lastValidOffset = 0;

    public LineOffsetCounter() {
      originalLineOffsets.add(0);
    }

    @Override
    protected void handleAll(char c) {
      currentOriginalOffset++;
    }

    @Override
    protected void newLine() {
      originalLineOffsets.add(currentOriginalOffset);
    }

    @Override
    protected void eof() {
      lastValidOffset = currentOriginalOffset;
    }

    public List<Integer> getOriginalLineOffsets() {
      return originalLineOffsets;
    }

    public int getLastValidOffset() {
      return lastValidOffset;
    }

  }

  /**
   * Compute hash of a file ignoring line ends differences.
   * Maximum performance is needed.
   */
  public Metadata readMetadata(File file, Charset encoding) {
    LineCounter lineCounter = new LineCounter(file, encoding);
    FileHashComputer fileHashComputer = new FileHashComputer();
    LineOffsetCounter lineOffsetCounter = new LineOffsetCounter();
    readFile(file, encoding, lineCounter, fileHashComputer, lineOffsetCounter);
    return new Metadata(lineCounter.lines(), lineCounter.nonBlankLines(), fileHashComputer.getHash(), lineOffsetCounter.getOriginalLineOffsets(),
      lineOffsetCounter.getLastValidOffset());
  }

  /**
   * For testing purpose
   */
  public Metadata readMetadata(Reader reader) {
    LineCounter lineCounter = new LineCounter(new File("fromString"), Charsets.UTF_16);
    FileHashComputer fileHashComputer = new FileHashComputer();
    LineOffsetCounter lineOffsetCounter = new LineOffsetCounter();
    try {
      read(reader, lineCounter, fileHashComputer, lineOffsetCounter);
    } catch (IOException e) {
      throw new IllegalStateException("Should never occurs", e);
    }
    return new Metadata(lineCounter.lines(), lineCounter.nonBlankLines(), fileHashComputer.getHash(), lineOffsetCounter.getOriginalLineOffsets(),
      lineOffsetCounter.getLastValidOffset());
  }

  public static void readFile(File file, Charset encoding, CharHandler... handlers) {
    try (BOMInputStream bomIn = new BOMInputStream(new FileInputStream(file),
      ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_32LE, ByteOrderMark.UTF_32BE);
      Reader reader = new BufferedReader(new InputStreamReader(bomIn, encoding))) {
      read(reader, handlers);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to read file '%s' with encoding '%s'", file.getAbsolutePath(), encoding), e);
    }
  }

  private static void read(Reader reader, CharHandler... handlers) throws IOException {
    char c = (char) 0;
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
  }

  public static class Metadata {
    final int lines;
    final int nonBlankLines;
    final String hash;
    final int[] originalLineOffsets;
    final int lastValidOffset;

    private Metadata(int lines, int nonBlankLines, String hash, List<Integer> originalLineOffsets, int lastValidOffset) {
      this.lines = lines;
      this.nonBlankLines = nonBlankLines;
      this.hash = hash;
      this.originalLineOffsets = Ints.toArray(originalLineOffsets);
      this.lastValidOffset = lastValidOffset;
    }
  }

  public interface LineHashConsumer {

    void consume(int lineIdx, @Nullable byte[] hash);

  }

  /**
   * Compute a MD5 hash of each line of the file after removing of all blank chars
   */
  public static void computeLineHashesForIssueTracking(DefaultInputFile f, LineHashConsumer consumer) {
    readFile(f.file(), f.charset(), new LineHashComputer(consumer));
  }
}
