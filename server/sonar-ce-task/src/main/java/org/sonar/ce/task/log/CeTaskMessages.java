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
package org.sonar.ce.task.log;

import java.util.Collection;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.db.dismissmessage.MessageType;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

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
    private final MessageType type;

    public Message(String text, long timestamp, MessageType type) {
      requireNonNull(text, "Text can't be null");
      checkArgument(!text.isEmpty(), "Text can't be empty");
      checkArgument(timestamp >= 0, "Timestamp can't be less than 0");
      this.text = text;
      this.timestamp = timestamp;
      this.type = type;
    }

    public Message(String text, long timestamp) {
      this(text, timestamp, MessageType.GENERIC);
    }

    public String getText() {
      return text;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public MessageType getType() {
      return type;
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
        ", type=" + type +
        '}';
    }
  }
}
