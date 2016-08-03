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

package org.sonarqube.ws.client.component;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

public class BulkUpdateWsRequest {
  private final String id;
  private final String key;
  private final String from;
  private final String to;
  private final boolean dryRun;

  public BulkUpdateWsRequest(Builder builder) {
    this.id = builder.id;
    this.key = builder.key;
    this.from = builder.from;
    this.to = builder.to;
    this.dryRun = builder.dryRun;
  }

  @CheckForNull
  public String getId() {
    return id;
  }

  @CheckForNull
  public String getKey() {
    return key;
  }

  public String getFrom() {
    return from;
  }

  public String getTo() {
    return to;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String id;
    private String key;
    private String from;
    private String to;
    private boolean dryRun;

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

    public Builder setFrom(String from) {
      this.from = from;
      return this;
    }

    public Builder setTo(String to) {
      this.to = to;
      return this;
    }

    public Builder setDryRun(boolean dryRun) {
      this.dryRun = dryRun;
      return this;
    }

    public BulkUpdateWsRequest build() {
      checkArgument(from != null && !from.isEmpty(), "The string to match must not be empty");
      checkArgument(to != null && !to.isEmpty(), "The string replacement must not be empty");
      return new BulkUpdateWsRequest(this);
    }
  }
}
