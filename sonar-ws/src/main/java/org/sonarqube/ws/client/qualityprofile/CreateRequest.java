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
package org.sonarqube.ws.client.qualityprofile;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;

@Immutable
public class CreateRequest {

  private final String name;
  private final String language;
  private final String organizationKey;

  private CreateRequest(Builder builder) {
    this.name = builder.name;
    this.language = builder.language;
    this.organizationKey = builder.organizationKey;
  }

  public String getLanguage() {
    return language;
  }

  public String getName() {
    return name;
  }

  public String getOrganizationKey() {
    return organizationKey;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String language;
    private String name;
    private String organizationKey;

    private Builder() {
      // enforce factory method use
    }

    public Builder setLanguage(@Nullable String language) {
      this.language = language;
      return this;
    }

    public Builder setName(@Nullable String profileName) {
      this.name = profileName;
      return this;
    }

    public Builder setOrganizationKey(@Nullable String organizationKey) {
      this.organizationKey = organizationKey;
      return this;
    }

    public CreateRequest build() {
      checkArgument(language != null && !language.isEmpty(), "Language is mandatory and must not be empty.");
      checkArgument(name != null && !name.isEmpty(), "Profile name is mandatory and must not be empty.");
      checkArgument(organizationKey == null || !organizationKey.isEmpty(), "Organization key may be either null or not empty. Empty organization key is invalid.");
      return new CreateRequest(this);
    }
  }
}
