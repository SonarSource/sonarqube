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

package org.sonarqube.ws.client.project;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;

@Immutable
public class UpdateKeyWsRequest {
  private final String id;
  private final String key;
  private final String newKey;

  public UpdateKeyWsRequest(Builder builder) {
    this.id = builder.id;
    this.key = builder.key;
    this.newKey = builder.newKey;
  }

  @CheckForNull
  public String getId() {
    return id;
  }

  @CheckForNull
  public String getKey() {
    return key;
  }

  public String getNewKey() {
    return newKey;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String id;
    private String key;
    private String newKey;

    private Builder() {
      // enforce method constructor
    }

    public Builder setId(@Nullable String id) {
      this.id = id;
      return this;
    }

    public Builder setKey(@Nullable String key) {
      this.key = key;
      return this;
    }

    public Builder setNewKey(String newKey) {
      this.newKey = newKey;
      return this;
    }

    public UpdateKeyWsRequest build() {
      checkArgument(newKey != null && !newKey.isEmpty(), "The new key must not be empty");
      return new UpdateKeyWsRequest(this);
    }
  }
}
