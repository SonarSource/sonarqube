/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.notification;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class RemoveRequest {
  private final String type;
  private final String channel;
  private final String project;
  private final String login;

  private RemoveRequest(Builder builder) {
    this.channel = builder.channel;
    this.type = builder.type;
    this.project = builder.project;
    this.login = builder.login;
  }

  public String getType() {
    return type;
  }

  @CheckForNull
  public String getChannel() {
    return channel;
  }

  @CheckForNull
  public String getProject() {
    return project;
  }

  @CheckForNull
  public String getLogin() {
    return login;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String type;
    private String channel;
    private String project;
    private String login;

    private Builder() {
      // enforce factory method
    }

    public Builder setType(String type) {
      this.type = type;
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

    public Builder setLogin(@Nullable String login) {
      this.login = login;
      return this;
    }

    public RemoveRequest build() {
      requireNonNull(type, "Notification is required");
      return new RemoveRequest(this);
    }
  }
}
