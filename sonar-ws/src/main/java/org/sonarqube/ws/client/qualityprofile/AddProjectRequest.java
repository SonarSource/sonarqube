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
package org.sonarqube.ws.client.qualityprofile;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class AddProjectRequest {

  private final String language;
  private final String profileName;
  private final String profileKey;
  private final String projectKey;
  private final String projectUuid;

  private AddProjectRequest(Builder builder) {
    this.language = builder.language;
    this.profileName = builder.profileName;
    this.profileKey = builder.profileKey;
    this.projectKey = builder.projectKey;
    this.projectUuid = builder.projectUuid;
  }

  @CheckForNull
  public String getLanguage() {
    return language;
  }

  @CheckForNull
  public String getProfileName() {
    return profileName;
  }

  @CheckForNull
  public String getProfileKey() {
    return profileKey;
  }

  @CheckForNull
  public String getProjectKey() {
    return projectKey;
  }

  @CheckForNull
  public String getProjectUuid() {
    return projectUuid;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String language;
    private String profileName;
    private String profileKey;
    private String projectKey;
    private String projectUuid;

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

    public Builder setProfileKey(@Nullable String profileKey) {
      this.profileKey = profileKey;
      return this;
    }

    public Builder setProjectKey(@Nullable String projectKey) {
      this.projectKey = projectKey;
      return this;
    }

    public Builder setProjectUuid(@Nullable String projectUuid) {
      this.projectUuid = projectUuid;
      return this;
    }

    public AddProjectRequest build() {
      return new AddProjectRequest(this);
    }
  }
}
