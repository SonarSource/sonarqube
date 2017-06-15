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

public class ChangeParentRequest {
  private final String language;
  private final String parentKey;
  private final String parentName;
  private final String profileKey;
  private final String profileName;
  private final String organization;

  public ChangeParentRequest(Builder builder) {
    this.language = builder.language;
    this.parentKey = builder.parentKey;
    this.parentName = builder.parentName;
    this.profileKey = builder.profileKey;
    this.profileName = builder.profileName;
    this.organization = builder.organization;
  }

  public String getLanguage() {
    return language;
  }

  public String getParentKey() {
    return parentKey;
  }

  public String getParentName() {
    return parentName;
  }

  public String getProfileKey() {
    return profileKey;
  }

  public String getProfileName() {
    return profileName;
  }

  public String getOrganization() {
    return organization;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String language;
    private String parentKey;
    private String parentName;
    private String profileKey;
    private String profileName;
    private String organization;

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

    public Builder setParentKey(@Nullable String parentKey) {
      this.parentKey = parentKey;
      return this;
    }

    public Builder setParentName(@Nullable String parentName) {
      this.parentName = parentName;
      return this;
    }

    public Builder setOrganization(@Nullable String s) {
      this.organization = s;
      return this;
    }

    public ChangeParentRequest build() {
      return new ChangeParentRequest(this);
    }
  }
}
