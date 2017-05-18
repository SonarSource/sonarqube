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
package org.sonarqube.ws.client.organization;

import javax.annotation.concurrent.Immutable;

@Immutable
public class CreateWsRequest {
  private final String name;
  private final String key;
  private final String description;
  private final String url;
  private final String avatar;

  private CreateWsRequest(Builder builder) {
    this.name = builder.name;
    this.key = builder.key;
    this.description = builder.description;
    this.url = builder.url;
    this.avatar = builder.avatar;
  }

  public String getName() {
    return name;
  }

  public String getKey() {
    return key;
  }

  public String getDescription() {
    return description;
  }

  public String getUrl() {
    return url;
  }

  public String getAvatar() {
    return avatar;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String name;
    private String key;
    private String description;
    private String url;
    private String avatar;

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setKey(String key) {
      this.key = key;
      return this;
    }

    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder setUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder setAvatar(String avatar) {
      this.avatar = avatar;
      return this;
    }

    public CreateWsRequest build() {
      return new CreateWsRequest(this);
    }
  }
}
