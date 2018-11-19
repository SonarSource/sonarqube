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

import javax.annotation.concurrent.Immutable;

@Immutable
public class AddUserRequest {

  private final String organization;
  private final String language;
  private final String qualityProfile;
  private final String userLogin;

  private AddUserRequest(Builder builder) {
    this.language = builder.language;
    this.organization = builder.organization;
    this.qualityProfile = builder.qualityProfile;
    this.userLogin = builder.userLogin;
  }

  public String getLanguage() {
    return language;
  }

  public String getOrganization() {
    return organization;
  }

  public String getQualityProfile() {
    return qualityProfile;
  }

  public String getUserLogin() {
    return userLogin;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String organization;
    private String qualityProfile;
    private String language;
    private String userLogin;

    private Builder() {
      // enforce factory method use
    }

    public Builder setLanguage(String language) {
      this.language = language;
      return this;
    }

    public Builder setOrganization(String organization) {
      this.organization = organization;
      return this;
    }

    public Builder setQualityProfile(String qualityProfile) {
      this.qualityProfile = qualityProfile;
      return this;
    }

    public Builder setUserLogin(String userLogin) {
      this.userLogin = userLogin;
      return this;
    }

    public AddUserRequest build() {
      return new AddUserRequest(this);
    }
  }
}
