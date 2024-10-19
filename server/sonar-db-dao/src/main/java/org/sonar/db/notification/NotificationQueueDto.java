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
package org.sonar.db.notification;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.sonar.api.notifications.Notification;

/**
 * @since 3.7.1
 */
public class NotificationQueueDto {

  private String uuid;
  private byte[] data;
  private long createdAt;

  public String getUuid() {
    return uuid;
  }

  NotificationQueueDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public byte[] getData() {
    return data;
  }

  public NotificationQueueDto setData(byte[] data) {
    this.data = data;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  NotificationQueueDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public static NotificationQueueDto toNotificationQueueDto(Notification notification) {
    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
      objectOutputStream.writeObject(notification);
      objectOutputStream.close();
      return new NotificationQueueDto().setData(byteArrayOutputStream.toByteArray());

    } catch (IOException e) {
      throw new IllegalStateException("Unable to write notification", e);
    }
  }

  public <T extends Notification> T toNotification() throws IOException, ClassNotFoundException {
    if (this.data == null) {
      return null;
    }

    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.data);
      ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
      Object result = objectInputStream.readObject();
      objectInputStream.close();
      return (T) result;
    }
  }

}
