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
package org.sonar.server.exceptions;

import com.google.common.base.Preconditions;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

public class Message {

  private final String msg;

  private Message(String format, Object... params) {
    Preconditions.checkArgument(!isNullOrEmpty(format), "Message cannot be empty");
    this.msg = format(format, params);
  }

  public String getMessage() {
    return msg;
  }

  public static Message of(String msg, Object... arguments) {
    return new Message(msg, arguments);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Message other = (Message) o;
    return this.msg.equals(other.msg);
  }

  @Override
  public int hashCode() {
    return msg.hashCode();
  }

  @Override
  public String toString() {
    return msg;
  }
}
