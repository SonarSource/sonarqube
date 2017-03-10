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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class SearchWsRequest {
  private String organizationKey;
  private boolean defaults;
  private String language;
  private String profileName;
  private String projectKey;

  public String getOrganizationKey() {
    return organizationKey;
  }

  public SearchWsRequest setOrganizationKey(String organizationKey) {
    this.organizationKey = organizationKey;
    return this;
  }

  public boolean getDefaults() {
    return defaults;
  }

  public SearchWsRequest setDefaults(boolean defaults) {
    this.defaults = defaults;
    return this;
  }

  @CheckForNull
  public String getLanguage() {
    return language;
  }

  public SearchWsRequest setLanguage(@Nullable String language) {
    this.language = language;
    return this;
  }

  @CheckForNull
  public String getProfileName() {
    return profileName;
  }

  public SearchWsRequest setProfileName(@Nullable String profileName) {
    this.profileName = profileName;
    return this;
  }

  @CheckForNull
  public String getProjectKey() {
    return projectKey;
  }

  public SearchWsRequest setProjectKey(@Nullable String projectKey) {
    this.projectKey = projectKey;
    return this;
  }
}
