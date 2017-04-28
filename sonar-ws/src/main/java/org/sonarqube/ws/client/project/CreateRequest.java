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

@Immutable
public class CreateRequest {

  private final String organization;
  private final String key;
  private final String name;
  private final String branch;
  @CheckForNull
  private final String visibility;

  private CreateRequest(Builder builder) {
    this.organization = builder.organization;
    this.key = builder.key;
    this.name = builder.name;
    this.branch = builder.branch;
    this.visibility = builder.visibility;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  @CheckForNull
  public String getKey() {
    return key;
  }

  @CheckForNull
  public String getName() {
    return name;
  }

  @CheckForNull
  public String getBranch() {
    return branch;
  }

  @CheckForNull
  public String getVisibility() {
    return visibility;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String organization;
    private String key;
    private String name;
    private String branch;
    @CheckForNull
    private String visibility;

    private Builder() {
    }

    public Builder setOrganization(@Nullable String organization) {
      this.organization = organization;
      return this;
    }

    public Builder setKey(@Nullable String key) {
      this.key = key;
      return this;
    }

    public Builder setName(@Nullable String name) {
      this.name = name;
      return this;
    }

    public Builder setBranch(@Nullable String branch) {
      this.branch = branch;
      return this;
    }

    public Builder setVisibility(@Nullable String visibility) {
      this.visibility = visibility;
      return this;
    }

    public CreateRequest build() {
      return new CreateRequest(this);
    }
  }
}
