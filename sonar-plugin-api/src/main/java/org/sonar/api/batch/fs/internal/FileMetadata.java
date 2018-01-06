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
package org.sonar.api.batch.fs.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.charhandler.CharHandler;
import org.sonar.api.batch.fs.internal.charhandler.FileHashComputer;
import org.sonar.api.batch.fs.internal.charhandler.LineCounter;
import org.sonar.api.batch.fs.internal.charhandler.LineHashComputer;
import org.sonar.api.batch.fs.internal.charhandler.LineOffsetCounter;

/**
 * Computes hash of files. Ends of Lines are ignored, so files with
 * same content but different EOL encoding have the same hash.
 */
@ScannerSide
@Immutable
public class FileMetadata {
  private static final char LINE_FEED = '\n';
  private static final char CARRIAGE_RETURN = '\r';

  /**
   * Compute hash of a file ignoring line ends differences.
   * Maximum performance is needed.
   */
  public Metadata readMetadata(InputStream stream, Charset encoding, String filePath, @Nullable CharHandler otherHandler) {
    LineCounter lineCounter = new LineCounter(filePath, encoding);
    FileHashComputer fileHashComputer = new FileHashComputer(filePath);
    LineOffsetCounter lineOffsetCounter = new LineOffsetCounter();

    if (otherHandler != null) {
      CharHandler[] handlers = {lineCounter, fileHashComputer, lineOffsetCounter, otherHandler};
      readFile(stream, encoding, filePath, handlers);
    } else {
      CharHandler[] handlers = {lineCounter, fileHashComputer, lineOffsetCounter};
      readFile(stream, encoding, filePath, handlers);
    }
    return new Metadata(lineCounter.lines(), lineCounter.nonBlankLines(), fileHashComputer.getHash(), lineOffsetCounter.getOriginalLineOffsets(),
      lineOffsetCounter.getLastValidOffset());
  }

  public Metadata readMetadata(InputStream stream, Charset encoding, String filePath) {
    return readMetadata(stream, encoding, filePath, null);
  }

  /**
   * For testing purpose
   */
  public Metadata readMetadata(Reader reader) {
    LineCounter lineCounter = new LineCounter("fromString", StandardCharsets.UTF_16);
    FileHashComputer fileHashComputer = new FileHashComputer("fromString");
    LineOffsetCounter lineOffsetCounter = new LineOffsetCounter();
    CharHandler[] handlers = {lineCounter, fileHashComputer, lineOffsetCounter};

    try {
      read(reader, handlers);
    } catch (IOException e) {
      throw new IllegalStateException("Should never occur", e);
    }
    return new Metadata(lineCounter.lines(), lineCounter.nonBlankLines(), fileHashComputer.getHash(), lineOffsetCounter.getOriginalLineOffsets(),
      lineOffsetCounter.getLastValidOffset());
  }

  public static void readFile(InputStream stream, Charset encoding, String filePath, CharHandler[] handlers) {
    try (Reader reader = new BufferedReader(new InputStreamReader(stream, encoding))) {
      read(reader, handlers);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Fail to read file '%s' with encoding '%s'", filePath, encoding), e);
    }
  }

  private static void read(Reader reader, CharHandler[] handlers) throws IOException {
    char c;
    int i = reader.read();
    boolean afterCR = false;
    while (i != -1) {
      c = (char) i;
      if (afterCR) {
        for (CharHandler handler : handlers) {
          if (c == CARRIAGE_RETURN) {
            handler.newLine();
            handler.handleAll(c);
          } else if (c == LINE_FEED) {
            handler.handleAll(c);
            handler.newLine();
          } else {
            handler.newLine();
            handler.handleIgnoreEoL(c);
            handler.handleAll(c);
          }
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
      if (afterCR) {
        handler.newLine();
      }
      handler.eof();
    }
  }

  @FunctionalInterface
  public interface LineHashConsumer {
    void consume(int lineIdx, @Nullable byte[] hash);
  }

  /**
   * Compute a MD5 hash of each line of the file after removing of all blank chars
   */
  public static void computeLineHashesForIssueTracking(InputFile f, LineHashConsumer consumer) {
    try {
      readFile(f.inputStream(), f.charset(), f.absolutePath(), new CharHandler[] {new LineHashComputer(consumer, f.file())});
    } catch (IOException e) {
      throw new IllegalStateException("Failed to compute line hashes for " + f.absolutePath(), e);
    }
  }
}
