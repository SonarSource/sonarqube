/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.core.notification.db;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.notifications.Notification;
import org.sonar.api.utils.SonarException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

/**
 * @since 4.0
 */
public class NotificationQueueDto {

  private Long id;
  private Date createdAt;
  private byte[] data;

  public Long getId() {
    return id;
  }

  public NotificationQueueDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public NotificationQueueDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public byte[] getData() {
    return data;
  }

  public NotificationQueueDto setData(byte[] data) {
    this.data = data;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    NotificationQueueDto actionPlanDto = (NotificationQueueDto) o;
    return !(id != null ? !id.equals(actionPlanDto.id) : actionPlanDto.id != null);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  public static NotificationQueueDto toNotificationQueueDto(Notification notification) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(notification);
      objectOutputStream.close();
      return new NotificationQueueDto().setData(byteArrayOutputStream.toByteArray());

    } catch (IOException e) {
      throw new SonarException(e);

    } finally {
      IOUtils.closeQuietly(byteArrayOutputStream);
    }
  }

  public Notification toNotification() {
    if (this.data == null) {
      return null;
    }
    ByteArrayInputStream byteArrayInputStream = null;
    try {
      byteArrayInputStream = new ByteArrayInputStream(this.data);
      ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
      Object result = objectInputStream.readObject();
      objectInputStream.close();
      return (Notification) result;

    } catch (IOException e) {
      throw new SonarException(e);

    } catch (ClassNotFoundException e) {
      throw new SonarException(e);

    } finally {
      IOUtils.closeQuietly(byteArrayInputStream);
    }
  }

}
