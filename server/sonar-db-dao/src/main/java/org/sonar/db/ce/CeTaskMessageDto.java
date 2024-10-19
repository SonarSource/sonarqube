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
package org.sonar.db.ce;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.db.dismissmessage.MessageType;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang3.StringUtils.abbreviate;

public class CeTaskMessageDto {
  @VisibleForTesting
  static final int MAX_MESSAGE_SIZE = 4000;
  /**
   * Unique identifier of each message. Not null
   */
  private String uuid;
  /**
   * UUID of the task the message belongs to. Not null
   */
  private String taskUuid;

  /**
   * The text of the message. Not null
   */
  private String message;

  /**
   * Type of the message
   */
  private MessageType type;
  /**
   * Timestamp the message was created. Not null
   */
  private long createdAt;

  public CeTaskMessageDto() {
    //Nothing to do
  }

  public String getUuid() {
    return uuid;
  }

  public CeTaskMessageDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getTaskUuid() {
    return taskUuid;
  }

  public CeTaskMessageDto setTaskUuid(String taskUuid) {
    this.taskUuid = taskUuid;
    return this;
  }

  public String getMessage() {
    return message;
  }

  public CeTaskMessageDto setMessage(String message) {
    checkArgument(message != null && !message.isEmpty(), "message can't be null nor empty");
    this.message = abbreviate(message, MAX_MESSAGE_SIZE);
    return this;
  }

  public MessageType getType() {
    return type;
  }

  public CeTaskMessageDto setType(MessageType type) {
    this.type = type;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public CeTaskMessageDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
