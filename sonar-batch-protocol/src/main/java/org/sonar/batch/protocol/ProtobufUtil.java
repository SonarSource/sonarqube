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
package org.sonar.batch.protocol;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.sonar.core.util.CloseableIterator;

import static java.lang.String.format;

public class ProtobufUtil {
  private ProtobufUtil() {
    // only static stuff
  }

  /**
   * Returns the message contained in the given file. Throws an unchecked exception
   * if the file does not exist or is empty.
   */
  public static <MSG extends Message> MSG readFile(File file, Parser<MSG> parser) {
    InputStream input = null;
    try {
      input = new BufferedInputStream(new FileInputStream(file));
      return parser.parseFrom(input);
    } catch (IOException e) {
      throw new IllegalStateException(format("Unable to read file %s", file), e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  /**
   * Writes a single message to a file, by replacing existing content. The message is
   * NOT appended.
   */
  public static void writeToFile(Message message, File toFile) {
    OutputStream out = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(toFile, false));
      message.writeTo(out);
    } catch (IOException e) {
      throw new IllegalStateException(format("Unable to write protobuf message to file %s", toFile), e);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  public static void writeStreamToFile(Iterable<? extends Message> messages, File toFile, boolean append) {
    OutputStream out = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(toFile, append));
      for (Message message : messages) {
        message.writeDelimitedTo(out);
      }
    } catch (IOException e) {
      throw new IllegalStateException(format("Unable to write protobuf messages to file %s", toFile), e);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  public static <MSG extends Message> CloseableIterator<MSG> readStreamFromFile(File file, Parser<MSG> parser) {
    try {
      return new ProtobufIterator<>(parser, new BufferedInputStream(new FileInputStream(file)));
    } catch (FileNotFoundException e) {
      throw new IllegalStateException(format("Unable to read protobuf file %s", file), e);
    }
  }

  private static class ProtobufIterator<MSG extends Message> extends CloseableIterator<MSG> {
    private final Parser<MSG> parser;
    private final InputStream input;

    private ProtobufIterator(Parser<MSG> parser, InputStream input) {
      this.parser = parser;
      this.input = input;
    }

    @Override
    protected MSG doNext() {
      try {
        return parser.parseDelimitedFrom(input);
      } catch (InvalidProtocolBufferException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    protected void doClose() throws Exception {
      IOUtils.closeQuietly(input);
    }
  }
}
