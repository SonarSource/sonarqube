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
package org.sonar.core.notification;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.notifications.Notification;
import org.sonar.api.utils.SonarException;

import javax.persistence.*;
import java.io.*;
import java.util.Date;

@Entity
@Table(name = "notifications")
public class NotificationQueueElement {

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Integer id;

  @Column(name = "created_at")
  private Date createdAt;

  @Column(name = "data", updatable = true, nullable = true, length = 167772150)
  private byte[] data;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public void setNotification(Notification notification) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(notification);
      objectOutputStream.close();
      this.data = byteArrayOutputStream.toByteArray();

    } catch (IOException e) {
      throw new SonarException(e);

    } finally {
      IOUtils.closeQuietly(byteArrayOutputStream);
    }
  }

  public Notification getNotification() {
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

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }
}
