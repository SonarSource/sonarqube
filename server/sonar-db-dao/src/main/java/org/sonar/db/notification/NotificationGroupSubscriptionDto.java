/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.notification;

public class NotificationGroupSubscriptionDto {

  private String uuid;
  private String groupUuid;
  private String notificationType;
  private String channelKey;
  private long createdAt;

  public NotificationGroupSubscriptionDto() {
    // Builder class
  }

  public String getUuid() {
    return uuid;
  }

  public NotificationGroupSubscriptionDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getGroupUuid() {
    return groupUuid;
  }

  public NotificationGroupSubscriptionDto setGroupUuid(String groupUuid) {
    this.groupUuid = groupUuid;
    return this;
  }

  public String getNotificationType() {
    return notificationType;
  }

  public NotificationGroupSubscriptionDto setNotificationType(String notificationType) {
    this.notificationType = notificationType;
    return this;
  }

  public String getChannelKey() {
    return channelKey;
  }

  public NotificationGroupSubscriptionDto setChannelKey(String channelKey) {
    this.channelKey = channelKey;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public NotificationGroupSubscriptionDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
