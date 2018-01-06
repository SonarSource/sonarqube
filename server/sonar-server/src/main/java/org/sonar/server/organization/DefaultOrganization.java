/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.organization;

import static java.util.Objects.requireNonNull;

public class DefaultOrganization {
  private final String uuid;
  private final String key;
  private final String name;
  private final long createdAt;
  private final long updatedAt;

  private DefaultOrganization(Builder builder) {
    this.uuid = requireNonNull(builder.uuid, "uuid can't be null");
    this.key = requireNonNull(builder.key, "key can't be null");
    this.name = requireNonNull(builder.name, "name can't be null");
    this.createdAt = requireNonNull(builder.createdAt, "createdAt can't be null");
    this.updatedAt = requireNonNull(builder.updatedAt, "updatedAt can't be null");
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public String getUuid() {
    return uuid;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public String toString() {
    return "DefaultOrganization{" +
      "uuid='" + uuid + '\'' +
      ", key='" + key + '\'' +
      ", name='" + name + '\'' +
      ", createdAt=" + createdAt +
      ", updatedAt=" + updatedAt +
      '}';
  }

  public static final class Builder {
    private String uuid;
    private String key;
    private String name;
    private Long createdAt;
    private Long updatedAt;

    public Builder setUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder setKey(String key) {
      this.key = key;
      return this;
    }

    public Builder setName(String name) {
      this.name = name;
      return this;
    }

    public Builder setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder setUpdatedAt(long updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public DefaultOrganization build() {
      return new DefaultOrganization(this);
    }
  }
}
