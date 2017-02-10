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
package org.sonarqube.ws.client.qualityprofile;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static com.google.common.base.Preconditions.checkArgument;

@Immutable
public class CreateRequest {

  private final String profileName;
  private final String language;

  private CreateRequest(Builder builder) {
    this.profileName = builder.profileName;
    this.language = builder.language;
  }

  public String getLanguage() {
    return language;
  }

  public String getProfileName() {
    return profileName;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String language;
    private String profileName;

    private Builder() {
      // enforce factory method use
    }

    public Builder setLanguage(@Nullable String language) {
      this.language = language;
      return this;
    }

    public Builder setProfileName(@Nullable String profileName) {
      this.profileName = profileName;
      return this;
    }

    public CreateRequest build() {
      checkArgument(language != null && !language.isEmpty(), "Language is mandatory and must not be empty.");
      checkArgument(profileName != null && !profileName.isEmpty(), "Profile name is mandatory and must not be empty.");
      return new CreateRequest(this);
    }
  }
}
