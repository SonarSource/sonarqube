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

import com.google.protobuf.Message;
import com.google.protobuf.Parser;

import java.io.*;

public class ProtobufUtil {
  private ProtobufUtil() {
    // only static stuff
  }

  public static <T extends Message> T readFile(File file, Parser<T> parser) {
    try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
      return parser.parseFrom(input);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read file: " + file, e);
    }
  }

  public static void writeToFile(Message message, File toFile) {
    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(toFile, false))) {
      message.writeTo(out);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to write protocol buffer data to file " + toFile, e);
    }
  }

  public static <MESSAGE extends Message> void writeMessagesToFile(Iterable<MESSAGE> messages, File file) {
    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file, true))) {
      for (MESSAGE message : messages) {
        message.writeDelimitedTo(out);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read file: " + file, e);
    }
  }

}
