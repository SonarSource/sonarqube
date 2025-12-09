/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.user;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.dismissmessage.MessageType;

public class UserDismissedMessageDto {

  private String uuid;
  /**
   * Uuid of the user that dismissed the message type
   */
  private String userUuid;
  /**
   * Uuid of the project for which the message type was dismissed
   */
  @CheckForNull
  private String projectUuid;
  /**
   * Message type of the dismissed message
   */
  private MessageType messageType;
  /**
   * Technical creation date
   */
  private long createdAt;

  public UserDismissedMessageDto() {
    // nothing to do here
  }

  public String getUuid() {
    return uuid;
  }

  public UserDismissedMessageDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public UserDismissedMessageDto setUserUuid(String userUuid) {
    this.userUuid = userUuid;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public UserDismissedMessageDto setProjectUuid(@Nullable String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public MessageType getMessageType() {
    return messageType;
  }

  public UserDismissedMessageDto setMessageType(MessageType messageType) {
    this.messageType = messageType;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public UserDismissedMessageDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
