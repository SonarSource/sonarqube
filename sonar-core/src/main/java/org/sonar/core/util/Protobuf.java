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
package org.sonar.core.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;

/**
 * Utility to read and write Protocol Buffers messages
 */
public class Protobuf {
  private Protobuf() {
    // only static stuff
  }

  /**
   * Returns the message contained in {@code file}. Throws an unchecked exception
   * if the file does not exist, is empty or does not contain message with the
   * expected type.
   */
  public static <MSG extends Message> MSG read(File file, Parser<MSG> parser) {
    InputStream input = null;
    try {
      input = new BufferedInputStream(new FileInputStream(file));
      return parser.parseFrom(input);
    } catch (Exception e) {
      throw ContextException.of("Unable to read message", e).addContext("file", file);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  public static <MSG extends Message> MSG read(InputStream input, Parser<MSG> parser) {
    try {
      return parser.parseFrom(input);
    } catch (Exception e) {
      throw ContextException.of("Unable to read message", e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  /**
   * Writes a single message to {@code file}. Existing content is replaced, the message is not
   * appended.
   */
  public static void write(Message message, File toFile) {
    OutputStream out = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(toFile, false));
      message.writeTo(out);
    } catch (Exception e) {
      throw ContextException.of("Unable to write message", e).addContext("file", toFile);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  /**
   * Streams multiple messages to {@code file}. Reading the messages back requires to
   * call methods {@code readStream(...)}.
   * <p>
   * See https://developers.google.com/protocol-buffers/docs/techniques#streaming
   * </p>
   */
  public static <MSG extends Message> void writeStream(Iterable<MSG> messages, File toFile, boolean append) {
    OutputStream out = null;
    try {
      out = new BufferedOutputStream(new FileOutputStream(toFile, append));
      writeStream(messages, out);
    } catch (Exception e) {
      throw ContextException.of("Unable to write messages", e).addContext("file", toFile);
    } finally {
      IOUtils.closeQuietly(out);
    }
  }

  /**
   * Streams multiple messages to {@code output}. Reading the messages back requires to
   * call methods {@code readStream(...)}.
   * <p>
   * See https://developers.google.com/protocol-buffers/docs/techniques#streaming
   * </p>
   */
  public static <MSG extends Message> void writeStream(Iterable<MSG> messages, OutputStream output) {
    try {
      for (Message message : messages) {
        message.writeDelimitedTo(output);
      }
    } catch (Exception e) {
      throw ContextException.of("Unable to write messages", e);
    }
  }

  /**
   * Reads a stream of messages. This method returns an empty iterator if there are no messages. An 
   * exception is raised on IO error, if file does not exist or if messages have a 
   * different type than {@code parser}.
   */
  public static <MSG extends Message> CloseableIterator<MSG> readStream(File file, Parser<MSG> parser) {
    try {
      // the input stream is closed by the CloseableIterator
      BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
      return readStream(input, parser);
    } catch (Exception e) {
      throw ContextException.of("Unable to read messages", e).addContext("file", file);
    }
  }

  /**
   * Reads a stream of messages. This method returns an empty iterator if there are no messages. An 
   * exception is raised on IO error or if messages have a different type than {@code parser}.
   * <p>
   *   The stream is not closed by this method. It is closed when {@link CloseableIterator} traverses 
   *   all messages or when {@link CloseableIterator#close()} is called.
   * </p>
   */
  public static <MSG extends Message> CloseableIterator<MSG> readStream(InputStream input, Parser<MSG> parser) {
    // the stream is closed by the CloseableIterator
    return new StreamIterator<>(parser, input);
  }

  private static class StreamIterator<MSG extends Message> extends CloseableIterator<MSG> {
    private final Parser<MSG> parser;
    private final InputStream input;

    private StreamIterator(Parser<MSG> parser, InputStream input) {
      this.parser = parser;
      this.input = input;
    }

    @Override
    protected MSG doNext() {
      try {
        return parser.parsePartialDelimitedFrom(input);
      } catch (InvalidProtocolBufferException e) {
        throw ContextException.of(e);
      }
    }

    @Override
    protected void doClose() {
      IOUtils.closeQuietly(input);
    }
  }

  /**
   * Call a setter method of {@link com.google.protobuf.GeneratedMessage.Builder} if the parameter
   * is not null. Do nothing if parameter is null.
   * <br/>
   * This utility method is convenient as the setter methods of Protobuf 2 do not accept
   * {@code null} parameters. It avoid increasing complexity with "if not null" conditions.
   * <br/>
   * Example:
   * <pre>
   * setNullable(dto.getLine(), issueBuilder::setLine);
   * </pre>
   */
  public static <PARAM> void setNullable(@Nullable PARAM parameter, Function<PARAM, ?> setter) {
    if (parameter != null) {
      setter.apply(parameter);
    }
  }

  /**
   * Same as {@link #setNullable(Object, Function)} but the parameter is converted by the function "{@code paramConverter}"
   * before being applied to setter. If the converter returns {@code null}, then setter method
   * is not called.
   * <br/>
   * Example:
   * <pre>
   * setNullable(dto.getIssueCreationDate(), issueBuilder::setCreationDate, DateUtils::formatDateTime);
   * </pre>
   * @see #setNullable(Object, Function)
   */
  public static <PARAM, PARAM2> void setNullable(@Nullable PARAM param, Function<PARAM2, ?> setter,
    Function<PARAM, PARAM2> paramConverter) {
    if (param != null) {
      PARAM2 output = paramConverter.apply(param);
      setNullable(output, setter);
    }
  }
}
