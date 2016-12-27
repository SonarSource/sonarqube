/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonarqube.ws.client.notification;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class AddRequest {
  private final String notification;
  private final String channel;
  private final String project;

  private AddRequest(Builder builder) {
    this.channel = builder.channel;
    this.notification = builder.notification;
    this.project = builder.project;
  }

  public String getNotification() {
    return notification;
  }

  @CheckForNull
  public String getChannel() {
    return channel;
  }

  @CheckForNull
  public String getProject() {
    return project;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String notification;
    private String channel;
    private String project;

    private Builder() {
      // enforce factory method
    }

    public Builder setNotification(String notification) {
      this.notification = notification;
      return this;
    }

    public Builder setChannel(@Nullable String channel) {
      this.channel = channel;
      return this;
    }

    public Builder setProject(@Nullable String project) {
      this.project = project;
      return this;
    }

    public AddRequest build() {
      requireNonNull(notification, "Notification is required");
      return new AddRequest(this);
    }
  }
}
