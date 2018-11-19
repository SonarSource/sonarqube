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

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static java.util.Objects.requireNonNull;

@Immutable
public class AddProjectRequest {

  private final String language;
  private final Optional<String> organization;
  private final String qualityProfile;
  private final String key;
  private final String projectKey;
  private final String projectUuid;

  private AddProjectRequest(Builder builder) {
    this.language = builder.language;
    this.organization = requireNonNull(builder.organization);
    this.qualityProfile = builder.qualityProfile;
    this.key = builder.key;
    this.projectKey = builder.projectKey;
    this.projectUuid = builder.projectUuid;
  }

  @CheckForNull
  public String getLanguage() {
    return language;
  }

  public Optional<String> getOrganization() {
    return organization;
  }

  @CheckForNull
  public String getQualityProfile() {
    return qualityProfile;
  }

  @CheckForNull
  public String getKey() {
    return key;
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
    private Optional<String> organization = Optional.empty();
    private String qualityProfile;
    private String key;
    private String projectKey;
    private String projectUuid;

    private Builder() {
      // enforce factory method use
    }

    public Builder setLanguage(@Nullable String language) {
      this.language = language;
      return this;
    }

    public Builder setOrganization(@Nullable String organization) {
      this.organization = Optional.ofNullable(organization);
      return this;
    }

    public Builder setQualityProfile(@Nullable String qualityProfile) {
      this.qualityProfile = qualityProfile;
      return this;
    }

    public Builder setKey(@Nullable String key) {
      this.key = key;
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
