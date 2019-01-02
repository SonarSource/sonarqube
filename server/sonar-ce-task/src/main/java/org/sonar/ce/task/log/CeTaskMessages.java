/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.log;

import java.util.Collection;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.ComputeEngineSide;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Provides the ability to record message attached to the current task.
 */
@ComputeEngineSide
public interface CeTaskMessages {

  /**
   * Add a single message
   */
  void add(Message message);

  /**
   * Add multiple messages. Use this method over {@link #add(Message)} if you have more than one message to add, you'll
   * allow implementation to batch persistence if possible.
   */
  void addAll(Collection<Message> messages);

  @Immutable
  class Message {
    private final String text;
    private final long timestamp;

    public Message(String text, long timestamp) {
      checkArgument(text != null && !text.isEmpty(), "Text can't be null nor empty");
      checkArgument(timestamp >= 0, "Text can't be less than 0");
      this.text = text;
      this.timestamp = timestamp;
    }

    public String getText() {
      return text;
    }

    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Message message1 = (Message) o;
      return timestamp == message1.timestamp &&
        Objects.equals(text, message1.text);
    }

    @Override
    public int hashCode() {
      return Objects.hash(text, timestamp);
    }

    @Override
    public String toString() {
      return "Message{" +
        "text='" + text + '\'' +
        ", timestamp=" + timestamp +
        '}';
    }
  }
}
