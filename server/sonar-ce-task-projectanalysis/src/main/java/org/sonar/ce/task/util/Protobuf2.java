/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Parser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.annotation.CheckForNull;

public class Protobuf2 {

  public static final Protobuf2 PROTOBUF2 = new Protobuf2();

  private Protobuf2() {
  }

  public Protobuf2 writeTo(Message msg, OutputStream output) {
    try {
      msg.writeTo(output);
    } catch (IOException e) {
      throw new IllegalStateException("Can not write message " + msg, e);
    }
    return this;
  }

  public Protobuf2 writeDelimitedTo(Message msg, OutputStream output) {
    try {
      msg.writeDelimitedTo(output);
    } catch (IOException e) {
      throw new IllegalStateException("Can not write message " + msg, e);
    }
    return this;
  }

  public <M extends Message> M parseFrom(Parser<M> parser, InputStream input) {
    try {
      return parser.parseFrom(input);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException("Can not parse message", e);
    }
  }

  @CheckForNull
  public <M extends Message> M parseDelimitedFrom(Parser<M> parser, InputStream input) {
    try {
      return parser.parseDelimitedFrom(input);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalStateException("Can not parse message", e);
    }
  }
}
